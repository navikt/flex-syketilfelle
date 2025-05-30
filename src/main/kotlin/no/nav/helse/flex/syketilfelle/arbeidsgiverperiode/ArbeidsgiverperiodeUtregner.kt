package no.nav.helse.flex.syketilfelle.arbeidsgiverperiode

import no.nav.helse.flex.sykepengesoknad.kafka.SykepengesoknadDTO
import no.nav.helse.flex.syketilfelle.arbeidsgiverperiode.domain.Arbeidsgiverperiode
import no.nav.helse.flex.syketilfelle.arbeidsgiverperiode.domain.PeriodeDTO
import no.nav.helse.flex.syketilfelle.juridiskvurdering.JuridiskVurdering
import no.nav.helse.flex.syketilfelle.juridiskvurdering.JuridiskVurderingKafkaProducer
import no.nav.helse.flex.syketilfelle.juridiskvurdering.SporingType
import no.nav.helse.flex.syketilfelle.juridiskvurdering.SporingType.ORGANISASJONSNUMMER
import no.nav.helse.flex.syketilfelle.juridiskvurdering.SporingType.SYKMELDING
import no.nav.helse.flex.syketilfelle.juridiskvurdering.Utfall
import no.nav.helse.flex.syketilfelle.soknad.mapSoknadTilBiter
import no.nav.helse.flex.syketilfelle.syketilfellebit.SyketilfellebitRepository
import no.nav.helse.flex.syketilfelle.syketilfellebit.tilSyketilfellebit
import no.nav.helse.flex.syketilfelle.syketilfellebit.utenKorrigerteSoknader
import no.nav.helse.flex.syketilfelle.sykmelding.domain.SykmeldingKafkaMessage
import no.nav.helse.flex.syketilfelle.sykmelding.mapTilBiter
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class ArbeidsgiverperiodeUtregner(
    private val syketilfellebitRepository: SyketilfellebitRepository,
    private val juridiskVurderingKafkaProducer: JuridiskVurderingKafkaProducer,
) {
    fun beregnArbeidsgiverperiode(
        fnrs: List<String>,
        andreKorrigerteRessurser: List<String>,
        soknad: SykepengesoknadDTO,
        forelopig: Boolean,
        sykmelding: SykmeldingKafkaMessage?,
    ): Arbeidsgiverperiode? {
        val biter =
            finnBiter(fnrs)
                .filter { it.orgnummer == soknad.arbeidsgiver?.orgnummer }
                .filterNot { it.ressursId == sykmelding?.sykmelding?.id }
                .toMutableList()
                .also { it.addAll(sykmelding?.mapTilBiter() ?: emptyList()) }

        return genererOppfolgingstilfelle(
            fnrs = fnrs,
            biter = biter,
            andreKorrigerteRessurser = andreKorrigerteRessurser,
            tilleggsbiter = soknad.mapSoknadTilBiter(),
            grense = soknad.tom!!.atStartOfDay(),
            startSyketilfelle = soknad.startSyketilfelle,
        )?.lastOrNull {
            if (it.sisteSykedagEllerFeriedag == null) {
                return@lastOrNull it.oppbruktArbeidsgvierperiode()
            }
            it.sisteSykedagEllerFeriedag.plusDays(16).isEqualOrAfter(soknad.forsteDagISoknad())
        }?.let {
            Arbeidsgiverperiode(
                it.dagerAvArbeidsgiverperiode,
                it.oppbruktArbeidsgvierperiode(),
                it.arbeidsgiverperiode().let { p -> PeriodeDTO(p.first, p.second) },
            )
        }?.also { arbeidsgiverperiode ->
            if (!forelopig) {
                listOf(
                    skapJuridiskVurdering(
                        fnr = fnrs.first(),
                        soknad = soknad,
                        arbeidsgiverperiode = arbeidsgiverperiode,
                        ledd = 2,
                    ),
                    skapJuridiskVurdering(
                        fnr = fnrs.first(),
                        soknad = soknad,
                        arbeidsgiverperiode = arbeidsgiverperiode,
                        ledd = 3,
                    ),
                    skapJuridiskVurdering(
                        fnr = fnrs.first(),
                        soknad = soknad,
                        arbeidsgiverperiode = arbeidsgiverperiode,
                        ledd = 4,
                    ),
                ).forEach {
                    juridiskVurderingKafkaProducer.produserMelding(
                        it,
                    )
                }
            }
        }
    }

    private fun skapJuridiskVurdering(
        fnr: String,
        soknad: SykepengesoknadDTO,
        arbeidsgiverperiode: Arbeidsgiverperiode,
        ledd: Int,
    ): JuridiskVurdering =
        JuridiskVurdering(
            fodselsnummer = fnr,
            sporing =
                hashMapOf(SporingType.SOKNAD to listOf(soknad.id))
                    .also { map ->
                        soknad.sykmeldingId?.let {
                            map[SYKMELDING] = listOf(it)
                        }
                        soknad.arbeidsgiver?.orgnummer?.let {
                            map[ORGANISASJONSNUMMER] = listOf(it)
                        }
                    },
            input =
                mapOf(
                    "soknad" to soknad.id,
                    "versjon" to LocalDate.of(2022, 2, 1),
                ),
            output =
                hashMapOf(
                    "versjon" to LocalDate.of(2022, 2, 1),
                    "arbeidsgiverperiode" to arbeidsgiverperiode.arbeidsgiverPeriode,
                    "oppbruktArbeidsgiverperiode" to arbeidsgiverperiode.oppbruktArbeidsgiverperiode,
                ),
            lovverk = "folketrygdloven",
            paragraf = "8-19",
            bokstav = null,
            ledd = ledd,
            punktum = null,
            lovverksversjon = LocalDate.of(2001, 1, 1),
            utfall = Utfall.VILKAR_BEREGNET,
        )

    private fun SykepengesoknadDTO.forsteDagISoknad(): LocalDate =
        egenmeldinger?.mapNotNull { it.fom }?.minOrNull()
            ?: fravarForSykmeldingen?.mapNotNull { it.fom }?.minOrNull()
            ?: fom!!

    private fun finnBiter(fnrs: List<String>) =
        syketilfellebitRepository
            .findByFnrIn(fnrs)
            .map { it.tilSyketilfellebit() }
            .filter { it.slettet == null }
            .utenKorrigerteSoknader()
}
