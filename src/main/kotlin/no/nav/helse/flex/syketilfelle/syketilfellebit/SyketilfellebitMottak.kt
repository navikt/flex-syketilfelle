package no.nav.helse.flex.syketilfelle.syketilfellebit

import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.flex.syketilfelle.logger
import org.springframework.stereotype.Component
import kotlin.system.measureTimeMillis

@Component
class SyketilfellebitMottak(
    val syketilfellebitBatchInsertDAO: SyketilfellebitBatchInsertDAO,
    registry: MeterRegistry,
) {
    val log = logger()

    val mottattSyketilfellebit = registry.counter("mottatt_syketilfellebit_counter")

    fun mottaBitListe(kafkaSyketilfellebiter: List<KafkaSyketilfellebit>) {

        val unikeBiterInn = kafkaSyketilfellebiter.distinctBy { it.id }
        if (unikeBiterInn.isEmpty()) {
            return
        }
        val elapsed = measureTimeMillis {
            syketilfellebitBatchInsertDAO.batchInsert(unikeBiterInn.map { it.tilSyketilfellebit() })
            mottattSyketilfellebit.increment(unikeBiterInn.size.toDouble())
        }
        log.info("Behandlet ${kafkaSyketilfellebiter.size} biter fra kafka i $elapsed millis")
    }
}

private fun KafkaSyketilfellebit.tilSyketilfellebit(): SyketilfellebitDbRecord = SyketilfellebitDbRecord(
    id = null,
    syketilfellebitId = id,
    fnr = fnr,
    orgnummer = orgnummer,
    opprettet = opprettet,
    inntruffet = inntruffet,
    tags = tags.asString(),
    ressursId = ressursId,
    fom = fom,
    tom = tom,
    korrigererSendtSoknad = korrigererSendtSoknad,
    publisert = true,
)
