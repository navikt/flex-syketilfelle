package no.nav.helse.flex.syketilfelle.config

import no.nav.security.token.support.client.core.ClientProperties
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import no.nav.security.token.support.client.spring.oauth2.EnableOAuth2Client
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager
import org.apache.hc.core5.http.io.SocketConfig
import org.apache.hc.core5.util.Timeout
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.RestTemplate
import java.time.Duration
import java.util.function.Supplier

const val PDL_REST_TEMPLATE_CONNECT_TIMEOUT = 1L
const val PDL_REST_TEMPLATE_READ_TIMEOUT = 1L

@EnableOAuth2Client(cacheEnabled = true)
@Configuration
class AadRestTemplateConfiguration {
    @Bean
    fun httpClient(): CloseableHttpClient {
        val connectionManager = PoolingHttpClientConnectionManager()
        connectionManager.defaultMaxPerRoute = 50
        connectionManager.maxTotal = 50
        // Erstatter deprecated HttpComponentsClientHttpRequestFactory.setReadTimeout()
        connectionManager.defaultSocketConfig =
            SocketConfig.custom()
                .setSoTimeout(Timeout.of(PDL_REST_TEMPLATE_READ_TIMEOUT, java.util.concurrent.TimeUnit.SECONDS))
                .build()

        return HttpClientBuilder.create()
            .setConnectionManager(connectionManager)
            .build()
    }

    @Bean
    fun pdlRestTemplate(
        restTemplateBuilder: RestTemplateBuilder,
        httpClient: CloseableHttpClient,
        clientConfigurationProperties: ClientConfigurationProperties,
        oAuth2AccessTokenService: OAuth2AccessTokenService,
    ): RestTemplate {
        val registrationName = "pdl-api-client-credentials"
        val clientProperties =
            clientConfigurationProperties.registration[registrationName]
                ?: throw RuntimeException("Fant ikke config for $registrationName.")

        return restTemplateBuilder
            // https://kotlinlang.org/docs/fun-interfaces.html#sam-conversions
            .requestFactory(Supplier { HttpComponentsClientHttpRequestFactory(httpClient) })
            .additionalInterceptors(bearerTokenInterceptor(clientProperties, oAuth2AccessTokenService))
            .connectTimeout(Duration.ofSeconds(PDL_REST_TEMPLATE_CONNECT_TIMEOUT))
            .build()
    }

    private fun bearerTokenInterceptor(
        clientProperties: ClientProperties,
        oAuth2AccessTokenService: OAuth2AccessTokenService,
    ): ClientHttpRequestInterceptor {
        return ClientHttpRequestInterceptor { request: HttpRequest, body: ByteArray, execution: ClientHttpRequestExecution ->
            val response = oAuth2AccessTokenService.getAccessToken(clientProperties)
            response.accessToken?.let { request.headers.setBearerAuth(it) }
            execution.execute(request, body)
        }
    }
}
