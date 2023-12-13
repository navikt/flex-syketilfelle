package no.nav.helse.flex.syketilfelle.sykepengesoknad

import no.nav.helse.flex.sykepengesoknad.kafka.*
import no.nav.helse.flex.syketilfelle.Testoppsett
import no.nav.helse.flex.syketilfelle.serialisertTilString
import no.nav.helse.flex.syketilfelle.soknad.SYKEPENGESOKNAD_TOPIC
import no.nav.helse.flex.syketilfelle.syketilfellebit.*
import no.nav.helse.flex.syketilfelle.syketilfellebit.Tag.*
import no.nav.helse.flex.syketilfelle.syketilfellebit.Tag.SYKEPENGESOKNAD
import no.nav.helse.flex.syketilfelle.syketilfellebit.tilSyketilfellebit
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldHaveSize
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.TimeUnit

class SykepengesoknadMottakTest : Testoppsett() {
    private final val fnr = "12345432123"

    @BeforeEach
    @AfterEach
    fun setUp() {
        syketilfellebitRepository.deleteAll()
    }

    @Test
    fun `vanlig sendt soknad`() {
        val soknad =
            SykepengesoknadDTO(
                id = "id",
                type = SoknadstypeDTO.ARBEIDSTAKERE,
                status = SoknadsstatusDTO.SENDT,
                sykmeldingId = "sykmeldingId",
                arbeidsgiver =
                    ArbeidsgiverDTO(
                        navn = "arbeidsgivernavn",
                        orgnummer = "orgnummer",
                    ),
                arbeidssituasjon = ArbeidssituasjonDTO.ARBEIDSTAKER,
                korrigerer = "korrigerer",
                korrigertAv = "korrigertAv",
                soktUtenlandsopphold = null,
                fom = LocalDate.now().minusWeeks(1),
                tom = LocalDate.now(),
                startSyketilfelle = LocalDate.now().minusWeeks(1),
                sykmeldingSkrevet = LocalDateTime.now().minusWeeks(1),
                arbeidGjenopptatt = null,
                opprettet = LocalDateTime.now().minusWeeks(1),
                sendtNav = LocalDateTime.now(),
                sendtArbeidsgiver = LocalDateTime.now(),
                egenmeldinger = emptyList(),
                papirsykmeldinger = emptyList(),
                fravar = emptyList(),
                andreInntektskilder = emptyList(),
                soknadsperioder =
                    listOf(
                        SoknadsperiodeDTO(
                            fom = LocalDate.now().minusWeeks(1),
                            tom = LocalDate.now(),
                            sykmeldingsgrad = 100,
                        ),
                    ),
                sporsmal = null,
                fnr = fnr,
            )

        produserPåSøknadTopic(soknad)

        await().atMost(10, TimeUnit.SECONDS).until {
            syketilfellebitRepository.findByFnr(fnr).size == 1
        }

        val biter = syketilfellebitRepository.findByFnr(fnr).map { it.tilSyketilfellebit() }
        biter shouldHaveSize 1
        biter.first().tags `should be equal to` setOf(SYKEPENGESOKNAD, SENDT)
        biter.first().korrigererSendtSoknad `should be equal to` soknad.korrigerer

        syketilfellebitRepository.findFirst300ByPublisertOrderByOpprettetAsc(false).shouldHaveSize(1)
        syketilfellebitRepository.findFirst300ByPublisertOrderByOpprettetAsc(true).shouldHaveSize(0)
    }

