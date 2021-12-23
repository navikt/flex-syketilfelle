package no.nav.helse.flex.syketilfelle.syketilfellebit

import io.micrometer.core.instrument.MeterRegistry
import no.nav.helse.flex.syketilfelle.kafka.KafkaSyketilfellebit
import org.springframework.stereotype.Component

@Component
class SyketilfellebitMottak(val syketilfellebitRepository: SyketilfellebitRepository, registry: MeterRegistry) {

    val mottattSyketilfellebit = registry.counter("mottatt_syketilfellebit_counter")

    fun mottaBit(kafkaSyketilfellebit: KafkaSyketilfellebit) {
        if (syketilfellebitRepository.existsBySyketilfellebitId(kafkaSyketilfellebit.id)) {
            return
        }
        syketilfellebitRepository.save(kafkaSyketilfellebit.tilSyketilfellebit())
        mottattSyketilfellebit.increment()
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
