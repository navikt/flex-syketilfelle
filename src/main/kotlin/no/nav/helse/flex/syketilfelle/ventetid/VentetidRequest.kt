package no.nav.helse.flex.syketilfelle.ventetid

import no.nav.helse.flex.syketilfelle.sykmelding.domain.SykmeldingKafkaMessage
import java.time.LocalDate

data class VentetidRequest(
    val tilleggsopplysninger: Tilleggsopplysninger? = null,
    val sykmeldingKafkaMessage: SykmeldingKafkaMessage? = null,
)

data class VenteperiodeRequest(
    val tilleggsopplysninger: Tilleggsopplysninger? = null,
    val sykmeldingKafkaMessage: SykmeldingKafkaMessage? = null,
)

fun VentetidRequest.tilVenteperiodeRequest(): VenteperiodeRequest =
    VenteperiodeRequest(
        tilleggsopplysninger = tilleggsopplysninger,
        sykmeldingKafkaMessage = sykmeldingKafkaMessage,
    )

data class Tilleggsopplysninger(
    val egenmeldingsperioder: List<Datospenn>?,
)

data class Datospenn(
    val fom: LocalDate,
    val tom: LocalDate,
)
