package no.nav.helse.flex.syketilfelle.ventetid

import no.nav.helse.flex.syketilfelle.FellesTestOppsett
import no.nav.helse.flex.syketilfelle.erUtenforVentetid
import no.nav.helse.flex.syketilfelle.hentVenteperiode
import no.nav.helse.flex.syketilfelle.sykmelding.SykmeldingLagring
import no.nav.syfo.model.sykmelding.arbeidsgiver.SykmeldingsperiodeAGDTO
import no.nav.syfo.model.sykmelding.model.PeriodetypeDTO
import no.nav.syfo.model.sykmeldingstatus.ShortNameDTO
import no.nav.syfo.model.sykmeldingstatus.SporsmalOgSvarDTO
import no.nav.syfo.model.sykmeldingstatus.SvartypeDTO
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should be false`
import org.amshove.kluent.`should be true`
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.Month

class VenteperiodeRefakturertTest :
    FellesTestOppsett(),
    VentetidFellesOppsett {
    @Autowired
    override lateinit var sykmeldingLagring: SykmeldingLagring

    @BeforeEach
    @AfterEach
    fun setUp() {
        syketilfellebitRepository.deleteAll()
    }

    final override val fnr = "11111111111"

    /**
     * 1. juli 2024 er en mandag.
     *
     * Venteperioden: 16 dager.
     *
     * Alle kalenderdager telles i venteperioden på 16 dager. Også helgedager. Samtidig betaler
     * ikke Nav for helgedager. Derfor telles lørdag og søndag hvis venteperioden starter i en helg, men
     * ikke hvis den avsluttes i en helg.
     */
    @Nested
    inner class Grunnregler {
        @Test
        fun `Periode på 16 dager er innenfor ventetiden`() {
            val melding =
                skapSykmeldingKafkaMessage(
                    fom = LocalDate.of(2024, Month.JULY, 1),
                    tom = LocalDate.of(2024, Month.JULY, 16),
                )

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                ventetidRequest = VentetidRequest(sykmeldingKafkaMessage = melding),
            ).`should be false`()

            hentVenteperiode(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                venteperiodeRequest = VenteperiodeRequest(sykmeldingKafkaMessage = melding),
            ).venteperiode `should be equal to` null
        }

        @Test
        fun `Periode på 17 dager er utenfor ventetiden`() {
            val melding =
                skapSykmeldingKafkaMessage(
                    fom = LocalDate.of(2024, Month.JULY, 1),
                    tom = LocalDate.of(2024, Month.JULY, 17),
                )

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                ventetidRequest = VentetidRequest(sykmeldingKafkaMessage = melding),
            ).`should be true`()

            hentVenteperiode(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                venteperiodeRequest = VenteperiodeRequest(sykmeldingKafkaMessage = melding),
            ).venteperiode.also {
                it!!.fom `should be equal to` LocalDate.of(2024, Month.JULY, 1)
                it.tom `should be equal to` LocalDate.of(2024, Month.JULY, 16)
            }
        }

        @Test
        fun `Periode på 17 dager som slutter på lørdag er innenfor ventetiden`() {
            val melding =
                skapSykmeldingKafkaMessage(
                    fom = LocalDate.of(2024, Month.JULY, 4),
                    tom = LocalDate.of(2024, Month.JULY, 20),
                )

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                ventetidRequest = VentetidRequest(sykmeldingKafkaMessage = melding),
            ).`should be false`()

            hentVenteperiode(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                venteperiodeRequest = VenteperiodeRequest(sykmeldingKafkaMessage = melding),
            ).venteperiode `should be equal to` null
        }

        @Test
        fun `Periode på 17 dager som slutter på søndag er innenfor ventetiden`() {
            val melding =
                skapSykmeldingKafkaMessage(
                    fom = LocalDate.of(2024, Month.JULY, 5),
                    tom = LocalDate.of(2024, Month.JULY, 21),
                )

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                ventetidRequest = VentetidRequest(sykmeldingKafkaMessage = melding),
            ).`should be false`()

            hentVenteperiode(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                venteperiodeRequest = VenteperiodeRequest(sykmeldingKafkaMessage = melding),
            ).venteperiode `should be equal to` null
        }

        @Test
        fun `Periode på 17 dager som starter på lørdag er utenfor ventetid`() {
            val melding =
                skapSykmeldingKafkaMessage(
                    fom = LocalDate.of(2024, Month.JUNE, 29),
                    tom = LocalDate.of(2024, Month.JULY, 15),
                )

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                ventetidRequest = VentetidRequest(sykmeldingKafkaMessage = melding),
            ).`should be true`()

            hentVenteperiode(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                venteperiodeRequest = VenteperiodeRequest(sykmeldingKafkaMessage = melding),
            ).venteperiode.also {
                it!!.fom `should be equal to` LocalDate.of(2024, Month.JUNE, 29)
                it.tom `should be equal to` LocalDate.of(2024, Month.JULY, 14)
            }
        }

        @Test
        fun `Periode på 17 dager som starter på søndag er utenfor ventetid`() {
            val melding =
                skapSykmeldingKafkaMessage(
                    fom = LocalDate.of(2024, Month.JUNE, 30),
                    tom = LocalDate.of(2024, Month.JULY, 16),
                )

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                ventetidRequest = VentetidRequest(sykmeldingKafkaMessage = melding),
            ).`should be true`()

            hentVenteperiode(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                venteperiodeRequest = VenteperiodeRequest(sykmeldingKafkaMessage = melding),
            ).venteperiode.also {
                it!!.fom `should be equal to` LocalDate.of(2024, Month.JUNE, 30)
                it.tom `should be equal to` LocalDate.of(2024, Month.JULY, 15)
            }
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
                skapApenSykmeldingKafkaMessage(
                    fom = LocalDate.of(2024, Month.JULY, 1),
                    tom = LocalDate.of(2024, Month.JULY, 8),
                ).also { it.publiser() }

            val melding2 =
                skapApenSykmeldingKafkaMessage(
                    fom = LocalDate.of(2024, Month.JULY, 9),
                    tom = LocalDate.of(2024, Month.JULY, 16),
                ).also { it.publiser() }

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding1.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).`should be false`()

            hentVenteperiode(
                listOf(fnr),
                sykmeldingId = melding1.sykmelding.id,
                venteperiodeRequest = VenteperiodeRequest(),
            ).venteperiode `should be equal to` null

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding2.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).`should be false`()

            hentVenteperiode(
                listOf(fnr),
                sykmeldingId = melding2.sykmelding.id,
                venteperiodeRequest = VenteperiodeRequest(),
            ).venteperiode `should be equal to` null
        }

        @Test
        fun `To perioder til 17 dager er utenfor ventetiden`() {
            val melding1 =
                skapApenSykmeldingKafkaMessage(
                    fom = LocalDate.of(2024, Month.JULY, 1),
                    tom = LocalDate.of(2024, Month.JULY, 8),
                ).also { it.publiser() }

            val melding2 =
                skapApenSykmeldingKafkaMessage(
                    fom = LocalDate.of(2024, Month.JULY, 9),
                    tom = LocalDate.of(2024, Month.JULY, 17),
                ).also { it.publiser() }

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding1.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).`should be false`()

            hentVenteperiode(
                listOf(fnr),
                sykmeldingId = melding1.sykmelding.id,
                venteperiodeRequest = VenteperiodeRequest(),
            ).venteperiode `should be equal to` null

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding2.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).`should be true`()

            hentVenteperiode(
                listOf(fnr),
                sykmeldingId = melding2.sykmelding.id,
                venteperiodeRequest = VenteperiodeRequest(),
            ).venteperiode.also {
                it!!.fom `should be equal to` LocalDate.of(2024, Month.JULY, 1)
                it.tom `should be equal to` LocalDate.of(2024, Month.JULY, 16)
            }
        }

        @Test
        fun `To perioder til sammen over 16 dager med lørdag mellom er utenfor ventetiden`() {
            val melding1 =
                skapApenSykmeldingKafkaMessage(
                    fom = LocalDate.of(2024, Month.JULY, 1),
                    tom = LocalDate.of(2024, Month.JULY, 12),
                ).also { it.publiser() }

            val melding2 =
                skapApenSykmeldingKafkaMessage(
                    fom = LocalDate.of(2024, Month.JULY, 14),
                    tom = LocalDate.of(2024, Month.JULY, 21),
                ).also { it.publiser() }

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding1.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).`should be false`()

            hentVenteperiode(
                listOf(fnr),
                sykmeldingId = melding1.sykmelding.id,
                venteperiodeRequest = VenteperiodeRequest(),
            ).venteperiode `should be equal to` null

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding2.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).`should be true`()

            hentVenteperiode(
                listOf(fnr),
                sykmeldingId = melding2.sykmelding.id,
                venteperiodeRequest = VenteperiodeRequest(),
            ).venteperiode.also {
                it!!.fom `should be equal to` LocalDate.of(2024, Month.JULY, 1)
                it.tom `should be equal to` LocalDate.of(2024, Month.JULY, 16)
            }
        }

        @Test
        fun `To perioder til sammen over 16 dager med søndag mellom er utenfor ventetiden`() {
            val melding1 =
                skapApenSykmeldingKafkaMessage(
                    fom = LocalDate.of(2024, Month.JULY, 1),
                    tom = LocalDate.of(2024, Month.JULY, 13),
                ).also { it.publiser() }

            val melding2 =
                skapApenSykmeldingKafkaMessage(
                    fom = LocalDate.of(2024, Month.JULY, 15),
                    tom = LocalDate.of(2024, Month.JULY, 21),
                ).also { it.publiser() }

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding1.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).`should be false`()

            hentVenteperiode(
                listOf(fnr),
                sykmeldingId = melding1.sykmelding.id,
                venteperiodeRequest = VenteperiodeRequest(),
            ).venteperiode `should be equal to` null

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding2.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).`should be true`()

            hentVenteperiode(
                listOf(fnr),
                sykmeldingId = melding2.sykmelding.id,
                venteperiodeRequest = VenteperiodeRequest(),
            ).venteperiode.also {
                it!!.fom `should be equal to` LocalDate.of(2024, Month.JULY, 1)
                it.tom `should be equal to` LocalDate.of(2024, Month.JULY, 16)
            }
        }

        @Test
        fun `To perioder til sammen over 16 dager med lørdag og søndag mellom er utenfor ventetiden`() {
            val melding1 =
                skapApenSykmeldingKafkaMessage(
                    fom = LocalDate.of(2024, Month.JULY, 1),
                    tom = LocalDate.of(2024, Month.JULY, 12),
                ).also { it.publiser() }

            val melding2 =
                skapApenSykmeldingKafkaMessage(
                    fom = LocalDate.of(2024, Month.JULY, 15),
                    tom = LocalDate.of(2024, Month.JULY, 21),
                ).also { it.publiser() }

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding1.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).`should be false`()

            hentVenteperiode(
                listOf(fnr),
                sykmeldingId = melding1.sykmelding.id,
                venteperiodeRequest = VenteperiodeRequest(),
            ).venteperiode `should be equal to` null

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding2.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).`should be true`()

            hentVenteperiode(
                listOf(fnr),
                sykmeldingId = melding2.sykmelding.id,
                venteperiodeRequest = VenteperiodeRequest(),
            ).venteperiode.also {
                it!!.fom `should be equal to` LocalDate.of(2024, Month.JULY, 1)
                it.tom `should be equal to` LocalDate.of(2024, Month.JULY, 16)
            }
        }

        @Test
        fun `Tre perioder til sammen 16 dager er innenfor ventetiden`() {
            val melding1 =
                skapApenSykmeldingKafkaMessage(
                    fom = LocalDate.of(2024, Month.JULY, 1),
                    tom = LocalDate.of(2024, Month.JULY, 5),
                ).also { it.publiser() }

            val melding2 =
                skapApenSykmeldingKafkaMessage(
                    fom = LocalDate.of(2024, Month.JULY, 6),
                    tom = LocalDate.of(2024, Month.JULY, 10),
                ).also { it.publiser() }

            val melding3 =
                skapApenSykmeldingKafkaMessage(
                    fom = LocalDate.of(2024, Month.JULY, 11),
                    tom = LocalDate.of(2024, Month.JULY, 16),
                ).also { it.publiser() }

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding1.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).`should be false`()

            hentVenteperiode(
                listOf(fnr),
                sykmeldingId = melding1.sykmelding.id,
                venteperiodeRequest = VenteperiodeRequest(),
            ).venteperiode `should be equal to` null

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding2.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).`should be false`()

            hentVenteperiode(
                listOf(fnr),
                sykmeldingId = melding2.sykmelding.id,
                venteperiodeRequest = VenteperiodeRequest(),
            ).venteperiode `should be equal to` null

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding3.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).`should be false`()

            hentVenteperiode(
                listOf(fnr),
                sykmeldingId = melding3.sykmelding.id,
                venteperiodeRequest = VenteperiodeRequest(),
            ).venteperiode `should be equal to` null
        }

        @Test
        fun `Tre perioder til sammen 17 dager er utenfor ventetiden`() {
            val melding1 =
                skapApenSykmeldingKafkaMessage(
                    fom = LocalDate.of(2024, Month.JULY, 1),
                    tom = LocalDate.of(2024, Month.JULY, 5),
                ).also { it.publiser() }

            val melding2 =
                skapApenSykmeldingKafkaMessage(
                    fom = LocalDate.of(2024, Month.JULY, 6),
                    tom = LocalDate.of(2024, Month.JULY, 10),
                ).also { it.publiser() }

            val melding3 =
                skapApenSykmeldingKafkaMessage(
                    fom = LocalDate.of(2024, Month.JULY, 11),
                    tom = LocalDate.of(2024, Month.JULY, 17),
                ).also { it.publiser() }

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding1.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).`should be false`()

            hentVenteperiode(
                listOf(fnr),
                sykmeldingId = melding1.sykmelding.id,
                venteperiodeRequest = VenteperiodeRequest(),
            ).venteperiode `should be equal to` null

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding2.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).`should be false`()

            hentVenteperiode(
                listOf(fnr),
                sykmeldingId = melding2.sykmelding.id,
                venteperiodeRequest = VenteperiodeRequest(),
            ).venteperiode `should be equal to` null

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding3.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).`should be true`()

            hentVenteperiode(
                listOf(fnr),
                sykmeldingId = melding3.sykmelding.id,
                venteperiodeRequest = VenteperiodeRequest(),
            ).venteperiode.also {
                it!!.fom `should be equal to` LocalDate.of(2024, Month.JULY, 1)
                it.tom `should be equal to` LocalDate.of(2024, Month.JULY, 16)
            }
        }

        @Test
        fun `Tre perioder til sammen over 16 dager på grunn av helg er utenfor ventetiden`() {
            val melding1 =
                skapApenSykmeldingKafkaMessage(
                    fom = LocalDate.of(2024, Month.JULY, 1),
                    tom = LocalDate.of(2024, Month.JULY, 5),
                ).also { it.publiser() }

            val melding2 =
                skapApenSykmeldingKafkaMessage(
                    fom = LocalDate.of(2024, Month.JULY, 6),
                    tom = LocalDate.of(2024, Month.JULY, 12),
                ).also { it.publiser() }

            val melding3 =
                skapApenSykmeldingKafkaMessage(
                    fom = LocalDate.of(2024, Month.JULY, 14),
                    tom = LocalDate.of(2024, Month.JULY, 17),
                ).also { it.publiser() }

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding1.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).`should be false`()

            hentVenteperiode(
                listOf(fnr),
                sykmeldingId = melding1.sykmelding.id,
                venteperiodeRequest = VenteperiodeRequest(),
            ).venteperiode `should be equal to` null

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding2.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).`should be false`()

            hentVenteperiode(
                listOf(fnr),
                sykmeldingId = melding2.sykmelding.id,
                venteperiodeRequest = VenteperiodeRequest(),
            ).venteperiode `should be equal to` null

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding3.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).`should be true`()

            hentVenteperiode(
                listOf(fnr),
                sykmeldingId = melding3.sykmelding.id,
                venteperiodeRequest = VenteperiodeRequest(),
            ).venteperiode.also {
                it!!.fom `should be equal to` LocalDate.of(2024, Month.JULY, 1)
                it.tom `should be equal to` LocalDate.of(2024, Month.JULY, 16)
            }
        }

        @Test
        fun `To perioder til sammen 17 dager hvor siste periode bar er én dag lang er utenfor ventetden`() {
            val melding1 =
                skapApenSykmeldingKafkaMessage(
                    fom = LocalDate.of(2024, Month.JULY, 1),
                    tom = LocalDate.of(2024, Month.JULY, 16),
                ).also { it.publiser() }

            val melding2 =
                skapApenSykmeldingKafkaMessage(
                    fom = LocalDate.of(2024, Month.JULY, 17),
                    tom = LocalDate.of(2024, Month.JULY, 17),
                ).also { it.publiser() }

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding1.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).`should be false`()

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding2.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).`should be true`()

            hentVenteperiode(
                listOf(fnr),
                sykmeldingId = melding2.sykmelding.id,
                venteperiodeRequest = VenteperiodeRequest(),
            ).venteperiode.also {
                it!!.fom `should be equal to` LocalDate.of(2024, Month.JULY, 1)
                it.tom `should be equal to` LocalDate.of(2024, Month.JULY, 16)
            }
        }
    }

    /**
     * Tidligere utbetalte (perioder utenfor ventetiden) perioden teller på ventetide på neste periode hvis oppholdet
     * mellom perioden er 16 dager eller mindre.
     *
     * Kort Periode: Sykmeldingsperiode på 16 dager eller mindre.
     * Lang Periode: Sykmeldingsperiode lengre enn 16 dager.
     *
     */
    @Nested
    inner class OppholdMellomPerioder {
        @Test
        fun `Kort periode påvirker ikke kort periode selv om opphold er kortere enn 17 dager`() {
            skapApenSykmeldingKafkaMessage(
                fom = LocalDate.of(2024, Month.JULY, 1),
                tom = LocalDate.of(2024, Month.JULY, 16),
            ).also { it.publiser() }

            val melding =
                skapApenSykmeldingKafkaMessage(
                    fom = LocalDate.of(2024, Month.JULY, 18),
                    tom = LocalDate.of(2024, Month.JULY, 31),
                ).also { it.publiser() }

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).`should be false`()
        }

        @Test
        fun `Kort periode påvirker ikke lang periode selv om opphold er kortere enn 17 dager`() {
            skapApenSykmeldingKafkaMessage(
                fom = LocalDate.of(2024, Month.JULY, 1),
                tom = LocalDate.of(2024, Month.JULY, 10),
            ).also { it.publiser() }

            val melding =
                skapApenSykmeldingKafkaMessage(
                    fom = LocalDate.of(2024, Month.JULY, 12),
                    tom = LocalDate.of(2024, Month.JULY, 31),
                ).also { it.publiser() }

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).`should be true`()

            hentVenteperiode(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                venteperiodeRequest = VenteperiodeRequest(),
            ).venteperiode.also {
                it!!.fom `should be equal to` LocalDate.of(2024, Month.JULY, 12)
                it.tom `should be equal to` LocalDate.of(2024, Month.JULY, 27)
            }
        }

        @Test
        fun `Lang periode påvirker kort periode teller hvis opphold er kortere enn 17 dager`() {
            skapApenSykmeldingKafkaMessage(
                fom = LocalDate.of(2024, Month.JULY, 1),
                tom = LocalDate.of(2024, Month.JULY, 17),
            ).also { it.publiser() }

            val melding =
                skapApenSykmeldingKafkaMessage(
                    fom = LocalDate.of(2024, Month.JULY, 21),
                    tom = LocalDate.of(2024, Month.JULY, 31),
                ).also { it.publiser() }

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).`should be true`()

            // Venteperiode fra lang periode er gjeldende for kort periode når opphold er mindre enn 17 dager.
            hentVenteperiode(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                venteperiodeRequest = VenteperiodeRequest(),
            ).venteperiode.also {
                it!!.fom `should be equal to` LocalDate.of(2024, Month.JULY, 1)
                it.tom `should be equal to` LocalDate.of(2024, Month.JULY, 16)
            }
        }

        @Test
        fun `Lang periode påvirker ikke neste periode hvis opphold er lengre enn 16 dager`() {
            skapApenSykmeldingKafkaMessage(
                fom = LocalDate.of(2024, Month.JULY, 1),
                tom = LocalDate.of(2024, Month.JULY, 17),
            ).also { it.publiser() }

            val melding =
                skapApenSykmeldingKafkaMessage(
                    fom = LocalDate.of(2024, Month.AUGUST, 3),
                    tom = LocalDate.of(2024, Month.AUGUST, 16),
                ).also { it.publiser() }

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).`should be false`()
        }
    }

    /**
     * Når det finnes overlappende perioder, skal kun den delen av de tidligere periodene som er før eller samtidig
     * med den nåværende periodens tom tas med.
     */
    @Nested
    inner class OverlappendePerioder {
        @Test
        fun `Inkluderer ikke del av overlappende periode som er etter aktuell periode`() {
            skapApenSykmeldingKafkaMessage(
                fom = LocalDate.of(2024, Month.JULY, 1),
                tom = LocalDate.of(2024, Month.JULY, 31),
            ).also { it.publiser() }

            val melding =
                skapApenSykmeldingKafkaMessage(
                    fom = LocalDate.of(2024, Month.JULY, 1),
                    tom = LocalDate.of(2024, Month.JULY, 16),
                ).also { it.publiser() }

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).`should be false`()
        }

        @Test
        fun `Inkluderer del av overlappende periode som er før aktuell periode`() {
            skapApenSykmeldingKafkaMessage(
                fom = LocalDate.of(2024, Month.JUNE, 30),
                tom = LocalDate.of(2024, Month.JULY, 31),
            ).also { it.publiser() }

            val melding =
                skapApenSykmeldingKafkaMessage(
                    fom = LocalDate.of(2024, Month.JULY, 1),
                    tom = LocalDate.of(2024, Month.JULY, 16),
                ).also { it.publiser() }

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).`should be true`()

            hentVenteperiode(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                venteperiodeRequest = VenteperiodeRequest(),
            ).venteperiode.also {
                it!!.fom `should be equal to` LocalDate.of(2024, Month.JUNE, 30)
                it.tom `should be equal to` LocalDate.of(2024, Month.JULY, 15)
            }
        }

        @Test
        fun `Inkluderer ikke del av overlappende periode som er etter aktuell sykmelding med flere perioder`() {
            skapApenSykmeldingKafkaMessage(
                fom = LocalDate.of(2024, Month.JULY, 1),
                tom = LocalDate.of(2024, Month.JULY, 31),
            ).publiser()

            val sykmeldingKafkaMessage = skapApenSykmeldingKafkaMessage()

            val melding =
                sykmeldingKafkaMessage
                    .copy(
                        sykmelding =
                            sykmeldingKafkaMessage.sykmelding.copy(
                                sykmeldingsperioder =
                                    listOf(
                                        SykmeldingsperiodeAGDTO(
                                            fom = LocalDate.of(2024, Month.JULY, 1),
                                            tom = LocalDate.of(2024, Month.JULY, 8),
                                            type = PeriodetypeDTO.AKTIVITET_IKKE_MULIG,
                                            reisetilskudd = false,
                                            aktivitetIkkeMulig = null,
                                            behandlingsdager = null,
                                            gradert = null,
                                            innspillTilArbeidsgiver = null,
                                        ),
                                        SykmeldingsperiodeAGDTO(
                                            fom = LocalDate.of(2024, Month.JULY, 8),
                                            tom = LocalDate.of(2024, Month.JULY, 16),
                                            type = PeriodetypeDTO.AKTIVITET_IKKE_MULIG,
                                            reisetilskudd = false,
                                            aktivitetIkkeMulig = null,
                                            behandlingsdager = null,
                                            gradert = null,
                                            innspillTilArbeidsgiver = null,
                                        ),
                                    ),
                            ),
                    ).also { it.publiser() }

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).`should be false`()

            hentVenteperiode(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                venteperiodeRequest = VenteperiodeRequest(),
            ).venteperiode `should be equal to` null
        }
    }

    @Nested
    inner class Egenmeldingsdager {
        @Test
        fun `Sykmelding på 16 dager med én dag egenmelding foran er utenfor ventetid`() {
            val melding =
                skapSykmeldingKafkaMessage(
                    fom = LocalDate.of(2024, Month.JULY, 1),
                    tom = LocalDate.of(2024, Month.JULY, 16),
                    sporsmals =
                        listOf(
                            SporsmalOgSvarDTO(
                                tekst = "Hvilke dager var du borte fra jobb før datoen sykmeldingen gjelder fra?",
                                shortName = ShortNameDTO.PERIODE,
                                svar =
                                    """
                                    [
                                    {
                                     "fom":"${LocalDate.of(2024, Month.JUNE, 30)}",
                                     "tom":"${LocalDate.of(2024, Month.JUNE, 30)}"
                                    }
                                    ]
                                    """.trimIndent(),
                                svartype = SvartypeDTO.PERIODER,
                            ),
                        ),
                )

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                ventetidRequest = VentetidRequest(sykmeldingKafkaMessage = melding),
            ).`should be true`()

            hentVenteperiode(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                venteperiodeRequest = VenteperiodeRequest(sykmeldingKafkaMessage = melding),
            ).venteperiode.also {
                it!!.fom `should be equal to` LocalDate.of(2024, Month.JUNE, 30)
                it.tom `should be equal to` LocalDate.of(2024, Month.JULY, 15)
            }
        }

        @Test
        fun `Kort sykmelding med sammenhengende egenmeldingsperioder til sammen over 16 dager er utenfor ventetiden`() {
            val melding =
                skapSykmeldingKafkaMessage(
                    fom = LocalDate.of(2024, Month.JULY, 1),
                    tom = LocalDate.of(2024, Month.JULY, 15),
                    sporsmals =
                        listOf(
                            SporsmalOgSvarDTO(
                                tekst = "Hvilke dager var du borte fra jobb før datoen sykmeldingen gjelder fra?",
                                shortName = ShortNameDTO.PERIODE,
                                svar =
                                    """
                                    [
                                    {
                                     "fom":"${LocalDate.of(2024, Month.JUNE, 29)}",
                                     "tom":"${LocalDate.of(2024, Month.JUNE, 29)}"
                                    },
                                    {
                                     "fom":"${LocalDate.of(2024, Month.JUNE, 30)}",
                                     "tom":"${LocalDate.of(2024, Month.JUNE, 30)}"
                                    } 
                                    ]
                                    """.trimIndent(),
                                svartype = SvartypeDTO.PERIODER,
                            ),
                        ),
                )

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                ventetidRequest = VentetidRequest(sykmeldingKafkaMessage = melding),
            ).`should be true`()

            hentVenteperiode(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                venteperiodeRequest = VenteperiodeRequest(sykmeldingKafkaMessage = melding),
            ).venteperiode.also {
                it!!.fom `should be equal to` LocalDate.of(2024, Month.JUNE, 29)
                it.tom `should be equal to` LocalDate.of(2024, Month.JULY, 14)
            }
        }

        @Test
        fun `Sykmelding på 16 dager med en dag egenmelding foran er utenfor ventetid med data i tilleggsopplysninger`() {
            val melding =
                skapApenSykmeldingKafkaMessage(
                    fom = LocalDate.of(2024, Month.JULY, 1),
                    tom = LocalDate.of(2024, Month.JULY, 16),
                ).also { it.publiser() }

            val tilleggsopplysninger =
                Tilleggsopplysninger(
                    egenmeldingsperioder =
                        listOf(
                            Datospenn(
                                fom = LocalDate.of(2024, Month.JUNE, 30),
                                tom = LocalDate.of(2024, Month.JUNE, 30),
                            ),
                        ),
                )

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                ventetidRequest = VentetidRequest(tilleggsopplysninger = tilleggsopplysninger),
            ).`should be true`()

            hentVenteperiode(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                venteperiodeRequest = VenteperiodeRequest(tilleggsopplysninger = tilleggsopplysninger),
            ).venteperiode.also {
                it!!.fom `should be equal to` LocalDate.of(2024, Month.JUNE, 30)
                it.tom `should be equal to` LocalDate.of(2024, Month.JULY, 15)
            }
        }
    }

    @Nested
    inner class Behandlingsdager {
        @Test
        fun `Lang periode med behandlingsdager er innenfor ventetiden`() {
            val melding =
                skapApenSykmeldingKafkaMessage(
                    fom = LocalDate.of(2024, Month.JULY, 1),
                    tom = LocalDate.of(2024, Month.JULY, 31),
                    type = PeriodetypeDTO.BEHANDLINGSDAGER,
                ).also { it.publiser() }

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).`should be false`()

            hentVenteperiode(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                venteperiodeRequest = VenteperiodeRequest(),
            ).venteperiode `should be equal to` null
        }

        @Test
        fun `Kort periode med behandlingsdager etter sammenhengende lang periode utenfor ventetiden`() {
            val melding1 =
                skapApenSykmeldingKafkaMessage(
                    fom = LocalDate.of(2024, Month.JUNE, 1),
                    tom = LocalDate.of(2024, Month.JUNE, 30),
                ).also { it.publiser() }

            val melding2 =
                skapApenSykmeldingKafkaMessage(
                    fom = LocalDate.of(2024, Month.JULY, 2),
                    tom = LocalDate.of(2024, Month.JULY, 10),
                    type = PeriodetypeDTO.BEHANDLINGSDAGER,
                ).also { it.publiser() }

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding1.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).`should be true`()

            hentVenteperiode(
                listOf(fnr),
                sykmeldingId = melding1.sykmelding.id,
                venteperiodeRequest = VenteperiodeRequest(),
            ).venteperiode.also {
                it!!.fom `should be equal to` LocalDate.of(2024, Month.JUNE, 1)
                it.tom `should be equal to` LocalDate.of(2024, Month.JUNE, 16)
            }

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding2.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).`should be true`()

            hentVenteperiode(
                listOf(fnr),
                sykmeldingId = melding2.sykmelding.id,
                venteperiodeRequest = VenteperiodeRequest(),
            ).venteperiode.also {
                it!!.fom `should be equal to` LocalDate.of(2024, Month.JUNE, 1)
                it.tom `should be equal to` LocalDate.of(2024, Month.JUNE, 16)
            }
        }

        @Test
        fun `Lang periode med behandlingsdager etter sammenhengende lang periode utenfor ventetiden`() {
            val melding1 =
                skapApenSykmeldingKafkaMessage(
                    fom = LocalDate.of(2024, Month.JUNE, 1),
                    tom = LocalDate.of(2024, Month.JUNE, 30),
                ).also { it.publiser() }

            val melding2 =
                skapApenSykmeldingKafkaMessage(
                    fom = LocalDate.of(2024, Month.JULY, 1),
                    tom = LocalDate.of(2024, Month.JULY, 31),
                    type = PeriodetypeDTO.BEHANDLINGSDAGER,
                ).also { it.publiser() }

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding1.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).`should be true`()

            hentVenteperiode(
                listOf(fnr),
                sykmeldingId = melding1.sykmelding.id,
                venteperiodeRequest = VenteperiodeRequest(),
            ).venteperiode.also {
                it!!.fom `should be equal to` LocalDate.of(2024, Month.JUNE, 1)
                it.tom `should be equal to` LocalDate.of(2024, Month.JUNE, 16)
            }

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding2.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).`should be true`()

            hentVenteperiode(
                listOf(fnr),
                sykmeldingId = melding2.sykmelding.id,
                venteperiodeRequest = VenteperiodeRequest(),
            ).venteperiode.also {
                it!!.fom `should be equal to` LocalDate.of(2024, Month.JUNE, 1)
                it.tom `should be equal to` LocalDate.of(2024, Month.JUNE, 16)
            }
        }

        @Test
        fun `Lang periode med behandlingsdager med en dag mellomrom til forrige lange periodeer utenfor ventetiden`() {
            val melding1 =
                skapApenSykmeldingKafkaMessage(
                    fom = LocalDate.of(2024, Month.JUNE, 1),
                    tom = LocalDate.of(2024, Month.JUNE, 30),
                ).also { it.publiser() }

            val melding2 =
                skapApenSykmeldingKafkaMessage(
                    fom = LocalDate.of(2024, Month.JULY, 2),
                    tom = LocalDate.of(2024, Month.JULY, 31),
                    type = PeriodetypeDTO.BEHANDLINGSDAGER,
                ).also { it.publiser() }

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding1.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).`should be true`()

            hentVenteperiode(
                listOf(fnr),
                sykmeldingId = melding1.sykmelding.id,
                venteperiodeRequest = VenteperiodeRequest(),
            ).venteperiode.also {
                it!!.fom `should be equal to` LocalDate.of(2024, Month.JUNE, 1)
                it.tom `should be equal to` LocalDate.of(2024, Month.JUNE, 16)
            }

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding2.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).`should be true`()

            hentVenteperiode(
                listOf(fnr),
                sykmeldingId = melding2.sykmelding.id,
                venteperiodeRequest = VenteperiodeRequest(),
            ).venteperiode.also {
                it!!.fom `should be equal to` LocalDate.of(2024, Month.JUNE, 1)
                it.tom `should be equal to` LocalDate.of(2024, Month.JUNE, 16)
            }
        }
    }

    @Nested
    inner class IkkeTellendePeriodetyper {
        @Test
        fun `Lang Avventede Sykmelding påvirker ikke ventetiden`() {
            val melding1 =
                skapApenSykmeldingKafkaMessage(
                    fom = LocalDate.of(2024, Month.JULY, 1),
                    tom = LocalDate.of(2024, Month.JULY, 17),
                    type = PeriodetypeDTO.AVVENTENDE,
                ).also { it.publiser() }

            val melding2 =
                skapApenSykmeldingKafkaMessage(
                    fom = LocalDate.of(2024, Month.JULY, 18),
                    tom = LocalDate.of(2024, Month.JULY, 30),
                ).also { it.publiser() }

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding1.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).`should be false`()

            hentVenteperiode(
                listOf(fnr),
                sykmeldingId = melding1.sykmelding.id,
                venteperiodeRequest = VenteperiodeRequest(),
            ).venteperiode `should be equal to` null

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding2.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).`should be false`()

            hentVenteperiode(
                listOf(fnr),
                sykmeldingId = melding2.sykmelding.id,
                venteperiodeRequest = VenteperiodeRequest(),
            ).venteperiode `should be equal to` null
        }

        @Test
        fun `Lang sykmelding med Reisetilskudd påvirker ikke ventetiden`() {
            val melding1 =
                skapApenSykmeldingKafkaMessage(
                    fom = LocalDate.of(2024, Month.JULY, 1),
                    tom = LocalDate.of(2024, Month.JULY, 17),
                    type = PeriodetypeDTO.REISETILSKUDD,
                ).also { it.publiser() }

            val melding2 =
                skapApenSykmeldingKafkaMessage(
                    fom = LocalDate.of(2024, Month.JULY, 18),
                    tom = LocalDate.of(2024, Month.JULY, 30),
                ).also { it.publiser() }

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding1.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).`should be false`()

            hentVenteperiode(
                listOf(fnr),
                sykmeldingId = melding1.sykmelding.id,
                venteperiodeRequest = VenteperiodeRequest(),
            ).venteperiode `should be equal to` null

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding2.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).`should be false`()

            hentVenteperiode(
                listOf(fnr),
                sykmeldingId = melding2.sykmelding.id,
                venteperiodeRequest = VenteperiodeRequest(),
            ).venteperiode `should be equal to` null
        }

        @Test
        fun `Lang sykmelding med Behandlingsdager påvirker ikke ventetiden`() {
            val melding1 =
                skapApenSykmeldingKafkaMessage(
                    fom = LocalDate.of(2024, Month.JULY, 1),
                    tom = LocalDate.of(2024, Month.JULY, 17),
                    type = PeriodetypeDTO.BEHANDLINGSDAGER,
                ).also { it.publiser() }

            val melding2 =
                skapApenSykmeldingKafkaMessage(
                    fom = LocalDate.of(2024, Month.JULY, 18),
                    tom = LocalDate.of(2024, Month.JULY, 30),
                ).also { it.publiser() }

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding1.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).`should be false`()

            hentVenteperiode(
                listOf(fnr),
                sykmeldingId = melding1.sykmelding.id,
                venteperiodeRequest = VenteperiodeRequest(),
            ).venteperiode `should be equal to` null

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding2.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).`should be false`()

            hentVenteperiode(
                listOf(fnr),
                sykmeldingId = melding2.sykmelding.id,
                venteperiodeRequest = VenteperiodeRequest(),
            ).venteperiode `should be equal to` null
        }
    }

    /**
     * Når flagget 'harForsikring' er 'true' returneres det en venteperiode selv om perioden er innenfor ventetden.
     * Venteperioden vil da være lik perioden, inkludert eventuelte egenmeldingsdager.
     */
    @Nested
    inner class SykmeldtHarForsikring {
        @Test
        fun `Periode på 16 dager som har forsikring returnerer venteperiode`() {
            val melding =
                skapApenSykmeldingKafkaMessage(
                    fom = LocalDate.of(2024, Month.JULY, 1),
                    tom = LocalDate.of(2024, Month.JULY, 16),
                ).also { it.publiser() }

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).`should be false`()

            hentVenteperiode(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                venteperiodeRequest = VenteperiodeRequest(harForsikring = true),
            ).venteperiode.also {
                it!!.fom `should be equal to` LocalDate.of(2024, Month.JULY, 1)
                it.tom `should be equal to` LocalDate.of(2024, Month.JULY, 16)
            }
        }

        @Test
        fun `Periode på 1 dag som har forsikring returnerer venteperiode`() {
            val melding =
                skapApenSykmeldingKafkaMessage(
                    fom = LocalDate.of(2024, Month.JULY, 1),
                    tom = LocalDate.of(2024, Month.JULY, 1),
                ).also { it.publiser() }

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).`should be false`()

            hentVenteperiode(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                venteperiodeRequest = VenteperiodeRequest(harForsikring = true),
            ).venteperiode.also {
                it!!.fom `should be equal to` LocalDate.of(2024, Month.JULY, 1)
                it.tom `should be equal to` LocalDate.of(2024, Month.JULY, 1)
            }
        }

        @Test
        fun `Periode på 6 dager som har forsikring og 2 egenmeldingsdager returnerer venteperiode`() {
            val melding =
                skapSykmeldingKafkaMessage(
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
                ventetidRequest = VentetidRequest(sykmeldingKafkaMessage = melding),
            ).`should be false`()

            hentVenteperiode(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                venteperiodeRequest = VenteperiodeRequest(sykmeldingKafkaMessage = melding, harForsikring = true),
            ).venteperiode.also {
                it!!.fom `should be equal to` LocalDate.of(2024, Month.JUNE, 29)
                // Perioden slutter på en lørdag, som blir fjernet fra perioden.
                it.tom `should be equal to` LocalDate.of(2024, Month.JULY, 5)
            }
        }

        @Test
        fun `Periode normalt utenfor ventetiden skal ikke påvirkes av sykmeldt har forsikring`() {
            val melding =
                skapApenSykmeldingKafkaMessage(
                    fom = LocalDate.of(2024, Month.JULY, 1),
                    tom = LocalDate.of(2024, Month.JULY, 31),
                ).also { it.publiser() }

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).`should be true`()

            hentVenteperiode(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                venteperiodeRequest = VenteperiodeRequest(harForsikring = true),
            ).venteperiode.also {
                it!!.fom `should be equal to` LocalDate.of(2024, Month.JULY, 1)
                it.tom `should be equal to` LocalDate.of(2024, Month.JULY, 16)
            }
        }

        @Test
        fun `Siste periode av to perioder i samme sykmelding brukes når periodene ikke kan merges`() {
            val melding =
                skapApenSykmeldingKafkaMessage(
                    fom = LocalDate.of(2024, Month.JULY, 1),
                    tom = LocalDate.of(2024, Month.JULY, 8),
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
                                        fom = LocalDate.of(2024, Month.JULY, 10),
                                        tom = LocalDate.of(2024, Month.JULY, 16),
                                    ),
                                ),
                        ),
                )
            }.also { it.publiser() }

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                ventetidRequest = VentetidRequest(),
            ).`should be false`()

            hentVenteperiode(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                venteperiodeRequest = VenteperiodeRequest(harForsikring = true),
            ).venteperiode.also {
                it!!.fom `should be equal to` LocalDate.of(2024, Month.JULY, 10)
                it.tom `should be equal to` LocalDate.of(2024, Month.JULY, 16)
            }
        }
    }
}
