package no.nav.helse.flex.syketilfelle.sykmelding

import no.nav.helse.flex.syketilfelle.Testoppsett
import no.nav.helse.flex.syketilfelle.serialisertTilString
import no.nav.helse.flex.syketilfelle.`should be equal to ignoring nano and zone`
import no.nav.helse.flex.syketilfelle.syketilfellebit.Tag
import no.nav.helse.flex.syketilfelle.syketilfellebit.tilSyketilfellebit
import no.nav.helse.flex.syketilfelle.sykmelding.domain.MottattSykmeldingKafkaMessage
import no.nav.helse.flex.syketilfelle.sykmelding.domain.SykmeldingKafkaMessage
import no.nav.syfo.model.sykmelding.arbeidsgiver.SykmeldingsperiodeAGDTO
import no.nav.syfo.model.sykmelding.model.GradertDTO
import no.nav.syfo.model.sykmelding.model.PeriodetypeDTO
import no.nav.syfo.model.sykmeldingstatus.*
import no.nav.syfo.model.sykmeldingstatus.ShortNameDTO
import no.nav.syfo.model.sykmeldingstatus.SvartypeDTO
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit.SECONDS
import java.util.concurrent.TimeUnit

class SykmeldingMottakTest : Testoppsett() {

    private final val fnr = "12345432123"

    @BeforeEach
    @AfterEach
    fun setUp() {
        syketilfellebitRepository.deleteAll()
    }

    final val sykmelding = skapArbeidsgiverSykmelding()

    final val kafkaMetadata = KafkaMetadataDTO(
        sykmeldingId = sykmelding.id,
        fnr = fnr,
        timestamp = OffsetDateTime.now(),
        source = "Denne testen"
    )

    final val event = SykmeldingStatusKafkaEventDTO(
        sykmeldingId = sykmelding.id,
        timestamp = OffsetDateTime.now(),
        statusEvent = STATUS_SENDT,
        arbeidsgiver = null,
        sporsmals = emptyList()
    )

    @Test
    fun `tar imot vanlig sykmelding`() {
        val kafkaMessage = SykmeldingKafkaMessage(
            sykmelding = sykmelding.copy(
                sykmeldingsperioder = listOf(
                    SykmeldingsperiodeAGDTO(
                        fom = LocalDate.of(2020, 3, 12),
                        tom = LocalDate.of(2020, 6, 19),
                        reisetilskudd = false,
                        type = PeriodetypeDTO.AKTIVITET_IKKE_MULIG,
                        aktivitetIkkeMulig = null,
                        behandlingsdager = null,
                        gradert = null,
                        innspillTilArbeidsgiver = null
                    )
                )
            ),
            kafkaMetadata = kafkaMetadata,
            event = event.copy(timestamp = OffsetDateTime.of(2020, 6, 20, 6, 34, 4, 0, ZoneOffset.UTC))
        )

        producerPåSendtBekreftetTopic(kafkaMessage)

        await().atMost(10, TimeUnit.SECONDS).until {
            syketilfellebitRepository.findByFnr(fnr).size == 1
        }

        val biter = syketilfellebitRepository.findByFnr(fnr).map { it.tilSyketilfellebit() }
        assertThat(biter).hasSize(1)
        assertThat(biter[0].fom).isEqualTo(LocalDate.of(2020, 3, 12))
        assertThat(biter[0].tom).isEqualTo(LocalDate.of(2020, 6, 19))
        assertThat(biter[0].ressursId).isEqualTo(sykmelding.id)
        biter[0].inntruffet `should be equal to ignoring nano and zone` OffsetDateTime.of(2020, 6, 20, 6, 34, 4, 0, ZoneOffset.UTC)
        assertThat(biter[0].orgnummer).isNull()
        assertThat(biter[0].tags).isEqualTo(setOf(Tag.SYKMELDING, Tag.SENDT, Tag.PERIODE, Tag.INGEN_AKTIVITET))
    }

