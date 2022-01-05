package no.nav.helse.flex.syketilfelle.sykeforloep

import no.nav.helse.flex.syketilfelle.Testoppsett
import no.nav.helse.flex.syketilfelle.hentSykeforloepMedLoginserviceToken
import no.nav.helse.flex.syketilfelle.sykmelding.domain.MottattSykmeldingKafkaMessage
import no.nav.helse.flex.syketilfelle.sykmelding.domain.SykmeldingKafkaMessage
import no.nav.helse.flex.syketilfelle.sykmelding.skapArbeidsgiverSykmelding
import no.nav.syfo.model.sykmelding.arbeidsgiver.ArbeidsgiverSykmelding
import no.nav.syfo.model.sykmelding.arbeidsgiver.SykmeldingsperiodeAGDTO
import no.nav.syfo.model.sykmelding.model.PeriodetypeDTO
import no.nav.syfo.model.sykmeldingstatus.KafkaMetadataDTO
import no.nav.syfo.model.sykmeldingstatus.STATUS_SENDT
import no.nav.syfo.model.sykmeldingstatus.SykmeldingStatusKafkaEventDTO
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit.SECONDS

class SykeforloepTest : Testoppsett() {

    private final val fnr = "12345432123"

    private final val basisDato = LocalDate.of(2020, 3, 12)

    @BeforeEach
    @AfterEach
    fun setUp() {
        syketilfellebitRepository.deleteAll()
    }

    @Test
    fun `beregner for det enkleste tilfellet`() {

        val sykmelding = skapArbeidsgiverSykmelding(fom = basisDato.minusDays(12), tom = basisDato)

        opprettMottattSykmelding(sykmelding)
        opprettSendtSykmelding(sykmelding)

        await().atMost(10, SECONDS).until {
            syketilfellebitRepository.findByFnr(fnr).size == 2
        }
        val sykeforloep = hentSykeforloepMedLoginserviceToken(fnr)

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
    }

    @Test
    fun `beregner for et forloep med mange etterfølgende sykmeldinger`() {

        val sykmelding = skapArbeidsgiverSykmelding(fom = basisDato.minusDays(12), tom = basisDato)
        val sykmelding2 = skapArbeidsgiverSykmelding(fom = basisDato.minusDays(24), tom = basisDato.minusDays(13))
        val sykmelding3 = skapArbeidsgiverSykmelding(fom = basisDato.minusDays(40), tom = basisDato.minusDays(25))

        opprettMottattSykmelding(sykmelding)
        opprettMottattSykmelding(sykmelding2)
        opprettMottattSykmelding(sykmelding3)
        opprettSendtSykmelding(sykmelding)

        await().atMost(10, SECONDS).until {
            syketilfellebitRepository.findByFnr(fnr).size == 4
        }
        val sykeforloep = hentSykeforloepMedLoginserviceToken(fnr)

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
    }

    @Test
    fun `15 dager i mellom er samme forloep`() {

        val sykmelding = skapArbeidsgiverSykmelding(fom = basisDato, tom = basisDato.plusDays(10))
        val sykmelding2 = skapArbeidsgiverSykmelding(fom = basisDato.minusDays(30), tom = basisDato.minusDays(16))

        opprettMottattSykmelding(sykmelding)
        opprettMottattSykmelding(sykmelding2)
        opprettSendtSykmelding(sykmelding)

        await().atMost(10, SECONDS).until {
            syketilfellebitRepository.findByFnr(fnr).size == 3
        }
        val sykeforloep = hentSykeforloepMedLoginserviceToken(fnr)

        assertThat(sykeforloep).hasSize(1)
        assertThat(sykeforloep[0].oppfolgingsdato).isEqualTo(basisDato.minusDays(30))
    }

    @Test
    fun `16 dager i mellom er to forloep`() {

        val sykmelding = skapArbeidsgiverSykmelding(fom = basisDato, tom = basisDato.plusDays(10))
        val sykmelding2 = skapArbeidsgiverSykmelding(fom = basisDato.minusDays(30), tom = basisDato.minusDays(17))

        opprettMottattSykmelding(sykmelding)
        opprettMottattSykmelding(sykmelding2)
        opprettSendtSykmelding(sykmelding)

        await().atMost(10, SECONDS).until {
            syketilfellebitRepository.findByFnr(fnr).size == 3
        }
        val sykeforloep = hentSykeforloepMedLoginserviceToken(fnr)

        assertThat(sykeforloep).hasSize(2)
        assertThat(sykeforloep[0].oppfolgingsdato).isEqualTo(basisDato.minusDays(30))
        assertThat(sykeforloep[1].oppfolgingsdato).isEqualTo(basisDato)
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
        opprettMottattSykmelding(sykmelding)
        opprettSendtSykmelding(sykmelding)
git st
        await().atMost(10, SECONDS).until {
            syketilfellebitRepository.findByFnr(fnr).size == 4
        }
        val sykeforloep = hentSykeforloepMedLoginserviceToken(fnr)

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

    fun opprettSendtSykmelding(sykmelding: ArbeidsgiverSykmelding): String {

        val kafkaMessage = opprettSykmeldingKafkaMessage(sykmelding)
        producerPåSendtBekreftetTopic(kafkaMessage)
        return sykmelding.id
    }

    fun opprettMottattSykmelding(sykmelding: ArbeidsgiverSykmelding): String {

        val kafkaMessage = opprettSykmeldingKafkaMessage(sykmelding)
        val apenSykmeldingKafkaMessage = MottattSykmeldingKafkaMessage(
            sykmelding = kafkaMessage.sykmelding,
            kafkaMetadata = kafkaMessage.kafkaMetadata
        )
        producerPåMottattTopic(apenSykmeldingKafkaMessage)
        return sykmelding.id
    }

    fun opprettSykmeldingKafkaMessage(sykmelding: ArbeidsgiverSykmelding): SykmeldingKafkaMessage {

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
