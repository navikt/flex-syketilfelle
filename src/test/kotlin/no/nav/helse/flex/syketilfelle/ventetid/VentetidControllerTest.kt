package no.nav.helse.flex.syketilfelle.ventetid

import no.nav.helse.flex.syketilfelle.FellesTestOppsett
import no.nav.helse.flex.syketilfelle.azureToken
import no.nav.helse.flex.syketilfelle.erUtenforVentetid
import no.nav.helse.flex.syketilfelle.erUtenforVentetidSomBruker
import no.nav.helse.flex.syketilfelle.objectMapper
import no.nav.helse.flex.syketilfelle.sykmelding.SykmeldingLagring
import no.nav.helse.flex.syketilfelle.tokenxToken
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should be true`
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.time.LocalDate
import java.time.Month
import java.util.*

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
    fun `Kall til erUtenforVentetid som bruker feiler hvis audience er feil`() {
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .get("/api/bruker/v2/ventetid/$sykmeldingId/erUtenforVentetid")
                    .header("Authorization", "Bearer ${server.tokenxToken(fnr = fnr, audience = "facebook")}")
                    .contentType(MediaType.APPLICATION_JSON),
            ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    fun `Kall til erUtenforVentetid som bruker feiler hvis token mangler`() {
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .get("/api/bruker/v2/ventetid/$sykmeldingId/erUtenforVentetid")
                    .contentType(MediaType.APPLICATION_JSON),
            ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    fun `Kall til erUtenforVentetid som bruker feiler hvis clientId er feil`() {
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .get("/api/bruker/v2/ventetid/$sykmeldingId/erUtenforVentetid")
                    .header("Authorization", "Bearer ${server.tokenxToken(fnr = fnr, clientId = "facebook")}")
                    .contentType(MediaType.APPLICATION_JSON),
            ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    fun `Kall til erUtenforVentetid feiler hvis token mangler er feil`() {
        mockMvc
            .perform(
                post("/api/v1/ventetid/$sykmeldingId/erUtenforVentetid")
                    .header("fnr", fnr)
                    .content(objectMapper.writeValueAsString(ErUtenforVentetidRequest()))
                    .contentType(MediaType.APPLICATION_JSON),
            ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    fun `Kall til erUtenforVentetid feiler hvis subject er feil`() {
        mockMvc
            .perform(
                post("/api/v1/ventetid/$sykmeldingId/erUtenforVentetid")
                    .header("Authorization", "Bearer ${server.azureToken(subject = "facebook")}")
                    .header("fnr", fnr)
                    .content(objectMapper.writeValueAsString(ErUtenforVentetidRequest()))
                    .contentType(MediaType.APPLICATION_JSON),
            ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    fun `Periode på 17 dager er utenfor ventetiden`() {
        val melding =
            skapSykmeldingKafkaMessage(
                fom = LocalDate.of(2024, Month.JULY, 1),
                tom = LocalDate.of(2024, Month.JULY, 17),
            ).also { it.publiser() }

        verifiserAtBiterErLagret(1)

        erUtenforVentetid(
            listOf(fnr),
            sykmeldingId = melding.sykmelding.id,
            erUtenforVentetidRequest = ErUtenforVentetidRequest(),
        ).`should be true`()
    }

    @Test
    fun `Periode på 16 dager er innefor ventetiden som bruker`() {
        val melding =
            skapApenSykmeldingKafkaMessage(
                fom = LocalDate.of(2024, Month.JULY, 1),
                tom = LocalDate.of(2024, Month.JULY, 16),
            ).also { it.publiser() }

        verifiserAtBiterErLagret(1)

        erUtenforVentetidSomBruker(
            fnr,
            sykmeldingId = melding.sykmelding.id,
        ).also {
            it.erUtenforVentetid `should be` false
            it.oppfolgingsdato `should be equal to` LocalDate.of(2024, Month.JULY, 1)
            it.ventetid `should be equal to`
                FomTomPeriode(
                    LocalDate.of(2024, Month.JULY, 1),
                    LocalDate.of(2024, Month.JULY, 16),
                )
        }
    }

    @Test
    fun `Periode på 17 dager er utenfor ventetiden som bruker`() {
        val melding =
            skapApenSykmeldingKafkaMessage(
                fom = LocalDate.of(2024, Month.JULY, 1),
                tom = LocalDate.of(2024, Month.JULY, 17),
            ).also { it.publiser() }

        verifiserAtBiterErLagret(1)

        erUtenforVentetidSomBruker(
            fnr,
            sykmeldingId = melding.sykmelding.id,
        ).also {
            it.erUtenforVentetid `should be` true
            it.oppfolgingsdato `should be equal to` LocalDate.of(2024, Month.JULY, 1)
            it.ventetid `should be equal to`
                FomTomPeriode(
                    LocalDate.of(2024, Month.JULY, 1),
                    LocalDate.of(2024, Month.JULY, 16),
                )
        }
    }
}
