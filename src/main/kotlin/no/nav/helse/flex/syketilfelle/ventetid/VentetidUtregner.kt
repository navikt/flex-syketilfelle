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
import java.util.ArrayList

@Component
class VentetidUtregner(
    private val syketilfellebitRepository: SyketilfellebitRepository,
) {
    val log = logger()
    final val koronaPeriodeMedFireDager =
        LocalDate.of(2020, Month.MARCH, 16).rangeTo(LocalDate.of(2021, Month.SEPTEMBER, 30))
    final val koronaPeriodeMedSeksDager =
        LocalDate.of(2021, Month.DECEMBER, 6).rangeTo(LocalDate.of(2022, Month.JUNE, 30))

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
        val syketilfellebiter =
            biter
                .toMutableList()
                .also {
                    with(erUtenforVentetidRequest) {
                        sykmeldingKafkaMessage?.let { sm ->
                            it.addAll(sm.mapTilBiter())
                        }
                        tilleggsopplysninger?.let { to ->
                            it.addAll(to.mapTilBiter(sykmeldingId, fnrs.first()))
                        }
                    }
                }.toList()

        val aktuellSykmeldingBiter = syketilfellebiter.filter { it.ressursId == sykmeldingId }

        if (aktuellSykmeldingBiter.isEmpty()) {
            log.error("Fant ikke biter til sykmelding $sykmeldingId i flex-syketilfelledatabasen.")
            return false
        }

        val sykmeldingSisteTom = aktuellSykmeldingBiter.maxOf { it.tom }

        fun Periode.kuttBitSomErLengreEnnAktuellTom(): Periode =
            if (this.tom.isAfter(sykmeldingSisteTom)) {
                this.copy(tom = sykmeldingSisteTom)
            } else {
                this
            }

        return syketilfellebiter
            .asSequence()
            .filter { it.tags.contains(Tag.SYKMELDING) }
            .filter {
                it.tags.any { t ->
                    setOf(
                        Tag.GRADERT_AKTIVITET,
                        Tag.INGEN_AKTIVITET,
                        Tag.BEHANDLINGSDAGER,
                        Tag.ANNET_FRAVAR,
                    ).contains(t)
                }
            }.filterNot { it.tags.contains(Tag.REISETILSKUDD) }
            .filterNot { it.tags.contains(Tag.AVVENTENDE) }
            .map { it.tilPeriode() }
            .map { it.splittPeriodeMedBehandlingsdagerIPerioderForHverMandag() }
            .flatten()
            .filter { it.fom.isBeforeOrEqual(sykmeldingSisteTom) }
            .map { it.kuttBitSomErLengreEnnAktuellTom() }
            .toList()
            .mergePerioder()
            .fjernHelgFraSluttenAvPeriodenForSykmelding(sykmeldingId)
            .erUtenforVentetid()
    }

    private fun Syketilfellebit.tilPeriode(): Periode =
        Periode(
            tom = this.tom,
            fom = this.fom,
            redusertVentePeriode = this.tags.contains(Tag.REDUSERT_ARBEIDSGIVERPERIODE),
            behandlingsdager = this.tags.contains(Tag.BEHANDLINGSDAGER),
            ressursId = this.ressursId,
        )

    private data class Periode(
        val fom: LocalDate,
        val tom: LocalDate,
        val redusertVentePeriode: Boolean,
        val behandlingsdager: Boolean,
        val ressursId: String,
    )

    private fun Periode.splittPeriodeMedBehandlingsdagerIPerioderForHverMandag(): List<Periode> {
        if (!this.behandlingsdager) {
            return listOf(this)
        }
        val behandlingsMandager = ArrayList<Periode>()
        var mandag = this.fom.with(nextOrSame(MONDAY))
        while (!mandag.isAfter(this.tom)) {
            behandlingsMandager.add(this.copy(fom = mandag, tom = mandag))
            mandag = mandag.with(next(MONDAY))
        }
        return behandlingsMandager
    }

    private fun List<Periode>.mergePerioder(): List<Periode> {
        val perioderMedSenesteTomForst = this.sortedByDescending { it.tom }
        if (this.size <= 1) {
            return perioderMedSenesteTomForst
        }
        val mergedePerioder = ArrayList<Periode>()
        var periode = perioderMedSenesteTomForst[0]
        for (i in 1 until perioderMedSenesteTomForst.size) {
            val tidligerePeriode = perioderMedSenesteTomForst[i]
            if (skalMerges(tidligerePeriode, periode)) {
                if (tidligerePeriode.redusertVentePeriode && !periode.redusertVentePeriode) {
                    periode = periode.copy(redusertVentePeriode = true)
                }
                if (tidligerePeriode.fom.isBefore(periode.fom)) {
                    periode = periode.copy(fom = tidligerePeriode.fom)
                }
            } else {
                mergedePerioder.add(periode)
                periode = tidligerePeriode
            }
        }
        mergedePerioder.add(periode)
        return mergedePerioder.sortedByDescending { it.tom }
    }

    private fun skalMerges(
        tidligstePeriode: Periode,
        senestePeriode: Periode,
    ): Boolean {
        val dagerMellomPeriodene = DAYS.between(tidligstePeriode.tom, senestePeriode.fom)
        if (dagerMellomPeriodene > 3) {
            return false
        }
        for (i in 1 until dagerMellomPeriodene) {
            val dagMellomPeriodene = tidligstePeriode.tom.plusDays(i)
            if (dagMellomPeriodene.dayOfWeek != SATURDAY && dagMellomPeriodene.dayOfWeek != SUNDAY) {
                return false
            }
        }
        return true
    }

    private fun List<Periode>.fjernHelgFraSluttenAvPeriodenForSykmelding(sykmeldingId: String): List<Periode> =
        this.map { periode ->
            val sisteDagIPerioden = periode.tom.getDayOfWeek()
            if (periode.ressursId == sykmeldingId &&
                (sisteDagIPerioden == SATURDAY || sisteDagIPerioden == SUNDAY) &&
                !periode.tom
                    .with(
                        previous(FRIDAY),
                    ).isBefore(periode.fom)
            ) {
                periode.copy(tom = periode.tom.with(previous(FRIDAY)))
            } else {
                periode
            }
        }

    private fun List<Periode>.erUtenforVentetid(): Boolean {
        var index = 0
        while (index < this.size) {
            val periode = this[index]
            if (DAYS.between(periode.fom, periode.tom) >= 16) {
                return true
            }
            if (periode.redusertVentePeriode) {
                if (periode in koronaPeriodeMedSeksDager &&
                    DAYS.between(
                        periode.fom,
                        periode.tom,
                    ) >= 5
                ) { // fra og med 6 dag
                    return true
                }
                if (periode in koronaPeriodeMedFireDager &&
                    DAYS.between(
                        periode.fom,
                        periode.tom,
                    ) >= 3
                ) { // fra og med 4 dag
                    return true
                }
            }
            if (index + 1 < this.size && DAYS.between(this[index + 1].tom, periode.fom) > 16) {
                break
            }
            index++
        }
        return false
    }

    private operator fun ClosedRange<LocalDate>.contains(periode: Periode): Boolean = contains(periode.fom) || contains(periode.tom)
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
            tags = setOf(Tag.SYKMELDING, Tag.BEKREFTET, Tag.ANNET_FRAVAR),
            ressursId = ressursId,
            fnr = fnr,
            orgnummer = null,
        )
    } ?: emptyList()

private fun LocalDate.isBeforeOrEqual(other: LocalDate): Boolean = this == other || this.isBefore(other)
