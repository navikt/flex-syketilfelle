package no.nav.helse.flex.syketilfelle.arbeidsgiverperiode.domain

import java.time.LocalDate

data class PeriodeDTO(
    val fom: LocalDate,
    val tom: LocalDate,
)
