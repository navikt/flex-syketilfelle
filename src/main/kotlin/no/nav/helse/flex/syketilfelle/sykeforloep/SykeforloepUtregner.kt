package no.nav.helse.flex.syketilfelle.sykeforloep

import no.nav.helse.flex.sykepengesoknad.kafka.ArbeidssituasjonDTO
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
        arbeidssituasjon: ArbeidssituasjonDTO? = null,
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
            .grupperSykmeldingerIForloep(arbeidssituasjon)
            .sortedBy { it.oppfolgingsdato }
    }
}

fun List<SimpleSykmelding>.grupperSykmeldingerIForloep(arbeidssituasjon: ArbeidssituasjonDTO? = null): List<Sykeforloep> {
    if (this.isEmpty()) return emptyList()
    val sykmeldinger = this.sortedBy { it.fom }

    val alleSykeforloep =
        mutableListOf<Sykeforloep>().apply {
            add(
                Sykeforloep(
                    oppfolgingsdato = sykmeldinger.first().fom,
                    skjaeringstidspunkt = sykmeldinger.first().fom,
                ).also { forloep ->
                    forloep.sykmeldinger.add(sykmeldinger.first())
                },
            )
        }
    var gjeldendeSykeforloep = alleSykeforloep.first()
    var dagerSykIForloep = ChronoUnit.DAYS.between(sykmeldinger.first().fom, sykmeldinger.first().tom).toInt()

    sykmeldinger.drop(1).forEach { sykmelding ->
        if (skalLageNyttSykeforloep(gjeldendeSykeforloep, sykmelding)) {
            dagerSykIForloep = 0
            gjeldendeSykeforloep = Sykeforloep(oppfolgingsdato = sykmelding.fom, skjaeringstidspunkt = sykmelding.fom)
            alleSykeforloep.add(gjeldendeSykeforloep)
        }
        gjeldendeSykeforloep.sykmeldinger.add(sykmelding)

        val (dager, skjaering) = finnSkjaeringstidspunkt(dagerSykIForloep, sykmelding, gjeldendeSykeforloep, arbeidssituasjon)
        dagerSykIForloep = dager
        gjeldendeSykeforloep.skjaeringstidspunkt = skjaering
    }

    return alleSykeforloep
}

private fun finnSkjaeringstidspunkt(
    dagerSykFremTilSykmelding: Int,
    sykmelding: SimpleSykmelding,
    gjeldendeSykeforloep: Sykeforloep,
    arbeidssituasjon: ArbeidssituasjonDTO? = null,
): Pair<Int, LocalDate?> {
    val totaleDagerSyk =
        when (arbeidssituasjon) {
            ArbeidssituasjonDTO.ARBEIDSTAKER -> {
                dagerSykFremTilSykmelding + ChronoUnit.DAYS.between(sykmelding.fom, sykmelding.tom).toInt()
            }
            ArbeidssituasjonDTO.JORDBRUKER,
            ArbeidssituasjonDTO.SELVSTENDIG_NARINGSDRIVENDE,
            -> {
                ChronoUnit.DAYS.between(sykmelding.fom, sykmelding.tom).toInt()
            }
            // TODO: legg inn egne regler for resten
            else -> {
                ChronoUnit.DAYS.between(sykmelding.fom, sykmelding.tom).toInt()
            }
        }
    val nyttSkjaeringstidspunkt =
        if (dagerSykFremTilSykmelding < 16 && totaleDagerSyk >= 16) {
            sykmelding.fom
        } else {
            gjeldendeSykeforloep.skjaeringstidspunkt
        }

    return Pair(totaleDagerSyk, nyttSkjaeringstidspunkt)
}

private fun skalLageNyttSykeforloep(
    gjeldendeSykeforloep: Sykeforloep,
    sykmelding: SimpleSykmelding,
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
    var skjaeringstidspunkt: LocalDate? = null,
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
