package no.nav.helse.flex.syketilfelle.sykmelding

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.syketilfelle.extensions.osloZone
import no.nav.helse.flex.syketilfelle.logger
import no.nav.helse.flex.syketilfelle.objectMapper
import no.nav.helse.flex.syketilfelle.sykmelding.domain.SykmeldingKafkaMessage
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.TopicPartition
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.listener.ConsumerSeekAware
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDate

const val SYKMELDINGSENDT_TOPIC = "teamsykmelding." + "syfo-sendt-sykmelding"
const val SYKMELDINGBEKREFTET_TOPIC = "teamsykmelding." + "syfo-bekreftet-sykmelding"

@Component
class SykmeldingSendtBekreftetListener(
    private val sykmeldingLagring: SykmeldingLagring,
    @Value("\${NAIS_CLUSTER_NAME}") val cluster: String,
) : ConsumerSeekAware {
    private val log = logger()
    private val startTimestamp = LocalDate.of(2024, 1, 8).atStartOfDay(osloZone).withHour(17).withMinute(20).toInstant().toEpochMilli()

    @KafkaListener(
        topics = [SYKMELDINGSENDT_TOPIC],
        id = "sykmelding-sendt",
        idIsGroup = false,
        containerFactory = "syketilfelleKafkaListenerContainerFactory",
    )
    fun listenSendt(
        cr: ConsumerRecord<String, String?>,
        acknowledgment: Acknowledgment,
    ) {
        listen(cr, acknowledgment)
    }

    @KafkaListener(
        topics = [SYKMELDINGBEKREFTET_TOPIC],
        id = "sykmelding-bekreftet",
        idIsGroup = false,
        containerFactory = "syketilfelleKafkaListenerContainerFactory",
    )
    fun listenBekreftet(
        cr: ConsumerRecord<String, String?>,
        acknowledgment: Acknowledgment,
    ) {
        listen(cr, acknowledgment)
    }

    override fun onPartitionsAssigned(
        assignments: Map<TopicPartition, Long>,
        callback: ConsumerSeekAware.ConsumerSeekCallback,
    ) {
        // Seek all the assigned partition to a certain offset
        callback.seekToTimestamp(assignments.keys, startTimestamp)
        log.info("Ferdig med seekToTimestamp fra klassen ${this.javaClass.simpleName}")
    }

    private fun listen(
        cr: ConsumerRecord<String, String?>,
        acknowledgment: Acknowledgment,
    ) {
        if (cluster in listOf("dev-gcp", "prod-gcp")) {
            acknowledgment.nack(Duration.ofSeconds(5))
            return
        }

        val sykmeldingSentBekreftetDTO = cr.value()?.tilSykmeldingDTO()

        sykmeldingLagring.handterSykmelding(cr.key(), sykmeldingSentBekreftetDTO, cr.topic())

        acknowledgment.acknowledge()
    }

    fun String.tilSykmeldingDTO(): SykmeldingKafkaMessage = objectMapper.readValue(this)
}
