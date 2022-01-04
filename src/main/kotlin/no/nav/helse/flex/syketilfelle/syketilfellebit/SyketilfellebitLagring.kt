package no.nav.helse.flex.syketilfelle.syketilfellebit

import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class SyketilfellebitLagring(
    val syketilfellebitRepository: SyketilfellebitRepository,
) {

    fun lagreBiter(syketilfellebiter: List<Syketilfellebit>) {
        if (syketilfellebiter.isEmpty()) {
            return
        }
        val dbBiter = syketilfellebiter.map { it.tilSyketilfellebitDbRecord() }
        val eksisterendeBiter = syketilfellebitRepository
            .findByFnr(dbBiter.first().fnr)
            .map { it.tilSammenlikner() }

        val nyeBiter = dbBiter.filterNot { b ->
            eksisterendeBiter.any { it == b.tilSammenlikner() }
        }
        syketilfellebitRepository.saveAll(nyeBiter)
    }
}

fun SyketilfellebitDbRecord.tilSammenlikner(): SyketilfellebitSammenlikner {
    return SyketilfellebitSammenlikner(
        tags = tags.tagsFromString(),
        ressursId = ressursId,
        fom = fom,
        tom = tom,
    )
}

data class SyketilfellebitSammenlikner(
    val tags: Set<Tag>,
    val ressursId: String,
    val fom: LocalDate,
    val tom: LocalDate
)
