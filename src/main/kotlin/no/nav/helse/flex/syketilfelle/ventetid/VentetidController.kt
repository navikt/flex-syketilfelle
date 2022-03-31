package no.nav.helse.flex.syketilfelle.ventetid

import no.nav.helse.flex.syketilfelle.client.pdl.PdlClient
import no.nav.helse.flex.syketilfelle.clientidvalidation.ClientIdValidation
import no.nav.helse.flex.syketilfelle.clientidvalidation.ClientIdValidation.NamespaceAndApp
import no.nav.helse.flex.syketilfelle.identer.MedPdlClient
import no.nav.helse.flex.syketilfelle.identer.fnrFraLoginservicetoken
import no.nav.helse.flex.syketilfelle.sykeforloep.SykeforloepUtregner
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.context.TokenValidationContextHolder
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
) : MedPdlClient {

    @PostMapping(
        value = ["/api/v1/ventetid/{sykmeldingId}/erUtenforVentetid"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @ProtectedWithClaims(issuer = "azureator")
    @ResponseBody
    fun erUtenforVentetid(
        @RequestHeader fnr: String,
        @RequestParam(required = false) hentAndreIdenter: Boolean = true,
        @PathVariable sykmeldingId: String,
        @RequestBody erUtenforVentetidRequest: ErUtenforVentetidRequest
    ): Boolean {
        clientIdValidation.validateClientId(
            listOf(
                NamespaceAndApp(
                    namespace = "flex",
                    app = "syfosoknad",
                ),
                NamespaceAndApp(
                    namespace = "flex",
                    app = "sykepengesoknad-backend",
                )
            )
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
        "/api/bruker/v1/ventetid/{sykmeldingId}/erUtenforVentetid",
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @ResponseBody
    @ProtectedWithClaims(issuer = "loginservice", claimMap = ["acr=Level4"])
    fun erUtenforVentetid(@PathVariable("sykmeldingId") sykmeldingId: String): ErUtenforVentetidResponse {
        val fnr = tokenValidationContextHolder.fnrFraLoginservicetoken()
        val fnrs = pdlClient.hentFolkeregisterIdenter(fnr)

        val utenforVentetid =
            ventetidUtregner.beregnOmSykmeldingErUtenforVentetid(sykmeldingId, fnrs, ErUtenforVentetidRequest())

        val sykeforloep = sykeforloepUtregner.hentSykeforloep(fnrs, inkluderPapirsykmelding = false)
        val oppfolgingsdato = sykeforloep
            .find { it.sykmeldinger.any { sm -> sm.id == sykmeldingId } }
            ?.oppfolgingsdato

        return ErUtenforVentetidResponse(utenforVentetid, oppfolgingsdato)
    }
}
