package no.nav.helse.flex.syketilfelle.arbeidsgiverperiode

import no.nav.helse.flex.syketilfelle.arbeidsgiverperiode.domain.Syketilfelledag
import no.nav.helse.flex.syketilfelle.extensions.osloZone
import no.nav.helse.flex.syketilfelle.syketilfellebit.Syketilfellebit
import no.nav.helse.flex.syketilfelle.syketilfellebit.Tag.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.OffsetDateTime

class GrupperOppfolgingstilfellerTest {
    val mandag = LocalDate.of(2018, 11, 26)
    val mandagMorgen = mandag.osloStartOfDay()

    fun createSyketilfelleDag(
        dag: LocalDate,
        syketilfellebit: Syketilfellebit?,
    ): Syketilfelledag =
        Syketilfelledag(
            dag,
            syketilfellebit,
            syketilfellebit?.let { listOf(it) }
                ?: emptyList(),
        )

    @Test
    fun grupperIOppfolgingstilfellerEttTilfelle5Sykedager2Feriedager() {
        val tidslinje =
            listOf(
                createSyketilfelleDag(
                    mandag,
                    Syketilfellebit(
                        id = "id",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        opprettet = mandagMorgen,
                        inntruffet = mandagMorgen,
                        tags = setOf(SYKMELDING, SENDT, PERIODE, INGEN_AKTIVITET),
                        ressursId = "ressursId",
                        fom = mandag,
                        tom = mandag.plusDays(6),
                    ),
                ),
                createSyketilfelleDag(
                    mandag.plusDays(1),
                    Syketilfellebit(
                        id = "id",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        opprettet = mandagMorgen,
                        inntruffet = mandagMorgen,
                        tags = setOf(SYKMELDING, SENDT, PERIODE, INGEN_AKTIVITET),
                        ressursId = "ressursId",
                        fom = mandag,
                        tom = mandag.plusDays(6),
                    ),
                ),
                createSyketilfelleDag(
                    mandag.plusDays(2),
                    Syketilfellebit(
                        id = "id",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        opprettet = mandagMorgen,
                        inntruffet = mandagMorgen,
                        tags = setOf(SYKMELDING, SENDT, PERIODE, INGEN_AKTIVITET),
                        ressursId = "ressursId",
                        fom = mandag,
                        tom = mandag.plusDays(6),
                    ),
                ),
                createSyketilfelleDag(
                    mandag.plusDays(3),
                    Syketilfellebit(
                        id = "id",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        opprettet = mandagMorgen,
                        inntruffet = mandagMorgen,
                        tags = setOf(SYKMELDING, SENDT, PERIODE, INGEN_AKTIVITET),
                        ressursId = "ressursId",
                        fom = mandag,
                        tom = mandag.plusDays(6),
                    ),
                ),
                createSyketilfelleDag(
                    mandag.plusDays(4),
                    Syketilfellebit(
                        id = "id",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        opprettet = mandagMorgen,
                        inntruffet = mandagMorgen,
                        tags = setOf(SYKMELDING, SENDT, PERIODE, INGEN_AKTIVITET),
                        ressursId = "ressursId",
                        fom = mandag,
                        tom = mandag.plusDays(6),
                    ),
                ),
                createSyketilfelleDag(
                    mandag.plusDays(5),
                    Syketilfellebit(
                        id = "id",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        opprettet = mandagMorgen,
                        inntruffet = mandagMorgen,
                        tags = setOf(SYKEPENGESOKNAD, FERIE),
                        ressursId = "ressursId",
                        fom = mandag,
                        tom = mandag.plusDays(6),
                    ),
                ),
                createSyketilfelleDag(
                    mandag.plusDays(6),
                    Syketilfellebit(
                        id = "id",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        opprettet = mandagMorgen,
                        inntruffet = mandagMorgen,
                        tags = setOf(SYKEPENGESOKNAD, FERIE),
                        ressursId = "ressursId",
                        fom = mandag,
                        tom = mandag.plusDays(6),
                    ),
                ),
            )

        val tilfeller = grupperIOppfolgingstilfeller(tidslinje)

        assertThat(tilfeller).hasSize(1)
        assertThat(tilfeller[0].antallDager()).isEqualTo(5)
    }

    @Test
    fun grupperIOppfolgingstilfellerEttTilfelle15DagerMellom() {
        val tidslinje =
            listOf(
                createSyketilfelleDag(
                    mandag,
                    Syketilfellebit(
                        id = "id",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        opprettet = mandagMorgen,
                        inntruffet = mandagMorgen,
                        tags = setOf(SYKMELDING, SENDT, PERIODE, INGEN_AKTIVITET),
                        ressursId = "ressursId",
                        fom = mandag,
                        tom = mandag,
                    ),
                ),
                createSyketilfelleDag(mandag.plusDays(1), null),
                createSyketilfelleDag(mandag.plusDays(2), null),
                createSyketilfelleDag(mandag.plusDays(3), null),
                createSyketilfelleDag(mandag.plusDays(4), null),
                createSyketilfelleDag(mandag.plusDays(5), null),
                createSyketilfelleDag(mandag.plusDays(6), null),
                createSyketilfelleDag(mandag.plusDays(7), null),
                createSyketilfelleDag(mandag.plusDays(8), null),
                createSyketilfelleDag(mandag.plusDays(9), null),
                createSyketilfelleDag(mandag.plusDays(10), null),
                createSyketilfelleDag(mandag.plusDays(11), null),
                createSyketilfelleDag(mandag.plusDays(12), null),
                createSyketilfelleDag(mandag.plusDays(13), null),
                createSyketilfelleDag(mandag.plusDays(14), null),
                createSyketilfelleDag(mandag.plusDays(15), null),
                createSyketilfelleDag(
                    mandag.plusDays(16),
                    Syketilfellebit(
                        id = "id",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        opprettet = mandagMorgen,
                        inntruffet = mandagMorgen,
                        tags = setOf(SYKMELDING, SENDT, PERIODE, INGEN_AKTIVITET),
                        ressursId = "ressursId",
                        fom = mandag.plusDays(16),
                        tom = mandag.plusDays(16),
                    ),
                ),
            )

        val tilfeller = grupperIOppfolgingstilfeller(tidslinje)

        assertThat(tilfeller).hasSize(1)
        assertThat(tilfeller[0].antallDager()).isEqualTo(2)
    }

