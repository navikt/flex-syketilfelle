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
        topic: String,
    ) {
        when {
            sykmeldingKafkaMessage == null -> {
                val slettetTimestamp = OffsetDateTime.now()
                val biter =
                    syketilfellebitRepository.findByRessursId(key)
                        .filter { it.slettet == null }
                        .filter {
                            when (topic) {
                                SYKMELDINGSENDT_TOPIC -> {
                                    Tag.SENDT in it.tags.tagsFromString()
                                }
                                SYKMELDINGBEKREFTET_TOPIC -> {
                                    Tag.BEKREFTET in it.tags.tagsFromString()
                                }
                                else -> {
                                    // Sletter alle sykmelding biter
                                    true
                                }
                            }
                        }
                        .map { it.copy(slettet = slettetTimestamp) }

                if (biter.isEmpty()) {
                    log.info("Mottok tombstone for sykmelding $key på kafka. Ingen tilhørende biter.")
                } else {
                    log.info("Mottok status åpen for sykmelding $key på kafka. Markerer ${biter.size} biter som slettet.")
                    syketilfellebitRepository.saveAll(biter)
                }
            }

            sykmeldingKafkaMessage.event.erSvarOppdatering == true -> {
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
            }

            else -> {
                log.info("Prosseserer sykmelding $key med status ${sykmeldingKafkaMessage.event.statusEvent}")
                val biter = sykmeldingKafkaMessage.mapTilBiter()
                syketilfellebitLagring.lagreBiter(biter)
            }
        }
    }

    fun handterMottattSykmelding(
        key: String,
        mottattSykmeldingKafkaMessage: MottattSykmeldingKafkaMessage?,
        topic: String,
    ) {
        handterSykmelding(
            key,
            mottattSykmeldingKafkaMessage?.let {
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
                )
            },
            topic,
        )
    }
}
