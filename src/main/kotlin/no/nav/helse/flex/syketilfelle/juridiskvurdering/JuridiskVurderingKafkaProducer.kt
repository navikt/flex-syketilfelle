package no.nav.helse.flex.syketilfelle.juridiskvurdering

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.helse.flex.syketilfelle.logger
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*
import java.util.concurrent.Future

@Component
class JuridiskVurderingKafkaProducer(
    private val producer: KafkaProducer<String, JuridiskVurderingKafkaDto>,
    @param:Value("\${nais.app.name}")
    private val naisAppName: String,
    @param:Value("\${nais.app.image}")
    private val naisAppImage: String,
) {
    val log = logger()

    // Sender meldingen asynkront og returnerer en Future slik at kalleren kan vente på bekreftelse
    // fra broker etter at alle meldinger er sendt.
    @WithSpan
    fun sendMelding(juridiskVurdering: JuridiskVurdering): Future<RecordMetadata> {
        val dto = juridiskVurdering.tilDto()
        return producer.send(ProducerRecord(juridiskVurderingTopic, dto.fodselsnummer, dto)) { _, e ->
            if (e != null) {
                log.warn(
                    "Uventet exception ved publisering av juridiskvurdering ${dto.id} på topic $juridiskVurderingTopic",
                    e,
                )
            }
        }
    }

    fun JuridiskVurdering.tilDto(): JuridiskVurderingKafkaDto =
        JuridiskVurderingKafkaDto(
            bokstav = bokstav,
            fodselsnummer = fodselsnummer,
            sporing = sporing,
            lovverk = lovverk,
            lovverksversjon = lovverksversjon,
            paragraf = paragraf,
            ledd = ledd,
            punktum = punktum,
            input = input,
            output = output,
            utfall = utfall,
            id = UUID.randomUUID().toString(),
            eventName = "subsumsjon",
            versjon = "1.0.0",
            kilde = naisAppName,
            versjonAvKode = naisAppImage,
            tidsstempel = Instant.now(),
        )
}

val juridiskVurderingTopic = "flex.omrade-helse-etterlevelse"
