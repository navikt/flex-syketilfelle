package no.nav.helse.flex.syketilfelle.sykmelding

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.syketilfelle.objectMapper
import no.nav.helse.flex.syketilfelle.sykmelding.domain.MottattSykmeldingKafkaMessage
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

const val SYKMELDINGMOTTATT_TOPIC = "teamsykmelding." + "syfo-mottatt-sykmelding"

@Component
class AivenSykmeldingMottattListener(

    private val sykmeldingLagring: SykmeldingLagring,
) {

    @KafkaListener(
        topics = [SYKMELDINGMOTTATT_TOPIC],
        idIsGroup = false,
        containerFactory = "syketilfelleKafkaListenerContainerFactory",
    )
    fun listen(cr: ConsumerRecord<String, String?>, acknowledgment: Acknowledgment) {

        cr.value()?.let {
            sykmeldingLagring.handterMottattSykmelding(cr.key(), it.tilSykmeldingDTO())
        }

        acknowledgment.acknowledge()
    }

    fun String.tilSykmeldingDTO(): MottattSykmeldingKafkaMessage = objectMapper.readValue(this)
}
