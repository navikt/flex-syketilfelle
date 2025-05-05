package no.nav.helse.flex.syketilfelle.kafkaprodusering

import no.nav.helse.flex.syketilfelle.logger
import no.nav.helse.flex.syketilfelle.syketilfellebit.KafkaSyketilfellebit
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeader
import org.apache.kafka.common.header.internals.RecordHeaders
import org.apache.kafka.common.utils.Utils
import org.springframework.stereotype.Component

const val SYKETILFELLEBIT_TOPIC = "flex.syketilfellebiter"

@Component
class SyketilfellebitKafkaProducer(
    private val producer: KafkaProducer<String, KafkaSyketilfellebit?>,
) {
    val antallPartisjoner = producer.partitionsFor(SYKETILFELLEBIT_TOPIC).size

    private val log = logger()
    private val headers = RecordHeaders().also { it.add(RecordHeader("kilde", "flex-syketilfelle".toByteArray())) }

    fun kalkulerPartisjonForFnr(fnr: String): Int = kalkulerPartisjon(fnr.toByteArray(), antallPartisjoner)

    fun produserMelding(kafkaSyketilfellebit: KafkaSyketilfellebit) {
        try {
            producer
                .send(
                    ProducerRecord(
                        SYKETILFELLEBIT_TOPIC,
                        kalkulerPartisjonForFnr(kafkaSyketilfellebit.fnr),
                        kafkaSyketilfellebit.id,
                        kafkaSyketilfellebit,
                        headers,
                    ),
                ).get()
        } catch (e: Throwable) {
            log.error(
                "Feil ved sending av syketilfellebit id: ${kafkaSyketilfellebit.id} til topic: $SYKETILFELLEBIT_TOPIC.",
                e,
            )
            throw e
        }
    }

    fun produserTombstone(
        key: String,
        fnr: String,
    ) {
        try {
            producer
                .send(
                    ProducerRecord(SYKETILFELLEBIT_TOPIC, kalkulerPartisjonForFnr(fnr), key, null, headers),
                ).get()
        } catch (e: Throwable) {
            log.error(
                "Feil ved sending av tombstone for key: $key til topic: $SYKETILFELLEBIT_TOPIC.",
                e,
            )
            throw e
        }
    }
}

fun kalkulerPartisjon(
    keyBytes: ByteArray,
    antallPartisjoner: Int,
): Int = Utils.toPositive(Utils.murmur2(keyBytes)) % (antallPartisjoner)
