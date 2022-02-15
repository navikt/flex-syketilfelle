package no.nav.helse.flex.syketilfelle.arbeidsgiverperiode

import no.nav.helse.flex.sykepengesoknad.kafka.*
import no.nav.helse.flex.syketilfelle.arbeidsgiverperiode.domain.Arbeidsgiverperiode
import no.nav.helse.flex.syketilfelle.client.pdl.PdlClient
import no.nav.helse.flex.syketilfelle.clientidvalidation.ClientIdValidation
import no.nav.helse.flex.syketilfelle.clientidvalidation.ClientIdValidation.NamespaceAndApp
import no.nav.helse.flex.syketilfelle.identer.MedPdlClient
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
class ArbeidsgiverperiodeController(
    private val clientIdValidation: ClientIdValidation,
    private val oppfolgingstilfelleService: ArbeidsgiverperiodeUtregner,
    override val pdlClient: PdlClient,
) : MedPdlClient {

    @PostMapping(
        value = ["/api/v1/arbeidsgiverperiode"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @ResponseBody
    @ProtectedWithClaims(issuer = "azureator")
    fun beregnArbeidsgiverperiode(
        @RequestHeader fnr: String,
        @RequestParam(required = false) hentAndreIdenter: Boolean = true,
        @RequestHeader(required = false) forelopig: Boolean = false,
        @RequestParam(defaultValue = "") andreKorrigerteRessurser: List<String>,
        @RequestBody sykepengesoknadDTO: SykepengesoknadDTO
    ): ResponseEntity<Arbeidsgiverperiode> {
        clientIdValidation.validateClientId(
            NamespaceAndApp(
                namespace = "flex",
                app = "syfosoknad",
            )
        )

        val alleFnrs = fnr.split(", ").validerFnrOgHentAndreIdenter(hentAndreIdenter)

        val soknad = sykepengesoknadDTO.copy(status = SoknadsstatusDTO.SENDT)

        val arbeidsgiverperiode =
            oppfolgingstilfelleService.beregnArbeidsgiverperiode(
                fnrs = alleFnrs,
                andreKorrigerteRessurser = andreKorrigerteRessurser,
                soknad = soknad,
                forelopig = forelopig,
            )

        return (
            arbeidsgiverperiode
                ?.let { ResponseEntity.ok(it) }
                ?: ResponseEntity.noContent().build()
            )
    }
}
