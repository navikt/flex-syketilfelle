package no.nav.helse.flex.syketilfelle.pdl

import no.nav.helse.flex.syketilfelle.FellesTestOppsett
import no.nav.helse.flex.syketilfelle.client.pdl.PdlClient
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class PdlClientTest : FellesTestOppsett() {
    @Autowired
    private lateinit var pdlClient: PdlClient

    @Test
    fun `Kun et fnr i response`() {
        val responseData = pdlClient.hentFolkeregisterIdenter("12345")
        responseData `should be equal to` listOf("12345")
    }

    @Test
    fun `To fnr i response`() {
        val responseData = pdlClient.hentFolkeregisterIdenter("22345")
        responseData `should be equal to` listOf("22345", "12345")
    }

    @Test
    fun `Tre fnr i response`() {
        val responseData = pdlClient.hentFolkeregisterIdenter("32345")
        responseData `should be equal to` listOf("32345", "12345", "22345")
    }
}
