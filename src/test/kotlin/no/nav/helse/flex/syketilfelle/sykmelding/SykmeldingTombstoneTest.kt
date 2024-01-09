package no.nav.helse.flex.syketilfelle.sykmelding

import no.nav.helse.flex.syketilfelle.*
import no.nav.helse.flex.syketilfelle.sykeforloep.SimpleSykmelding
import no.nav.helse.flex.syketilfelle.ventetid.VentetidFellesOppsett
import no.nav.syfo.model.sykmeldingstatus.ShortNameDTO
import no.nav.syfo.model.sykmeldingstatus.SporsmalOgSvarDTO
import no.nav.syfo.model.sykmeldingstatus.SvartypeDTO
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.format.DateTimeFormatter.ISO_DATE
import java.util.concurrent.TimeUnit

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class SykmeldingTombstoneTest : VentetidFellesOppsett, Testoppsett() {
    override val fnr = "12345432123"
    final val fom = LocalDate.of(2024, 1, 1)
    final val tom = LocalDate.of(2024, 1, 8)
    final val tidligDato = LocalDate.of(2000, 1, 1)
    lateinit var sykmeldingId: String

    @Autowired
    override lateinit var sykmeldingLagring: SykmeldingLagring

    @Autowired
    lateinit var nullableStringConsumer: KafkaConsumer<String, String?>

    @Autowired
    lateinit var sykmeldingTombstoneFixer: SykmeldingTombstoneFixer

    @BeforeAll
    fun subscribe() {
        nullableStringConsumer.subscribeHvisIkkeSubscribed(SYKMELDINGBEKREFTET_TOPIC, SYKMELDINGSENDT_TOPIC)
    }

    @Test
    @Order(1)
    fun `Mottar bekreftet sykmelding med fravær i år 2000`() {
        val sykmeldingKafka =
            skapSykmeldingKafkaMessage(
                fom = fom,
                tom = tom,
                sporsmals =
                    listOf(
                        SporsmalOgSvarDTO(
                            tekst = "Hadde du annet fravær i år 2000?",
                            shortName = ShortNameDTO.PERIODE,
                            svar = """[{"fom":"${tidligDato.format(ISO_DATE)}","tom":"${tidligDato.format(ISO_DATE)}"}]""",
                            svartype = SvartypeDTO.PERIODER,
                        ),
                    ),
            )
        sykmeldingId = sykmeldingKafka.sykmelding.id

        sendKafkaMelding(sykmeldingId, sykmeldingKafka.serialisertTilString(), SYKMELDINGBEKREFTET_TOPIC)

        Awaitility.await().atMost(10, TimeUnit.SECONDS).until {
            syketilfellebitRepository.findByFnr(fnr).size == 2
        }
        val sykeforloep = hentSykeforloep(listOf(fnr))

        assertThat(sykeforloep).hasSize(1)
        assertThat(sykeforloep[0].oppfolgingsdato).isEqualTo(tidligDato)
        assertThat(sykeforloep[0].sykmeldinger.toList()).isEqualTo(
            listOf(
                SimpleSykmelding(
                    fom = tidligDato,
                    tom = tom,
                    id = sykmeldingId,
                ),
            ),
        )
    }

    @Test
    @Order(2)
    fun `Mottar tombstone på kafka`() {
        sendKafkaMelding(sykmeldingId, null, SYKMELDINGBEKREFTET_TOPIC)

        Awaitility.await().atMost(10, TimeUnit.SECONDS).until {
            val biter = syketilfellebitRepository.findByFnr(fnr)
            biter.size == 2 && biter.all { it.slettet != null }
        }
    }

    @Test
    @Order(3)
    fun `Sykmeldingen bekreftes uten spørsmål`() {
        val sykmeldingKafka =
            skapSykmeldingKafkaMessage(
                fom = fom,
                tom = tom,
            ).run {
                copy(
                    sykmelding =
                        this.sykmelding.copy(
                            id = sykmeldingId,
                        ),
                    kafkaMetadata =
                        this.kafkaMetadata.copy(
                            sykmeldingId = sykmeldingId,
                        ),
                    event =
                        this.event.copy(
                            sykmeldingId = sykmeldingId,
                        ),
                )
            }

        sendKafkaMelding(sykmeldingId, sykmeldingKafka.serialisertTilString(), SYKMELDINGBEKREFTET_TOPIC)

        Awaitility.await().atMost(10, TimeUnit.SECONDS).until {
            syketilfellebitRepository.findByFnr(fnr).size == 2 + 1
        }
        val sykeforloep = hentSykeforloep(listOf(fnr))

        assertThat(sykeforloep).hasSize(1)
        assertThat(sykeforloep[0].oppfolgingsdato).isEqualTo(fom)
        assertThat(sykeforloep[0].sykmeldinger.toList()).isEqualTo(
            listOf(
                SimpleSykmelding(
                    fom = fom,
                    tom = tom,
                    id = sykmeldingId,
                ),
            ),
        )
    }

    @Test
    @Order(4)
    fun `Mottar enda en tombstone på kafka`() {
        sendKafkaMelding(sykmeldingId, null, SYKMELDINGBEKREFTET_TOPIC)

        Awaitility.await().atMost(10, TimeUnit.SECONDS).until {
            val biter = syketilfellebitRepository.findByFnr(fnr)
            biter.size == 2 + 1 && biter.all { it.slettet != null }
        }
    }

    @Test
    @Order(5)
    fun `Sykmeldingen gjenåpnes som arbeidstaker sykmelding og oppfolgingsdato er lik fom`() {
        val sykmelding = skapArbeidsgiverSykmelding(fom, tom, sykmeldingId)

        opprettMottattSykmelding(sykmelding, fnr)
        opprettSendtSykmelding(sykmelding, fnr)

        Awaitility.await().atMost(10, TimeUnit.SECONDS).until {
            syketilfellebitRepository.findByFnr(fnr).size == 2 + 1 + 2
        }
        val sykeforloep = hentSykeforloep(listOf(fnr))

        assertThat(sykeforloep).hasSize(1)
        assertThat(sykeforloep[0].oppfolgingsdato).isEqualTo(fom)
        assertThat(sykeforloep[0].sykmeldinger.toList()).isEqualTo(
            listOf(
                SimpleSykmelding(
                    fom = fom,
                    tom = tom,
                    id = sykmeldingId,
                ),
            ),
        )
    }

    @Test
    @Order(6)
    fun `Sykmelding tombstone fixer`() {
        nullableStringConsumer
            .ventPåRecords(5)
            .forEach {
                sykmeldingTombstoneFixer.prosesserSykmeldingPåNytt(it)
            }

        val sykeforloep = hentSykeforloep(listOf(fnr))
        assertThat(sykeforloep).hasSize(1)
        assertThat(sykeforloep[0].oppfolgingsdato).isEqualTo(fom)
        assertThat(sykeforloep[0].sykmeldinger.toList()).isEqualTo(
            listOf(
                SimpleSykmelding(
                    fom = fom,
                    tom = tom,
                    id = sykmeldingId,
                ),
            ),
        )
    }
}