    @Test
    fun `soknad sendt alle relevante felter fylt ut`() {
        val soknad =
            SykepengesoknadDTO(
                id = "id",
                type = SoknadstypeDTO.ARBEIDSTAKERE,
                status = SoknadsstatusDTO.SENDT,
                sykmeldingId = "sykmeldingId",
                arbeidsgiver =
                    ArbeidsgiverDTO(
                        navn = "arbeidsgivernavn",
                        orgnummer = "orgnummer",
                    ),
                arbeidssituasjon = ArbeidssituasjonDTO.ARBEIDSTAKER,
                korrigerer = "korrigerer",
                korrigertAv = "korrigertAv",
                soktUtenlandsopphold = null,
                fom = LocalDate.now().minusWeeks(1),
                tom = LocalDate.now(),
                startSyketilfelle = LocalDate.now().minusWeeks(1),
                sykmeldingSkrevet = LocalDateTime.now().minusWeeks(1),
                opprettet = LocalDateTime.now().minusWeeks(1),
                sendtNav = LocalDateTime.now(),
                sendtArbeidsgiver = LocalDateTime.now(),
                arbeidGjenopptatt = LocalDate.now().minusWeeks(1),
                egenmeldinger =
                    listOf(
                        PeriodeDTO(
                            fom = LocalDate.now().minusWeeks(1),
                            tom = LocalDate.now().minusWeeks(1).plusDays(1),
                        ),
                    ),
                fravarForSykmeldingen =
                    listOf(
                        PeriodeDTO(
                            fom = LocalDate.now().minusWeeks(1),
                            tom = LocalDate.now().minusWeeks(1).plusDays(1),
                        ),
                    ),
                papirsykmeldinger =
                    listOf(
                        PeriodeDTO(
                            fom = LocalDate.now().minusWeeks(1).plusDays(2),
                            tom = LocalDate.now().minusWeeks(1).plusDays(3),
                        ),
                    ),
                fravar =
                    listOf(
                        FravarDTO(
                            fom = LocalDate.now().minusWeeks(1),
                            tom = LocalDate.now().minusWeeks(1),
                            type = FravarstypeDTO.PERMISJON,
                        ),
                        FravarDTO(
                            fom = LocalDate.now().minusWeeks(1).plusDays(1),
                            tom = LocalDate.now().minusWeeks(1).plusDays(1),
                            type = FravarstypeDTO.UTLANDSOPPHOLD,
                        ),
                        FravarDTO(
                            fom = LocalDate.now().minusWeeks(1).plusDays(2),
                            tom = LocalDate.now().minusWeeks(1).plusDays(2),
                            type = FravarstypeDTO.UTDANNING_FULLTID,
                        ),
                        FravarDTO(
                            fom = LocalDate.now().minusWeeks(1).plusDays(3),
                            tom = LocalDate.now().minusWeeks(1).plusDays(3),
                            type = FravarstypeDTO.UTDANNING_DELTID,
                        ),
                        FravarDTO(
                            fom = LocalDate.now().minusWeeks(1).plusDays(4),
                            tom = LocalDate.now().minusWeeks(1).plusDays(4),
                            type = FravarstypeDTO.FERIE,
                        ),
                    ),
                andreInntektskilder = emptyList(),
                soknadsperioder =
                    listOf(
                        SoknadsperiodeDTO(
                            fom = LocalDate.now().minusWeeks(1),
                            tom = LocalDate.now().minusWeeks(1).plusDays(1),
                            sykmeldingsgrad = 100,
                            faktiskGrad = 0,
                        ),
                        SoknadsperiodeDTO(
                            fom = LocalDate.now().minusWeeks(1).plusDays(2),
                            tom = LocalDate.now().minusWeeks(1).plusDays(3),
                            sykmeldingsgrad = 100,
                            faktiskGrad = 49,
                        ),
                        SoknadsperiodeDTO(
                            fom = (LocalDate.now().minusWeeks(1).plusDays(4)),
                            tom = (LocalDate.now().minusWeeks(1).plusDays(5)),
                            sykmeldingsgrad = (100),
                            faktiskGrad = (100),
                        ),
                        SoknadsperiodeDTO(
                            fom = (LocalDate.now().minusWeeks(1).plusDays(6)),
                            tom = (LocalDate.now().minusWeeks(1).plusDays(7)),
                            sykmeldingstype = (SykmeldingstypeDTO.BEHANDLINGSDAGER),
                            sykmeldingsgrad = (100),
                            faktiskGrad = (0),
                        ),
                    ),
                sporsmal = null,
                fnr = fnr,
            )

        produserPåSøknadTopic(soknad)

        await().atMost(10, TimeUnit.SECONDS).until {
            syketilfellebitRepository.findByFnr(fnr).size == 14
        }

        val biter = syketilfellebitRepository.findByFnr(fnr).map { it.tilSyketilfellebit() }
        biter shouldHaveSize 14
        biter.first().tags `should be equal to` setOf(SYKEPENGESOKNAD, SENDT)
        biter.first().korrigererSendtSoknad `should be equal to` soknad.korrigerer

        biter[0].tags `should be equal to` setOf(SYKEPENGESOKNAD, SENDT)
        biter[1].tags `should be equal to` setOf(SYKEPENGESOKNAD, SENDT, PERMISJON)
        biter[2].tags `should be equal to` setOf(SYKEPENGESOKNAD, SENDT, OPPHOLD_UTENFOR_NORGE)
        biter[3].tags `should be equal to` setOf(SYKEPENGESOKNAD, SENDT, UTDANNING, FULLTID)
        biter[4].tags `should be equal to` setOf(SYKEPENGESOKNAD, SENDT, UTDANNING, DELTID)
        biter[5].tags `should be equal to` setOf(SYKEPENGESOKNAD, SENDT, FERIE)
        biter[12].tags `should be equal to` setOf(SYKEPENGESOKNAD, SENDT, FRAVAR_FOR_SYKMELDING)

        syketilfellebitRepository.findFirst300ByPublisertOrderByOpprettetAsc(false).shouldHaveSize(14)
        syketilfellebitRepository.findFirst300ByPublisertOrderByOpprettetAsc(true).shouldHaveSize(0)
    }

