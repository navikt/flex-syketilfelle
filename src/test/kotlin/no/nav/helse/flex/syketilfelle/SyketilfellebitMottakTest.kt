package no.nav.helse.flex.syketilfelle

import no.nav.helse.flex.syketilfelle.kafka.KafkaSyketilfellebit
import no.nav.helse.flex.syketilfelle.syketilfellebit.tagsFromString
import org.amshove.kluent.`should be equal to`
import org.awaitility.Awaitility.await
import org.junit.Before
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*
import java.util.concurrent.TimeUnit

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)

class SyketilfellebitMottakTest : Testoppsett() {

    @Before
    fun `clean`() {
        `Vi tømmer databasen`()
    }

    @Test
    @Order(1)
    fun `Mottar en bit`() {
        syketilfellebitRepository.deleteAll()
        syketilfellebitRepository.count() `should be equal to` 0

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
            tags = listOf("SENDT", "SYKEPENGESOKNAD"),
        )

        sendSyketilfellebitPaKafka(bit)
        sendSyketilfellebitPaKafka(bit)
        sendSyketilfellebitPaKafka(bit)

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
        dbBit.tags.tagsFromString() `should be equal to` bit.tags
    }

    @Test
    @Order(1)
    fun `Mottar 200 biter`() {
        syketilfellebitRepository.deleteAll()
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
                tags = listOf("SENDT", "SYKEPENGESOKNAD"),
            )
            sendSyketilfellebitPaKafka(bit)
        }

        repeat(200) { sendBit() }

        await().atMost(10, TimeUnit.SECONDS).until {
            syketilfellebitRepository.count() == 200L
        }

        syketilfellebitRepository.count() `should be equal to` 200L
    }
}
