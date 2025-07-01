package no.nav.helse.flex.syketilfelle.ventetid

import java.time.LocalDate

data class VenteperiodeResponse(
    val venteperiode: Venteperiode?,
)

data class Venteperiode(
    val fom: LocalDate,
    val tom: LocalDate,
)
