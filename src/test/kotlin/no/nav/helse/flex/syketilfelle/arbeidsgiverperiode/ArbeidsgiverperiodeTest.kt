package no.nav.helse.flex.syketilfelle.arbeidsgiverperiode

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.syketilfelle.Testoppsett
import no.nav.helse.flex.syketilfelle.arbeidsgiverperiode.domain.Arbeidsgiverperiode
import no.nav.helse.flex.syketilfelle.azureToken
import no.nav.helse.flex.syketilfelle.extensions.tilOsloZone
import no.nav.helse.flex.syketilfelle.juridiskvurdering.Utfall
import no.nav.helse.flex.syketilfelle.objectMapper
import no.nav.helse.flex.syketilfelle.syketilfellebit.Syketilfellebit
import no.nav.helse.flex.syketilfelle.syketilfellebit.Tag.*
import no.nav.helse.flex.syketilfelle.syketilfellebit.tilSyketilfellebitDbRecord
import no.nav.helse.flex.syketilfelle.tilJuridiskVurdering
import no.nav.helse.flex.syketilfelle.ventPåRecords
import no.nav.syfo.kafka.felles.*
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should be null`
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class ArbeidsgiverperiodeTest : Testoppsett() {

    private final val fnr = "12345432123"

    @BeforeEach
    @AfterEach
    fun setUp() {
        syketilfellebitRepository.deleteAll()
    }

    fun lagreBitSomRecord(bit: Syketilfellebit) {
        syketilfellebitRepository.save(bit.tilSyketilfellebitDbRecord())
    }

    @Test
    fun `juridisk vurdering publiserers ved utregning av arbeidsgiver perioden når det ikke er en foreløpig beregning`() {
        val soknad = SykepengesoknadDTO(
            id = UUID.randomUUID().toString(),
            arbeidsgiver = ArbeidsgiverDTO(navn = "navn", orgnummer = "orgnummer"),
            fravar = emptyList(),
            andreInntektskilder = emptyList(),
            fom = LocalDate.of(2019, 3, 1),
            tom = LocalDate.of(2019, 3, 16),
            arbeidGjenopptatt = null,
            egenmeldinger = emptyList(),
            fnr = fnr,
            status = SoknadsstatusDTO.SENDT,
            sykmeldingId = UUID.randomUUID().toString(),
            type = SoknadstypeDTO.ARBEIDSTAKERE
        )

        val res = post(soknad = soknad, forelopig = false)!!
        assertThat(res.oppbruktArbeidsgiverperiode).isEqualTo(false)
        assertThat(res.arbeidsgiverPeriode.fom).isEqualTo(soknad.fom)
        assertThat(res.arbeidsgiverPeriode.tom).isEqualTo(soknad.tom)

        val vurdering = juridiskVurderingKafkaConsumer
            .ventPåRecords(antall = 1, duration = Duration.ofSeconds(5))
            .tilJuridiskVurdering()
            .first { it.paragraf == "8-19" }

        vurdering.ledd.`should be null`()
        vurdering.bokstav.`should be null`()
        vurdering.punktum.`should be null`()
        vurdering.kilde `should be equal to` "flex-syketilfelle"
        vurdering.versjonAvKode `should be equal to` "flex-syketilfelle-12432536"

        vurdering.utfall `should be equal to` Utfall.VILKAR_BEREGNET
        vurdering.input `should be equal to` mapOf(
            "soknad" to soknad.id,
            "versjon" to "2022-02-01",
        )
        vurdering.output `should be equal to` mapOf(
            "arbeidsgiverperiode" to mapOf(
                "fom" to "2019-03-01",
                "tom" to "2019-03-16",
            ),
            "oppbruktArbeidsgiverperiode" to false,
            "versjon" to "2022-02-01",
        )
    }

    @Test
    fun brukerInnsendtSoknadsBiterForGrensesetting() {
        lagreBitSomRecord(
            Syketilfellebit(
                fnr = fnr,
                orgnummer = null,
                tags = setOf(SYKMELDING, NY, PERIODE, INGEN_AKTIVITET),
                inntruffet = LocalDateTime.of(2019, 3, 20, 8, 42, 34).tilOsloZone(),
                opprettet = LocalDateTime.of(2019, 3, 20, 8, 42, 34).tilOsloZone(),
                ressursId = "68093d7d-2c6e-4efb-ad9e-f215b2eff152",
                fom = LocalDate.of(2019, 2, 1),
                tom = LocalDate.of(2019, 2, 20)
            )
        )

        lagreBitSomRecord(
            Syketilfellebit(
                fnr = fnr,
                orgnummer = "orgnummer",
                tags = setOf(SYKEPENGESOKNAD, SENDT),
                inntruffet = LocalDateTime.of(2019, 3, 20, 8, 44, 38).tilOsloZone(),
                opprettet = LocalDateTime.of(2019, 3, 20, 8, 44, 38).tilOsloZone(),
                ressursId = "0db8e867-bffa-41fe-8f6c-900729156ac5",
                fom = LocalDate.of(2019, 2, 1),
                tom = LocalDate.of(2019, 2, 20)
            )
        )

        lagreBitSomRecord(
            Syketilfellebit(
                fnr = fnr,
                orgnummer = null,
                tags = setOf(SYKMELDING, NY, PERIODE, INGEN_AKTIVITET),
                inntruffet = LocalDateTime.of(2019, 3, 20, 8, 43, 55).tilOsloZone(),
                opprettet = LocalDateTime.of(2019, 3, 20, 8, 43, 55).tilOsloZone(),
                ressursId = "469637ce-4be2-4c02-b5b0-52a599bd8efc",
                fom = LocalDate.of(2019, 3, 11),
                tom = LocalDate.of(2019, 3, 13)
            )
        )

        lagreBitSomRecord(
            Syketilfellebit(
                fnr = fnr,
                orgnummer = "orgnummer",
                tags = setOf(SYKMELDING, SENDT, PERIODE, INGEN_AKTIVITET),
                inntruffet = LocalDateTime.of(2019, 3, 20, 8, 44, 19).tilOsloZone(),
                opprettet = LocalDateTime.of(2019, 3, 20, 8, 44, 22).tilOsloZone(),
                ressursId = "68093d7d-2c6e-4efb-ad9e-f215b2eff152",
                fom = LocalDate.of(2019, 2, 1),
                tom = LocalDate.of(2019, 2, 20)
            )
        )

        lagreBitSomRecord(
            Syketilfellebit(
                fnr = fnr,
                orgnummer = "orgnummer",
                tags = setOf(SYKMELDING, SENDT, PERIODE, INGEN_AKTIVITET),
                inntruffet = LocalDateTime.of(2019, 3, 20, 8, 45, 16).tilOsloZone(),
                opprettet = LocalDateTime.of(2019, 3, 20, 8, 45, 19).tilOsloZone(),
                ressursId = "469637ce-4be2-4c02-b5b0-52a599bd8efc",
                fom = LocalDate.of(2019, 3, 11),
                tom = LocalDate.of(2019, 3, 13)
            )
        )

        val soknad = SykepengesoknadDTO(
            id = ("469637ce-4be2-4c02-b5b0-52a599bd8efc"),
            arbeidsgiver = (ArbeidsgiverDTO(navn = "navn", orgnummer = ("orgnummer"))),
            andreInntektskilder = (emptyList()),
            fom = (LocalDate.of(2019, 3, 11)),
            tom = (LocalDate.of(2019, 3, 13)),
            arbeidGjenopptatt = (LocalDate.of(2019, 3, 11)),
            egenmeldinger = (emptyList()),
            fnr = fnr,
            status = SoknadsstatusDTO.SENDT,
            type = SoknadstypeDTO.ARBEIDSTAKERE
        )

        post(soknad = soknad, expectNoContent = true)
    }

    @Test
    fun brukerEksisterendeBiterSomSammenhengerMedSoknadenSomSendesInnOmSoknadenIkkeHarSykedager() {
        lagreBitSomRecord(
            Syketilfellebit(
                fnr = fnr,
                orgnummer = null,
                tags = setOf(SYKMELDING, NY, PERIODE, INGEN_AKTIVITET),
                inntruffet = LocalDateTime.of(2019, 3, 20, 8, 42, 34).tilOsloZone(),
                opprettet = LocalDateTime.of(2019, 3, 20, 8, 42, 34).tilOsloZone(),
                ressursId = "68093d7d-2c6e-4efb-ad9e-f215b2eff152",
                fom = LocalDate.of(2019, 2, 1),
                tom = LocalDate.of(2019, 3, 6)
            )
        )

        lagreBitSomRecord(
            Syketilfellebit(
                fnr = fnr,
                orgnummer = "orgnummer",
                tags = setOf(SYKMELDING, SENDT, PERIODE, INGEN_AKTIVITET),
                inntruffet = LocalDateTime.of(2019, 3, 20, 8, 44, 19).tilOsloZone(),
                opprettet = LocalDateTime.of(2019, 3, 20, 8, 44, 22).tilOsloZone(),
                ressursId = "68093d7d-2c6e-4efb-ad9e-f215b2eff152",
                fom = LocalDate.of(2019, 2, 1),
                tom = LocalDate.of(2019, 3, 6)
            )
        )

        lagreBitSomRecord(
            Syketilfellebit(
                fnr = fnr,
                orgnummer = "orgnummer",
                tags = setOf(SYKEPENGESOKNAD, SENDT),
                inntruffet = LocalDateTime.of(2019, 3, 20, 8, 44, 38).tilOsloZone(),
                opprettet = LocalDateTime.of(2019, 3, 20, 8, 44, 38).tilOsloZone(),
                ressursId = "0db8e867-bffa-41fe-8f6c-900729156ac5",
                fom = LocalDate.of(2019, 2, 1),
                tom = LocalDate.of(2019, 3, 6)
            )
        )

        lagreBitSomRecord(
            Syketilfellebit(
                fnr = fnr,
                orgnummer = null,
                tags = setOf(SYKMELDING, NY, PERIODE, INGEN_AKTIVITET),
                inntruffet = LocalDateTime.of(2019, 3, 20, 8, 43, 55).tilOsloZone(),
                opprettet = LocalDateTime.of(2019, 3, 20, 8, 43, 55).tilOsloZone(),
                ressursId = "469637ce-4be2-4c02-b5b0-52a599bd8efc",
                fom = LocalDate.of(2019, 3, 11),
                tom = LocalDate.of(2019, 3, 13)
            )
        )

        lagreBitSomRecord(
            Syketilfellebit(
                fnr = fnr,
                orgnummer = "orgnummer",
                tags = setOf(SYKMELDING, SENDT, PERIODE, INGEN_AKTIVITET),
                inntruffet = LocalDateTime.of(2019, 3, 20, 8, 45, 16).tilOsloZone(),
                opprettet = LocalDateTime.of(2019, 3, 20, 8, 45, 19).tilOsloZone(),
                ressursId = "469637ce-4be2-4c02-b5b0-52a599bd8efc",
                fom = LocalDate.of(2019, 3, 11),
                tom = LocalDate.of(2019, 3, 13)
            )
        )

        val soknad = SykepengesoknadDTO(
            id = ("469637ce-4be2-4c02-b5b0-52a599bd8efc"),
            arbeidsgiver = (ArbeidsgiverDTO(navn = "navn", orgnummer = ("orgnummer"))),
            andreInntektskilder = (emptyList()),
            fom = (LocalDate.of(2019, 3, 11)),
            tom = (LocalDate.of(2019, 3, 13)),
            arbeidGjenopptatt = (LocalDate.of(2019, 3, 11)),
            egenmeldinger = (emptyList()),
            fnr = fnr,
            status = SoknadsstatusDTO.SENDT,
            sykmeldingId = UUID.randomUUID().toString(),
            type = SoknadstypeDTO.ARBEIDSTAKERE
        )

        val res = post(soknad = soknad)
        assertThat(res?.arbeidsgiverPeriode?.fom).isEqualTo(LocalDate.of(2019, 2, 1))
        assertThat(res?.arbeidsgiverPeriode?.tom).isEqualTo(LocalDate.of(2019, 2, 16))
    }

    @Test
    fun nyArbeidsgiverperiodeMedArbeidgjenopptatt() {
        lagreBitSomRecord(
            Syketilfellebit(
                fnr = fnr,
                orgnummer = null,
                tags = setOf(SYKMELDING, NY, PERIODE, INGEN_AKTIVITET),
                inntruffet = LocalDateTime.of(2019, 3, 20, 8, 42, 34).tilOsloZone(),
                opprettet = LocalDateTime.of(2019, 3, 20, 8, 42, 34).tilOsloZone(),
                ressursId = "68093d7d-2c6e-4efb-ad9e-f215b2eff152",
                fom = LocalDate.of(2019, 2, 1),
                tom = LocalDate.of(2019, 2, 20)
            )
        )

        lagreBitSomRecord(
            Syketilfellebit(
                fnr = fnr,
                orgnummer = "orgnummer",
                tags = setOf(SYKEPENGESOKNAD, SENDT),
                inntruffet = LocalDateTime.of(2019, 3, 20, 8, 44, 38).tilOsloZone(),
                opprettet = LocalDateTime.of(2019, 3, 20, 8, 44, 38).tilOsloZone(),
                ressursId = "0db8e867-bffa-41fe-8f6c-900729156ac5",
                fom = LocalDate.of(2019, 2, 1),
                tom = LocalDate.of(2019, 2, 20)
            )
        )

        lagreBitSomRecord(
            Syketilfellebit(
                fnr = fnr,
                orgnummer = null,
                tags = setOf(SYKMELDING, NY, PERIODE, INGEN_AKTIVITET),
                inntruffet = LocalDateTime.of(2019, 3, 20, 8, 43, 55).tilOsloZone(),
                opprettet = LocalDateTime.of(2019, 3, 20, 8, 43, 55).tilOsloZone(),
                ressursId = "469637ce-4be2-4c02-b5b0-52a599bd8efc",
                fom = LocalDate.of(2019, 3, 11),
                tom = LocalDate.of(2019, 3, 13)
            )
        )

        lagreBitSomRecord(
            Syketilfellebit(
                fnr = fnr,
                orgnummer = "orgnummer",
                tags = setOf(SYKMELDING, SENDT, PERIODE, INGEN_AKTIVITET),
                inntruffet = LocalDateTime.of(2019, 3, 20, 8, 44, 19).tilOsloZone(),
                opprettet = LocalDateTime.of(2019, 3, 20, 8, 44, 22).tilOsloZone(),
                ressursId = "68093d7d-2c6e-4efb-ad9e-f215b2eff152",
                fom = LocalDate.of(2019, 2, 1),
                tom = LocalDate.of(2019, 2, 20)
            )
        )

        lagreBitSomRecord(
            Syketilfellebit(
                fnr = fnr,
                orgnummer = "orgnummer",
                tags = setOf(SYKMELDING, SENDT, PERIODE, INGEN_AKTIVITET),
                inntruffet = LocalDateTime.of(2019, 3, 20, 8, 45, 16).tilOsloZone(),
                opprettet = LocalDateTime.of(2019, 3, 20, 8, 45, 19).tilOsloZone(),
                ressursId = "469637ce-4be2-4c02-b5b0-52a599bd8efc",
                fom = LocalDate.of(2019, 3, 11),
                tom = LocalDate.of(2019, 3, 13)
            )
        )

        val soknad = SykepengesoknadDTO(
            id = ("469637ce-4be2-4c02-b5b0-52a599bd8efc"),
            arbeidsgiver = (ArbeidsgiverDTO(navn = "navn", orgnummer = ("orgnummer"))),
            andreInntektskilder = (emptyList()),
            fom = (LocalDate.of(2019, 3, 11)),
            tom = (LocalDate.of(2019, 3, 13)),
            arbeidGjenopptatt = (LocalDate.of(2019, 3, 12)),
            egenmeldinger = (emptyList()),
            fnr = fnr,
            status = SoknadsstatusDTO.SENDT,
            sykmeldingId = UUID.randomUUID().toString(),
            type = SoknadstypeDTO.ARBEIDSTAKERE
        )

        val res = post(soknad = soknad)
        assertThat(res?.arbeidsgiverPeriode?.fom).isEqualTo(LocalDate.of(2019, 3, 11))
        assertThat(res?.arbeidsgiverPeriode?.tom).isEqualTo(LocalDate.of(2019, 3, 11))
    }

    @Test
    fun ikkeNyArbeidsgiverperiodeMedEgenmeldingerSomBinderSammenToPerioder() {
        lagreBitSomRecord(
            Syketilfellebit(
                fnr = fnr,
                orgnummer = null,
                tags = setOf(SYKMELDING, NY, PERIODE, INGEN_AKTIVITET),
                inntruffet = LocalDateTime.of(2019, 3, 20, 8, 42, 34).tilOsloZone(),
                opprettet = LocalDateTime.of(2019, 3, 20, 8, 42, 34).tilOsloZone(),
                ressursId = "68093d7d-2c6e-4efb-ad9e-f215b2eff152",
                fom = LocalDate.of(2019, 2, 1),
                tom = LocalDate.of(2019, 2, 20)
            )
        )

        lagreBitSomRecord(
            Syketilfellebit(
                fnr = fnr,
                orgnummer = "orgnummer",
                tags = setOf(SYKEPENGESOKNAD, SENDT),
                inntruffet = LocalDateTime.of(2019, 3, 20, 8, 44, 38).tilOsloZone(),
                opprettet = LocalDateTime.of(2019, 3, 20, 8, 44, 38).tilOsloZone(),
                ressursId = "0db8e867-bffa-41fe-8f6c-900729156ac5",
                fom = LocalDate.of(2019, 2, 1),
                tom = LocalDate.of(2019, 2, 20)
            )
        )

        lagreBitSomRecord(
            Syketilfellebit(
                fnr = fnr,
                orgnummer = null,
                tags = setOf(SYKMELDING, NY, PERIODE, INGEN_AKTIVITET),
                inntruffet = LocalDateTime.of(2019, 3, 20, 8, 43, 55).tilOsloZone(),
                opprettet = LocalDateTime.of(2019, 3, 20, 8, 43, 55).tilOsloZone(),
                ressursId = "469637ce-4be2-4c02-b5b0-52a599bd8efc",
                fom = LocalDate.of(2019, 3, 11),
                tom = LocalDate.of(2019, 3, 13)
            )
        )

        lagreBitSomRecord(
            Syketilfellebit(
                fnr = fnr,
                orgnummer = "orgnummer",
                tags = setOf(SYKMELDING, SENDT, PERIODE, INGEN_AKTIVITET),
                inntruffet = LocalDateTime.of(2019, 3, 20, 8, 44, 19).tilOsloZone(),
                opprettet = LocalDateTime.of(2019, 3, 20, 8, 44, 22).tilOsloZone(),
                ressursId = "68093d7d-2c6e-4efb-ad9e-f215b2eff152",
                fom = LocalDate.of(2019, 2, 1),
                tom = LocalDate.of(2019, 2, 20)
            )
        )

        lagreBitSomRecord(
            Syketilfellebit(
                fnr = fnr,
                orgnummer = "orgnummer",
                tags = setOf(SYKMELDING, SENDT, PERIODE, INGEN_AKTIVITET),
                inntruffet = LocalDateTime.of(2019, 3, 20, 8, 45, 16).tilOsloZone(),
                opprettet = LocalDateTime.of(2019, 3, 20, 8, 45, 19).tilOsloZone(),
                ressursId = "469637ce-4be2-4c02-b5b0-52a599bd8efc",
                fom = LocalDate.of(2019, 3, 11),
                tom = LocalDate.of(2019, 3, 13)
            )
        )

        val soknad = SykepengesoknadDTO(
            id = ("469637ce-4be2-4c02-b5b0-52a599bd8efc"),
            arbeidsgiver = (ArbeidsgiverDTO(navn = "navn", orgnummer = ("orgnummer"))),
            andreInntektskilder = (emptyList()),
            fom = (LocalDate.of(2019, 3, 11)),
            tom = (LocalDate.of(2019, 3, 13)),
            arbeidGjenopptatt = (null),
            egenmeldinger = (
                listOf(
                    PeriodeDTO(
                        fom = (LocalDate.of(2019, 3, 1)),
                        tom = (LocalDate.of(2019, 3, 10))
                    )
                )
                ),
            fnr = fnr,
            status = SoknadsstatusDTO.SENDT,
            sykmeldingId = UUID.randomUUID().toString(),
            type = SoknadstypeDTO.ARBEIDSTAKERE
        )

        val res = post(soknad = soknad)
        assertThat(res?.arbeidsgiverPeriode?.fom).isEqualTo(LocalDate.of(2019, 2, 1))
        assertThat(res?.arbeidsgiverPeriode?.tom).isEqualTo(LocalDate.of(2019, 2, 16))
    }

    @Test
    fun `andregangs ferie innsending skal sendes til NAV`() {
        lagreBitSomRecord(
            Syketilfellebit(
                fnr = fnr,
                orgnummer = null,
                tags = setOf(SYKMELDING, NY, PERIODE, INGEN_AKTIVITET),
                inntruffet = LocalDateTime.of(2019, 2, 1, 8, 42, 34).tilOsloZone(),
                opprettet = LocalDateTime.of(2019, 2, 1, 8, 42, 34).tilOsloZone(),
                ressursId = "68093d7d-2c6e-4efb-ad9e-f215b2eff152",
                fom = LocalDate.of(2019, 2, 1),
                tom = LocalDate.of(2019, 2, 20)
            )
        )

        lagreBitSomRecord(
            Syketilfellebit(
                fnr = fnr,
                orgnummer = "orgnummer",
                tags = setOf(SYKEPENGESOKNAD, SENDT, FERIE),
                inntruffet = LocalDateTime.of(2019, 2, 1, 8, 42, 34).tilOsloZone(),
                opprettet = LocalDateTime.of(2019, 3, 20, 8, 44, 38).tilOsloZone(),
                ressursId = "0db8e867-bffa-41fe-8f6c-900729156ac5",
                fom = LocalDate.of(2019, 2, 21),
                tom = LocalDate.of(2019, 3, 10)
            )
        )

        val soknad = SykepengesoknadDTO(
            id = ("469637ce-4be2-4c02-b5b0-52a599bd8efc"),
            arbeidsgiver = (ArbeidsgiverDTO(navn = "navn", orgnummer = ("orgnummer"))),
            fravar = (
                listOf(
                    FravarDTO
                    (
                        type = (FravarstypeDTO.FERIE),
                        fom = (LocalDate.of(2019, 3, 11)),
                        tom = (LocalDate.of(2019, 3, 27))
                    )
                )
                ),
            andreInntektskilder = (emptyList()),
            fom = (LocalDate.of(2019, 3, 11)),
            tom = (LocalDate.of(2019, 3, 27)),
            arbeidGjenopptatt = (null),
            egenmeldinger = (emptyList()),
            fnr = fnr,
            status = SoknadsstatusDTO.SENDT,
            sykmeldingId = UUID.randomUUID().toString(),
            type = SoknadstypeDTO.ARBEIDSTAKERE
        )

        val res = post(soknad = soknad)
        assertThat(res?.oppbruktArbeidsgiverperiode).isEqualTo(true)
        assertThat(res?.arbeidsgiverPeriode?.fom).isEqualTo(LocalDate.of(2019, 2, 1))
        assertThat(res?.arbeidsgiverPeriode?.tom).isEqualTo(LocalDate.of(2019, 2, 16))
    }

    @Test
    fun `førstegangssøknad på 17 dager har brukt opp arbeidsgiverperioden`() {

        val soknad = SykepengesoknadDTO(
            id = ("469637ce-4be2-4c02-b5b0-52a599bd8efc"),
            arbeidsgiver = (ArbeidsgiverDTO(navn = "navn", orgnummer = ("orgnummer"))),
            fravar = (emptyList()),
            andreInntektskilder = (emptyList()),
            fom = (LocalDate.of(2019, 3, 1)),
            tom = (LocalDate.of(2019, 3, 17)),
            arbeidGjenopptatt = (null),
            egenmeldinger = (emptyList()),
            fnr = fnr,
            sykmeldingId = UUID.randomUUID().toString(),
            status = SoknadsstatusDTO.SENDT,
            type = SoknadstypeDTO.ARBEIDSTAKERE
        )

        val res = post(soknad = soknad)
        assertThat(res?.oppbruktArbeidsgiverperiode).isEqualTo(true)
        assertThat(res?.arbeidsgiverPeriode?.fom).isEqualTo(soknad.fom)
        assertThat(res?.arbeidsgiverPeriode?.tom).isEqualTo(soknad.tom!!.minusDays(1))
    }

    @Test
    fun `førstegangssøknad på 16 dager har ikke brukt opp arbeidsgiverperioden`() {
        val soknad = SykepengesoknadDTO(
            id = ("469637ce-4be2-4c02-b5b0-52a599bd8efc"),
            arbeidsgiver = (ArbeidsgiverDTO(navn = "navn", orgnummer = ("orgnummer"))),
            fravar = (emptyList()),
            andreInntektskilder = (emptyList()),
            fom = (LocalDate.of(2019, 3, 1)),
            tom = (LocalDate.of(2019, 3, 16)),
            arbeidGjenopptatt = (null),
            egenmeldinger = (emptyList()),
            fnr = fnr,
            status = SoknadsstatusDTO.SENDT,
            sykmeldingId = UUID.randomUUID().toString(),
            type = SoknadstypeDTO.ARBEIDSTAKERE
        )

        val res = post(soknad = soknad)
        assertThat(res?.oppbruktArbeidsgiverperiode).isEqualTo(false)
        assertThat(res?.arbeidsgiverPeriode?.fom).isEqualTo(soknad.fom)
        assertThat(res?.arbeidsgiverPeriode?.tom).isEqualTo(soknad.tom)
    }

    @Test
    fun `førstegangssøknad på 15 dager og etterfølgende søknad på 3 dager bruker opp arbeidsgiverperioden`() {

        lagreBitSomRecord(
            Syketilfellebit(
                fnr = fnr,
                orgnummer = null,
                tags = setOf(SYKMELDING, NY, PERIODE, INGEN_AKTIVITET),
                inntruffet = LocalDateTime.of(2019, 3, 1, 8, 42, 34).tilOsloZone(),
                opprettet = LocalDateTime.of(2019, 3, 1, 8, 42, 34).tilOsloZone(),
                ressursId = "68093d7d-2c6e-4efb-ad9e-f215b2eff152",
                fom = LocalDate.of(2019, 3, 1),
                tom = LocalDate.of(2019, 3, 15)
            )
        )

        val soknad = SykepengesoknadDTO(
            id = ("469637ce-4be2-4c02-b5b0-52a599bd8efd"),
            arbeidsgiver = (ArbeidsgiverDTO(navn = "navn", orgnummer = ("orgnummer"))),
            fravar = (emptyList()),
            andreInntektskilder = (emptyList()),
            fom = (LocalDate.of(2019, 3, 16)),
            tom = (LocalDate.of(2019, 3, 19)),
            arbeidGjenopptatt = (null),
            egenmeldinger = (emptyList()),
            fnr = fnr,
            status = SoknadsstatusDTO.SENDT,
            sykmeldingId = UUID.randomUUID().toString(),
            type = SoknadstypeDTO.ARBEIDSTAKERE
        )

        val res = post(soknad = soknad)
        assertThat(res?.oppbruktArbeidsgiverperiode).isEqualTo(true)
        assertThat(res?.arbeidsgiverPeriode?.fom).isEqualTo(LocalDate.of(2019, 3, 1))
        assertThat(res?.arbeidsgiverPeriode?.tom).isEqualTo(LocalDate.of(2019, 3, 16))
    }

    @Test
    fun `syketilfelle etter ferie etter hvor personen var en dag på jobb før ferien er ny arbeidsgiverperiode`() {
        lagreBitSomRecord(
            Syketilfellebit(
                fnr = fnr,
                orgnummer = null,
                tags = setOf(SYKMELDING, NY, PERIODE, INGEN_AKTIVITET),
                inntruffet = LocalDateTime.of(2019, 2, 1, 8, 42, 34).tilOsloZone(),
                opprettet = LocalDateTime.of(2019, 2, 1, 8, 42, 34).tilOsloZone(),
                ressursId = "68093d7d-2c6e-4efb-ad9e-f215b2eff152",
                fom = LocalDate.of(2019, 2, 1),
                tom = LocalDate.of(2019, 2, 20)
            )
        )

        lagreBitSomRecord(
            Syketilfellebit(
                fnr = fnr,
                orgnummer = null,
                tags = setOf(SYKMELDING, PERIODE, FULL_AKTIVITET),
                inntruffet = LocalDateTime.of(2019, 2, 1, 8, 42, 34).tilOsloZone(),
                opprettet = LocalDateTime.of(2019, 2, 1, 8, 42, 34).tilOsloZone(),
                ressursId = "68093d7d-2c6e-4efb-ad9e-f215b2eff153",
                fom = LocalDate.of(2019, 2, 21),
                tom = LocalDate.of(2019, 2, 21)
            )
        )

        lagreBitSomRecord(
            Syketilfellebit(
                fnr = fnr,
                orgnummer = "orgnummer",
                tags = setOf(SYKEPENGESOKNAD, SENDT, FERIE),
                inntruffet = LocalDateTime.of(2019, 2, 1, 8, 42, 34).tilOsloZone(),
                opprettet = LocalDateTime.of(2019, 3, 20, 8, 44, 38).tilOsloZone(),
                ressursId = "0db8e867-bffa-41fe-8f6c-900729156ac5",
                fom = LocalDate.of(2019, 2, 22),
                tom = LocalDate.of(2019, 3, 15)
            )
        )

        val soknad = SykepengesoknadDTO(
            id = ("469637ce-4be2-4c02-b5b0-52a599bd8efd"),
            arbeidsgiver = (ArbeidsgiverDTO(navn = ("navn"), orgnummer = ("orgnummer"))),
            fravar = (emptyList()),
            andreInntektskilder = (emptyList()),
            fom = (LocalDate.of(2019, 3, 16)),
            tom = (LocalDate.of(2019, 3, 18)),
            arbeidGjenopptatt = (null),
            egenmeldinger = (emptyList()),
            fnr = fnr,
            status = SoknadsstatusDTO.SENDT,
            sykmeldingId = UUID.randomUUID().toString(),
            type = SoknadstypeDTO.ARBEIDSTAKERE
        )

        val res = post(soknad = soknad)
        assertThat(res?.oppbruktArbeidsgiverperiode).isEqualTo(false)
        assertThat(res?.arbeidsgiverPeriode?.fom).isEqualTo(LocalDate.of(2019, 3, 16))
        assertThat(res?.arbeidsgiverPeriode?.tom).isEqualTo(LocalDate.of(2019, 3, 18))
    }

    @Test
    fun `syketilfelle etter ferie etter hvor personen var en dag på jobb etter ferien er ikke ny arbeidsgiverperiode`() {
        lagreBitSomRecord(
            Syketilfellebit(
                fnr = fnr,
                orgnummer = null,
                tags = setOf(SYKMELDING, NY, PERIODE, INGEN_AKTIVITET),
                inntruffet = LocalDateTime.of(2019, 2, 1, 8, 42, 34).tilOsloZone(),
                opprettet = LocalDateTime.of(2019, 2, 1, 8, 42, 34).tilOsloZone(),
                ressursId = "68093d7d-2c6e-4efb-ad9e-f215b2eff152",
                fom = LocalDate.of(2019, 2, 1),
                tom = LocalDate.of(2019, 2, 20)
            )
        )

        lagreBitSomRecord(
            Syketilfellebit(
                fnr = fnr,
                orgnummer = "orgnummer",
                tags = setOf(SYKEPENGESOKNAD, SENDT, FERIE),
                inntruffet = LocalDateTime.of(2019, 2, 1, 8, 42, 34).tilOsloZone(),
                opprettet = LocalDateTime.of(2019, 3, 20, 8, 44, 38).tilOsloZone(),
                ressursId = "0db8e867-bffa-41fe-8f6c-900729156ac5",
                fom = LocalDate.of(2019, 2, 21),
                tom = LocalDate.of(2019, 3, 14)
            )
        )

        lagreBitSomRecord(
            Syketilfellebit(
                fnr = fnr,
                orgnummer = null,
                tags = setOf(SYKMELDING, PERIODE, FULL_AKTIVITET),
                inntruffet = LocalDateTime.of(2019, 2, 1, 8, 42, 34).tilOsloZone(),
                opprettet = LocalDateTime.of(2019, 2, 1, 8, 42, 34).tilOsloZone(),
                ressursId = "68093d7d-2c6e-4efb-ad9e-f215b2eff153",
                fom = LocalDate.of(2019, 3, 15),
                tom = LocalDate.of(2019, 3, 15)
            )
        )

        val soknad = SykepengesoknadDTO(
            id = ("469637ce-4be2-4c02-b5b0-52a599bd8efd"),
            arbeidsgiver = (ArbeidsgiverDTO(navn = "navn", orgnummer = ("orgnummer"))),
            fravar = (emptyList()),
            andreInntektskilder = (emptyList()),
            fom = (LocalDate.of(2019, 3, 16)),
            tom = (LocalDate.of(2019, 3, 18)),
            arbeidGjenopptatt = (null),
            egenmeldinger = (emptyList()),
            fnr = fnr,
            status = SoknadsstatusDTO.SENDT,
            sykmeldingId = UUID.randomUUID().toString(),
            type = SoknadstypeDTO.ARBEIDSTAKERE
        )

        val res = post(soknad = soknad)

        assertThat(res?.oppbruktArbeidsgiverperiode).isEqualTo(true)
        assertThat(res?.arbeidsgiverPeriode?.fom).isEqualTo(LocalDate.of(2019, 2, 1))
        assertThat(res?.arbeidsgiverPeriode?.tom).isEqualTo(LocalDate.of(2019, 2, 16))
    }

    @Test
    fun `digital sykemelding etter papirsykemelding hvor den digitale har markert start på syketilfelle`() {
        lagreBitSomRecord(
            Syketilfellebit(
                fnr = fnr,
                orgnummer = null,
                tags = setOf(SYKMELDING, NY, PERIODE, INGEN_AKTIVITET),
                inntruffet = LocalDateTime.of(2019, 2, 1, 8, 42, 34).tilOsloZone(),
                opprettet = LocalDateTime.of(2019, 2, 1, 8, 42, 34).tilOsloZone(),
                ressursId = "68093d7d-2c6e-4efb-ad9e-f215b2eff152",
                fom = LocalDate.of(2019, 2, 10),
                tom = LocalDate.of(2019, 2, 23)
            )
        )

        val soknad = SykepengesoknadDTO(
            id = ("469637ce-4be2-4c02-b5b0-52a599bd8efd"),
            arbeidsgiver = (ArbeidsgiverDTO(navn = "navn", orgnummer = ("orgnummer"))),
            fravar = (emptyList()),
            startSyketilfelle = (LocalDate.of(2019, 2, 5)),
            andreInntektskilder = (emptyList()),
            fom = (LocalDate.of(2019, 2, 10)),
            tom = (LocalDate.of(2019, 2, 23)),
            arbeidGjenopptatt = (null),
            egenmeldinger = (emptyList()),
            fnr = fnr,
            status = SoknadsstatusDTO.SENDT,
            type = SoknadstypeDTO.ARBEIDSTAKERE,
            sykmeldingId = UUID.randomUUID().toString(),
        )

        val res = post(soknad = soknad)

        assertThat(res?.oppbruktArbeidsgiverperiode).isEqualTo(true)
        assertThat(res?.arbeidsgiverPeriode?.fom).isEqualTo(LocalDate.of(2019, 2, 5))
        assertThat(res?.arbeidsgiverPeriode?.tom).isEqualTo(LocalDate.of(2019, 2, 20))
    }

    @Test
    fun sisteBehandlingsdagErSisteDagISoknadTest() {
        lagreBitSomRecord(
            Syketilfellebit(
                fnr = fnr,
                orgnummer = null,
                tags = setOf(SYKMELDING, NY, PERIODE, GRADERT_AKTIVITET),
                inntruffet = LocalDateTime.of(2019, 3, 20, 8, 42, 34).tilOsloZone(),
                opprettet = LocalDateTime.of(2019, 3, 20, 8, 42, 34).tilOsloZone(),
                ressursId = "68093d7d-2c6e-4efb-ad9e-f215b2eff152",
                fom = LocalDate.of(2020, 1, 27),
                tom = LocalDate.of(2020, 2, 16)
            )
        )

        lagreBitSomRecord(
            Syketilfellebit(
                fnr = fnr,
                orgnummer = "995816598",
                tags = setOf(SYKMELDING, SENDT, PERIODE, GRADERT_AKTIVITET),
                inntruffet = LocalDateTime.of(2019, 3, 20, 8, 42, 35).tilOsloZone(),
                opprettet = LocalDateTime.of(2019, 3, 20, 8, 42, 35).tilOsloZone(),
                ressursId = "68093d7d-2c6e-4efb-ad9e-f215b2eff152",
                fom = LocalDate.of(2020, 1, 27),
                tom = LocalDate.of(2020, 2, 16)
            )
        )

        lagreBitSomRecord(
            Syketilfellebit(
                fnr = fnr,
                orgnummer = null,
                tags = setOf(SYKMELDING, NY, PERIODE, BEHANDLINGSDAGER),
                inntruffet = LocalDateTime.of(2019, 3, 20, 8, 42, 36).tilOsloZone(),
                opprettet = LocalDateTime.of(2019, 3, 20, 8, 42, 36).tilOsloZone(),
                ressursId = "68093d7d-2c6e-4efb-ad9e-f215b2eff153",
                fom = LocalDate.of(2020, 2, 21),
                tom = LocalDate.of(2020, 3, 20)
            )
        )

        lagreBitSomRecord(
            Syketilfellebit(
                fnr = fnr,
                orgnummer = "995816598",
                tags = setOf(SYKMELDING, SENDT, PERIODE, BEHANDLINGSDAGER),
                inntruffet = LocalDateTime.of(2019, 3, 20, 8, 42, 37).tilOsloZone(),
                opprettet = LocalDateTime.of(2019, 3, 20, 8, 42, 37).tilOsloZone(),
                ressursId = "68093d7d-2c6e-4efb-ad9e-f215b2eff153",
                fom = LocalDate.of(2020, 2, 21),
                tom = LocalDate.of(2020, 3, 20)
            )
        )

        lagreBitSomRecord(
            Syketilfellebit(
                fnr = fnr,
                orgnummer = "995816598",
                tags = setOf(SYKEPENGESOKNAD, SENDT),
                inntruffet = LocalDateTime.of(2019, 3, 20, 8, 42, 38).tilOsloZone(),
                opprettet = LocalDateTime.of(2019, 3, 20, 8, 42, 38).tilOsloZone(),
                ressursId = "68093d7d-2c6e-4efb-ad9e-f215b2eff154",
                fom = LocalDate.of(2020, 1, 27),
                tom = LocalDate.of(2020, 2, 16)
            )
        )

        lagreBitSomRecord(
            Syketilfellebit(
                fnr = fnr,
                orgnummer = "995816598",
                tags = setOf(SYKEPENGESOKNAD, SENDT, BEHANDLINGSDAGER),
                inntruffet = LocalDateTime.of(2019, 3, 20, 8, 42, 37).tilOsloZone(),
                opprettet = LocalDateTime.of(2019, 3, 20, 8, 42, 37).tilOsloZone(),
                ressursId = "68093d7d-2c6e-4efb-ad9e-f215b2eff155",
                fom = LocalDate.of(2020, 2, 21),
                tom = LocalDate.of(2020, 3, 8)
            )
        )

        lagreBitSomRecord(
            Syketilfellebit(
                fnr = fnr,
                orgnummer = "995816598",
                tags = setOf(SYKEPENGESOKNAD, SENDT, BEHANDLINGSDAG),
                inntruffet = LocalDateTime.of(2019, 3, 20, 8, 42, 37).tilOsloZone(),
                opprettet = LocalDateTime.of(2019, 3, 20, 8, 42, 37).tilOsloZone(),
                ressursId = "68093d7d-2c6e-4efb-ad9e-f215b2eff155",
                fom = LocalDate.of(2020, 2, 21),
                tom = LocalDate.of(2020, 2, 21)
            )
        )

        lagreBitSomRecord(
            Syketilfellebit(
                fnr = fnr,
                orgnummer = "995816598",
                tags = setOf(SYKEPENGESOKNAD, SENDT, BEHANDLINGSDAG),
                inntruffet = LocalDateTime.of(2019, 3, 20, 8, 42, 37).tilOsloZone(),
                opprettet = LocalDateTime.of(2019, 3, 20, 8, 42, 37).tilOsloZone(),
                ressursId = "68093d7d-2c6e-4efb-ad9e-f215b2eff155",
                fom = LocalDate.of(2020, 2, 28),
                tom = LocalDate.of(2020, 2, 28)
            )
        )

        lagreBitSomRecord(
            Syketilfellebit(
                fnr = fnr,
                orgnummer = "995816598",
                tags = setOf(SYKEPENGESOKNAD, SENDT, BEHANDLINGSDAG),
                inntruffet = LocalDateTime.of(2019, 3, 20, 8, 42, 37).tilOsloZone(),
                opprettet = LocalDateTime.of(2019, 3, 20, 8, 42, 37).tilOsloZone(),
                ressursId = "68093d7d-2c6e-4efb-ad9e-f215b2eff155",
                fom = LocalDate.of(2020, 3, 6),
                tom = LocalDate.of(2020, 3, 6)
            )
        )

        val soknad = SykepengesoknadDTO(
            id = ("469637ce-4be2-4c02-b5b0-52a599bd8efc"),
            type = SoknadstypeDTO.BEHANDLINGSDAGER,
            arbeidsgiver = (ArbeidsgiverDTO(navn = "navn", orgnummer = ("orgnummer"))),
            andreInntektskilder = (emptyList()),
            behandlingsdager = listOf(
                LocalDate.of(2020, 3, 13),
                LocalDate.of(2020, 3, 20)
            ),
            tom = (LocalDate.of(2020, 3, 20)),
            fom = (LocalDate.of(2020, 3, 9)),
            egenmeldinger = (emptyList()),
            fnr = fnr,
            sykmeldingId = UUID.randomUUID().toString(),
            status = SoknadsstatusDTO.SENDT,
        )

        val res = post(soknad = soknad)
        assertThat(res?.antallBrukteDager).isEqualTo(26)
        assertThat(res?.oppbruktArbeidsgiverperiode).isEqualTo(true)
        assertThat(res?.arbeidsgiverPeriode?.fom).isEqualTo(LocalDate.of(2020, 1, 27))
        assertThat(res?.arbeidsgiverPeriode?.tom).isEqualTo(LocalDate.of(2020, 2, 11))
    }

    private fun post(
        soknad: SykepengesoknadDTO,
        expectNoContent: Boolean = false,
        forelopig: Boolean = true
    ): Arbeidsgiverperiode? {
        val result = mockMvc.perform(
            MockMvcRequestBuilders.post("/api/v1/arbeidsgiverperiode")
                .header("Authorization", "Bearer ${server.azureToken(subject = "syfosoknad-client-id")}")
                .header("fnr", fnr)
                .header("forelopig", forelopig.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(soknad))
        )
            .andExpect(MockMvcResultMatchers.status().is2xxSuccessful)
            .andExpect(
                if (expectNoContent) MockMvcResultMatchers.status().isNoContent else
                    MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
            )
            .andReturn()
        return result.response.contentAsString.takeIf { it.isNotBlank() }
            ?.let { objectMapper.readValue(it) }
    }
}
