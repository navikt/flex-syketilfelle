package no.nav.helse.flex.syketilfelle.ventetid

import no.nav.helse.flex.syketilfelle.FellesTestOppsett
import no.nav.helse.flex.syketilfelle.azureToken
import no.nav.helse.flex.syketilfelle.serialisertTilString
import no.nav.helse.flex.syketilfelle.sykmelding.SykmeldingLagring
import no.nav.helse.flex.syketilfelle.tokenxToken
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.util.UUID

class VentetidControllerTest :
    FellesTestOppsett(),
    VentetidFellesOppsett {
    @Autowired
    override lateinit var sykmeldingLagring: SykmeldingLagring

    final override val fnr = "11111111111"
    private val sykmeldingId = UUID.randomUUID().toString()

    @BeforeEach
    @AfterEach
    fun setUp() {
        syketilfellebitRepository.deleteAll()
    }

    @Test
    fun `Authorization for erUtenforVentetid feiler hvis audience er feil`() {
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .get("/api/bruker/v2/ventetid/$sykmeldingId/erUtenforVentetid")
                    .header("Authorization", "Bearer ${server.tokenxToken(fnr = fnr, audience = "facebook")}")
                    .contentType(MediaType.APPLICATION_JSON),
            ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    fun `Authorization for erUtenforVentetid feiler hvis token mangler`() {
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .get("/api/bruker/v2/ventetid/$sykmeldingId/erUtenforVentetid")
                    .contentType(MediaType.APPLICATION_JSON),
            ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    fun `Authorization for erUtenforVentetid feiler hvis clientId er feil`() {
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .get("/api/bruker/v2/ventetid/$sykmeldingId/erUtenforVentetid")
                    .header("Authorization", "Bearer ${server.tokenxToken(fnr = fnr, clientId = "facebook")}")
                    .contentType(MediaType.APPLICATION_JSON),
            ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    fun `Authorization for ventetid feiler hvis audience er feil`() {
        val ventetidRequest = VentetidRequest()
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .post("/api/v1/ventetid/$sykmeldingId/ventetid")
                    .header(
                        "Authorization",
                        "Bearer ${server.azureToken(subject = "sykepengesoknad-backend-client-id", audience = "facebook")}",
                    ).header("fnr", fnr)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(ventetidRequest.serialisertTilString()),
            ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    fun `Authorization for ventetid feiler hvis token mangler`() {
        val ventetidRequest = VentetidRequest()
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .post("/api/v1/ventetid/$sykmeldingId/ventetid")
                    .header("fnr", fnr)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(ventetidRequest.serialisertTilString()),
            ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    fun `Authorization for ventetid feiler hvis clientId er feil`() {
        val ventetidRequest = VentetidRequest()
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .post("/api/v1/ventetid/$sykmeldingId/ventetid")
                    .header("Authorization", "Bearer ${server.azureToken(subject = "facebook")}")
                    .header("fnr", fnr)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(ventetidRequest.serialisertTilString()),
            ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }
}
