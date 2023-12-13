package no.nav.helse.flex.syketilfelle.kafkaprodusering

import no.nav.helse.flex.syketilfelle.logger
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class TombstoneCronJob(
    val tombsstoneProduseringJob: TombsstoneProduseringJob,
    val leaderElection: LeaderElection,
) {
    val log = logger()

    @Scheduled(initialDelay = 60 * 2, fixedDelay = 30, timeUnit = TimeUnit.SECONDS)
    fun run() {
        if (leaderElection.isLeader()) {
            try {
                tombsstoneProduseringJob.publiser()
            } catch (e: Exception) {
                log.error("Feil i cronjob", e)
            }
        }
    }
}
