package no.nav.helse.flex.syketilfelle

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.syketilfelle.sykeforloep.Sykeforloep
import no.nav.helse.flex.syketilfelle.ventetid.ErUtenforVentetidRequest
import no.nav.helse.flex.syketilfelle.ventetid.ErUtenforVentetidResponse
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.util.*

fun FellesTestOppsett.hentSykeforloep(
    fnr: List<String>,
    hentAndreIdenter: Boolean = true,
    inkluderPapirsykmelding: Boolean = true,
    token: String = server.azureToken(subject = "sykepengesoknad-backend-client-id"),
): List<Sykeforloep> {
    val json =
        mockMvc.perform(
            get("/api/v1/sykeforloep")
                .header("Authorization", "Bearer $token")
                .header("fnr", fnr.joinToString(separator = ", "))
                .queryParam("hentAndreIdenter", hentAndreIdenter.toString())
                .queryParam("inkluderPapirsykmelding", inkluderPapirsykmelding.toString())
                .contentType(MediaType.APPLICATION_JSON),
        ).andExpect(MockMvcResultMatchers.status().isOk).andReturn().response.contentAsString

    return objectMapper.readValue(json)
}

fun FellesTestOppsett.erUtenforVentetidSomBrukerTokenX(
    fnr: String,
    sykmeldingId: String,
): ErUtenforVentetidResponse {
    val json =
        mockMvc.perform(
            get("/api/bruker/v2/ventetid/$sykmeldingId/erUtenforVentetid")
                .header("Authorization", "Bearer ${server.tokenxToken(fnr = fnr)}")
                .contentType(MediaType.APPLICATION_JSON),
        ).andExpect(MockMvcResultMatchers.status().isOk).andReturn().response.contentAsString

    return objectMapper.readValue(json)
}

fun FellesTestOppsett.erUtenforVentetid(
    fnr: List<String>,
    hentAndreIdenter: Boolean = true,
    sykmeldingId: String,
    erUtenforVentetidRequest: ErUtenforVentetidRequest,
    token: String = server.azureToken(subject = "sykepengesoknad-backend-client-id"),
): Boolean {
    val json =
        mockMvc.perform(
            post("/api/v1/ventetid/$sykmeldingId/erUtenforVentetid")
                .header("Authorization", "Bearer $token")
                .header("fnr", fnr.joinToString(separator = ", "))
                .content(objectMapper.writeValueAsString(erUtenforVentetidRequest))
                .queryParam("hentAndreIdenter", hentAndreIdenter.toString())
                .contentType(MediaType.APPLICATION_JSON),
        ).andExpect(MockMvcResultMatchers.status().isOk).andReturn().response.contentAsString

    return objectMapper.readValue(json)
}

fun MockOAuth2Server.azureToken(
    subject: String,
    issuer: String = "azureator",
    audience: String = "flex-syketilfelle-client-id",
): String {
    val claims = HashMap<String, String>()

    return this.issueToken(
        issuer,
        subject,
        DefaultOAuth2TokenCallback(
            issuerId = issuer,
            subject = subject,
            audience = listOf(audience),
            claims = claims,
            expiry = 3600,
        ),
    ).serialize()
}

fun MockOAuth2Server.tokenxToken(
    fnr: String,
    audience: String = "flex-syketilfelle-client-id",
    issuerId: String = "tokenx",
    clientId: String = "frontend-client-id",
    claims: Map<String, Any> =
        mapOf(
            "acr" to "idporten-loa-high",
            "idp" to "idporten",
            "client_id" to clientId,
            "pid" to fnr,
        ),
): String {
    return issueToken(
        issuerId,
        clientId,
        DefaultOAuth2TokenCallback(
            issuerId = issuerId,
            subject = UUID.randomUUID().toString(),
            audience = listOf(audience),
            claims = claims,
            expiry = 3600,
        ),
    ).serialize()
}
