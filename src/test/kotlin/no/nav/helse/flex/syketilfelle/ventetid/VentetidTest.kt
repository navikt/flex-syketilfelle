package no.nav.helse.flex.syketilfelle.ventetid

import no.nav.helse.flex.syketilfelle.Testoppsett
import no.nav.helse.flex.syketilfelle.erUtenforVentetid
import no.nav.helse.flex.syketilfelle.sykmelding.SykmeldingLagring
import no.nav.syfo.model.sykmelding.arbeidsgiver.SykmeldingsperiodeAGDTO
import no.nav.syfo.model.sykmelding.model.PeriodetypeDTO
import no.nav.syfo.model.sykmeldingstatus.ShortNameDTO
import no.nav.syfo.model.sykmeldingstatus.SporsmalOgSvarDTO
import no.nav.syfo.model.sykmeldingstatus.SvartypeDTO
import org.amshove.kluent.`should be false`
import org.amshove.kluent.`should be true`
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.Month

class VentetidTest : VentetidFellesOppsett, Testoppsett() {

    private val mandag = LocalDate.of(2020, Month.JUNE, 1)
    private val søndag = mandag.minusDays(1)
    private val lørdag = mandag.minusDays(2)
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
    fun `periode over 16 dager er utenfor ventetiden`() {
        val melding = skapApenSykmeldingKafkaMessage(
            fom = mandag,
            tom = mandag.plusDays(16)
        ).also { it.publiser() }
        erUtenforVentetid(
            listOf(element = fnr),
            sykmeldingId = melding.sykmelding.id,
            erUtenforVentetidRequest = ErUtenforVentetidRequest()
        ).`should be true`()
    }

    @Test
    fun `periode over 16 dager som starter på søndag utenfor ventetiden`() {
        val melding = skapApenSykmeldingKafkaMessage(
            fom = søndag,
            tom = søndag.plusDays(16)
        ).also { it.publiser() }
        erUtenforVentetid(
            listOf(element = fnr),
            sykmeldingId = melding.sykmelding.id,
            erUtenforVentetidRequest = ErUtenforVentetidRequest()
        ).`should be true`()
    }

    @Test
    fun `periode over 16 dager som slutter på lørdag innenfor ventetiden`() {
        val melding = skapApenSykmeldingKafkaMessage(
            fom = lørdag.minusDays(16),
            tom = lørdag
        ).also { it.publiser() }
        erUtenforVentetid(
            listOf(element = fnr),
            sykmeldingId = melding.sykmelding.id,
            erUtenforVentetidRequest = ErUtenforVentetidRequest()
        ).`should be false`()
    }

    @Test
    fun `periode over 16 dager som slutter på fredag utafor ventetiden`() {
        val melding = skapApenSykmeldingKafkaMessage(
            fom = fredag.minusDays(16),
            tom = fredag
        ).also { it.publiser() }
        erUtenforVentetid(
            listOf(element = fnr),
            sykmeldingId = melding.sykmelding.id,
            erUtenforVentetidRequest = ErUtenforVentetidRequest()
        ).`should be true`()
    }

    @Test
    fun `periode under 16 dager er utenfor ventetiden`() {
        val melding = skapApenSykmeldingKafkaMessage(
            fom = mandag,
            tom = mandag.plusDays(15)
        ).also { it.publiser() }
        erUtenforVentetid(
            listOf(element = fnr),
            sykmeldingId = melding.sykmelding.id,
            erUtenforVentetidRequest = ErUtenforVentetidRequest()
        ).`should be false`()
    }

    @Test
    fun `tidligere periode over 16 dager teller ikke hvis opphold er over 16 dager`() {
        val melding = skapApenSykmeldingKafkaMessage(
            fom = onsdag,
            tom = onsdag.plusDays(15)
        ).also { it.publiser() }

        skapApenSykmeldingKafkaMessage(
            fom = onsdag.minusDays(33),
            tom = onsdag.minusDays(17)
        ).publiser()

        erUtenforVentetid(
            listOf(element = fnr),
            sykmeldingId = melding.sykmelding.id,
            erUtenforVentetidRequest = ErUtenforVentetidRequest()
        ).`should be false`()
    }

