package no.nav.helse.flex.syketilfelle

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.syketilfelle.inntektsmelding.INNTEKTSMELDING_TOPIC
import no.nav.helse.flex.syketilfelle.juridiskvurdering.juridiskVurderingTopic
import no.nav.helse.flex.syketilfelle.kafkaprodusering.SYKETILFELLEBIT_TOPIC
import no.nav.helse.flex.syketilfelle.syketilfellebit.SyketilfellebitDbRecord
import no.nav.helse.flex.syketilfelle.syketilfellebit.SyketilfellebitRepository
import no.nav.helse.flex.syketilfelle.sykmelding.SYKMELDINGBEKREFTET_TOPIC
import no.nav.helse.flex.syketilfelle.sykmelding.SYKMELDINGMOTTATT_TOPIC
import no.nav.helse.flex.syketilfelle.sykmelding.SYKMELDINGSENDT_TOPIC
import no.nav.helse.flex.syketilfelle.sykmelding.SykmeldingLagring
import no.nav.helse.flex.syketilfelle.sykmelding.domain.MottattSykmeldingKafkaMessage
import no.nav.helse.flex.syketilfelle.sykmelding.domain.SykmeldingKafkaMessage
import no.nav.inntektsmeldingkontrakt.Inntektsmelding
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import no.nav.syfo.model.sykmelding.arbeidsgiver.ArbeidsgiverSykmelding
import okhttp3.mockwebserver.MockWebServer
import org.amshove.kluent.shouldBeEmpty
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.header.Headers
import org.apache.kafka.common.header.internals.RecordHeaders
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.util.concurrent.TimeUnit

private class PostgreSQLContainer14 : PostgreSQLContainer("postgres:14-alpine")

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

    @Autowired
    lateinit var sykmeldingLagring: SykmeldingLagring

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

            MockWebServer().apply {
                System.setProperty("PDL_BASE_URL", "http://localhost:$port")
                dispatcher = PdlMockDispatcher
            }
        }
    }

    @Autowired
    lateinit var syketilfelleBitConsumer: Consumer<String, String?>

    @Autowired
    lateinit var juridiskVurderingKafkaConsumer: Consumer<String, String>

    @AfterAll
    fun `Slett alt innhold i databasen`() {
        syketilfellebitRepository.deleteAll()
    }

    @BeforeAll
    fun `Verifiser at Kafka-topics før tester`() {
        syketilfelleBitConsumer.subscribeHvisIkkeSubscribed(SYKETILFELLEBIT_TOPIC)
        syketilfelleBitConsumer.hentProduserteRecords().shouldBeEmpty()
        juridiskVurderingKafkaConsumer.subscribeHvisIkkeSubscribed(juridiskVurderingTopic)
        juridiskVurderingKafkaConsumer.hentProduserteRecords().shouldBeEmpty()
    }

    @AfterAll
    fun `Verifiser at Kafka-topics etter tester`() {
        syketilfelleBitConsumer.hentProduserteRecords().shouldBeEmpty()
        juridiskVurderingKafkaConsumer.hentProduserteRecords().shouldBeEmpty()
    }

    fun opprettMottattSykmelding(
        sykmelding: ArbeidsgiverSykmelding,
        fnr: String,
    ): String {
        sendMottattSykmelding(lagMottattSykmeldingKafkaMessage(fnr, sykmelding))
        return sykmelding.id
    }

    fun opprettSendtSykmelding(
        sykmelding: ArbeidsgiverSykmelding,
        fnr: String,
        orgnummer: String? = null,
    ): String {
        sendSendtSykmelding(lagSendtSykmeldingKafkaMessage(fnr, sykmelding, orgnummer))
        return sykmelding.id
    }

    fun opprettBekreftetSykmelding(
        sykmelding: ArbeidsgiverSykmelding,
        fnr: String,
    ): String {
        sendBekreftetSykmelding(lagBekreftetSykmeldingKafkaMessage(opprettMottattSykmelding(sykmelding, fnr)))
        return sykmelding.id
    }

    fun sendMottattSykmelding(sykmelding: MottattSykmeldingKafkaMessage) =
        sendKafkaMelding(
            sykmelding.sykmelding.id,
            sykmelding.serialisertTilString(),
            SYKMELDINGMOTTATT_TOPIC,
        )

    fun sendBekreftetSykmelding(sykmelding: SykmeldingKafkaMessage) =
        sendKafkaMelding(
            sykmelding.sykmelding.id,
            sykmelding.serialisertTilString(),
            SYKMELDINGBEKREFTET_TOPIC,
        )

    fun sendSendtSykmelding(sykmelding: SykmeldingKafkaMessage) =
        sendKafkaMelding(
            sykmelding.sykmelding.id,
            sykmelding.serialisertTilString(),
            SYKMELDINGSENDT_TOPIC,
        )

    fun sendInntektsmelding(inntektsmelding: Inntektsmelding) =
        sendKafkaMelding(inntektsmelding.arbeidstakerFnr, inntektsmelding.serialisertTilString(), INNTEKTSMELDING_TOPIC)

    fun sendKafkaMelding(
        key: String,
        value: String?,
        topic: String,
        headers: Headers = RecordHeaders(),
    ): RecordMetadata? = kafkaProducer.send(ProducerRecord(topic, null, null, key, value, headers)).get()

    fun MottattSykmeldingKafkaMessage.prosesser() {
        sykmeldingLagring.handterMottattSykmelding("key", this, SYKMELDINGMOTTATT_TOPIC)
    }

    fun SykmeldingKafkaMessage.prosesser() {
        sykmeldingLagring.prosesserSykmelding("key", this, SYKMELDINGSENDT_TOPIC)
    }

    fun verifiserAtBiterErLagret(forventetAntallBiter: Int) {
        await().atMost(5, TimeUnit.SECONDS).until {
            syketilfellebitRepository.count().toInt() == forventetAntallBiter
        }
    }
}

fun Any.serialisertTilString(): String = objectMapper.writeValueAsString(this)

fun String.tilSyketilfellebitDbRecord(): List<SyketilfellebitDbRecord> = objectMapper.readValue(this)
