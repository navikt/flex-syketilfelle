package no.nav.helse.flex.syketilfelle.client.pdl

import no.nav.helse.flex.syketilfelle.FellesTestOppsett
import no.nav.helse.flex.syketilfelle.PdlMockDispatcher
import no.nav.helse.flex.syketilfelle.objectMapper
import no.nav.helse.flex.syketilfelle.serialisertTilString
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import tools.jackson.module.kotlin.readValue
import java.util.concurrent.atomic.AtomicInteger

class PdlClientRetryTest : FellesTestOppsett() {
    @Autowired
    lateinit var pdlClient: PdlClient

    @Test
    fun `Retry kall til PDL ved feil`() {
        val antallKall = AtomicInteger(0)

        pdlMockWebServer.dispatcher =
            object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    if (antallKall.incrementAndGet() == 1) {
                        return MockResponse().setResponseCode(500)
                    }
                    val ident =
                        objectMapper.readValue<GraphQLRequest>(request.body.readUtf8()).variables.getValue("ident")

                    return MockResponse().setBody(
                        GetPersonResponse(
                            data =
                                HentIdenterResponseData(
                                    hentIdenter = HentIdenter(identer = listOf(PdlIdent(gruppe = FOLKEREGISTERIDENT, ident = ident))),
                                ),
                            errors = null,
                        ).serialisertTilString(),
                    )
                }
            }

        try {
            pdlClient.hentFolkeregisterIdenter("10000000000") shouldBeEqualTo listOf("10000000000")
            antallKall.get() shouldBeEqualTo 2
        } finally {
            pdlMockWebServer.dispatcher = PdlMockDispatcher
        }
    }
}
