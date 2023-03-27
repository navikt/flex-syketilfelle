package no.nav.helse.flex.syketilfelle.kafkaprodusering

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.syketilfelle.Testoppsett
import no.nav.helse.flex.syketilfelle.objectMapper
import no.nav.helse.flex.syketilfelle.syketilfellebit.KafkaSyketilfellebit
import no.nav.helse.flex.syketilfelle.syketilfellebit.Syketilfellebit
import no.nav.helse.flex.syketilfelle.syketilfellebit.Tag.SENDT
import no.nav.helse.flex.syketilfelle.syketilfellebit.Tag.SYKEPENGESOKNAD
import no.nav.helse.flex.syketilfelle.syketilfellebit.tilSyketilfellebitDbRecord
import no.nav.helse.flex.syketilfelle.ventPåRecords
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldHaveSize
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

class KafkaProduseringTest : Testoppsett() {

    @Autowired
    private lateinit var kafkaProduseringJob: KafkaProduseringJob

    @BeforeEach
    fun setUp() {
        syketilfellebitRepository.deleteAll()
    }

    @Test
    fun `tester med ingen data`() {
        kafkaProduseringJob.publiser()
        syketilfellebitRepository.findFirst300ByPublisertOrderByOpprettetAsc(false).shouldBeEmpty()
        syketilfelleBitConsumer.ventPåRecords(antall = 0).shouldBeEmpty()
    }

    @Test
    fun `tester med data`() {
        syketilfellebitRepository.findFirst300ByPublisertOrderByOpprettetAsc(false).shouldBeEmpty()

        syketilfellebitRepository.save(
            Syketilfellebit(
                fnr = "fnr",
                orgnummer = "orgnummer",
                opprettet = OffsetDateTime.now().minusDays(4),
                inntruffet = OffsetDateTime.now().minusDays(1),
                tags = setOf(SYKEPENGESOKNAD, SENDT),
                ressursId = "den eldste",
                fom = LocalDate.now().minusWeeks(1),
                tom = LocalDate.now(),
                publisert = false
            ).tilSyketilfellebitDbRecord()
        )
        syketilfellebitRepository.save(
            Syketilfellebit(
                fnr = "fnr",
                orgnummer = "orgnummer",
                opprettet = OffsetDateTime.now().minusDays(1),
                inntruffet = OffsetDateTime.now().minusDays(1),
                tags = setOf(SYKEPENGESOKNAD, SENDT),
                ressursId = "den eldste",
                fom = LocalDate.now().minusWeeks(1),
                tom = LocalDate.now(),
                publisert = true
            ).tilSyketilfellebitDbRecord()
        )
        syketilfellebitRepository.save(
            Syketilfellebit(
                fnr = "fnr",
                orgnummer = "orgnummer",
                opprettet = OffsetDateTime.now().minusDays(1),
                inntruffet = OffsetDateTime.now().minusDays(1),
                tags = setOf(SYKEPENGESOKNAD, SENDT),
                ressursId = "den nyeste",
                fom = LocalDate.now().minusWeeks(1),
                tom = LocalDate.now(),
                publisert = false
            ).tilSyketilfellebitDbRecord()
        )
        syketilfellebitRepository.findFirst300ByPublisertOrderByOpprettetAsc(false).shouldHaveSize(2)
        syketilfellebitRepository.findFirst300ByPublisertOrderByOpprettetAsc(true).shouldHaveSize(1)
        kafkaProduseringJob.publiser()

        val records = syketilfelleBitConsumer.ventPåRecords(antall = 2).map { it.deserialisert() }

        records[0].second!!.ressursId `should be equal to` "den eldste"
        records[1].second!!.ressursId `should be equal to` "den nyeste"
        syketilfellebitRepository.findFirst300ByPublisertOrderByOpprettetAsc(false).shouldBeEmpty()
        syketilfellebitRepository.findFirst300ByPublisertOrderByOpprettetAsc(true).shouldHaveSize(3)

        kafkaProduseringJob.publiser()
        syketilfelleBitConsumer.ventPåRecords(antall = 0).shouldBeEmpty()
    }

    @Test
    fun `db query returnerer kun 300 om gangen`() {
        syketilfellebitRepository.count() `should be equal to` 0L
        syketilfellebitRepository.findFirst300ByPublisertOrderByOpprettetAsc(false).shouldBeEmpty()

        fun lagreBit() {
            syketilfellebitRepository.save(
                Syketilfellebit(
                    fnr = "fnr",
                    orgnummer = "orgnummer",
                    opprettet = OffsetDateTime.now().minusDays(4),
                    inntruffet = OffsetDateTime.now().minusDays(1),
                    tags = setOf(SYKEPENGESOKNAD, SENDT),
                    ressursId = UUID.randomUUID().toString(),
                    fom = LocalDate.now().minusWeeks(1),
                    tom = LocalDate.now(),
                    publisert = false
                ).tilSyketilfellebitDbRecord()
            )
        }

        repeat(420) { lagreBit() }
        syketilfellebitRepository.count() `should be equal to` 420
        syketilfellebitRepository.findFirst300ByPublisertOrderByOpprettetAsc(false).shouldHaveSize(300)

        kafkaProduseringJob.publiser()

        syketilfelleBitConsumer.ventPåRecords(antall = 300, duration = Duration.ofSeconds(10))
        syketilfellebitRepository.count() `should be equal to` 420
        syketilfellebitRepository.findFirst300ByPublisertOrderByOpprettetAsc(false).shouldHaveSize(120)
        syketilfellebitRepository.findFirst300ByPublisertOrderByOpprettetAsc(true).shouldHaveSize(300)
    }
}

fun ConsumerRecord<String, String?>.deserialisert(): Pair<String, KafkaSyketilfellebit?> =
    Pair(key(), value()?.tilKafkaSyketilfellebit())

fun String.tilKafkaSyketilfellebit(): KafkaSyketilfellebit = objectMapper.readValue(this)
