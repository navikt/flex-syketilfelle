package no.nav.helse.flex.syketilfelle.sykeforloep

import no.nav.helse.flex.syketilfelle.syketilfellebit.Syketilfellebit
import no.nav.helse.flex.syketilfelle.syketilfellebit.SyketilfellebitRepository
import no.nav.helse.flex.syketilfelle.syketilfellebit.Tag
import no.nav.helse.flex.syketilfelle.syketilfellebit.tilSyketilfellebit
import no.nav.helse.flex.syketilfelle.syketilfellebit.utenKorrigerteSoknader
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.ArrayList

@Component
class SykeforloepUtregner(
    private val syketilfellebitRepository: SyketilfellebitRepository,
) {
    fun hentSykeforloep(
        fnrs: List<String>,
        inkluderPapirsykmelding: Boolean,
        syketilfellebiter: List<Syketilfellebit>? = null,
    ): List<Sykeforloep> {
        return syketilfellebitRepository
            .findByFnrIn(fnrs)
            .map { it.tilSyketilfellebit() }
            .utenKorrigerteSoknader()
            .toMutableList()
            .also {
                it.addAll(syketilfellebiter ?: emptyList())
            }
            .filter { it.slettet == null }
            .filter {
                if (inkluderPapirsykmelding) {
                    return@filter it.tags.contains(Tag.SYKMELDING) || it.tags.contains(Tag.PAPIRSYKMELDING)
                }
                return@filter it.tags.contains(Tag.SYKMELDING)
            }
            .groupBy { it.ressursId }
            .map { SimpleSykmelding(id = it.key, tom = it.value.senesteTom(), fom = it.value.firstFom()) }
            .grupperSykmeldingerIForloep()
            .sortedBy { it.oppfolgingsdato }
    }
}

fun List<SimpleSykmelding>.grupperSykmeldingerIForloep(): List<Sykeforloep> {
    if (this.isEmpty()) return emptyList()
    val sykmeldinger = this.sortedBy { it.fom }

    val alleSykeforloep =
        mutableListOf<Sykeforloep>().apply {
            add(
                Sykeforloep(oppfolgingsdato = sykmeldinger.first().fom).also { forloep ->
                    forloep.sykmeldinger.add(sykmeldinger.first())
                },
            )
        }
    var gjeldendeSykeforloep = alleSykeforloep.first()

    sykmeldinger.drop(1).forEach { sykmelding ->
        if (skalLageNyttSykeforloep(gjeldendeSykeforloep, sykmelding)) {
            gjeldendeSykeforloep = Sykeforloep(oppfolgingsdato = sykmelding.fom)
            alleSykeforloep.add(gjeldendeSykeforloep)
        }
        gjeldendeSykeforloep.sykmeldinger.add(sykmelding)
    }

    return alleSykeforloep
}

private fun skalLageNyttSykeforloep(
    gjeldendeSykeforloep: Sykeforloep,
    sykmelding: SimpleSykmelding
) = antallDagerMellom(gjeldendeSykeforloep.sisteDagIForloep(), sykmelding.fom) >= 16

fun List<Syketilfellebit>.senesteTom(): LocalDate {
    return this.maxOf { it.tom }
}

fun List<Syketilfellebit>.firstFom(): LocalDate {
    return this.minOf { it.fom }
}

fun Sykeforloep.sisteDagIForloep(): LocalDate {
    return this.sykmeldinger.maxOf { it.tom }
}

data class Sykeforloep(
    var oppfolgingsdato: LocalDate,
    val sykmeldinger: ArrayList<SimpleSykmelding> = ArrayList(),
)

data class SimpleSykmelding(
    val id: String,
    val fom: LocalDate,
    val tom: LocalDate,
)

private fun antallDagerMellom(
    tidligst: LocalDate,
    eldst: LocalDate,
): Int {
    return ChronoUnit.DAYS.between(tidligst, eldst).toInt() - 1
}
