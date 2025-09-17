package no.nav.helse.flex.syketilfelle.ventetid

import no.nav.helse.flex.syketilfelle.logger
import no.nav.helse.flex.syketilfelle.syketilfellebit.Syketilfellebit
import no.nav.helse.flex.syketilfelle.syketilfellebit.SyketilfellebitRepository
import no.nav.helse.flex.syketilfelle.syketilfellebit.Tag
import no.nav.helse.flex.syketilfelle.syketilfellebit.tilSyketilfellebit
import no.nav.helse.flex.syketilfelle.syketilfellebit.utenKorrigerteSoknader
import no.nav.helse.flex.syketilfelle.sykmelding.mapTilBiter
import org.springframework.stereotype.Component
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.LocalDate
import java.time.Month
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit.DAYS
import java.time.temporal.TemporalAdjusters.next
import java.time.temporal.TemporalAdjusters.nextOrSame
import java.time.temporal.TemporalAdjusters.previous

const val SEKS_DAGER = 6L
const val FIRE_DAGER = 4L
const val SEKSTEN_DAGER = 16L

@Component
class VentetidUtregner(
    private val syketilfellebitRepository: SyketilfellebitRepository,
) {
    val log = logger()

    final val koronaPeriodeMedFireDager =
        LocalDate.of(2020, Month.MARCH, 16).rangeTo(LocalDate.of(2021, Month.SEPTEMBER, 30))

    final val koronaPeriodeMedSeksDager =
        LocalDate.of(2021, Month.DECEMBER, 6).rangeTo(LocalDate.of(2022, Month.JUNE, 30))

    // Skiller ut konstanter sånn at settet ikke gjenopprettes for hvert kall.
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
        identer: List<String>,
        erUtenforVentetidRequest: ErUtenforVentetidRequest,
    ): Boolean = beregnVenteperiode(sykmeldingId, identer, erUtenforVentetidRequest.tilVentetidRequest()) != null

    fun beregnVenteperiode(
        sykmeldingId: String,
        identer: List<String>,
        ventetidRequest: VentetidRequest,
    ): FomTomPeriode? {
        val biter =
            syketilfellebitRepository
                .findByFnrIn(identer)
                .filter { it.slettet == null }
                .map { it.tilSyketilfellebit() }
                .utenKorrigerteSoknader()

        val sykmeldingBiter = lagSykmeldingBiter(biter, sykmeldingId, identer, ventetidRequest)
        val aktuellSykmeldingBiter = sykmeldingBiter.filter { it.ressursId == sykmeldingId }

        if (aktuellSykmeldingBiter.isEmpty()) {
            log.error("Fant ikke biter til sykmelding $sykmeldingId i flex-syketilfelledatabasen.")
            return null
        }

        val sykmeldingSisteTom = aktuellSykmeldingBiter.maxOf { it.tom }

        val perioder =
            sykmeldingBiter
                .asSequence()
                .filter { it.tags.contains(Tag.SYKMELDING) }
                .filter { bit -> bit.tags.any { tag -> tag in AKTIVITET_TAGS } }
                .filterNot { bit -> bit.tags.any { it in EKSKLUDERTE_TAGS } }
                .map { it.tilPeriode() }
                .flatMap { it.splittPeriodeMedBehandlingsdagerTilMandager() }
                .filter { it.fom <= sykmeldingSisteTom }
                .map { it.kuttBitSomErLengreEnnAktuellTom(sykmeldingSisteTom) }
                .toList()
                .mergePerioder()
                .fjernHelgFraSluttenAvPeriodenForSykmelding(sykmeldingId)

        perioder.beregnVenteperiode()?.let { beregnetVenteperiode ->
            return FomTomPeriode(
                fom = beregnetVenteperiode.fom,
                tom = beregnVenteperiodeTom(beregnetVenteperiode.fom, perioder),
            )
        }

        // Hvis perioden normalt ikke er utenfor ventetid, men 'returnerPerioderInnenforVentetid' er satt,
        // returneres hele perioden inkludert eventuelle egenmeldingsdager.
        if (ventetidRequest.returnerPerioderInnenforVentetid) {
            perioder
                .asSequence()
                .filter { it.ressursId == sykmeldingId }
                .maxByOrNull { it.tom }
                ?.let { return FomTomPeriode(it.fom, it.tom) }
        }

        return null
    }

    private fun List<Periode>.beregnVenteperiode(): Periode? {
        // Hvis det er mindre enn 17 siden forrige periode og forrige periode var utenfor ventetid, returneres forrige
        // periodes ventetid.
        if (size >= 2) {
            val (_, forrigePeriode) = this
            if (!erForLengeSidenForrigePeriode(0) && forrigePeriode.erLengreEnnVentetid()) {
                return forrigePeriode
            }
        }
        // Går gjennom periodene og finner den første som kvalifiserer som venteperiode.
        for ((index, periode) in withIndex()) {
            when {
                periode.erLengreEnnVentetid() -> return periode
                erForLengeSidenForrigePeriode(index) -> return null
            }
        }

        return null
    }

    private fun Periode.erLengreEnnVentetid(): Boolean =
        erLengreEnnStandardVentetid() || (redusertVentePeriode && erLengreEnnKoronaVentetid())

    private fun lagSykmeldingBiter(
        baseBiter: List<Syketilfellebit>,
        sykmeldingId: String,
        fnrs: List<String>,
        ventetidRequest: VentetidRequest,
    ): List<Syketilfellebit> =
        baseBiter.toMutableList().apply {
            ventetidRequest.sykmeldingKafkaMessage?.let { sykmeldingMessage ->
                addAll(sykmeldingMessage.mapTilBiter())
            }
            ventetidRequest.tilleggsopplysninger?.let { tilleggsopplysninger ->
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

    private fun Periode.splittPeriodeMedBehandlingsdagerTilMandager(): List<Periode> {
        if (!this.behandlingsdager) {
            return listOf(this)
        }

        // Lazy: evaluerer så lenge neste mandag er før periodens tom.
        return generateSequence(this.fom.with(nextOrSame(MONDAY))) { aktuellMandag ->
            aktuellMandag.with(next(MONDAY)).takeIf { !it.isAfter(this.tom) }
        }.map { mandag -> this.copy(fom = mandag, tom = mandag) }.toList()
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
                    akkumulerteListe[akkumulerteListe.lastIndex] = mergePerioder(forrigePeriode, gjeldendePeriode)
                } else {
                    akkumulerteListe.add(forrigePeriode)
                }
                akkumulerteListe
            }.sortedByDescending { it.tom }
    }

    private fun mergePerioder(
        forrigePeriode: Periode,
        gjeldendePeriode: Periode,
    ): Periode {
        val ressursId =
            if (gjeldendePeriode.overlapper(forrigePeriode)) {
                forrigePeriode.ressursId
            } else {
                gjeldendePeriode.ressursId
            }

        return gjeldendePeriode.copy(
            // Bruker tidligste fom-dato og setter redusertVentePeriode hvis en av periodene har det.
            fom = minOf(forrigePeriode.fom, gjeldendePeriode.fom),
            redusertVentePeriode = forrigePeriode.redusertVentePeriode || gjeldendePeriode.redusertVentePeriode,
            ressursId = ressursId,
        )
    }

    private fun skalMerges(
        gjeldendePeriode: Periode,
        nestePeriode: Periode,
    ): Boolean {
        val dagerMellomPeriodene = DAYS.between(gjeldendePeriode.tom, nestePeriode.fom)

        // Siden 'fom' og 'tom' i perioden er inclusive vil sammenhengende perioder returnere '1 dag'. En hel helg
        // mellom to perioder blir da 3 dager og derfor kan metoden returnerer tidlig hvis dager i mellom er > 3.
        if (dagerMellomPeriodene > 3) {
            return false
        }

        // Sjekk om alle dager mellom periodene er helgedager.
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

    private fun beregnVenteperiodeTom(
        fom: LocalDate,
        perioder: List<Periode>,
    ): LocalDate {
        val harRedusertVenteperiode = perioder.any { it.redusertVentePeriode }
        val erKoronaPeriodeMedSeksDager = perioder.any { it.erKoronaPeriodeMedSeksDager() }
        val erKoronaPeriodeMedFireDager = perioder.any { it.erKoronaPeriodeMedFireDager() }

        return when {
            harRedusertVenteperiode && erKoronaPeriodeMedSeksDager -> fom.plusDays(SEKS_DAGER - 1L)
            harRedusertVenteperiode && erKoronaPeriodeMedFireDager -> fom.plusDays(FIRE_DAGER - 1L)
            else -> fom.plusDays(SEKSTEN_DAGER - 1L)
        }
    }

    private fun Periode.erLengreEnnStandardVentetid(): Boolean = DAYS.between(this.fom, this.tom) >= SEKSTEN_DAGER

    private fun Periode.erLengreEnnKoronaVentetid(): Boolean =
        when {
            erKoronaPeriodeMedSeksDager() -> DAYS.between(this.fom, this.tom) >= SEKS_DAGER - 1L
            erKoronaPeriodeMedFireDager() -> DAYS.between(this.fom, this.tom) >= FIRE_DAGER - 1L
            else -> false
        }

    private fun Periode.erKoronaPeriodeMedSeksDager(): Boolean =
        koronaPeriodeMedSeksDager.contains(this.fom) || koronaPeriodeMedSeksDager.contains(this.tom)

    private fun Periode.erKoronaPeriodeMedFireDager(): Boolean =
        koronaPeriodeMedFireDager.contains(this.fom) || koronaPeriodeMedFireDager.contains(this.tom)

    private fun List<Periode>.erForLengeSidenForrigePeriode(index: Int): Boolean {
        val gjeldendePeriode = this[index]
        val nestePeriode = this.getOrNull(index + 1) ?: return false
        return DAYS.between(nestePeriode.tom, gjeldendePeriode.fom) > SEKSTEN_DAGER
    }

    private fun Periode.overlapper(annen: Periode): Boolean = annen.tom in this.fom..this.tom

    private data class Periode(
        val fom: LocalDate,
        val tom: LocalDate,
        val redusertVentePeriode: Boolean,
        val behandlingsdager: Boolean,
        val ressursId: String,
    )
}
