package no.nav.helse.flex.syketilfelle

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.syketilfelle.client.pdl.FOLKEREGISTERIDENT
import no.nav.helse.flex.syketilfelle.client.pdl.GetPersonResponse
import no.nav.helse.flex.syketilfelle.client.pdl.GraphQLRequest
import no.nav.helse.flex.syketilfelle.client.pdl.HentIdenter
import no.nav.helse.flex.syketilfelle.client.pdl.HentIdenterResponseData
import no.nav.helse.flex.syketilfelle.client.pdl.PdlIdent
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest

object PdlMockDispatcher : Dispatcher() {
    override fun dispatch(request: RecordedRequest): MockResponse {
        val graphReq: GraphQLRequest = objectMapper.readValue(request.body.readUtf8())
        val ident = graphReq.variables["ident"] ?: return MockResponse().setStatus("400")
        if (ident.startsWith("2")) {
            return skapResponse(listOf(ident, ident.replaceFirstChar { "1" }))
        }
        if (ident.startsWith("3")) {
            return skapResponse(listOf(ident, ident.replaceFirstChar { "1" }, ident.replaceFirstChar { "2" }))
        }
        return skapResponse(listOf(ident))
    }

    fun skapResponse(identer: List<String>): MockResponse {
        return MockResponse().setBody(
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
}
