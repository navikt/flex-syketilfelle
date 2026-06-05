package no.nav.helse.flex.syketilfelle.config

import no.nav.security.token.support.client.core.ClientProperties
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import no.nav.security.token.support.client.spring.oauth2.EnableOAuth2Client
import org.apache.hc.client5.http.config.ConnectionConfig
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager
import org.apache.hc.core5.http.io.SocketConfig
import org.apache.hc.core5.util.Timeout
import org.springframework.boot.restclient.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.RestTemplate

const val PDL_REST_TEMPLATE_CONNECT_TIMEOUT = 5L
const val PDL_REST_TEMPLATE_READ_TIMEOUT = 15L

@EnableOAuth2Client(cacheEnabled = true)
@Configuration
class AadRestTemplateConfiguration {
    @Bean
    fun httpClient(): CloseableHttpClient {
        val connectionManager =
            PoolingHttpClientConnectionManager().apply {
                defaultMaxPerRoute = 50
                maxTotal = 50

                defaultSocketConfig =
                    SocketConfig
                        .custom()
                        .setSoTimeout(Timeout.ofSeconds(PDL_REST_TEMPLATE_READ_TIMEOUT))
                        .build()

                setDefaultConnectionConfig(
                    ConnectionConfig
                        .custom()
                        .setConnectTimeout(Timeout.ofSeconds(PDL_REST_TEMPLATE_CONNECT_TIMEOUT))
                        .build(),
                )
            }

        return HttpClients
            .custom()
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
            .requestFactory { HttpComponentsClientHttpRequestFactory(httpClient) }
            .additionalInterceptors(bearerTokenInterceptor(clientProperties, oAuth2AccessTokenService))
            .build()
    }

    private fun bearerTokenInterceptor(
        clientProperties: ClientProperties,
        oAuth2AccessTokenService: OAuth2AccessTokenService,
    ): ClientHttpRequestInterceptor =
        ClientHttpRequestInterceptor { request: HttpRequest, body: ByteArray, execution: ClientHttpRequestExecution ->
            val response = oAuth2AccessTokenService.getAccessToken(clientProperties)
            response.access_token?.let { request.headers.setBearerAuth(it) }
            execution.execute(request, body)
        }
}
