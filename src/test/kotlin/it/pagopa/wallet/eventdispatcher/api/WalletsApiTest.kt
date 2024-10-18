package it.pagopa.wallet.eventdispatcher.api

import it.pagopa.generated.wallets.model.WalletStatusErrorPatchRequest
import it.pagopa.wallet.eventdispatcher.configuration.WebClientConfiguration
import it.pagopa.wallet.eventdispatcher.configuration.properties.WalletsApiConfiguration
import it.pagopa.wallet.eventdispatcher.exceptions.WalletPatchStatusError
import java.util.*
import okhttp3.mockwebserver.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpStatusCode
import reactor.kotlin.test.test

class WalletsApiTest {

    companion object {
        private val mockWebService = MockWebServer()

        @JvmStatic
        @BeforeAll
        fun setup() {
            mockWebService.start(8888)
        }

        @JvmStatic
        @AfterAll
        fun tearDown() {
            mockWebService.shutdown()
        }
    }

    private val walletsApi =
        WalletsApi(
            WebClientConfiguration()
                .walletsApiClient(
                    WalletsApiConfiguration(
                        uri = "http://localhost:8888",
                        readTimeout = 1000,
                        connectionTimeout = 1000
                    )
                )
        )

    @Test
    fun `should handle successfully wallet update status 204 response`() {
        val walletId = UUID.randomUUID()
        mockWebService.dispatch {
            when (it.path) {
                "/wallets/$walletId" -> MockResponse().setResponseCode(204)
                else -> MockResponse().setResponseCode(400)
            }
        }
        walletsApi
            .updateWalletStatus(
                walletId = walletId,
                walletStatusPatchRequest = WalletStatusErrorPatchRequest().status("ERROR")
            )
            .test()
            .expectNext(Unit)
            .verifyComplete()
    }

    @ParameterizedTest
    @ValueSource(ints = [400, 404, 409, 500])
    fun `should handle http error code while updating wallet status`(httpErrorStatusCode: Int) {
        val walletId = UUID.randomUUID()
        val walletUpdateStatus = "ERROR"
        mockWebService.dispatch { MockResponse().setResponseCode(httpErrorStatusCode) }
        walletsApi
            .updateWalletStatus(
                walletId = walletId,
                walletStatusPatchRequest =
                    WalletStatusErrorPatchRequest().status(walletUpdateStatus)
            )
            .test()
            .expectErrorMatches {
                assertTrue(it is WalletPatchStatusError)
                assertEquals(
                    "Error patching wallet: [$walletId] to status: [$walletUpdateStatus]",
                    it.message
                )
                assertEquals(
                    HttpStatusCode.valueOf(httpErrorStatusCode),
                    (it as WalletPatchStatusError).getHttpResponseCode().get()
                )
                true
            }
            .verify()
    }

    @Test
    fun `should handle timeout patching wallet status`() {
        val walletId = UUID.randomUUID()
        val walletUpdateStatus = "ERROR"
        mockWebService.dispatch { MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE) }
        walletsApi
            .updateWalletStatus(
                walletId = walletId,
                walletStatusPatchRequest =
                    WalletStatusErrorPatchRequest().status(walletUpdateStatus)
            )
            .test()
            .expectErrorMatches {
                assertTrue(it is WalletPatchStatusError)
                assertEquals(
                    "Error patching wallet: [$walletId] to status: [$walletUpdateStatus]",
                    it.message
                )
                assertTrue((it as WalletPatchStatusError).getHttpResponseCode().isEmpty)
                true
            }
            .verify()
    }

    private fun MockWebServer.dispatch(dispatcher: (request: RecordedRequest) -> MockResponse) {
        this.dispatcher =
            object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse = dispatcher(request)
            }
    }
}