    @Test
    fun `tidligere periode under 16 dager er utenfor ventetid`() {
        val melding = skapApenSykmeldingKafkaMessage(
            fom = onsdag,
            tom = onsdag.plusDays(15)
        ).also { it.publiser() }

        skapApenSykmeldingKafkaMessage(
            fom = onsdag.minusDays(17),
            tom = onsdag.minusDays(2)
        ).publiser()

        erUtenforVentetid(
            listOf(element = fnr),
            sykmeldingId = melding.sykmelding.id,
            erUtenforVentetidRequest = ErUtenforVentetidRequest()
        ).`should be false`()
    }

    @Test
    fun `tidligere periode over 16 dager teller hvis opphold er under 16 dager`() {
        val melding = skapApenSykmeldingKafkaMessage(
            fom = onsdag,
            tom = onsdag.plusDays(15)
        ).also { it.publiser() }

        skapApenSykmeldingKafkaMessage(
            fom = onsdag.minusDays(31),
            tom = onsdag.minusDays(15)
        ).publiser()

        erUtenforVentetid(
            listOf(element = fnr),
            sykmeldingId = melding.sykmelding.id,
            erUtenforVentetidRequest = ErUtenforVentetidRequest()
        ).`should be true`()
    }

    @Test
    fun `tidligere periode over 16 dager teller hvis opphold er 16 dager`() {
        skapApenSykmeldingKafkaMessage(
            fom = onsdag.minusDays(32),
            tom = onsdag.minusDays(16)
        ).publiser()

        val melding = skapApenSykmeldingKafkaMessage(
            fom = onsdag,
            tom = onsdag.plusDays(15)
        ).also { it.publiser() }

        erUtenforVentetid(
            listOf(element = fnr),
            sykmeldingId = melding.sykmelding.id,
            erUtenforVentetidRequest = ErUtenforVentetidRequest()
        ).`should be true`()
    }

    @Test
    fun `testcase fra jirasak`() {
        skapApenSykmeldingKafkaMessage(
            fom = LocalDate.of(2020, 11, 23),
            tom = LocalDate.of(2020, 12, 20)
        )
            .publiser()

        val melding = skapApenSykmeldingKafkaMessage(
            fom = LocalDate.of(2021, 1, 5),
            tom = LocalDate.of(2021, 1, 18)
        ).also { it.publiser() }

        erUtenforVentetid(
            listOf(element = fnr),
            sykmeldingId = melding.sykmelding.id,
            erUtenforVentetidRequest = ErUtenforVentetidRequest()
        ).`should be true`()
    }

    @Test
    fun `tidligere periode under 16 dager er utenfor ventetid ny sykmelding i body`() {
        val melding = skapSykmeldingKafkaMessage(
            fom = onsdag,
            tom = onsdag.plusDays(15)
        )

        skapApenSykmeldingKafkaMessage(
            fom = onsdag.minusDays(17),
            tom = onsdag.minusDays(2)
        ).publiser()

        erUtenforVentetid(
            listOf(element = fnr),
            sykmeldingId = melding.sykmelding.id,
            erUtenforVentetidRequest = ErUtenforVentetidRequest(sykmeldingKafkaMessage = melding)
        ).`should be false`()
    }

    @Test
    fun `tidligere periode over 16 dager teller hvis opphold er under 16 dager ny sykmelding i body`() {
        val melding = skapSykmeldingKafkaMessage(
            fom = onsdag,
            tom = onsdag.plusDays(15)
        )

        skapApenSykmeldingKafkaMessage(
            fom = onsdag.minusDays(31),
            tom = onsdag.minusDays(15)
        ).publiser()

        erUtenforVentetid(
            listOf(element = fnr),
            sykmeldingId = melding.sykmelding.id,
            erUtenforVentetidRequest = ErUtenforVentetidRequest(sykmeldingKafkaMessage = melding)
        ).`should be true`()
    }

