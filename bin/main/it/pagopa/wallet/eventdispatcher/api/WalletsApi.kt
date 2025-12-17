package it.pagopa.wallet.eventdispatcher.api

import it.pagopa.generated.wallets.model.WalletStatusPatchRequest
import it.pagopa.wallet.eventdispatcher.exceptions.WalletPatchStatusError
import java.util.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono

@Component
class WalletsApi(private val walletsApiClient: it.pagopa.generated.wallets.api.WalletsApi) {

    private val logger = LoggerFactory.getLogger(WalletsApi::class.java.name)

    fun updateWalletStatus(
        walletId: UUID,
        walletStatusPatchRequest: WalletStatusPatchRequest
    ): Mono<Unit> {
        return walletsApiClient
            .patchWalletWithHttpInfo(walletId, walletStatusPatchRequest)
            .doOnNext {
                logger.info(
                    "Wallet status patched successfully! walletId [{}] to status: [{}]",
                    walletId,
                    walletStatusPatchRequest.status
                )
            }
            .onErrorMap { error ->
                val (httpResponseCode, httpResponseBody) =
                    Optional.of(error)
                        .filter { it is WebClientResponseException }
                        .map { it as WebClientResponseException }
                        .map { it.statusCode.value() to it.responseBodyAsString }
                        .orElse(0 to "N/A")
                logger.error(
                    "Error patching wallet status: HTTP status code: [$httpResponseCode], response body: [$httpResponseBody]",
                    error
                )
                WalletPatchStatusError(
                    walletId = walletId,
                    patchWalletStatus = walletStatusPatchRequest.status,
                    cause = error
                )
            }
            .thenReturn(Unit)
    }
}