    @Test
    fun grupperIOppfolgingstilfellerToTilfeller16DagerMellom() {
        val tidslinje =
            listOf(
                createSyketilfelleDag(
                    mandag,
                    Syketilfellebit(
                        id = "id",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        opprettet = mandagMorgen,
                        inntruffet = mandagMorgen,
                        tags = setOf(SYKMELDING, SENDT, PERIODE, INGEN_AKTIVITET),
                        ressursId = "ressursId",
                        fom = mandag,
                        tom = mandag,
                    ),
                ),
                createSyketilfelleDag(mandag.plusDays(1), null),
                createSyketilfelleDag(mandag.plusDays(2), null),
                createSyketilfelleDag(mandag.plusDays(3), null),
                createSyketilfelleDag(mandag.plusDays(4), null),
                createSyketilfelleDag(mandag.plusDays(5), null),
                createSyketilfelleDag(mandag.plusDays(6), null),
                createSyketilfelleDag(mandag.plusDays(7), null),
                createSyketilfelleDag(mandag.plusDays(8), null),
                createSyketilfelleDag(mandag.plusDays(9), null),
                createSyketilfelleDag(mandag.plusDays(10), null),
                createSyketilfelleDag(mandag.plusDays(11), null),
                createSyketilfelleDag(mandag.plusDays(12), null),
                createSyketilfelleDag(mandag.plusDays(13), null),
                createSyketilfelleDag(mandag.plusDays(14), null),
                createSyketilfelleDag(mandag.plusDays(15), null),
                createSyketilfelleDag(mandag.plusDays(16), null),
                createSyketilfelleDag(
                    mandag.plusDays(17),
                    Syketilfellebit(
                        id = "id",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        opprettet = mandagMorgen,
                        inntruffet = mandagMorgen,
                        tags = setOf(SYKMELDING, SENDT, PERIODE, INGEN_AKTIVITET),
                        ressursId = "ressursId",
                        fom = mandag.plusDays(17),
                        tom = mandag.plusDays(17),
                    ),
                ),
            )

        val tilfeller = grupperIOppfolgingstilfeller(tidslinje)

        assertThat(tilfeller).hasSize(2)
        assertThat(tilfeller[0].antallDager()).isEqualTo(1)
        assertThat(tilfeller[1].antallDager()).isEqualTo(1)
    }

    @Test
    fun grupperIOppfolgingstilfellerEttTilfelleSlutterMedHelg() {
        val tidslinje =
            listOf(
                createSyketilfelleDag(
                    mandag,
                    Syketilfellebit(
                        id = "id",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        opprettet = mandagMorgen,
                        inntruffet = mandagMorgen,
                        tags = setOf(SYKMELDING, SENDT, PERIODE, INGEN_AKTIVITET),
                        ressursId = "ressursId",
                        fom = mandag,
                        tom = mandag.plusDays(6),
                    ),
                ),
                createSyketilfelleDag(
                    mandag.plusDays(1),
                    Syketilfellebit(
                        id = "id",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        opprettet = mandagMorgen,
                        inntruffet = mandagMorgen,
                        tags = setOf(SYKMELDING, SENDT, PERIODE, INGEN_AKTIVITET),
                        ressursId = "ressursId",
                        fom = mandag,
                        tom = mandag.plusDays(6),
                    ),
                ),
                createSyketilfelleDag(
                    mandag.plusDays(2),
                    Syketilfellebit(
                        id = "id",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        opprettet = mandagMorgen,
                        inntruffet = mandagMorgen,
                        tags = setOf(SYKMELDING, SENDT, PERIODE, INGEN_AKTIVITET),
                        ressursId = "ressursId",
                        fom = mandag,
                        tom = mandag.plusDays(6),
                    ),
                ),
                createSyketilfelleDag(
                    mandag.plusDays(3),
                    Syketilfellebit(
                        id = "id",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        opprettet = mandagMorgen,
                        inntruffet = mandagMorgen,
                        tags = setOf(SYKMELDING, SENDT, PERIODE, INGEN_AKTIVITET),
                        ressursId = "ressursId",
                        fom = mandag,
                        tom = mandag.plusDays(6),
                    ),
                ),
                createSyketilfelleDag(
                    mandag.plusDays(4),
                    Syketilfellebit(
                        id = "id",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        opprettet = mandagMorgen,
                        inntruffet = mandagMorgen,
                        tags = setOf(SYKMELDING, SENDT, PERIODE, INGEN_AKTIVITET),
                        ressursId = "ressursId",
                        fom = mandag,
                        tom = mandag.plusDays(6),
                    ),
                ),
                createSyketilfelleDag(
                    mandag.plusDays(5),
                    Syketilfellebit(
                        id = "id",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        opprettet = mandagMorgen,
                        inntruffet = mandagMorgen,
                        tags = setOf(SYKMELDING, SENDT, PERIODE, INGEN_AKTIVITET),
                        ressursId = "ressursId",
                        fom = mandag,
                        tom = mandag.plusDays(6),
                    ),
                ),
                createSyketilfelleDag(
                    mandag.plusDays(6),
                    Syketilfellebit(
                        id = "id",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        opprettet = mandagMorgen,
                        inntruffet = mandagMorgen,
                        tags = setOf(SYKMELDING, SENDT, PERIODE, INGEN_AKTIVITET),
                        ressursId = "ressursId",
                        fom = mandag,
                        tom = mandag.plusDays(6),
                    ),
                ),
            )

        val tilfeller = grupperIOppfolgingstilfeller(tidslinje)

        assertThat(tilfeller).hasSize(1)
        assertThat(tilfeller[0].antallDager()).isEqualTo(7)
    }

