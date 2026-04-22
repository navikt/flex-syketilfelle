package no.nav.helse.flex.syketilfelle.ventetid

import no.nav.helse.flex.syketilfelle.sykmelding.domain.SykmeldingKafkaMessage
import java.time.LocalDate

data class ErUtenforVentetidRequest(
    val tilleggsopplysninger: Tilleggsopplysninger? = null,
    val sykmeldingKafkaMessage: SykmeldingKafkaMessage? = null,
    val kunSendtBekreftet: Boolean = false,
)

fun ErUtenforVentetidRequest.tilVentetidRequest(): VentetidRequest =
    VentetidRequest(
        tilleggsopplysninger = tilleggsopplysninger,
        sykmeldingKafkaMessage = sykmeldingKafkaMessage,
        kunSendtBekreftet = kunSendtBekreftet,
    )

fun SammeVentetidRequest.tilVentetidRequest(returnerPerioderInnenforVentetid: Boolean): VentetidRequest =
    VentetidRequest(
        tilleggsopplysninger = tilleggsopplysninger,
        sykmeldingKafkaMessage = sykmeldingKafkaMessage,
        returnerPerioderInnenforVentetid = returnerPerioderInnenforVentetid,
        kunSendtBekreftet = kunSendtBekreftet,
    )

// Brukes i TokenX-response.
data class ErUtenforVentetidResponse(
    val erUtenforVentetid: Boolean,
    val oppfolgingsdato: LocalDate?,
    val ventetid: FomTomPeriode? = null,
)

data class Tilleggsopplysninger(
    val egenmeldingsperioder: List<FomTomPeriode>?,
)

data class VentetidRequest(
    val tilleggsopplysninger: Tilleggsopplysninger? = null,
    val sykmeldingKafkaMessage: SykmeldingKafkaMessage? = null,
    val returnerPerioderInnenforVentetid: Boolean = false,
    val kunSendtBekreftet: Boolean = false,
)

data class VentetidResponse(
    val ventetid: FomTomPeriode? = null,
)

data class FomTomPeriode(
    val fom: LocalDate,
    val tom: LocalDate,
)

data class SammeVentetidRequest(
    val tilleggsopplysninger: Tilleggsopplysninger? = null,
    val sykmeldingKafkaMessage: SykmeldingKafkaMessage? = null,
    val kunSendtBekreftet: Boolean = false,
)

data class SammeVentetidPeriode(
    val ressursId: String,
    val ventetid: FomTomPeriode,
)

data class SammeVentetidResponse(
    val ventetidPerioder: List<SammeVentetidPeriode>,
)