    @Test
    fun `Inkluderer ikke del av periode som er etter aktuell sykmeldingstom`() {
        val melding = skapApenSykmeldingKafkaMessage(
            fom = LocalDate.of(2018, 8, 18),
            tom = LocalDate.of(2018, 8, 26)
        ).also { it.publiser() }

        skapApenSykmeldingKafkaMessage(
            fom = LocalDate.of(2018, 8, 18),
            tom = LocalDate.of(2018, 9, 26)
        ).publiser()

        erUtenforVentetid(
            listOf(element = fnr),
            sykmeldingId = melding.sykmelding.id,
            erUtenforVentetidRequest = ErUtenforVentetidRequest()
        ).`should be false`()
    }

    @Test
    fun `Inkluderer ikke del av periode som er etter aktuell sykmeldingstom med flere perioder`() {
        val apensykmedingKafkaMessage = skapApenSykmeldingKafkaMessage()

        val melding = apensykmedingKafkaMessage
            .copy(
                sykmelding = apensykmedingKafkaMessage.sykmelding.copy(
                    sykmeldingsperioder = listOf(
                        SykmeldingsperiodeAGDTO(
                            fom = LocalDate.of(2018, 8, 18),
                            tom = LocalDate.of(2018, 8, 20),
                            type = PeriodetypeDTO.AKTIVITET_IKKE_MULIG,
                            reisetilskudd = false,
                            aktivitetIkkeMulig = null,
                            behandlingsdager = null,
                            gradert = null,
                            innspillTilArbeidsgiver = null
                        ),
                        SykmeldingsperiodeAGDTO(
                            fom = LocalDate.of(2018, 8, 21),
                            tom = LocalDate.of(2018, 8, 26),
                            type = PeriodetypeDTO.AKTIVITET_IKKE_MULIG,
                            reisetilskudd = false,
                            aktivitetIkkeMulig = null,
                            behandlingsdager = null,
                            gradert = null,
                            innspillTilArbeidsgiver = null
                        )
                    )
                )
            )
            .also { it.publiser() }

        skapApenSykmeldingKafkaMessage(
            fom = LocalDate.of(2018, 8, 18),
            tom = LocalDate.of(2018, 9, 26)
        ).publiser()

        erUtenforVentetid(
            listOf(element = fnr),
            sykmeldingId = melding.sykmelding.id,
            erUtenforVentetidRequest = ErUtenforVentetidRequest()
        ).`should be false`()
    }

    @Test
    fun `Lang avventede sykmelding teller ikke på venteperioden`() {
        val melding = skapApenSykmeldingKafkaMessage(
            fom = LocalDate.of(2018, 6, 18),
            tom = LocalDate.of(2018, 8, 26),
            type = PeriodetypeDTO.AVVENTENDE
        ).also { it.publiser() }

        erUtenforVentetid(
            listOf(element = fnr),
            sykmeldingId = melding.sykmelding.id,
            erUtenforVentetidRequest = ErUtenforVentetidRequest()
        ).`should be false`()
    }

    @Test
    fun `Lang sykmelding med reisetilskudd teller ikke på venteperioden`() {
        val melding = skapApenSykmeldingKafkaMessage(
            fom = LocalDate.of(2018, 6, 18),
            tom = LocalDate.of(2018, 8, 26),
            type = PeriodetypeDTO.REISETILSKUDD
        ).also { it.publiser() }

        erUtenforVentetid(
            listOf(element = fnr),
            sykmeldingId = melding.sykmelding.id,
            erUtenforVentetidRequest = ErUtenforVentetidRequest()
        ).`should be false`()
    }

    @Test
    fun `periode på 4 dager er utenfor ventetide ved redusert venteperiode`() {
        val melding = skapApenSykmeldingKafkaMessage(
            fom = mandag,
            tom = mandag.plusDays(3),
            harRedusertArbeidsgiverperiode = true
        ).also { it.publiser() }
        erUtenforVentetid(
            listOf(element = fnr),
            sykmeldingId = melding.sykmelding.id,
            erUtenforVentetidRequest = ErUtenforVentetidRequest()
        ).`should be true`()
    }

