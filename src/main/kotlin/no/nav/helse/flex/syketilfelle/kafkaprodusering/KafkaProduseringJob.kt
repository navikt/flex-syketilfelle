package no.nav.helse.flex.syketilfelle.kafkaprodusering

import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.flex.syketilfelle.syketilfellebit.SyketilfellebitRepository
import no.nav.helse.flex.syketilfelle.syketilfellebit.tilKafkasyketilfellebit
import org.springframework.stereotype.Component

@Component
class KafkaProduseringJob(
    val syketilfellebitRepository: SyketilfellebitRepository,
    val syketilfellebitKafkaProducer: SyketilfellebitKafkaProducer,
    registry: MeterRegistry,
) {
    val publisertSyketilfellebit = registry.counter("publisert_syketilfellebit_counter")

    fun publiser() {
        syketilfellebitRepository.findFirst300ByPublisertOrderByOpprettetAsc(false)
            .forEach {
                syketilfellebitKafkaProducer.produserMelding(it.tilKafkasyketilfellebit())
                syketilfellebitRepository.save(it.copy(publisert = true))
                publisertSyketilfellebit.increment()
            }
    }
}
