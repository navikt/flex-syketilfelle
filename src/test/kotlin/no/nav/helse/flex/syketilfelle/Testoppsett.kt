package no.nav.helse.flex.syketilfelle

import no.nav.helse.flex.syketilfelle.kafka.KafkaSyketilfellebit
import no.nav.helse.flex.syketilfelle.kafka.SYKETILFELLEBIT_TOPIC
import no.nav.helse.flex.syketilfelle.syketilfellebit.SyketilfellebitRepository
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.Headers
import org.apache.kafka.common.header.internals.RecordHeaders
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

private class PostgreSQLContainer12 : PostgreSQLContainer<PostgreSQLContainer12>("postgres:12-alpine")

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest
@EnableMockOAuth2Server
abstract class Testoppsett {

    @Autowired
    lateinit var kafkaProducer: KafkaProducer<String, String>

    @Autowired
    lateinit var syketilfellebitRepository: SyketilfellebitRepository

    companion object {

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
        }
    }

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

    fun sendSyketilfellebitPaKafka(bit: KafkaSyketilfellebit, headers: Headers = RecordHeaders()) =
        sendKafkaMelding(bit.fnr, bit.serialisertTilString(), SYKETILFELLEBIT_TOPIC, headers)
}

fun Any.serialisertTilString(): String = objectMapper.writeValueAsString(this)
