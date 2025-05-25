package no.nav.helse.flex.syketilfelle

import no.nav.helse.flex.syketilfelle.inntektsmelding.INNTEKTSMELDING_TOPIC
import no.nav.helse.flex.syketilfelle.juridiskvurdering.juridiskVurderingTopic
import no.nav.helse.flex.syketilfelle.kafkaprodusering.SYKETILFELLEBIT_TOPIC
import no.nav.helse.flex.syketilfelle.syketilfellebit.SyketilfellebitRepository
import no.nav.helse.flex.syketilfelle.sykmelding.SYKMELDINGBEKREFTET_TOPIC
import no.nav.helse.flex.syketilfelle.sykmelding.SYKMELDINGMOTTATT_TOPIC
import no.nav.helse.flex.syketilfelle.sykmelding.domain.MottattSykmeldingKafkaMessage
import no.nav.helse.flex.syketilfelle.sykmelding.domain.SykmeldingKafkaMessage
import no.nav.inntektsmeldingkontrakt.Inntektsmelding
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import okhttp3.mockwebserver.MockWebServer
import org.amshove.kluent.shouldBeEmpty
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.Headers
import org.apache.kafka.common.header.internals.RecordHeaders
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.utility.DockerImageName

private class PostgreSQLContainer14 : PostgreSQLContainer<PostgreSQLContainer14>("postgres:14-alpine")

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest
@EnableMockOAuth2Server
@AutoConfigureMockMvc
abstract class FellesTestOppsett {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var server: MockOAuth2Server

    @Autowired
    lateinit var kafkaProducer: KafkaProducer<String, String>

    @Autowired
    lateinit var syketilfellebitRepository: SyketilfellebitRepository

    companion object {
        init {

            KafkaContainer(DockerImageName.parse("apache/kafka-native:3.9.1")).also {
                it.start()
                System.setProperty("KAFKA_BROKERS", it.bootstrapServers)
            }

            PostgreSQLContainer14().apply {
                withCommand("postgres", "-c", "wal_level=logical")
                start()
                System.setProperty("spring.datasource.url", "$jdbcUrl&reWriteBatchedInserts=true")
                System.setProperty("spring.datasource.username", username)
                System.setProperty("spring.datasource.password", password)
            }
        }

        val pdlMockWebserver =
            MockWebServer().apply {
                System.setProperty("PDL_BASE_URL", "http://localhost:$port")
                dispatcher = PdlMockDispatcher
            }
    }

    @Autowired
    lateinit var syketilfelleBitConsumer: Consumer<String, String?>

    @Autowired
    lateinit var juridiskVurderingKafkaConsumer: Consumer<String, String>

    @AfterAll
    fun `Vi tømmer databasen`() {
        syketilfellebitRepository.deleteAll()
    }

    fun sendKafkaMelding(
        key: String,
        value: String?,
        topic: String,
        headers: Headers = RecordHeaders(),
    ) {
        kafkaProducer
            .send(
                ProducerRecord(
                    topic,
                    null,
                    null,
                    key,
                    value,
                    headers,
                ),
            ).get()
    }

    fun producerPåInntektsmeldingTopic(inntektsmelding: Inntektsmelding) =
        sendKafkaMelding(inntektsmelding.arbeidstakerFnr, inntektsmelding.serialisertTilString(), INNTEKTSMELDING_TOPIC)

    fun producerPåSendtBekreftetTopic(sykmeldingSendtBekreftet: SykmeldingKafkaMessage) =
        sendKafkaMelding(sykmeldingSendtBekreftet.sykmelding.id, sykmeldingSendtBekreftet.serialisertTilString(), SYKMELDINGBEKREFTET_TOPIC)

    fun producerPåMottattTopic(sykmeldingMottatt: MottattSykmeldingKafkaMessage) =
        sendKafkaMelding(sykmeldingMottatt.sykmelding.id, sykmeldingMottatt.serialisertTilString(), SYKMELDINGMOTTATT_TOPIC)

    @BeforeAll
    fun `Vi leser kafka topicet og feiler om noe eksisterer`() {
        syketilfelleBitConsumer.subscribeHvisIkkeSubscribed(SYKETILFELLEBIT_TOPIC)
        syketilfelleBitConsumer.hentProduserteRecords().shouldBeEmpty()
    }

    @AfterAll
    fun `Vi leser topicet og feiler hvis noe finnes og slik at subklassetestene leser alt`() {
        syketilfelleBitConsumer.hentProduserteRecords().shouldBeEmpty()
    }

    @AfterAll
    fun `Vi leser juridisk vurdering topicet og feiler hvis noe finnes og slik at subklassetestene leser alt`() {
        juridiskVurderingKafkaConsumer.hentProduserteRecords().shouldBeEmpty()
    }

    @BeforeAll
    fun `Vi leser juridiskvurdering kafka topicet og feiler om noe eksisterer`() {
        juridiskVurderingKafkaConsumer.subscribeHvisIkkeSubscribed(juridiskVurderingTopic)
        juridiskVurderingKafkaConsumer.hentProduserteRecords().shouldBeEmpty()
    }
}

fun Any.serialisertTilString(): String = objectMapper.writeValueAsString(this)
