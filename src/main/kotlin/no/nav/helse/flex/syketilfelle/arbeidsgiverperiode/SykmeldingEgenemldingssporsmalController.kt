package no.nav.helse.flex.syketilfelle.arbeidsgiverperiode

import no.nav.helse.flex.sykepengesoknad.kafka.*
import no.nav.helse.flex.syketilfelle.arbeidsgiverperiode.domain.EgenmeldingSporsmalForSykmelding
import no.nav.helse.flex.syketilfelle.client.pdl.PdlClient
import no.nav.helse.flex.syketilfelle.exceptionhandler.AbstractApiError
import no.nav.helse.flex.syketilfelle.exceptionhandler.LogLevel
import no.nav.helse.flex.syketilfelle.identer.MedPdlClient
import no.nav.helse.flex.syketilfelle.logger
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*

@Controller
class SykmeldingArbeidsgiverperiodeController(
    private val tokenValidationContextHolder: TokenValidationContextHolder,
    override val pdlClient: PdlClient,

    @Value("\${SYKMELDINGER_FRONTEND_CLIENT_ID}")
    val sykmeldingerFrontendClientId: String,

) : MedPdlClient {
    val log = logger()

    @GetMapping(
        "/api/bruker/v2/sykmelding/{sykmeldingId}/egenmeldingsporsmal",
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @ResponseBody
    @ProtectedWithClaims(issuer = "tokenx", claimMap = ["acr=Level4"])
    fun erUtenforVentetid(@PathVariable("sykmeldingId") sykmeldingId: String): EgenmeldingSporsmalForSykmelding {
        val fnr = validerTokenXClaims().fnrFraIdportenTokenX()
        val fnrs = pdlClient.hentFolkeregisterIdenter(fnr)

        return EgenmeldingSporsmalForSykmelding(null, null)
    }

    private fun validerTokenXClaims(): JwtTokenClaims {
        val context = tokenValidationContextHolder.tokenValidationContext
        val claims = context.getClaims("tokenx")
        val clientId = claims.getStringClaim("client_id")
        if (clientId != sykmeldingerFrontendClientId) {
            throw IngenTilgang("Uventet client id $clientId")
        }

        return claims
    }

    private fun JwtTokenClaims.fnrFraIdportenTokenX(): String {
        return this.getStringClaim("pid")
    }
}

private class IngenTilgang(override val message: String) : AbstractApiError(
    message = message,
    httpStatus = HttpStatus.FORBIDDEN,
    reason = "INGEN_TILGANG",
    loglevel = LogLevel.WARN
)
