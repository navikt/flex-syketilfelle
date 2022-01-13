package no.nav.helse.flex.syketilfelle.arbeidsgiverperiode

import no.nav.helse.flex.syketilfelle.arbeidsgiverperiode.domain.Arbeidsgiverperiode
import no.nav.helse.flex.syketilfelle.arbeidsgiverperiode.domain.PeriodeDTO
import no.nav.helse.flex.syketilfelle.syketilfellebit.Syketilfellebit
import no.nav.helse.flex.syketilfelle.syketilfellebit.SyketilfellebitRepository
import no.nav.helse.flex.syketilfelle.syketilfellebit.tilSyketilfellebit
import no.nav.helse.flex.syketilfelle.syketilfellebit.utenKorrigerteSoknader
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class ArbeidsgiverperiodeUtregner(
    private val syketilfellebitRepository: SyketilfellebitRepository,
) {

    fun beregnOppfolgingstilfelleForSoknadTilInnsending(
        fnrs: List<String>,
        andreKorrigerteRessurser: List<String>,
        tillegsbiter: List<Syketilfellebit>,
        grense: LocalDate,
        forsteDagISoknad: LocalDate,
        startSyketilfelle: LocalDate?
    ): Arbeidsgiverperiode? {
        val biter = finnBiter(fnrs)
        return genererOppfolgingstilfelle(
            fnrs,
            biter,
            andreKorrigerteRessurser,
            tillegsbiter,
            grense.atStartOfDay(),
            startSyketilfelle
        )
            ?.lastOrNull {
                if (it.sisteSykedagEllerFeriedag == null) {
                    return@lastOrNull it.oppbruktArbeidsgvierperiode()
                }
                it.sisteSykedagEllerFeriedag.plusDays(16).isEqualOrAfter(forsteDagISoknad)
            }
            ?.let {
                Arbeidsgiverperiode(
                    it.dagerAvArbeidsgiverperiode,
                    it.oppbruktArbeidsgvierperiode(),
                    it.arbeidsgiverperiode().let { p -> PeriodeDTO(p.first, p.second) }
                )
            }
    }

    private fun finnBiter(fnrs: List<String>) = syketilfellebitRepository
        .findByFnrIn(fnrs)
        .map { it.tilSyketilfellebit() }
        .utenKorrigerteSoknader()
}
