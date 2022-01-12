package no.nav.helse.flex.syketilfelle.testdata

import no.nav.helse.flex.syketilfelle.logger
import no.nav.helse.flex.syketilfelle.syketilfellebit.SyketilfellebitRepository
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
@Profile("testdatareset")
class TestdataResetListener(val syketilfellebitRepository: SyketilfellebitRepository) {

    val log = logger()

    @KafkaListener(
        topics = [TESTDATA_RESET_TOPIC],
        containerFactory = "aivenKafkaListenerContainerFactory",
        properties = ["auto.offset.reset = earliest"],
    )
    fun listen(cr: ConsumerRecord<String, String>, acknowledgment: Acknowledgment) {
        val fnr = cr.value()
        val antall = syketilfellebitRepository.deleteByFnr(fnr)
        log.info("Slettet $antall biter på fnr $fnr - Key ${cr.key()}")
        acknowledgment.acknowledge()
    }
}

const val TESTDATA_RESET_TOPIC = "flex.testdata-reset"
