package no.nav.helse.flex.syketilfelle.ventetid

import no.nav.helse.flex.syketilfelle.FellesTestOppsett
import no.nav.helse.flex.syketilfelle.erUtenforVentetid
import no.nav.helse.flex.syketilfelle.erUtenforVentetidSomBrukerTokenX
import no.nav.helse.flex.syketilfelle.sykmelding.SykmeldingLagring
import no.nav.helse.flex.syketilfelle.tokenxToken
import no.nav.syfo.model.sykmelding.arbeidsgiver.SykmeldingsperiodeAGDTO
import no.nav.syfo.model.sykmelding.model.PeriodetypeDTO
import no.nav.syfo.model.sykmeldingstatus.ShortNameDTO
import no.nav.syfo.model.sykmeldingstatus.SporsmalOgSvarDTO
import no.nav.syfo.model.sykmeldingstatus.SvartypeDTO
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should be false`
import org.amshove.kluent.`should be true`
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.time.LocalDate
import java.time.Month

class VentetidTest :
    FellesTestOppsett(),
    VentetidFellesOppsett {
    private val mandag = LocalDate.of(2020, Month.JUNE, 1)
    private val sondag = mandag.minusDays(1)
    private val lordag = mandag.minusDays(2)
    private val fredag = mandag.minusDays(3)
    private val onsdag = mandag.plusDays(2)

    @Autowired
    override lateinit var sykmeldingLagring: SykmeldingLagring

    final override val fnr = "12345432123"

    @BeforeEach
    @AfterEach
    fun setUp() {
        syketilfellebitRepository.deleteAll()
    }

    @Test
    fun `Periode på 17 dager er utenfor ventetid`() {
        val melding =
            skapSykmeldingKafkaMessage(
                fom = onsdag,
                tom = onsdag.plusDays(16),
            )

        erUtenforVentetid(
            listOf(fnr),
            sykmeldingId = melding.sykmelding.id,
            erUtenforVentetidRequest = ErUtenforVentetidRequest(sykmeldingKafkaMessage = melding),
        ).`should be true`()
    }

    @Test
    fun `Periode 17 dager er utenfor ventetid som TokenX-bruker`() {
        val melding =
            skapApenSykmeldingKafkaMessage(
                fom = mandag,
                tom = mandag.plusDays(16),
            ).also { it.publiser() }

        erUtenforVentetid(
            listOf(fnr),
            sykmeldingId = melding.sykmelding.id,
            erUtenforVentetidRequest = ErUtenforVentetidRequest(),
        ).`should be true`()

        erUtenforVentetidSomBrukerTokenX(fnr, sykmeldingId = melding.sykmelding.id).also {
            it.erUtenforVentetid.`should be true`()
            it.oppfolgingsdato `should be equal to` mandag
            erUtenforVentetidSomBrukerTokenX(fnr, sykmeldingId = melding.sykmelding.id).shouldBeEqualTo(it)
        }
    }

    @Test
    fun `Periode på 16 dager er innenfor ventetid`() {
        val melding =
            skapSykmeldingKafkaMessage(
                fom = onsdag,
                tom = onsdag.plusDays(15),
            )
        erUtenforVentetid(
            listOf(fnr),
            sykmeldingId = melding.sykmelding.id,
            erUtenforVentetidRequest = ErUtenforVentetidRequest(sykmeldingKafkaMessage = melding),
        ).`should be false`()
    }

    @Test
    fun `Periode på 17 dager som starter på søndag er utenfor ventetid`() {
        val melding =
            skapApenSykmeldingKafkaMessage(
                fom = sondag,
                tom = sondag.plusDays(16),
            ).also { it.publiser() }
        erUtenforVentetid(
            listOf(fnr),
            sykmeldingId = melding.sykmelding.id,
            erUtenforVentetidRequest = ErUtenforVentetidRequest(),
        ).`should be true`()
    }

    @Test
    fun `Periode på 17 dager som slutter på lørdag innenfor ventetid`() {
        val melding =
            skapApenSykmeldingKafkaMessage(
                fom = lordag.minusDays(16),
                tom = lordag,
            ).also { it.publiser() }
        erUtenforVentetid(
            listOf(fnr),
            sykmeldingId = melding.sykmelding.id,
            erUtenforVentetidRequest = ErUtenforVentetidRequest(),
        ).`should be false`()

        val ventetidResponse = erUtenforVentetidSomBrukerTokenX(fnr, sykmeldingId = melding.sykmelding.id)
        ventetidResponse.erUtenforVentetid.`should be false`()
        ventetidResponse.oppfolgingsdato `should be equal to` lordag.minusDays(16)
        erUtenforVentetidSomBrukerTokenX(fnr, sykmeldingId = melding.sykmelding.id).shouldBeEqualTo(ventetidResponse)
    }

    @Test
    fun `Periode på 17 dager som slutter på fredag utenfor ventetiden`() {
        val melding =
            skapApenSykmeldingKafkaMessage(
                fom = fredag.minusDays(16),
                tom = fredag,
            ).also { it.publiser() }
        erUtenforVentetid(
            listOf(fnr),
            sykmeldingId = melding.sykmelding.id,
            erUtenforVentetidRequest = ErUtenforVentetidRequest(),
        ).`should be true`()
    }

    @Test
    fun `Periode på 16 dager er innenfor ventetiden`() {
        val melding =
            skapApenSykmeldingKafkaMessage(
                fom = mandag,
                tom = mandag.plusDays(15),
            ).also { it.publiser() }
        erUtenforVentetid(
            listOf(fnr),
            sykmeldingId = melding.sykmelding.id,
            erUtenforVentetidRequest = ErUtenforVentetidRequest(),
        ).`should be false`()
    }

    @Test
    fun `Tidligere periode under 16 dager teller ikke`() {
        val melding =
            skapApenSykmeldingKafkaMessage(
                fom = onsdag,
                tom = onsdag.plusDays(15),
            ).also { it.publiser() }

        skapApenSykmeldingKafkaMessage(
            fom = onsdag.minusDays(17),
            tom = onsdag.minusDays(2),
        ).publiser()

        erUtenforVentetid(
            listOf(fnr),
            sykmeldingId = melding.sykmelding.id,
            erUtenforVentetidRequest = ErUtenforVentetidRequest(),
        ).`should be false`()
    }

    @Test
    fun `Tidligere periode over 16 dager teller ikke hvis opphold er mer enn 16 dager`() {
        val melding =
            skapApenSykmeldingKafkaMessage(
                fom = onsdag.plusDays(1),
                tom = onsdag.plusDays(15),
            ).also { it.publiser() }

        skapApenSykmeldingKafkaMessage(
            fom = onsdag.minusDays(33),
            tom = onsdag.minusDays(17),
        ).publiser()

        erUtenforVentetid(
            listOf(fnr),
            sykmeldingId = melding.sykmelding.id,
            erUtenforVentetidRequest = ErUtenforVentetidRequest(),
        ).`should be false`()
    }

    @Test
    fun `Tidligere periode over 16 dager teller hvis opphold er mindre enn 16 dager`() {
        val melding =
            skapApenSykmeldingKafkaMessage(
                fom = onsdag,
                tom = onsdag.plusDays(15),
            ).also { it.publiser() }

        skapApenSykmeldingKafkaMessage(
            fom = onsdag.minusDays(31),
            tom = onsdag.minusDays(15),
        ).publiser()

        erUtenforVentetid(
            listOf(fnr),
            sykmeldingId = melding.sykmelding.id,
            erUtenforVentetidRequest = ErUtenforVentetidRequest(),
        ).`should be true`()
    }

    @Test
    fun `Tidligere periode over 16 dager teller ikke hvis opphold er 16 dager`() {
        skapApenSykmeldingKafkaMessage(
            fom = onsdag.minusDays(32),
            tom = onsdag.minusDays(16),
        ).publiser()

        val melding =
            skapApenSykmeldingKafkaMessage(
                fom = onsdag.plusDays(1),
                tom = onsdag.plusDays(15),
            ).also { it.publiser() }

        erUtenforVentetid(
            listOf(fnr),
            sykmeldingId = melding.sykmelding.id,
            erUtenforVentetidRequest = ErUtenforVentetidRequest(),
        ).`should be false`()
    }

    @Test
    fun `Tidligere periode på 16 dager eller mindre teller ikke selv opphold er mindre enn 16 dager`() {
        val melding =
            skapSykmeldingKafkaMessage(
                fom = onsdag,
                tom = onsdag.plusDays(15),
            )

        skapApenSykmeldingKafkaMessage(
            fom = onsdag.minusDays(17),
            tom = onsdag.minusDays(2),
        ).publiser()

        erUtenforVentetid(
            listOf(fnr),
            sykmeldingId = melding.sykmelding.id,
            erUtenforVentetidRequest = ErUtenforVentetidRequest(sykmeldingKafkaMessage = melding),
        ).`should be false`()
    }

    @Test
    fun `Inkluderer ikke del av periode som er etter aktuell sykmelding tom`() {
        val melding =
            skapApenSykmeldingKafkaMessage(
                fom = LocalDate.of(2018, 8, 18),
                tom = LocalDate.of(2018, 8, 26),
            ).also { it.publiser() }

        skapApenSykmeldingKafkaMessage(
            fom = LocalDate.of(2018, 8, 18),
            tom = LocalDate.of(2018, 9, 26),
        ).publiser()

        erUtenforVentetid(
            listOf(fnr),
            sykmeldingId = melding.sykmelding.id,
            erUtenforVentetidRequest = ErUtenforVentetidRequest(),
        ).`should be false`()
    }

    @Test
    fun `Inkluderer ikke del av periode som er etter aktuell sykmelding tom med flere perioder`() {
        val apensykmedingKafkaMessage = skapApenSykmeldingKafkaMessage()

        val melding =
            apensykmedingKafkaMessage
                .copy(
                    sykmelding =
                        apensykmedingKafkaMessage.sykmelding.copy(
                            sykmeldingsperioder =
                                listOf(
                                    SykmeldingsperiodeAGDTO(
                                        fom = LocalDate.of(2018, 8, 18),
                                        tom = LocalDate.of(2018, 8, 20),
                                        type = PeriodetypeDTO.AKTIVITET_IKKE_MULIG,
                                        reisetilskudd = false,
                                        aktivitetIkkeMulig = null,
                                        behandlingsdager = null,
                                        gradert = null,
                                        innspillTilArbeidsgiver = null,
                                    ),
                                    SykmeldingsperiodeAGDTO(
                                        fom = LocalDate.of(2018, 8, 21),
                                        tom = LocalDate.of(2018, 8, 26),
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

        skapApenSykmeldingKafkaMessage(
            fom = LocalDate.of(2018, 8, 18),
            tom = LocalDate.of(2018, 9, 26),
        ).publiser()

        erUtenforVentetid(
            listOf(fnr),
            sykmeldingId = melding.sykmelding.id,
            erUtenforVentetidRequest = ErUtenforVentetidRequest(),
        ).`should be false`()
    }

    @Test
    fun `Lang avventede sykmelding teller ikke på venteperioden`() {
        val melding =
            skapApenSykmeldingKafkaMessage(
                fom = LocalDate.of(2018, 6, 18),
                tom = LocalDate.of(2018, 8, 26),
                type = PeriodetypeDTO.AVVENTENDE,
            ).also { it.publiser() }

        erUtenforVentetid(
            listOf(fnr),
            sykmeldingId = melding.sykmelding.id,
            erUtenforVentetidRequest = ErUtenforVentetidRequest(),
        ).`should be false`()
    }

    @Test
    fun `Lang sykmelding med reisetilskudd teller ikke på venteperioden`() {
        val melding =
            skapApenSykmeldingKafkaMessage(
                fom = LocalDate.of(2018, 6, 18),
                tom = LocalDate.of(2018, 8, 26),
                type = PeriodetypeDTO.REISETILSKUDD,
            ).also { it.publiser() }

        erUtenforVentetid(
            listOf(fnr),
            sykmeldingId = melding.sykmelding.id,
            erUtenforVentetidRequest = ErUtenforVentetidRequest(),
        ).`should be false`()
    }

    @Test
    fun `Periode på 4 dager er utenfor ventetiden ved redusert venteperiode`() {
        val melding =
            skapApenSykmeldingKafkaMessage(
                fom = mandag,
                tom = mandag.plusDays(3),
                harRedusertArbeidsgiverperiode = true,
            ).also { it.publiser() }
        erUtenforVentetid(
            listOf(fnr),
            sykmeldingId = melding.sykmelding.id,
            erUtenforVentetidRequest = ErUtenforVentetidRequest(),
        ).`should be true`()
    }

    @Test
    fun `Periode på 3 dager er innenfor ventetiden ved redusert venteperiode`() {
        val melding =
            skapApenSykmeldingKafkaMessage(
                fom = mandag,
                tom = mandag.plusDays(2),
                harRedusertArbeidsgiverperiode = true,
            ).also { it.publiser() }
        erUtenforVentetid(
            listOf(fnr),
            sykmeldingId = melding.sykmelding.id,
            erUtenforVentetidRequest = ErUtenforVentetidRequest(),
        ).`should be false`()
    }

    @Test
    fun `Tidligere periode over 3 dager teller ikke hvis opphold er over 16 dager ved redusert venteperiode`() {
        val melding =
            skapApenSykmeldingKafkaMessage(
                fom = onsdag,
                tom = onsdag.plusDays(1),
                harRedusertArbeidsgiverperiode = true,
            ).also { it.publiser() }

        skapApenSykmeldingKafkaMessage(
            fom = onsdag.minusDays(31),
            tom = onsdag.minusDays(15),
            harRedusertArbeidsgiverperiode = true,
        ).publiser()

        erUtenforVentetid(
            listOf(fnr),
            sykmeldingId = melding.sykmelding.id,
            erUtenforVentetidRequest = ErUtenforVentetidRequest(),
        ).`should be true`()
    }

    @Test
    fun `Tidligere periode under 3 dager er innenfor ventetiden ved redusert venteperiode`() {
        val melding =
            skapApenSykmeldingKafkaMessage(
                fom = onsdag,
                tom = onsdag.plusDays(2),
                harRedusertArbeidsgiverperiode = true,
            ).also { it.publiser() }

        skapApenSykmeldingKafkaMessage(
            fom = onsdag.minusDays(22),
            tom = onsdag.minusDays(17),
            harRedusertArbeidsgiverperiode = true,
        ).publiser()

        erUtenforVentetid(
            listOf(fnr),
            sykmeldingId = melding.sykmelding.id,
            erUtenforVentetidRequest = ErUtenforVentetidRequest(),
        ).`should be false`()
    }

    @Test
    fun `Tidligere periode under 3 dager er innenfor venteperiode`() {
        val melding =
            skapApenSykmeldingKafkaMessage(
                fom = onsdag,
                tom = onsdag.plusDays(2),
            ).also { it.publiser() }

        skapApenSykmeldingKafkaMessage(
            fom = onsdag.minusDays(3),
            tom = onsdag.minusDays(2),
        ).publiser()

        erUtenforVentetid(
            listOf(fnr),
            sykmeldingId = melding.sykmelding.id,
            erUtenforVentetidRequest = ErUtenforVentetidRequest(),
        ).`should be false`()
    }

    @Test
    fun `Lang sykmelding med behandlingsdager er innenfor venteperioden`() {
        val melding =
            skapApenSykmeldingKafkaMessage(
                fom = mandag,
                tom = mandag.plusMonths(3),
                type = PeriodetypeDTO.BEHANDLINGSDAGER,
            ).also { it.publiser() }

        erUtenforVentetid(
            listOf(fnr),
            sykmeldingId = melding.sykmelding.id,
            erUtenforVentetidRequest = ErUtenforVentetidRequest(),
        ).`should be false`()
    }

    @Test
    fun `Lang sykmelding med behandlingsdager med minst 16 dager sykmelding foran er utenfor venteperioden`() {
        val melding =
            skapApenSykmeldingKafkaMessage(
                fom = mandag,
                tom = mandag.plusMonths(3),
                type = PeriodetypeDTO.BEHANDLINGSDAGER,
            ).also { it.publiser() }

        skapApenSykmeldingKafkaMessage(
            fom = mandag.minusDays(20),
            tom = mandag.minusDays(1),
        ).publiser()

        erUtenforVentetid(
            listOf(fnr),
            sykmeldingId = melding.sykmelding.id,
            erUtenforVentetidRequest = ErUtenforVentetidRequest(),
        ).`should be true`()
    }

    @Test
    fun `Lang sykmelding med behandlingsdager og 16 dagers tidligere sykmelding med én dag mellom er utenfor venteperioden`() {
        val melding =
            skapApenSykmeldingKafkaMessage(
                fom = mandag,
                tom = mandag.plusMonths(3),
                type = PeriodetypeDTO.BEHANDLINGSDAGER,
            ).also { it.publiser() }

        skapApenSykmeldingKafkaMessage(
            fom = mandag.minusDays(21),
            tom = mandag.minusDays(2),
        ).publiser()

        erUtenforVentetid(
            listOf(fnr),
            sykmeldingId = melding.sykmelding.id,
            erUtenforVentetidRequest = ErUtenforVentetidRequest(),
        ).`should be true`()
    }

    @Test
    fun `Sykmelding på 16 dager med én dag egenmelding foran er utenfor ventetid`() {
        val melding =
            skapSykmeldingKafkaMessage(
                fom = onsdag,
                tom = onsdag.plusDays(15),
                sporsmals =
                    listOf(
                        SporsmalOgSvarDTO(
                            tekst = "Hvilke dager var du borte fra jobb før datoen sykmeldingen gjelder fra?",
                            shortName = ShortNameDTO.PERIODE,
                            svar = "[{\"fom\":\"${onsdag.minusDays(1)}\",\"tom\":\"${onsdag.minusDays(1)}\"}]",
                            svartype = SvartypeDTO.PERIODER,
                        ),
                    ),
            )

        erUtenforVentetid(
            listOf(fnr),
            sykmeldingId = melding.sykmelding.id,
            erUtenforVentetidRequest = ErUtenforVentetidRequest(sykmeldingKafkaMessage = melding),
        ).`should be true`()
    }

    @Nested
    inner class ReturnerVenteperiodeTester {
        @Test
        fun `Venteperioden er 16 dager for en periode lengre enn 16 dager`() {
            val melding1 =
                skapApenSykmeldingKafkaMessage(
                    fom = LocalDate.of(2024, Month.JULY, 1),
                    tom = LocalDate.of(2024, Month.JULY, 20),
                ).also { it.publiser() }

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding1.sykmelding.id,
                erUtenforVentetidRequest = ErUtenforVentetidRequest(),
            ).`should be true`()
        }

        @Test
        fun `Venteperioden er 16 dager for to sammenhengende perioder til sammen lengre enn 16 dager`() {
            val melding1 =
                skapApenSykmeldingKafkaMessage(
                    fom = LocalDate.of(2024, Month.JULY, 1),
                    tom = LocalDate.of(2024, Month.JULY, 10),
                ).also { it.publiser() }

            val melding2 =
                skapApenSykmeldingKafkaMessage(
                    fom = LocalDate.of(2024, Month.JULY, 11),
                    tom = LocalDate.of(2024, Month.JULY, 20),
                ).also { it.publiser() }

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
        }

        @Test
        fun `Venteperioden er 16 dager for periode lengre enn 16 dager med én dags opphold etter forrige sykmelding`() {
            val melding1 =
                skapApenSykmeldingKafkaMessage(
                    fom = LocalDate.of(2024, Month.JULY, 1),
                    tom = LocalDate.of(2024, Month.JULY, 10),
                ).also { it.publiser() }

            val melding2 =
                skapApenSykmeldingKafkaMessage(
                    fom = LocalDate.of(2024, Month.JULY, 12),
                    tom = LocalDate.of(2024, Month.JULY, 31),
                ).also { it.publiser() }

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
        }

        @Test
        fun `To perioder kortere enn 16 dager med én dag mellom som er søndag`() {
            val melding1 =
                skapApenSykmeldingKafkaMessage(
                    fom = LocalDate.of(2024, Month.JULY, 1),
                    tom = LocalDate.of(2024, Month.JULY, 13),
                ).also { it.publiser() }

            val melding2 =
                skapApenSykmeldingKafkaMessage(
                    fom = LocalDate.of(2024, Month.JULY, 15),
                    tom = LocalDate.of(2024, Month.JULY, 23),
                ).also { it.publiser() }

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
        }

        @Test
        fun `Tre perioder kortere enn 16 dager hver med én dag mellom er innenfor ventetiden`() {
            val melding1 =
                skapApenSykmeldingKafkaMessage(
                    fom = LocalDate.of(2024, Month.JULY, 1),
                    tom = LocalDate.of(2024, Month.JULY, 10),
                ).also { it.publiser() }

            val melding2 =
                skapApenSykmeldingKafkaMessage(
                    fom = LocalDate.of(2024, Month.JULY, 12),
                    tom = LocalDate.of(2024, Month.JULY, 18),
                ).also { it.publiser() }

            val melding3 =
                skapApenSykmeldingKafkaMessage(
                    fom = LocalDate.of(2024, Month.JULY, 22),
                    tom = LocalDate.of(2024, Month.JULY, 31),
                ).also { it.publiser() }

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding1.sykmelding.id,
                erUtenforVentetidRequest = ErUtenforVentetidRequest(),
            ).`should be false`()

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding2.sykmelding.id,
                erUtenforVentetidRequest = ErUtenforVentetidRequest(),
            ).`should be false`()

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding3.sykmelding.id,
                erUtenforVentetidRequest = ErUtenforVentetidRequest(),
            ).`should be false`()
        }

        @Test
        fun `Venteperiode fra forrige periode er gjeldende siden opphold i mellom er 16 dager eller mindre`() {
            val melding1 =
                skapApenSykmeldingKafkaMessage(
                    fom = LocalDate.of(2024, Month.JULY, 1),
                    tom = LocalDate.of(2024, Month.JULY, 20),
                ).also { it.publiser() }

            val melding2 =
                skapApenSykmeldingKafkaMessage(
                    fom = LocalDate.of(2024, Month.AUGUST, 22),
                    tom = LocalDate.of(2024, Month.AUGUST, 31),
                ).also { it.publiser() }

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding1.sykmelding.id,
                erUtenforVentetidRequest = ErUtenforVentetidRequest(),
            ).`should be true`()

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding2.sykmelding.id,
                erUtenforVentetidRequest = ErUtenforVentetidRequest(),
            ).`should be false`()
        }
    }

    @Nested
    inner class FeilrettingTester {
        @Test
        fun `Testcase fra Jira-sak`() {
            skapApenSykmeldingKafkaMessage(
                fom = LocalDate.of(2020, 11, 23),
                tom = LocalDate.of(2020, 12, 20),
            ).publiser()

            val melding =
                skapApenSykmeldingKafkaMessage(
                    fom = LocalDate.of(2021, 1, 5),
                    tom = LocalDate.of(2021, 1, 18),
                ).also { it.publiser() }

            erUtenforVentetid(
                listOf(fnr),
                sykmeldingId = melding.sykmelding.id,
                erUtenforVentetidRequest = ErUtenforVentetidRequest(),
            ).`should be true`()
        }

        @Test
        fun `Håndter at siste sykmelding er bare én dag lang`() {
            val melding1 =
                skapApenSykmeldingKafkaMessage(
                    fom = LocalDate.of(2020, 8, 14),
                    tom = LocalDate.of(2020, 8, 30),
                ).also { it.publiser() }

            val melding2 =
                skapApenSykmeldingKafkaMessage(
                    fom = LocalDate.of(2020, 8, 31),
                    tom = LocalDate.of(2020, 8, 31),
                ).also { it.publiser() }

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
        }
    }

    @Test
    fun `Sykmelding på 16 dager med en dag egenmelding foran er utenfor ventetid med data i tilleggsopplysninger`() {
        val melding =
            skapApenSykmeldingKafkaMessage(
                fom = onsdag,
                tom = onsdag.plusDays(15),
            ).also { it.publiser() }

        val tilleggsopplysninger =
            Tilleggsopplysninger(
                egenmeldingsperioder =
                    listOf(
                        Datospenn(
                            fom = onsdag.minusDays(1),
                            tom = onsdag.minusDays(1),
                        ),
                    ),
            )

        erUtenforVentetid(
            listOf(fnr),
            sykmeldingId = melding.sykmelding.id,
            erUtenforVentetidRequest = ErUtenforVentetidRequest(tilleggsopplysninger = tilleggsopplysninger),
        ).`should be true`()
    }

    @Nested
    inner class TokenXSecurityTester {
        @Test
        fun `Authorization feiler hvis audience er feil`() {
            mockMvc
                .perform(
                    MockMvcRequestBuilders
                        .get("/api/bruker/v2/ventetid/12345/erUtenforVentetid")
                        .header("Authorization", "Bearer ${server.tokenxToken(fnr = fnr, audience = "facebook")}")
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
        }

        @Test
        fun `Authorization feiler hvis token mangler`() {
            mockMvc
                .perform(
                    MockMvcRequestBuilders
                        .get("/api/bruker/v2/ventetid/12345/erUtenforVentetid")
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
        }

        @Test
        fun `Authorization feiler hvis clientId er feil`() {
            mockMvc
                .perform(
                    MockMvcRequestBuilders
                        .get("/api/bruker/v2/ventetid/12345/erUtenforVentetid")
                        .header("Authorization", "Bearer ${server.tokenxToken(fnr = fnr, clientId = "facebook")}")
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(MockMvcResultMatchers.status().isForbidden)
        }
    }
}