    @Test
    fun `periode på 3 dager er utenfor ventetiden ved redusert venteperiode`() {
        val melding = skapApenSykmeldingKafkaMessage(
            fom = mandag,
            tom = mandag.plusDays(2),
            harRedusertArbeidsgiverperiode = true
        ).also { it.publiser() }
        erUtenforVentetid(
            listOf(element = fnr),
            sykmeldingId = melding.sykmelding.id,
            erUtenforVentetidRequest = ErUtenforVentetidRequest()
        ).`should be false`()
    }

    @Test
    fun `tidligere periode over 3 dager teller ikke hvis opphold er over 16 dager ved redusert venteperiode`() {
        val melding = skapApenSykmeldingKafkaMessage(
            fom = onsdag,
            tom = onsdag.plusDays(1),
            harRedusertArbeidsgiverperiode = true
        ).also { it.publiser() }

        skapApenSykmeldingKafkaMessage(
            fom = onsdag.minusDays(31),
            tom = onsdag.minusDays(15),
            harRedusertArbeidsgiverperiode = true
        ).publiser()

        erUtenforVentetid(
            listOf(element = fnr),
            sykmeldingId = melding.sykmelding.id,
            erUtenforVentetidRequest = ErUtenforVentetidRequest()
        ).`should be true`()
    }

    @Test
    fun `tidligere periode under 3 dager er utenfor ventetid ved redusert veteperiode`() {
        val melding = skapApenSykmeldingKafkaMessage(
            fom = onsdag,
            tom = onsdag.plusDays(2),
            harRedusertArbeidsgiverperiode = true
        ).also { it.publiser() }

        skapApenSykmeldingKafkaMessage(
            fom = onsdag.minusDays(22),
            tom = onsdag.minusDays(17),
            harRedusertArbeidsgiverperiode = true
        ).publiser()

        erUtenforVentetid(
            listOf(element = fnr),
            sykmeldingId = melding.sykmelding.id,
            erUtenforVentetidRequest = ErUtenforVentetidRequest()
        ).`should be false`()
    }

    @Test
    fun `tidligere periode under 3 dager er innenfor venteperiode`() {
        val melding = skapApenSykmeldingKafkaMessage(
            fom = onsdag,
            tom = onsdag.plusDays(2)
        ).also { it.publiser() }

        skapApenSykmeldingKafkaMessage(
            fom = onsdag.minusDays(3),
            tom = onsdag.minusDays(2)
        ).publiser()

        erUtenforVentetid(
            listOf(element = fnr),
            sykmeldingId = melding.sykmelding.id,
            erUtenforVentetidRequest = ErUtenforVentetidRequest()
        ).`should be false`()
    }

    @Test
    fun `Lang sykmelding med behandlingsdager er innenfor venteperioden`() {
        val melding = skapApenSykmeldingKafkaMessage(
            fom = mandag,
            tom = mandag.plusMonths(3),
            type = PeriodetypeDTO.BEHANDLINGSDAGER
        ).also { it.publiser() }

        erUtenforVentetid(
            listOf(element = fnr),
            sykmeldingId = melding.sykmelding.id,
            erUtenforVentetidRequest = ErUtenforVentetidRequest()
        ).`should be false`()
    }

    @Test
    fun `Lang sykmelding med behandlingsdager med minst 16 dager sykmelding foran er utafor venteperioden`() {
        val melding = skapApenSykmeldingKafkaMessage(
            fom = mandag,
            tom = mandag.plusMonths(3),
            type = PeriodetypeDTO.BEHANDLINGSDAGER
        ).also { it.publiser() }

        skapApenSykmeldingKafkaMessage(
            fom = mandag.minusDays(20),
            tom = mandag.minusDays(1)
        ).publiser()

        erUtenforVentetid(
            listOf(element = fnr),
            sykmeldingId = melding.sykmelding.id,
            erUtenforVentetidRequest = ErUtenforVentetidRequest()
        ).`should be true`()
    }

