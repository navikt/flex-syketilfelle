package no.nav.helse.flex.syketilfelle.kafkaprodusering

import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.flex.syketilfelle.syketilfellebit.SyketilfellebitRepository
import org.springframework.stereotype.Component
import java.time.OffsetDateTime

@Component
class TombsstoneProduseringJob(
    val syketilfellebitRepository: SyketilfellebitRepository,
    val syketilfellebitKafkaProducer: SyketilfellebitKafkaProducer,
    registry: MeterRegistry
) {
    val publisertSyketilfellebit = registry.counter("publisert_syketilfellebit_counter")

    fun publiser() {
        syketilfellebitRepository.findFirst300ByTombstonePublistertIsNullAndSlettetIsNotNullOrderByOpprettetAsc()
            .filter { it.publisert } // Tombstone skal kun produseres for publiserte syketilfellebiter
            .forEach {
                syketilfellebitKafkaProducer.produserTombstone(it.syketilfellebitId, it.fnr)
                syketilfellebitRepository.save(it.copy(tombstonePublistert = OffsetDateTime.now()))
                publisertSyketilfellebit.increment()
            }
    }
}
