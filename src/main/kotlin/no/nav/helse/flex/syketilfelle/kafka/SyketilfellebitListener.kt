package no.nav.helse.flex.syketilfelle.kafka

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.syketilfelle.objectMapper
import no.nav.helse.flex.syketilfelle.syketilfellebit.SyketilfellebitMottak
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class SyketilfellebitListener(val syketilfellebitMottak: SyketilfellebitMottak) {

    @KafkaListener(
        topics = [SYKETILFELLEBIT_TOPIC],
        containerFactory = "aivenKafkaListenerContainerFactory",
        batch = "true"
    )
    fun listen(records: List<ConsumerRecord<String, String>>, acknowledgment: Acknowledgment) {
        syketilfellebitMottak.mottaBitListe(records.map { it.value().tilKafkaSyketilfellebit() })
        acknowledgment.acknowledge()
    }

    fun String.tilKafkaSyketilfellebit(): KafkaSyketilfellebit = objectMapper.readValue(this)
}

const val SYKETILFELLEBIT_TOPIC = "flex.syketilfellebiter"
