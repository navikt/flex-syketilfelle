package no.nav.helse.flex.syketilfelle.arbeidsgiverperiode

import no.nav.helse.flex.syketilfelle.arbeidsgiverperiode.domain.Arbeidsgiverperiode
import no.nav.helse.flex.syketilfelle.client.pdl.PdlClient
import no.nav.helse.flex.syketilfelle.clientidvalidation.ClientIdValidation
import no.nav.helse.flex.syketilfelle.clientidvalidation.ClientIdValidation.NamespaceAndApp
import no.nav.helse.flex.syketilfelle.identer.MedPdlClient
import no.nav.helse.flex.syketilfelle.juridiskvurdering.JuridiskVurdering
import no.nav.helse.flex.syketilfelle.juridiskvurdering.JuridiskVurderingKafkaProducer
import no.nav.helse.flex.syketilfelle.juridiskvurdering.Utfall
import no.nav.helse.flex.syketilfelle.soknad.mapSoknadTilBiter
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.syfo.kafka.felles.SoknadsstatusDTO
import no.nav.syfo.kafka.felles.SykepengesoknadDTO
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
class ArbeidsgiverperiodeController(
    private val clientIdValidation: ClientIdValidation,
    private val oppfolgingstilfelleService: ArbeidsgiverperiodeUtregner,
    private val juridiskVurderingKafkaProducer: JuridiskVurderingKafkaProducer,
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
            oppfolgingstilfelleService.beregnOppfolgingstilfelleForSoknadTilInnsending(
                alleFnrs,
                andreKorrigerteRessurser,
                soknad.mapSoknadTilBiter(),
                soknad.tom!!,
                soknad.forsteDagISoknad(),
                sykepengesoknadDTO.startSyketilfelle
            )

        arbeidsgiverperiode?.let {
            juridiskVurderingKafkaProducer.produserMelding(
                JuridiskVurdering(
                    fødselsnummer = fnr,
                    sporing = mapOf(
                        sykepengesoknadDTO.id to "SØKNAD",
                        sykepengesoknadDTO.sykmeldingId!! to "SYKMELDING"
                    ),
                    input = mapOf("in" to "TODO!!"),
                    output = mapOf("arbeidsgiverperiode" to it),
                    lovverk = "folketrygdloven",
                    paragraf = "§8-19",
                    ledd = null,
                    punktum = null,
                    bokstav = null,
                    lovverksversjon = LocalDate.of(1997, 5, 1),
                    organisasjonsnummer = sykepengesoknadDTO.arbeidsgiver?.orgnummer ?: "TODO optional i schema",
                    utfall = Utfall.VILKAR_BEREGNET,
                )
            )
        }
        return (
            arbeidsgiverperiode
                ?.let { ResponseEntity.ok(it) }
                ?: ResponseEntity.noContent().build()
            )
    }

    private fun SykepengesoknadDTO.forsteDagISoknad(): LocalDate {

        return egenmeldinger!!.map { it.fom!! }.minOrNull() ?: fom!!
    }
}
