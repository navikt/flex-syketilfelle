package no.nav.helse.flex.syketilfelle

import no.nav.helse.flex.syketilfelle.syketilfellebit.SyketilfellebitDbRecord
import no.nav.helse.flex.syketilfelle.syketilfellebit.Tag
import no.nav.helse.flex.syketilfelle.sykmelding.domain.MottattSykmeldingKafkaMessage
import no.nav.helse.flex.syketilfelle.sykmelding.domain.SykmeldingKafkaMessage
import no.nav.syfo.model.sykmelding.arbeidsgiver.ArbeidsgiverAGDTO
import no.nav.syfo.model.sykmelding.arbeidsgiver.ArbeidsgiverSykmelding
import no.nav.syfo.model.sykmelding.arbeidsgiver.BehandlerAGDTO
import no.nav.syfo.model.sykmelding.arbeidsgiver.KontaktMedPasientAGDTO
import no.nav.syfo.model.sykmelding.arbeidsgiver.SykmeldingsperiodeAGDTO
import no.nav.syfo.model.sykmelding.model.AdresseDTO
import no.nav.syfo.model.sykmelding.model.PeriodetypeDTO
import no.nav.syfo.model.sykmeldingstatus.ArbeidsgiverStatusDTO
import no.nav.syfo.model.sykmeldingstatus.KafkaMetadataDTO
import no.nav.syfo.model.sykmeldingstatus.STATUS_BEKREFTET
import no.nav.syfo.model.sykmeldingstatus.STATUS_SENDT
import no.nav.syfo.model.sykmeldingstatus.SporsmalOgSvarDTO
import no.nav.syfo.model.sykmeldingstatus.SykmeldingStatusKafkaEventDTO
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

fun lagMottattSykmeldingKafkaMessage(
    fnr: String,
    fom: LocalDate = LocalDate.now(),
    tom: LocalDate = LocalDate.now(),
    type: PeriodetypeDTO = PeriodetypeDTO.AKTIVITET_IKKE_MULIG,
    sykmeldingId: String = UUID.randomUUID().toString(),
) = lagMottattSykmeldingKafkaMessage(
    fnr = fnr,
    sykmelding = lagArbeidsgiverSykmelding(fom = fom, tom = tom, sykmeldingId = sykmeldingId, type = type),
)

fun lagMottattSykmeldingKafkaMessage(
    fnr: String,
    sykmelding: ArbeidsgiverSykmelding,
    timestamp: OffsetDateTime = OffsetDateTime.now(),
) = MottattSykmeldingKafkaMessage(
    sykmelding = sykmelding,
    kafkaMetadata =
        KafkaMetadataDTO(
            sykmeldingId = sykmelding.id,
            timestamp = timestamp,
            fnr = fnr,
            source = "Test",
        ),
)

fun lagSendtSykmeldingKafkaMessage(
    fnr: String,
    sykmelding: ArbeidsgiverSykmelding,
    orgnummer: String? = null,
) = SykmeldingKafkaMessage(
    sykmelding = sykmelding,
    kafkaMetadata =
        KafkaMetadataDTO(
            sykmeldingId = sykmelding.id,
            fnr = fnr,
            timestamp = OffsetDateTime.now(),
            source = "Test",
        ),
    event =
        SykmeldingStatusKafkaEventDTO(
            sykmeldingId = sykmelding.id,
            timestamp = OffsetDateTime.now(),
            statusEvent = STATUS_SENDT,
            arbeidsgiver = orgnummer?.let { ArbeidsgiverStatusDTO(orgnummer = it, orgNavn = "orgNavn") },
            sporsmals = emptyList(),
        ),
)

fun lagBekreftetSykmeldingKafkaMessage(
    fnr: String,
    fom: LocalDate = LocalDate.now(),
    tom: LocalDate = LocalDate.now(),
    type: PeriodetypeDTO = PeriodetypeDTO.AKTIVITET_IKKE_MULIG,
    sporsmals: List<SporsmalOgSvarDTO>? = null,
) = lagBekreftetSykmeldingKafkaMessage(
    lagMottattSykmeldingKafkaMessage(fnr = fnr, fom = fom, tom = tom, type = type),
    sporsmals,
)

fun lagBekreftetSykmeldingKafkaMessage(
    mottatt: MottattSykmeldingKafkaMessage,
    sporsmals: List<SporsmalOgSvarDTO>? = null,
) = SykmeldingKafkaMessage(
    sykmelding = mottatt.sykmelding,
    kafkaMetadata = mottatt.kafkaMetadata,
    event =
        SykmeldingStatusKafkaEventDTO(
            sykmeldingId = mottatt.sykmelding.id,
            timestamp = OffsetDateTime.now(),
            statusEvent = STATUS_BEKREFTET,
            arbeidsgiver = null,
            sporsmals = sporsmals,
        ),
)

fun lagArbeidsgiverSykmelding(
    fom: LocalDate = LocalDate.now(),
    tom: LocalDate = LocalDate.now(),
    sykmeldingId: String = UUID.randomUUID().toString(),
    type: PeriodetypeDTO = PeriodetypeDTO.AKTIVITET_IKKE_MULIG,
): ArbeidsgiverSykmelding =
    ArbeidsgiverSykmelding(
        id = sykmeldingId,
        mottattTidspunkt = OffsetDateTime.now(),
        arbeidsgiver = ArbeidsgiverAGDTO(null, null),
        sykmeldingsperioder =
            listOf(
                SykmeldingsperiodeAGDTO(
                    fom = fom,
                    tom = tom,
                    reisetilskudd = false,
                    type = type,
                    aktivitetIkkeMulig = null,
                    behandlingsdager = null,
                    gradert = null,
                    innspillTilArbeidsgiver = null,
                ),
            ),
        behandletTidspunkt = OffsetDateTime.now(),
        syketilfelleStartDato = null,
        egenmeldt = false,
        harRedusertArbeidsgiverperiode = false,
        behandler =
            BehandlerAGDTO(
                fornavn = "Lege",
                mellomnavn = null,
                etternavn = "Legesen",
                hpr = null,
                adresse =
                    AdresseDTO(
                        gate = null,
                        postnummer = null,
                        kommune = null,
                        postboks = null,
                        land = null,
                    ),
                tlf = null,
            ),
        kontaktMedPasient = KontaktMedPasientAGDTO(null),
        meldingTilArbeidsgiver = null,
        tiltakArbeidsplassen = null,
        prognose = null,
        papirsykmelding = false,
        merknader = null,
        utenlandskSykmelding = null,
        signaturDato = null,
    )

fun lagSyketilfelleBit(
    fnr: String,
    ressursId: String,
    fom: LocalDate,
    tom: LocalDate,
    tags: List<Tag>,
    opprettet: OffsetDateTime = OffsetDateTime.now(),
): SyketilfellebitDbRecord =
    SyketilfellebitDbRecord(
        syketilfellebitId = UUID.randomUUID().toString(),
        fnr = fnr,
        orgnummer = null,
        opprettet = opprettet,
        inntruffet = opprettet,
        tags = tags.joinToString(separator = ",") { it.name },
        ressursId = ressursId,
        fom = fom,
        tom = tom,
        korrigererSendtSoknad = null,
        publisert = true,
    )
