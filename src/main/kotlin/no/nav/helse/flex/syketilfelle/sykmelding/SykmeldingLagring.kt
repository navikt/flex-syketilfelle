package no.nav.helse.flex.syketilfelle.sykmelding

import no.nav.helse.flex.syketilfelle.client.pdl.PdlClient
import no.nav.helse.flex.syketilfelle.logger
import no.nav.helse.flex.syketilfelle.syketilfellebit.SyketilfellebitLagring
import no.nav.helse.flex.syketilfelle.syketilfellebit.SyketilfellebitRepository
import no.nav.helse.flex.syketilfelle.syketilfellebit.Tag
import no.nav.helse.flex.syketilfelle.syketilfellebit.tagsFromString
import no.nav.helse.flex.syketilfelle.sykmelding.domain.MottattSykmeldingKafkaMessage
import no.nav.helse.flex.syketilfelle.sykmelding.domain.SykmeldingKafkaMessage
import no.nav.syfo.model.sykmeldingstatus.STATUS_APEN
import no.nav.syfo.model.sykmeldingstatus.SykmeldingStatusKafkaEventDTO
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

@Service
class SykmeldingLagring(
    private val syketilfellebitLagring: SyketilfellebitLagring,
    private val pdlClient: PdlClient,
    private val syketilfellebitRepository: SyketilfellebitRepository,
) {
    val log = logger()

    fun handterSykmelding(
        key: String,
        sykmeldingKafkaMessage: SykmeldingKafkaMessage?,
    ) {
        if (sykmeldingKafkaMessage == null) {
            log.debug("Mottok tombstone event for sykmelding $key")
            return
        }
        log.info("Prosseserer sykmelding $key med status ${sykmeldingKafkaMessage.event.statusEvent}")

        if (sykmeldingKafkaMessage.event.erSvarOppdatering == true) {
            // Slett de gamle som ikke lengre stemmer
            val identer = pdlClient.hentFolkeregisterIdenter(sykmeldingKafkaMessage.kafkaMetadata.fnr)
            val biter =
                syketilfellebitRepository.findByFnrIn(identer)
                    .filter { it.ressursId == sykmeldingKafkaMessage.sykmelding.id }
                    .filter { it.tags.tagsFromString().contains(Tag.EGENMELDING) }
                    .map { it.copy(slettet = OffsetDateTime.now()) }

            syketilfellebitRepository.saveAll(biter)

            val nyeEgenmeldingsBiter = sykmeldingKafkaMessage.mapTilEgenmeldingBiter()
            syketilfellebitLagring.lagreBiter(nyeEgenmeldingsBiter)

            return
        }

        val biter = sykmeldingKafkaMessage.mapTilBiter()
        syketilfellebitLagring.lagreBiter(biter)
    }

    fun handterMottattSykmelding(
        key: String,
        mottattSykmeldingKafkaMessage: MottattSykmeldingKafkaMessage,
    ) {
        handterSykmelding(
            key,
            SykmeldingKafkaMessage(
                sykmelding = mottattSykmeldingKafkaMessage.sykmelding,
                kafkaMetadata = mottattSykmeldingKafkaMessage.kafkaMetadata,
                event =
                    SykmeldingStatusKafkaEventDTO(
                        sykmeldingId = key,
                        timestamp = mottattSykmeldingKafkaMessage.kafkaMetadata.timestamp,
                        arbeidsgiver = null,
                        sporsmals = emptyList(),
                        statusEvent = STATUS_APEN,
                    ),
            ),
        )
    }
}