    @Test
    fun `Lang sykmelding med behandlingsdager med minst 16 dager sykmelding foran med en dag mellom er utafor venteperioden`() {
        val melding = skapApenSykmeldingKafkaMessage(
            fom = mandag,
            tom = mandag.plusMonths(3),
            type = PeriodetypeDTO.BEHANDLINGSDAGER
        ).also { it.publiser() }

        skapApenSykmeldingKafkaMessage(
            fom = mandag.minusDays(21),
            tom = mandag.minusDays(2)
        ).publiser()

        erUtenforVentetid(
            listOf(element = fnr),
            sykmeldingId = melding.sykmelding.id,
            erUtenforVentetidRequest = ErUtenforVentetidRequest()
        ).`should be true`()
    }

    @Test
    fun `Håndter at siste sykmelding er bare en dag -  Dette skapte en bug`() {
        skapApenSykmeldingKafkaMessage(
            fom = LocalDate.of(2020, 8, 14),
            tom = LocalDate.of(2020, 8, 30)
        ).also { it.publiser() }

        val melding = skapApenSykmeldingKafkaMessage(
            fom = LocalDate.of(2020, 8, 31),
            tom = LocalDate.of(2020, 8, 31)
        ).also { it.publiser() }

        erUtenforVentetid(
            listOf(element = fnr),
            sykmeldingId = melding.sykmelding.id,
            erUtenforVentetidRequest = ErUtenforVentetidRequest()
        ).`should be true`()
    }

    @Test
    fun `sykmelding på 17 dager er utenfor ventetid`() {
        val melding = skapSykmeldingKafkaMessage(
            fom = onsdag,
            tom = onsdag.plusDays(16)
        )

        erUtenforVentetid(
            listOf(element = fnr),
            sykmeldingId = melding.sykmelding.id,
            erUtenforVentetidRequest = ErUtenforVentetidRequest(sykmeldingKafkaMessage = melding)
        ).`should be true`()
    }

    @Test
    fun `sykmelding på 16 dager er innenfor ventetid`() {
        val melding = skapSykmeldingKafkaMessage(
            fom = onsdag,
            tom = onsdag.plusDays(15)
        )
        erUtenforVentetid(
            listOf(element = fnr),
            sykmeldingId = melding.sykmelding.id,
            erUtenforVentetidRequest = ErUtenforVentetidRequest(sykmeldingKafkaMessage = melding)
        ).`should be false`()
    }

    @Test
    fun `sykmelding på 16 dager med en dag egenmelding foran er utafor ventetid`() {
        val melding = skapSykmeldingKafkaMessage(
            fom = onsdag,
            tom = onsdag.plusDays(15),
            sporsmals = listOf(
                SporsmalOgSvarDTO(
                    tekst = "Hvilke dager var du borte fra jobb før datoen sykmeldingen gjelder fra?",
                    shortName = ShortNameDTO.PERIODE,
                    svar = "[{\"fom\":\"${onsdag.minusDays(1)}\",\"tom\":\"${onsdag.minusDays(1)}\"}]",
                    svartype = SvartypeDTO.PERIODER
                )
            )
        )

        erUtenforVentetid(
            listOf(element = fnr),
            sykmeldingId = melding.sykmelding.id,
            erUtenforVentetidRequest = ErUtenforVentetidRequest(sykmeldingKafkaMessage = melding)
        ).`should be true`()
    }

    @Test
    fun `sykmelding på 16 dager med en dag egenmelding foran er utafor ventetid med data i tilleggsopplysninger`() {
        val melding = skapApenSykmeldingKafkaMessage(
            fom = onsdag,
            tom = onsdag.plusDays(15)
        ).also { it.publiser() }

        val tilleggsopplysninger = Tilleggsopplysninger(
            egenmeldingsperioder = listOf(
                Datospenn(
                    fom = onsdag.minusDays(1),
                    tom = onsdag.minusDays(1)
                )
            )
        )

        erUtenforVentetid(
            listOf(element = fnr),
            sykmeldingId = melding.sykmelding.id,
            erUtenforVentetidRequest = ErUtenforVentetidRequest(tilleggsopplysninger = tilleggsopplysninger)
        ).`should be true`()
    }
}
