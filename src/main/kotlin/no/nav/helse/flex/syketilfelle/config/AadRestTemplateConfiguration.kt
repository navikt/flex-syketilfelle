package no.nav.helse.flex.syketilfelle.config

import no.nav.security.token.support.client.core.ClientProperties
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import no.nav.security.token.support.client.spring.oauth2.EnableOAuth2Client
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.RestTemplate
import java.time.Duration

@EnableOAuth2Client(cacheEnabled = true)
@Configuration
class AadRestTemplateConfiguration {

    @Bean
    fun pdlRestTemplate(
        restTemplateBuilder: RestTemplateBuilder,
        clientConfigurationProperties: ClientConfigurationProperties,
        oAuth2AccessTokenService: OAuth2AccessTokenService,
        httpClient: CloseableHttpClient,

    ): RestTemplate =
        downstreamRestTemplate(
            registrationName = "pdl-api-client-credentials",
            restTemplateBuilder = restTemplateBuilder,
            clientConfigurationProperties = clientConfigurationProperties,
            oAuth2AccessTokenService = oAuth2AccessTokenService,
            httpClient = httpClient,
        )

    @Bean
    fun clientHttpRequestFactory(httpClient: CloseableHttpClient): ClientHttpRequestFactory {
        return HttpComponentsClientHttpRequestFactory(httpClient)
            .also {
                it.setConnectTimeout(Duration.ofSeconds(1).toMillis().toInt())
                it.setReadTimeout(Duration.ofSeconds(1).toMillis().toInt())
            }
    }

    @Bean
    fun httpClient(): CloseableHttpClient {
        val connectionManager = PoolingHttpClientConnectionManager()
        connectionManager.defaultMaxPerRoute = 50
        connectionManager.maxTotal = 50

        return HttpClientBuilder.create()
            .setConnectionManager(connectionManager)
            .build()
    }

    private fun downstreamRestTemplate(
        restTemplateBuilder: RestTemplateBuilder,
        clientConfigurationProperties: ClientConfigurationProperties,
        oAuth2AccessTokenService: OAuth2AccessTokenService,
        httpClient: CloseableHttpClient,
        registrationName: String
    ): RestTemplate {
        val clientProperties = clientConfigurationProperties.registration[registrationName]
            ?: throw RuntimeException("Fant ikke config for $registrationName")

        return restTemplateBuilder
            .requestFactory { HttpComponentsClientHttpRequestFactory(httpClient) }
            .additionalInterceptors(bearerTokenInterceptor(clientProperties, oAuth2AccessTokenService))
            .build()
    }

    private fun bearerTokenInterceptor(
        clientProperties: ClientProperties,
        oAuth2AccessTokenService: OAuth2AccessTokenService
    ): ClientHttpRequestInterceptor {
        return ClientHttpRequestInterceptor { request: HttpRequest, body: ByteArray, execution: ClientHttpRequestExecution ->
            val response = oAuth2AccessTokenService.getAccessToken(clientProperties)
            request.headers.setBearerAuth(response.accessToken)
            execution.execute(request, body)
        }
    }
}