    @Test
    fun `tar imot vanlig sykmelding sendt til arbeidsgiver`() {
        val kafkaMessage = SykmeldingKafkaMessage(
            sykmelding = sykmelding.copy(
                sykmeldingsperioder = listOf(
                    SykmeldingsperiodeAGDTO(
                        fom = LocalDate.of(2020, 3, 12),
                        tom = LocalDate.of(2020, 6, 19),
                        reisetilskudd = false,
                        type = PeriodetypeDTO.AKTIVITET_IKKE_MULIG,
                        aktivitetIkkeMulig = null,
                        behandlingsdager = null,
                        gradert = null,
                        innspillTilArbeidsgiver = null
                    )
                )
            ),
            kafkaMetadata = kafkaMetadata,
            event = event.copy(arbeidsgiver = ArbeidsgiverStatusDTO(orgnummer = "12344", orgNavn = "Kiwi"))
        )
        producerPåSendtBekreftetTopic(kafkaMessage)
        await().atMost(10, TimeUnit.SECONDS).until {
            syketilfellebitRepository.findByFnr(fnr).size == 1
        }
        val biter = syketilfellebitRepository.findByFnr(fnr).map { it.tilSyketilfellebit() }
        assertThat(biter).hasSize(1)
        assertThat(biter[0].fom).isEqualTo(LocalDate.of(2020, 3, 12))
        assertThat(biter[0].tom).isEqualTo(LocalDate.of(2020, 6, 19))
        assertThat(biter[0].ressursId).isEqualTo(sykmelding.id)
        assertThat(biter[0].orgnummer).isEqualTo("12344")
        assertThat(biter[0].tags).isEqualTo(setOf(Tag.SYKMELDING, Tag.SENDT, Tag.PERIODE, Tag.INGEN_AKTIVITET))
    }

    @Test
    fun `tar imot åpen sykmelding og redusert arbeidsgiverperiode`() {
        val kafkaMessage = MottattSykmeldingKafkaMessage(
            sykmelding = sykmelding.copy(
                sykmeldingsperioder = listOf(
                    SykmeldingsperiodeAGDTO(
                        fom = LocalDate.of(2020, 3, 12),
                        tom = LocalDate.of(2020, 6, 19),
                        reisetilskudd = false,
                        type = PeriodetypeDTO.AKTIVITET_IKKE_MULIG,
                        aktivitetIkkeMulig = null,
                        behandlingsdager = null,
                        gradert = null,
                        innspillTilArbeidsgiver = null
                    )
                ),
                harRedusertArbeidsgiverperiode = true
            ),
            kafkaMetadata = kafkaMetadata
        )
        producerPåMottattTopic(kafkaMessage)
        await().atMost(10, TimeUnit.SECONDS).until {
            syketilfellebitRepository.findByFnr(fnr).size == 1
        }
        val biter = syketilfellebitRepository.findByFnr(fnr).map { it.tilSyketilfellebit() }
        assertThat(biter).hasSize(1)
        assertThat(biter[0].fom).isEqualTo(LocalDate.of(2020, 3, 12))
        assertThat(biter[0].tom).isEqualTo(LocalDate.of(2020, 6, 19))
        assertThat(biter[0].ressursId).isEqualTo(sykmelding.id)
        assertThat(biter[0].orgnummer).isNull()
        assertThat(biter[0].tags).isEqualTo(setOf(Tag.SYKMELDING, Tag.NY, Tag.PERIODE, Tag.INGEN_AKTIVITET, Tag.REDUSERT_ARBEIDSGIVERPERIODE))
    }

