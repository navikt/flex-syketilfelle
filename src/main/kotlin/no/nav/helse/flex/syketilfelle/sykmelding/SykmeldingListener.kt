package no.nav.helse.flex.syketilfelle.sykmelding

import no.nav.helse.flex.syketilfelle.extensions.osloZone
import no.nav.helse.flex.syketilfelle.logger
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.TopicPartition
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.listener.ConsumerSeekAware
import org.springframework.kafka.listener.ConsumerSeekAware.ConsumerSeekCallback
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.OffsetDateTime

@Component
class SykmeldingListener(
    private val sykmeldingTombstoneFixer: SykmeldingTombstoneFixer,
) : ConsumerSeekAware {
    private val log = logger()
    private val startTimestamp = LocalDate.of(2024, 1, 8).atStartOfDay(osloZone).withHour(17).withMinute(20).toInstant().toEpochMilli()
    private val fixTimestamp = LocalDate.of(2024, 1, 9).atStartOfDay(osloZone).withHour(11).withMinute(50)

    @KafkaListener(
        topics = [SYKMELDINGSENDT_TOPIC, SYKMELDINGBEKREFTET_TOPIC],
        id = "sykmelding-tombstones-fixer",
        idIsGroup = true,
        properties = ["auto.offset.reset = earliest"],
        containerFactory = "syketilfelleKafkaListenerContainerFactory",
    )
    fun listenSendt(
        cr: ConsumerRecord<String, String?>,
        acknowledgment: Acknowledgment,
    ) {
        if (OffsetDateTime.now().atZoneSameInstant(osloZone) > fixTimestamp) {
            // Disse er ok
        } else {
            sykmeldingTombstoneFixer.prosesserSykmeldingPåNytt(cr)
        }
        acknowledgment.acknowledge()
    }

    override fun onPartitionsAssigned(
        assignments: Map<TopicPartition, Long>,
        callback: ConsumerSeekCallback,
    ) {
        // Seek all the assigned partition to a certain offset
        callback.seekToTimestamp(assignments.keys, startTimestamp)
        log.info("Ferdig med seekToTimestamp fra klassen ${this.javaClass.simpleName}")
    }
}
