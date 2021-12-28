package no.nav.helse.flex.syketilfelle

import no.nav.helse.flex.syketilfelle.syketilfellebit.Syketilfellebit
import no.nav.helse.flex.syketilfelle.syketilfellebit.SyketilfellebitBatchInsertDAO
import org.amshove.kluent.`should be equal to`
import org.junit.Before
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

class OnConflictTest : Testoppsett() {

    @Autowired
    lateinit var syketilfellebitBatchInsertDAO: SyketilfellebitBatchInsertDAO

    @Before
    fun `clean`() {
        `Vi tømmer databasen`()
    }

    @Test
    fun `Mottar en bit`() {
        syketilfellebitRepository.deleteAll()
        syketilfellebitRepository.count() `should be equal to` 0

        val bit = Syketilfellebit(
            id = null,
            syketilfellebitId = UUID.randomUUID().toString(),
            fnr = "12345678987",
            orgnummer = "org",
            fom = LocalDate.now().minusDays(2),
            tom = LocalDate.now(),
            korrigererSendtSoknad = UUID.randomUUID().toString(),
            inntruffet = OffsetDateTime.now().plusMinutes(2),
            opprettet = OffsetDateTime.now().plusHours(3),
            ressursId = UUID.randomUUID().toString(),
            tags = "SENDT,HEI",
        )

        syketilfellebitBatchInsertDAO.batchInsert(listOf(bit, bit, bit, bit, bit, bit, bit, bit, bit, bit))
        syketilfellebitBatchInsertDAO.batchInsert(listOf(bit))

        syketilfellebitRepository.count() `should be equal to` 1

        val dbBit = syketilfellebitRepository.findAll().first()

        dbBit.syketilfellebitId `should be equal to` bit.syketilfellebitId
        dbBit.fnr `should be equal to` bit.fnr
        dbBit.orgnummer `should be equal to` bit.orgnummer
        dbBit.fom `should be equal to` bit.fom
        dbBit.tom `should be equal to` bit.tom
        dbBit.korrigererSendtSoknad `should be equal to` bit.korrigererSendtSoknad
        dbBit.inntruffet `should be equal to ignoring nano and zone` bit.inntruffet
        dbBit.opprettet `should be equal to ignoring nano and zone` bit.opprettet
        dbBit.ressursId `should be equal to` bit.ressursId
        dbBit.tags `should be equal to` bit.tags
    }
}