    @Test
    fun `tar imot behandlingsdag, gradert og ukjent sendt til arbeidsgiver`() {
        val kafkaMessage = SykmeldingKafkaMessage(
            sykmelding = sykmelding.copy(
                sykmeldingsperioder = listOf(
                    SykmeldingsperiodeAGDTO(
                        fom = LocalDate.of(2020, 3, 12),
                        tom = LocalDate.of(2020, 6, 19),
                        reisetilskudd = false,
                        type = PeriodetypeDTO.BEHANDLINGSDAGER,
                        aktivitetIkkeMulig = null,
                        behandlingsdager = null,
                        gradert = null,
                        innspillTilArbeidsgiver = null

                    ),
                    SykmeldingsperiodeAGDTO(
                        fom = LocalDate.of(2020, 6, 20),
                        tom = LocalDate.of(2020, 6, 25),
                        reisetilskudd = false,
                        type = PeriodetypeDTO.GRADERT,
                        gradert = GradertDTO(3, false),
                        aktivitetIkkeMulig = null,
                        behandlingsdager = null,
                        innspillTilArbeidsgiver = null
                    ),
                    SykmeldingsperiodeAGDTO(
                        fom = LocalDate.of(2020, 6, 26),
                        tom = LocalDate.of(2020, 6, 29),
                        reisetilskudd = false,
                        type = PeriodetypeDTO.AVVENTENDE,
                        gradert = null,
                        aktivitetIkkeMulig = null,
                        behandlingsdager = null,
                        innspillTilArbeidsgiver = null
                    )
                )
            ),
            kafkaMetadata = kafkaMetadata,
            event = event.copy(arbeidsgiver = ArbeidsgiverStatusDTO(orgnummer = "12344", orgNavn = "Kiwi"))
        )
        producerPåSendtBekreftetTopic(kafkaMessage)
        await().atMost(10, TimeUnit.SECONDS).until {
            syketilfellebitRepository.findByFnr(fnr).size == 3
        }
        val biter = syketilfellebitRepository.findByFnr(fnr).map { it.tilSyketilfellebit() }.sortedBy { it.fom }
        assertThat(biter).hasSize(3)
        assertThat(biter[0].fom).isEqualTo(LocalDate.of(2020, 3, 12))
        assertThat(biter[0].tom).isEqualTo(LocalDate.of(2020, 6, 19))
        assertThat(biter[0].ressursId).isEqualTo(sykmelding.id)
        assertThat(biter[0].orgnummer).isEqualTo("12344")
        assertThat(biter[0].tags).isEqualTo(setOf(Tag.SYKMELDING, Tag.SENDT, Tag.PERIODE, Tag.BEHANDLINGSDAGER))

        assertThat(biter[1].fom).isEqualTo(LocalDate.of(2020, 6, 20))
        assertThat(biter[1].tom).isEqualTo(LocalDate.of(2020, 6, 25))
        assertThat(biter[1].ressursId).isEqualTo(sykmelding.id)
        assertThat(biter[1].orgnummer).isEqualTo("12344")
        assertThat(biter[1].tags).isEqualTo(setOf(Tag.SYKMELDING, Tag.SENDT, Tag.PERIODE, Tag.GRADERT_AKTIVITET))

        assertThat(biter[2].fom).isEqualTo(LocalDate.of(2020, 6, 26))
        assertThat(biter[2].tom).isEqualTo(LocalDate.of(2020, 6, 29))
        assertThat(biter[2].ressursId).isEqualTo(sykmelding.id)
        assertThat(biter[2].orgnummer).isEqualTo("12344")
        assertThat(biter[2].tags).isEqualTo(setOf(Tag.SYKMELDING, Tag.SENDT, Tag.PERIODE, Tag.AVVENTENDE, Tag.UKJENT_AKTIVITET))
    }

