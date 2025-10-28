package no.nav.helse.flex.syketilfelle.ventetid

import no.nav.helse.flex.syketilfelle.sykmelding.domain.SykmeldingKafkaMessage
import java.time.LocalDate

data class ErUtenforVentetidRequest(
    val tilleggsopplysninger: Tilleggsopplysninger? = null,
    val sykmeldingKafkaMessage: SykmeldingKafkaMessage? = null,
)

fun ErUtenforVentetidRequest.tilVentetidRequest(): VentetidRequest =
    VentetidRequest(
        tilleggsopplysninger = tilleggsopplysninger,
        sykmeldingKafkaMessage = sykmeldingKafkaMessage,
    )

// Brukes i TokenX-response.
data class ErUtenforVentetidResponse(
    val erUtenforVentetid: Boolean,
    val oppfolgingsdato: LocalDate?,
)

data class Tilleggsopplysninger(
    val egenmeldingsperioder: List<FomTomPeriode>?,
)

data class VentetidRequest(
    val tilleggsopplysninger: Tilleggsopplysninger? = null,
    val sykmeldingKafkaMessage: SykmeldingKafkaMessage? = null,
    val returnerPerioderInnenforVentetid: Boolean = false,
)

data class VentetidResponse(
    val ventetid: FomTomPeriode? = null,
)

data class VentetidInternalResponse(
    var erUtenforVentetid: Boolean,
    val ventetid: FomTomPeriode,
    var sykmeldingsperiode: FomTomPeriode?,
    val syketilfellebiter: List<SyketilfellebitInternal> = emptyList(),
)

data class SyketilfellebitInternal(
    val syketilfellebitId: String,
    val fnr: String,
    val opprettet: java.time.OffsetDateTime,
    val inntruffet: java.time.OffsetDateTime,
    val orgnummer: String?,
    val tags: String,
    val ressursId: String,
    val korrigererSendtSoknad: String?,
    val fom: LocalDate,
    val tom: LocalDate,
    val publisert: Boolean,
    val slettet: java.time.OffsetDateTime?,
    val tombstonePublisert: java.time.OffsetDateTime?,
)

data class FomTomPeriode(
    val fom: LocalDate,
    val tom: LocalDate,
)
