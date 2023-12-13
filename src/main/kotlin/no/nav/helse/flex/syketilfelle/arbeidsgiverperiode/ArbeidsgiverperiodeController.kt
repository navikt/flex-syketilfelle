package no.nav.helse.flex.syketilfelle.arbeidsgiverperiode

import no.nav.helse.flex.sykepengesoknad.kafka.*
import no.nav.helse.flex.syketilfelle.arbeidsgiverperiode.domain.Arbeidsgiverperiode
import no.nav.helse.flex.syketilfelle.client.pdl.PdlClient
import no.nav.helse.flex.syketilfelle.clientidvalidation.ClientIdValidation
import no.nav.helse.flex.syketilfelle.clientidvalidation.ClientIdValidation.NamespaceAndApp
import no.nav.helse.flex.syketilfelle.identer.MedPdlClient
import no.nav.helse.flex.syketilfelle.sykmelding.domain.SykmeldingKafkaMessage
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
        value = ["/api/v2/arbeidsgiverperiode"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    @ResponseBody
    @ProtectedWithClaims(issuer = "azureator")
    fun beregnArbeidsgiverperiode(
        @RequestHeader fnr: String,
        @RequestParam(required = false) hentAndreIdenter: Boolean = true,
        @RequestHeader(required = false) forelopig: Boolean = false,
        @RequestParam(defaultValue = "") andreKorrigerteRessurser: List<String>,
        @RequestBody requestBody: SoknadOgSykmelding,
    ): ResponseEntity<Arbeidsgiverperiode> {
        clientIdValidation.validateClientId(
            NamespaceAndApp(
                namespace = "flex",
                app = "sykepengesoknad-backend",
            ),
        )

        val alleFnrs = fnr.split(", ").validerFnrOgHentAndreIdenter(hentAndreIdenter)

        val soknad = requestBody.soknad.copy(status = SoknadsstatusDTO.SENDT)

        val arbeidsgiverperiode =
            oppfolgingstilfelleService.beregnArbeidsgiverperiode(
                fnrs = alleFnrs,
                andreKorrigerteRessurser = andreKorrigerteRessurser,
                soknad = soknad,
                forelopig = forelopig,
                sykmelding = requestBody.sykmelding,
            )

        return (
            arbeidsgiverperiode
                ?.let { ResponseEntity.ok(it) }
                ?: ResponseEntity.noContent().build()
        )
    }
}

data class SoknadOgSykmelding(
    val soknad: SykepengesoknadDTO,
    val sykmelding: SykmeldingKafkaMessage? = null,
)
