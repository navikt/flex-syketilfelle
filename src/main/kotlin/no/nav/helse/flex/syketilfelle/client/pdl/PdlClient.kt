package no.nav.helse.flex.syketilfelle.client.pdl

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.syketilfelle.objectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.util.*

@Component
class PdlClient(
    @Value("\${PDL_BASE_URL}")
    private val pdlApiUrl: String,
    private val pdlRestTemplate: RestTemplate
) {

    private val TEMA = "Tema"
    private val TEMA_SYK = "SYK"
    private val IDENT = "ident"

    private val HENT_PERSON_QUERY =
        """
query(${"$"}ident: ID!){
  hentIdenter(ident: ${"$"}ident, historikk: true) {
    identer {
      ident,
      gruppe
    }
  }
}
"""

    @Retryable(exclude = [FunctionalPdlError::class])
    fun hentFolkeregisterIdenter(ident: String): List<String> {

        val graphQLRequest = GraphQLRequest(
            query = HENT_PERSON_QUERY,
            variables = Collections.singletonMap(IDENT, ident)
        )

        val responseEntity = pdlRestTemplate.exchange(
            "$pdlApiUrl/graphql",
            HttpMethod.POST,
            HttpEntity(requestToJson(graphQLRequest), createHeaderWithTema()),
            String::class.java
        )

        if (responseEntity.statusCode != HttpStatus.OK) {
            throw RuntimeException("PDL svarer med status ${responseEntity.statusCode} - ${responseEntity.body}")
        }

        val parsedResponse: GetPersonResponse? = responseEntity.body?.let { objectMapper.readValue(it) }

        parsedResponse?.data?.let {
            return it.folkeregisteridenter()
        }
        throw FunctionalPdlError("Fant ikke person, ingen body eller data. ${parsedResponse.hentErrors()}")
    }

    private fun HentIdenterResponseData.folkeregisteridenter(): List<String> {
        return this.hentIdenter?.identer?.filter { it.gruppe == FOLKEREGISTERIDENT }?.map { it.ident } ?: emptyList()
    }

    private fun createHeaderWithTema(): HttpHeaders {
        val headers = createHeader()
        headers[TEMA] = TEMA_SYK
        return headers
    }

    private fun createHeader(): HttpHeaders {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        return headers
    }

    private fun requestToJson(graphQLRequest: Any): String {
        return try {
            ObjectMapper().writeValueAsString(graphQLRequest)
        } catch (e: JsonProcessingException) {
            throw RuntimeException(e)
        }
    }

    private fun GetPersonResponse?.hentErrors(): String? {
        return this?.errors?.map { it.message }?.joinToString(" - ")
    }

    class FunctionalPdlError(message: String) : RuntimeException(message)
}

data class GraphQLRequest(val query: String, val variables: Map<String, String>)
