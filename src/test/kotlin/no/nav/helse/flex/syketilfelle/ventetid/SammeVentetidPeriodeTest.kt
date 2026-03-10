package no.nav.helse.flex.syketilfelle.ventetid

import no.nav.helse.flex.syketilfelle.FellesTestOppsett
import no.nav.helse.flex.syketilfelle.syketilfellebit.Tag
import no.nav.helse.flex.syketilfelle.sykmelding.SykmeldingLagring
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.Month
import java.util.UUID

class SammeVentetidPeriodeTest :
    FellesTestOppsett(),
    VentetidFellesOppsett {
    @Autowired
    override lateinit var sykmeldingLagring: SykmeldingLagring

    @Autowired
    private lateinit var ventetidUtregner: VentetidUtregner

    @BeforeEach
    @AfterEach
    fun setUp() {
        syketilfellebitRepository.deleteAll()
    }

    final override val fnr = "11111111111"

    @Test
    fun `Sykmelding som ikke er SENDT eller BEKREFTET tas også med i beregningen`() {
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
    fun `Sykmelding med tags REISETILSKUDD eller AVVENTENDE tas ikke med i beregningen`() {
        val sykmelding1 = UUID.randomUUID().toString()
        val sykmelding2 = UUID.randomUUID().toString()
        val sykmeldingReisetilskudd = UUID.randomUUID().toString()

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
                ressursId = sykmeldingReisetilskudd,
                fom = LocalDate.of(2026, Month.JANUARY, 20),
                tom = LocalDate.of(2026, Month.FEBRUARY, 1),
                tags = listOf(Tag.SYKMELDING, Tag.BEKREFTET, Tag.PERIODE, Tag.REISETILSKUDD, Tag.INGEN_AKTIVITET),
            ),
            lagSyketilfelleBit(
                fnr = fnr,
                ressursId = sykmeldingReisetilskudd,
                fom = LocalDate.of(2026, Month.JANUARY, 20),
                tom = LocalDate.of(2026, Month.FEBRUARY, 1),
                tags = listOf(Tag.SYKMELDING, Tag.BEKREFTET, Tag.PERIODE, Tag.AVVENTENDE, Tag.INGEN_AKTIVITET),
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
            ventetidperioder.none { it.ressursId == sykmeldingReisetilskudd } `should be` true
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

        val sykmeldingKafkaMessage =
            skapSykmeldingKafkaMessage(
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
    fun `Sykmelding både lagret og sendt med i request tas med i beregningen`() {
        val sykmelding1 = UUID.randomUUID().toString()
        val sykmelding2 = UUID.randomUUID().toString()

        val sykmeldingKafkaMessage =
            skapSykmeldingKafkaMessage(
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
}
