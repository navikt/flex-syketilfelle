package no.nav.helse.flex.syketilfelle.ventetid

import no.nav.helse.flex.syketilfelle.FellesTestOppsett
import no.nav.helse.flex.syketilfelle.lagBekreftetSykmeldingKafkaMessage
import no.nav.helse.flex.syketilfelle.lagMottattSykmeldingKafkaMessage
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.Month

class VentetidTilbakedateringTest : FellesTestOppsett() {
    @Autowired
    private lateinit var ventetidUtregner: VentetidUtregner

    @BeforeEach
    @AfterEach
    fun setUp() {
        syketilfellebitRepository.deleteAll()
    }

    private val fnr = "11111111111"

    @Test
    fun `To perioder til sammen 16 dager har samme ventetid`() {
        val melding1 =
            lagBekreftetSykmeldingKafkaMessage(
                fnr = fnr,
                fom = LocalDate.of(2026, Month.JUNE, 1),
                tom = LocalDate.of(2026, Month.JUNE, 8),
            ).also { it.prosesser() }

        val melding2 =
            lagBekreftetSykmeldingKafkaMessage(
                fnr = fnr,
                fom = LocalDate.of(2026, Month.JUNE, 9),
                tom = LocalDate.of(2026, Month.JUNE, 16),
            ).also { it.prosesser() }

        ventetidUtregner
            .erUtenforVentetid(
                identer = listOf(fnr),
                sykmeldingId = melding1.sykmelding.id,
                erUtenforVentetidRequest = ErUtenforVentetidRequest(),
            ) `should be` false

        ventetidUtregner
            .beregnVentetid(
                identer = listOf(fnr),
                sykmeldingId = melding1.sykmelding.id,
                ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
            ).also {
                it!!.fom `should be equal to` LocalDate.of(2026, Month.JUNE, 1)
                it.tom `should be equal to` LocalDate.of(2026, Month.JUNE, 16)
            }

        ventetidUtregner
            .erUtenforVentetid(
                identer = listOf(fnr),
                sykmeldingId = melding2.sykmelding.id,
                erUtenforVentetidRequest = ErUtenforVentetidRequest(),
            ) `should be` false

        ventetidUtregner
            .beregnVentetid(
                identer = listOf(fnr),
                sykmeldingId = melding2.sykmelding.id,
                ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
            ).also {
                it!!.fom `should be equal to` LocalDate.of(2026, Month.JUNE, 1)
                it.tom `should be equal to` LocalDate.of(2026, Month.JUNE, 16)
            }
    }

    @Test
    fun `Begge perioder på til sammen 17 dager er utenfor ventetiden`() {
        val melding1 =
            lagBekreftetSykmeldingKafkaMessage(
                fnr = fnr,
                fom = LocalDate.of(2026, Month.JUNE, 1),
                tom = LocalDate.of(2026, Month.JUNE, 8),
            ).also { it.prosesser() }

        val melding2 =
            lagBekreftetSykmeldingKafkaMessage(
                fnr = fnr,
                fom = LocalDate.of(2026, Month.JUNE, 9),
                tom = LocalDate.of(2026, Month.JUNE, 17),
            ).also { it.prosesser() }

        ventetidUtregner.erUtenforVentetid(
            identer = listOf(fnr),
            sykmeldingId = melding1.sykmelding.id,
            erUtenforVentetidRequest = ErUtenforVentetidRequest(),
        ) `should be` true

        ventetidUtregner
            .beregnVentetid(
                identer = listOf(fnr),
                sykmeldingId = melding1.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).also {
                it!!.fom `should be equal to` LocalDate.of(2026, Month.JUNE, 1)
                it.tom `should be equal to` LocalDate.of(2026, Month.JUNE, 16)
            }

        ventetidUtregner.erUtenforVentetid(
            identer = listOf(fnr),
            sykmeldingId = melding2.sykmelding.id,
            erUtenforVentetidRequest = ErUtenforVentetidRequest(),
        ) `should be` true

        ventetidUtregner
            .beregnVentetid(
                identer = listOf(fnr),
                sykmeldingId = melding2.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).also {
                it!!.fom `should be equal to` LocalDate.of(2026, Month.JUNE, 1)
                it.tom `should be equal to` LocalDate.of(2026, Month.JUNE, 16)
            }
    }

