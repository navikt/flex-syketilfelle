package no.nav.helse.flex.syketilfelle.ventetid

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.syketilfelle.FellesTestOppsett
import no.nav.helse.flex.syketilfelle.azureToken
import no.nav.helse.flex.syketilfelle.objectMapper
import no.nav.helse.flex.syketilfelle.sykmelding.SykmeldingLagring
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

class VentetidFlexInternalControllerTest :
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
    fun `Kall til ventetid feiler hvis token mangler`() {
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .get("/api/v1/flex/ventetid/$sykmeldingId")
                    .contentType(MediaType.APPLICATION_JSON),
            ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    fun `Kall til ventetid feiler hvis subject er feil`() {
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .get("/api/v1/flex/ventetid/$sykmeldingId")
                    .header("Authorization", "Bearer ${server.azureToken(subject = "facebook")}")
                    .contentType(MediaType.APPLICATION_JSON),
            ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    fun `Kall til ventetid returnerer korrekt resultat`() {
        """
        [
          {
            "syketilfellebitId": "43e1c0c8-6a73-419a-8a20-42a77461d1ad",
            "fnr": "$fnr",
            "opprettet": "2025-09-01T00:00:00.000000Z",
            "inntruffet": "2025-09-01T00:00:00.000000Z",
            "orgnummer": null,
            "tags": "SYKMELDING,BEKREFTET,PERIODE,INGEN_AKTIVITET",
            "ressursId": "$sykmeldingId",
            "korrigererSendtSoknad": null,
            "fom": "2025-09-01",
            "tom": "2025-09-18",
            "publisert": true,
            "slettet": null,
            "tombstonePublisert": null
          }
        ] 
        """.trimIndent().tilSyketilfellebitDbRecords().also {
            syketilfellebitRepository.saveAll(it)
        }

        val json =
            mockMvc
                .perform(
                    MockMvcRequestBuilders
                        .get("/api/v1/flex/ventetid/$sykmeldingId")
                        .header(
                            "Authorization",
                            "Bearer ${server.azureToken(subject = "flex-internal-frontend-client-id")}",
                        ).contentType(MediaType.APPLICATION_JSON),
                ).andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()
                .response.contentAsString

        val respons: VentetidInternalResponse = objectMapper.readValue(json)
        val forventetResponse =
            VentetidInternalResponse(
                erUtenforVentetid = true,
                ventetid =
                    FomTomPeriode(
                        LocalDate.of(2025, 9, 1),
                        LocalDate.of(2025, 9, 16),
                    ),
                sykmeldingsperiode =
                    FomTomPeriode(
                        LocalDate.of(2025, 9, 1),
                        LocalDate.of(2025, 9, 18),
                    ),
                syketilfellebiter =
                    listOf(
                        SyketilfellebitInternal(
                            syketilfellebitId = "43e1c0c8-6a73-419a-8a20-42a77461d1ad",
                            fnr = fnr,
                            opprettet = OffsetDateTime.parse("2025-09-01T00:00:00.000000Z"),
                            inntruffet = OffsetDateTime.parse("2025-09-01T00:00:00.000000Z"),
                            orgnummer = null,
                            tags = "SYKMELDING,BEKREFTET,PERIODE,INGEN_AKTIVITET",
                            ressursId = sykmeldingId,
                            korrigererSendtSoknad = null,
                            fom = LocalDate.of(2025, 9, 1),
                            tom = LocalDate.of(2025, 9, 18),
                            publisert = true,
                            slettet = null,
                            tombstonePublisert = null,
                        ),
                    ),
            )
        respons `should be equal to` forventetResponse
    }
}
