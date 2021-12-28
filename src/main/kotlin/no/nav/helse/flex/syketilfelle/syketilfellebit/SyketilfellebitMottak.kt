package no.nav.helse.flex.syketilfelle.syketilfellebit

import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.flex.syketilfelle.kafka.KafkaSyketilfellebit
import no.nav.helse.flex.syketilfelle.logger
import org.springframework.stereotype.Component

@Component
class SyketilfellebitMottak(
    val syketilfellebitRepository: SyketilfellebitRepository,
    val syketilfellebitBatchInsertDAO: SyketilfellebitBatchInsertDAO,
    registry: MeterRegistry,
) {
    val log = logger()

    val mottattSyketilfellebit = registry.counter("mottatt_syketilfellebit_counter")

    fun mottaBitListe(kafkaSyketilfellebiter: List<KafkaSyketilfellebit>) {
        log.info("Behandlet ${kafkaSyketilfellebiter.size} biter fra kafka")

        val unikeBiterInn = kafkaSyketilfellebiter.distinctBy { it.id }
        if (unikeBiterInn.isEmpty()) {
            return
        }
        val eksisterendeBiter = syketilfellebitRepository
            .findBySyketilfellebitIdIn(unikeBiterInn.map { it.id })
            .map { it.syketilfellebitId }
            .toSet()

        val biterSomSkalOpprettes = unikeBiterInn
            .filter { !eksisterendeBiter.contains(it.id) }
            .map { it.tilSyketilfellebit() }

        if (biterSomSkalOpprettes.isEmpty()) {
            return
        }
        syketilfellebitBatchInsertDAO.batchInsert(biterSomSkalOpprettes)
        mottattSyketilfellebit.increment(biterSomSkalOpprettes.size.toDouble())
    }
}

private fun KafkaSyketilfellebit.tilSyketilfellebit(): Syketilfellebit = Syketilfellebit(
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
    korrigererSendtSoknad = korrigererSendtSoknad
)

fun List<String>.asString() = this.joinToString(",")

fun String.tagsFromString() = split(',').map(String::trim)
