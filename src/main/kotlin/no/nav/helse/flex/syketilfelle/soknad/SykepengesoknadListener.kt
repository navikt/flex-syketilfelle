package no.nav.helse.flex.syketilfelle.soknad

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.sykepengesoknad.kafka.SykepengesoknadDTO
import no.nav.helse.flex.syketilfelle.objectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

const val SYKEPENGESOKNAD_TOPIC = "flex." + "sykepengesoknad"

@Component
class AivenSykepengesoknadListener(

    private val sykepengesoknadLagring: SykepengesoknadLagring,
) {

    @KafkaListener(
        topics = [SYKEPENGESOKNAD_TOPIC],
        containerFactory = "syketilfelleKafkaListenerContainerFactory"
    )
    fun listen(cr: ConsumerRecord<String, String>, acknowledgment: Acknowledgment) {
        sykepengesoknadLagring.lagreBiterFraSoknad(cr.value().tilSykepengesoknadDTO())
        acknowledgment.acknowledge()
    }

    fun String.tilSykepengesoknadDTO(): SykepengesoknadDTO = objectMapper.readValue(this)
}
