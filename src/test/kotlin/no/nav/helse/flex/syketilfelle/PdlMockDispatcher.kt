package no.nav.helse.flex.syketilfelle

import no.nav.helse.flex.syketilfelle.client.pdl.FOLKEREGISTERIDENT
import no.nav.helse.flex.syketilfelle.client.pdl.GetPersonResponse
import no.nav.helse.flex.syketilfelle.client.pdl.GraphQLRequest
import no.nav.helse.flex.syketilfelle.client.pdl.HentIdenter
import no.nav.helse.flex.syketilfelle.client.pdl.HentIdenterResponseData
import no.nav.helse.flex.syketilfelle.client.pdl.PdlIdent
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import tools.jackson.module.kotlin.readValue

object PdlMockDispatcher : Dispatcher() {
    override fun dispatch(request: RecordedRequest): MockResponse {
        val graphReq: GraphQLRequest = objectMapper.readValue(request.body.readUtf8())
        val ident = graphReq.variables["ident"] ?: return MockResponse().setStatus("400")
        return when {
            ident.startsWith("2") -> skapResponse(listOf(ident, ident.replaceFirstChar { "1" }))
            ident.startsWith("3") ->
                skapResponse(
                    listOf(
                        ident,
                        ident.replaceFirstChar { "1" },
                        ident.replaceFirstChar { "2" },
                    ),
                )

            else -> skapResponse(listOf(ident))
        }
    }

    fun skapResponse(identer: List<String>): MockResponse =
        MockResponse().setBody(
            GetPersonResponse(
                data =
                    HentIdenterResponseData(
                        hentIdenter =
                            HentIdenter(
                                identer = identer.map { PdlIdent(gruppe = FOLKEREGISTERIDENT, ident = it) },
                            ),
                    ),
                errors = null,
            ).serialisertTilString(),
        )
}
