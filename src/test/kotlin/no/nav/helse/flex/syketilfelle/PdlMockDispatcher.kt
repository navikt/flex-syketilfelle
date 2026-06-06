package no.nav.helse.flex.syketilfelle

import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.RecordedRequest
import no.nav.helse.flex.syketilfelle.client.pdl.FOLKEREGISTERIDENT
import no.nav.helse.flex.syketilfelle.client.pdl.GetPersonResponse
import no.nav.helse.flex.syketilfelle.client.pdl.GraphQLRequest
import no.nav.helse.flex.syketilfelle.client.pdl.HentIdenter
import no.nav.helse.flex.syketilfelle.client.pdl.HentIdenterResponseData
import no.nav.helse.flex.syketilfelle.client.pdl.PdlIdent
import tools.jackson.module.kotlin.readValue

object PdlMockDispatcher : Dispatcher() {
    override fun dispatch(request: RecordedRequest): MockResponse {
        val graphReq: GraphQLRequest = objectMapper.readValue(request.body!!.utf8())
        val ident = graphReq.variables["ident"] ?: return MockResponse(code = 400)
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
        MockResponse(
            body =
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