    @Test
    fun `tar imot bekreftet sykmelding med egenmeldingsperioder`() {
        val kafkaMessage = SykmeldingKafkaMessage(
            sykmelding = sykmelding.copy(
                sykmeldingsperioder = listOf(
                    SykmeldingsperiodeAGDTO(
                        fom = LocalDate.of(2020, 3, 12),
                        tom = LocalDate.of(2020, 6, 19),
                        reisetilskudd = false,
                        type = PeriodetypeDTO.AKTIVITET_IKKE_MULIG,
                        aktivitetIkkeMulig = null,
                        behandlingsdager = null,
                        gradert = null,
                        innspillTilArbeidsgiver = null
                    )
                )
            ),
            kafkaMetadata = kafkaMetadata,
            event = event.copy(
                statusEvent = STATUS_BEKREFTET,
                sporsmals = listOf(
                    SporsmalOgSvarDTO(
                        tekst = "Hvilke dager var du borte fra jobb før datoen sykmeldingen gjelder fra?",
                        shortName = ShortNameDTO.PERIODE,
                        svar = "[{\"fom\":\"2020-02-03\",\"tom\":\"2020-02-06\"}]",
                        svartype = SvartypeDTO.PERIODER
                    )
                )
            )
        )
        producerPåSendtBekreftetTopic(kafkaMessage)
        await().atMost(10, TimeUnit.SECONDS).until {
            syketilfellebitRepository.findByFnr(fnr).size == 2
        }
        val biter = syketilfellebitRepository.findByFnr(fnr).map { it.tilSyketilfellebit() }
        assertThat(biter).hasSize(2)
        assertThat(biter[0].fom).isEqualTo(LocalDate.of(2020, 3, 12))
        assertThat(biter[0].tom).isEqualTo(LocalDate.of(2020, 6, 19))
        assertThat(biter[0].ressursId).isEqualTo(sykmelding.id)
        assertThat(biter[0].orgnummer).isNull()
        assertThat(biter[0].tags).isEqualTo(setOf(Tag.SYKMELDING, Tag.BEKREFTET, Tag.PERIODE, Tag.INGEN_AKTIVITET))

        assertThat(biter[1].fom).isEqualTo(LocalDate.of(2020, 2, 3))
        assertThat(biter[1].tom).isEqualTo(LocalDate.of(2020, 2, 6))
        assertThat(biter[1].ressursId).isEqualTo(sykmelding.id)
        assertThat(biter[1].orgnummer).isNull()
        assertThat(biter[1].tags).isEqualTo(setOf(Tag.SYKMELDING, Tag.BEKREFTET, Tag.ANNET_FRAVAR))
    }

    @Test
    fun `tar imot reisetilskudd sykmelding sendt til arbeidsgiver`() {
        val kafkaMessage = SykmeldingKafkaMessage(
            sykmelding = sykmelding.copy(
                sykmeldingsperioder = listOf(
                    SykmeldingsperiodeAGDTO(
                        fom = LocalDate.of(2020, 3, 12),
                        tom = LocalDate.of(2020, 6, 19),
                        reisetilskudd = true,
                        type = PeriodetypeDTO.REISETILSKUDD,
                        aktivitetIkkeMulig = null,
                        behandlingsdager = null,
                        gradert = null,
                        innspillTilArbeidsgiver = null
                    )
                )
            ),
            kafkaMetadata = kafkaMetadata,
            event = event.copy(arbeidsgiver = ArbeidsgiverStatusDTO(orgnummer = "12344", orgNavn = "Kiwi"))
        )
        producerPåSendtBekreftetTopic(kafkaMessage)
        await().atMost(10, TimeUnit.SECONDS).until {
            syketilfellebitRepository.findByFnr(fnr).size == 1
        }
        val biter = syketilfellebitRepository.findByFnr(fnr).map { it.tilSyketilfellebit() }
        assertThat(biter).hasSize(1)
        assertThat(biter[0].fom).isEqualTo(LocalDate.of(2020, 3, 12))
        assertThat(biter[0].tom).isEqualTo(LocalDate.of(2020, 6, 19))
        assertThat(biter[0].ressursId).isEqualTo(sykmelding.id)
        assertThat(biter[0].orgnummer).isEqualTo("12344")
        biter[0].inntruffet `should be equal to ignoring nano and zone` event.timestamp
        assertThat(biter[0].tags).isEqualTo(setOf(Tag.SYKMELDING, Tag.SENDT, Tag.PERIODE, Tag.REISETILSKUDD, Tag.UKJENT_AKTIVITET))
    }

    private fun producerPåSendtBekreftetTopic(sykmeldingSendtBekreftet: SykmeldingKafkaMessage) =
        sendKafkaMelding(sykmeldingSendtBekreftet.sykmelding.id, sykmeldingSendtBekreftet.serialisertTilString(), SYKMELDINGBEKREFTET_TOPIC)

    private fun producerPåMottattTopic(sykmeldingMottatt: MottattSykmeldingKafkaMessage) =
        sendKafkaMelding(sykmeldingMottatt.sykmelding.id, sykmeldingMottatt.serialisertTilString(), SYKMELDINGMOTTATT_TOPIC)
}