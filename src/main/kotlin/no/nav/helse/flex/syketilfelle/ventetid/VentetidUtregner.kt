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
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit.DAYS
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
                // Sykmelding med Periodetype.REISETILSKUDD får UKJENT_AKTIVITET.
                Tag.UKJENT_AKTIVITET,
            )

        private val EKSKLUDERTE_TAGS =
            setOf(
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

    fun finnPerioderMedSammeVentetid(
        sykmeldingId: String,
        identer: List<String>,
        sammeVentetidRequest: SammeVentetidRequest,
    ): List<SammeVentetidPeriode> {
        val ventetidRequest = sammeVentetidRequest.tilVentetidRequest(returnerPerioderInnenforVentetid = true)

        // Sender med VentetidRequest i tilfelle Kafka-meldingen ikke er lagret enda.
        val sykmeldingVentetid =
            beregnVentetid(
                sykmeldingId = sykmeldingId,
                identer = identer,
                ventetidRequest = ventetidRequest,
            ) ?: return emptyList()

        val biterFraRequest = sammeVentetidRequest.sykmeldingKafkaMessage?.mapTilBiter() ?: emptyList()

        val kandidatRessurser =
            (
                syketilfellebitRepository
                    .findByFnrIn(identer)
                    .filter { it.slettet == null }
                    .map { it.tilSyketilfellebit() }
                    .utenKorrigerteSoknader() + biterFraRequest
            ).asSequence()
                .filter { it.tags.contains(Tag.SYKMELDING) }
                .filter { bit -> bit.tags.any { tag -> tag in AKTIVITET_TAGS } }
                .filterNot { bit -> bit.tags.any { it in EKSKLUDERTE_TAGS } }
                .run { if (ventetidRequest.kunSendtBekreftet) filterNot { it.tags.contains(Tag.NY) } else this }
                .map { it.ressursId }
                .distinct()
                .toList()

        return kandidatRessurser.mapNotNull {
            val ventetid =
                beregnVentetid(
                    sykmeldingId = it,
                    identer = identer,
                    // Sender med samme VentetidRequest fra request på hvert kall for sikre riktig beregning av
                    // ventetid selv om Kafka-meldingen ikke er lagret enda.
                    ventetidRequest = ventetidRequest,
                ) ?: return@mapNotNull null
            if (ventetid.fom == sykmeldingVentetid.fom) {
                SammeVentetidPeriode(ressursId = it, ventetid = ventetid)
            } else {
                null
            }
        }
    }

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
            log.error("Fant ikke biter tilhørende sykmelding: $sykmeldingId ved beregning av ventetid.")
            return null
        }

        val sykmeldingSenesteTom = aktuellSykmeldingBiter.maxOf { it.tom }

        val perioder =
            sykmeldingBiter
                .asSequence()
                .filter { it.tags.contains(Tag.SYKMELDING) }
                .filter { bit -> bit.tags.any { tag -> tag in AKTIVITET_TAGS } }
                .filterNot { bit -> bit.tags.any { it in EKSKLUDERTE_TAGS } }
                .map { it.tilPeriode() }
                .filter { it.fom <= sykmeldingSenesteTom }
                .map { it.kuttBitSomErLengreEnnAktuellTom(sykmeldingSenesteTom) }
                .toList()
                .mergePerioder(sykmeldingId)
                .fjernHelgFraSluttenAvPerioden(sykmeldingId)

        perioder.beregnVentetid()?.let { ventetid ->
            return FomTomPeriode(
                fom = ventetid.fom,
                tom = ventetid.fom.plusDays(SEKSTEN_DAGER - 1L),
            )
        }

        // Hvis 'returnerPerioderInnenforVentetid' er true, returneres hele perioden selv om den er innfor ventetiden.
        if (ventetidRequest.returnerPerioderInnenforVentetid) {
            perioder
                .asSequence()
                .filter { it.ressursId == sykmeldingId }
                .maxByOrNull { it.tom }
                ?.let { return FomTomPeriode(it.fom, it.tom) }

            val harEkskluderteTags = aktuellSykmeldingBiter.any { bit -> bit.tags.any { it in EKSKLUDERTE_TAGS } }
            if (!harEkskluderteTags) {
                log.error("Klarte ikke å kalkulere ventetid for sykmelding: $sykmeldingId.")
            }
        }

        return null
    }

    private fun List<Periode>.beregnVentetid(): Periode? {
        // Hvis det er mindre enn 17 siden forrige periode, og forrige periode var utenfor ventetiden, returneres
        // forrige periodes ventetid.
        if (size >= 2) {
            val (_, forrigePeriode) = this
            if (!erForLengeSidenForrigePeriode(0) && forrigePeriode.erLengreEnnVentetiden()) {
                return forrigePeriode
            }
        }
        // Går gjennom periodene og finner den første som kvalifiserer som ventetidsperiode.
        for ((index, periode) in withIndex()) {
            when {
                periode.erLengreEnnVentetiden() -> return periode
                // Returnerer ikke periodre korterer en antall ventetidsdager.
                erForLengeSidenForrigePeriode(index) -> return null
            }
        }

        return null
    }

    private fun Periode.erLengreEnnVentetiden(): Boolean = DAYS.between(this.fom, this.tom) >= SEKSTEN_DAGER

    // Kombinerer eksisterende syketilfellebiter med nye biter fra sykmeldingen og eventuelle tilleggsopplysninger (som
    // egenmeldinger) fra forespørselen. Det kan resulterer i duplikate biter hvis den aktuelle sykmeldignen både er
    // lagret i databasen og sendt med i sykmeldingKafkaMessage. Duplikate biter blir slått sammen i mergePerioder().
    private fun lagSykmeldingBiter(
        eksisterendeBiter: List<Syketilfellebit>,
        sykmeldingId: String,
        identer: List<String>,
        ventetidRequest: VentetidRequest,
    ): List<Syketilfellebit> =
        eksisterendeBiter
            .toMutableList()
            .apply {
                ventetidRequest.sykmeldingKafkaMessage?.let { sykmeldingMessage ->
                    addAll(sykmeldingMessage.mapTilBiter())
                }
                ventetidRequest.tilleggsopplysninger?.let { tilleggsopplysninger ->
                    addAll(tilleggsopplysninger.mapTilBiter(sykmeldingId, identer.first()))
                }
            }.let { biter ->
                if (ventetidRequest.kunSendtBekreftet) biter.filterNot { it.tags.contains(Tag.NY) } else biter
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

    private fun List<Periode>.mergePerioder(foretrukketRessursId: String): List<Periode> {
        if (size <= 1) return this

        // Sorter periodene med sist tom først for å enklere sjekke tid siden forrige periode.
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

        // Siden 'fom' og 'tom' i perioden er inklusiv, vil sammenhengende perioder returnere '1 dag'. En hel helg
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
        // Når to like perioder merges, kan rekkefølgen fra databasen være tilfeldig, så hvis én av
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

    private fun List<Periode>.fjernHelgFraSluttenAvPerioden(sykmeldingId: String): List<Periode> =
        map { periode ->
            when {
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
        // Egenmeldingsdager tas ikke med i beregningen av tid siden forrige perode.
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
