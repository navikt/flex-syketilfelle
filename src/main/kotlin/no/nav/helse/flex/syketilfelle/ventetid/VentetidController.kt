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
import org.springframework.web.bind.annotation.*

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
        @RequestBody ventetidRequest: VentetidRequest,
    ): Boolean {
        validerVenteperiodeRequest(ventetidRequest.tilVenteperiodeRequest(), sykmeldingId)
        val identer = hentIdenter(fnr, hentAndreIdenter)

        return ventetidUtregner.beregnOmSykmeldingErUtenforVentetid(
            sykmeldingId = sykmeldingId,
            ventetidRequest = ventetidRequest,
            identer = identer,
        )
    }

    @PostMapping(
        value = ["/api/v1/ventetid/{sykmeldingId}/venteperiode"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    @ProtectedWithClaims(issuer = "azureator")
    @ResponseBody
    fun hentVenteperiode(
        @RequestHeader fnr: String,
        @RequestParam(required = false) hentAndreIdenter: Boolean = true,
        @PathVariable sykmeldingId: String,
        @RequestBody venteperiodeRequest: VenteperiodeRequest,
    ): VenteperiodeResponse {
        validerVenteperiodeRequest(venteperiodeRequest, sykmeldingId)
        val identer = hentIdenter(fnr, hentAndreIdenter)

        val venteperiode =
            ventetidUtregner.beregnVenteperiode(
                sykmeldingId = sykmeldingId,
                venteperiodeRequest = venteperiodeRequest,
                identer = identer,
            )
        return VenteperiodeResponse(venteperiode)
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
        val identer = pdlClient.hentFolkeregisterIdenter(fnr)

        val utenforVentetid =
            ventetidUtregner.beregnOmSykmeldingErUtenforVentetid(sykmeldingId, identer, VentetidRequest())

        val sykeforloep = sykeforloepUtregner.hentSykeforloep(identer, inkluderPapirsykmelding = false)
        val oppfolgingsdato =
            sykeforloep
                .find { it.sykmeldinger.any { sm -> sm.id == sykmeldingId } }
                ?.oppfolgingsdato

        return ErUtenforVentetidResponse(utenforVentetid, oppfolgingsdato)
    }

    private fun validerVenteperiodeRequest(
        venteperiodeRequest: VenteperiodeRequest,
        sykmeldingId: String,
    ) {
        clientIdValidation.validateClientId(
            NamespaceAndApp(
                namespace = "flex",
                app = "sykepengesoknad-backend",
            ),
        )
        with(venteperiodeRequest) {
            if (sykmeldingKafkaMessage != null && sykmeldingKafkaMessage.sykmelding.id != sykmeldingId) {
                throw IllegalArgumentException("sykmeldingId i path er ikke samme som i request body.")
            }
        }
    }

    private fun hentIdenter(
        fnr: String,
        hentAndreIdenter: Boolean,
    ): List<String> = fnr.split(", ").validerFnrOgHentAndreIdenter(hentAndreIdenter)

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
