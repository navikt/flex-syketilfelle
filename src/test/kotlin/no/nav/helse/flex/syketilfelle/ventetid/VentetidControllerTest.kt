package no.nav.helse.flex.syketilfelle.ventetid

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.syketilfelle.FellesTestOppsett
import no.nav.helse.flex.syketilfelle.azureToken
import no.nav.helse.flex.syketilfelle.erUtenforVentetid
import no.nav.helse.flex.syketilfelle.erUtenforVentetidSomBruker
import no.nav.helse.flex.syketilfelle.finnPerioderMedSammeVentetid
import no.nav.helse.flex.syketilfelle.finnPerioderMedSammeVentetidSomBruker
import no.nav.helse.flex.syketilfelle.lagBekreftetSykmeldingKafkaMessage
import no.nav.helse.flex.syketilfelle.lagMottattSykmeldingKafkaMessage
import no.nav.helse.flex.syketilfelle.lagSyketilfelleBit
import no.nav.helse.flex.syketilfelle.objectMapper
import no.nav.helse.flex.syketilfelle.syketilfellebit.Tag
import no.nav.helse.flex.syketilfelle.tokenxToken
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should be true`
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.time.LocalDate
import java.time.Month
import java.time.OffsetDateTime
import java.util.*

class VentetidControllerTest : FellesTestOppsett() {
    private val sykmeldingId = UUID.randomUUID().toString()
    private val fnr = "11111111111"

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
            lagBekreftetSykmeldingKafkaMessage(
                fnr = fnr,
                fom = LocalDate.of(2024, Month.JULY, 1),
                tom = LocalDate.of(2024, Month.JULY, 17),
            ).also { it.prosesser() }

        erUtenforVentetid(
            listOf(fnr),
            sykmeldingId = melding.sykmelding.id,
            erUtenforVentetidRequest = ErUtenforVentetidRequest(),
        ).`should be true`()
    }

    @Test
    fun `Periode på 16 dager er innefor ventetiden som bruker`() {
        val melding =
            lagMottattSykmeldingKafkaMessage(
                fnr = fnr,
                fom = LocalDate.of(2024, Month.JULY, 1),
                tom = LocalDate.of(2024, Month.JULY, 16),
            ).also { it.prosesser() }

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
            lagMottattSykmeldingKafkaMessage(
                fnr = fnr,
                fom = LocalDate.of(2024, Month.JULY, 1),
                tom = LocalDate.of(2024, Month.JULY, 17),
            ).also { it.prosesser() }

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

    @Test
    fun `Kall til perioderMedSammeVentetid som bruker feiler hvis audience er feil`() {
        mockMvc
            .perform(
                get("/api/bruker/v2/ventetid/$sykmeldingId/perioderMedSammeVentetid")
                    .header("Authorization", "Bearer ${server.tokenxToken(fnr = fnr, audience = "facebook")}")
                    .contentType(MediaType.APPLICATION_JSON),
            ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    fun `Kall til perioderMedSammeVentetid som bruker feiler hvis token mangler`() {
        mockMvc
            .perform(
                get("/api/bruker/v2/ventetid/$sykmeldingId/perioderMedSammeVentetid")
                    .contentType(MediaType.APPLICATION_JSON),
            ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    fun `Kall til perioderMedSammeVentetid som bruker feiler hvis clientId er feil`() {
        mockMvc
            .perform(
                get("/api/bruker/v2/ventetid/$sykmeldingId/perioderMedSammeVentetid")
                    .header("Authorization", "Bearer ${server.tokenxToken(fnr = fnr, clientId = "facebook")}")
                    .contentType(MediaType.APPLICATION_JSON),
            ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    fun `Kall til perioderMedSammeVentetid feiler hvis token mangler`() {
        mockMvc
            .perform(
                post("/api/v1/ventetid/$sykmeldingId/perioderMedSammeVentetid")
                    .header("fnr", fnr)
                    .contentType(MediaType.APPLICATION_JSON),
            ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    fun `Kall til perioderMedSammeVentetid feiler hvis subject er feil`() {
        mockMvc
            .perform(
                post("/api/v1/ventetid/$sykmeldingId/perioderMedSammeVentetid")
                    .header("Authorization", "Bearer ${server.azureToken(subject = "facebook")}")
                    .header("fnr", fnr)
                    .content(objectMapper.writeValueAsString(SammeVentetidRequest()))
                    .contentType(MediaType.APPLICATION_JSON),
            ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    fun `Kall til perioderMedSammeVentetid som bruker returnerer riktig periode`() {
        val melding =
            lagMottattSykmeldingKafkaMessage(
                fnr = fnr,
                fom = LocalDate.of(2026, Month.FEBRUARY, 2),
                tom = LocalDate.of(2026, Month.FEBRUARY, 17),
            ).also { it.prosesser() }

        verifiserAtBiterErLagret(1)

        val response =
            finnPerioderMedSammeVentetidSomBruker(
                fnr = fnr,
                sykmeldingId = melding.sykmelding.id,
            )

        response.ventetidPerioder.size `should be equal to` 1
        response.ventetidPerioder.first().also {
            it.ressursId `should be equal to` melding.sykmelding.id
            it.ventetid.fom `should be equal to` LocalDate.of(2026, Month.FEBRUARY, 2)
            it.ventetid.tom `should be equal to` LocalDate.of(2026, Month.FEBRUARY, 17)
        }
    }

    @Test
    fun `Kall til perioderMedSammeVentetid returnerer riktig periode`() {
        val melding =
            lagMottattSykmeldingKafkaMessage(
                fnr = fnr,
                fom = LocalDate.of(2026, Month.FEBRUARY, 2),
                tom = LocalDate.of(2026, Month.FEBRUARY, 17),
            ).also { it.prosesser() }

        verifiserAtBiterErLagret(1)

        val response =
            finnPerioderMedSammeVentetid(
                fnr = listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
            )

        response.ventetidPerioder.size `should be equal to` 1
        response.ventetidPerioder.first().also {
            it.ressursId `should be equal to` melding.sykmelding.id
            it.ventetid.fom `should be equal to` LocalDate.of(2026, Month.FEBRUARY, 2)
            it.ventetid.tom `should be equal to` LocalDate.of(2026, Month.FEBRUARY, 17)
        }
    }

    @Test
    fun `Kall til perioderMedSammeVentetid returnerer tom liste hvis fnr ikke matcher biter`() {
        val melding =
            lagMottattSykmeldingKafkaMessage(
                fnr = fnr,
                fom = LocalDate.of(2026, Month.FEBRUARY, 2),
                tom = LocalDate.of(2026, Month.FEBRUARY, 17),
            ).also { it.prosesser() }

        verifiserAtBiterErLagret(1)

        mockMvc
            .perform(
                get("/api/bruker/v2/ventetid/${melding.sykmelding.id}/perioderMedSammeVentetid")
                    .header(
                        "Authorization",
                        "Bearer ${server.tokenxToken(fnr = "99999999999", clientId = "backend-client-id")}",
                    ).contentType(MediaType.APPLICATION_JSON),
            ).andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.ventetidPerioder").isEmpty)
    }

    @Test
    fun `Kall til flex-internal ventetid-API feiler hvis token mangler`() {
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .get("/api/v1/flex/ventetid/$sykmeldingId")
                    .contentType(MediaType.APPLICATION_JSON),
            ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    fun `Kall til flex-internal ventetid-API feiler hvis subject er feil`() {
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .get("/api/v1/flex/ventetid/$sykmeldingId")
                    .header("Authorization", "Bearer ${server.azureToken(subject = "facebook")}")
                    .contentType(MediaType.APPLICATION_JSON),
            ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    fun `Kall til flex-internal ventetid-API returnerer korrekt ventetid`() {
        val tid = OffsetDateTime.parse("2025-09-01T00:00:00.000000Z")
        lagSyketilfelleBit(
            fnr = fnr,
            ressursId = sykmeldingId,
            fom = LocalDate.of(2025, Month.SEPTEMBER, 1),
            tom = LocalDate.of(2025, Month.SEPTEMBER, 18),
            tags = listOf(Tag.SYKMELDING, Tag.BEKREFTET, Tag.PERIODE, Tag.INGEN_AKTIVITET),
            opprettet = tid,
        ).also { syketilfellebitRepository.save(it) }

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
            )
        respons `should be equal to` forventetResponse
    }

    @Test
    fun `Kall til flex-internal syketilfellebit-API returnerer korrekt resultat`() {
        val tid = OffsetDateTime.parse("2025-09-01T00:00:00.000000Z")
        val syketilfelleBit =
            lagSyketilfelleBit(
                fnr = fnr,
                ressursId = sykmeldingId,
                fom = LocalDate.of(2025, Month.SEPTEMBER, 1),
                tom = LocalDate.of(2025, Month.SEPTEMBER, 18),
                tags = listOf(Tag.SYKMELDING, Tag.BEKREFTET, Tag.PERIODE, Tag.INGEN_AKTIVITET),
                opprettet = tid,
            ).also { syketilfellebitRepository.save(it) }

        val json =
            mockMvc
                .perform(
                    MockMvcRequestBuilders
                        .post("/api/v1/flex/syketilfellebiter")
                        .header(
                            "Authorization",
                            "Bearer ${server.azureToken(subject = "flex-internal-frontend-client-id")}",
                        ).header("fnr", fnr)
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()
                .response.contentAsString

        val respons: SyketilfellebitResponse = objectMapper.readValue(json)

        val forventetResponse =
            SyketilfellebitResponse(
                syketilfellebiter =
                    listOf(
                        SyketilfellebitInternal(
                            syketilfellebitId = syketilfelleBit.syketilfellebitId,
                            fnr = fnr,
                            opprettet = tid,
                            inntruffet = tid,
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

    @Test
    fun `Kall til erUtenforVentetifeiler hvis sykmeldingId i path ikke er liksykmeldingId i Kafka-melding`() {
        val sykmeldingIdIPathen = UUID.randomUUID().toString()
        val melding = lagBekreftetSykmeldingKafkaMessage(fnr = fnr, fom = LocalDate.now(), tom = LocalDate.now().plusDays(10))

        mockMvc
            .perform(
                post("/api/v1/ventetid/$sykmeldingIdIPathen/erUtenforVentetid")
                    .header("Authorization", "Bearer ${server.azureToken(subject = "sykepengesoknad-backend-client-id")}")
                    .header("fnr", fnr)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(ErUtenforVentetidRequest(sykmeldingKafkaMessage = melding))),
            ).andExpect(MockMvcResultMatchers.status().isInternalServerError)
    }

    @Test
    fun `Kall til perioderMedSammeVentetid feiler hvis sykmeldingId i path ikke er lik sykmeldingId i Kafka-melding`() {
        val sykmeldingIdIPathen = UUID.randomUUID().toString()
        val melding = lagBekreftetSykmeldingKafkaMessage(fnr = fnr, fom = LocalDate.now(), tom = LocalDate.now().plusDays(10))

        mockMvc
            .perform(
                post("/api/v1/ventetid/$sykmeldingIdIPathen/perioderMedSammeVentetid")
                    .header("Authorization", "Bearer ${server.azureToken(subject = "sykepengesoknad-backend-client-id")}")
                    .header("fnr", fnr)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(SammeVentetidRequest(sykmeldingKafkaMessage = melding))),
            ).andExpect(MockMvcResultMatchers.status().isInternalServerError)
    }
}
