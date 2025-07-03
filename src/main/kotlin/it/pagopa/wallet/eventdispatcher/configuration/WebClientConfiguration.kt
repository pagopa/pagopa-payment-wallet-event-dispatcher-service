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
        val webClient =
            ApiClient.buildWebClientBuilder()
                .clientConnector(ReactorClientHttpConnector(httpClient))
                .baseUrl(walletsApiConfiguration.uri)
                .defaultHeader("x-api-key", walletsApiConfiguration.apiKey)
                .build()
        val apiClient = ApiClient(webClient).setBasePath(walletsApiConfiguration.uri)
        return WalletsApi(apiClient)
    }
}
