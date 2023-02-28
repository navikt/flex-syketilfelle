package no.nav.helse.flex.syketilfelle.arbeidsgiverperiode

import no.nav.helse.flex.syketilfelle.arbeidsgiverperiode.ListContainsPredicate.Companion.of
import no.nav.helse.flex.syketilfelle.arbeidsgiverperiode.domain.Syketilfellebiter
import no.nav.helse.flex.syketilfelle.arbeidsgiverperiode.domain.Tidslinje
import no.nav.helse.flex.syketilfelle.syketilfellebit.Syketilfellebit
import no.nav.helse.flex.syketilfelle.syketilfellebit.Tag.*
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.OffsetDateTime

class TidslinjeTest {

    val prioriteringsliste = listOf(of(SYKEPENGESOKNAD) and KORRIGERT_ARBEIDSTID and (of(GRADERT_AKTIVITET) or INGEN_AKTIVITET))

    @Test
    internal fun beregnOppfolgingstilfelleIngenBiter() {
        assertThatThrownBy { Tidslinje(Syketilfellebiter(emptyList(), prioriteringsliste)) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    internal fun beregnOppfolgingstilfelleEttTilfelle() {
        val biter = listOf(
            Syketilfellebit(
                id = "id",
                orgnummer = "orgnummer",
                opprettet = OffsetDateTime.now(),
                inntruffet = OffsetDateTime.now(),
                tags = setOf(SYKEPENGESOKNAD, KORRIGERT_ARBEIDSTID, GRADERT_AKTIVITET),
                ressursId = "ressursId",
                fom = LocalDate.now().minusDays(16),
                tom = LocalDate.now().minusDays(16),
                fnr = "fnr"
            ),
            Syketilfellebit(
                id = "id",
                orgnummer = "orgnummer",
                opprettet = OffsetDateTime.now(),
                inntruffet = OffsetDateTime.now(),
                tags = setOf(SYKEPENGESOKNAD, KORRIGERT_ARBEIDSTID, INGEN_AKTIVITET),
                ressursId = "ressursId",
                fom = LocalDate.now(),
                tom = LocalDate.now(),
                fnr = "fnr"
            )
        )

        val tidslinje = Tidslinje(Syketilfellebiter(biter, prioriteringsliste))

        assertThat(tidslinje.tidslinjeSomListe()).hasSize(17)
    }

    @Test
    internal fun beregnOppfolgingstilfelleToTilfeller() {
        val biter = listOf(
            Syketilfellebit(
                id = "id",
                orgnummer = "orgnummer",
                opprettet = OffsetDateTime.now(),
                inntruffet = OffsetDateTime.now(),
                tags = setOf(SYKEPENGESOKNAD, KORRIGERT_ARBEIDSTID, GRADERT_AKTIVITET),
                ressursId = "ressursId",
                fom = LocalDate.now().minusDays(17),
                tom = LocalDate.now().minusDays(17),
                fnr = "fnr"
            ),
            Syketilfellebit(
                id = "id",
                orgnummer = "orgnummer",
                opprettet = OffsetDateTime.now(),
                inntruffet = OffsetDateTime.now(),
                tags = setOf(SYKEPENGESOKNAD, KORRIGERT_ARBEIDSTID, INGEN_AKTIVITET),
                ressursId = "ressursId",
                fom = LocalDate.now(),
                tom = LocalDate.now(),
                fnr = "fnr"
            )
        )

        val tidslinje = Tidslinje(Syketilfellebiter(biter, prioriteringsliste))

        assertThat(tidslinje.tidslinjeSomListe()).hasSize(18)
    }
}
