package no.nav.helse.flex.syketilfelle.juridiskvurdering

import java.time.LocalDate

data class JuridiskVurdering(
    val fødselsnummer: String,
    val organisasjonsnummer: String,
    val sporing: Map<String, String>,
    val lovverk: String,
    val lovverksversjon: LocalDate,
    val paragraf: String,
    val ledd: Int?,
    val punktum: Int?,
    val bokstav: String?,
    val input: Map<String, Any>,
    val output: Map<String, Any>?,
    val utfall: Utfall,
)