    @Test
    fun `Tilbakedatert bekreftet periode er innenfor ventetiden når kun bekreftet periode tas med i beregningen`() {
        lagMottattSykmeldingKafkaMessage(
            fnr = fnr,
            fom = LocalDate.of(2026, Month.JUNE, 9),
            tom = LocalDate.of(2026, Month.JUNE, 17),
        ).also { it.prosesser() }

        val melding =
            lagBekreftetSykmeldingKafkaMessage(
                fnr = fnr,
                fom = LocalDate.of(2026, Month.JUNE, 1),
                tom = LocalDate.of(2026, Month.JUNE, 8),
            ).also { it.prosesser() }

        ventetidUtregner.erUtenforVentetid(
            identer = listOf(fnr),
            sykmeldingId = melding.sykmelding.id,
            erUtenforVentetidRequest = ErUtenforVentetidRequest(),
        ) `should be` false

        ventetidUtregner
            .beregnVentetid(
                identer = listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
            ).also {
                it!!.fom `should be equal to` LocalDate.of(2026, Month.JUNE, 1)
                it.tom `should be equal to` LocalDate.of(2026, Month.JUNE, 8)
            }
    }

    @Test
    fun `Tilbakedatert bekreftet periode er utenfor ventetiden når det finnes en senere bekreftet periode`() {
        lagBekreftetSykmeldingKafkaMessage(
            fnr = fnr,
            fom = LocalDate.of(2026, Month.JUNE, 9),
            tom = LocalDate.of(2026, Month.JUNE, 17),
        ).also { it.prosesser() }

        val melding =
            lagBekreftetSykmeldingKafkaMessage(
                fnr = fnr,
                fom = LocalDate.of(2026, Month.JUNE, 1),
                tom = LocalDate.of(2026, Month.JUNE, 8),
            ).also { it.prosesser() }

        ventetidUtregner.erUtenforVentetid(
            identer = listOf(fnr),
            sykmeldingId = melding.sykmelding.id,
            erUtenforVentetidRequest = ErUtenforVentetidRequest(),
        ) `should be` true

        ventetidUtregner
            .beregnVentetid(
                identer = listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).also {
                it!!.fom `should be equal to` LocalDate.of(2026, Month.JUNE, 1)
                it.tom `should be equal to` LocalDate.of(2026, Month.JUNE, 16)
            }
    }

    @Test
    fun `Ventetid beregnes fra første periode når det er opphold mellom flere perioder`() {
        lagBekreftetSykmeldingKafkaMessage(
            fnr = fnr,
            fom = LocalDate.of(2026, Month.JULY, 13),
            tom = LocalDate.of(2026, Month.AUGUST, 2),
        ).also { it.prosesser() }

        lagBekreftetSykmeldingKafkaMessage(
            fnr = fnr,
            fom = LocalDate.of(2026, Month.AUGUST, 10),
            tom = LocalDate.of(2026, Month.AUGUST, 21),
        ).also { it.prosesser() }

        val melding1 =
            lagBekreftetSykmeldingKafkaMessage(
                fnr = fnr,
                fom = LocalDate.of(2026, Month.AUGUST, 28),
                tom = LocalDate.of(2026, Month.SEPTEMBER, 6),
            ).also { it.prosesser() }

        val melding2 =
            lagBekreftetSykmeldingKafkaMessage(
                fnr = fnr,
                fom = LocalDate.of(2026, Month.SEPTEMBER, 7),
                tom = LocalDate.of(2026, Month.SEPTEMBER, 20),
            ).also { it.prosesser() }

        ventetidUtregner
            .beregnVentetid(
                identer = listOf(fnr),
                sykmeldingId = melding1.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).also {
                it!!.fom `should be equal to` LocalDate.of(2026, Month.JULY, 13)
                it.tom `should be equal to` LocalDate.of(2026, Month.JULY, 28)
            }

        ventetidUtregner
            .finnPerioderMedSammeVentetid(
                sykmeldingId = melding1.sykmelding.id,
                identer = listOf(fnr),
                sammeVentetidRequest = SammeVentetidRequest(),
            ).also {
                it.size `should be equal to` 4
            }

        ventetidUtregner
            .beregnVentetid(
                identer = listOf(fnr),
                sykmeldingId = melding2.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).also {
                it!!.fom `should be equal to` LocalDate.of(2026, Month.JULY, 13)
                it.tom `should be equal to` LocalDate.of(2026, Month.JULY, 28)
            }

        ventetidUtregner
            .finnPerioderMedSammeVentetid(
                sykmeldingId = melding2.sykmelding.id,
                identer = listOf(fnr),
                sammeVentetidRequest = SammeVentetidRequest(),
            ).also {
                it.size `should be equal to` 4
            }
    }
}