    @Test
    fun `gradert reisetilskudd arbeidstaker soknad sendt lagres`() {
        val soknad =
            SykepengesoknadDTO(
                id = "id",
                type = SoknadstypeDTO.GRADERT_REISETILSKUDD,
                status = SoknadsstatusDTO.SENDT,
                sykmeldingId = "sykmeldingId",
                arbeidsgiver =
                    ArbeidsgiverDTO(
                        navn = "arbeidsgivernavn",
                        orgnummer = "orgnummer",
                    ),
                arbeidssituasjon = ArbeidssituasjonDTO.ARBEIDSTAKER,
                korrigerer = null,
                korrigertAv = null,
                soktUtenlandsopphold = null,
                fom = LocalDate.now().minusWeeks(1),
                tom = LocalDate.now(),
                startSyketilfelle = LocalDate.now().minusWeeks(1),
                sykmeldingSkrevet = LocalDateTime.now().minusWeeks(1),
                arbeidGjenopptatt = null,
                opprettet = LocalDateTime.now().minusWeeks(1),
                sendtNav = LocalDateTime.now(),
                sendtArbeidsgiver = LocalDateTime.now(),
                egenmeldinger = emptyList(),
                papirsykmeldinger = emptyList(),
                fravar = emptyList(),
                andreInntektskilder = emptyList(),
                soknadsperioder =
                    listOf(
                        SoknadsperiodeDTO(
                            fom = LocalDate.now().minusWeeks(1),
                            tom = LocalDate.now(),
                            sykmeldingsgrad = 100,
                        ),
                    ),
                sporsmal = null,
                fnr = fnr,
            )

        produserPåSøknadTopic(soknad)

        await().atMost(10, TimeUnit.SECONDS).until {
            syketilfellebitRepository.findByFnr(fnr).size == 1
        }

        val biter = syketilfellebitRepository.findByFnr(fnr).map { it.tilSyketilfellebit() }
        biter shouldHaveSize 1
        biter.first().tags `should be equal to` setOf(SYKEPENGESOKNAD, SENDT)
        biter.first().korrigererSendtSoknad `should be equal to` soknad.korrigerer

        biter[0].tags `should be equal to` setOf(SYKEPENGESOKNAD, SENDT)
    }

    @Test
    fun `gradert reisetilskudd arbeidsledig soknad sendt lagres ikke`() {
        val soknad =
            SykepengesoknadDTO(
                id = "id",
                type = SoknadstypeDTO.GRADERT_REISETILSKUDD,
                status = SoknadsstatusDTO.SENDT,
                sykmeldingId = "sykmeldingId",
                arbeidsgiver =
                    ArbeidsgiverDTO(
                        navn = "arbeidsgivernavn",
                        orgnummer = "orgnummer",
                    ),
                arbeidssituasjon = ArbeidssituasjonDTO.ARBEIDSLEDIG,
                korrigerer = null,
                korrigertAv = null,
                soktUtenlandsopphold = null,
                fom = LocalDate.now().minusWeeks(1),
                tom = LocalDate.now(),
                startSyketilfelle = LocalDate.now().minusWeeks(1),
                sykmeldingSkrevet = LocalDateTime.now().minusWeeks(1),
                arbeidGjenopptatt = null,
                opprettet = LocalDateTime.now().minusWeeks(1),
                sendtNav = LocalDateTime.now(),
                sendtArbeidsgiver = LocalDateTime.now(),
                egenmeldinger = emptyList(),
                papirsykmeldinger = emptyList(),
                fravar = emptyList(),
                andreInntektskilder = emptyList(),
                soknadsperioder =
                    listOf(
                        SoknadsperiodeDTO(
                            fom = LocalDate.now().minusWeeks(1),
                            tom = LocalDate.now(),
                            sykmeldingsgrad = 100,
                        ),
                    ),
                sporsmal = null,
                fnr = "fnr",
            )

        produserPåSøknadTopic(soknad)

        await().during(4, TimeUnit.SECONDS).until {
            syketilfellebitRepository.findByFnr(fnr).isEmpty()
        }
    }

