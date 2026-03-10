package no.nav.helse.flex.syketilfelle.ventetid

import no.nav.helse.flex.syketilfelle.FellesTestOppsett
import no.nav.helse.flex.syketilfelle.lagBekreftetSykmeldingKafkaMessage
import no.nav.helse.flex.syketilfelle.lagMottattSykmeldingKafkaMessage
import no.nav.syfo.model.sykmeldingstatus.ShortNameDTO
import no.nav.syfo.model.sykmeldingstatus.SporsmalOgSvarDTO
import no.nav.syfo.model.sykmeldingstatus.SvartypeDTO
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should be false`
import org.amshove.kluent.`should be true`
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.Month

class DebugEgenmeldingsdagerTest : FellesTestOppsett() {
    @Autowired
    private lateinit var ventetidUtregner: VentetidUtregner

    @BeforeEach
    @AfterEach
    fun setUp() {
        syketilfellebitRepository.deleteAll()
    }

    private val fnr = "11111111111"

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