    @Test
    fun grupperIOppfolgingstilfellerToTilfeller1Sykedag1Feriedag16Arbeidsdager1Sykedag() {
        val tidslinje =
            listOf(
                createSyketilfelleDag(
                    mandag,
                    Syketilfellebit(
                        id = "id",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        opprettet = mandagMorgen,
                        inntruffet = mandagMorgen,
                        tags = setOf(SYKMELDING, SENDT, PERIODE, INGEN_AKTIVITET),
                        ressursId = "ressursId",
                        fom = mandag,
                        tom = mandag.plusDays(1),
                    ),
                ),
                createSyketilfelleDag(
                    mandag.plusDays(1),
                    Syketilfellebit(
                        id = "id",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        opprettet = mandagMorgen,
                        inntruffet = mandagMorgen,
                        tags = setOf(SYKEPENGESOKNAD, FERIE),
                        ressursId = "ressursId",
                        fom = mandag,
                        tom = mandag.plusDays(1),
                    ),
                ),
                createSyketilfelleDag(mandag.plusDays(2), null),
                createSyketilfelleDag(mandag.plusDays(3), null),
                createSyketilfelleDag(mandag.plusDays(4), null),
                createSyketilfelleDag(mandag.plusDays(5), null),
                createSyketilfelleDag(mandag.plusDays(6), null),
                createSyketilfelleDag(mandag.plusDays(7), null),
                createSyketilfelleDag(mandag.plusDays(8), null),
                createSyketilfelleDag(mandag.plusDays(9), null),
                createSyketilfelleDag(mandag.plusDays(10), null),
                createSyketilfelleDag(mandag.plusDays(11), null),
                createSyketilfelleDag(mandag.plusDays(12), null),
                createSyketilfelleDag(mandag.plusDays(13), null),
                createSyketilfelleDag(mandag.plusDays(14), null),
                createSyketilfelleDag(mandag.plusDays(15), null),
                createSyketilfelleDag(mandag.plusDays(16), null),
                createSyketilfelleDag(mandag.plusDays(17), null),
                createSyketilfelleDag(
                    mandag.plusDays(18),
                    Syketilfellebit(
                        id = "id",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        opprettet = mandagMorgen,
                        inntruffet = mandagMorgen,
                        tags = setOf(SYKMELDING, SENDT, PERIODE, INGEN_AKTIVITET),
                        ressursId = "ressursId",
                        fom = mandag.plusDays(18),
                        tom = mandag.plusDays(18),
                    ),
                ),
            )

        val tilfeller = grupperIOppfolgingstilfeller(tidslinje)

        assertThat(tilfeller).hasSize(2)
        assertThat(tilfeller[0].antallDager()).isEqualTo(1)
        assertThat(tilfeller[1].antallDager()).isEqualTo(1)
    }

