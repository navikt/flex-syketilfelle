package no.nav.helse.flex.syketilfelle.ventetid

import no.nav.helse.flex.syketilfelle.sykmelding.domain.SykmeldingKafkaMessage
import java.time.LocalDate

data class VenteperiodeRequest(
    val tilleggsopplysninger: Tilleggsopplysninger? = null,
    val sykmeldingKafkaMessage: SykmeldingKafkaMessage? = null,
    val returnerPerioderInnenforVentetid: Boolean = false,
)

data class VenteperiodeResponse(
    val venteperiode: Venteperiode?,
)

data class Venteperiode(
    val fom: LocalDate,
    val tom: LocalDate,
)
