package no.nav.helse.flex.syketilfelle.ventetid

import no.nav.helse.flex.syketilfelle.arbeidsgiverperiode.not
import no.nav.helse.flex.syketilfelle.client.pdl.PdlClient
import no.nav.helse.flex.syketilfelle.clientidvalidation.ClientIdValidation
import no.nav.helse.flex.syketilfelle.clientidvalidation.ClientIdValidation.NamespaceAndApp
import no.nav.helse.flex.syketilfelle.exceptionhandler.AbstractApiError
import no.nav.helse.flex.syketilfelle.exceptionhandler.LogLevel
import no.nav.helse.flex.syketilfelle.identer.MedPdlClient
import no.nav.helse.flex.syketilfelle.logger
import no.nav.helse.flex.syketilfelle.sykeforloep.SykeforloepUtregner
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*

@Controller
class VentetidController(
    private val clientIdValidation: ClientIdValidation,
    private val ventetidUtregner: VentetidUtregner,
    private val tokenValidationContextHolder: TokenValidationContextHolder,
    private val sykeforloepUtregner: SykeforloepUtregner,
    override val pdlClient: PdlClient,
    @Value("\${SYKMELDINGER_FRONTEND_CLIENT_ID}")
    val sykmeldingerFrontendClientId: String,
    @Value("\${FLEX_SYKMELDINGER_FRONTEND_CLIENT_ID}")
    val flexSykmeldingerFrontendClientId: String,
) : MedPdlClient {
    val log = logger()

    @PostMapping(
        value = ["/api/v1/ventetid/{sykmeldingId}/erUtenforVentetid"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    @ProtectedWithClaims(issuer = "azureator")
    @ResponseBody
    fun erUtenforVentetid(
        @RequestHeader fnr: String,
        @RequestParam(required = false) hentAndreIdenter: Boolean = true,
        @PathVariable sykmeldingId: String,
        @RequestBody erUtenforVentetidRequest: ErUtenforVentetidRequest,
    ): Boolean {
        clientIdValidation.validateClientId(
            NamespaceAndApp(
                namespace = "flex",
                app = "sykepengesoknad-backend",
            ),
        )
        with(erUtenforVentetidRequest) {
            if (sykmeldingKafkaMessage != null && sykmeldingKafkaMessage.sykmelding.id != sykmeldingId) {
                throw IllegalArgumentException("Sykmelding id i path skal være samme som i body")
            }
        }
        val alleFnrs = fnr.split(", ").validerFnrOgHentAndreIdenter(hentAndreIdenter)

        return ventetidUtregner.beregnOmSykmeldingErUtenforVentetid(
            sykmeldingId = sykmeldingId,
            erUtenforVentetidRequest = erUtenforVentetidRequest,
            fnrs = alleFnrs,
        )
    }

    @GetMapping(
        "/api/bruker/v2/ventetid/{sykmeldingId}/erUtenforVentetid",
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    @ResponseBody
    @ProtectedWithClaims(issuer = "tokenx", combineWithOr = true, claimMap = ["acr=Level4", "acr=idporten-loa-high"])
    fun erUtenforVentetid(
        @PathVariable("sykmeldingId") sykmeldingId: String,
    ): ErUtenforVentetidResponse {
        val fnr = validerTokenXClaims().fnrFraIdportenTokenX()
        return erUtenforVentetidResponse(fnr, sykmeldingId)
    }

    private fun erUtenforVentetidResponse(
        fnr: String,
        sykmeldingId: String,
    ): ErUtenforVentetidResponse {
        val fnrs = pdlClient.hentFolkeregisterIdenter(fnr)

        val utenforVentetid =
            ventetidUtregner.beregnOmSykmeldingErUtenforVentetid(sykmeldingId, fnrs, ErUtenforVentetidRequest())

        val sykeforloep = sykeforloepUtregner.hentSykeforloep(fnrs, inkluderPapirsykmelding = false)
        val oppfolgingsdato =
            sykeforloep
                .find { it.sykmeldinger.any { sm -> sm.id == sykmeldingId } }
                ?.oppfolgingsdato

        return ErUtenforVentetidResponse(utenforVentetid, oppfolgingsdato)
    }

    private fun validerTokenXClaims(): JwtTokenClaims {
        val context = tokenValidationContextHolder.getTokenValidationContext()
        val claims = context.getClaims("tokenx")
        val clientId = claims.getStringClaim("client_id")
        if (!listOf(sykmeldingerFrontendClientId, flexSykmeldingerFrontendClientId).contains(clientId)) {
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
    loglevel = LogLevel.WARN,
)
