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
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

private class PostgreSQLContainer12 : PostgreSQLContainer<PostgreSQLContainer12>("postgres:12-alpine")

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest
@EnableMockOAuth2Server
@AutoConfigureMockMvc
abstract class Testoppsett {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var server: MockOAuth2Server

    @Autowired
    lateinit var kafkaProducer: KafkaProducer<String, String>

    @Autowired
    lateinit var syketilfellebitRepository: SyketilfellebitRepository

    companion object {
        var pdlMockWebserver: MockWebServer

        init {
            PostgreSQLContainer12().also {
                it.start()
                System.setProperty("spring.datasource.url", "${it.jdbcUrl}&reWriteBatchedInserts=true")
                System.setProperty("spring.datasource.username", it.username)
                System.setProperty("spring.datasource.password", it.password)
            }

            KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:6.1.1")).also {
                it.start()
                System.setProperty("KAFKA_BROKERS", it.bootstrapServers)
            }

            pdlMockWebserver = MockWebServer()
                .also {
                    System.setProperty("PDL_BASE_URL", "http://localhost:${it.port}")
                }
                .also { it.dispatcher = PdlMockDispatcher }
        }
    }

    @Autowired
    lateinit var syketilfelleBitConsumer: Consumer<String, String>

    @Autowired
    lateinit var juridiskVurderingKafkaConsumer: Consumer<String, String>

    @AfterAll
    fun `Vi tømmer databasen`() {
        syketilfellebitRepository.deleteAll()
    }

    fun sendKafkaMelding(key: String, value: String, topic: String, headers: Headers = RecordHeaders()) {
        kafkaProducer.send(
            ProducerRecord(
                topic,
                null,
                null,
                key,
                value,
                headers
            )
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
