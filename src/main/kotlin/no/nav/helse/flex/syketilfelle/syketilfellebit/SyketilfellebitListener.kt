package no.nav.helse.flex.syketilfelle.syketilfellebit

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.syketilfelle.objectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.header.Headers
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets

@Component
class SyketilfellebitListener(val syketilfellebitMottak: SyketilfellebitMottak) {

    @KafkaListener(
        topics = [SYKETILFELLEBIT_TOPIC],
        containerFactory = "aivenKafkaListenerContainerFactory",
    )
    fun listen(cr: ConsumerRecord<String, String>, acknowledgment: Acknowledgment) {
        val fraFlexSyketilfelle = cr.headers().getLastHeaderByKeyAsString("kilde") == "flex-syketilfelle"
        if (!fraFlexSyketilfelle) {
            syketilfellebitMottak.mottaBitListe(listOf(cr.value().tilKafkaSyketilfellebit()))
        }

        acknowledgment.acknowledge()
    }

    fun Headers?.getLastHeaderByKeyAsString(key: String): String? =
        this?.lastHeader(key)
            ?.value()
            ?.let { String(it, StandardCharsets.UTF_8) }
}
fun String.tilKafkaSyketilfellebit(): KafkaSyketilfellebit = objectMapper.readValue(this)

const val SYKETILFELLEBIT_TOPIC = "flex.syketilfellebiter"
