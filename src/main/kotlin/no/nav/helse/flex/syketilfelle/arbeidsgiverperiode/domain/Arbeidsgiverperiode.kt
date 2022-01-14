package no.nav.helse.flex.syketilfelle.arbeidsgiverperiode.domain

data class Arbeidsgiverperiode(
    val antallBrukteDager: Int,
    val oppbruktArbeidsgiverperiode: Boolean,
    val arbeidsgiverPeriode: PeriodeDTO?
)
