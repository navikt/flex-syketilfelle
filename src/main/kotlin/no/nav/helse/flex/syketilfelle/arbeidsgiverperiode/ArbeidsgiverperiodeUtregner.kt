package no.nav.helse.flex.syketilfelle.arbeidsgiverperiode

import no.nav.helse.flex.syketilfelle.arbeidsgiverperiode.domain.Arbeidsgiverperiode
import no.nav.helse.flex.syketilfelle.arbeidsgiverperiode.domain.PeriodeDTO
import no.nav.helse.flex.syketilfelle.juridiskvurdering.JuridiskVurdering
import no.nav.helse.flex.syketilfelle.juridiskvurdering.JuridiskVurderingKafkaProducer
import no.nav.helse.flex.syketilfelle.juridiskvurdering.SporingType
import no.nav.helse.flex.syketilfelle.juridiskvurdering.SporingType.organisasjonsnummer
import no.nav.helse.flex.syketilfelle.juridiskvurdering.SporingType.sykmelding
import no.nav.helse.flex.syketilfelle.juridiskvurdering.Utfall
import no.nav.helse.flex.syketilfelle.soknad.mapSoknadTilBiter
import no.nav.helse.flex.syketilfelle.syketilfellebit.SyketilfellebitRepository
import no.nav.helse.flex.syketilfelle.syketilfellebit.tilSyketilfellebit
import no.nav.helse.flex.syketilfelle.syketilfellebit.utenKorrigerteSoknader
import no.nav.syfo.kafka.felles.SykepengesoknadDTO
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
        forelopig: Boolean
    ): Arbeidsgiverperiode? {

        return genererOppfolgingstilfelle(
            fnrs = fnrs,
            biter = finnBiter(fnrs),
            andreKorrigerteRessurser = andreKorrigerteRessurser,
            tilleggsbiter = soknad.mapSoknadTilBiter(),
            grense = soknad.tom!!.atStartOfDay(),
            startSyketilfelle = soknad.startSyketilfelle
        )
            ?.lastOrNull {
                if (it.sisteSykedagEllerFeriedag == null) {
                    return@lastOrNull it.oppbruktArbeidsgvierperiode()
                }
                it.sisteSykedagEllerFeriedag.plusDays(16).isEqualOrAfter(soknad.forsteDagISoknad())
            }
            ?.let {
                Arbeidsgiverperiode(
                    it.dagerAvArbeidsgiverperiode,
                    it.oppbruktArbeidsgvierperiode(),
                    it.arbeidsgiverperiode().let { p -> PeriodeDTO(p.first, p.second) }
                )
            }?.also { arbeidsgiverperiode ->
                if (!forelopig) {
                    juridiskVurderingKafkaProducer.produserMelding(
                        JuridiskVurdering(
                            fodselsnummer = fnrs.first(),
                            sporing = hashMapOf(soknad.id to SporingType.soknad)
                                .also { map ->
                                    soknad.sykmeldingId?.let {
                                        map[it] = sykmelding
                                    }
                                    soknad.arbeidsgiver?.orgnummer?.let {
                                        map[it] = organisasjonsnummer
                                    }
                                },
                            input = mapOf(
                                "soknad" to soknad.id,
                                "versjon" to LocalDate.of(2022, 2, 1),
                            ),
                            output = hashMapOf(
                                "versjon" to LocalDate.of(2022, 2, 1),
                                "arbeidsgiverperiode" to arbeidsgiverperiode.arbeidsgiverPeriode,
                                "oppbruktArbeidsgiverperiode" to arbeidsgiverperiode.oppbruktArbeidsgiverperiode,
                            ),
                            lovverk = "folketrygdloven",
                            paragraf = "§8-19",
                            bokstav = null,
                            ledd = null,
                            punktum = null,
                            lovverksversjon = LocalDate.of(2001, 1, 1),
                            utfall = Utfall.VILKAR_BEREGNET,
                        )
                    )
                }
            }
    }

    private fun SykepengesoknadDTO.forsteDagISoknad(): LocalDate {
        return egenmeldinger?.mapNotNull { it.fom }?.minOrNull()
            ?: fravarForSykmeldingen?.mapNotNull { it.fom }?.minOrNull()
            ?: fom!!
    }

    private fun finnBiter(fnrs: List<String>) = syketilfellebitRepository
        .findByFnrIn(fnrs)
        .map { it.tilSyketilfellebit() }
        .utenKorrigerteSoknader()
}
