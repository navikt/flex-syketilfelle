package no.nav.helse.flex.syketilfelle.sykmelding

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.syketilfelle.extensions.osloZone
import no.nav.helse.flex.syketilfelle.logger
import no.nav.helse.flex.syketilfelle.objectMapper
import no.nav.helse.flex.syketilfelle.sykmelding.domain.MottattSykmeldingKafkaMessage
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.TopicPartition
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.listener.ConsumerSeekAware
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import java.time.LocalDate

const val SYKMELDINGMOTTATT_TOPIC = "teamsykmelding." + "syfo-mottatt-sykmelding"

@Component
class AivenSykmeldingMottattListener(
    private val sykmeldingLagring: SykmeldingLagring,
) : ConsumerSeekAware {
    private val log = logger()
    private val startTimestamp = LocalDate.of(2024, 1, 8).atStartOfDay(osloZone).withHour(17).withMinute(20).toInstant().toEpochMilli()

    @KafkaListener(
        topics = [SYKMELDINGMOTTATT_TOPIC],
        id = "sykmelding-mottatt",
        idIsGroup = false,
        containerFactory = "syketilfelleKafkaListenerContainerFactory",
    )
    fun listen(
        cr: ConsumerRecord<String, String?>,
        acknowledgment: Acknowledgment,
    ) {
        val sykmeldingDTO = cr.value()?.tilSykmeldingDTO()

        sykmeldingLagring.handterMottattSykmelding(cr.key(), sykmeldingDTO, cr.topic())

        acknowledgment.acknowledge()
    }

    override fun onPartitionsAssigned(
        assignments: Map<TopicPartition, Long>,
        callback: ConsumerSeekAware.ConsumerSeekCallback,
    ) {
        // Seek all the assigned partition to a certain offset
        callback.seekToTimestamp(assignments.keys, startTimestamp)
        log.info("Ferdig med seekToTimestamp fra klassen ${this.javaClass.simpleName}")
    }

    fun String.tilSykmeldingDTO(): MottattSykmeldingKafkaMessage = objectMapper.readValue(this)
}
