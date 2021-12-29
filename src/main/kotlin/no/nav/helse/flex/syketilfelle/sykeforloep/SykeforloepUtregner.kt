package no.nav.helse.flex.syketilfelle.sykeforloep

import no.nav.helse.flex.syketilfelle.client.pdl.PdlClient
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
    private val pdlClient: PdlClient,
) {

    fun hentSykeforloep(fnr: String, inkluderPapirsykmelding: Boolean): List<Sykeforloep> {
        val fnrs = pdlClient.hentFolkeregisterIdenter(fnr)
        return syketilfellebitRepository
            .findByFnrIn(fnrs)
            .map { it.tilSyketilfellebit() }
            .utenKorrigerteSoknader()
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
    val sykmeldinger = this.sortedBy { it.fom }
    val alleSykeforloep = ArrayList<Sykeforloep>()

    if (sykmeldinger.isEmpty()) {
        return emptyList()
    }

    val iterator = sykmeldinger.iterator()
    val gjeldendeSykmelding = iterator.next()

    var gjeldendeSykeforloep = Sykeforloep(oppfolgingsdato = gjeldendeSykmelding.fom)
    gjeldendeSykeforloep.sykmeldinger.add(gjeldendeSykmelding)
    alleSykeforloep.add(gjeldendeSykeforloep)

    while (iterator.hasNext()) {
        val sykmelding = iterator.next()

        if (antallDagerMellom(gjeldendeSykeforloep.sisteDagIForloep(), sykmelding.fom) >= 16) {
            gjeldendeSykeforloep = Sykeforloep(oppfolgingsdato = sykmelding.fom)
            alleSykeforloep.add(gjeldendeSykeforloep)
        }
        gjeldendeSykeforloep.sykmeldinger.add(sykmelding)
    }
    alleSykeforloep.forEach {
        it.sykmeldinger.sortBy { sykmelding -> sykmelding.fom }
    }

    return alleSykeforloep
}

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
    val sykmeldinger: ArrayList<SimpleSykmelding> = ArrayList()
)

data class SimpleSykmelding(
    val id: String,
    val fom: LocalDate,
    val tom: LocalDate
)

private fun antallDagerMellom(tidligst: LocalDate, eldst: LocalDate): Int {
    return ChronoUnit.DAYS.between(tidligst, eldst).toInt() - 1
}
