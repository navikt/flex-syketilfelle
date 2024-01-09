package no.nav.helse.flex.syketilfelle.sykmelding

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.syketilfelle.logger
import no.nav.helse.flex.syketilfelle.objectMapper
import no.nav.helse.flex.syketilfelle.sykmelding.domain.SykmeldingKafkaMessage
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.listener.ConsumerSeekAware
import org.springframework.stereotype.Component

@Component
class SykmeldingTombstoneFixer(
    private val sykmeldingLagring: SykmeldingLagring,
) : ConsumerSeekAware {
    private val log = logger()
    private val tombstoneListe = mutableListOf<String>()

    fun prosesserSykmeldingPåNytt(cr: ConsumerRecord<String, String?>) {
        if (cr.value() == null && cr.key() !in tombstoneListe) {
            // Nye tombstones legges til i liste
            log.info("SykmeldingTombstoneFixer: Legger til sykmelding ${cr.key()} i tombstoneListe")
            tombstoneListe.add(cr.key())
        } else if (cr.key() in tombstoneListe) {
            // Sykmeldinger som tidligere har blitt tombstoned prosesseres på nytt
            log.info("SykmeldingTombstoneFixer: Prosesserer sykmelding ${cr.key()} på nytt")
            sykmeldingLagring.handterSykmelding(cr.key(), cr.value()?.tilSykmeldingDTO())
        }
    }

    fun String.tilSykmeldingDTO(): SykmeldingKafkaMessage = objectMapper.readValue(this)
}