    @Test
    fun grupperIOppfolgingstilfellerEttTilfelle16Sykedager1Feriedag15Arbeidsdager1Sykedag() {
        val tidslinje =
            listOf(
                createSyketilfelleDag(
                    mandag,
                    Syketilfellebit(
                        id = "id",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        opprettet = mandagMorgen,
                        inntruffet = mandagMorgen,
                        tags = setOf(SYKMELDING, SENDT, PERIODE, INGEN_AKTIVITET),
                        ressursId = "ressursId",
                        fom = mandag,
                        tom = mandag.plusDays(16),
                    ),
                ),
                createSyketilfelleDag(
                    mandag.plusDays(1),
                    Syketilfellebit(
                        id = "id",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        opprettet = mandagMorgen,
                        inntruffet = mandagMorgen,
                        tags = setOf(SYKMELDING, SENDT, PERIODE, INGEN_AKTIVITET),
                        ressursId = "ressursId",
                        fom = mandag,
                        tom = mandag.plusDays(16),
                    ),
                ),
                createSyketilfelleDag(
                    mandag.plusDays(2),
                    Syketilfellebit(
                        id = "id",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        opprettet = mandagMorgen,
                        inntruffet = mandagMorgen,
                        tags = setOf(SYKMELDING, SENDT, PERIODE, INGEN_AKTIVITET),
                        ressursId = "ressursId",
                        fom = mandag,
                        tom = mandag.plusDays(16),
                    ),
                ),
                createSyketilfelleDag(
                    mandag.plusDays(3),
                    Syketilfellebit(
                        id = "id",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        opprettet = mandagMorgen,
                        inntruffet = mandagMorgen,
                        tags = setOf(SYKMELDING, SENDT, PERIODE, INGEN_AKTIVITET),
                        ressursId = "ressursId",
                        fom = mandag,
                        tom = mandag.plusDays(16),
                    ),
                ),
                createSyketilfelleDag(
                    mandag.plusDays(4),
                    Syketilfellebit(
                        id = "id",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        opprettet = mandagMorgen,
                        inntruffet = mandagMorgen,
                        tags = setOf(SYKMELDING, SENDT, PERIODE, INGEN_AKTIVITET),
                        ressursId = "ressursId",
                        fom = mandag,
                        tom = mandag.plusDays(16),
                    ),
                ),
                createSyketilfelleDag(
                    mandag.plusDays(5),
                    Syketilfellebit(
                        id = "id",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        opprettet = mandagMorgen,
                        inntruffet = mandagMorgen,
                        tags = setOf(SYKMELDING, SENDT, PERIODE, INGEN_AKTIVITET),
                        ressursId = "ressursId",
                        fom = mandag,
                        tom = mandag.plusDays(16),
                    ),
                ),
                createSyketilfelleDag(
                    mandag.plusDays(6),
                    Syketilfellebit(
                        id = "id",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        opprettet = mandagMorgen,
                        inntruffet = mandagMorgen,
                        tags = setOf(SYKMELDING, SENDT, PERIODE, INGEN_AKTIVITET),
                        ressursId = "ressursId",
                        fom = mandag,
                        tom = mandag.plusDays(16),
                    ),
                ),
                createSyketilfelleDag(
                    mandag.plusDays(7),
                    Syketilfellebit(
                        id = "id",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        opprettet = mandagMorgen,
                        inntruffet = mandagMorgen,
                        tags = setOf(SYKMELDING, SENDT, PERIODE, INGEN_AKTIVITET),
                        ressursId = "ressursId",
                        fom = mandag,
                        tom = mandag.plusDays(16),
                    ),
                ),
                createSyketilfelleDag(
                    mandag.plusDays(8),
                    Syketilfellebit(
                        id = "id",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        opprettet = mandagMorgen,
                        inntruffet = mandagMorgen,
                        tags = setOf(SYKMELDING, SENDT, PERIODE, INGEN_AKTIVITET),
                        ressursId = "ressursId",
                        fom = mandag,
                        tom = mandag.plusDays(16),
                    ),
                ),
                createSyketilfelleDag(
                    mandag.plusDays(9),
                    Syketilfellebit(
                        id = "id",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        opprettet = mandagMorgen,
                        inntruffet = mandagMorgen,
                        tags = setOf(SYKMELDING, SENDT, PERIODE, INGEN_AKTIVITET),
                        ressursId = "ressursId",
                        fom = mandag,
                        tom = mandag.plusDays(16),
                    ),
                ),
                createSyketilfelleDag(
                    mandag.plusDays(10),
                    Syketilfellebit(
                        id = "id",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        opprettet = mandagMorgen,
                        inntruffet = mandagMorgen,
                        tags = setOf(SYKMELDING, SENDT, PERIODE, INGEN_AKTIVITET),
                        ressursId = "ressursId",
                        fom = mandag,
                        tom = mandag.plusDays(16),
                    ),
                ),
                createSyketilfelleDag(
                    mandag.plusDays(11),
                    Syketilfellebit(
                        id = "id",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        opprettet = mandagMorgen,
                        inntruffet = mandagMorgen,
                        tags = setOf(SYKMELDING, SENDT, PERIODE, INGEN_AKTIVITET),
                        ressursId = "ressursId",
                        fom = mandag,
                        tom = mandag.plusDays(16),
                    ),
                ),
                createSyketilfelleDag(
                    mandag.plusDays(12),
                    Syketilfellebit(
                        id = "id",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        opprettet = mandagMorgen,
                        inntruffet = mandagMorgen,
                        tags = setOf(SYKMELDING, SENDT, PERIODE, INGEN_AKTIVITET),
                        ressursId = "ressursId",
                        fom = mandag,
                        tom = mandag.plusDays(16),
                    ),
                ),
                createSyketilfelleDag(
                    mandag.plusDays(13),
                    Syketilfellebit(
                        id = "id",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        opprettet = mandagMorgen,
                        inntruffet = mandagMorgen,
                        tags = setOf(SYKMELDING, SENDT, PERIODE, INGEN_AKTIVITET),
                        ressursId = "ressursId",
                        fom = mandag,
                        tom = mandag.plusDays(16),
                    ),
                ),
                createSyketilfelleDag(
                    mandag.plusDays(14),
                    Syketilfellebit(
                        id = "id",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        opprettet = mandagMorgen,
                        inntruffet = mandagMorgen,
                        tags = setOf(SYKMELDING, SENDT, PERIODE, INGEN_AKTIVITET),
                        ressursId = "ressursId",
                        fom = mandag,
                        tom = mandag.plusDays(16),
                    ),
                ),
                createSyketilfelleDag(
                    mandag.plusDays(15),
                    Syketilfellebit(
                        id = "id",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        opprettet = mandagMorgen,
                        inntruffet = mandagMorgen,
                        tags = setOf(SYKMELDING, SENDT, PERIODE, INGEN_AKTIVITET),
                        ressursId = "ressursId",
                        fom = mandag,
                        tom = mandag.plusDays(16),
                    ),
                ),
                createSyketilfelleDag(
                    mandag.plusDays(16),
                    Syketilfellebit(
                        id = "id",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        opprettet = mandagMorgen,
                        inntruffet = mandagMorgen,
                        tags = setOf(SYKEPENGESOKNAD, FERIE),
                        ressursId = "ressursId",
                        fom = mandag,
                        tom = mandag.plusDays(16),
                    ),
                ),
                createSyketilfelleDag(mandag.plusDays(17), null),
                createSyketilfelleDag(mandag.plusDays(18), null),
                createSyketilfelleDag(mandag.plusDays(19), null),
                createSyketilfelleDag(mandag.plusDays(20), null),
                createSyketilfelleDag(mandag.plusDays(21), null),
                createSyketilfelleDag(mandag.plusDays(22), null),
                createSyketilfelleDag(mandag.plusDays(23), null),
                createSyketilfelleDag(mandag.plusDays(24), null),
                createSyketilfelleDag(mandag.plusDays(25), null),
                createSyketilfelleDag(mandag.plusDays(26), null),
                createSyketilfelleDag(mandag.plusDays(27), null),
                createSyketilfelleDag(mandag.plusDays(28), null),
                createSyketilfelleDag(mandag.plusDays(29), null),
                createSyketilfelleDag(mandag.plusDays(30), null),
                createSyketilfelleDag(mandag.plusDays(31), null),
                createSyketilfelleDag(
                    mandag.plusDays(32),
                    Syketilfellebit(
                        id = "id",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        opprettet = mandagMorgen,
                        inntruffet = mandagMorgen,
                        tags = setOf(SYKMELDING, SENDT, PERIODE, INGEN_AKTIVITET),
                        ressursId = "ressursId",
                        fom = mandag.plusDays(32),
                        tom = mandag.plusDays(32),
                    ),
                ),
            )

        val tilfeller = grupperIOppfolgingstilfeller(tidslinje)

        assertThat(tilfeller).hasSize(1)
        assertThat(tilfeller[0].antallDager()).isEqualTo(17)
    }

