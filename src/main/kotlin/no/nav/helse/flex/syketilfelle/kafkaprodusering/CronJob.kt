package no.nav.helse.flex.syketilfelle.kafkaprodusering

import no.nav.helse.flex.syketilfelle.logger
import no.nav.helse.flex.syketilfelle.syketilfellebit.SyketilfellebitRepository
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit

@Component
@Profile("default")
class CronJob(
    val kafkaProduseringJob: KafkaProduseringJob,
    val leaderElection: LeaderElection,
    val syketilfellebitRepository: SyketilfellebitRepository
) {
    val log = logger()

    @Scheduled(initialDelay = 60 * 2, fixedDelay = 1, timeUnit = TimeUnit.SECONDS)
    fun run() {
        if (leaderElection.isLeader()) {
            try {
                kafkaProduseringJob.publiser()
            } catch (e: Exception) {
                log.error("Feil i cronjob", e)
            }
        }
    }

    @Scheduled(initialDelay = 3, fixedDelay = 60, timeUnit = TimeUnit.MINUTES)
    fun slettSyketilfellebiter() {
        if (leaderElection.isLeader()) {
            try {
                val biter = syketilfellebitRepository.findAllById(
                    listOf(
                        "36050d1d-b1bf-47fa-bbab-01d1a7b0f91b",
                        "c1c4e1b8-fe17-46a6-bab9-39272788141b"
                    )
                ).toList()

                assert(biter.size == 2)

                val bit1 = biter.first { it.id == "36050d1d-b1bf-47fa-bbab-01d1a7b0f91b" }
                assert(bit1.fom == LocalDate.of(2022, 11, 21))
                assert(bit1.tom == LocalDate.of(2023, 1, 9))

                val bit2 = biter.first { it.id == "c1c4e1b8-fe17-46a6-bab9-39272788141b" }
                assert(bit2.fom == LocalDate.of(2022, 11, 21))
                assert(bit2.tom == LocalDate.of(2023, 1, 9))

                val now = OffsetDateTime.now()

                if (bit1.slettet == null) {
                    syketilfellebitRepository.save(bit1.copy(slettet = now))
                }

                if (bit2.slettet == null) {
                    syketilfellebitRepository.save(bit2.copy(slettet = now))
                }
            } catch (e: Exception) {
                log.error("Kunne ikke slettet syketilfellebiter", e)
            }
        }
    }
}
