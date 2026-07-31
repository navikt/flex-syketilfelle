package no.nav.helse.flex.syketilfelle.sykmelding.domain

import no.nav.syfo.model.sykmelding.arbeidsgiver.ArbeidsgiverSykmeldingDTO
import no.nav.syfo.model.sykmeldingstatus.KafkaMetadataDTO
import no.nav.syfo.model.sykmeldingstatus.SykmeldingStatusKafkaEventDTO

data class SykmeldingKafkaMessage(
    val sykmelding: ArbeidsgiverSykmeldingDTO,
    val kafkaMetadata: KafkaMetadataDTO,
    val event: SykmeldingStatusKafkaEventDTO,
)

data class SykmeldingRequest(
    val sykmeldingKafkaMessage: SykmeldingKafkaMessage,
)
