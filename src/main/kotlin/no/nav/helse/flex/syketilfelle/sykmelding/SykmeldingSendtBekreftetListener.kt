package no.nav.helse.flex.syketilfelle.sykmelding

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.syketilfelle.objectMapper
import no.nav.helse.flex.syketilfelle.sykmelding.domain.SykmeldingKafkaMessage
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

const val SYKMELDINGSENDT_TOPIC = "teamsykmelding." + "syfo-sendt-sykmelding"
const val SYKMELDINGBEKREFTET_TOPIC = "teamsykmelding." + "syfo-bekreftet-sykmelding"

@Component
class SykmeldingSendtBekreftetListener(
    private val sykmeldingLagring: SykmeldingLagring
) {

    @KafkaListener(
        topics = [SYKMELDINGSENDT_TOPIC],
        id = "sykmelding-sendt",
        idIsGroup = false,
        containerFactory = "syketilfelleKafkaListenerContainerFactory"
    )
    fun listenSendt(cr: ConsumerRecord<String, String?>, acknowledgment: Acknowledgment) {
        listen(cr, acknowledgment)
    }

    @KafkaListener(
        topics = [SYKMELDINGBEKREFTET_TOPIC],
        id = "sykmelding-bekreftet",
        idIsGroup = false,
        containerFactory = "syketilfelleKafkaListenerContainerFactory"
    )
    fun listenBekreftet(cr: ConsumerRecord<String, String?>, acknowledgment: Acknowledgment) {
        listen(cr, acknowledgment)
    }

    private fun listen(cr: ConsumerRecord<String, String?>, acknowledgment: Acknowledgment) {
        val sykmeldingSentBekreftetDTO = cr.value()?.tilSykmeldingDTO()

        sykmeldingLagring.handterSykmelding(cr.key(), sykmeldingSentBekreftetDTO)

        acknowledgment.acknowledge()
    }

    fun String.tilSykmeldingDTO(): SykmeldingKafkaMessage = objectMapper.readValue(this)
}
