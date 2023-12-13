package no.nav.helse.flex.syketilfelle

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.sykepengesoknad.kafka.SykepengesoknadDTO
import no.nav.helse.flex.syketilfelle.arbeidsgiverperiode.SoknadOgSykmelding
import no.nav.helse.flex.syketilfelle.arbeidsgiverperiode.domain.Arbeidsgiverperiode
import no.nav.helse.flex.syketilfelle.sykmelding.domain.MottattSykmeldingKafkaMessage
import no.nav.helse.flex.syketilfelle.sykmelding.domain.SykmeldingKafkaMessage
import no.nav.syfo.model.sykmelding.arbeidsgiver.ArbeidsgiverSykmelding
import no.nav.syfo.model.sykmeldingstatus.ArbeidsgiverStatusDTO
import no.nav.syfo.model.sykmeldingstatus.KafkaMetadataDTO
import no.nav.syfo.model.sykmeldingstatus.STATUS_SENDT
import no.nav.syfo.model.sykmeldingstatus.SykmeldingStatusKafkaEventDTO
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.time.OffsetDateTime
import java.time.ZoneOffset

fun Testoppsett.kallArbeidsgiverperiodeApi(
    soknad: SykepengesoknadDTO,
    sykmelding: SykmeldingKafkaMessage? = null,
    expectNoContent: Boolean = false,
    forelopig: Boolean = true,
    fnr: String,
): Arbeidsgiverperiode? {
    val requestBody = SoknadOgSykmelding(soknad, sykmelding)
    val result =
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v2/arbeidsgiverperiode")
                .header("Authorization", "Bearer ${server.azureToken(subject = "sykepengesoknad-backend-client-id")}")
                .header("fnr", fnr)
                .header("forelopig", forelopig.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)),
        )
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(
                if (expectNoContent) {
                    MockMvcResultMatchers.status().isNoContent
                } else {
                    MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                },
            )
            .andReturn()
    return result.response.contentAsString.takeIf { it.isNotBlank() }
        ?.let { objectMapper.readValue(it) }
}

fun Testoppsett.opprettSendtSykmelding(
    sykmelding: ArbeidsgiverSykmelding,
    fnr: String,
    orgnummer: String? = null,
): String {
    val kafkaMessage = opprettSykmeldingKafkaMessage(sykmelding, fnr, orgnummer)
    producerPåSendtBekreftetTopic(kafkaMessage)
    return sykmelding.id
}

fun Testoppsett.opprettMottattSykmelding(
    sykmelding: ArbeidsgiverSykmelding,
    fnr: String,
): String {
    val kafkaMessage = opprettSykmeldingKafkaMessage(sykmelding = sykmelding, fnr = fnr)
    val apenSykmeldingKafkaMessage =
        MottattSykmeldingKafkaMessage(
            sykmelding = kafkaMessage.sykmelding,
            kafkaMetadata = kafkaMessage.kafkaMetadata,
        )
    producerPåMottattTopic(apenSykmeldingKafkaMessage)
    return sykmelding.id
}

private fun opprettSykmeldingKafkaMessage(
    sykmelding: ArbeidsgiverSykmelding,
    fnr: String,
    orgnummer: String? = null,
): SykmeldingKafkaMessage {
    val kafkaMetadata =
        KafkaMetadataDTO(
            sykmeldingId = sykmelding.id,
            fnr = fnr,
            timestamp = OffsetDateTime.now(),
            source = "Denne testen",
        )

    val arbeidsgiver =
        if (orgnummer != null) {
            ArbeidsgiverStatusDTO(orgnummer = orgnummer, orgNavn = "$orgnummer sitt navn")
        } else {
            null
        }

    val event =
        SykmeldingStatusKafkaEventDTO(
            sykmeldingId = sykmelding.id,
            timestamp = OffsetDateTime.now(),
            statusEvent = STATUS_SENDT,
            arbeidsgiver = arbeidsgiver,
            sporsmals = emptyList(),
        )
    return SykmeldingKafkaMessage(
        sykmelding = sykmelding,
        kafkaMetadata = kafkaMetadata,
        event = event.copy(timestamp = OffsetDateTime.of(2020, 6, 20, 6, 34, 4, 0, ZoneOffset.UTC)),
    )
}
