package no.nav.helse.flex.syketilfelle.ventetid

import no.nav.helse.flex.syketilfelle.FellesTestOppsett
import no.nav.helse.flex.syketilfelle.sykmelding.SykmeldingLagring
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.Month

class RedusertVenteperiodeTest :
    FellesTestOppsett(),
    VentetidFellesOppsett {
    @Autowired
    override lateinit var sykmeldingLagring: SykmeldingLagring

    @Autowired
    private lateinit var ventetidUtregner: VentetidUtregner

    @BeforeEach
    @AfterEach
    fun setUp() {
        syketilfellebitRepository.deleteAll()
    }

    final override val fnr = "11111111111"

    @Test
    fun `Redusert Venteperioder langt tilbake i tid påvirker ikke beregning av ventetid`() {
        """
        [
          {
            "syketilfellebitId": "151dfae6-7910-485d-b62b-9fc56483e9de",
            "fnr": "$fnr",
            "opprettet": "2022-01-23T13:51:14.849049Z",
            "inntruffet": "2022-01-23T13:51:14.849047Z",
            "orgnummer": null,
            "tags": "SYKMELDING,NY,PERIODE,GRADERT_AKTIVITET,REDUSERT_ARBEIDSGIVERPERIODE",
            "ressursId": "f65c85a8-a215-416b-a26d-c8aa280abf74",
            "korrigererSendtSoknad": null,
            "fom": "2022-01-02",
            "tom": "2022-01-30",
            "publisert": true,
            "slettet": null,
            "tombstonePublisert": null
          },
          {
            "syketilfellebitId": "60afd674-9956-40a6-94b3-c6c51865cee7",
            "fnr": "$fnr",
            "opprettet": "2025-09-15T12:30:23.588773Z",
            "inntruffet": "2025-09-15T12:30:23.588771Z",
            "orgnummer": null,
            "tags": "SYKMELDING,NY,PERIODE,INGEN_AKTIVITET",
            "ressursId": "a2821114-194a-44c5-b27f-3db65ba83b44",
            "korrigererSendtSoknad": null,
            "fom": "2025-09-10",
            "tom": "2025-10-13",
            "publisert": true,
            "slettet": null,
            "tombstonePublisert": null
          }
        ]
        """.trimIndent().tilSyketilfellebitDbRecords().also {
            syketilfellebitRepository.saveAll(it)
        }

        hentVentetid(
            listOf(fnr),
            sykmeldingId = "a2821114-194a-44c5-b27f-3db65ba83b44",
            ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
        ).ventetid.also {
            it!!.fom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 10)
            it.tom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 25)
        }
    }

    private fun hentVentetid(
        identer: List<String>,
        sykmeldingId: String,
        ventetidRequest: VentetidRequest,
    ): VentetidResponse =
        VentetidResponse(
            ventetid =
                ventetidUtregner.beregnVentetid(
                    sykmeldingId = sykmeldingId,
                    identer = identer,
                    ventetidRequest = ventetidRequest,
                ),
        )
}
