package no.nav.helse.flex.syketilfelle.arbeidsgiverperiode

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.sykepengesoknad.kafka.SykepengesoknadDTO
import no.nav.helse.flex.syketilfelle.FellesTestOppsett
import no.nav.helse.flex.syketilfelle.arbeidsgiverperiode.domain.Arbeidsgiverperiode
import no.nav.helse.flex.syketilfelle.azureToken
import no.nav.helse.flex.syketilfelle.objectMapper
import no.nav.helse.flex.syketilfelle.sykmelding.domain.SykmeldingKafkaMessage
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

fun FellesTestOppsett.kallArbeidsgiverperiodeApi(
    soknad: SykepengesoknadDTO,
    sykmelding: SykmeldingKafkaMessage? = null,
    expectNoContent: Boolean = false,
    forelopig: Boolean = true,
    fnr: String,
): Arbeidsgiverperiode? {
    val requestBody = SoknadOgSykmelding(soknad, sykmelding)
    val result =
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .post("/api/v2/arbeidsgiverperiode")
                    .header("Authorization", "Bearer ${server.azureToken(subject = "sykepengesoknad-backend-client-id")}")
                    .header("fnr", fnr)
                    .header("forelopig", forelopig.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestBody)),
            ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(
                if (expectNoContent) {
                    MockMvcResultMatchers.status().isNoContent
                } else {
                    MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                },
            ).andReturn()
    return result.response.contentAsString
        .takeIf { it.isNotBlank() }
        ?.let { objectMapper.readValue(it) }
}
