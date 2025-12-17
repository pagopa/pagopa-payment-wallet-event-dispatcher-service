package it.pagopa.wallet.eventdispatcher.exceptions

import java.util.*
import org.springframework.http.HttpStatusCode
import org.springframework.web.reactive.function.client.WebClientResponseException

class WalletPatchStatusError(val walletId: UUID, val patchWalletStatus: String, cause: Throwable?) :
    Exception("Error patching wallet: [$walletId] to status: [$patchWalletStatus]", cause) {

    fun getHttpResponseCode(): Optional<HttpStatusCode> =
        Optional.ofNullable(cause)
            .filter { it is WebClientResponseException }
            .map { it as WebClientResponseException }
            .map { it.statusCode }
}
