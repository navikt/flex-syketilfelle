package no.nav.helse.flex.syketilfelle.juridiskvurdering

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant
import java.time.LocalDate

data class JuridiskVurderingKafkaDto(
    @JsonProperty("@id")
    val id: String,
    @JsonProperty("@versjon")
    val versjon: String,
    @JsonProperty("@event_name")
    val eventName: String,
    @JsonProperty("@kilde")
    val kilde: String,
    val versjonAvKode: String,
    val fødselsnummer: String,
    val organisasjonsnummer: String,
    val sporing: Map<String, String>,
    val tidsstempel: Instant,
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

enum class Utfall {
    VILKAR_OPPFYLT,
    VILKAR_IKKE_OPPFYLT,
    VILKAR_UAVKLART,
    VILKAR_BEREGNET
}
