package no.nav.helse.flex.syketilfelle.arbeidsgiverperiode.domain

data class Arbeidsgiverperiode(
    val antallBrukteDager: Int,
    val oppbruktArbeidsgvierperiode: Boolean,
    val arbeidsgiverperiode: PeriodeDTO?
)
