package no.nav.helse.flex.syketilfelle.ventetid

import no.nav.helse.flex.syketilfelle.client.pdl.PdlClient
import no.nav.helse.flex.syketilfelle.clientidvalidation.ClientIdValidation
import no.nav.helse.flex.syketilfelle.clientidvalidation.ClientIdValidation.NamespaceAndApp
import no.nav.helse.flex.syketilfelle.identer.MedPdlClient
import no.nav.helse.flex.syketilfelle.syketilfellebit.SyketilfellebitRepository
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ResponseBody
import java.time.LocalDate

@Controller
class VentetidFlexInternalController(
    private val clientIdValidation: ClientIdValidation,
    private val ventetidUtregner: VentetidUtregner,
    private val syketilfellebitRepository: SyketilfellebitRepository,
    override val pdlClient: PdlClient,
) : MedPdlClient {
    @GetMapping(
        value = ["/api/v1/flex/ventetid/{sykmeldingId}"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    @ProtectedWithClaims(issuer = "azureator")
    @ResponseBody
    fun hentVentetidForFlexInternal(
        @PathVariable sykmeldingId: String,
    ): VentetidInternalResponse {
        clientIdValidation.validateClientId(NamespaceAndApp(namespace = "flex", app = "flex-internal-frontend"))
        val fnr =
            syketilfellebitRepository
                .findByRessursId(sykmeldingId)
                .map { it.fnr }
                .distinct()
                .single()
        val identer = hentIdenter(fnr, true)

        val erUtenforVentetid =
            ventetidUtregner.erUtenforVentetid(
                sykmeldingId = sykmeldingId,
                identer = identer,
                erUtenforVentetidRequest = ErUtenforVentetidRequest(),
            )

        val ventetid =
            ventetidUtregner.beregnVentetid(
                sykmeldingId = sykmeldingId,
                identer = identer,
                ventetidRequest = VentetidRequest(returnerPerioderInnenforVentetid = true),
            )

        val sykmeldingsperiode =
            syketilfellebitRepository
                .findByRessursId(sykmeldingId)
                .firstOrNull()
                ?.let { FomTomPeriode(it.fom, it.tom) }

        val syketilfellebiter =
            syketilfellebitRepository
                .findByFnrIn(identer)
                .sortedBy { it.opprettet }
                .map {
                    SyketilfellebitInternal(
                        syketilfellebitId = it.syketilfellebitId,
                        fnr = it.fnr,
                        opprettet = it.opprettet,
                        inntruffet = it.inntruffet,
                        orgnummer = it.orgnummer,
                        tags = it.tags,
                        ressursId = it.ressursId,
                        korrigererSendtSoknad = it.korrigererSendtSoknad,
                        fom = it.fom,
                        tom = it.tom,
                        publisert = it.publisert,
                        slettet = it.slettet,
                        tombstonePublisert = it.tombstonePublistert,
                    )
                }

        return VentetidInternalResponse(
            erUtenforVentetid = erUtenforVentetid,
            ventetid = ventetid!!,
            sykmeldingsperiode = sykmeldingsperiode,
            syketilfellebiter = syketilfellebiter,
        )
    }
}

data class VentetidInternalResponse(
    var erUtenforVentetid: Boolean,
    val ventetid: FomTomPeriode,
    var sykmeldingsperiode: FomTomPeriode?,
    val syketilfellebiter: List<SyketilfellebitInternal> = emptyList(),
)

data class SyketilfellebitInternal(
    val syketilfellebitId: String,
    val fnr: String,
    val opprettet: java.time.OffsetDateTime,
    val inntruffet: java.time.OffsetDateTime,
    val orgnummer: String?,
    val tags: String,
    val ressursId: String,
    val korrigererSendtSoknad: String?,
    val fom: LocalDate,
    val tom: LocalDate,
    val publisert: Boolean,
    val slettet: java.time.OffsetDateTime?,
    val tombstonePublisert: java.time.OffsetDateTime?,
)
