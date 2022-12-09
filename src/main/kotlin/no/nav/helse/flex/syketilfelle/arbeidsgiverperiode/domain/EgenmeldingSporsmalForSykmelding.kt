package no.nav.helse.flex.syketilfelle.arbeidsgiverperiode.domain

import java.time.LocalDate

data class Sporsmal(
    val fom: LocalDate,
    val tom: LocalDate
)
data class EgenmeldingSporsmalForSykmelding(
    val sporsmal: Sporsmal?,
)
