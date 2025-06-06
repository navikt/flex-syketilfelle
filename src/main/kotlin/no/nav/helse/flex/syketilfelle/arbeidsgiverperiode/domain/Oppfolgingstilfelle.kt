package no.nav.helse.flex.syketilfelle.arbeidsgiverperiode.domain

import no.nav.helse.flex.syketilfelle.arbeidsgiverperiode.erArbeidsdag
import no.nav.helse.flex.syketilfelle.arbeidsgiverperiode.erFeriedag
import no.nav.helse.flex.syketilfelle.arbeidsgiverperiode.erSendt
import no.nav.helse.flex.syketilfelle.syketilfellebit.Tag
import java.time.LocalDate

class Oppfolgingstilfelle(
    val tidslinje: List<Syketilfelledag>,
    val sisteDagIArbeidsgiverperiode: Syketilfelledag,
    val dagerAvArbeidsgiverperiode: Int,
    val behandlingsdager: Int,
    val sisteSykedagEllerFeriedag: LocalDate?,
) {
    fun antallDager() =
        tidslinje.count { syketilfelledag ->
            (
                syketilfelledag.prioritertSyketilfellebit
                    ?.tags
                    ?.containsAll(listOf(Tag.SYKEPENGESOKNAD, Tag.FERIE))
                    ?.not()
            )
                ?: true
        }

    fun antallSykedager() =
        tidslinje.count { syketilfelledag ->
            syketilfelledag.erSendt() && !syketilfelledag.erArbeidsdag() && !syketilfelledag.erFeriedag()
        }

    fun oppbruktArbeidsgvierperiode() = dagerAvArbeidsgiverperiode > 16 || behandlingsdager > 12

    fun arbeidsgiverperiode(): Pair<LocalDate, LocalDate> = forsteDagITilfellet() to sisteDagIArbeidsgiverperiode.dag

    private fun forsteDagITilfellet(): LocalDate = tidslinje.first().dag
}
