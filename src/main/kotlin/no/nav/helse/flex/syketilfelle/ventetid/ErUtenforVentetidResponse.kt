package no.nav.helse.flex.syketilfelle.ventetid

import java.time.LocalDate

data class ErUtenforVentetidResponse(
    val erUtenforVentetid: Boolean,
    val oppfolgingsdato: LocalDate?,
)