    @Test
    fun grupperIOppfolgingstilfellerEttTilfelle1Sykedag1Feriedag14Arbeidsdager1Sykedag() {
        val tidslinje =
            listOf(
                createSyketilfelleDag(
                    mandag,
                    Syketilfellebit(
                        id = "id",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        opprettet = mandagMorgen,
                        inntruffet = mandagMorgen,
                        tags = setOf(SYKMELDING, SENDT, PERIODE, INGEN_AKTIVITET),
                        ressursId = "ressursId",
                        fom = mandag,
                        tom = mandag.plusDays(1),
                    ),
                ),
                createSyketilfelleDag(
                    mandag.plusDays(1),
                    Syketilfellebit(
                        id = "id",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        opprettet = mandagMorgen,
                        inntruffet = mandagMorgen,
                        tags = setOf(SYKEPENGESOKNAD, FERIE),
                        ressursId = "ressursId",
                        fom = mandag,
                        tom = mandag.plusDays(1),
                    ),
                ),
                createSyketilfelleDag(mandag.plusDays(2), null),
                createSyketilfelleDag(mandag.plusDays(3), null),
                createSyketilfelleDag(mandag.plusDays(4), null),
                createSyketilfelleDag(mandag.plusDays(5), null),
                createSyketilfelleDag(mandag.plusDays(6), null),
                createSyketilfelleDag(mandag.plusDays(7), null),
                createSyketilfelleDag(mandag.plusDays(8), null),
                createSyketilfelleDag(mandag.plusDays(9), null),
                createSyketilfelleDag(mandag.plusDays(10), null),
                createSyketilfelleDag(mandag.plusDays(11), null),
                createSyketilfelleDag(mandag.plusDays(12), null),
                createSyketilfelleDag(mandag.plusDays(13), null),
                createSyketilfelleDag(mandag.plusDays(14), null),
                createSyketilfelleDag(mandag.plusDays(15), null),
                createSyketilfelleDag(
                    mandag.plusDays(16),
                    Syketilfellebit(
                        id = "id",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        opprettet = mandagMorgen,
                        inntruffet = mandagMorgen,
                        tags = setOf(SYKMELDING, SENDT, PERIODE, INGEN_AKTIVITET),
                        ressursId = "ressursId",
                        fom = mandag.plusDays(16),
                        tom = mandag.plusDays(16),
                    ),
                ),
            )

        val tilfeller = grupperIOppfolgingstilfeller(tidslinje)

        assertThat(tilfeller).hasSize(1)
        assertThat(tilfeller[0].antallDager()).isEqualTo(2)
    }

