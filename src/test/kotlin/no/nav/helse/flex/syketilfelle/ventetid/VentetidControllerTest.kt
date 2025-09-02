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
    fun `Authorization for ventetid feiler hvis audience er feil`() {
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .get("/api/bruker/v2/ventetid/$sykmeldingId/erUtenforVentetid")
                    .header("Authorization", "Bearer ${server.tokenxToken(fnr = fnr, audience = "facebook")}")
                    .contentType(MediaType.APPLICATION_JSON),
            ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    fun `Authorization for ventetid feiler hvis token mangler`() {
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .get("/api/bruker/v2/ventetid/$sykmeldingId/erUtenforVentetid")
                    .contentType(MediaType.APPLICATION_JSON),
            ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    fun `Authorization for ventetid feiler hvis clientId er feil`() {
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .get("/api/bruker/v2/ventetid/$sykmeldingId/erUtenforVentetid")
                    .header("Authorization", "Bearer ${server.tokenxToken(fnr = fnr, clientId = "facebook")}")
                    .contentType(MediaType.APPLICATION_JSON),
            ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    fun `Authorization for venteperiode feiler hvis audience er feil`() {
        val venteperiodeRequest = VenteperiodeRequest()
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .post("/api/v1/ventetid/$sykmeldingId/venteperiode")
                    .header(
                        "Authorization",
                        "Bearer ${server.azureToken(subject = "sykepengesoknad-backend-client-id", audience = "facebook")}",
                    ).header("fnr", fnr)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(venteperiodeRequest.serialisertTilString()),
            ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    fun `Authorization for venteperiode feiler hvis token mangler`() {
        val venteperiodeRequest = VenteperiodeRequest()
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .post("/api/v1/ventetid/$sykmeldingId/venteperiode")
                    .header("fnr", fnr)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(venteperiodeRequest.serialisertTilString()),
            ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    fun `Authorization for venteperiode feiler hvis clientId er feil`() {
        val venteperiodeRequest = VenteperiodeRequest()
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .post("/api/v1/ventetid/$sykmeldingId/venteperiode")
                    .header("Authorization", "Bearer ${server.azureToken(subject = "facebook")}")
                    .header("fnr", fnr)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(venteperiodeRequest.serialisertTilString()),
            ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    fun `Ugyldig sykmeldingId returnerer 400 BadRequest ved kall til erUtenforVentetid`() {
        val sykemeldingId = "er-ikke-uuid"
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .get("/api/bruker/v2/ventetid/$sykemeldingId/erUtenforVentetid")
                    .header("Authorization", "Bearer ${server.tokenxToken(fnr = fnr)}")
                    .contentType(MediaType.APPLICATION_JSON),
            ).andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    fun `Uppercase sykmeldingId er gyldig for erUtenforVentetidfor`() {
        val sykemeldingId = sykmeldingId.uppercase()
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .get("/api/bruker/v2/ventetid/$sykemeldingId/erUtenforVentetid")
                    .header("Authorization", "Bearer ${server.tokenxToken(fnr = fnr)}")
                    .contentType(MediaType.APPLICATION_JSON),
            ).andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    fun `Ugyldig sykmeldingId returnerer 400 BadRequest ved kall til venteperiode`() {
        val sykemeldingId = "er-ikke-uuid"
        val venteperiodeRequest = VenteperiodeRequest()
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .post("/api/v1/ventetid/$sykemeldingId/venteperiode")
                    .header("Authorization", "Bearer ${server.azureToken(subject = "sykepengesoknad-backend-client-id")}")
                    .header("fnr", fnr)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(venteperiodeRequest.serialisertTilString()),
            ).andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    fun `Avvik mellom sykmeldingId i path og body returnerer 400 BadRequest for erUtenforVentetid`() {
        val sykmeldingId = UUID.randomUUID().toString()
        val request = VentetidRequest(sykmeldingKafkaMessage = skapSykmeldingKafkaMessage())

        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .post("/api/v1/ventetid/$sykmeldingId/erUtenforVentetid")
                    .header("Authorization", "Bearer ${server.azureToken(subject = "sykepengesoknad-backend-client-id")}")
                    .header("fnr", fnr)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request.serialisertTilString()),
            ).andExpect(MockMvcResultMatchers.status().isInternalServerError)
    }

    @Test
    fun `Avvik mellom sykmeldingId i path og body returnerer 400 BadRequest for venteperiode`() {
        val sykmeldingId = UUID.randomUUID().toString()
        val request = VenteperiodeRequest(sykmeldingKafkaMessage = skapSykmeldingKafkaMessage())

        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .post("/api/v1/ventetid/$sykmeldingId/venteperiode")
                    .header("Authorization", "Bearer ${server.azureToken(subject = "sykepengesoknad-backend-client-id")}")
                    .header("fnr", fnr)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request.serialisertTilString()),
            ).andExpect(MockMvcResultMatchers.status().isInternalServerError)
    }

    @Test
    fun `Samme UUID i path og body med ulik case er OK for erUtenforVentetid`() {
        val sykmeldingKafkaMessage = skapSykmeldingKafkaMessage()
        val sykmeldignId = sykmeldingKafkaMessage.sykmelding.id.uppercase()
        val request = VentetidRequest(sykmeldingKafkaMessage = sykmeldingKafkaMessage)

        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .post("/api/v1/ventetid/$sykmeldignId/erUtenforVentetid")
                    .header("Authorization", "Bearer ${server.azureToken(subject = "sykepengesoknad-backend-client-id")}")
                    .header("fnr", fnr)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request.serialisertTilString()),
            ).andExpect(MockMvcResultMatchers.status().isOk)
    }

    @Test
    fun `Samme UUID i path og body med ulik case er OK for venteperiode`() {
        val sykmeldingKafkaMessage = skapSykmeldingKafkaMessage()
        val sykmeldignId = sykmeldingKafkaMessage.sykmelding.id.uppercase()
        val request = VenteperiodeRequest(sykmeldingKafkaMessage = sykmeldingKafkaMessage)

        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .post("/api/v1/ventetid/$sykmeldignId/venteperiode")
                    .header("Authorization", "Bearer ${server.azureToken(subject = "sykepengesoknad-backend-client-id")}")
                    .header("fnr", fnr)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request.serialisertTilString()),
            ).andExpect(MockMvcResultMatchers.status().isOk)
    }
}
