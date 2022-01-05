package no.nav.helse.flex.syketilfelle.sykmelding

import no.nav.helse.flex.syketilfelle.logger
import no.nav.helse.flex.syketilfelle.syketilfellebit.SyketilfellebitLagring
import no.nav.helse.flex.syketilfelle.sykmelding.domain.MottattSykmeldingKafkaMessage
import no.nav.helse.flex.syketilfelle.sykmelding.domain.SykmeldingKafkaMessage
import no.nav.syfo.model.sykmeldingstatus.STATUS_APEN
import no.nav.syfo.model.sykmeldingstatus.SykmeldingStatusKafkaEventDTO
import org.springframework.stereotype.Service

@Service
class SykmeldingLagring(
    private val syketilfellebitLagring: SyketilfellebitLagring,
) {

    val log = logger()

    fun handterSykmelding(key: String, sykmeldingKafkaMessage: SykmeldingKafkaMessage?) {
        if (sykmeldingKafkaMessage == null) {
            log.debug("Mottok tombstone event for sykmelding $key")
            return
        }
        log.info("Prosseserer sykmelding $key med status ${sykmeldingKafkaMessage.event.statusEvent}")

        val biter = sykmeldingKafkaMessage.mapTilBiter()
        syketilfellebitLagring.lagreBiter(biter)
    }

    fun handterMottattSykmelding(key: String, mottattSykmeldingKafkaMessage: MottattSykmeldingKafkaMessage) {
        handterSykmelding(
            key,
            SykmeldingKafkaMessage(
                sykmelding = mottattSykmeldingKafkaMessage.sykmelding,
                kafkaMetadata = mottattSykmeldingKafkaMessage.kafkaMetadata,
                event = SykmeldingStatusKafkaEventDTO(
                    sykmeldingId = key,
                    timestamp = mottattSykmeldingKafkaMessage.kafkaMetadata.timestamp,
                    arbeidsgiver = null,
                    sporsmals = emptyList(),
                    statusEvent = STATUS_APEN
                )
            )
        )
    }
}
