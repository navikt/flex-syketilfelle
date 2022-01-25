package no.nav.helse.flex.syketilfelle.juridiskvurdering

import no.nav.helse.flex.syketilfelle.logger
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*

@Component
class JuridiskVurderingKafkaProducer(
    private val producer: KafkaProducer<String, JuridiskVurderingKafkaDto>,
    @Value("\${nais.app.name}")
    private val naisAppName: String,
    @Value("\${nais.app.image}")
    private val naisAppImage: String,
) {
    val log = logger()

    fun produserMelding(juridiskVurdering: JuridiskVurdering): RecordMetadata {
        val dto = juridiskVurdering.tilDto()
        try {
            return producer.send(
                ProducerRecord(
                    juridiskVurderingTopic,
                    dto.id,
                    dto
                )
            ).get()
        } catch (e: Throwable) {
            log.warn(
                "Uventet exception ved publisering av juridiskvurdering ${dto.id} på topic $juridiskVurderingTopic",
                e
            )
            throw e
        }
    }

    fun JuridiskVurdering.tilDto(): JuridiskVurderingKafkaDto = JuridiskVurderingKafkaDto(
        bokstav = bokstav,
        fødselsnummer = fødselsnummer,
        organisasjonsnummer = organisasjonsnummer,
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

val juridiskVurderingTopic = "flex.juridisk-vurdering-test"
