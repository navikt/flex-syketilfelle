package no.nav.helse.flex.syketilfelle

import no.nav.helse.flex.syketilfelle.config.AadRestTemplateConfiguration
import no.nav.helse.flex.syketilfelle.config.PDL_REST_TEMPLATE_CONNECT_TIMEOUT
import no.nav.helse.flex.syketilfelle.config.PDL_REST_TEMPLATE_READ_TIMEOUT
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import okhttp3.mockwebserver.MockWebServer
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestTemplate
import java.util.concurrent.TimeUnit

@AutoConfigureWebClient
@EnableMockOAuth2Server
@SpringBootTest(
    classes = [
        AadRestTemplateConfiguration::class,
        no.nav.security.token.support.client.spring.oauth2.OAuth2ClientConfiguration::class,
        no.nav.security.token.support.spring.SpringTokenValidationContextHolder::class,
    ],
)
class RestTemplateTimeoutTest {
    init {
        MockWebServer()
            .also {
                System.setProperty("PDL_BASE_URL", "http://localhost:${it.port}")
            }
            .also { it.dispatcher = PdlMockDispatcher }
    }

    @Autowired
    private lateinit var pdlRestTemplate: RestTemplate

    @Test
    fun failOnConnectTimeout() {
        await().atMost(PDL_REST_TEMPLATE_CONNECT_TIMEOUT + 1, TimeUnit.SECONDS).untilAsserted {
            assertThrows<ResourceAccessException> {
                pdlRestTemplate.getForEntity(
                    // Non-routable IP addresse. så vi får ikke opprettet en connection.
                    "http://172.0.0.1",
                    String::class.java,
                )
            }
        }
    }

    @Test
    fun failOnReadTimeout() {
        val responsDelayInSeconds = PDL_REST_TEMPLATE_READ_TIMEOUT + 1L
        assertThrows<ResourceAccessException> {
            // Oppretter connection, men bruker lang tid på å svare
            pdlRestTemplate.getForEntity("https://httpbin.org/delay/$responsDelayInSeconds", String::class.java)
        }
    }
}
