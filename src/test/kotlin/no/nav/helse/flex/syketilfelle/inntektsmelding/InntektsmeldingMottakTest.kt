package no.nav.helse.flex.syketilfelle.inntektsmelding

import no.nav.helse.flex.syketilfelle.FellesTestOppsett
import no.nav.helse.flex.syketilfelle.extensions.tilOsloZone
import no.nav.helse.flex.syketilfelle.`should be equal to ignoring nano and zone`
import no.nav.helse.flex.syketilfelle.syketilfellebit.Tag
import no.nav.helse.flex.syketilfelle.syketilfellebit.tilSyketilfellebit
import no.nav.inntektsmeldingkontrakt.Arbeidsgivertype
import no.nav.inntektsmeldingkontrakt.Inntektsmelding
import no.nav.inntektsmeldingkontrakt.Periode
import no.nav.inntektsmeldingkontrakt.Refusjon
import no.nav.inntektsmeldingkontrakt.Status
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldHaveSize
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.TimeUnit

class InntektsmeldingMottakTest : FellesTestOppsett() {
    private final val fnr = "12345432123"

    private final val inntektsmelding =
        Inntektsmelding(
            inntektsmeldingId = UUID.randomUUID().toString(),
            arbeidstakerFnr = fnr,
            arbeidstakerAktorId = "",
            virksomhetsnummer = "org123",
            arbeidsgiverFnr = null,
            arbeidsgiverAktorId = null,
            begrunnelseForReduksjonEllerIkkeUtbetalt = null,
            arbeidsgivertype = Arbeidsgivertype.VIRKSOMHET,
            arbeidsforholdId = null,
            beregnetInntekt = null,
            refusjon = Refusjon(beloepPrMnd = null, opphoersdato = null),
            endringIRefusjoner = listOf(),
            opphoerAvNaturalytelser = listOf(),
            gjenopptakelseNaturalytelser = listOf(),
            arbeidsgiverperioder =
                listOf(
                    Periode(
                        fom = LocalDate.now().minusDays(10),
                        tom = LocalDate.now().minusDays(5),
                    ),
                ),
            status = Status.GYLDIG,
            arkivreferanse = "",
            ferieperioder = listOf(),
            foersteFravaersdag = null,
            mottattDato = LocalDateTime.now(),
            naerRelasjon = null,
            innsenderFulltNavn = "Dagfinn",
            innsenderTelefon = "123",
        )

    @BeforeEach
    @AfterEach
    fun setUp() {
        syketilfellebitRepository.deleteAll()
    }

    @Test
    fun `tar imot inntektsmelding`() {
        producerPåInntektsmeldingTopic(inntektsmelding)

        await().until {
            syketilfellebitRepository.findByFnr(fnr).size == 1
        }

        val biter = syketilfellebitRepository.findByFnr(fnr).map { it.tilSyketilfellebit() }
        biter shouldHaveSize 1
        biter[0].fom `should be equal to` LocalDate.now().minusDays(10)
        biter[0].tom `should be equal to` LocalDate.now().minusDays(5)
        biter[0].inntruffet `should be equal to ignoring nano and zone` inntektsmelding.mottattDato.tilOsloZone()
        biter[0].orgnummer `should be equal to` inntektsmelding.virksomhetsnummer
        biter[0].tags `should be equal to` setOf(Tag.INNTEKTSMELDING, Tag.ARBEIDSGIVERPERIODE)
    }

    @Test
    fun `tar ikke imot mangelfull inntektsmelding`() {
        val inntektsmelding = inntektsmelding.copy(status = Status.MANGELFULL)

        producerPåInntektsmeldingTopic(inntektsmelding)

        await().during(3, TimeUnit.SECONDS).until {
            syketilfellebitRepository.findByFnr(fnr).isEmpty()
        }
    }

    @Test
    fun `tar ikke imot privat inntektsmelding`() {
        val inntektsmelding = inntektsmelding.copy(arbeidsgivertype = Arbeidsgivertype.PRIVAT)

        producerPåInntektsmeldingTopic(inntektsmelding)

        await().during(3, TimeUnit.SECONDS).until {
            syketilfellebitRepository.findByFnr(fnr).isEmpty()
        }
    }
}
