package no.nav.helse.flex.syketilfelle.clientidvalidation

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.syketilfelle.clientidvalidation.ClientIdValidation.NamespaceAndApp
import no.nav.helse.flex.syketilfelle.exceptionhandler.AbstractApiError
import no.nav.helse.flex.syketilfelle.exceptionhandler.LogLevel
import no.nav.helse.flex.syketilfelle.logger
import no.nav.helse.flex.syketilfelle.objectMapper
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

@Component
class ClientIdValidation(
    private val tokenValidationContextHolder: TokenValidationContextHolder,
    @Value("\${AZURE_APP_PRE_AUTHORIZED_APPS}") private val azureAppPreAuthorizedApps: String,
) {
    private val log = logger()
    private val allowedClientIds: List<PreAuthorizedClient> = objectMapper.readValue(azureAppPreAuthorizedApps)

    data class NamespaceAndApp(val namespace: String, val app: String)

    fun validateClientId(app: NamespaceAndApp) = validateClientId(listOf(app))

    fun validateClientId(apps: List<NamespaceAndApp>) {
        val clientIds =
            allowedClientIds
                .filter { apps.contains(it.tilNamespaceAndApp()) }
                .map { it.clientId }

        val azp = tokenValidationContextHolder.hentAzpClaim()
        if (clientIds.ikkeInneholder(azp)) {
            throw UkjentClientException("Ukjent client")
        }
    }

    private fun TokenValidationContextHolder.hentAzpClaim(): String {
        try {
            return this.tokenValidationContext.getJwtToken("azureator").jwtTokenClaims.getStringClaim("azp")!!
        } catch (e: Exception) {
            log.error("Fant ikke azp claim!", e)
            throw UkjentClientException("ukjent feil", e)
        }
    }

    private fun List<String>.ikkeInneholder(s: String): Boolean {
        return !this.contains(s)
    }
}

class UkjentClientException(message: String, grunn: Throwable? = null) : AbstractApiError(
    message = message,
    httpStatus = HttpStatus.FORBIDDEN,
    reason = "UKJENT_CLIENT",
    loglevel = LogLevel.WARN,
    grunn = grunn,
)

private fun PreAuthorizedClient.tilNamespaceAndApp(): NamespaceAndApp {
    val splitt = name.split(":")
    return NamespaceAndApp(namespace = splitt[1], app = splitt[2])
}

data class PreAuthorizedClient(val name: String, val clientId: String)
