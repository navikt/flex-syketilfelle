package no.nav.helse.flex.syketilfelle

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.syketilfelle.sykeforloep.Sykeforloep
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.util.*

fun Testoppsett.hentSykeforloepMedLoginserviceToken(fnr: String): List<Sykeforloep> {
    val json = mockMvc.perform(
        MockMvcRequestBuilders.get("/api/v1/sykeforloep")
            .header("Authorization", "Bearer ${server.loginserviceToken(subject = fnr)}")
            .contentType(MediaType.APPLICATION_JSON)
    ).andExpect(MockMvcResultMatchers.status().isOk).andReturn().response.contentAsString

    return objectMapper.readValue(json)
}

fun MockOAuth2Server.azureToken(
    subject: String,
    issuer: String = "azureator",
    audience: String = "flex-syketilfelle-client-id"
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
            expiry = 3600
        )
    ).serialize()
}

fun MockOAuth2Server.loginserviceToken(
    subject: String,
    issuerId: String = "loginservice",
    clientId: String = UUID.randomUUID().toString(),
    audience: String = "loginservice-client-id",
    claims: Map<String, Any> = mapOf("acr" to "Level4"),

): String {
    return this.issueToken(
        issuerId,
        clientId,
        DefaultOAuth2TokenCallback(
            issuerId = issuerId,
            subject = subject,
            audience = listOf(audience),
            claims = claims,
            expiry = 3600
        )
    ).serialize()
}
