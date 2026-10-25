package no.nav.helse.flex.syketilfelle

import io.kotest.matchers.shouldBe
import no.nav.helse.flex.syketilfelle.syketilfellebit.SyketilfellebitDbRecord
import no.nav.helse.flex.syketilfelle.testdata.TESTDATA_RESET_TOPIC
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*
import java.util.concurrent.TimeUnit

class ResetTestdataTest : FellesTestOppsett() {
    @Test
    fun `Kan slette en bit`() {
        syketilfellebitRepository.count() shouldBe 0

        val fnr = "12345678987"
        val bit =
            SyketilfellebitDbRecord(
                id = null,
                syketilfellebitId = UUID.randomUUID().toString(),
                fnr = fnr,
                orgnummer = "org",
                fom = LocalDate.now().minusDays(2),
                tom = LocalDate.now(),
                korrigererSendtSoknad = UUID.randomUUID().toString(),
                inntruffet = OffsetDateTime.now().plusMinutes(2),
                opprettet = OffsetDateTime.now().plusHours(3),
                ressursId = UUID.randomUUID().toString(),
                tags = "SENDT",
                publisert = true,
            )
        syketilfellebitRepository.save(bit)
        syketilfellebitRepository.save(bit.copy(fnr = "annet", syketilfellebitId = UUID.randomUUID().toString()))
        syketilfellebitRepository.count() shouldBe 2
        sendKafkaMelding(UUID.randomUUID().toString(), fnr, TESTDATA_RESET_TOPIC)
        await().atMost(4, TimeUnit.SECONDS).until {
            syketilfellebitRepository.count() == 1L
        }
        syketilfellebitRepository.count() shouldBe 1
        syketilfellebitRepository.deleteByFnr(bit.fnr) shouldBe 0
        syketilfellebitRepository.count() shouldBe 1
    }
}
