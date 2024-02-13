package no.nav.helse.flex.syketilfelle.arbeidsgiverperiode

import no.nav.helse.flex.sykepengesoknad.kafka.ArbeidsgiverDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadsstatusDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadstypeDTO
import no.nav.helse.flex.sykepengesoknad.kafka.SykepengesoknadDTO
import no.nav.helse.flex.syketilfelle.FellesTestOppsett
import no.nav.helse.flex.syketilfelle.kallArbeidsgiverperiodeApi
import no.nav.helse.flex.syketilfelle.opprettMottattSykmelding
import no.nav.helse.flex.syketilfelle.opprettSendtSykmelding
import no.nav.helse.flex.syketilfelle.sykmelding.skapArbeidsgiverSykmelding
import no.nav.helse.flex.syketilfelle.ventPåRecords
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDate
import java.util.*

class ArbeidsgiverperiodeMedFlereArbeidsgivereTest : FellesTestOppsett() {
    private final val fnr = "11111555555"
    private final val orgnr1 = "888888888"
    private final val orgnr2 = "999999999"
    private final val basisDato = LocalDate.of(2022, 6, 30)

    @BeforeEach
    @AfterEach
    fun setUp() {
        syketilfellebitRepository.deleteAll()
    }

    @Test
    fun `arbrbeidsgiverperioden regnes ut per arbeidsgiver`() {
        val sykmelding = skapArbeidsgiverSykmelding(fom = basisDato.minusDays(12), tom = basisDato)
        val sykmelding2 = skapArbeidsgiverSykmelding(fom = basisDato.minusDays(24), tom = basisDato)

        opprettMottattSykmelding(sykmelding = sykmelding, fnr = fnr)
        opprettMottattSykmelding(sykmelding = sykmelding2, fnr = fnr)
        opprettSendtSykmelding(sykmelding = sykmelding, fnr = fnr, orgnummer = orgnr1)
        opprettSendtSykmelding(sykmelding = sykmelding2, fnr = fnr, orgnummer = orgnr2)

        val soknad1 =
            SykepengesoknadDTO(
                id = UUID.randomUUID().toString(),
                arbeidsgiver = ArbeidsgiverDTO(navn = "navn", orgnummer = orgnr1),
                fravar = emptyList(),
                andreInntektskilder = emptyList(),
                fom = basisDato.minusDays(12),
                tom = basisDato,
                arbeidGjenopptatt = null,
                egenmeldinger = emptyList(),
                fnr = fnr,
                status = SoknadsstatusDTO.SENDT,
                sykmeldingId = sykmelding.id,
                type = SoknadstypeDTO.ARBEIDSTAKERE,
            )

        val ag1 = kallArbeidsgiverperiodeApi(soknad = soknad1, forelopig = false, fnr = fnr)!!
        assertThat(ag1.oppbruktArbeidsgiverperiode).isEqualTo(false)
        assertThat(ag1.arbeidsgiverPeriode.fom).isEqualTo(soknad1.fom)
        assertThat(ag1.arbeidsgiverPeriode.tom).isEqualTo(soknad1.tom)

        val soknad2 =
            SykepengesoknadDTO(
                id = UUID.randomUUID().toString(),
                arbeidsgiver = ArbeidsgiverDTO(navn = "navn", orgnummer = orgnr2),
                fravar = emptyList(),
                andreInntektskilder = emptyList(),
                fom = basisDato.minusDays(24),
                tom = basisDato,
                arbeidGjenopptatt = null,
                egenmeldinger = emptyList(),
                fnr = fnr,
                status = SoknadsstatusDTO.SENDT,
                sykmeldingId = sykmelding2.id,
                type = SoknadstypeDTO.ARBEIDSTAKERE,
            )

        val ag2 = kallArbeidsgiverperiodeApi(soknad = soknad2, forelopig = false, fnr = fnr)!!
        assertThat(ag2.oppbruktArbeidsgiverperiode).isEqualTo(true)
        assertThat(ag2.arbeidsgiverPeriode.fom).isEqualTo(soknad2.fom)
        assertThat(ag2.arbeidsgiverPeriode.tom).isEqualTo(soknad2.fom!!.plusDays(15))

        juridiskVurderingKafkaConsumer.ventPåRecords(antall = 2, duration = Duration.ofSeconds(5))
    }
}
