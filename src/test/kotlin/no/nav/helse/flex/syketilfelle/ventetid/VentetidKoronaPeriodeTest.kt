package no.nav.helse.flex.syketilfelle.ventetid

import no.nav.helse.flex.syketilfelle.FellesTestOppsett
import no.nav.helse.flex.syketilfelle.erUtenforVentetid
import no.nav.helse.flex.syketilfelle.hentVenteperiode
import no.nav.helse.flex.syketilfelle.sykmelding.SykmeldingLagring
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should be false`
import org.amshove.kluent.`should be true`
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.Month

class VentetidKoronaPeriodeTest :
    FellesTestOppsett(),
    VentetidFellesOppsett {
    private val onsdag = LocalDate.of(2022, Month.JANUARY, 5)

    @Autowired
    override lateinit var sykmeldingLagring: SykmeldingLagring

    final override val fnr = "12345432123"

    @BeforeEach
    @AfterEach
    fun setUp() {
        syketilfellebitRepository.deleteAll()
    }

    @Test
    fun `Periode på 6 dager er utenfor ventetiden`() {
        val melding =
            skapApenSykmeldingKafkaMessage(
                fom = onsdag,
                tom = onsdag.plusDays(5),
                harRedusertArbeidsgiverperiode = true,
            ).also { it.publiser() }

        erUtenforVentetid(
            listOf(element = fnr),
            sykmeldingId = melding.sykmelding.id,
            ventetidRequest = VentetidRequest(),
        ).`should be true`()

        hentVenteperiode(
            listOf(fnr),
            sykmeldingId = melding.sykmelding.id,
            ventetidRequest = VentetidRequest(),
        ).venteperiode.also {
            it!!.fom `should be equal to` onsdag
            it.tom `should be equal to` onsdag.plusDays(SEKS_DAGER - 1L)
        }
    }

    @Test
    fun `Periode på 5 dager er innenfor ventetiden`() {
        val melding =
            skapApenSykmeldingKafkaMessage(
                fom = onsdag,
                tom = onsdag.plusDays(4),
                harRedusertArbeidsgiverperiode = true,
            ).also { it.publiser() }

        erUtenforVentetid(
            listOf(element = fnr),
            sykmeldingId = melding.sykmelding.id,
            ventetidRequest = VentetidRequest(),
        ).`should be false`()
    }

    @Test
    fun `Tidligere periode på 6 dager eller mer teller ikke hvis opphold er over 16 dager ved redusert venteperiode`() {
        val melding =
            skapApenSykmeldingKafkaMessage(
                fom = onsdag,
                tom = onsdag.plusDays(1),
                harRedusertArbeidsgiverperiode = true,
            ).also { it.publiser() }

        skapApenSykmeldingKafkaMessage(
            fom = onsdag.minusDays(31),
            tom = onsdag.minusDays(17),
            harRedusertArbeidsgiverperiode = true,
        ).publiser()

        erUtenforVentetid(
            listOf(element = fnr),
            sykmeldingId = melding.sykmelding.id,
            ventetidRequest = VentetidRequest(),
        ).`should be false`()
    }

    @Test
    fun `Tidligere periode teller hvis opphold er mindre enn 16 dager`() {
        val melding =
            skapApenSykmeldingKafkaMessage(
                fom = onsdag,
                tom = onsdag.plusDays(1),
                harRedusertArbeidsgiverperiode = true,
            ).also { it.publiser() }

        skapApenSykmeldingKafkaMessage(
            fom = onsdag.minusDays(22),
            tom = onsdag.minusDays(15),
            harRedusertArbeidsgiverperiode = true,
        ).publiser()

        erUtenforVentetid(
            listOf(element = fnr),
            sykmeldingId = melding.sykmelding.id,
            ventetidRequest = VentetidRequest(),
        ).`should be true`()

        hentVenteperiode(
            listOf(fnr),
            sykmeldingId = melding.sykmelding.id,
            ventetidRequest = VentetidRequest(),
        ).venteperiode.also {
            it!!.fom `should be equal to` onsdag.minusDays(22)
            it.tom `should be equal to` it.fom.plusDays(SEKS_DAGER - 1L)
        }
    }

    @Test
    fun `Tidligere periode under 6 dager er innenfor ventetiden`() {
        val melding =
            skapApenSykmeldingKafkaMessage(
                fom = onsdag,
                tom = onsdag.plusDays(1),
                harRedusertArbeidsgiverperiode = true,
            ).also { it.publiser() }

        skapApenSykmeldingKafkaMessage(
            fom = onsdag.minusDays(3),
            tom = onsdag.minusDays(2),
            harRedusertArbeidsgiverperiode = true,
        ).publiser()

        erUtenforVentetid(
            listOf(element = fnr),
            sykmeldingId = melding.sykmelding.id,
            ventetidRequest = VentetidRequest(),
        ).`should be false`()
    }

    @Test
    fun `Redusert venteperiode på 4 dager gjelder ikke i fra 1 desember 2021`() {
        val melding =
            skapApenSykmeldingKafkaMessage(
                fom = LocalDate.of(2021, 11, 1),
                tom = LocalDate.of(2021, 11, 4),
                harRedusertArbeidsgiverperiode = true,
            ).also { it.publiser() }

        erUtenforVentetid(
            listOf(element = fnr),
            sykmeldingId = melding.sykmelding.id,
            ventetidRequest = VentetidRequest(),
        ).`should be false`()
    }

    @Test
    fun `Periode er utenfor ventetiden når sluttdato er lik første dag av koronaperioden med seks dagers grense`() {
        val tom = LocalDate.of(2021, 12, 6)
        val melding =
            skapApenSykmeldingKafkaMessage(
                fom = tom.minusDays(5),
                tom = tom,
                harRedusertArbeidsgiverperiode = true,
            ).also { it.publiser() }

        erUtenforVentetid(
            listOf(element = fnr),
            sykmeldingId = melding.sykmelding.id,
            ventetidRequest = VentetidRequest(),
        ).`should be true`()

        hentVenteperiode(
            listOf(fnr),
            sykmeldingId = melding.sykmelding.id,
            ventetidRequest = VentetidRequest(),
        ).venteperiode.also {
            it!!.fom `should be equal to` tom.minusDays(SEKS_DAGER - 1L)
            it.tom `should be equal to` tom
        }
    }
}
