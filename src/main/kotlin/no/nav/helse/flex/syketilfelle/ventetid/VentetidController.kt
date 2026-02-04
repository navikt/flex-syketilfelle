package no.nav.helse.flex.syketilfelle.ventetid

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
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody

@Controller
class VentetidController(
    private val clientIdValidation: ClientIdValidation,
    private val ventetidUtregner: VentetidUtregner,
    private val tokenValidationContextHolder: TokenValidationContextHolder,
    private val sykeforloepUtregner: SykeforloepUtregner,
    override val pdlClient: PdlClient,
    @param:Value("\${SYKMELDINGER_FRONTEND_CLIENT_ID}")
    val sykmeldingerFrontendClientId: String,
    @param:Value("\${FLEX_SYKMELDINGER_BACKEND_CLIENT_ID}")
    val flexSykmeldingerBackendClientId: String,
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
        validerVentetidRequest(erUtenforVentetidRequest, sykmeldingId)
        val identer = hentIdenter(fnr, hentAndreIdenter)

        return ventetidUtregner.erUtenforVentetid(
            sykmeldingId = sykmeldingId,
            erUtenforVentetidRequest = erUtenforVentetidRequest,
            identer = identer,
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
        val identer = pdlClient.hentFolkeregisterIdenter(validerTokenXClaims().fnrFraIdportenTokenX())

        val erUtenforVentetid =
            ventetidUtregner.erUtenforVentetid(sykmeldingId, identer, ErUtenforVentetidRequest())
        val ventetid =
            ventetidUtregner.beregnVentetid(
                sykmeldingId = sykmeldingId,
                identer = identer,
                ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
            )

        val sykeforloep = sykeforloepUtregner.hentSykeforloep(identer, inkluderPapirsykmelding = false)
        val oppfolgingsdato =
            sykeforloep
                .find { it.sykmeldinger.any { sm -> sm.id == sykmeldingId } }
                ?.oppfolgingsdato

        return ErUtenforVentetidResponse(erUtenforVentetid, oppfolgingsdato, ventetid)
    }

    private fun validerVentetidRequest(
        ventetidRequest: ErUtenforVentetidRequest,
        sykmeldingId: String,
    ) {
        clientIdValidation.validateClientId(
            listOf(NamespaceAndApp(namespace = "flex", app = "sykepengesoknad-backend")),
        )
        with(ventetidRequest) {
            if (sykmeldingKafkaMessage != null && sykmeldingKafkaMessage.sykmelding.id != sykmeldingId) {
                throw IllegalArgumentException("sykmeldingId i path er ikke samme som i request body.")
            }
        }
    }

    private fun validerTokenXClaims(): JwtTokenClaims {
        val context = tokenValidationContextHolder.getTokenValidationContext()
        val claims = context.getClaims("tokenx")
        val clientId = claims.getStringClaim("client_id")
        if (clientId !in listOf(sykmeldingerFrontendClientId, flexSykmeldingerBackendClientId)) {
            throw IngenTilgang("Uventet client id $clientId")
        }

        return claims
    }

    private fun JwtTokenClaims.fnrFraIdportenTokenX(): String = this.getStringClaim("pid")
}

private class IngenTilgang(
    override val message: String,
) : AbstractApiError(
        message = message,
        httpStatus = HttpStatus.FORBIDDEN,
        reason = "INGEN_TILGANG",
        loglevel = LogLevel.WARN,
    )
