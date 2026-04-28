package no.nav.helse.flex.syketilfelle.ventetid

import no.nav.helse.flex.syketilfelle.FellesTestOppsett
import no.nav.helse.flex.syketilfelle.lagMottattSykmeldingKafkaMessage
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.Month

/**
 * Validerer at ventetid blir beregnet korrekt når flere perioder overlapper.
 *
 * Overlappende perioder vil alltid merges og første 'fom' og aktuell 'tom' brukes til å beregne ventetidsperioden.
 * Helgedager på slutten av en ventetidsperiode tas ikke med. Hvis en periode slutter på en lørdag eller søndag,
 * vil fredag bli brukt som 'tom'.
 */
@Disabled
class OverlappendePerioderTest : FellesTestOppsett() {
    @Autowired
    private lateinit var ventetidUtregner: VentetidUtregner

    @BeforeEach
    @AfterEach
    fun setUp() {
        syketilfellebitRepository.deleteAll()
    }

    private val fnr = "11111111111"

    @Nested
    internal inner class BeggePerioderInnenforVentetid {
        /*
         * A: [--------]
         * B: [--------]
         */
        @Test
        fun `Første og siste periode starter og slutter samtidig`() {
            val melding1 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2025, Month.SEPTEMBER, 1),
                    tom = LocalDate.of(2025, Month.SEPTEMBER, 8),
                ).also { it.prosesser() }

