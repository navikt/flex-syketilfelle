package no.nav.helse.flex.syketilfelle.ventetid

import no.nav.helse.flex.syketilfelle.FellesTestOppsett
import no.nav.helse.flex.syketilfelle.tilSyketilfellebitDbRecord
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.Month

class DebugProduksjonTest : FellesTestOppsett() {
    @Autowired
    private lateinit var ventetidUtregner: VentetidUtregner

    @BeforeEach
    @AfterEach
    fun setUp() {
        syketilfellebitRepository.deleteAll()
    }

    private val fnr = "11111111111"

    @Test
    fun `Perioder innenfor ventetid som er like returnerer ventetid`() {
        """
        [
          {
            "syketilfellebitId": "f1e6abc7-889c-46b5-96a1-c8170bfe4998",
            "fnr": "$fnr",
            "opprettet": "2026-01-01T00:00:00.00000Z",
            "inntruffet": "2026-01-01T00:00:00.00000Z",
            "orgnummer": null,
            "tags": "SYKMELDING,BEKREFTET,PERIODE,INGEN_AKTIVITET",
            "ressursId": "a9d5ac93-0c06-4472-8fdb-88f5da6cd0f9",
            "korrigererSendtSoknad": null,
            "fom": "2026-01-01",
            "tom": "2026-01-12",
            "publisert": true,
            "slettet": null,
            "tombstonePublisert": null
          },
          {
            "syketilfellebitId": "43e1c0c8-6a73-419a-8a20-42a77461d1ad",
            "fnr": "$fnr",
            "opprettet": "2026-01-02T00:00:00.00000Z",
            "inntruffet": "2026-01-02T00:00:00.00000Z",
            "orgnummer": null,
            "tags": "SYKMELDING,BEKREFTET,PERIODE,INGEN_AKTIVITET",
            "ressursId": "42c3bb8e-0a18-491d-b01a-9c6af2af5cbf",
            "korrigererSendtSoknad": null,
            "fom": "2026-01-01",
            "tom": "2026-01-12",
            "publisert": true,
            "slettet": null,
            "tombstonePublisert": null
          }
        ] 
        """.trimIndent().tilSyketilfellebitDbRecord().also {
            syketilfellebitRepository.saveAll(it)
        }

        ventetidUtregner
            .beregnVentetid(
                sykmeldingId = "a9d5ac93-0c06-4472-8fdb-88f5da6cd0f9",
                listOf(fnr),
                ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
            ).also {
                it!!.fom `should be equal to` LocalDate.of(2026, Month.JANUARY, 1)
                it.tom `should be equal to` LocalDate.of(2026, Month.JANUARY, 12)
            }

        ventetidUtregner
            .beregnVentetid(
                sykmeldingId = "42c3bb8e-0a18-491d-b01a-9c6af2af5cbf",
                listOf(fnr),
                ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
            ).also {
                it!!.fom `should be equal to` LocalDate.of(2026, Month.JANUARY, 1)
                it.tom `should be equal to` LocalDate.of(2026, Month.JANUARY, 12)
            }
    }
}
