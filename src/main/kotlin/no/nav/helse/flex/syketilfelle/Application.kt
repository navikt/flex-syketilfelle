package no.nav.helse.flex.syketilfelle

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.resilience.annotation.EnableResilientMethods
import org.springframework.scheduling.annotation.EnableScheduling
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule

@SpringBootApplication
@EnableScheduling
@EnableResilientMethods
@EnableJwtTokenValidation
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}

inline fun <reified T> T.logger(): Logger = LoggerFactory.getLogger(T::class.java)

val objectMapper: ObjectMapper =
    JsonMapper
        .builder()
        .addModule(kotlinModule())
        .build()

// Fjerner kontroll- og formateringstegn (Cc/Cf) fra teksten for å hindre logginjeksjon.
fun String.sanitizeForLog() = replace(Regex("[\\p{Cc}\\p{Cf}]"), " ").trim()
