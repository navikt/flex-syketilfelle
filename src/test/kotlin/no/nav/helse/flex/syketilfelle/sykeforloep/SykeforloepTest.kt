package no.nav.helse.flex.syketilfelle.sykeforloep

import no.nav.helse.flex.syketilfelle.Testoppsett
import no.nav.helse.flex.syketilfelle.azureToken
import no.nav.helse.flex.syketilfelle.hentSykeforloep
import no.nav.helse.flex.syketilfelle.hentSykeforloepSomBruker
import no.nav.helse.flex.syketilfelle.sykmelding.domain.MottattSykmeldingKafkaMessage
import no.nav.helse.flex.syketilfelle.sykmelding.domain.SykmeldingKafkaMessage
import no.nav.helse.flex.syketilfelle.sykmelding.skapArbeidsgiverSykmelding
import no.nav.syfo.model.sykmelding.arbeidsgiver.ArbeidsgiverSykmelding
import no.nav.syfo.model.sykmelding.arbeidsgiver.SykmeldingsperiodeAGDTO
import no.nav.syfo.model.sykmelding.model.PeriodetypeDTO
import no.nav.syfo.model.sykmeldingstatus.KafkaMetadataDTO
import no.nav.syfo.model.sykmeldingstatus.STATUS_SENDT
import no.nav.syfo.model.sykmeldingstatus.SykmeldingStatusKafkaEventDTO
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should not be equal to`
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit.SECONDS

class SykeforloepTest : Testoppsett() {

    private final val fnr = "12345432123"
    private final val nyttFnr = "22345432123"

    private final val basisDato = LocalDate.of(2020, 3, 12)

    @BeforeEach
    @AfterEach
    fun setUp() {
        syketilfellebitRepository.deleteAll()
    }

    @Test
    fun `beregner for det enkleste tilfellet`() {

        val sykmelding = skapArbeidsgiverSykmelding(fom = basisDato.minusDays(12), tom = basisDato)

        opprettMottattSykmelding(sykmelding, fnr)
        opprettSendtSykmelding(sykmelding, nyttFnr)

        await().atMost(10, SECONDS).until {
            syketilfellebitRepository.findByFnr(fnr).size + syketilfellebitRepository.findByFnr(nyttFnr).size == 2
        }
        val sykeforloep = hentSykeforloepSomBruker(nyttFnr)

        assertThat(sykeforloep).hasSize(1)
        assertThat(sykeforloep[0].oppfolgingsdato).isEqualTo(basisDato.minusDays(12))
        assertThat(sykeforloep[0].sykmeldinger.toList()).isEqualTo(
            listOf(
                SimpleSykmelding(
                    fom = basisDato.minusDays(12),
                    tom = basisDato,
                    id = sykmelding.id

                )
            )
        )

        val sykeforloepMaskin = hentSykeforloep(listOf(nyttFnr), hentAndreIdenter = true)
        sykeforloepMaskin `should be equal to` sykeforloep
    }

    @Test
    fun `beregner for et forloep med mange etterfølgende sykmeldinger`() {

        val sykmelding = skapArbeidsgiverSykmelding(fom = basisDato.minusDays(12), tom = basisDato)
        val sykmelding2 = skapArbeidsgiverSykmelding(fom = basisDato.minusDays(24), tom = basisDato.minusDays(13))
        val sykmelding3 = skapArbeidsgiverSykmelding(fom = basisDato.minusDays(40), tom = basisDato.minusDays(25))

        opprettMottattSykmelding(sykmelding, fnr)
        opprettMottattSykmelding(sykmelding2, fnr)
        opprettMottattSykmelding(sykmelding3, fnr)
        opprettSendtSykmelding(sykmelding, fnr)

        await().atMost(10, SECONDS).until {
            syketilfellebitRepository.findByFnr(fnr).size == 4
        }
        val sykeforloep = hentSykeforloepSomBruker(nyttFnr)

        assertThat(sykeforloep).hasSize(1)
        assertThat(sykeforloep[0].oppfolgingsdato).isEqualTo(basisDato.minusDays(40))
        assertThat(sykeforloep[0].sykmeldinger.toList()).isEqualTo(
            listOf(

                SimpleSykmelding(
                    fom = basisDato.minusDays(40),
                    tom = basisDato.minusDays(25),
                    id = sykmelding3.id
                ),
                SimpleSykmelding(
                    fom = basisDato.minusDays(24),
                    tom = basisDato.minusDays(13),
                    id = sykmelding2.id
                ),
                SimpleSykmelding(
                    fom = basisDato.minusDays(12),
                    tom = basisDato,
                    id = sykmelding.id
                )
            )
        )

        val sykeforloepMaskin = hentSykeforloep(listOf(nyttFnr), hentAndreIdenter = true)
        sykeforloepMaskin `should be equal to` sykeforloep
    }

    @Test
    fun `15 dager i mellom er samme forloep`() {

        val sykmelding = skapArbeidsgiverSykmelding(fom = basisDato, tom = basisDato.plusDays(10))
        val sykmelding2 = skapArbeidsgiverSykmelding(fom = basisDato.minusDays(30), tom = basisDato.minusDays(16))

        opprettMottattSykmelding(sykmelding, fnr)
        opprettMottattSykmelding(sykmelding2, fnr)
        opprettSendtSykmelding(sykmelding, fnr)

        await().atMost(10, SECONDS).until {
            syketilfellebitRepository.findByFnr(fnr).size == 3
        }
        val sykeforloep = hentSykeforloepSomBruker(fnr)

        assertThat(sykeforloep).hasSize(1)
        assertThat(sykeforloep[0].oppfolgingsdato).isEqualTo(basisDato.minusDays(30))
    }

    @Test
    fun `16 dager i mellom er to forloep`() {

        val sykmelding = skapArbeidsgiverSykmelding(fom = basisDato, tom = basisDato.plusDays(10))
        val sykmelding2 = skapArbeidsgiverSykmelding(fom = basisDato.minusDays(30), tom = basisDato.minusDays(17))

        opprettMottattSykmelding(sykmelding, fnr)
        opprettMottattSykmelding(sykmelding2, nyttFnr)
        opprettSendtSykmelding(sykmelding, fnr)

        await().atMost(10, SECONDS).until {
            syketilfellebitRepository.findByFnr(fnr).size + syketilfellebitRepository.findByFnr(nyttFnr).size == 3
        }
        val sykeforloep = hentSykeforloepSomBruker(nyttFnr)

        assertThat(sykeforloep).hasSize(2)
        assertThat(sykeforloep[0].oppfolgingsdato).isEqualTo(basisDato.minusDays(30))
        assertThat(sykeforloep[1].oppfolgingsdato).isEqualTo(basisDato)

        val sykeforloepMaskin = hentSykeforloep(listOf(nyttFnr), hentAndreIdenter = true)
        sykeforloepMaskin `should be equal to` sykeforloep

        sykeforloepMaskin `should not be equal to` hentSykeforloep(listOf(nyttFnr), hentAndreIdenter = false)
        sykeforloepMaskin `should not be equal to` hentSykeforloep(listOf(fnr), hentAndreIdenter = false)
        sykeforloepMaskin `should be equal to` hentSykeforloep(listOf(nyttFnr, fnr), hentAndreIdenter = false)
    }

    @Test
    fun `sykmelding med flere perioder settes korrekt sammen`() {

        val sykmelding = skapArbeidsgiverSykmelding().copy(
            sykmeldingsperioder = listOf(
                SykmeldingsperiodeAGDTO(
                    fom = basisDato,
                    tom = basisDato.plusDays(5),
                    type = PeriodetypeDTO.AKTIVITET_IKKE_MULIG,
                    reisetilskudd = false,
                    aktivitetIkkeMulig = null,
                    behandlingsdager = null,
                    gradert = null,
                    innspillTilArbeidsgiver = null
                ),
                SykmeldingsperiodeAGDTO(
                    fom = basisDato.minusDays(13),
                    tom = basisDato.minusDays(1),
                    type = PeriodetypeDTO.GRADERT,
                    reisetilskudd = false,
                    aktivitetIkkeMulig = null,
                    behandlingsdager = null,
                    gradert = null,
                    innspillTilArbeidsgiver = null
                )
            )
        )
        opprettMottattSykmelding(sykmelding, fnr)
        opprettSendtSykmelding(sykmelding, fnr)
        await().atMost(10, SECONDS).until {
            syketilfellebitRepository.findByFnr(fnr).size == 4
        }
        val sykeforloep = hentSykeforloepSomBruker(fnr)

        assertThat(sykeforloep).hasSize(1)
        assertThat(sykeforloep[0].oppfolgingsdato).isEqualTo(basisDato.minusDays(13))
        assertThat(sykeforloep[0].sykmeldinger.toList()).isEqualTo(
            listOf(
                SimpleSykmelding(
                    fom = basisDato.minusDays(13),
                    tom = basisDato.plusDays(5),
                    id = sykmelding.id

                )
            )
        )
    }

    @Test
    fun `Krever fnr header som input`() {
        val json = mockMvc.perform(
            get("/api/v1/sykeforloep")
                .header("Authorization", "Bearer ${server.azureToken(subject = "syfosoknad-client-id")}")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isBadRequest).andReturn().response.contentAsString
        json `should be equal to` "{\"reason\":\"Bad Request\"}"
    }

    @Test
    fun `Krever riktig subject`() {
        val json = mockMvc.perform(
            get("/api/v1/sykeforloep")
                .header("Authorization", "Bearer ${server.azureToken(subject = "slem-app-client-id")}")
                .header("fnr", fnr)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isForbidden).andReturn().response.contentAsString
        json `should be equal to` "{\"reason\":\"UKJENT_CLIENT\"}"
    }

    @Test
    fun `Krever auth header`() {
        val json = mockMvc.perform(
            get("/api/v1/sykeforloep")
                .header("fnr", fnr)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isUnauthorized).andReturn().response.contentAsString
        json `should be equal to` "{\"reason\":\"Unauthorized\"}"
    }

    @Test
    fun `Krever gyldig fnr i input`() {
        val json = mockMvc.perform(
            get("/api/v1/sykeforloep")
                .header("Authorization", "Bearer ${server.azureToken(subject = "syfosoknad-client-id")}")
                .header("fnr", "blah")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isBadRequest).andReturn().response.contentAsString
        json `should be equal to` "{\"reason\":\"UGYLDIG_FNR\"}"
    }

    @Test
    fun `Kan ikke hente identer fra PDL hvis det er flere enn en ident i requesten`() {
        val json = mockMvc.perform(
            get("/api/v1/sykeforloep")
                .header("Authorization", "Bearer ${server.azureToken(subject = "syfosoknad-client-id")}")
                .header("fnr", "$fnr, $nyttFnr")
                .queryParam("hentAndreIdenter", true.toString())
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isBadRequest).andReturn().response.contentAsString
        json `should be equal to` "{\"reason\":\"FLERE_IDENTER_OG_HENTING\"}"
    }

    fun opprettSendtSykmelding(sykmelding: ArbeidsgiverSykmelding, fnr: String): String {

        val kafkaMessage = opprettSykmeldingKafkaMessage(sykmelding, fnr)
        producerPåSendtBekreftetTopic(kafkaMessage)
        return sykmelding.id
    }

    fun opprettMottattSykmelding(sykmelding: ArbeidsgiverSykmelding, fnr: String): String {

        val kafkaMessage = opprettSykmeldingKafkaMessage(sykmelding, fnr)
        val apenSykmeldingKafkaMessage = MottattSykmeldingKafkaMessage(
            sykmelding = kafkaMessage.sykmelding,
            kafkaMetadata = kafkaMessage.kafkaMetadata
        )
        producerPåMottattTopic(apenSykmeldingKafkaMessage)
        return sykmelding.id
    }

    fun opprettSykmeldingKafkaMessage(sykmelding: ArbeidsgiverSykmelding, fnr: String): SykmeldingKafkaMessage {

        val kafkaMetadata = KafkaMetadataDTO(
            sykmeldingId = sykmelding.id,
            fnr = fnr,
            timestamp = OffsetDateTime.now(),
            source = "Denne testen"
        )

        val event = SykmeldingStatusKafkaEventDTO(
            sykmeldingId = sykmelding.id,
            timestamp = OffsetDateTime.now(),
            statusEvent = STATUS_SENDT,
            arbeidsgiver = null,
            sporsmals = emptyList()
        )
        return SykmeldingKafkaMessage(
            sykmelding = sykmelding,
            kafkaMetadata = kafkaMetadata,
            event = event.copy(timestamp = OffsetDateTime.of(2020, 6, 20, 6, 34, 4, 0, ZoneOffset.UTC))
        )
    }
}
