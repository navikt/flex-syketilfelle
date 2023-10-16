package no.nav.helse.flex.syketilfelle.sykmelding

import no.nav.syfo.model.sykmelding.arbeidsgiver.*
import no.nav.syfo.model.sykmelding.model.AdresseDTO
import no.nav.syfo.model.sykmelding.model.PeriodetypeDTO
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

fun skapArbeidsgiverSykmelding(
    fom: LocalDate = LocalDate.now(),
    tom: LocalDate = LocalDate.now(),
    sykmeldingId: String = UUID.randomUUID().toString(),
    harRedusertArbeidsgiverperiode: Boolean = false,
    type: PeriodetypeDTO = PeriodetypeDTO.AKTIVITET_IKKE_MULIG

): ArbeidsgiverSykmelding {
    return ArbeidsgiverSykmelding(
        id = sykmeldingId,
        mottattTidspunkt = OffsetDateTime.now(),
        arbeidsgiver = ArbeidsgiverAGDTO(null, null),
        sykmeldingsperioder = listOf(
            SykmeldingsperiodeAGDTO(
                fom = fom,
                tom = tom,
                reisetilskudd = false,
                type = type,
                aktivitetIkkeMulig = null,
                behandlingsdager = null,
                gradert = null,
                innspillTilArbeidsgiver = null

            )
        ),
        behandletTidspunkt = OffsetDateTime.now(),
        syketilfelleStartDato = null,
        egenmeldt = false,
        harRedusertArbeidsgiverperiode = harRedusertArbeidsgiverperiode,
        behandler = BehandlerAGDTO(
            fornavn = "Lege",
            mellomnavn = null,
            etternavn = "Legesen",
            hpr = null,
            adresse = AdresseDTO(
                gate = null,
                postnummer = null,
                kommune = null,
                postboks = null,
                land = null
            ),
            tlf = null
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
}
