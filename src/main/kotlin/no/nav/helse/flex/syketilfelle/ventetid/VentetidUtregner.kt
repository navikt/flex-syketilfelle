package no.nav.helse.flex.syketilfelle.ventetid

import no.nav.helse.flex.syketilfelle.logger
import no.nav.helse.flex.syketilfelle.syketilfellebit.Syketilfellebit
import no.nav.helse.flex.syketilfelle.syketilfellebit.SyketilfellebitRepository
import no.nav.helse.flex.syketilfelle.syketilfellebit.Tag
import no.nav.helse.flex.syketilfelle.syketilfellebit.tilSyketilfellebit
import no.nav.helse.flex.syketilfelle.syketilfellebit.utenKorrigerteSoknader
import no.nav.helse.flex.syketilfelle.sykmelding.mapTilBiter
import org.springframework.stereotype.Component
import java.time.DayOfWeek.*
import java.time.LocalDate
import java.time.Month
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit.DAYS
import java.time.temporal.TemporalAdjusters.next
import java.time.temporal.TemporalAdjusters.nextOrSame
import java.time.temporal.TemporalAdjusters.previous

@Component
class VentetidUtregner(
    private val syketilfellebitRepository: SyketilfellebitRepository,
) {
    val log = logger()
    final val koronaPeriodeMedFireDager =
        LocalDate.of(2020, Month.MARCH, 16).rangeTo(LocalDate.of(2021, Month.SEPTEMBER, 30))
    final val koronaPeriodeMedSeksDager =
        LocalDate.of(2021, Month.DECEMBER, 6).rangeTo(LocalDate.of(2022, Month.JUNE, 30))

    // Forbedrer ytelse ved å skiller ut konstante verdier sånn at settet ikke gjenopprettes for hvert kall.
    private companion object {
        private val AKTIVITET_TAGS =
            setOf(
                Tag.GRADERT_AKTIVITET,
                Tag.INGEN_AKTIVITET,
                Tag.BEHANDLINGSDAGER,
                Tag.ANNET_FRAVAR,
            )

        private val EKSKLUDERTE_TAGS =
            setOf(
                Tag.REISETILSKUDD,
                Tag.AVVENTENDE,
            )

        private val EGENMELDING_TAGS =
            setOf(
                Tag.SYKMELDING,
                Tag.BEKREFTET,
                Tag.ANNET_FRAVAR,
            )
    }

    fun beregnOmSykmeldingErUtenforVentetid(
        sykmeldingId: String,
        fnrs: List<String>,
        erUtenforVentetidRequest: ErUtenforVentetidRequest,
    ): Boolean {
        val biter =
            syketilfellebitRepository
                .findByFnrIn(fnrs)
                .filter { it.slettet == null }
                .map { it.tilSyketilfellebit() }
                .utenKorrigerteSoknader()

        val sykmeldingBiter = lagSykmeldingBiter(biter, sykmeldingId, fnrs, erUtenforVentetidRequest)

        val aktuellSykmeldingBiter = sykmeldingBiter.filter { it.ressursId == sykmeldingId }

        if (aktuellSykmeldingBiter.isEmpty()) {
            log.error("Fant ikke biter til sykmelding $sykmeldingId i flex-syketilfelledatabasen.")
            return false
        }

        val sykmeldingSisteTom = aktuellSykmeldingBiter.maxOf { it.tom }

        return sykmeldingBiter
            .asSequence()
            .filter { it.tags.contains(Tag.SYKMELDING) }
            .filter { bit -> bit.tags.any { tag -> tag in AKTIVITET_TAGS } }
            .filterNot { bit -> bit.tags.any { it in EKSKLUDERTE_TAGS } }
            .map { it.tilPeriode() }
            .flatMap { it.splittPeriodeMedBehandlingsdagerIPerioderForHverMandag() }
            .filter { it.fom <= sykmeldingSisteTom }
            .map { it.kuttBitSomErLengreEnnAktuellTom(sykmeldingSisteTom) }
            .toList()
            .mergePerioder()
            .fjernHelgFraSluttenAvPeriodenForSykmelding(sykmeldingId)
            .erUtenforVentetid()
    }

    private fun lagSykmeldingBiter(
        baseBiter: List<Syketilfellebit>,
        sykmeldingId: String,
        fnrs: List<String>,
        request: ErUtenforVentetidRequest,
    ): List<Syketilfellebit> =
        baseBiter.toMutableList().apply {
            request.sykmeldingKafkaMessage?.let { sykmeldingMessage ->
                addAll(sykmeldingMessage.mapTilBiter())
            }
            request.tilleggsopplysninger?.let { tilleggsopplysninger ->
                addAll(tilleggsopplysninger.mapTilBiter(sykmeldingId, fnrs.first()))
            }
        }

    private fun Tilleggsopplysninger.mapTilBiter(
        ressursId: String,
        fnr: String,
    ): List<Syketilfellebit> =
        this.egenmeldingsperioder?.map {
            Syketilfellebit(
                fom = it.fom,
                tom = it.tom,
                inntruffet = OffsetDateTime.now(),
                opprettet = OffsetDateTime.now(),
                tags = EGENMELDING_TAGS,
                ressursId = ressursId,
                fnr = fnr,
                orgnummer = null,
            )
        } ?: emptyList()

    private fun Syketilfellebit.tilPeriode(): Periode =
        Periode(
            tom = this.tom,
            fom = this.fom,
            redusertVentePeriode = this.tags.contains(Tag.REDUSERT_ARBEIDSGIVERPERIODE),
            behandlingsdager = this.tags.contains(Tag.BEHANDLINGSDAGER),
            ressursId = this.ressursId,
        )

    private fun Periode.kuttBitSomErLengreEnnAktuellTom(sykmeldingSisteTom: LocalDate): Periode =
        if (this.tom.isAfter(sykmeldingSisteTom)) {
            this.copy(tom = sykmeldingSisteTom)
        } else {
            this
        }

    private fun Periode.splittPeriodeMedBehandlingsdagerIPerioderForHverMandag(): List<Periode> {
        if (!this.behandlingsdager) {
            return listOf(this)
        }

        // Lazy: evaluerer kun så mange mandager som nødvendig.
        return generateSequence(this.fom.with(nextOrSame(MONDAY))) { currentMonday ->
            currentMonday.with(next(MONDAY)).takeIf { !it.isAfter(this.tom) }
        }.map { mandag -> this.copy(fom = mandag, tom = mandag) }
            .toList()
    }

    // Slår sammen periode som ligger kant i kant, eller som har kun helgedager mellom periodene.
    private fun List<Periode>.mergePerioder(): List<Periode> {
        if (size <= 1) return this

        // Sorter periodene med nyeste periode først for å enklere sjekke tid siden forrige periode.
        val sortertePerioder = sortedByDescending { it.tom }

        return sortertePerioder
            .drop(1)
            .fold(mutableListOf(sortertePerioder.first())) { akkumulerteListe, forrigePeriode ->
                val gjeldendePeriode = akkumulerteListe.last()

                if (skalMerges(forrigePeriode, gjeldendePeriode)) {
                    log.info(
                        "Gjeldende periode: ${gjeldendePeriode.tilLoggbarPeriode()} merges med forrige periode: ${forrigePeriode.tilLoggbarPeriode()}",
                    )
                    akkumulerteListe[akkumulerteListe.lastIndex] = mergePerioder(forrigePeriode, gjeldendePeriode)
                } else {
                    akkumulerteListe.add(forrigePeriode)
                }
                akkumulerteListe
            }.sortedByDescending { it.tom }
    }

    private fun mergePerioder(
        tidligerePeriode: Periode,
        senestePeriode: Periode,
    ): Periode =
        senestePeriode.copy(
            // Bruker tidligste fom-dato og setter redusertVentePeriode hvis en av periodene har det.
            fom = minOf(tidligerePeriode.fom, senestePeriode.fom),
            redusertVentePeriode = tidligerePeriode.redusertVentePeriode || senestePeriode.redusertVentePeriode,
        )

    private fun skalMerges(
        gjeldendePeriode: Periode,
        nestePeriode: Periode,
    ): Boolean {
        val dagerMellomPeriodene = DAYS.between(gjeldendePeriode.tom, nestePeriode.fom)

        if (dagerMellomPeriodene > 3) {
            return false
        }

        // Sjekk at alle dager mellom periodene er helgedager.
        return (1 until dagerMellomPeriodene)
            .asSequence()
            .map { gjeldendePeriode.tom.plusDays(it) }
            .all { it.dayOfWeek in setOf(SATURDAY, SUNDAY) }
    }

    private fun List<Periode>.fjernHelgFraSluttenAvPeriodenForSykmelding(sykmeldingId: String): List<Periode> =
        map { periode ->
            when {
                // Mandag til søndag blir mandag til fredag.
                periode.skalJusteresForHelg(sykmeldingId) -> {
                    val sisteFredagIPerioden = periode.tom.with(previous(FRIDAY))
                    if (sisteFredagIPerioden >= periode.fom) {
                        periode.copy(tom = sisteFredagIPerioden)
                    } else {
                        periode
                    }
                }

                else -> periode
            }
        }

    private fun Periode.skalJusteresForHelg(sykmeldingId: String): Boolean =
        ressursId == sykmeldingId && (tom.dayOfWeek == SATURDAY || tom.dayOfWeek == SUNDAY)

    private fun List<Periode>.erUtenforVentetid(): Boolean {
        this.forEachIndexed { index, periode ->
            if (periode.erLangNokForStandardVentetid()) {
                return true
            }

            if (periode.redusertVentePeriode && periode.erLangNokForKoronaVentetid()) {
                return true
            }

            if (this.erForLengeSidenForrigePeriode(index)) {
                return false
            }
        }
        return false
    }

    private fun Periode.erLangNokForStandardVentetid(): Boolean = DAYS.between(this.fom, this.tom) >= 16

    private fun Periode.erLangNokForKoronaVentetid(): Boolean =
        when {
            erKoronaPeriodeMedSeksDager() -> erLangNokForKoronaVentetid(5)
            erKoronaPeriodeMedFireDager() -> erLangNokForKoronaVentetid(3)
            else -> false
        }

    private fun Periode.erLangNokForKoronaVentetid(terskel: Long): Boolean = DAYS.between(this.fom, this.tom) >= terskel

    private fun Periode.erKoronaPeriodeMedSeksDager(): Boolean =
        koronaPeriodeMedSeksDager.contains(this.fom) || koronaPeriodeMedSeksDager.contains(this.tom)

    private fun Periode.erKoronaPeriodeMedFireDager(): Boolean =
        koronaPeriodeMedFireDager.contains(this.fom) || koronaPeriodeMedFireDager.contains(this.tom)

    private fun List<Periode>.erForLengeSidenForrigePeriode(index: Int): Boolean {
        val gjeldendePeriode = this[index]
        val nestePeriode = this.getOrNull(index + 1) ?: return false
        return DAYS.between(nestePeriode.tom, gjeldendePeriode.fom) > 16
    }

    private data class LoggbarPeriode(
        val id: String,
        val fom: LocalDate,
        val tom: LocalDate,
    )

    private data class Periode(
        val fom: LocalDate,
        val tom: LocalDate,
        val redusertVentePeriode: Boolean,
        val behandlingsdager: Boolean,
        val ressursId: String,
    )

    private fun Periode.tilLoggbarPeriode() =
        LoggbarPeriode(
            id = ressursId,
            fom = fom,
            tom = tom,
        )
}
