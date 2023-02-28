package no.nav.helse.flex.syketilfelle.sykeforloep

import no.nav.helse.flex.syketilfelle.client.pdl.PdlClient
import no.nav.helse.flex.syketilfelle.clientidvalidation.ClientIdValidation
import no.nav.helse.flex.syketilfelle.clientidvalidation.ClientIdValidation.NamespaceAndApp
import no.nav.helse.flex.syketilfelle.identer.MedPdlClient
import no.nav.helse.flex.syketilfelle.logger
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*

@Controller
class SykeforloepController(
    private val clientIdValidation: ClientIdValidation,
    private val sykeforloepUtregner: SykeforloepUtregner,
    override val pdlClient: PdlClient
) : MedPdlClient {

    val log = logger()

    @GetMapping("/api/v1/sykeforloep", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    @ProtectedWithClaims(issuer = "azureator")
    fun hentSykeforloep(
        @RequestHeader fnr: String,
        @RequestParam(required = false) hentAndreIdenter: Boolean = true,
        @RequestParam(required = false) inkluderPapirsykmelding: Boolean = false
    ): List<Sykeforloep> {
        clientIdValidation.validateClientId(
            listOf(
                NamespaceAndApp(
                    namespace = "flex",
                    app = "sykepengesoknad-backend"
                ),
                NamespaceAndApp(
                    namespace = "teamsykmelding",
                    app = "sparenaproxy"
                ),
                NamespaceAndApp(
                    namespace = "teamsykmelding",
                    app = "syfosmregler"
                ),
                NamespaceAndApp(
                    namespace = "teamsykmelding",
                    app = "syfosmpapirregler"
                ),
                NamespaceAndApp(
                    namespace = "teamsykmelding",
                    app = "dinesykmeldte-kafka"
                )
            )
        )

        val alleFnrs = fnr.split(", ").validerFnrOgHentAndreIdenter(hentAndreIdenter)
        return sykeforloepUtregner.hentSykeforloep(fnrs = alleFnrs, inkluderPapirsykmelding = inkluderPapirsykmelding)
    }
}
