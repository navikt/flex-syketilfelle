package no.nav.helse.flex.syketilfelle.ventetid

import no.nav.helse.flex.syketilfelle.FellesTestOppsett
import no.nav.helse.flex.syketilfelle.lagBekreftetSykmeldingKafkaMessage
import no.nav.helse.flex.syketilfelle.lagMottattSykmeldingKafkaMessage
import no.nav.helse.flex.syketilfelle.lagSyketilfelleBit
import no.nav.helse.flex.syketilfelle.syketilfellebit.Tag
import no.nav.syfo.model.sykmelding.arbeidsgiver.SykmeldingsperiodeAGDTO
import no.nav.syfo.model.sykmelding.model.PeriodetypeDTO
import no.nav.syfo.model.sykmeldingstatus.ShortNameDTO
import no.nav.syfo.model.sykmeldingstatus.SporsmalOgSvarDTO
import no.nav.syfo.model.sykmeldingstatus.SvartypeDTO
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should be false`
import org.amshove.kluent.`should be true`
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.Month
import java.util.UUID

class VentetidUtregnerTest : FellesTestOppsett() {
    @Autowired
    private lateinit var ventetidUtregner: VentetidUtregner

    @BeforeEach
    @AfterEach
    fun setUp() {
        syketilfellebitRepository.deleteAll()
    }

    private val fnr = "11111111111"

    /**
     * 1. juli 2024 er en mandag.
     *
     * Ventetid: 16 dager.
     *
     * Alle kalenderdager telles i ventetiden på 16 dager. Også helgedager. Samtidig betaler
     * ikke Nav for helgedager. Derfor telles lørdag og søndag hvis ventetiden starter i en helg, men
     * ikke hvis den avsluttes i en helg.
     */
    @Nested
    inner class Grunnregler {
        @Test
        fun `Periode på 16 dager er innenfor ventetiden`() {
            val melding =
                lagBekreftetSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2024, Month.JULY, 1),
                    tom = LocalDate.of(2024, Month.JULY, 16),
                )

            erUtenforVentetid(
                sykmeldingId = melding.sykmelding.id,
                identer = listOf(fnr),
                erUtenforVentetidRequest = ErUtenforVentetidRequest(sykmeldingKafkaMessage = melding),
            ) `should be` false

            hentVentetid(
                identer = listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                ventetidRequest = VentetidRequest(sykmeldingKafkaMessage = melding),
            ).ventetid `should be` null
        }

        @Test
        fun `Periode på 17 dager er utenfor ventetiden`() {
            val melding =
                lagBekreftetSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2024, Month.JULY, 1),
                    tom = LocalDate.of(2024, Month.JULY, 17),
                )

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                erUtenforVentetidRequest = ErUtenforVentetidRequest(sykmeldingKafkaMessage = melding),
            ).`should be true`()

            hentVentetid(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                ventetidRequest = VentetidRequest(sykmeldingKafkaMessage = melding),
            ).ventetid.also {
                it!!.fom `should be equal to` LocalDate.of(2024, Month.JULY, 1)
                it.tom `should be equal to` LocalDate.of(2024, Month.JULY, 16)
            }
        }

        @Test
        fun `Periode på 17 dager som slutter på lørdag er innenfor ventetiden`() {
            val melding =
                lagBekreftetSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2024, Month.JULY, 4),
                    tom = LocalDate.of(2024, Month.JULY, 20),
                )

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                erUtenforVentetidRequest = ErUtenforVentetidRequest(sykmeldingKafkaMessage = melding),
            ).`should be false`()

            hentVentetid(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                ventetidRequest = VentetidRequest(sykmeldingKafkaMessage = melding),
            ).ventetid `should be` null
        }

        @Test
        fun `Periode på 17 dager som slutter på søndag er innenfor ventetiden`() {
            val melding =
                lagBekreftetSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2024, Month.JULY, 5),
                    tom = LocalDate.of(2024, Month.JULY, 21),
                )

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                erUtenforVentetidRequest = ErUtenforVentetidRequest(sykmeldingKafkaMessage = melding),
            ).`should be false`()

            hentVentetid(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                ventetidRequest = VentetidRequest(sykmeldingKafkaMessage = melding),
            ).ventetid `should be` null
        }

        @Test
        fun `Periode på 17 dager som starter på lørdag er utenfor ventetiden`() {
            val melding =
                lagBekreftetSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2024, Month.JUNE, 29),
                    tom = LocalDate.of(2024, Month.JULY, 15),
                )

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                erUtenforVentetidRequest = ErUtenforVentetidRequest(sykmeldingKafkaMessage = melding),
            ).`should be true`()

            hentVentetid(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                ventetidRequest = VentetidRequest(sykmeldingKafkaMessage = melding),
            ).ventetid.also {
                it!!.fom `should be equal to` LocalDate.of(2024, Month.JUNE, 29)
                it.tom `should be equal to` LocalDate.of(2024, Month.JULY, 14)
            }
        }

        @Test
        fun `Periode på 17 dager som starter på søndag er utenfor ventetiden`() {
            val melding =
                lagBekreftetSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2024, Month.JUNE, 30),
                    tom = LocalDate.of(2024, Month.JULY, 16),
                )

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                erUtenforVentetidRequest = ErUtenforVentetidRequest(sykmeldingKafkaMessage = melding),
            ).`should be true`()

            hentVentetid(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                ventetidRequest = VentetidRequest(sykmeldingKafkaMessage = melding),
            ).ventetid.also {
                it!!.fom `should be equal to` LocalDate.of(2024, Month.JUNE, 30)
                it.tom `should be equal to` LocalDate.of(2024, Month.JULY, 15)
            }
        }

        @Test
        fun `Periode på 17 dager og tag REISETILSKUDD er utenfor ventetiden`() {
            val melding =
                lagBekreftetSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2024, Month.JULY, 1),
                    tom = LocalDate.of(2024, Month.JULY, 17),
                    type = PeriodetypeDTO.REISETILSKUDD,
                )

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                erUtenforVentetidRequest = ErUtenforVentetidRequest(sykmeldingKafkaMessage = melding),
            ).`should be true`()

            hentVentetid(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                ventetidRequest = VentetidRequest(sykmeldingKafkaMessage = melding),
            ).ventetid.also {
                it!!.fom `should be equal to` LocalDate.of(2024, Month.JULY, 1)
                it.tom `should be equal to` LocalDate.of(2024, Month.JULY, 16)
            }
        }

        @Test
        fun `Periode på 17 dager og tag AVVENTENDE er ikke utenfor ventetiden`() {
            val melding =
                lagBekreftetSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2024, Month.JULY, 1),
                    tom = LocalDate.of(2024, Month.JULY, 17),
                    type = PeriodetypeDTO.AVVENTENDE,
                )

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                erUtenforVentetidRequest = ErUtenforVentetidRequest(sykmeldingKafkaMessage = melding),
            ).`should be false`()

            hentVentetid(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                ventetidRequest = VentetidRequest(sykmeldingKafkaMessage = melding),
            ).ventetid `should be` null
        }
    }

    /**
     * Perioder regnes som én periode når de henger sammen heller det kun er helgedager mellom periodene.
     */
    @Nested
    inner class SammenhengendePerioder {
        @Test
        fun `To perioder til sammen 16 dager er innenfor ventetiden`() {
            val melding1 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2024, Month.JULY, 1),
                    tom = LocalDate.of(2024, Month.JULY, 8),
                ).also { it.prosesser() }

            val melding2 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2024, Month.JULY, 9),
                    tom = LocalDate.of(2024, Month.JULY, 16),
                ).also { it.prosesser() }

            verifiserAtBiterErLagret(2)

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding1.sykmelding.id,
                erUtenforVentetidRequest = ErUtenforVentetidRequest(),
            ).`should be false`()

            hentVentetid(
                listOf(fnr),
                sykmeldingId = melding1.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).ventetid `should be` null

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding2.sykmelding.id,
                erUtenforVentetidRequest = ErUtenforVentetidRequest(),
            ).`should be false`()

            hentVentetid(
                listOf(fnr),
                sykmeldingId = melding2.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).ventetid `should be` null
        }

        @Test
        fun `To perioder til sammen 17 dager er utenfor ventetiden`() {
            val melding1 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2024, Month.JULY, 1),
                    tom = LocalDate.of(2024, Month.JULY, 8),
                ).also { it.prosesser() }

            val melding2 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2024, Month.JULY, 9),
                    tom = LocalDate.of(2024, Month.JULY, 17),
                ).also { it.prosesser() }

            verifiserAtBiterErLagret(2)

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding1.sykmelding.id,
                erUtenforVentetidRequest = ErUtenforVentetidRequest(),
            ).`should be false`()

            hentVentetid(
                listOf(fnr),
                sykmeldingId = melding1.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).ventetid `should be` null

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding2.sykmelding.id,
                erUtenforVentetidRequest = ErUtenforVentetidRequest(),
            ).`should be true`()

            hentVentetid(
                listOf(fnr),
                sykmeldingId = melding2.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).ventetid.also {
                it!!.fom `should be equal to` LocalDate.of(2024, Month.JULY, 1)
                it.tom `should be equal to` LocalDate.of(2024, Month.JULY, 16)
            }
        }

        @Test
        fun `To perioder til sammen over 16 dager med lørdag mellom er utenfor ventetiden`() {
            val melding1 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2024, Month.JULY, 1),
                    tom = LocalDate.of(2024, Month.JULY, 12),
                ).also { it.prosesser() }

            val melding2 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2024, Month.JULY, 14),
                    tom = LocalDate.of(2024, Month.JULY, 21),
                ).also { it.prosesser() }

            verifiserAtBiterErLagret(2)

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding1.sykmelding.id,
                erUtenforVentetidRequest = ErUtenforVentetidRequest(),
            ).`should be false`()

            hentVentetid(
                listOf(fnr),
                sykmeldingId = melding1.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).ventetid `should be` null

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding2.sykmelding.id,
                erUtenforVentetidRequest = ErUtenforVentetidRequest(),
            ).`should be true`()

            hentVentetid(
                listOf(fnr),
                sykmeldingId = melding2.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).ventetid.also {
                it!!.fom `should be equal to` LocalDate.of(2024, Month.JULY, 1)
                it.tom `should be equal to` LocalDate.of(2024, Month.JULY, 16)
            }
        }

        @Test
        fun `To perioder til sammen over 16 dager med søndag mellom er utenfor ventetiden`() {
            val melding1 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2024, Month.JULY, 1),
                    tom = LocalDate.of(2024, Month.JULY, 13),
                ).also { it.prosesser() }

            val melding2 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2024, Month.JULY, 15),
                    tom = LocalDate.of(2024, Month.JULY, 21),
                ).also { it.prosesser() }

            verifiserAtBiterErLagret(2)

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding1.sykmelding.id,
                erUtenforVentetidRequest = ErUtenforVentetidRequest(),
            ).`should be false`()

            hentVentetid(
                listOf(fnr),
                sykmeldingId = melding1.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).ventetid `should be` null

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding2.sykmelding.id,
                erUtenforVentetidRequest = ErUtenforVentetidRequest(),
            ).`should be true`()

            hentVentetid(
                listOf(fnr),
                sykmeldingId = melding2.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).ventetid.also {
                it!!.fom `should be equal to` LocalDate.of(2024, Month.JULY, 1)
                it.tom `should be equal to` LocalDate.of(2024, Month.JULY, 16)
            }
        }

        @Test
        fun `To perioder til sammen over 16 dager med lørdag og søndag mellom er utenfor ventetiden`() {
            val melding1 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2024, Month.JULY, 1),
                    tom = LocalDate.of(2024, Month.JULY, 12),
                ).also { it.prosesser() }

            val melding2 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2024, Month.JULY, 15),
                    tom = LocalDate.of(2024, Month.JULY, 21),
                ).also { it.prosesser() }

            verifiserAtBiterErLagret(2)

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding1.sykmelding.id,
                erUtenforVentetidRequest = ErUtenforVentetidRequest(),
            ).`should be false`()

            hentVentetid(
                listOf(fnr),
                sykmeldingId = melding1.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).ventetid `should be` null

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding2.sykmelding.id,
                erUtenforVentetidRequest = ErUtenforVentetidRequest(),
            ).`should be true`()

            hentVentetid(
                listOf(fnr),
                sykmeldingId = melding2.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).ventetid.also {
                it!!.fom `should be equal to` LocalDate.of(2024, Month.JULY, 1)
                it.tom `should be equal to` LocalDate.of(2024, Month.JULY, 16)
            }
        }

        @Test
        fun `Tre perioder til sammen 16 dager er innenfor ventetiden`() {
            val melding1 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2024, Month.JULY, 1),
                    tom = LocalDate.of(2024, Month.JULY, 5),
                ).also { it.prosesser() }

            val melding2 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2024, Month.JULY, 6),
                    tom = LocalDate.of(2024, Month.JULY, 10),
                ).also { it.prosesser() }

            val melding3 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2024, Month.JULY, 11),
                    tom = LocalDate.of(2024, Month.JULY, 16),
                ).also { it.prosesser() }

            verifiserAtBiterErLagret(3)

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding1.sykmelding.id,
                erUtenforVentetidRequest = ErUtenforVentetidRequest(),
            ).`should be false`()

            hentVentetid(
                listOf(fnr),
                sykmeldingId = melding1.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).ventetid `should be` null

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding2.sykmelding.id,
                erUtenforVentetidRequest = ErUtenforVentetidRequest(),
            ).`should be false`()

            hentVentetid(
                listOf(fnr),
                sykmeldingId = melding2.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).ventetid `should be` null

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding3.sykmelding.id,
                erUtenforVentetidRequest = ErUtenforVentetidRequest(),
            ).`should be false`()

            hentVentetid(
                listOf(fnr),
                sykmeldingId = melding3.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).ventetid `should be` null
        }

        @Test
        fun `Tre perioder til sammen 17 dager er utenfor ventetiden`() {
            val melding1 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2024, Month.JULY, 1),
                    tom = LocalDate.of(2024, Month.JULY, 5),
                ).also { it.prosesser() }

            val melding2 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2024, Month.JULY, 6),
                    tom = LocalDate.of(2024, Month.JULY, 10),
                ).also { it.prosesser() }

            val melding3 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2024, Month.JULY, 11),
                    tom = LocalDate.of(2024, Month.JULY, 17),
                ).also { it.prosesser() }

            verifiserAtBiterErLagret(3)

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding1.sykmelding.id,
                erUtenforVentetidRequest = ErUtenforVentetidRequest(),
            ).`should be false`()

            hentVentetid(
                listOf(fnr),
                sykmeldingId = melding1.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).ventetid `should be` null

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding2.sykmelding.id,
                erUtenforVentetidRequest = ErUtenforVentetidRequest(),
            ).`should be false`()

            hentVentetid(
                listOf(fnr),
                sykmeldingId = melding2.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).ventetid `should be` null

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding3.sykmelding.id,
                erUtenforVentetidRequest = ErUtenforVentetidRequest(),
            ).`should be true`()

            hentVentetid(
                listOf(fnr),
                sykmeldingId = melding3.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).ventetid.also {
                it!!.fom `should be equal to` LocalDate.of(2024, Month.JULY, 1)
                it.tom `should be equal to` LocalDate.of(2024, Month.JULY, 16)
            }
        }

        @Test
        fun `Tre perioder til sammen over 16 dager på grunn av helg er utenfor ventetiden`() {
            val melding1 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2024, Month.JULY, 1),
                    tom = LocalDate.of(2024, Month.JULY, 5),
                ).also { it.prosesser() }

            val melding2 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2024, Month.JULY, 6),
                    tom = LocalDate.of(2024, Month.JULY, 12),
                ).also { it.prosesser() }

            val melding3 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2024, Month.JULY, 14),
                    tom = LocalDate.of(2024, Month.JULY, 17),
                ).also { it.prosesser() }

            verifiserAtBiterErLagret(3)

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding1.sykmelding.id,
                erUtenforVentetidRequest = ErUtenforVentetidRequest(),
            ).`should be false`()

            hentVentetid(
                listOf(fnr),
                sykmeldingId = melding1.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).ventetid `should be` null

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding2.sykmelding.id,
                erUtenforVentetidRequest = ErUtenforVentetidRequest(),
            ).`should be false`()

            hentVentetid(
                listOf(fnr),
                sykmeldingId = melding2.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).ventetid `should be` null

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding3.sykmelding.id,
                erUtenforVentetidRequest = ErUtenforVentetidRequest(),
            ).`should be true`()

            hentVentetid(
                listOf(fnr),
                sykmeldingId = melding3.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).ventetid.also {
                it!!.fom `should be equal to` LocalDate.of(2024, Month.JULY, 1)
                it.tom `should be equal to` LocalDate.of(2024, Month.JULY, 16)
            }
        }

        @Test
        fun `To perioder til sammen 17 dager og siste periode er én dag lang er utenfor ventetiden`() {
            val melding1 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2024, Month.JULY, 1),
                    tom = LocalDate.of(2024, Month.JULY, 16),
                ).also { it.prosesser() }

            val melding2 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2024, Month.JULY, 17),
                    tom = LocalDate.of(2024, Month.JULY, 17),
                ).also { it.prosesser() }

            verifiserAtBiterErLagret(2)

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding1.sykmelding.id,
                erUtenforVentetidRequest = ErUtenforVentetidRequest(),
            ).`should be false`()

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding2.sykmelding.id,
                erUtenforVentetidRequest = ErUtenforVentetidRequest(),
            ).`should be true`()

            hentVentetid(
                listOf(fnr),
                sykmeldingId = melding2.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).ventetid.also {
                it!!.fom `should be equal to` LocalDate.of(2024, Month.JULY, 1)
                it.tom `should be equal to` LocalDate.of(2024, Month.JULY, 16)
            }
        }

        @Test
        fun `To perioder i samme sykmelding merges likt perioder fra to sykmeldinger`() {
            val melding =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2024, Month.JULY, 1),
                    tom = LocalDate.of(2024, Month.JULY, 16),
                )

            with(melding) {
                val p = sykmelding.sykmeldingsperioder.first()
                copy(
                    sykmelding =
                        sykmelding.copy(
                            sykmeldingsperioder =
                                listOf(
                                    p.copy(),
                                    p.copy(
                                        fom = LocalDate.of(2024, Month.JULY, 17),
                                        tom = LocalDate.of(2024, Month.JULY, 31),
                                    ),
                                ),
                        ),
                )
            }.also { it.prosesser() }

            verifiserAtBiterErLagret(2)

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                erUtenforVentetidRequest = ErUtenforVentetidRequest(),
            ).`should be true`()

            hentVentetid(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).ventetid.also {
                it!!.fom `should be equal to` LocalDate.of(2024, Month.JULY, 1)
                it.tom `should be equal to` LocalDate.of(2024, Month.JULY, 16)
            }
        }

        @Test
        fun `To perioder til sammen 17 dager er utenfor ventetiden når første periode har tag REISETILSKUDD`() {
            val melding1 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2024, Month.JULY, 1),
                    tom = LocalDate.of(2024, Month.JULY, 8),
                    type = PeriodetypeDTO.REISETILSKUDD,
                ).also { it.prosesser() }

            val melding2 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2024, Month.JULY, 9),
                    tom = LocalDate.of(2024, Month.JULY, 17),
                ).also { it.prosesser() }

            verifiserAtBiterErLagret(2)

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding1.sykmelding.id,
                erUtenforVentetidRequest = ErUtenforVentetidRequest(),
            ).`should be false`()

            hentVentetid(
                listOf(fnr),
                sykmeldingId = melding1.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).ventetid `should be` null

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding2.sykmelding.id,
                erUtenforVentetidRequest = ErUtenforVentetidRequest(),
            ).`should be true`()

            hentVentetid(
                listOf(fnr),
                sykmeldingId = melding2.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).ventetid.also {
                it!!.fom `should be equal to` LocalDate.of(2024, Month.JULY, 1)
                it.tom `should be equal to` LocalDate.of(2024, Month.JULY, 16)
            }
        }

        @Test
        fun `To perioder til sammen 17 dager er innenfor ventetiden når første periode har tag AVVENTENDE`() {
            val melding1 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2024, Month.JULY, 1),
                    tom = LocalDate.of(2024, Month.JULY, 8),
                    type = PeriodetypeDTO.AVVENTENDE,
                ).also { it.prosesser() }

            val melding2 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2024, Month.JULY, 9),
                    tom = LocalDate.of(2024, Month.JULY, 17),
                ).also { it.prosesser() }

            verifiserAtBiterErLagret(2)

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding1.sykmelding.id,
                erUtenforVentetidRequest = ErUtenforVentetidRequest(),
            ).`should be false`()

            hentVentetid(
                listOf(fnr),
                sykmeldingId = melding1.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).ventetid `should be` null

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding2.sykmelding.id,
                erUtenforVentetidRequest = ErUtenforVentetidRequest(),
            ).`should be false`()

            hentVentetid(
                listOf(fnr),
                sykmeldingId = melding2.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).ventetid `should be` null
        }
    }

    /**
     * Tidligere utbetalte perioder (perioder som i seg selv er utenfor ventetiden) teller på ventetiden for
     * neste periode hvis oppholdet mellom den gamle og nye perioden er 16 dager eller mindre
     *
     * Kort Periode: Sykmeldingsperiode på 16 dager eller mindre.
     * Lang Periode: Sykmeldingsperiode lengre enn 16 dager.
     */
    @Nested
    inner class OppholdMellomPerioder {
        @Test
        fun `Kort periode påvirker ikke kort periode selv om opphold er kortere enn 17 dager`() {
            lagMottattSykmeldingKafkaMessage(
                fnr = fnr,
                fom = LocalDate.of(2024, Month.JULY, 1),
                tom = LocalDate.of(2024, Month.JULY, 16),
            ).also { it.prosesser() }

            val melding =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2024, Month.JULY, 18),
                    tom = LocalDate.of(2024, Month.JULY, 31),
                ).also { it.prosesser() }

            verifiserAtBiterErLagret(2)

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                erUtenforVentetidRequest = ErUtenforVentetidRequest(),
            ).`should be false`()
        }

        @Test
        fun `Kort periode påvirker ikke lang periode selv om opphold er kortere enn 17 dager`() {
            lagMottattSykmeldingKafkaMessage(
                fnr = fnr,
                fom = LocalDate.of(2024, Month.JULY, 1),
                tom = LocalDate.of(2024, Month.JULY, 10),
            ).also { it.prosesser() }

            val melding =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2024, Month.JULY, 12),
                    tom = LocalDate.of(2024, Month.JULY, 31),
                ).also { it.prosesser() }

            verifiserAtBiterErLagret(2)

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                erUtenforVentetidRequest = ErUtenforVentetidRequest(),
            ).`should be true`()

            hentVentetid(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).ventetid.also {
                it!!.fom `should be equal to` LocalDate.of(2024, Month.JULY, 12)
                it.tom `should be equal to` LocalDate.of(2024, Month.JULY, 27)
            }
        }

        @Test
        fun `Lang periode påvirker kort periode teller hvis opphold er kortere enn 17 dager`() {
            lagMottattSykmeldingKafkaMessage(
                fnr = fnr,
                fom = LocalDate.of(2024, Month.JULY, 1),
                tom = LocalDate.of(2024, Month.JULY, 17),
            ).also { it.prosesser() }

            val melding =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2024, Month.JULY, 21),
                    tom = LocalDate.of(2024, Month.JULY, 31),
                ).also { it.prosesser() }

            verifiserAtBiterErLagret(2)

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                erUtenforVentetidRequest = ErUtenforVentetidRequest(),
            ).`should be true`()

            // Ventetiden fra lang periode er gjeldende for kort periode når opphold er mindre enn 17 dager.
            hentVentetid(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).ventetid.also {
                it!!.fom `should be equal to` LocalDate.of(2024, Month.JULY, 1)
                it.tom `should be equal to` LocalDate.of(2024, Month.JULY, 16)
            }
        }

        @Test
        fun `Lang periode påvirker ikke neste periode hvis opphold er lengre enn 16 dager`() {
            lagMottattSykmeldingKafkaMessage(
                fnr = fnr,
                fom = LocalDate.of(2024, Month.JULY, 1),
                tom = LocalDate.of(2024, Month.JULY, 17),
            ).also { it.prosesser() }

            val melding =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2024, Month.AUGUST, 3),
                    tom = LocalDate.of(2024, Month.AUGUST, 16),
                ).also { it.prosesser() }

            verifiserAtBiterErLagret(2)

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                erUtenforVentetidRequest = ErUtenforVentetidRequest(),
            ).`should be false`()
        }
    }

    @Nested
    inner class OverlappendePerioder {
        @Test
        fun `Siste sykmelding har flere periode og starter samtidig og slutter før første periode`() {
            val melding1 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2025, Month.SEPTEMBER, 1),
                    tom = LocalDate.of(2025, Month.SEPTEMBER, 30),
                ).also { it.prosesser() }

            val sykmeldingKafkaMessage = lagMottattSykmeldingKafkaMessage(fnr)

            val melding2 =
                sykmeldingKafkaMessage
                    .copy(
                        sykmelding =
                            sykmeldingKafkaMessage.sykmelding.copy(
                                sykmeldingsperioder =
                                    listOf(
                                        SykmeldingsperiodeAGDTO(
                                            fom = LocalDate.of(2025, Month.SEPTEMBER, 1),
                                            tom = LocalDate.of(2025, Month.SEPTEMBER, 4),
                                            type = PeriodetypeDTO.AKTIVITET_IKKE_MULIG,
                                            reisetilskudd = false,
                                            aktivitetIkkeMulig = null,
                                            behandlingsdager = null,
                                            gradert = null,
                                            innspillTilArbeidsgiver = null,
                                        ),
                                        SykmeldingsperiodeAGDTO(
                                            fom = LocalDate.of(2025, Month.SEPTEMBER, 5),
                                            tom = LocalDate.of(2025, Month.SEPTEMBER, 8),
                                            type = PeriodetypeDTO.AKTIVITET_IKKE_MULIG,
                                            reisetilskudd = false,
                                            aktivitetIkkeMulig = null,
                                            behandlingsdager = null,
                                            gradert = null,
                                            innspillTilArbeidsgiver = null,
                                        ),
                                    ),
                            ),
                    ).also { it.prosesser() }

            verifiserAtBiterErLagret(3)

            erUtenforVentetid(
                listOf(fnr),
                melding1.sykmelding.id,
                ErUtenforVentetidRequest(),
            ) `should be` true

            hentVentetid(
                sykmeldingId = melding1.sykmelding.id,
                identer = listOf(fnr),
                ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
            ).ventetid!!.also {
                it.fom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 1)
                it.tom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 16)
            }

            erUtenforVentetid(
                listOf(fnr),
                melding2.sykmelding.id,
                ErUtenforVentetidRequest(),
            ) `should be` false

            hentVentetid(
                sykmeldingId = melding2.sykmelding.id,
                identer = listOf(fnr),
                ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
            ).ventetid!!.also {
                it.fom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 1)
                it.tom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 8)
            }
        }
    }

    @Nested
    inner class Egenmeldingsdager {
        @Test
        fun `Egenmeldingsdag uten opphold til sykmeldingsperioden tas med i ventetiden`() {
            val sykmeldingKafkaMessage =
                lagBekreftetSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2025, Month.SEPTEMBER, 1),
                    tom = LocalDate.of(2025, Month.SEPTEMBER, 20),
                    sporsmals =
                        listOf(
                            SporsmalOgSvarDTO(
                                tekst = "Hvilke dager var du borte fra jobb før datoen sykmeldingen gjelder fra?",
                                shortName = ShortNameDTO.PERIODE,
                                svar =
                                    """
                                    [{
                                     "fom":"${LocalDate.of(2025, Month.AUGUST, 31)}",
                                     "tom":"${LocalDate.of(2025, Month.AUGUST, 31)}"
                                    }]
                                    """.trimIndent(),
                                svartype = SvartypeDTO.PERIODER,
                            ),
                        ),
                )

            ventetidUtregner
                .erUtenforVentetid(
                    sykmeldingId = sykmeldingKafkaMessage.sykmelding.id,
                    listOf(fnr),
                    erUtenforVentetidRequest = ErUtenforVentetidRequest(sykmeldingKafkaMessage = sykmeldingKafkaMessage),
                ).`should be true`()

            ventetidUtregner
                .beregnVentetid(
                    sykmeldingId = sykmeldingKafkaMessage.sykmelding.id,
                    listOf(fnr),
                    ventetidRequest = VentetidRequest(sykmeldingKafkaMessage = sykmeldingKafkaMessage),
                ).also {
                    it!!.fom `should be equal to` LocalDate.of(2025, Month.AUGUST, 31)
                    it.tom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 15)
                }
        }

        @Test
        fun `Egenmeldingsdag som tilleggsopplysning uten opphold til sykmeldingsperioden tas med i ventetiden`() {
            val melding =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2024, Month.JULY, 1),
                    tom = LocalDate.of(2024, Month.JULY, 16),
                ).also { it.prosesser() }

            val tilleggsopplysninger =
                Tilleggsopplysninger(
                    egenmeldingsperioder =
                        listOf(
                            FomTomPeriode(
                                fom = LocalDate.of(2024, Month.JUNE, 30),
                                tom = LocalDate.of(2024, Month.JUNE, 30),
                            ),
                        ),
                )

            verifiserAtBiterErLagret(1)

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                erUtenforVentetidRequest = ErUtenforVentetidRequest(tilleggsopplysninger = tilleggsopplysninger),
            ).`should be true`()

            hentVentetid(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                ventetidRequest = VentetidRequest(tilleggsopplysninger = tilleggsopplysninger),
            ).ventetid.also {
                it!!.fom `should be equal to` LocalDate.of(2024, Month.JUNE, 30)
                it.tom `should be equal to` LocalDate.of(2024, Month.JULY, 15)
            }
        }

        @Test
        fun `Egenmeldingsdag med helg mellom sykmeldingsperioden tas med i ventetiden`() {
            val sykmeldingKafkaMessage =
                lagBekreftetSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2025, Month.SEPTEMBER, 1),
                    tom = LocalDate.of(2025, Month.SEPTEMBER, 20),
                    sporsmals =
                        listOf(
                            SporsmalOgSvarDTO(
                                tekst = "Hvilke dager var du borte fra jobb før datoen sykmeldingen gjelder fra?",
                                shortName = ShortNameDTO.PERIODE,
                                svar =
                                    """
                                    [{
                                     "fom":"${LocalDate.of(2025, Month.AUGUST, 29)}",
                                     "tom":"${LocalDate.of(2025, Month.AUGUST, 29)}"
                                    }]
                                    """.trimIndent(),
                                svartype = SvartypeDTO.PERIODER,
                            ),
                        ),
                )

            ventetidUtregner
                .erUtenforVentetid(
                    sykmeldingId = sykmeldingKafkaMessage.sykmelding.id,
                    listOf(fnr),
                    erUtenforVentetidRequest = ErUtenforVentetidRequest(sykmeldingKafkaMessage = sykmeldingKafkaMessage),
                ).`should be true`()

            ventetidUtregner
                .beregnVentetid(
                    sykmeldingId = sykmeldingKafkaMessage.sykmelding.id,
                    listOf(fnr),
                    ventetidRequest = VentetidRequest(sykmeldingKafkaMessage = sykmeldingKafkaMessage),
                ).also {
                    it!!.fom `should be equal to` LocalDate.of(2025, Month.AUGUST, 29)
                    it.tom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 13)
                }
        }

        @Test
        fun `Egenmeldingsdag med ukedag mellom sykmeldingsperioden tas ikke med i ventetiden`() {
            val sykmeldingKafkaMessage =
                lagBekreftetSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2025, Month.SEPTEMBER, 3),
                    tom = LocalDate.of(2025, Month.SEPTEMBER, 23),
                    sporsmals =
                        listOf(
                            SporsmalOgSvarDTO(
                                tekst = "Hvilke dager var du borte fra jobb før datoen sykmeldingen gjelder fra?",
                                shortName = ShortNameDTO.PERIODE,
                                svar =
                                    """
                                    [{
                                     "fom":"${LocalDate.of(2025, Month.SEPTEMBER, 1)}",
                                     "tom":"${LocalDate.of(2025, Month.SEPTEMBER, 1)}"
                                    }]
                                    """.trimIndent(),
                                svartype = SvartypeDTO.PERIODER,
                            ),
                        ),
                )

            ventetidUtregner
                .erUtenforVentetid(
                    sykmeldingId = sykmeldingKafkaMessage.sykmelding.id,
                    listOf(fnr),
                    erUtenforVentetidRequest = ErUtenforVentetidRequest(sykmeldingKafkaMessage = sykmeldingKafkaMessage),
                ).`should be true`()

            ventetidUtregner
                .beregnVentetid(
                    sykmeldingId = sykmeldingKafkaMessage.sykmelding.id,
                    listOf(fnr),
                    ventetidRequest = VentetidRequest(sykmeldingKafkaMessage = sykmeldingKafkaMessage),
                ).also {
                    it!!.fom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 3)
                    it.tom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 18)
                }
        }

        @Test
        fun `Egenmeldingsdager mellom to perioder som tilleggsopplysninger tas ikke med i ventetiden`() {
            lagMottattSykmeldingKafkaMessage(
                fnr = fnr,
                fom = LocalDate.of(2025, Month.JULY, 9),
                tom = LocalDate.of(2025, Month.JULY, 25),
            ).also { it.prosesser() }

            val sykmeldingKafkaMessage =
                lagBekreftetSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2025, Month.AUGUST, 14),
                    tom = LocalDate.of(2025, Month.AUGUST, 21),
                ).also { it.prosesser() }

            val tilleggsopplysninger =
                Tilleggsopplysninger(
                    egenmeldingsperioder =
                        listOf(
                            FomTomPeriode(
                                fom = LocalDate.of(2025, Month.AUGUST, 7),
                                tom = LocalDate.of(2025, Month.AUGUST, 10),
                            ),
                        ),
                )

            ventetidUtregner
                .erUtenforVentetid(
                    sykmeldingId = sykmeldingKafkaMessage.sykmelding.id,
                    listOf(fnr),
                    erUtenforVentetidRequest =
                        ErUtenforVentetidRequest(tilleggsopplysninger = tilleggsopplysninger),
                ).`should be false`()

            ventetidUtregner
                .beregnVentetid(
                    sykmeldingId = sykmeldingKafkaMessage.sykmelding.id,
                    listOf(fnr),
                    ventetidRequest =
                        VentetidRequest(
                            tilleggsopplysninger = tilleggsopplysninger,
                            sykmeldingKafkaMessage = sykmeldingKafkaMessage,
                            returnerPerioderInnenforVentetid = true,
                        ),
                ).also {
                    it!!.fom `should be equal to` LocalDate.of(2025, Month.AUGUST, 14)
                    it.tom `should be equal to` LocalDate.of(2025, Month.AUGUST, 21)
                }
        }

        @Test
        fun `Egenmeldingsdager mellom to perioder som svar tas ikke med i ventetiden`() {
            lagMottattSykmeldingKafkaMessage(
                fnr = fnr,
                fom = LocalDate.of(2025, Month.JULY, 9),
                tom = LocalDate.of(2025, Month.JULY, 25),
            ).also { it.prosesser() }

            val sykmeldingKafkaMessage =
                lagBekreftetSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2025, Month.AUGUST, 14),
                    tom = LocalDate.of(2025, Month.AUGUST, 21),
                    sporsmals =
                        listOf(
                            SporsmalOgSvarDTO(
                                tekst = "Hvilke dager var du borte fra jobb før datoen sykmeldingen gjelder fra?",
                                shortName = ShortNameDTO.PERIODE,
                                svar =
                                    """
                                    [{
                                     "fom":"${LocalDate.of(2025, Month.AUGUST, 7)}",
                                     "tom":"${LocalDate.of(2025, Month.AUGUST, 10)}"
                                    }]
                                    """.trimIndent(),
                                svartype = SvartypeDTO.PERIODER,
                            ),
                        ),
                ).also { it.prosesser() }

            ventetidUtregner
                .erUtenforVentetid(
                    sykmeldingId = sykmeldingKafkaMessage.sykmelding.id,
                    listOf(fnr),
                    erUtenforVentetidRequest = ErUtenforVentetidRequest(sykmeldingKafkaMessage = sykmeldingKafkaMessage),
                ).`should be false`()

            ventetidUtregner
                .beregnVentetid(
                    sykmeldingId = sykmeldingKafkaMessage.sykmelding.id,
                    listOf(fnr),
                    ventetidRequest =
                        VentetidRequest(
                            returnerPerioderInnenforVentetid = true,
                        ),
                ).also {
                    it!!.fom `should be equal to` LocalDate.of(2025, Month.AUGUST, 14)
                    it.tom `should be equal to` LocalDate.of(2025, Month.AUGUST, 21)
                }
        }
    }

    @Nested
    inner class Behandlingsdager {
        @Test
        fun `Periode på 17 dager med behandlingsdager er utenfor ventetiden`() {
            val melding =
                lagBekreftetSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2024, Month.JULY, 1),
                    tom = LocalDate.of(2024, Month.JULY, 17),
                    type = PeriodetypeDTO.BEHANDLINGSDAGER,
                )

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                erUtenforVentetidRequest = ErUtenforVentetidRequest(sykmeldingKafkaMessage = melding),
            ).`should be true`()

            hentVentetid(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                ventetidRequest = VentetidRequest(sykmeldingKafkaMessage = melding),
            ).ventetid.also {
                it!!.fom `should be equal to` LocalDate.of(2024, Month.JULY, 1)
                it.tom `should be equal to` LocalDate.of(2024, Month.JULY, 16)
            }
        }

        @Test
        fun `Periode med behandlingsdager før tas med i beregningen`() {
            val melding1 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2024, Month.JULY, 1),
                    tom = LocalDate.of(2024, Month.JULY, 8),
                    type = PeriodetypeDTO.BEHANDLINGSDAGER,
                ).also { it.prosesser() }

            val melding2 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2024, Month.JULY, 9),
                    tom = LocalDate.of(2024, Month.JULY, 17),
                ).also { it.prosesser() }

            verifiserAtBiterErLagret(2)

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding1.sykmelding.id,
                erUtenforVentetidRequest = ErUtenforVentetidRequest(),
            ).`should be false`()

            hentVentetid(
                listOf(fnr),
                sykmeldingId = melding1.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).ventetid `should be` null

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding2.sykmelding.id,
                erUtenforVentetidRequest = ErUtenforVentetidRequest(),
            ).`should be true`()

            hentVentetid(
                listOf(fnr),
                sykmeldingId = melding2.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).ventetid.also {
                it!!.fom `should be equal to` LocalDate.of(2024, Month.JULY, 1)
                it.tom `should be equal to` LocalDate.of(2024, Month.JULY, 16)
            }
        }

        @Test
        fun `Periode med behandlinsdag etter tas med i beregningen`() {
            lagMottattSykmeldingKafkaMessage(
                fnr = fnr,
                fom = LocalDate.of(2024, Month.JULY, 1),
                tom = LocalDate.of(2024, Month.JULY, 8),
            ).also { it.prosesser() }

            lagMottattSykmeldingKafkaMessage(
                fnr = fnr,
                fom = LocalDate.of(2024, Month.JULY, 9),
                tom = LocalDate.of(2024, Month.JULY, 16),
            ).also { it.prosesser() }

            val melding =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2024, Month.JULY, 17),
                    tom = LocalDate.of(2024, Month.JULY, 22),
                    type = PeriodetypeDTO.BEHANDLINGSDAGER,
                ).also { it.prosesser() }

            verifiserAtBiterErLagret(3)

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                erUtenforVentetidRequest = ErUtenforVentetidRequest(),
            ).`should be true`()

            hentVentetid(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).ventetid.also {
                it!!.fom `should be equal to` LocalDate.of(2024, Month.JULY, 1)
                it.tom `should be equal to` LocalDate.of(2024, Month.JULY, 16)
            }
        }
    }

    @Nested
    inner class IkkeTellendePeriodetyper {
        @Test
        fun `Lang Avventede Sykmelding påvirker ikke ventetiden`() {
            val melding1 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2024, Month.JULY, 1),
                    tom = LocalDate.of(2024, Month.JULY, 17),
                    type = PeriodetypeDTO.AVVENTENDE,
                ).also { it.prosesser() }

            val melding2 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2024, Month.JULY, 18),
                    tom = LocalDate.of(2024, Month.JULY, 30),
                ).also { it.prosesser() }

            verifiserAtBiterErLagret(2)

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding1.sykmelding.id,
                erUtenforVentetidRequest = ErUtenforVentetidRequest(),
            ).`should be false`()

            hentVentetid(
                listOf(fnr),
                sykmeldingId = melding1.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).ventetid `should be` null

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding2.sykmelding.id,
                erUtenforVentetidRequest = ErUtenforVentetidRequest(),
            ).`should be false`()

            hentVentetid(
                listOf(fnr),
                sykmeldingId = melding2.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).ventetid `should be` null
        }
    }

    /**
     * Når flagget 'returnerPerioderInnenforVentetid' er 'true' returneres det en ventetid selv om perioden er
     * innenfor ventetiden. Ventetiden vil da være lik sykmeldingsperioden, inkludert eventuelte egenmeldingsdager.
     * Typisk brukt fra sykepengesoknad-backend når bruker har forsikring.
     */
    @Nested
    inner class PerioderInnenforVentetid {
        @Test
        fun `Periode på 16 returnerer ventetid lik perioden`() {
            val melding =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2024, Month.JULY, 1),
                    tom = LocalDate.of(2024, Month.JULY, 16),
                ).also { it.prosesser() }

            verifiserAtBiterErLagret(1)

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                erUtenforVentetidRequest = ErUtenforVentetidRequest(),
            ).`should be false`()

            hentVentetid(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
            ).ventetid.also {
                it!!.fom `should be equal to` LocalDate.of(2024, Month.JULY, 1)
                it.tom `should be equal to` LocalDate.of(2024, Month.JULY, 16)
            }
        }

        @Test
        fun `Periode på 1 dag returnerer ventetid lik perioden`() {
            val melding =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2024, Month.JULY, 1),
                    tom = LocalDate.of(2024, Month.JULY, 1),
                ).also { it.prosesser() }

            verifiserAtBiterErLagret(1)

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                erUtenforVentetidRequest = ErUtenforVentetidRequest(),
            ).`should be false`()

            hentVentetid(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
            ).ventetid.also {
                it!!.fom `should be equal to` LocalDate.of(2024, Month.JULY, 1)
                it.tom `should be equal to` LocalDate.of(2024, Month.JULY, 1)
            }
        }

        @Test
        fun `Periode som slutter på lørdag returnerer én dag kortere ventetid enn perioden`() {
            val melding =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2024, Month.JULY, 1),
                    tom = LocalDate.of(2024, Month.JULY, 6),
                ).also { it.prosesser() }

            verifiserAtBiterErLagret(1)

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                erUtenforVentetidRequest = ErUtenforVentetidRequest(),
            ).`should be false`()

            hentVentetid(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
            ).ventetid.also {
                it!!.fom `should be equal to` LocalDate.of(2024, Month.JULY, 1)
                it.tom `should be equal to` LocalDate.of(2024, Month.JULY, 5)
            }
        }

        @Test
        fun `Periode på 6 dager og 2 egenmeldingsdager returnerer ventetid lik perioden med egenmeldingsdager`() {
            val melding =
                lagBekreftetSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2024, Month.JULY, 1),
                    tom = LocalDate.of(2024, Month.JULY, 6),
                    sporsmals =
                        listOf(
                            SporsmalOgSvarDTO(
                                tekst = "Hvilke dager var du borte fra jobb før datoen sykmeldingen gjelder fra?",
                                shortName = ShortNameDTO.PERIODE,
                                svar =
                                    """
                                    [{
                                     "fom":"${LocalDate.of(2024, Month.JUNE, 29)}",
                                     "tom":"${LocalDate.of(2024, Month.JUNE, 30)}"
                                    }] 
                                    """.trimIndent(),
                                svartype = SvartypeDTO.PERIODER,
                            ),
                        ),
                )

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                erUtenforVentetidRequest = ErUtenforVentetidRequest(sykmeldingKafkaMessage = melding),
            ).`should be false`()

            hentVentetid(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                ventetidRequest =
                    VentetidRequest(
                        sykmeldingKafkaMessage = melding,
                        returnerPerioderInnenforVentetid = true,
                    ),
            ).ventetid.also {
                it!!.fom `should be equal to` LocalDate.of(2024, Month.JUNE, 29)
                // Perioden slutter på en lørdag, som blir fjernet fra perioden.
                it.tom `should be equal to` LocalDate.of(2024, Month.JULY, 5)
            }
        }

        @Test
        fun `Periode normalt utenfor ventetiden påvirkes ikke av 'returnerPerioderInnenforVentetid'`() {
            val melding =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2024, Month.JULY, 1),
                    tom = LocalDate.of(2024, Month.JULY, 31),
                ).also { it.prosesser() }

            verifiserAtBiterErLagret(1)

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                erUtenforVentetidRequest = ErUtenforVentetidRequest(),
            ).`should be true`()

            hentVentetid(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
            ).ventetid.also {
                it!!.fom `should be equal to` LocalDate.of(2024, Month.JULY, 1)
                it.tom `should be equal to` LocalDate.of(2024, Month.JULY, 16)
            }
        }

        @Test
        fun `Siste periode av to perioder i samme sykmelding brukes når periodene ikke kan merges`() {
            val melding =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2024, Month.JULY, 1),
                    tom = LocalDate.of(2024, Month.JULY, 9),
                )

            with(melding) {
                val p = sykmelding.sykmeldingsperioder.first()
                copy(
                    sykmelding =
                        sykmelding.copy(
                            sykmeldingsperioder =
                                listOf(
                                    p.copy(),
                                    p.copy(
                                        fom = LocalDate.of(2024, Month.JULY, 11),
                                        tom = LocalDate.of(2024, Month.JULY, 16),
                                    ),
                                ),
                        ),
                )
            }.also { it.prosesser() }

            verifiserAtBiterErLagret(2)

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                erUtenforVentetidRequest = ErUtenforVentetidRequest(),
            ).`should be false`()

            hentVentetid(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
            ).ventetid.also {
                it!!.fom `should be equal to` LocalDate.of(2024, Month.JULY, 11)
                it.tom `should be equal to` LocalDate.of(2024, Month.JULY, 16)
            }
        }

        @Test
        fun `To perioder til sammen 16 dager returnerer ventetid lik merget periode`() {
            val melding1 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2024, Month.JULY, 1),
                    tom = LocalDate.of(2024, Month.JULY, 9),
                ).also { it.prosesser() }

            val melding2 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2024, Month.JULY, 10),
                    tom = LocalDate.of(2024, Month.JULY, 16),
                ).also { it.prosesser() }

            verifiserAtBiterErLagret(2)

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding1.sykmelding.id,
                erUtenforVentetidRequest = ErUtenforVentetidRequest(),
            ).`should be false`()

            hentVentetid(
                listOf(fnr),
                sykmeldingId = melding1.sykmelding.id,
                ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
            ).ventetid.also {
                it!!.fom `should be equal to` LocalDate.of(2024, Month.JULY, 1)
                it.tom `should be equal to` LocalDate.of(2024, Month.JULY, 9)
            }

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding2.sykmelding.id,
                erUtenforVentetidRequest = ErUtenforVentetidRequest(),
            ).`should be false`()

            hentVentetid(
                listOf(fnr),
                sykmeldingId = melding2.sykmelding.id,
                ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
            ).ventetid.also {
                it!!.fom `should be equal to` LocalDate.of(2024, Month.JULY, 1)
                it.tom `should be equal to` LocalDate.of(2024, Month.JULY, 16)
            }
        }
    }

    @Nested
    inner class RedusertVenteperiode {
        @Test
        fun `Redusert Venteperiode langt tilbake i tid påvirker ikke beregning av ventetid`() {
            val sykmeldingId = UUID.randomUUID().toString()

            listOf(
                lagSyketilfelleBit(
                    fnr = fnr,
                    ressursId = UUID.randomUUID().toString(),
                    fom = LocalDate.of(2022, Month.JANUARY, 2),
                    tom = LocalDate.of(2022, Month.JANUARY, 30),
                    tags =
                        listOf(
                            Tag.SYKMELDING,
                            Tag.NY,
                            Tag.PERIODE,
                            Tag.INGEN_AKTIVITET,
                            Tag.REDUSERT_ARBEIDSGIVERPERIODE,
                        ),
                ),
                lagSyketilfelleBit(
                    fnr = fnr,
                    ressursId = sykmeldingId,
                    fom = LocalDate.of(2025, Month.SEPTEMBER, 10),
                    tom = LocalDate.of(2025, Month.OCTOBER, 13),
                    tags = listOf(Tag.SYKMELDING, Tag.NY, Tag.PERIODE, Tag.INGEN_AKTIVITET),
                ),
            ).also { syketilfellebitRepository.saveAll(it) }

            ventetidUtregner
                .beregnVentetid(
                    sykmeldingId = sykmeldingId,
                    listOf(fnr),
                    ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
                ).also {
                    it!!.fom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 10)
                    it.tom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 25)
                }
        }
    }

    @Nested
    inner class PerioderMedSammeVentetid {
        @Test
        fun `Sykmelding som ikke er SENDT eller BEKREFTET tas med i beregningen`() {
            val sykmelding1 = UUID.randomUUID().toString()
            val sykmelding2 = UUID.randomUUID().toString()

            listOf(
                lagSyketilfelleBit(
                    fnr = fnr,
                    ressursId = sykmelding1,
                    fom = LocalDate.of(2026, Month.FEBRUARY, 2),
                    tom = LocalDate.of(2026, Month.FEBRUARY, 10),
                    tags = listOf(Tag.SYKMELDING, Tag.NY, Tag.PERIODE, Tag.INGEN_AKTIVITET),
                ),
                lagSyketilfelleBit(
                    fnr = fnr,
                    ressursId = sykmelding2,
                    fom = LocalDate.of(2026, Month.FEBRUARY, 11),
                    tom = LocalDate.of(2026, Month.FEBRUARY, 22),
                    tags = listOf(Tag.SYKMELDING, Tag.NY, Tag.PERIODE, Tag.INGEN_AKTIVITET),
                ),
                lagSyketilfelleBit(
                    fnr = fnr,
                    ressursId = sykmelding1,
                    fom = LocalDate.of(2026, Month.FEBRUARY, 2),
                    tom = LocalDate.of(2026, Month.FEBRUARY, 10),
                    tags = listOf(Tag.SYKMELDING, Tag.BEKREFTET, Tag.PERIODE, Tag.INGEN_AKTIVITET),
                ),
            ).also { syketilfellebitRepository.saveAll(it) }

            val ventetidperioder: List<SammeVentetidPeriode> =
                ventetidUtregner.finnPerioderMedSammeVentetid(
                    sykmelding2,
                    listOf(fnr),
                    SammeVentetidRequest(),
                )

            ventetidperioder.shouldHaveSize(2).also { venteperiode ->
                venteperiode.map { it.ressursId }.containsAll(listOf(sykmelding1, sykmelding2)) `should be` true
            }

            ventetidperioder.first { it.ressursId == sykmelding1 }.also {
                it.ventetid.fom `should be equal to` LocalDate.of(2026, Month.FEBRUARY, 2)
                it.ventetid.tom `should be equal to` LocalDate.of(2026, Month.FEBRUARY, 10)
            }

            ventetidperioder.first { it.ressursId == sykmelding2 }.also {
                it.ventetid.fom `should be equal to` LocalDate.of(2026, Month.FEBRUARY, 2)
                it.ventetid.tom `should be equal to` LocalDate.of(2026, Month.FEBRUARY, 17)
            }
        }

        @Test
        fun `Egenmeldingsdager tas med i beregningen`() {
            val sykmelding1 = UUID.randomUUID().toString()
            val sykmelding2 = UUID.randomUUID().toString()

            listOf(
                lagSyketilfelleBit(
                    fnr = fnr,
                    ressursId = sykmelding1,
                    fom = LocalDate.of(2026, Month.FEBRUARY, 2),
                    tom = LocalDate.of(2026, Month.FEBRUARY, 10),
                    tags = listOf(Tag.SYKMELDING, Tag.NY, Tag.PERIODE, Tag.INGEN_AKTIVITET),
                ),
                lagSyketilfelleBit(
                    fnr = fnr,
                    ressursId = sykmelding2,
                    fom = LocalDate.of(2026, Month.FEBRUARY, 11),
                    tom = LocalDate.of(2026, Month.FEBRUARY, 22),
                    tags = listOf(Tag.SYKMELDING, Tag.NY, Tag.PERIODE, Tag.INGEN_AKTIVITET),
                ),
                lagSyketilfelleBit(
                    fnr = fnr,
                    ressursId = sykmelding1,
                    fom = LocalDate.of(2026, Month.FEBRUARY, 2),
                    tom = LocalDate.of(2026, Month.FEBRUARY, 10),
                    tags = listOf(Tag.SYKMELDING, Tag.BEKREFTET, Tag.PERIODE, Tag.INGEN_AKTIVITET),
                ),
                lagSyketilfelleBit(
                    fnr = fnr,
                    ressursId = sykmelding1,
                    fom = LocalDate.of(2026, Month.JANUARY, 29),
                    tom = LocalDate.of(2026, Month.FEBRUARY, 1),
                    tags = listOf(Tag.SYKMELDING, Tag.BEKREFTET, Tag.ANNET_FRAVAR),
                ),
                lagSyketilfelleBit(
                    fnr = fnr,
                    ressursId = sykmelding2,
                    fom = LocalDate.of(2026, Month.FEBRUARY, 11),
                    tom = LocalDate.of(2026, Month.FEBRUARY, 22),
                    tags = listOf(Tag.SYKMELDING, Tag.BEKREFTET, Tag.PERIODE, Tag.INGEN_AKTIVITET),
                ),
            ).also { syketilfellebitRepository.saveAll(it) }

            val ventetidperioder: List<SammeVentetidPeriode> =
                ventetidUtregner.finnPerioderMedSammeVentetid(
                    sykmelding2,
                    listOf(fnr),
                    SammeVentetidRequest(),
                )

            ventetidperioder.shouldHaveSize(2).also { venteperiode ->
                venteperiode.map { it.ressursId }.containsAll(listOf(sykmelding1, sykmelding2)) `should be` true
            }

            ventetidperioder.first { it.ressursId == sykmelding1 }.also {
                it.ventetid.fom `should be equal to` LocalDate.of(2026, Month.JANUARY, 29)
                it.ventetid.tom `should be equal to` LocalDate.of(2026, Month.FEBRUARY, 10)
            }

            ventetidperioder.first { it.ressursId == sykmelding2 }.also {
                it.ventetid.fom `should be equal to` LocalDate.of(2026, Month.JANUARY, 29)
                it.ventetid.tom `should be equal to` LocalDate.of(2026, Month.FEBRUARY, 13)
            }
        }

        @Test
        fun `Sykmelding tilhørende en annen ident tas ikke med i beregningen`() {
            val sykmelding1 = UUID.randomUUID().toString()
            val sykmelding2 = UUID.randomUUID().toString()
            val sykmeldingAnnenPerson = UUID.randomUUID().toString()

            listOf(
                lagSyketilfelleBit(
                    fnr = fnr,
                    ressursId = sykmelding1,
                    fom = LocalDate.of(2026, Month.FEBRUARY, 2),
                    tom = LocalDate.of(2026, Month.FEBRUARY, 10),
                    tags = listOf(Tag.SYKMELDING, Tag.BEKREFTET, Tag.PERIODE, Tag.INGEN_AKTIVITET),
                ),
                lagSyketilfelleBit(
                    fnr = fnr,
                    ressursId = sykmelding2,
                    fom = LocalDate.of(2026, Month.FEBRUARY, 11),
                    tom = LocalDate.of(2026, Month.FEBRUARY, 22),
                    tags = listOf(Tag.SYKMELDING, Tag.BEKREFTET, Tag.PERIODE, Tag.INGEN_AKTIVITET),
                ),
                lagSyketilfelleBit(
                    fnr = "22222222222",
                    ressursId = sykmeldingAnnenPerson,
                    fom = LocalDate.of(2026, Month.JANUARY, 20),
                    tom = LocalDate.of(2026, Month.FEBRUARY, 1),
                    tags = listOf(Tag.SYKMELDING, Tag.BEKREFTET, Tag.PERIODE, Tag.INGEN_AKTIVITET),
                ),
            ).also { syketilfellebitRepository.saveAll(it) }

            val ventetidperioder =
                ventetidUtregner.finnPerioderMedSammeVentetid(
                    sykmelding2,
                    listOf(fnr),
                    SammeVentetidRequest(),
                )
            ventetidperioder.shouldHaveSize(2).also { venteperiode ->
                venteperiode.map { it.ressursId }.containsAll(listOf(sykmelding1, sykmelding2)) `should be` true
                ventetidperioder.none { it.ressursId == sykmeldingAnnenPerson } `should be` true
            }

            ventetidperioder.first { it.ressursId == sykmelding1 }.also {
                it.ventetid.fom `should be equal to` LocalDate.of(2026, Month.FEBRUARY, 2)
                it.ventetid.tom `should be equal to` LocalDate.of(2026, Month.FEBRUARY, 10)
            }

            ventetidperioder.first { it.ressursId == sykmelding2 }.also {
                it.ventetid.fom `should be equal to` LocalDate.of(2026, Month.FEBRUARY, 2)
                it.ventetid.tom `should be equal to` LocalDate.of(2026, Month.FEBRUARY, 17)
            }
        }

        @Test
        fun `Sykmelding tilhørende person med to identer tas med i beregningen`() {
            val sykmelding1 = UUID.randomUUID().toString()
            val sykmelding2 = UUID.randomUUID().toString()

            listOf(
                lagSyketilfelleBit(
                    fnr = fnr,
                    ressursId = sykmelding1,
                    fom = LocalDate.of(2026, Month.FEBRUARY, 2),
                    tom = LocalDate.of(2026, Month.FEBRUARY, 10),
                    tags = listOf(Tag.SYKMELDING, Tag.BEKREFTET, Tag.PERIODE, Tag.INGEN_AKTIVITET),
                ),
                lagSyketilfelleBit(
                    fnr = "22222222222",
                    ressursId = sykmelding2,
                    fom = LocalDate.of(2026, Month.FEBRUARY, 11),
                    tom = LocalDate.of(2026, Month.FEBRUARY, 20),
                    tags = listOf(Tag.SYKMELDING, Tag.BEKREFTET, Tag.PERIODE, Tag.INGEN_AKTIVITET),
                ),
            ).also { syketilfellebitRepository.saveAll(it) }

            val ventetidperioder =
                ventetidUtregner.finnPerioderMedSammeVentetid(
                    sykmelding2,
                    listOf(fnr, "22222222222"),
                    SammeVentetidRequest(),
                )
            ventetidperioder.shouldHaveSize(2).also { venteperiode ->
                venteperiode.map { it.ressursId }.containsAll(listOf(sykmelding1, sykmelding2)) `should be` true
            }

            ventetidperioder.first { it.ressursId == sykmelding1 }.also {
                it.ventetid.fom `should be equal to` LocalDate.of(2026, Month.FEBRUARY, 2)
                it.ventetid.tom `should be equal to` LocalDate.of(2026, Month.FEBRUARY, 10)
            }

            ventetidperioder.first { it.ressursId == sykmelding2 }.also {
                it.ventetid.fom `should be equal to` LocalDate.of(2026, Month.FEBRUARY, 2)
                it.ventetid.tom `should be equal to` LocalDate.of(2026, Month.FEBRUARY, 17)
            }
        }

        @Test
        fun `Sykmelding send med request tas med i beregningen`() {
            val sykmelding1 = UUID.randomUUID().toString()
            val sykmelding2 = UUID.randomUUID().toString()

            listOf(
                lagSyketilfelleBit(
                    fnr = fnr,
                    ressursId = sykmelding1,
                    fom = LocalDate.of(2026, Month.FEBRUARY, 2),
                    tom = LocalDate.of(2026, Month.FEBRUARY, 8),
                    tags = listOf(Tag.SYKMELDING, Tag.BEKREFTET, Tag.PERIODE, Tag.INGEN_AKTIVITET),
                ),
                lagSyketilfelleBit(
                    fnr = fnr,
                    ressursId = sykmelding2,
                    fom = LocalDate.of(2026, Month.FEBRUARY, 9),
                    tom = LocalDate.of(2026, Month.FEBRUARY, 15),
                    tags = listOf(Tag.SYKMELDING, Tag.BEKREFTET, Tag.PERIODE, Tag.INGEN_AKTIVITET),
                ),
            ).also { syketilfellebitRepository.saveAll(it) }

            val sykmeldingKafkaMessage =
                lagBekreftetSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2026, Month.FEBRUARY, 16),
                    tom = LocalDate.of(2026, Month.FEBRUARY, 22),
                )

            val ventetidperioder =
                ventetidUtregner.finnPerioderMedSammeVentetid(
                    sykmeldingKafkaMessage.sykmelding.id,
                    listOf(fnr),
                    SammeVentetidRequest(sykmeldingKafkaMessage = sykmeldingKafkaMessage),
                )

            ventetidperioder.shouldHaveSize(3).also { venteperiode ->
                venteperiode.map { it.ressursId }.containsAll(
                    listOf(sykmelding1, sykmelding2, sykmeldingKafkaMessage.sykmelding.id),
                ) `should be` true
            }

            ventetidperioder.first { it.ressursId == sykmelding1 }.also {
                it.ventetid.fom `should be equal to` LocalDate.of(2026, Month.FEBRUARY, 2)
                it.ventetid.tom `should be equal to` LocalDate.of(2026, Month.FEBRUARY, 6)
            }

            ventetidperioder.first { it.ressursId == sykmelding2 }.also {
                it.ventetid.fom `should be equal to` LocalDate.of(2026, Month.FEBRUARY, 2)
                it.ventetid.tom `should be equal to` LocalDate.of(2026, Month.FEBRUARY, 13)
            }

            ventetidperioder.first { it.ressursId == sykmeldingKafkaMessage.sykmelding.id }.also {
                it.ventetid.fom `should be equal to` LocalDate.of(2026, Month.FEBRUARY, 2)
                it.ventetid.tom `should be equal to` LocalDate.of(2026, Month.FEBRUARY, 17)
            }
        }

        @Test
        fun `Sykmelding både lagret og sendt med i request tas med i beregningen`() {
            val sykmelding1 = UUID.randomUUID().toString()
            val sykmelding2 = UUID.randomUUID().toString()

            val sykmeldingKafkaMessage =
                lagBekreftetSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2026, Month.FEBRUARY, 16),
                    tom = LocalDate.of(2026, Month.FEBRUARY, 22),
                )

            listOf(
                lagSyketilfelleBit(
                    fnr = fnr,
                    ressursId = sykmelding1,
                    fom = LocalDate.of(2026, Month.FEBRUARY, 2),
                    tom = LocalDate.of(2026, Month.FEBRUARY, 8),
                    tags = listOf(Tag.SYKMELDING, Tag.BEKREFTET, Tag.PERIODE, Tag.INGEN_AKTIVITET),
                ),
                lagSyketilfelleBit(
                    fnr = fnr,
                    ressursId = sykmelding2,
                    fom = LocalDate.of(2026, Month.FEBRUARY, 9),
                    tom = LocalDate.of(2026, Month.FEBRUARY, 15),
                    tags = listOf(Tag.SYKMELDING, Tag.BEKREFTET, Tag.PERIODE, Tag.INGEN_AKTIVITET),
                ),
                lagSyketilfelleBit(
                    fnr = fnr,
                    ressursId = sykmeldingKafkaMessage.sykmelding.id,
                    fom = LocalDate.of(2026, Month.FEBRUARY, 16),
                    tom = LocalDate.of(2026, Month.FEBRUARY, 22),
                    tags = listOf(Tag.SYKMELDING, Tag.BEKREFTET, Tag.PERIODE, Tag.INGEN_AKTIVITET),
                ),
            ).also { syketilfellebitRepository.saveAll(it) }

            val ventetidperioder =
                ventetidUtregner.finnPerioderMedSammeVentetid(
                    sykmeldingKafkaMessage.sykmelding.id,
                    listOf(fnr),
                    SammeVentetidRequest(sykmeldingKafkaMessage = sykmeldingKafkaMessage),
                )

            ventetidperioder.shouldHaveSize(3).also { venteperiode ->
                venteperiode.map { it.ressursId }.containsAll(
                    listOf(sykmelding1, sykmelding2, sykmeldingKafkaMessage.sykmelding.id),
                ) `should be` true
            }

            ventetidperioder.first { it.ressursId == sykmelding1 }.also {
                it.ventetid.fom `should be equal to` LocalDate.of(2026, Month.FEBRUARY, 2)
                it.ventetid.tom `should be equal to` LocalDate.of(2026, Month.FEBRUARY, 6)
            }

            ventetidperioder.first { it.ressursId == sykmelding2 }.also {
                it.ventetid.fom `should be equal to` LocalDate.of(2026, Month.FEBRUARY, 2)
                it.ventetid.tom `should be equal to` LocalDate.of(2026, Month.FEBRUARY, 13)
            }

            ventetidperioder.first { it.ressursId == sykmeldingKafkaMessage.sykmelding.id }.also {
                it.ventetid.fom `should be equal to` LocalDate.of(2026, Month.FEBRUARY, 2)
                it.ventetid.tom `should be equal to` LocalDate.of(2026, Month.FEBRUARY, 17)
            }
        }

        @Test
        fun `Sykmelding som overlapper to andre sykmeldinger tas med i beregningen`() {
            val sykmelding1 = UUID.randomUUID().toString()
            val sykmelding2 = UUID.randomUUID().toString()
            val sykmelding3 = UUID.randomUUID().toString()

            listOf(
                lagSyketilfelleBit(
                    fnr = fnr,
                    ressursId = sykmelding1,
                    fom = LocalDate.of(2026, Month.FEBRUARY, 2),
                    tom = LocalDate.of(2026, Month.FEBRUARY, 10),
                    tags = listOf(Tag.SYKMELDING, Tag.BEKREFTET, Tag.PERIODE, Tag.INGEN_AKTIVITET),
                ),
                lagSyketilfelleBit(
                    fnr = fnr,
                    ressursId = sykmelding2,
                    fom = LocalDate.of(2026, Month.FEBRUARY, 11),
                    tom = LocalDate.of(2026, Month.FEBRUARY, 22),
                    tags = listOf(Tag.SYKMELDING, Tag.BEKREFTET, Tag.PERIODE, Tag.INGEN_AKTIVITET),
                ),
                lagSyketilfelleBit(
                    fnr = fnr,
                    ressursId = sykmelding3,
                    fom = LocalDate.of(2026, Month.FEBRUARY, 5),
                    tom = LocalDate.of(2026, Month.FEBRUARY, 15),
                    tags = listOf(Tag.SYKMELDING, Tag.BEKREFTET, Tag.PERIODE, Tag.INGEN_AKTIVITET),
                ),
            ).also { syketilfellebitRepository.saveAll(it) }

            val ventetidperioder: List<SammeVentetidPeriode> =
                ventetidUtregner.finnPerioderMedSammeVentetid(
                    sykmelding3,
                    listOf(fnr),
                    SammeVentetidRequest(),
                )

            ventetidperioder.shouldHaveSize(3).also { venteperiode ->
                venteperiode.map { it.ressursId }.containsAll(
                    listOf(sykmelding1, sykmelding2, sykmelding3),
                ) `should be` true
            }

            ventetidperioder.first { it.ressursId == sykmelding1 }.also {
                it.ventetid.fom `should be equal to` LocalDate.of(2026, Month.FEBRUARY, 2)
                it.ventetid.tom `should be equal to` LocalDate.of(2026, Month.FEBRUARY, 10)
            }

            ventetidperioder.first { it.ressursId == sykmelding2 }.also {
                it.ventetid.fom `should be equal to` LocalDate.of(2026, Month.FEBRUARY, 2)
                it.ventetid.tom `should be equal to` LocalDate.of(2026, Month.FEBRUARY, 17)
            }

            ventetidperioder.first { it.ressursId == sykmelding3 }.also {
                it.ventetid.fom `should be equal to` LocalDate.of(2026, Month.FEBRUARY, 2)
                // 14. og 15. februar er lørdag og søndag og tas derfor ikke med.
                it.ventetid.tom `should be equal to` LocalDate.of(2026, Month.FEBRUARY, 13)
            }
        }

        @Test
        fun `Tilbakedatert sykmelding mellom tidligere og senere sykmelding tas med i beregningen`() {
            val sykmelding1 = UUID.randomUUID().toString()
            val sykmelding2 = UUID.randomUUID().toString()
            val sykmelding3 = UUID.randomUUID().toString()

            listOf(
                lagSyketilfelleBit(
                    fnr = fnr,
                    ressursId = sykmelding1,
                    fom = LocalDate.of(2026, Month.FEBRUARY, 2),
                    tom = LocalDate.of(2026, Month.FEBRUARY, 10),
                    tags = listOf(Tag.SYKMELDING, Tag.BEKREFTET, Tag.PERIODE, Tag.INGEN_AKTIVITET),
                ),
                lagSyketilfelleBit(
                    fnr = fnr,
                    ressursId = sykmelding2,
                    fom = LocalDate.of(2026, Month.FEBRUARY, 28),
                    tom = LocalDate.of(2026, Month.MARCH, 4),
                    tags = listOf(Tag.SYKMELDING, Tag.BEKREFTET, Tag.PERIODE, Tag.INGEN_AKTIVITET),
                ),
                lagSyketilfelleBit(
                    fnr = fnr,
                    ressursId = sykmelding3,
                    fom = LocalDate.of(2026, Month.FEBRUARY, 11),
                    tom = LocalDate.of(2026, Month.FEBRUARY, 27),
                    tags = listOf(Tag.SYKMELDING, Tag.BEKREFTET, Tag.PERIODE, Tag.INGEN_AKTIVITET),
                ),
            ).also { syketilfellebitRepository.saveAll(it) }

            val ventetidperioder: List<SammeVentetidPeriode> =
                ventetidUtregner.finnPerioderMedSammeVentetid(
                    sykmelding2,
                    listOf(fnr),
                    SammeVentetidRequest(),
                )

            ventetidperioder.shouldHaveSize(3).also { venteperiode ->
                venteperiode.map { it.ressursId }.containsAll(
                    listOf(sykmelding1, sykmelding2, sykmelding3),
                ) `should be` true
            }

            ventetidperioder.first { it.ressursId == sykmelding1 }.also {
                it.ventetid.fom `should be equal to` LocalDate.of(2026, Month.FEBRUARY, 2)
                it.ventetid.tom `should be equal to` LocalDate.of(2026, Month.FEBRUARY, 10)
            }

            ventetidperioder.first { it.ressursId == sykmelding2 }.also {
                it.ventetid.fom `should be equal to` LocalDate.of(2026, Month.FEBRUARY, 2)
                it.ventetid.tom `should be equal to` LocalDate.of(2026, Month.FEBRUARY, 17)
            }

            ventetidperioder.first { it.ressursId == sykmelding3 }.also {
                it.ventetid.fom `should be equal to` LocalDate.of(2026, Month.FEBRUARY, 2)
                // 14. og 15. februar er lørdag og søndag og tas derfor ikke med.
                it.ventetid.tom `should be equal to` LocalDate.of(2026, Month.FEBRUARY, 17)
            }
        }

        @Test
        fun `Tilbakedatert sykmelding før senere periode tas med i beregningen`() {
            val sykmelding1 = UUID.randomUUID().toString()
            val sykmelding2 = UUID.randomUUID().toString()
            val sykmelding3 = UUID.randomUUID().toString()

            listOf(
                lagSyketilfelleBit(
                    fnr = fnr,
                    ressursId = sykmelding1,
                    fom = LocalDate.of(2026, Month.FEBRUARY, 2),
                    tom = LocalDate.of(2026, Month.FEBRUARY, 10),
                    tags = listOf(Tag.SYKMELDING, Tag.BEKREFTET, Tag.PERIODE, Tag.INGEN_AKTIVITET),
                ),
                lagSyketilfelleBit(
                    fnr = fnr,
                    ressursId = sykmelding3,
                    fom = LocalDate.of(2026, Month.FEBRUARY, 11),
                    tom = LocalDate.of(2026, Month.FEBRUARY, 17),
                    tags = listOf(Tag.SYKMELDING, Tag.BEKREFTET, Tag.PERIODE, Tag.INGEN_AKTIVITET),
                ),
                lagSyketilfelleBit(
                    fnr = fnr,
                    ressursId = sykmelding2,
                    fom = LocalDate.of(2026, Month.JANUARY, 12),
                    tom = LocalDate.of(2026, Month.FEBRUARY, 10),
                    tags = listOf(Tag.SYKMELDING, Tag.BEKREFTET, Tag.PERIODE, Tag.INGEN_AKTIVITET),
                ),
            ).also { syketilfellebitRepository.saveAll(it) }

            val ventetidperioder: List<SammeVentetidPeriode> =
                ventetidUtregner.finnPerioderMedSammeVentetid(
                    sykmelding3,
                    listOf(fnr),
                    SammeVentetidRequest(),
                )

            ventetidperioder.shouldHaveSize(3).also { venteperiode ->
                venteperiode.map { it.ressursId }.containsAll(
                    listOf(sykmelding1, sykmelding2, sykmelding3),
                ) `should be` true
            }

            ventetidperioder.first { it.ressursId == sykmelding1 }.also {
                it.ventetid.fom `should be equal to` LocalDate.of(2026, Month.JANUARY, 12)
                it.ventetid.tom `should be equal to` LocalDate.of(2026, Month.JANUARY, 27)
            }

            ventetidperioder.first { it.ressursId == sykmelding2 }.also {
                it.ventetid.fom `should be equal to` LocalDate.of(2026, Month.JANUARY, 12)
                it.ventetid.tom `should be equal to` LocalDate.of(2026, Month.JANUARY, 27)
            }

            ventetidperioder.first { it.ressursId == sykmelding3 }.also {
                it.ventetid.fom `should be equal to` LocalDate.of(2026, Month.JANUARY, 12)
                // 14. og 15. februar er lørdag og søndag og tas derfor ikke med.
                it.ventetid.tom `should be equal to` LocalDate.of(2026, Month.JANUARY, 27)
            }
        }

        @Test
        fun `Sykmelding med tag REISETILSKUDD tas med i beregningen`() {
            val sykmelding1 = UUID.randomUUID().toString()
            val sykmelding2 = UUID.randomUUID().toString()

            listOf(
                lagSyketilfelleBit(
                    fnr = fnr,
                    ressursId = sykmelding1,
                    fom = LocalDate.of(2026, Month.FEBRUARY, 2),
                    tom = LocalDate.of(2026, Month.FEBRUARY, 10),
                    tags = listOf(Tag.SYKMELDING, Tag.NY, Tag.PERIODE, Tag.REISETILSKUDD, Tag.UKJENT_AKTIVITET),
                ),
                lagSyketilfelleBit(
                    fnr = fnr,
                    ressursId = sykmelding2,
                    fom = LocalDate.of(2026, Month.FEBRUARY, 11),
                    tom = LocalDate.of(2026, Month.FEBRUARY, 22),
                    tags = listOf(Tag.SYKMELDING, Tag.NY, Tag.PERIODE, Tag.INGEN_AKTIVITET),
                ),
            ).also { syketilfellebitRepository.saveAll(it) }

            val ventetidperioder: List<SammeVentetidPeriode> =
                ventetidUtregner.finnPerioderMedSammeVentetid(
                    sykmelding2,
                    listOf(fnr),
                    SammeVentetidRequest(),
                )

            ventetidperioder.shouldHaveSize(2).also { venteperiode ->
                venteperiode.map { it.ressursId }.containsAll(listOf(sykmelding1, sykmelding2)) `should be` true
            }

            ventetidperioder.first { it.ressursId == sykmelding1 }.also {
                it.ventetid.fom `should be equal to` LocalDate.of(2026, Month.FEBRUARY, 2)
                it.ventetid.tom `should be equal to` LocalDate.of(2026, Month.FEBRUARY, 10)
            }

            ventetidperioder.first { it.ressursId == sykmelding2 }.also {
                it.ventetid.fom `should be equal to` LocalDate.of(2026, Month.FEBRUARY, 2)
                it.ventetid.tom `should be equal to` LocalDate.of(2026, Month.FEBRUARY, 17)
            }
        }

        @Test
        fun `Sykmelding med tag AVVENTENDE tas ikke med i beregningen`() {
            val sykmelding1 = UUID.randomUUID().toString()
            val sykmelding2 = UUID.randomUUID().toString()

            listOf(
                lagSyketilfelleBit(
                    fnr = fnr,
                    ressursId = sykmelding1,
                    fom = LocalDate.of(2026, Month.FEBRUARY, 2),
                    tom = LocalDate.of(2026, Month.FEBRUARY, 10),
                    tags = listOf(Tag.SYKMELDING, Tag.NY, Tag.PERIODE, Tag.AVVENTENDE),
                ),
                lagSyketilfelleBit(
                    fnr = fnr,
                    ressursId = sykmelding2,
                    fom = LocalDate.of(2026, Month.FEBRUARY, 11),
                    tom = LocalDate.of(2026, Month.FEBRUARY, 22),
                    tags = listOf(Tag.SYKMELDING, Tag.NY, Tag.PERIODE, Tag.INGEN_AKTIVITET),
                ),
            ).also { syketilfellebitRepository.saveAll(it) }

            val ventetidperioder: List<SammeVentetidPeriode> =
                ventetidUtregner.finnPerioderMedSammeVentetid(
                    sykmelding2,
                    listOf(fnr),
                    SammeVentetidRequest(),
                )

            ventetidperioder.shouldHaveSize(1).also { venteperiode ->
                venteperiode.map { it.ressursId }.containsAll(listOf(sykmelding2)) `should be` true
            }

            ventetidperioder.single { it.ressursId == sykmelding2 }.also {
                it.ventetid.fom `should be equal to` LocalDate.of(2026, Month.FEBRUARY, 11)
                it.ventetid.tom `should be equal to` LocalDate.of(2026, Month.FEBRUARY, 20)
            }
        }

        @Test
        fun `Sykmelding med tag AVVENTENDE returnerer tom liste`() {
            val sykmelding1 = UUID.randomUUID().toString()
            val sykmelding2 = UUID.randomUUID().toString()

            listOf(
                lagSyketilfelleBit(
                    fnr = fnr,
                    ressursId = sykmelding1,
                    fom = LocalDate.of(2026, Month.FEBRUARY, 2),
                    tom = LocalDate.of(2026, Month.FEBRUARY, 10),
                    tags = listOf(Tag.SYKMELDING, Tag.NY, Tag.PERIODE, Tag.INGEN_AKTIVITET),
                ),
                lagSyketilfelleBit(
                    fnr = fnr,
                    ressursId = sykmelding2,
                    fom = LocalDate.of(2026, Month.FEBRUARY, 11),
                    tom = LocalDate.of(2026, Month.FEBRUARY, 22),
                    tags = listOf(Tag.SYKMELDING, Tag.NY, Tag.PERIODE, Tag.AVVENTENDE),
                ),
            ).also { syketilfellebitRepository.saveAll(it) }

            ventetidUtregner
                .finnPerioderMedSammeVentetid(
                    sykmelding2,
                    listOf(fnr),
                    SammeVentetidRequest(),
                ).isEmpty() `should be` true
        }
    }

    private fun erUtenforVentetid(
        identer: List<String>,
        sykmeldingId: String,
        erUtenforVentetidRequest: ErUtenforVentetidRequest,
    ): Boolean =
        ventetidUtregner.erUtenforVentetid(
            sykmeldingId = sykmeldingId,
            identer = identer,
            erUtenforVentetidRequest = erUtenforVentetidRequest,
        )

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
