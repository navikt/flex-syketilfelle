package no.nav.helse.flex.syketilfelle.kafkaprodusering

import no.nav.helse.flex.syketilfelle.logger
import no.nav.helse.flex.syketilfelle.syketilfellebit.KafkaSyketilfellebit
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeader
import org.apache.kafka.common.header.internals.RecordHeaders
import org.springframework.stereotype.Component

const val SYKETILFELLEBIT_TOPIC = "flex.syketilfellebiter"

@Component
class SyketilfellebitKafkaProducer(
    private val producer: KafkaProducer<String, KafkaSyketilfellebit>
) {

    private val log = logger()
    private val headers = RecordHeaders().also { it.add(RecordHeader("kilde", "flex-syketilfelle".toByteArray())) }

    fun produserMelding(kafkaSyketilfellebit: KafkaSyketilfellebit) {
        try {
            producer.send(
                ProducerRecord(SYKETILFELLEBIT_TOPIC, null, kafkaSyketilfellebit.fnr, kafkaSyketilfellebit, headers),
            ).get()
        } catch (e: Throwable) {
            log.error(
                "Feil ved sending av syketilfellebit id: ${kafkaSyketilfellebit.id} til topic: $SYKETILFELLEBIT_TOPIC.",
                e
            )
            throw e
        }
    }
}
