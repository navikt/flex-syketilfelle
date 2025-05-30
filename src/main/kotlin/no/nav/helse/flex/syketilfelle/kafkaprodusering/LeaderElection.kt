package no.nav.helse.flex.syketilfelle.kafkaprodusering

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.helse.flex.syketilfelle.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.availability.ApplicationAvailability
import org.springframework.boot.availability.LivenessState
import org.springframework.boot.availability.ReadinessState
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.ConnectException
import java.net.InetAddress

@Component
class LeaderElection(
    private val plainTextUtf8RestTemplate: RestTemplate,
    @param:Value("\${elector.path}") private val electorPath: String,
    private val applicationAvailability: ApplicationAvailability,
) {
    val log = logger()

    fun isLeader(): Boolean {
        if (applicationAvailability.readinessState == ReadinessState.REFUSING_TRAFFIC ||
            applicationAvailability.livenessState == LivenessState.BROKEN
        ) {
            log.info(
                "Ser ikke etter leader med readiness [ ${applicationAvailability.readinessState} ] og " +
                    "liveness [ ${applicationAvailability.livenessState} ]",
            )
            return false
        }

        if (electorPath == "dont_look_for_leader") {
            log.info("Ser ikke etter leader, returnerer at jeg er leader")
            return true
        }
        return kallElector()
    }

    private fun kallElector(): Boolean {
        try {
            val hostname: String = InetAddress.getLocalHost().hostName

            val uriString =
                UriComponentsBuilder
                    .fromUriString(getHttpPath(electorPath))
                    .toUriString()
            val result =
                plainTextUtf8RestTemplate
                    .exchange(
                        uriString,
                        HttpMethod.GET,
                        null,
                        String::class.java,
                    )
            if (result.statusCode != HttpStatus.OK) {
                val message = "Kall mot elector feiler med HTTP-" + result.statusCode
                log.error(message)
                throw RuntimeException(message)
            }

            result.body?.let {
                val leader: Leader = objectMapper.readValue(it)
                return leader.name == hostname
            }

            val message = "Kall mot elector returnerer ikke data"
            log.error(message)
            throw RuntimeException(message)
        } catch (e: ResourceAccessException) {
            if (e.cause is ConnectException) {
                return false
            }
            throw e
        }
    }

    private fun getHttpPath(url: String): String =
        when (url.startsWith("http://")) {
            true -> url
            else -> "http://$url"
        }

    private data class Leader(
        val name: String,
    )

    private val objectMapper =
        ObjectMapper()
            .registerModule(JavaTimeModule())
            .registerKotlinModule()
            .configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE, true)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
}
