package no.nav.helse.flex.syketilfelle.inntektsmelding

import no.nav.helse.flex.syketilfelle.extensions.tilOsloZone
import no.nav.helse.flex.syketilfelle.logger
import no.nav.helse.flex.syketilfelle.syketilfellebit.Syketilfellebit
import no.nav.helse.flex.syketilfelle.syketilfellebit.SyketilfellebitLagring
import no.nav.helse.flex.syketilfelle.syketilfellebit.Tag
import no.nav.inntektsmeldingkontrakt.Arbeidsgivertype
import no.nav.inntektsmeldingkontrakt.Inntektsmelding
import no.nav.inntektsmeldingkontrakt.Status
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

@Service
class InntektsmeldingLagring(
    private val syketilfellebitLagring: SyketilfellebitLagring,
) {

    val log = logger()

    fun mottaInntektsmelding(inntektsmelding: Inntektsmelding) {
        if (inntektsmelding.arbeidsgivertype != Arbeidsgivertype.VIRKSOMHET) {
            return
        }
        if (inntektsmelding.status != Status.GYLDIG) {
            return
        }
        val biter = inntektsmelding.mapTilBiter()
        syketilfellebitLagring.lagreBiter(biter)
    }
}

private fun Inntektsmelding.mapTilBiter(): List<Syketilfellebit> = this.arbeidsgiverperioder.map {
    Syketilfellebit(
        fnr = this.arbeidstakerFnr,
        opprettet = OffsetDateTime.now(),
        inntruffet = this.mottattDato.tilOsloZone(),
        fom = it.fom,
        tom = it.tom,
        orgnummer = this.virksomhetsnummer,
        ressursId = this.inntektsmeldingId,
        tags = setOf(Tag.INNTEKTSMELDING, Tag.ARBEIDSGIVERPERIODE)
    )
}