    @Test
    fun grupperIOppfolgingstilfellerToTilfeller15DagerMellomOgEnFeriedag() {
        val tidslinje =
            listOf(
                createSyketilfelleDag(
                    mandag,
                    Syketilfellebit(
                        id = "id",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        opprettet = mandagMorgen,
                        inntruffet = mandagMorgen,
                        tags = setOf(SYKMELDING, SENDT, PERIODE, INGEN_AKTIVITET),
                        ressursId = "ressursId",
                        fom = mandag,
                        tom = mandag.plusDays(1),
                    ),
                ),
                createSyketilfelleDag(mandag.plusDays(1), null),
                createSyketilfelleDag(mandag.plusDays(2), null),
                createSyketilfelleDag(mandag.plusDays(3), null),
                createSyketilfelleDag(mandag.plusDays(4), null),
                createSyketilfelleDag(mandag.plusDays(5), null),
                createSyketilfelleDag(mandag.plusDays(6), null),
                createSyketilfelleDag(mandag.plusDays(7), null),
                createSyketilfelleDag(mandag.plusDays(8), null),
                createSyketilfelleDag(mandag.plusDays(9), null),
                createSyketilfelleDag(mandag.plusDays(10), null),
                createSyketilfelleDag(mandag.plusDays(11), null),
                createSyketilfelleDag(mandag.plusDays(12), null),
                createSyketilfelleDag(mandag.plusDays(13), null),
                createSyketilfelleDag(mandag.plusDays(14), null),
                createSyketilfelleDag(mandag.plusDays(15), null),
                createSyketilfelleDag(
                    mandag.plusDays(16),
                    Syketilfellebit(
                        id = "id",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        opprettet = mandagMorgen,
                        inntruffet = mandagMorgen,
                        tags = setOf(SYKEPENGESOKNAD, FERIE),
                        ressursId = "ressursId",
                        fom = mandag.plusDays(16),
                        tom = mandag.plusDays(16),
                    ),
                ),
                createSyketilfelleDag(
                    mandag.plusDays(17),
                    Syketilfellebit(
                        id = "id",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        opprettet = mandagMorgen,
                        inntruffet = mandagMorgen,
                        tags = setOf(SYKMELDING, SENDT, PERIODE, INGEN_AKTIVITET),
                        ressursId = "ressursId",
                        fom = mandag.plusDays(17),
                        tom = mandag.plusDays(17),
                    ),
                ),
            )

        val tilfeller = grupperIOppfolgingstilfeller(tidslinje)

        assertThat(tilfeller).hasSize(2)
        assertThat(tilfeller[0].antallDager()).isEqualTo(1)
        assertThat(tilfeller[1].antallDager()).isEqualTo(1)
    }

    @Test
    fun grupperIOppfolgingstilfellerToTilfellerEnFeriedagOg16DagerMellom() {
        val tidslinje =
            listOf(
                createSyketilfelleDag(
                    mandag,
                    Syketilfellebit(
                        id = "id",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        opprettet = mandagMorgen,
                        inntruffet = mandagMorgen,
                        tags = setOf(SYKMELDING, SENDT, PERIODE, INGEN_AKTIVITET),
                        ressursId = "ressursId",
                        fom = mandag,
                        tom = mandag.plusDays(1),
                    ),
                ),
                createSyketilfelleDag(
                    mandag.plusDays(1),
                    Syketilfellebit(
                        id = "id",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        opprettet = mandagMorgen,
                        inntruffet = mandagMorgen,
                        tags = setOf(SYKEPENGESOKNAD, FERIE),
                        ressursId = "ressursId",
                        fom = mandag,
                        tom = mandag.plusDays(1),
                    ),
                ),
                createSyketilfelleDag(mandag.plusDays(2), null),
                createSyketilfelleDag(mandag.plusDays(3), null),
                createSyketilfelleDag(mandag.plusDays(4), null),
                createSyketilfelleDag(mandag.plusDays(5), null),
                createSyketilfelleDag(mandag.plusDays(6), null),
                createSyketilfelleDag(mandag.plusDays(7), null),
                createSyketilfelleDag(mandag.plusDays(8), null),
                createSyketilfelleDag(mandag.plusDays(9), null),
                createSyketilfelleDag(mandag.plusDays(10), null),
                createSyketilfelleDag(mandag.plusDays(11), null),
                createSyketilfelleDag(mandag.plusDays(12), null),
                createSyketilfelleDag(mandag.plusDays(13), null),
                createSyketilfelleDag(mandag.plusDays(14), null),
                createSyketilfelleDag(mandag.plusDays(15), null),
                createSyketilfelleDag(mandag.plusDays(16), null),
                createSyketilfelleDag(mandag.plusDays(17), null),
                createSyketilfelleDag(
                    mandag.plusDays(18),
                    Syketilfellebit(
                        id = "id",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        opprettet = mandagMorgen,
                        inntruffet = mandagMorgen,
                        tags = setOf(SYKMELDING, SENDT, PERIODE, INGEN_AKTIVITET),
                        ressursId = "ressursId",
                        fom = mandag.plusDays(18),
                        tom = mandag.plusDays(18),
                    ),
                ),
            )

        val tilfeller = grupperIOppfolgingstilfeller(tidslinje)

        assertThat(tilfeller).hasSize(2)
        assertThat(tilfeller[0].antallDager()).isEqualTo(1)
        assertThat(tilfeller[1].antallDager()).isEqualTo(1)
    }

    @Test
    fun grupperIOppfolgingstilfellerEttTilfelle14DagerMellomOgEnFeriedag() {
        val tidslinje =
            listOf(
                createSyketilfelleDag(
                    mandag,
                    Syketilfellebit(
                        id = "id",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        opprettet = mandagMorgen,
                        inntruffet = mandagMorgen,
                        tags = setOf(SYKMELDING, SENDT, PERIODE, INGEN_AKTIVITET),
                        ressursId = "ressursId",
                        fom = mandag,
                        tom = mandag.plusDays(1),
                    ),
                ),
                createSyketilfelleDag(mandag.plusDays(1), null),
                createSyketilfelleDag(mandag.plusDays(2), null),
                createSyketilfelleDag(mandag.plusDays(3), null),
                createSyketilfelleDag(mandag.plusDays(4), null),
                createSyketilfelleDag(mandag.plusDays(5), null),
                createSyketilfelleDag(mandag.plusDays(6), null),
                createSyketilfelleDag(mandag.plusDays(7), null),
                createSyketilfelleDag(mandag.plusDays(8), null),
                createSyketilfelleDag(mandag.plusDays(9), null),
                createSyketilfelleDag(mandag.plusDays(10), null),
                createSyketilfelleDag(mandag.plusDays(11), null),
                createSyketilfelleDag(mandag.plusDays(12), null),
                createSyketilfelleDag(mandag.plusDays(13), null),
                createSyketilfelleDag(mandag.plusDays(14), null),
                createSyketilfelleDag(
                    mandag.plusDays(15),
                    Syketilfellebit(
                        id = "id",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        opprettet = mandagMorgen,
                        inntruffet = mandagMorgen,
                        tags = setOf(SYKEPENGESOKNAD, FERIE),
                        ressursId = "ressursId",
                        fom = mandag.plusDays(15),
                        tom = mandag.plusDays(15),
                    ),
                ),
                createSyketilfelleDag(
                    mandag.plusDays(16),
                    Syketilfellebit(
                        id = "id",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        opprettet = mandagMorgen,
                        inntruffet = mandagMorgen,
                        tags = setOf(SYKMELDING, SENDT, PERIODE, INGEN_AKTIVITET),
                        ressursId = "ressursId",
                        fom = mandag.plusDays(16),
                        tom = mandag.plusDays(16),
                    ),
                ),
            )

        val tilfeller = grupperIOppfolgingstilfeller(tidslinje)

        assertThat(tilfeller).hasSize(1)
        assertThat(tilfeller[0].antallDager()).isEqualTo(2)
    }

