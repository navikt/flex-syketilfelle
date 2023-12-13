package no.nav.helse.flex.syketilfelle.soknad

import no.nav.helse.flex.sykepengesoknad.kafka.*
import no.nav.helse.flex.sykepengesoknad.kafka.SoknadstypeDTO.*
import no.nav.helse.flex.syketilfelle.logger
import no.nav.helse.flex.syketilfelle.syketilfellebit.SyketilfellebitLagring
import org.springframework.stereotype.Service

@Service
class SykepengesoknadLagring(
    private val syketilfellebitLagring: SyketilfellebitLagring,
) {
    val log = logger()

    fun lagreBiterFraSoknad(soknad: SykepengesoknadDTO) {
        if (soknad.skalBehandles()) {
            log.info("Behandler soknad ${soknad.id}")
            if (soknad.status == SoknadsstatusDTO.SENDT) {
                val biter = soknad.mapSoknadTilBiter()
                syketilfellebitLagring.lagreBiter(biter)
            }
        }
    }

    private fun SykepengesoknadDTO.skalBehandles(): Boolean {
        return when (this.type) {
            ARBEIDSTAKERE -> true
            BEHANDLINGSDAGER, GRADERT_REISETILSKUDD -> {
                this.arbeidssituasjon == ArbeidssituasjonDTO.ARBEIDSTAKER
            }
            else -> false
        }
    }
}
