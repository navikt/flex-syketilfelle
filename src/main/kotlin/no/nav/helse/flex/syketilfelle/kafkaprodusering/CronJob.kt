package no.nav.helse.flex.syketilfelle.kafkaprodusering

import no.nav.helse.flex.syketilfelle.logger
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
@Profile("default")
class CronJob(
    val kafkaProduseringJob: KafkaProduseringJob,
    val leaderElection: LeaderElection,

) {
    val log = logger()

    @Scheduled(initialDelay = 60 * 2, fixedDelay = 1, timeUnit = TimeUnit.SECONDS)
    fun run() {
        val leader = leaderElection.isLeader()
        log.info("publisertSyketilfellebit leader = $leader")

        if (leader) {
            try {
                kafkaProduseringJob.publiser()
            } catch (e: Exception) {
                log.error("Feil i cronjob", e)
            }
        }
    }
}
