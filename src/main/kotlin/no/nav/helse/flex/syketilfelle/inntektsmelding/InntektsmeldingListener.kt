package no.nav.helse.flex.syketilfelle.inntektsmelding

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.syketilfelle.objectMapper
import no.nav.inntektsmeldingkontrakt.Inntektsmelding
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

const val INNTEKTSMELDING_TOPIC = "helsearbeidsgiver." + "privat-sykepenger-inntektsmelding"

@Component
class InntektsmeldingListener(
    private val inntektsmeldingLagring: InntektsmeldingLagring
) {

    @KafkaListener(
        topics = [INNTEKTSMELDING_TOPIC],
        id = "inntektsmelding-mottatt",
        idIsGroup = false,
        properties = ["auto.offset.reset = earliest"],
        containerFactory = "aivenKafkaListenerContainerFactory"
    )
    fun listen(cr: ConsumerRecord<String, String>, acknowledgment: Acknowledgment) {
        inntektsmeldingLagring.mottaInntektsmelding(cr.value().tilInntektsmelding())
        acknowledgment.acknowledge()
    }

    fun String.tilInntektsmelding(): Inntektsmelding = objectMapper.readValue(this)
}
