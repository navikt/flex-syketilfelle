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
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit.DAYS
import java.time.temporal.TemporalAdjusters.next
import java.time.temporal.TemporalAdjusters.nextOrSame
import java.time.temporal.TemporalAdjusters.previous

const val SEKSTEN_DAGER = 16L

@Component
class VentetidUtregner(
    private val syketilfellebitRepository: SyketilfellebitRepository,
) {
    val log = logger()

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

    fun erUtenforVentetid(
        sykmeldingId: String,
        identer: List<String>,
        erUtenforVentetidRequest: ErUtenforVentetidRequest,
    ): Boolean = beregnVentetid(sykmeldingId, identer, erUtenforVentetidRequest.tilVentetidRequest()) != null

    fun beregnVentetid(
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
            log.warn("Fant ikke biter tilhørende sykmelding: $sykmeldingId ved beregning av ventetid.")
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
                .mergePerioder(sykmeldingId)
                .fjernHelgFraSluttenAvPeriodenForSykmelding(sykmeldingId)

        perioder.beregnVentetid()?.let { ventetid ->
            return FomTomPeriode(
                fom = ventetid.fom,
                tom = ventetid.fom.plusDays(SEKSTEN_DAGER - 1L),
            )
        }

        // Hvis perioden er innenfor ventetid, men 'returnerPerioderInnenforVentetid' er satt,
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

    private fun List<Periode>.beregnVentetid(): Periode? {
        // Hvis det er mindre enn 17 siden forrige periode og forrige periode var utenfor ventetid, returneres forrige
        // periodes ventetid.
        if (size >= 2) {
            val (_, forrigePeriode) = this
            if (!erForLengeSidenForrigePeriode(0) && forrigePeriode.erLengreEnnVentetid()) {
                return forrigePeriode
            }
        }
        // Går gjennom periodene og finner den første som kvalifiserer som ventetid.
        for ((index, periode) in withIndex()) {
            when {
                periode.erLengreEnnVentetid() -> return periode
                // Returnerer ikke periodre korterer en antall ventetidsdager Brukes av 'erUtenforVentetid()' til å
                // beregne om en periode er utenfor ventetid eller ikke.
                erForLengeSidenForrigePeriode(index) -> return null
            }
        }

        return null
    }

    private fun Periode.erLengreEnnVentetid(): Boolean = DAYS.between(this.fom, this.tom) >= SEKSTEN_DAGER

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
            behandlingsdager = this.tags.contains(Tag.BEHANDLINGSDAGER),
            ressursId = this.ressursId,
            erAnnetFravaer = this.tags.contains(Tag.ANNET_FRAVAR),
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

    private fun List<Periode>.mergePerioder(foretrukketRessursId: String): List<Periode> {
        if (size <= 1) return this

        // Sorter periodene med nyeste periode først for å enklere sjekke tid siden forrige periode.
        val sortertePerioder = sortedByDescending { it.tom }

        return sortertePerioder
            .drop(1)
            .fold(mutableListOf(sortertePerioder.first())) { akkumulerteListe, forrigePeriode ->
                val gjeldendePeriode = akkumulerteListe.last()

                if (skalMerges(forrigePeriode, gjeldendePeriode)) {
                    akkumulerteListe[akkumulerteListe.lastIndex] =
                        mergePerioder(forrigePeriode, gjeldendePeriode, foretrukketRessursId)
                } else {
                    akkumulerteListe.add(forrigePeriode)
                }
                akkumulerteListe
            }.sortedByDescending { it.tom }
    }

    private fun skalMerges(
        forrigePeriode: Periode,
        gjeldendePeriode: Periode,
    ): Boolean {
        val dagerMellomPeriodene = DAYS.between(forrigePeriode.tom, gjeldendePeriode.fom)

        // Siden 'fom' og 'tom' i perioden er inclusive vil sammenhengende perioder returnere '1 dag'. En hel helg
        // mellom to perioder blir da 3 dager og derfor kan metoden returnerer tidlig hvis dager i mellom er > 3.
        if (dagerMellomPeriodene > 3) {
            return false
        }

        // Sjekk om alle dager mellom periodene er helgedager.
        return (1 until dagerMellomPeriodene)
            .asSequence()
            .map { forrigePeriode.tom.plusDays(it) }
            .all { it.dayOfWeek in setOf(SATURDAY, SUNDAY) }
    }

    private fun mergePerioder(
        forrigePeriode: Periode,
        gjeldendePeriode: Periode,
        foretrukketRessursId: String,
    ): Periode {
        // Når to like perioder merges, kan rekkefølgen de kommer fra databasen være tilfeldig, så hvis én av
        // periodene har ressursId lik foretrukketRessursId (aktuell sykmelding), vil den alltid overlever
        // uavhengig av rekkefølge.
        val valgtRessursId =
            when (foretrukketRessursId) {
                forrigePeriode.ressursId, gjeldendePeriode.ressursId -> foretrukketRessursId
                else -> gjeldendePeriode.ressursId
            }

        return gjeldendePeriode.copy(
            fom = minOf(forrigePeriode.fom, gjeldendePeriode.fom),
            ressursId = valgtRessursId,
        )
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

    private fun List<Periode>.erForLengeSidenForrigePeriode(index: Int): Boolean {
        val gjeldendePeriode = this[index]
        // Egenmeldingsdager skal ikke tas med i beregningen av tid siden forrige perode.
        val nestePeriode = this.drop(index + 1).firstOrNull { !it.erAnnetFravaer } ?: return false
        return DAYS.between(nestePeriode.tom, gjeldendePeriode.fom) > SEKSTEN_DAGER
    }

    private data class Periode(
        val fom: LocalDate,
        val tom: LocalDate,
        val behandlingsdager: Boolean,
        val ressursId: String,
        val erAnnetFravaer: Boolean,
    )
}
