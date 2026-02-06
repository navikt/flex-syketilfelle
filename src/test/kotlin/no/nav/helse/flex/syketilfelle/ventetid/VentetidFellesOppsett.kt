package no.nav.helse.flex.syketilfelle.ventetid

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.syketilfelle.objectMapper
import no.nav.helse.flex.syketilfelle.syketilfellebit.SyketilfellebitDbRecord
import no.nav.helse.flex.syketilfelle.syketilfellebit.SyketilfellebitRepository
import no.nav.helse.flex.syketilfelle.sykmelding.SYKMELDINGMOTTATT_TOPIC
import no.nav.helse.flex.syketilfelle.sykmelding.SYKMELDINGSENDT_TOPIC
import no.nav.helse.flex.syketilfelle.sykmelding.SykmeldingLagring
import no.nav.helse.flex.syketilfelle.sykmelding.domain.MottattSykmeldingKafkaMessage
import no.nav.helse.flex.syketilfelle.sykmelding.domain.SykmeldingKafkaMessage
import no.nav.helse.flex.syketilfelle.sykmelding.skapArbeidsgiverSykmelding
import no.nav.syfo.model.sykmelding.model.PeriodetypeDTO
import no.nav.syfo.model.sykmeldingstatus.KafkaMetadataDTO
import no.nav.syfo.model.sykmeldingstatus.STATUS_BEKREFTET
import no.nav.syfo.model.sykmeldingstatus.SporsmalOgSvarDTO
import no.nav.syfo.model.sykmeldingstatus.SykmeldingStatusKafkaEventDTO
import org.awaitility.Awaitility.await
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*
import java.util.concurrent.TimeUnit

interface VentetidFellesOppsett {
    var sykmeldingLagring: SykmeldingLagring
    var syketilfellebitRepository: SyketilfellebitRepository
    val fnr: String

    fun MottattSykmeldingKafkaMessage.publiser() {
        sykmeldingLagring.handterMottattSykmelding("key", this, SYKMELDINGMOTTATT_TOPIC)
    }

    fun SykmeldingKafkaMessage.publiser() {
        sykmeldingLagring.handterSykmelding("key", this, SYKMELDINGSENDT_TOPIC)
    }

    fun String.tilSyketilfellebitDbRecords(): List<SyketilfellebitDbRecord> = objectMapper.readValue(this)

    fun verifiserAtBiterErLagret(forventetAntallBiter: Int) {
        await().atMost(5, TimeUnit.SECONDS).until {
            syketilfellebitRepository.count().toInt() == forventetAntallBiter
        }
    }

    fun skapApenSykmeldingKafkaMessage(
        fom: LocalDate = LocalDate.now(),
        tom: LocalDate = LocalDate.now(),
        type: PeriodetypeDTO = PeriodetypeDTO.AKTIVITET_IKKE_MULIG,
    ): MottattSykmeldingKafkaMessage {
        val sykmeldingId = UUID.randomUUID().toString()
        return MottattSykmeldingKafkaMessage(
            sykmelding =
                skapArbeidsgiverSykmelding(
                    fom = fom,
                    tom = tom,
                    sykmeldingId = sykmeldingId,
                    type = type,
                ),
            kafkaMetadata =
                KafkaMetadataDTO(
                    sykmeldingId = sykmeldingId,
                    fnr = fnr,
                    timestamp = OffsetDateTime.now(),
                    source = "Denne testen",
                ),
        )
    }

    fun skapSykmeldingKafkaMessage(
        fom: LocalDate = LocalDate.now(),
        tom: LocalDate = LocalDate.now(),
        type: PeriodetypeDTO = PeriodetypeDTO.AKTIVITET_IKKE_MULIG,
        sporsmals: List<SporsmalOgSvarDTO>? = null,
    ): SykmeldingKafkaMessage {
        val apen = skapApenSykmeldingKafkaMessage(fom, tom, type)

        return SykmeldingKafkaMessage(
            sykmelding = apen.sykmelding,
            kafkaMetadata = apen.kafkaMetadata,
            event =
                SykmeldingStatusKafkaEventDTO(
                    sykmeldingId = apen.sykmelding.id,
                    timestamp = OffsetDateTime.now(),
                    statusEvent = STATUS_BEKREFTET,
                    arbeidsgiver = null,
                    sporsmals = sporsmals,
                ),
        )
    }
}
