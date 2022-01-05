package no.nav.helse.flex.syketilfelle.soknad

import no.nav.helse.flex.syketilfelle.syketilfellebit.SyketilfellebitLagring
import no.nav.syfo.kafka.felles.ArbeidssituasjonDTO
import no.nav.syfo.kafka.felles.SoknadsstatusDTO
import no.nav.syfo.kafka.felles.SoknadstypeDTO.*
import no.nav.syfo.kafka.felles.SykepengesoknadDTO
import org.springframework.stereotype.Service

@Service
class SykepengesoknadLagring(
    private val syketilfellebitLagring: SyketilfellebitLagring,
) {

    fun lagreBiterFraSoknad(soknad: SykepengesoknadDTO) {

        if (soknad.skalBehandles()) {

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