    @Test
    fun grupperIOppfolgingstilfellerEttTilfelle1Sykedag14Arbeidsdager1Feriedag1Sykedag1Arbeidsdag1Sykedag() {
        val tidslinje =
            listOf(
                createSyketilfelleDag(
                    mandag,
                    Syketilfellebit(
                        id = "id",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        opprettet = mandagMorgen,
                        inntruffet = mandagMorgen,
                        tags = setOf(SYKMELDING, SENDT, PERIODE, INGEN_AKTIVITET),
                        ressursId = "ressursId",
                        fom = mandag,
                        tom = mandag.plusDays(1),
                    ),
                ),
                createSyketilfelleDag(mandag.plusDays(1), null),
                createSyketilfelleDag(mandag.plusDays(2), null),
                createSyketilfelleDag(mandag.plusDays(3), null),
                createSyketilfelleDag(mandag.plusDays(4), null),
                createSyketilfelleDag(mandag.plusDays(5), null),
                createSyketilfelleDag(mandag.plusDays(6), null),
                createSyketilfelleDag(mandag.plusDays(7), null),
                createSyketilfelleDag(mandag.plusDays(8), null),
                createSyketilfelleDag(mandag.plusDays(9), null),
                createSyketilfelleDag(mandag.plusDays(10), null),
                createSyketilfelleDag(mandag.plusDays(11), null),
                createSyketilfelleDag(mandag.plusDays(12), null),
                createSyketilfelleDag(mandag.plusDays(13), null),
                createSyketilfelleDag(mandag.plusDays(14), null),
                createSyketilfelleDag(
                    mandag.plusDays(15),
                    Syketilfellebit(
                        id = "id",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        opprettet = mandagMorgen,
                        inntruffet = mandagMorgen,
                        tags = setOf(SYKEPENGESOKNAD, FERIE),
                        ressursId = "ressursId",
                        fom = mandag.plusDays(15),
                        tom = mandag.plusDays(15),
                    ),
                ),
                createSyketilfelleDag(
                    mandag.plusDays(16),
                    Syketilfellebit(
                        id = "id",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        opprettet = mandagMorgen,
                        inntruffet = mandagMorgen,
                        tags = setOf(SYKMELDING, SENDT, PERIODE, INGEN_AKTIVITET),
                        ressursId = "ressursId",
                        fom = mandag.plusDays(16),
                        tom = mandag.plusDays(16),
                    ),
                ),
                createSyketilfelleDag(mandag.plusDays(17), null),
                createSyketilfelleDag(
                    mandag.plusDays(18),
                    Syketilfellebit(
                        id = "id",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        opprettet = mandagMorgen,
                        inntruffet = mandagMorgen,
                        tags = setOf(SYKMELDING, SENDT, PERIODE, INGEN_AKTIVITET),
                        ressursId = "ressursId",
                        fom = mandag.plusDays(18),
                        tom = mandag.plusDays(18),
                    ),
                ),
            )

        val tilfeller = grupperIOppfolgingstilfeller(tidslinje)

        assertThat(tilfeller).hasSize(1)
        assertThat(tilfeller[0].antallDager()).isEqualTo(3)
    }

    @Test
    fun grupperIOppfolgingstilfellerEttTilfelle1EgenmeldingsdagFredag1SykedagMandag() {
        val fredag = mandag.plusDays(4)
        val tidslinje =
            listOf(
                createSyketilfelleDag(
                    fredag,
                    Syketilfellebit(
                        id = "id",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        opprettet = fredag.osloStartOfDay(),
                        inntruffet = fredag.osloStartOfDay(),
                        tags = setOf(SYKEPENGESOKNAD, EGENMELDING),
                        ressursId = "ressursId",
                        fom = fredag,
                        tom = fredag,
                    ),
                ),
                createSyketilfelleDag(
                    fredag.plusDays(3),
                    Syketilfellebit(
                        id = "id",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        opprettet = fredag.osloStartOfDay(),
                        inntruffet = fredag.osloStartOfDay(),
                        tags = setOf(SYKMELDING, SENDT, PERIODE, INGEN_AKTIVITET),
                        ressursId = "ressursId",
                        fom = fredag.plusDays(3),
                        tom = fredag.plusDays(3),
                    ),
                ),
            )

        val tilfeller = grupperIOppfolgingstilfeller(tidslinje)

        assertThat(tilfeller).hasSize(1)
        assertThat(tilfeller[0].antallDager()).isEqualTo(2)
    }

