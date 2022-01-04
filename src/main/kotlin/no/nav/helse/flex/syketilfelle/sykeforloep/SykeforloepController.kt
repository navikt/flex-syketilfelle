package no.nav.helse.flex.syketilfelle.sykeforloep

import no.nav.helse.flex.syketilfelle.clientidvalidation.ClientIdValidation
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*

@Controller
class SykeforloepController(
    private val clientIdValidation: ClientIdValidation,
    private val sykeforloepUtregner: SykeforloepUtregner,
    private val tokenValidationContextHolder: TokenValidationContextHolder,

) {

    @GetMapping("/api/v1/sykeforloep/maskin", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    @ProtectedWithClaims(issuer = "azureator")
    fun hentVedtak(@RequestHeader fnr: String): List<Sykeforloep> {
        clientIdValidation.validateClientId(
            ClientIdValidation.NamespaceAndApp(
                namespace = "flex",
                app = "syfosoknad"
            )
        )
        return sykeforloepUtregner.hentSykeforloep(fnr = fnr, inkluderPapirsykmelding = false)
    }

    @GetMapping("/api/v1/sykeforloep", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    @ProtectedWithClaims(issuer = "loginservice", claimMap = ["acr=Level4"])
    fun hentVedtak(): List<Sykeforloep> {
        val fnr = tokenValidationContextHolder.fnrFraOIDC()
        return sykeforloepUtregner.hentSykeforloep(fnr = fnr, inkluderPapirsykmelding = false)
    }
}

fun TokenValidationContextHolder.fnrFraOIDC(): String {
    val context = this.tokenValidationContext
    return context.getClaims("loginservice").subject
}
