package no.nav.helse.flex.syketilfelle.sykeforloep

import no.nav.helse.flex.syketilfelle.client.pdl.PdlClient
import no.nav.helse.flex.syketilfelle.clientidvalidation.ClientIdValidation
import no.nav.helse.flex.syketilfelle.clientidvalidation.ClientIdValidation.NamespaceAndApp
import no.nav.helse.flex.syketilfelle.exceptionhandler.AbstractApiError
import no.nav.helse.flex.syketilfelle.exceptionhandler.LogLevel
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*

@Controller
class SykeforloepController(
    private val clientIdValidation: ClientIdValidation,
    private val sykeforloepUtregner: SykeforloepUtregner,
    private val tokenValidationContextHolder: TokenValidationContextHolder,
    private val pdlClient: PdlClient,
) {

    @GetMapping("/api/v1/sykeforloep", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    @ProtectedWithClaims(issuer = "azureator")
    fun hentVedtak(
        @RequestHeader fnr: String,
        @RequestParam(required = false) hentAndreIdenter: Boolean = true,
    ): List<Sykeforloep> {
        clientIdValidation.validateClientId(
            NamespaceAndApp(
                namespace = "flex",
                app = "syfosoknad"
            )
        )

        val alleFnrs = fnr.split(", ").validerFnrOgHentAndreIdenter(hentAndreIdenter)
        return sykeforloepUtregner.hentSykeforloep(fnrs = alleFnrs, inkluderPapirsykmelding = false)
    }

    @GetMapping("/api/bruker/v1/sykeforloep", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    @ProtectedWithClaims(issuer = "loginservice", claimMap = ["acr=Level4"])
    fun hentVedtak(): List<Sykeforloep> {
        val fnr = tokenValidationContextHolder.fnrFraOIDC()
        val fnrs = pdlClient.hentFolkeregisterIdenter(fnr)
        return sykeforloepUtregner.hentSykeforloep(fnrs = fnrs, inkluderPapirsykmelding = false)
    }

    fun List<String>.validerFnrOgHentAndreIdenter(hentAndreIdenter: Boolean): List<String> {
        if (this.isEmpty()) {
            throw ManglerIdenterException()
        }
        fun String.isDigit(): Boolean = this.all { it.isDigit() }

        if (this.any { !it.isDigit() || it.length != 11 }) {
            throw FeilIdentException()
        }

        return if (hentAndreIdenter) {
            if (this.size != 1) {
                throw FlereIdenterVedHentingException()
            }
            pdlClient.hentFolkeregisterIdenter(this.first())
        } else {
            this
        }
    }
}

fun TokenValidationContextHolder.fnrFraOIDC(): String {
    val context = this.tokenValidationContext
    return context.getClaims("loginservice").subject
}

class FlereIdenterVedHentingException : AbstractApiError(
    message = "Kan ikke ha flere identer i input når vi skal hente flere identer",
    httpStatus = HttpStatus.BAD_REQUEST,
    reason = "FLERE_IDENTER_OG_HENTING",
    loglevel = LogLevel.ERROR
)

class ManglerIdenterException : AbstractApiError(
    message = "Må ha hvertfall en ident i input",
    httpStatus = HttpStatus.BAD_REQUEST,
    reason = "MANGLER_IDENTER",
    loglevel = LogLevel.ERROR
)

class FeilIdentException : AbstractApiError(
    message = "Forventer ident med 11 siffer",
    httpStatus = HttpStatus.BAD_REQUEST,
    reason = "UGYLDIG_FNR",
    loglevel = LogLevel.ERROR
)