            val melding2 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2025, Month.SEPTEMBER, 1),
                    tom = LocalDate.of(2025, Month.SEPTEMBER, 8),
                ).also { it.prosesser() }

            verifiserAtBiterErLagret(2)

            ventetidUtregner
                .beregnVentetid(
                    sykmeldingId = melding1.sykmelding.id,
                    identer = listOf(fnr),
                    ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
                )!!
                .also {
                    it.fom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 1)
                    it.tom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 8)
                }

            ventetidUtregner
                .beregnVentetid(
                    sykmeldingId = melding2.sykmelding.id,
                    identer = listOf(fnr),
                    ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
                )!!
                .also {
                    it.fom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 1)
                    it.tom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 8)
                }
        }

        /*
         * A: [--------]
         * B:     [--------]
         */
        @Test
        fun `Siste periode starter etter og slutter etter første periode`() {
            val melding1 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2025, Month.SEPTEMBER, 1),
                    tom = LocalDate.of(2025, Month.SEPTEMBER, 8),
                ).also { it.prosesser() }

            val melding2 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2025, Month.SEPTEMBER, 5),
                    tom = LocalDate.of(2025, Month.SEPTEMBER, 12),
                ).also { it.prosesser() }

            verifiserAtBiterErLagret(2)

            ventetidUtregner
                .beregnVentetid(
                    sykmeldingId = melding1.sykmelding.id,
                    identer = listOf(fnr),
                    ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
                )!!
                .also {
                    it.fom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 1)
                    it.tom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 8)
                }

            ventetidUtregner
                .beregnVentetid(
                    sykmeldingId = melding2.sykmelding.id,
                    identer = listOf(fnr),
                    ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
                )!!
                .also {
                    it.fom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 1)
                    it.tom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 12)
                }
        }

        /*
         * A: [--------]
         * B:     [----]
         */
        @Test
        fun `Siste periode starter etter og slutter samtidig med første periode`() {
            val melding1 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2025, Month.SEPTEMBER, 1),
                    tom = LocalDate.of(2025, Month.SEPTEMBER, 8),
                ).also { it.prosesser() }

            val melding2 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2025, Month.SEPTEMBER, 5),
                    tom = LocalDate.of(2025, Month.SEPTEMBER, 8),
                ).also { it.prosesser() }

            verifiserAtBiterErLagret(2)

            ventetidUtregner
                .beregnVentetid(
                    sykmeldingId = melding1.sykmelding.id,
                    identer = listOf(fnr),
                    ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
                )!!
                .also {
                    it.fom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 1)
                    it.tom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 8)
                }

            ventetidUtregner
                .beregnVentetid(
                    sykmeldingId = melding2.sykmelding.id,
                    identer = listOf(fnr),
                    ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
                )!!
                .also {
                    it.fom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 1)
                    it.tom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 8)
                }
        }

        /*
         * A: [--------]
         * B: [----]
         */
        @Test
        fun `Siste periode starter samtidig og slutter før første periode`() {
            val melding1 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2025, Month.SEPTEMBER, 1),
                    tom = LocalDate.of(2025, Month.SEPTEMBER, 8),
                ).also { it.prosesser() }

            val melding2 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2025, Month.SEPTEMBER, 1),
                    tom = LocalDate.of(2025, Month.SEPTEMBER, 4),
                ).also { it.prosesser() }

            verifiserAtBiterErLagret(2)

            ventetidUtregner
                .beregnVentetid(
                    sykmeldingId = melding1.sykmelding.id,
                    identer = listOf(fnr),
                    ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
                )!!
                .also {
                    it.fom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 1)
                    it.tom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 8)
                }

            ventetidUtregner
                .beregnVentetid(
                    sykmeldingId = melding2.sykmelding.id,
                    identer = listOf(fnr),
                    ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
                )!!
                .also {
                    it.fom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 1)
                    it.tom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 4)
                }
        }

        /*
         * A: [--------]
         * B:   [----]
         */
        @Test
        fun `Siste periode starter etter og slutter før første periode`() {
            val melding1 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2025, Month.SEPTEMBER, 1),
                    tom = LocalDate.of(2025, Month.SEPTEMBER, 8),
                ).also { it.prosesser() }

            val melding2 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2025, Month.SEPTEMBER, 3),
                    tom = LocalDate.of(2025, Month.SEPTEMBER, 6),
                ).also { it.prosesser() }

            verifiserAtBiterErLagret(2)

            ventetidUtregner
                .beregnVentetid(
                    sykmeldingId = melding1.sykmelding.id,
                    identer = listOf(fnr),
                    ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
                )!!
                .also {
                    it.fom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 1)
                    it.tom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 8)
                }

            ventetidUtregner
                .beregnVentetid(
                    sykmeldingId = melding2.sykmelding.id,
                    identer = listOf(fnr),
                    ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
                )!!
                .also {
                    it.fom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 1)
                    it.tom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 5)
                }
        }

        /*
         * A:   [----]
         * B: [--------]
         */
        @Test
        fun `Siste periode starter før og slutter etter første periode`() {
            val melding1 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2025, Month.SEPTEMBER, 3),
                    tom = LocalDate.of(2025, Month.SEPTEMBER, 6),
                ).also { it.prosesser() }

            val melding2 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2025, Month.SEPTEMBER, 1),
                    tom = LocalDate.of(2025, Month.SEPTEMBER, 8),
                ).also { it.prosesser() }

            verifiserAtBiterErLagret(2)

            ventetidUtregner
                .beregnVentetid(
                    sykmeldingId = melding1.sykmelding.id,
                    identer = listOf(fnr),
                    ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
                )!!
                .also {
                    it.fom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 1)
                    it.tom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 5)
                }

            ventetidUtregner
                .beregnVentetid(
                    sykmeldingId = melding2.sykmelding.id,
                    identer = listOf(fnr),
                    ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
                )!!
                .also {
                    it.fom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 1)
                    it.tom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 8)
                }
        }

        /*
         * A: [----]
         * B: [--------]
         */
        @Test
        fun `Siste periode starter samtidig og slutter etter første periode`() {
            val melding1 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2025, Month.SEPTEMBER, 1),
                    tom = LocalDate.of(2025, Month.SEPTEMBER, 4),
                ).also { it.prosesser() }

            val melding2 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2025, Month.SEPTEMBER, 1),
                    tom = LocalDate.of(2025, Month.SEPTEMBER, 8),
                ).also { it.prosesser() }

            verifiserAtBiterErLagret(2)

            ventetidUtregner
                .beregnVentetid(
                    sykmeldingId = melding1.sykmelding.id,
                    identer = listOf(fnr),
                    ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
                )!!
                .also {
                    it.fom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 1)
                    it.tom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 4)
                }

            ventetidUtregner
                .beregnVentetid(
                    sykmeldingId = melding2.sykmelding.id,
                    identer = listOf(fnr),
                    ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
                )!!
                .also {
                    it.fom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 1)
                    it.tom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 8)
                }
        }

        /*
         * A:     [----]
         * B: [--------]
         */
        @Test
        fun `Siste periode starter før og slutter samtidig med første periode`() {
            val melding1 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2025, Month.SEPTEMBER, 5),
                    tom = LocalDate.of(2025, Month.SEPTEMBER, 8),
                ).also { it.prosesser() }

            val melding2 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2025, Month.SEPTEMBER, 1),
                    tom = LocalDate.of(2025, Month.SEPTEMBER, 8),
                ).also { it.prosesser() }

            verifiserAtBiterErLagret(2)

            ventetidUtregner
                .beregnVentetid(
                    sykmeldingId = melding1.sykmelding.id,
                    identer = listOf(fnr),
                    ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
                )!!
                .also {
                    it.fom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 1)
                    it.tom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 8)
                }

            ventetidUtregner
                .beregnVentetid(
                    sykmeldingId = melding2.sykmelding.id,
                    identer = listOf(fnr),
                    ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
                )!!
                .also {
                    it.fom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 1)
                    it.tom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 8)
                }
        }

        /*
         * A:      [--------]
         * B: [--------]
         */
        @Test
        fun `Siste periode starter før og slutter før første periode`() {
            val melding1 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2025, Month.SEPTEMBER, 5),
                    tom = LocalDate.of(2025, Month.SEPTEMBER, 12),
                ).also { it.prosesser() }

            val melding2 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2025, Month.SEPTEMBER, 1),
                    tom = LocalDate.of(2025, Month.SEPTEMBER, 8),
                ).also { it.prosesser() }

            verifiserAtBiterErLagret(2)

            ventetidUtregner
                .beregnVentetid(
                    sykmeldingId = melding1.sykmelding.id,
                    identer = listOf(fnr),
                    ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
                )!!
                .also {
                    it.fom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 1)
                    it.tom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 12)
                }

            ventetidUtregner
                .beregnVentetid(
                    sykmeldingId = melding2.sykmelding.id,
                    identer = listOf(fnr),
                    ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
                )!!
                .also {
                    it.fom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 1)
                    it.tom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 8)
                }
        }
    }

    @Nested
    internal inner class EnPeriodeInnenforVentetid {
        /*
         * A: [------------------]
         * B:               [--------]
         */
        @Test
        fun `Siste periode starter etter og slutter etter første periode`() {
            val melding1 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2025, Month.SEPTEMBER, 1),
                    tom = LocalDate.of(2025, Month.SEPTEMBER, 18),
                ).also { it.prosesser() }

            val melding2 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2025, Month.SEPTEMBER, 15),
                    tom = LocalDate.of(2025, Month.SEPTEMBER, 22),
                ).also { it.prosesser() }

            verifiserAtBiterErLagret(2)

            ventetidUtregner
                .beregnVentetid(
                    sykmeldingId = melding1.sykmelding.id,
                    identer = listOf(fnr),
                    ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
                )!!
                .also {
                    it.fom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 1)
                    it.tom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 16)
                }

            ventetidUtregner
                .beregnVentetid(
                    sykmeldingId = melding2.sykmelding.id,
                    identer = listOf(fnr),
                    ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
                )!!
                .also {
                    it.fom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 1)
                    it.tom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 16)
                }
        }

        /*
         * A: [------------------]
         * B:           [--------]
         */
        @Test
        fun `Siste periode starter etter og slutter samtidig med første periode`() {
            val melding1 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2025, Month.SEPTEMBER, 1),
                    tom = LocalDate.of(2025, Month.SEPTEMBER, 18),
                ).also { it.prosesser() }

            val melding2 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2025, Month.SEPTEMBER, 11),
                    tom = LocalDate.of(2025, Month.SEPTEMBER, 18),
                ).also { it.prosesser() }

            verifiserAtBiterErLagret(2)

            ventetidUtregner
                .beregnVentetid(
                    sykmeldingId = melding1.sykmelding.id,
                    identer = listOf(fnr),
                    ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
                )!!
                .also {
                    it.fom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 1)
                    it.tom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 16)
                }

            ventetidUtregner
                .beregnVentetid(
                    sykmeldingId = melding2.sykmelding.id,
                    identer = listOf(fnr),
                    ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
                )!!
                .also {
                    it.fom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 1)
                    it.tom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 16)
                }
        }

        /*
         * A: [------------------]
         * B: [--------]
         */
        @Test
        fun `Siste periode starter samtidig og slutter før første periode`() {
            val melding1 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2025, Month.SEPTEMBER, 1),
                    tom = LocalDate.of(2025, Month.SEPTEMBER, 18),
                ).also { it.prosesser() }

            val melding2 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2025, Month.SEPTEMBER, 1),
                    tom = LocalDate.of(2025, Month.SEPTEMBER, 8),
                ).also { it.prosesser() }

            verifiserAtBiterErLagret(2)

            ventetidUtregner
                .beregnVentetid(
                    sykmeldingId = melding1.sykmelding.id,
                    identer = listOf(fnr),
                    ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
                )!!
                .also {
                    it.fom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 1)
                    it.tom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 16)
                }

            ventetidUtregner
                .beregnVentetid(
                    sykmeldingId = melding2.sykmelding.id,
                    identer = listOf(fnr),
                    ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
                )!!
                .also {
                    it.fom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 1)
                    it.tom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 8)
                }
        }

        /*
         * A: [------------------]
         * B:      [--------]
         */
        @Test
        fun `Siste periode starter etter og slutter før første periode`() {
            val melding1 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2025, Month.SEPTEMBER, 1),
                    tom = LocalDate.of(2025, Month.SEPTEMBER, 18),
                ).also { it.prosesser() }

            val melding2 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2025, Month.SEPTEMBER, 6),
                    tom = LocalDate.of(2025, Month.SEPTEMBER, 13),
                ).also { it.prosesser() }

            verifiserAtBiterErLagret(2)

            ventetidUtregner
                .beregnVentetid(
                    sykmeldingId = melding1.sykmelding.id,
                    identer = listOf(fnr),
                    ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
                )!!
                .also {
                    it.fom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 1)
                    it.tom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 16)
                }

            ventetidUtregner
                .beregnVentetid(
                    sykmeldingId = melding2.sykmelding.id,
                    identer = listOf(fnr),
                    ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
                )!!
                .also {
                    it.fom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 1)
                    it.tom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 12)
                }
        }

        /*
         * A:      [--------]
         * B: [------------------]
         */
        @Test
        fun `Siste periode starter før og slutter etter første periode`() {
            val melding1 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2025, Month.SEPTEMBER, 6),
                    tom = LocalDate.of(2025, Month.SEPTEMBER, 13),
                ).also { it.prosesser() }

            val melding2 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2025, Month.SEPTEMBER, 1),
                    tom = LocalDate.of(2025, Month.SEPTEMBER, 18),
                ).also { it.prosesser() }

            verifiserAtBiterErLagret(2)

            ventetidUtregner
                .beregnVentetid(
                    sykmeldingId = melding1.sykmelding.id,
                    identer = listOf(fnr),
                    ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
                )!!
                .also {
                    it.fom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 1)
                    it.tom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 12)
                }

            ventetidUtregner
                .beregnVentetid(
                    sykmeldingId = melding2.sykmelding.id,
                    identer = listOf(fnr),
                    ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
                )!!
                .also {
                    it.fom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 1)
                    it.tom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 16)
                }
        }

        /*
         * A: [--------]
         * B: [------------------]
         */
        @Test
        fun `Siste periode starter samtidig og slutter etter første periode`() {
            val melding1 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2025, Month.SEPTEMBER, 1),
                    tom = LocalDate.of(2025, Month.SEPTEMBER, 8),
                ).also { it.prosesser() }

            val melding2 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2025, Month.SEPTEMBER, 1),
                    tom = LocalDate.of(2025, Month.SEPTEMBER, 18),
                ).also { it.prosesser() }

            verifiserAtBiterErLagret(2)

            ventetidUtregner
                .beregnVentetid(
                    sykmeldingId = melding1.sykmelding.id,
                    identer = listOf(fnr),
                    ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
                )!!
                .also {
                    it.fom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 1)
                    it.tom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 8)
                }

            ventetidUtregner
                .beregnVentetid(
                    sykmeldingId = melding2.sykmelding.id,
                    identer = listOf(fnr),
                    ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
                )!!
                .also {
                    it.fom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 1)
                    it.tom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 16)
                }
        }

        /*
         * A:           [--------]
         * B: [------------------]
         */
        @Test
        fun `Siste periode starter før og slutter samtidig med første periode`() {
            val melding1 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2025, Month.SEPTEMBER, 11),
                    tom = LocalDate.of(2025, Month.SEPTEMBER, 18),
                ).also { it.prosesser() }

            val melding2 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2025, Month.SEPTEMBER, 1),
                    tom = LocalDate.of(2025, Month.SEPTEMBER, 18),
                ).also { it.prosesser() }

            verifiserAtBiterErLagret(2)

            ventetidUtregner
                .beregnVentetid(
                    sykmeldingId = melding1.sykmelding.id,
                    identer = listOf(fnr),
                    ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
                )!!
                .also {
                    it.fom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 1)
                    it.tom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 16)
                }

            ventetidUtregner
                .beregnVentetid(
                    sykmeldingId = melding2.sykmelding.id,
                    identer = listOf(fnr),
                    ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
                )!!
                .also {
                    it.fom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 1)
                    it.tom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 16)
                }
        }

        /*
         * A:               [--------]
         * B: [------------------]
         */
        @Test
        fun `Siste periode starter før og slutter før første periode`() {
            val melding1 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2025, Month.SEPTEMBER, 15),
                    tom = LocalDate.of(2025, Month.SEPTEMBER, 22),
                ).also { it.prosesser() }

            val melding2 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2025, Month.SEPTEMBER, 1),
                    tom = LocalDate.of(2025, Month.SEPTEMBER, 18),
                ).also { it.prosesser() }

            verifiserAtBiterErLagret(2)

            ventetidUtregner
                .beregnVentetid(
                    sykmeldingId = melding1.sykmelding.id,
                    identer = listOf(fnr),
                    ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
                )!!
                .also {
                    it.fom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 1)
                    it.tom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 16)
                }

            ventetidUtregner
                .beregnVentetid(
                    sykmeldingId = melding2.sykmelding.id,
                    identer = listOf(fnr),
                    ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
                )!!
                .also {
                    it.fom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 1)
                    it.tom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 16)
                }
        }
    }

    @Nested
    internal inner class BeggePeriodeUtenforVentetid {
        /*
         * A: [------------------]
         * B: [------------------]
         */
        @Test
        fun `Første og siste periode starter og slutter samtidig`() {
            val melding1 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2025, Month.SEPTEMBER, 1),
                    tom = LocalDate.of(2025, Month.SEPTEMBER, 18),
                ).also { it.prosesser() }

            val melding2 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2025, Month.SEPTEMBER, 1),
                    tom = LocalDate.of(2025, Month.SEPTEMBER, 18),
                ).also { it.prosesser() }

            verifiserAtBiterErLagret(2)

            ventetidUtregner
                .beregnVentetid(
                    sykmeldingId = melding1.sykmelding.id,
                    identer = listOf(fnr),
                    ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
                )!!
                .also {
                    it.fom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 1)
                    it.tom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 16)
                }

            ventetidUtregner
                .beregnVentetid(
                    sykmeldingId = melding2.sykmelding.id,
                    identer = listOf(fnr),
                    ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
                )!!
                .also {
                    it.fom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 1)
                    it.tom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 16)
                }
        }

        /*
         * A: [------------------]
         * B:           [------------------]
         */
        @Test
        fun `Siste periode starter etter og slutter etter første periode`() {
            val melding1 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2025, Month.SEPTEMBER, 1),
                    tom = LocalDate.of(2025, Month.SEPTEMBER, 18),
                ).also { it.prosesser() }

            val melding2 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2025, Month.SEPTEMBER, 11),
                    tom = LocalDate.of(2025, Month.SEPTEMBER, 28),
                ).also { it.prosesser() }

            verifiserAtBiterErLagret(2)

            ventetidUtregner
                .beregnVentetid(
                    sykmeldingId = melding1.sykmelding.id,
                    identer = listOf(fnr),
                    ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
                )!!
                .also {
                    it.fom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 1)
                    it.tom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 16)
                }

            ventetidUtregner
                .beregnVentetid(
                    sykmeldingId = melding2.sykmelding.id,
                    identer = listOf(fnr),
                    ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
                )!!
                .also {
                    it.fom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 1)
                    it.tom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 16)
                }
        }

        /*
         * A: [----------------------]
         * B:     [------------------]
         */
        @Test
        fun `Siste periode starter etter og slutter samtidig med første periode`() {
            val melding1 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2025, Month.SEPTEMBER, 1),
                    tom = LocalDate.of(2025, Month.SEPTEMBER, 22),
                ).also { it.prosesser() }

            val melding2 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2025, Month.SEPTEMBER, 5),
                    tom = LocalDate.of(2025, Month.SEPTEMBER, 22),
                ).also { it.prosesser() }

            verifiserAtBiterErLagret(2)

            ventetidUtregner
                .beregnVentetid(
                    sykmeldingId = melding1.sykmelding.id,
                    identer = listOf(fnr),
                    ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
                )!!
                .also {
                    it.fom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 1)
                    it.tom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 16)
                }

            ventetidUtregner
                .beregnVentetid(
                    sykmeldingId = melding2.sykmelding.id,
                    identer = listOf(fnr),
                    ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
                )!!
                .also {
                    it.fom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 1)
                    it.tom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 16)
                }
        }

        /*
         * A: [----------------------]
         * B: [------------------]
         */
        @Test
        fun `Siste periode starter samtidig og slutter før første periode`() {
            val melding1 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2025, Month.SEPTEMBER, 5),
                    tom = LocalDate.of(2025, Month.SEPTEMBER, 22),
                ).also { it.prosesser() }

            val melding2 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2025, Month.SEPTEMBER, 1),
                    tom = LocalDate.of(2025, Month.SEPTEMBER, 22),
                ).also { it.prosesser() }

            verifiserAtBiterErLagret(2)

            ventetidUtregner
                .beregnVentetid(
                    sykmeldingId = melding1.sykmelding.id,
                    identer = listOf(fnr),
                    ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
                )!!
                .also {
                    it.fom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 1)
                    it.tom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 16)
                }

            ventetidUtregner
                .beregnVentetid(
                    sykmeldingId = melding2.sykmelding.id,
                    identer = listOf(fnr),
                    ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
                )!!
                .also {
                    it.fom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 1)
                    it.tom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 16)
                }
        }

        /*
         * A: [----------------------]
         * B:   [------------------]
         */
        @Test
        fun `Siste periode starter etter og slutter før første periode`() {
            val melding1 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2025, Month.SEPTEMBER, 1),
                    tom = LocalDate.of(2025, Month.SEPTEMBER, 22),
                ).also { it.prosesser() }

            val melding2 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2025, Month.SEPTEMBER, 3),
                    tom = LocalDate.of(2025, Month.SEPTEMBER, 20),
                ).also { it.prosesser() }

            verifiserAtBiterErLagret(2)

            ventetidUtregner
                .beregnVentetid(
                    sykmeldingId = melding1.sykmelding.id,
                    identer = listOf(fnr),
                    ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
                )!!
                .also {
                    it.fom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 1)
                    it.tom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 16)
                }

            ventetidUtregner
                .beregnVentetid(
                    sykmeldingId = melding2.sykmelding.id,
                    identer = listOf(fnr),
                    ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
                )!!
                .also {
                    it.fom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 1)
                    it.tom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 16)
                }
        }

        /*
         * A:   [------------------]
         * B: [----------------------]
         */
        @Test
        fun `Siste periode starter før og slutter etter første periode`() {
            val melding1 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2025, Month.SEPTEMBER, 3),
                    tom = LocalDate.of(2025, Month.SEPTEMBER, 20),
                ).also { it.prosesser() }

            val melding2 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2025, Month.SEPTEMBER, 1),
                    tom = LocalDate.of(2025, Month.SEPTEMBER, 22),
                ).also { it.prosesser() }

            verifiserAtBiterErLagret(2)

            ventetidUtregner
                .beregnVentetid(
                    sykmeldingId = melding1.sykmelding.id,
                    identer = listOf(fnr),
                    ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
                )!!
                .also {
                    it.fom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 1)
                    it.tom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 16)
                }

            ventetidUtregner
                .beregnVentetid(
                    sykmeldingId = melding2.sykmelding.id,
                    identer = listOf(fnr),
                    ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
                )!!
                .also {
                    it.fom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 1)
                    it.tom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 16)
                }
        }

        /*
         * B: [------------------]
         * A: [----------------------]
         */
        @Test
        fun `Siste periode starter samtidig og slutter etter første periode`() {
            val melding1 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2025, Month.SEPTEMBER, 1),
                    tom = LocalDate.of(2025, Month.SEPTEMBER, 18),
                ).also { it.prosesser() }

            val melding2 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2025, Month.SEPTEMBER, 1),
                    tom = LocalDate.of(2025, Month.SEPTEMBER, 22),
                ).also { it.prosesser() }

            verifiserAtBiterErLagret(2)

            ventetidUtregner
                .beregnVentetid(
                    sykmeldingId = melding1.sykmelding.id,
                    identer = listOf(fnr),
                    ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
                )!!
                .also {
                    it.fom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 1)
                    it.tom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 16)
                }

            ventetidUtregner
                .beregnVentetid(
                    sykmeldingId = melding2.sykmelding.id,
                    identer = listOf(fnr),
                    ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
                )!!
                .also {
                    it.fom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 1)
                    it.tom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 16)
                }
        }

        /*
         * A:     [------------------]
         * B: [----------------------]
         */
        @Test
        fun `Siste periode starter før og slutter samtidig med første periode`() {
            val melding1 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2025, Month.SEPTEMBER, 5),
                    tom = LocalDate.of(2025, Month.SEPTEMBER, 22),
                ).also { it.prosesser() }

            val melding2 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2025, Month.SEPTEMBER, 1),
                    tom = LocalDate.of(2025, Month.SEPTEMBER, 22),
                ).also { it.prosesser() }

            verifiserAtBiterErLagret(2)

            ventetidUtregner
                .beregnVentetid(
                    sykmeldingId = melding1.sykmelding.id,
                    identer = listOf(fnr),
                    ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
                )!!
                .also {
                    it.fom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 1)
                    it.tom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 16)
                }

            ventetidUtregner
                .beregnVentetid(
                    sykmeldingId = melding2.sykmelding.id,
                    identer = listOf(fnr),
                    ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
                )!!
                .also {
                    it.fom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 1)
                    it.tom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 16)
                }
        }

        /*
         * A:           [------------------]
         * B: [------------------]
         */
        @Test
        fun `Siste periode starter før og slutter før første periode`() {
            val melding1 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2025, Month.SEPTEMBER, 11),
                    tom = LocalDate.of(2025, Month.SEPTEMBER, 28),
                ).also { it.prosesser() }

            val melding2 =
                lagMottattSykmeldingKafkaMessage(
                    fnr = fnr,
                    fom = LocalDate.of(2025, Month.SEPTEMBER, 1),
                    tom = LocalDate.of(2025, Month.SEPTEMBER, 18),
                ).also { it.prosesser() }

            verifiserAtBiterErLagret(2)

            ventetidUtregner
                .beregnVentetid(
                    sykmeldingId = melding1.sykmelding.id,
                    identer = listOf(fnr),
                    ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
                )!!
                .also {
                    it.fom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 1)
                    it.tom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 16)
                }

            ventetidUtregner
                .beregnVentetid(
                    sykmeldingId = melding2.sykmelding.id,
                    identer = listOf(fnr),
                    ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
                )!!
                .also {
                    it.fom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 1)
                    it.tom `should be equal to` LocalDate.of(2025, Month.SEPTEMBER, 16)
                }
        }
    }
}
