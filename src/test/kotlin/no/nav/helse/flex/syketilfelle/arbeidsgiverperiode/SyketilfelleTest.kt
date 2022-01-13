package no.nav.helse.flex.syketilfelle.arbeidsgiverperiode

import no.nav.helse.flex.syketilfelle.syketilfellebit.Syketilfellebit
import no.nav.helse.flex.syketilfelle.syketilfellebit.Tag.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.OffsetDateTime

class SyketilfelleTest {

    @Test
    fun genererOppfolgingstilfelleFiltrerBortKorrigerteBiter() {
        val biter =
            listOf(
                Syketilfellebit(
                    id = "id",
                    fnr = "fnr",
                    orgnummer = "orgnummer",
                    opprettet = OffsetDateTime.now(),
                    inntruffet = OffsetDateTime.now(),
                    tags = setOf(SYKEPENGESOKNAD, SENDT, KORRIGERT_ARBEIDSTID, GRADERT_AKTIVITET),
                    ressursId = "korrigertRessursId",
                    fom = LocalDate.now().minusWeeks(1),
                    tom = LocalDate.now()
                ),
                Syketilfellebit(
                    id = "id",
                    fnr = "fnr",
                    orgnummer = "orgnummer",
                    opprettet = OffsetDateTime.now(),
                    inntruffet = OffsetDateTime.now(),
                    tags = setOf(SYKEPENGESOKNAD, KORRIGERT, KORRIGERT_ARBEIDSTID, GRADERT_AKTIVITET),
                    ressursId = "korrigertRessursId",
                    fom = LocalDate.now().minusWeeks(1),
                    tom = LocalDate.now()
                ),
                Syketilfellebit(
                    id = "id",
                    fnr = "fnr",
                    orgnummer = "orgnummer",
                    opprettet = OffsetDateTime.now(),
                    inntruffet = OffsetDateTime.now(),
                    tags = setOf(SYKEPENGESOKNAD, SENDT, KORRIGERT_ARBEIDSTID, GRADERT_AKTIVITET),
                    ressursId = "ressursId",
                    fom = LocalDate.now(),
                    tom = LocalDate.now()
                )
            )

        val oppfolgingstilfeller = genererOppfolgingstilfelle(biter)

        assertThat(oppfolgingstilfeller?.get(0)?.tidslinje).hasSize(1)
        assertThat(oppfolgingstilfeller?.get(0)?.tidslinje?.get(0)?.dag).isEqualTo(LocalDate.now())
    }

    @Test
    fun genererOppfolgingstilfelleKorrigerOgFiltrerBortBiterSomTilleggsbiterKorrigerer() {
        val biter = listOf(
            Syketilfellebit(
                id = "id",
                fnr = "fnr",
                orgnummer = "orgnummer",
                opprettet = OffsetDateTime.now(),
                inntruffet = OffsetDateTime.now(),
                tags = setOf(SYKEPENGESOKNAD, SENDT, KORRIGERT_ARBEIDSTID, GRADERT_AKTIVITET),
                ressursId = "korrigertRessursId",
                fom = LocalDate.now().minusWeeks(1),
                tom = LocalDate.now()
            )
        )

        val tilleggsbiter = listOf(
            Syketilfellebit(
                id = "id",
                fnr = "fnr",
                orgnummer = "orgnummer",
                opprettet = OffsetDateTime.now(),
                inntruffet = OffsetDateTime.now(),
                tags = setOf(SYKEPENGESOKNAD, SENDT, KORRIGERT_ARBEIDSTID, GRADERT_AKTIVITET),
                ressursId = "ressursId",
                fom = LocalDate.now(),
                tom = LocalDate.now()
            )
        )

        val oppfolgingstilfeller = genererOppfolgingstilfelle(
            biter,
            listOf("korrigertRessursId"),
            tilleggsbiter
        )

        assertThat(oppfolgingstilfeller?.get(0)?.tidslinje).hasSize(1)
        assertThat(oppfolgingstilfeller?.get(0)?.tidslinje?.get(0)?.dag).isEqualTo(LocalDate.now())
    }

    @Test
    fun genererOppfolgingstilfelleUtdanningSkalIkkePavirkeArbeidsgiverperioden() {
        val biter = listOf(
            Syketilfellebit(
                id = "id",
                fnr = "fnr",
                orgnummer = "orgnummer",
                opprettet = OffsetDateTime.now(),
                inntruffet = OffsetDateTime.now(),
                tags = setOf(SYKEPENGESOKNAD, SENDT),
                ressursId = "korrigertRessursId",
                fom = LocalDate.now().minusWeeks(1),
                tom = LocalDate.now()
            )
        )

        val tilleggsbiter = listOf(
            Syketilfellebit(
                id = "id",
                fnr = "fnr",
                orgnummer = "orgnummer",
                opprettet = OffsetDateTime.now(),
                inntruffet = OffsetDateTime.now(),
                tags = setOf(SYKEPENGESOKNAD, SENDT, UTDANNING, FULLTID),
                ressursId = "ressursId",
                fom = LocalDate.now().minusYears(1),
                tom = LocalDate.now()
            )
        )

        val oppfolgingstilfeller = genererOppfolgingstilfelle(
            biter = biter,
            tilleggsbiter = tilleggsbiter
        )

        assertThat(oppfolgingstilfeller?.get(0)?.tidslinje).hasSize(8)
        assertThat(oppfolgingstilfeller?.get(0)?.tidslinje?.get(0)?.dag).isEqualTo(LocalDate.now().minusWeeks(1))
    }
}
