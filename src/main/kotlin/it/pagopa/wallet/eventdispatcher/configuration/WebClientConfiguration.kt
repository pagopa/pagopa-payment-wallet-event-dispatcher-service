package it.pagopa.wallet.eventdispatcher.configuration

import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import it.pagopa.generated.wallets.ApiClient
import it.pagopa.generated.wallets.api.WalletsApi
import it.pagopa.wallet.eventdispatcher.configuration.properties.WalletsApiConfiguration
import java.util.concurrent.TimeUnit
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import reactor.core.publisher.Mono
import reactor.netty.Connection
import reactor.netty.http.client.HttpClient

@Configuration
class WebClientConfiguration {

    @Bean(name = ["walletsApiClient"])
    fun walletsApiClient(walletsApiConfiguration: WalletsApiConfiguration): WalletsApi {
        val httpClient =
            HttpClient.create()
                .option(
                    ChannelOption.CONNECT_TIMEOUT_MILLIS,
                    walletsApiConfiguration.connectionTimeout
                )
                .doOnConnected { connection: Connection ->
                    connection.addHandlerLast(
                        ReadTimeoutHandler(
                            walletsApiConfiguration.readTimeout.toLong(),
                            TimeUnit.MILLISECONDS
                        )
                    )
                }
        val apiKeyFilter =
            ExchangeFilterFunction.ofRequestProcessor { clientRequest ->
                val newRequest =
                    ClientRequest.from(clientRequest)
                        .header("x-api-key", walletsApiConfiguration.apiKey)
                        .build()
                Mono.just(newRequest)
            }
        val webClient =
            ApiClient.buildWebClientBuilder()
                .clientConnector(ReactorClientHttpConnector(httpClient))
                .baseUrl(walletsApiConfiguration.uri)
                .filter(apiKeyFilter)
                .build()
        val apiClient = ApiClient(webClient).setBasePath(walletsApiConfiguration.uri)
        return WalletsApi(apiClient)
    }
}
