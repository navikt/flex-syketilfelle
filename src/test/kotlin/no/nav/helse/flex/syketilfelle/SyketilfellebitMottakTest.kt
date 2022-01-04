package no.nav.helse.flex.syketilfelle

import no.nav.helse.flex.syketilfelle.syketilfellebit.KafkaSyketilfellebit
import no.nav.helse.flex.syketilfelle.syketilfellebit.tagsFromString
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldHaveSize
import org.apache.kafka.common.header.internals.RecordHeader
import org.apache.kafka.common.header.internals.RecordHeaders
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import java.time.Duration
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*
import java.util.concurrent.TimeUnit

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class SyketilfellebitMottakTest : Testoppsett() {

    @AfterEach
    fun `clean`() {
        `Vi tømmer databasen`()
    }

    var produserteBiter = 0

    @Test
    @Order(1)
    fun `Mottar en bit`() {
        kafkaConsumer.ventPåRecords(antall = 0)
        syketilfellebitRepository.count() `should be equal to` 0

        val bit = KafkaSyketilfellebit(
            id = UUID.randomUUID().toString(),
            fnr = "12345678987",
            orgnummer = "org",
            fom = LocalDate.now().minusDays(2),
            tom = LocalDate.now(),
            korrigererSendtSoknad = UUID.randomUUID().toString(),
            inntruffet = OffsetDateTime.now().plusMinutes(2),
            opprettet = OffsetDateTime.now().plusHours(3),
            ressursId = UUID.randomUUID().toString(),
            tags = setOf("SENDT", "SYKEPENGESOKNAD"),
        )

        sendSyketilfellebitPaKafka(bit)
        sendSyketilfellebitPaKafka(bit)
        sendSyketilfellebitPaKafka(bit)
        produserteBiter += 3

        await().atMost(10, TimeUnit.SECONDS).until {
            syketilfellebitRepository.existsBySyketilfellebitId(bit.id)
        }

        syketilfellebitRepository.count() `should be equal to` 1

        val dbBit = syketilfellebitRepository.findBySyketilfellebitId(bit.id)!!

        dbBit.syketilfellebitId `should be equal to` bit.id
        dbBit.fnr `should be equal to` bit.fnr
        dbBit.orgnummer `should be equal to` bit.orgnummer
        dbBit.fom `should be equal to` bit.fom
        dbBit.tom `should be equal to` bit.tom
        dbBit.korrigererSendtSoknad `should be equal to` bit.korrigererSendtSoknad
        dbBit.inntruffet `should be equal to ignoring nano and zone` bit.inntruffet
        dbBit.opprettet `should be equal to ignoring nano and zone` bit.opprettet
        dbBit.ressursId `should be equal to` bit.ressursId
        dbBit.tags.tagsFromString().map { it.name }.toSet() `should be equal to` bit.tags

        syketilfellebitRepository.findFirst300ByPublisertOrderByOpprettetAsc(true).shouldHaveSize(1)
    }

    @Test
    @Order(2)
    fun `Mottar 20 biter`() {
        syketilfellebitRepository.count() `should be equal to` 0

        fun sendBit() {
            val bit = KafkaSyketilfellebit(
                id = UUID.randomUUID().toString(),
                fnr = "12345678987",
                orgnummer = "org",
                fom = LocalDate.now().minusDays(2),
                tom = LocalDate.now(),
                korrigererSendtSoknad = UUID.randomUUID().toString(),
                inntruffet = OffsetDateTime.now(),
                opprettet = OffsetDateTime.now(),
                ressursId = UUID.randomUUID().toString(),
                tags = setOf("SENDT", "SYKEPENGESOKNAD"),
            )
            repeat((1..10).random()) {
                sendSyketilfellebitPaKafka(bit)
                produserteBiter += 1
            }
        }

        repeat(20) { sendBit() }

        await().during(4, TimeUnit.SECONDS).until {
            syketilfellebitRepository.count() == 20L
        }

        syketilfellebitRepository.count() `should be equal to` 20L
        syketilfellebitRepository.findFirst300ByPublisertOrderByOpprettetAsc(false).shouldHaveSize(0)
        syketilfellebitRepository.findFirst300ByPublisertOrderByOpprettetAsc(true).shouldHaveSize(20)
    }

    @Test
    @Order(3)
    fun `Ignorerer bit fra flex-syketilfelle`() {
        syketilfellebitRepository.count() `should be equal to` 0

        val bit = KafkaSyketilfellebit(
            id = UUID.randomUUID().toString(),
            fnr = "12345678987",
            orgnummer = "org",
            fom = LocalDate.now().minusDays(2),
            tom = LocalDate.now(),
            korrigererSendtSoknad = UUID.randomUUID().toString(),
            inntruffet = OffsetDateTime.now().plusMinutes(2),
            opprettet = OffsetDateTime.now().plusHours(3),
            ressursId = UUID.randomUUID().toString(),
            tags = setOf("SENDT", "SYKEPENGESOKNAD"),
        )

        val header = RecordHeaders().also { it.add(RecordHeader("kilde", "flex-syketilfelle".toByteArray())) }

        sendSyketilfellebitPaKafka(bit, header)
        produserteBiter += 1

        await().during(2, TimeUnit.SECONDS).until {
            syketilfellebitRepository.count() == 0L
        }
        syketilfellebitRepository.count() `should be equal to` 0
        syketilfellebitRepository.findFirst300ByPublisertOrderByOpprettetAsc(false).shouldHaveSize(0)
    }

    @Test
    @Order(4)
    fun `Vi leser topicet ut med test konsumeren så den ikke feiler i AfterAll `() {
        kafkaConsumer.ventPåRecords(antall = produserteBiter, duration = Duration.ofSeconds(10))
    }
}