    private val behandlingsdagSøknad =
        SykepengesoknadDTO(
            id = "id",
            type = SoknadstypeDTO.BEHANDLINGSDAGER,
            status = SoknadsstatusDTO.SENDT,
            arbeidsgiver =
                ArbeidsgiverDTO(
                    navn = "arbeidsgivernavn",
                    orgnummer = "orgnummer",
                ),
            sporsmal = emptyList(),
            arbeidssituasjon = ArbeidssituasjonDTO.ARBEIDSTAKER,
            korrigerer = "korrigerer",
            korrigertAv = "korrigertAv",
            opprettet = LocalDateTime.now().minusWeeks(1),
            sendtNav = LocalDateTime.now(),
            sendtArbeidsgiver = LocalDateTime.now(),
            sykmeldingId = "sykmeldingId",
            fom = LocalDate.now().minusWeeks(1),
            tom = LocalDate.now(),
            startSyketilfelle = LocalDate.now().minusWeeks(1),
            sykmeldingSkrevet = LocalDateTime.now().minusWeeks(1),
            soknadsperioder =
                listOf(
                    SoknadsperiodeDTO(
                        fom = LocalDate.now().minusWeeks(1),
                        tom = LocalDate.now(),
                        sykmeldingstype = SykmeldingstypeDTO.BEHANDLINGSDAGER,
                    ),
                ),
            behandlingsdager = emptyList(),
            egenmeldinger = emptyList(),
            papirsykmeldinger = emptyList(),
            andreInntektskilder = emptyList(),
            fnr = fnr,
        )

    @Test
    fun `behandlingsdagsøknad sendt ingen ekstra felter`() {
        produserPåSøknadTopic(behandlingsdagSøknad)

        await().atMost(10, TimeUnit.SECONDS).until {
            syketilfellebitRepository.findByFnr(fnr).size == 1
        }
        val biter = syketilfellebitRepository.findByFnr(fnr).map { it.tilSyketilfellebit() }
        biter shouldHaveSize 1
        biter.first().tags `should be equal to` setOf(SYKEPENGESOKNAD, BEHANDLINGSDAGER, SENDT)
    }

    @Test
    fun `behandlingsdagsøknad sendt med alle relevante felter fyllt ut`() {
        val soknadMedFelter =
            behandlingsdagSøknad.copy(
                id = UUID.randomUUID().toString(),
                behandlingsdager =
                    listOf(
                        LocalDate.of(2019, 3, 9),
                        LocalDate.of(2019, 3, 4),
                    ),
                egenmeldinger =
                    listOf(
                        PeriodeDTO(
                            fom = LocalDate.now().minusWeeks(1),
                            tom = LocalDate.now().minusWeeks(1).plusDays(1),
                        ),
                    ),
                papirsykmeldinger =
                    listOf(
                        PeriodeDTO(
                            fom = LocalDate.now().minusWeeks(1).plusDays(2),
                            tom = LocalDate.now().minusWeeks(1).plusDays(3),
                        ),
                    ),
            )
        syketilfellebitRepository.findByFnr(fnr).shouldBeEmpty()

        produserPåSøknadTopic(soknadMedFelter)

        await().atMost(10, TimeUnit.SECONDS).until {
            val findByFnr = syketilfellebitRepository.findByFnr(fnr)
            findByFnr.size == 5
        }

        val biter = syketilfellebitRepository.findByFnr(fnr).map { it.tilSyketilfellebit() }
        biter shouldHaveSize 5
        biter[0].tags `should be equal to` setOf(SYKEPENGESOKNAD, BEHANDLINGSDAGER, SENDT)
        biter[1].tags `should be equal to` setOf(SYKEPENGESOKNAD, SENDT, EGENMELDING)
        biter[2].tags `should be equal to` setOf(SYKEPENGESOKNAD, SENDT, PAPIRSYKMELDING)
        biter[3].tags `should be equal to` setOf(SYKEPENGESOKNAD, SENDT, BEHANDLINGSDAG)
        biter[3].fom `should be equal to` LocalDate.of(2019, 3, 9)
        biter[3].tom `should be equal to` LocalDate.of(2019, 3, 9)
        biter[4].tags `should be equal to` setOf(SYKEPENGESOKNAD, SENDT, BEHANDLINGSDAG)
        biter[4].fom `should be equal to` LocalDate.of(2019, 3, 4)
        biter[4].tom `should be equal to` LocalDate.of(2019, 3, 4)
    }

    private fun produserPåSøknadTopic(soknad: SykepengesoknadDTO) =
        sendKafkaMelding(soknad.id, soknad.serialisertTilString(), SYKEPENGESOKNAD_TOPIC)
}
