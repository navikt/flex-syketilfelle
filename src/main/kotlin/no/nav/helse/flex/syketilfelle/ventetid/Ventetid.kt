package no.nav.helse.flex.syketilfelle.ventetid

import no.nav.helse.flex.syketilfelle.sykmelding.domain.SykmeldingKafkaMessage
import java.time.LocalDate

data class ErUtenforVentetidRequest(
    val sykmeldingKafkaMessage: SykmeldingKafkaMessage? = null,
)

fun ErUtenforVentetidRequest.tilVentetidRequest(): VentetidRequest =
    VentetidRequest(
        sykmeldingKafkaMessage = sykmeldingKafkaMessage,
    )

data class SammeVentetidRequest(
    val sykmeldingKafkaMessage: SykmeldingKafkaMessage? = null,
)

fun SammeVentetidRequest.tilVentetidRequest(): VentetidRequest =
    VentetidRequest(
        sykmeldingKafkaMessage = sykmeldingKafkaMessage,
        returnerPerioderInnenforVentetid = true,
    )

data class VentetidRequest(
    val sykmeldingKafkaMessage: SykmeldingKafkaMessage? = null,
    val returnerPerioderInnenforVentetid: Boolean = false,
)

data class VentetidResponse(
    val ventetid: FomTomPeriode? = null,
)

data class FomTomPeriode(
    val fom: LocalDate,
    val tom: LocalDate,
)

data class SammeVentetidResponse(
    val ventetidPerioder: List<SammeVentetidPeriode>,
)

data class SammeVentetidPeriode(
    val ressursId: String,
    val ventetid: FomTomPeriode,
)

// Brukes i TokenX-response fra flex-sykmeldinger-backend.
data class ErUtenforVentetidResponse(
    val erUtenforVentetid: Boolean,
)
