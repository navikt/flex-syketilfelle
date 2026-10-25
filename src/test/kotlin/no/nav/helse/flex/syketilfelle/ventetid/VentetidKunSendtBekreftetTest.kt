package no.nav.helse.flex.syketilfelle.ventetid

import io.kotest.matchers.shouldBe
import no.nav.helse.flex.syketilfelle.FellesTestOppsett
import no.nav.helse.flex.syketilfelle.lagArbeidsgiverSykmeldingDTO
import no.nav.helse.flex.syketilfelle.lagBekreftetSykmeldingKafkaMessage
import no.nav.helse.flex.syketilfelle.lagMottattSykmeldingKafkaMessage
import no.nav.helse.flex.syketilfelle.lagSendtSykmeldingKafkaMessage
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.Month

class VentetidKunSendtBekreftetTest : FellesTestOppsett() {
    @Autowired
    private lateinit var ventetidUtregner: VentetidUtregner

    @BeforeEach
    @AfterEach
    fun setUp() {
        syketilfellebitRepository.deleteAll()
    }

    private val fnr = "11111111111"

    @Test
    fun `Beregn ventetid for perioder kun sendt eller bekreftet`() {
        lagMottattSykmeldingKafkaMessage(
            fnr = fnr,
            fom = LocalDate.of(2026, Month.JUNE, 1),
            tom = LocalDate.of(2026, Month.JUNE, 7),
        ).also { it.prosesser() }

        val bekreftetSykmelding =
            lagBekreftetSykmeldingKafkaMessage(
                fnr = fnr,
                fom = LocalDate.of(2026, Month.JUNE, 8),
                tom = LocalDate.of(2026, Month.JUNE, 14),
            ).also { it.prosesser() }

        val sendtSykmelding =
            lagSendtSykmeldingKafkaMessage(
                fnr,
                lagArbeidsgiverSykmeldingDTO(
                    fom = LocalDate.of(2026, Month.JUNE, 15),
                    tom = LocalDate.of(2026, Month.JUNE, 21),
                ),
            ).also { it.prosesser() }

        ventetidUtregner.erUtenforVentetid(
            sykmeldingId = sendtSykmelding.sykmelding.id,
            identer = listOf(fnr),
            erUtenforVentetidRequest = ErUtenforVentetidRequest(),
        ) shouldBe false

        ventetidUtregner
            .beregnVentetid(
                identer = listOf(fnr),
                sykmeldingId = sendtSykmelding.sykmelding.id,
                ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
            ).also {
                it!!.fom shouldBe LocalDate.of(2026, Month.JUNE, 8)
                it.tom shouldBe LocalDate.of(2026, Month.JUNE, 19)
            }

        ventetidUtregner
            .finnPerioderMedSammeVentetid(
                identer = listOf(fnr),
                sykmeldingId = sendtSykmelding.sykmelding.id,
                sammeVentetidRequest = SammeVentetidRequest(),
            ).also { response ->
                response.size shouldBe 2
                response.map { it.ressursId }.containsAll(
                    listOf(
                        bekreftetSykmelding.sykmelding.id,
                        sendtSykmelding.sykmelding.id,
                    ),
                )
            }
    }

    @Test
    fun `Ny periode tetter ikke hull ved beregning av ventetid for kun sendt eller bekreftet`() {
        lagBekreftetSykmeldingKafkaMessage(
            fnr = fnr,
            fom = LocalDate.of(2026, Month.JUNE, 1),
            tom = LocalDate.of(2026, Month.JUNE, 8),
        ).also { it.prosesser() }

        lagMottattSykmeldingKafkaMessage(
            fnr = fnr,
            fom = LocalDate.of(2026, Month.JUNE, 9),
            tom = LocalDate.of(2026, Month.JUNE, 10),
        ).also { it.prosesser() }

        val melding =
            lagBekreftetSykmeldingKafkaMessage(
                fnr = fnr,
                fom = LocalDate.of(2026, Month.JUNE, 11),
                tom = LocalDate.of(2026, Month.JUNE, 21),
            ).also { it.prosesser() }

        ventetidUtregner.erUtenforVentetid(
            sykmeldingId = melding.sykmelding.id,
            identer = listOf(fnr),
            erUtenforVentetidRequest = ErUtenforVentetidRequest(),
        ) shouldBe false

        ventetidUtregner
            .beregnVentetid(
                identer = listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
            ).also {
                it!!.fom shouldBe LocalDate.of(2026, Month.JUNE, 11)
                it.tom shouldBe LocalDate.of(2026, Month.JUNE, 19)
            }

        ventetidUtregner
            .finnPerioderMedSammeVentetid(
                identer = listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                sammeVentetidRequest = SammeVentetidRequest(),
            ).also { response ->
                response.size shouldBe 1
                response.map { it.ressursId }.containsAll(
                    listOf(
                        melding.sykmelding.id,
                    ),
                )
            }
    }
}