    @Test
    fun grupperIOppfolgingstilfellerEttTilfelle1EgenmeldingsdagFredag() {
        val fredag = mandag.plusDays(4)
        val tidslinje =
            listOf(
                createSyketilfelleDag(
                    fredag,
                    Syketilfellebit(
                        id = "id",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        opprettet = fredag.osloStartOfDay(),
                        inntruffet = fredag.osloStartOfDay(),
                        tags = setOf(SYKEPENGESOKNAD, EGENMELDING),
                        ressursId = "ressursId",
                        fom = fredag,
                        tom = fredag,
                    ),
                ),
            )

        val tilfeller = grupperIOppfolgingstilfeller(tidslinje)

        assertThat(tilfeller).hasSize(1)
        assertThat(tilfeller[0].antallDager()).isEqualTo(1)
    }

    @Test
    fun grupperIOppfolgingstilfellerEttTilfelle3Dager1Ukjent1Full1Ingen1Gradert() {
        val tidslinje =
            listOf(
                createSyketilfelleDag(
                    mandag,
                    Syketilfellebit(
                        id = "id",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        opprettet = mandagMorgen,
                        inntruffet = mandagMorgen,
                        tags = setOf(SYKMELDING, PERIODE, UKJENT_AKTIVITET),
                        ressursId = "ressursId",
                        fom = mandag,
                        tom = mandag,
                    ),
                ),
                createSyketilfelleDag(
                    mandag.plusDays(1),
                    Syketilfellebit(
                        id = "id",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        opprettet = mandagMorgen,
                        inntruffet = mandagMorgen,
                        tags = setOf(SYKMELDING, PERIODE, FULL_AKTIVITET),
                        ressursId = "ressursId",
                        fom = mandag.plusDays(1),
                        tom = mandag.plusDays(1),
                    ),
                ),
                createSyketilfelleDag(
                    mandag.plusDays(2),
                    Syketilfellebit(
                        id = "id",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        opprettet = mandagMorgen,
                        inntruffet = mandagMorgen,
                        tags = setOf(SYKMELDING, PERIODE, INGEN_AKTIVITET),
                        ressursId = "ressursId",
                        fom = mandag.plusDays(2),
                        tom = mandag.plusDays(2),
                    ),
                ),
                createSyketilfelleDag(
                    mandag.plusDays(3),
                    Syketilfellebit(
                        id = "id",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        opprettet = mandagMorgen,
                        inntruffet = mandagMorgen,
                        tags = setOf(SYKMELDING, PERIODE, GRADERT_AKTIVITET),
                        ressursId = "ressursId",
                        fom = mandag.plusDays(3),
                        tom = mandag.plusDays(3),
                    ),
                ),
            )

        val tilfeller = grupperIOppfolgingstilfeller(tidslinje)

        assertThat(tilfeller).hasSize(1)
        assertThat(tilfeller[0].antallDager()).isEqualTo(3)
    }

    @Test
    fun grupperIOppfolgingstilfellerEttTilfelle1Dag1Full1ArbeidGjenopptatt() {
        val tidslinje =
            listOf(
                createSyketilfelleDag(
                    mandag,
                    Syketilfellebit(
                        id = "id",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        opprettet = mandagMorgen,
                        inntruffet = mandagMorgen,
                        tags = setOf(SYKMELDING, PERIODE, UKJENT_AKTIVITET),
                        ressursId = "ressursId",
                        fom = mandag,
                        tom = mandag,
                    ),
                ),
                createSyketilfelleDag(
                    mandag.plusDays(1),
                    Syketilfellebit(
                        id = "id",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        opprettet = mandagMorgen,
                        inntruffet = mandagMorgen,
                        tags = setOf(SYKEPENGESOKNAD, ARBEID_GJENNOPPTATT),
                        ressursId = "ressursId",
                        fom = mandag.plusDays(1),
                        tom = mandag.plusDays(1),
                    ),
                ),
            )

        val tilfeller = grupperIOppfolgingstilfeller(tidslinje)

        assertThat(tilfeller).hasSize(1)
        assertThat(tilfeller[0].antallDager()).isEqualTo(1)
    }

    @Test
    fun grupperIOppfolgingstilfellerEttTilfelle2Dager1Full1Papirsykmelding() {
        val tidslinje =
            listOf(
                createSyketilfelleDag(
                    mandag,
                    Syketilfellebit(
                        id = "id",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        opprettet = mandagMorgen,
                        inntruffet = mandagMorgen,
                        tags = setOf(SYKMELDING, PERIODE, UKJENT_AKTIVITET),
                        ressursId = "ressursId",
                        fom = mandag,
                        tom = mandag,
                    ),
                ),
                createSyketilfelleDag(
                    mandag.plusDays(1),
                    Syketilfellebit(
                        id = "id",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        opprettet = mandagMorgen,
                        inntruffet = mandagMorgen,
                        tags = setOf(SYKEPENGESOKNAD, PAPIRSYKMELDING),
                        ressursId = "ressursId",
                        fom = mandag.plusDays(1),
                        tom = mandag.plusDays(1),
                    ),
                ),
            )

        val tilfeller = grupperIOppfolgingstilfeller(tidslinje)

        assertThat(tilfeller).hasSize(1)
        assertThat(tilfeller[0].antallDager()).isEqualTo(2)
    }
}

fun LocalDate.osloStartOfDay(): OffsetDateTime = this.atStartOfDay(osloZone).toOffsetDateTime()
