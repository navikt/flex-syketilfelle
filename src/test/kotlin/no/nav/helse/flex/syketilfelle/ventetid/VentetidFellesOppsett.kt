package no.nav.helse.flex.syketilfelle.ventetid

import no.nav.helse.flex.syketilfelle.sykmelding.SykmeldingLagring
import no.nav.helse.flex.syketilfelle.sykmelding.domain.MottattSykmeldingKafkaMessage
import no.nav.helse.flex.syketilfelle.sykmelding.domain.SykmeldingKafkaMessage
import no.nav.helse.flex.syketilfelle.sykmelding.skapArbeidsgiverSykmelding
import no.nav.syfo.model.sykmelding.model.PeriodetypeDTO
import no.nav.syfo.model.sykmeldingstatus.KafkaMetadataDTO
import no.nav.syfo.model.sykmeldingstatus.STATUS_BEKREFTET
import no.nav.syfo.model.sykmeldingstatus.SporsmalOgSvarDTO
import no.nav.syfo.model.sykmeldingstatus.SykmeldingStatusKafkaEventDTO
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

interface VentetidFellesOppsett {

    var sykmeldingLagring: SykmeldingLagring
    val fnr: String

    fun MottattSykmeldingKafkaMessage.publiser() {
        sykmeldingLagring.handterMottattSykmelding("key", this)
    }

    fun skapApenSykmeldingKafkaMessage(
        fom: LocalDate = LocalDate.now(),
        tom: LocalDate = LocalDate.now(),
        harRedusertArbeidsgiverperiode: Boolean = false,
        type: PeriodetypeDTO = PeriodetypeDTO.AKTIVITET_IKKE_MULIG
    ): MottattSykmeldingKafkaMessage {
        val sykmeldingId = UUID.randomUUID().toString()
        return MottattSykmeldingKafkaMessage(
            sykmelding = skapArbeidsgiverSykmelding(
                fom = fom,
                tom = tom,
                sykmeldingId = sykmeldingId,
                harRedusertArbeidsgiverperiode = harRedusertArbeidsgiverperiode,
                type = type

            ),
            kafkaMetadata = KafkaMetadataDTO(
                sykmeldingId = sykmeldingId,
                fnr = fnr,
                timestamp = OffsetDateTime.now(),
                source = "Denne testen"
            )
        )
    }

    fun skapSykmeldingKafkaMessage(
        fom: LocalDate = LocalDate.now(),
        tom: LocalDate = LocalDate.now(),
        harRedusertArbeidsgiverperiode: Boolean = false,
        type: PeriodetypeDTO = PeriodetypeDTO.AKTIVITET_IKKE_MULIG,
        sporsmals: List<SporsmalOgSvarDTO>? = null
    ): SykmeldingKafkaMessage {
        val apen = skapApenSykmeldingKafkaMessage(fom, tom, harRedusertArbeidsgiverperiode, type)

        return SykmeldingKafkaMessage(
            sykmelding = apen.sykmelding,
            kafkaMetadata = apen.kafkaMetadata,
            event = SykmeldingStatusKafkaEventDTO(
                sykmeldingId = apen.sykmelding.id,
                timestamp = OffsetDateTime.now(),
                statusEvent = STATUS_BEKREFTET,
                arbeidsgiver = null,
                sporsmals = sporsmals
            )
        )
    }
}
