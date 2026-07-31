package no.nav.syfo.sykmelding.kafka.model

import no.nav.syfo.model.sykmelding.arbeidsgiver.ArbeidsgiverSykmeldingDTO

data class MottattSykmeldingKafkaMessage(
    val sykmelding: ArbeidsgiverSykmeldingDTO,
    val kafkaMetadata: KafkaMetadataDTO,
)
