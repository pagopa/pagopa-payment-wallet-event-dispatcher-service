package it.pagopa.wallet.eventdispatcher.controller

import it.pagopa.generated.paymentwallet.eventdispatcher.server.model.*
import it.pagopa.wallet.eventdispatcher.exceptions.NoEventReceiverStatusFound
import it.pagopa.wallet.eventdispatcher.service.EventReceiverService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.reactive.server.WebTestClient

@OptIn(ExperimentalCoroutinesApi::class)
@WebFluxTest(EventReceiversApiController::class)
@TestPropertySource(locations = ["classpath:application.properties"])
class EventReceiversApiControllerTest {

    @Autowired lateinit var webClient: WebTestClient

    @MockBean lateinit var eventReceiverService: EventReceiverService

    @Test
    fun `Should handle command creation successfully`() = runTest {
        val request = EventReceiverCommandRequestDto(EventReceiverCommandRequestDto.Command.START)
        given(eventReceiverService.handleCommand(request)).willReturn(Unit)
        webClient
            .post()
            .uri("/event-receivers/commands")
            .header("x-api-key", "primary-key")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isAccepted
    }

    @Test
    fun `Should return 400 bad request for invalid command request`() = runTest {
        val expectedProblemJsonDto =
            ProblemJsonDto(
                title = "Bad request",
                status = 400,
                detail = "Input request is invalid."
            )
        webClient
            .post()
            .uri("/event-receivers/commands")
            .header("x-api-key", "primary-key")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                """
                {
                  "command": "FAKE"
                }
            """
                    .trimIndent()
            )
            .exchange()
            .expectStatus()
            .isBadRequest
            .expectBody(ProblemJsonDto::class.java)
            .isEqualTo(expectedProblemJsonDto)
    }

    @Test
    fun `Should return 500 Internal Server Error for uncaught exception`() = runTest {
        val expectedProblemJsonDto =
            ProblemJsonDto(
                title = "Internal Server Error",
                status = 500,
                detail = "An unexpected error occurred processing the request"
            )
        val request = EventReceiverCommandRequestDto(EventReceiverCommandRequestDto.Command.START)
        given(eventReceiverService.handleCommand(request))
            .willThrow(RuntimeException("Uncaught exception"))
        webClient
            .post()
            .uri("/event-receivers/commands")
            .header("x-api-key", "primary-key")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus()
            .isEqualTo(500)
            .expectBody(ProblemJsonDto::class.java)
            .isEqualTo(expectedProblemJsonDto)
    }

    @Test
    fun `Should return receiver statuses successfully`() = runTest {
        val response =
            EventReceiverStatusResponseDto(
                listOf(
                    EventReceiverStatusDto(
                        instanceId = "instanceId",
                        deploymentVersion = DeploymentVersionDto.PROD,
                        receiverStatuses =
                            listOf(
                                ReceiverStatusDto(
                                    status = ReceiverStatusDto.Status.DOWN,
                                    name = "name"
                                )
                            )
                    )
                )
            )
        given(eventReceiverService.getReceiversStatus(null)).willReturn(response)
        webClient
            .get()
            .uri("/event-receivers/status")
            .header("x-api-key", "primary-key")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(EventReceiverStatusResponseDto::class.java)
            .isEqualTo(response)
    }

    @Test
    fun `Should return 404 for no receiver information found successfully`() = runTest {
        val expectedProblemJsonDto =
            ProblemJsonDto(
                title = "Not found",
                status = 404,
                detail = "No data found for receiver statuses"
            )
        given(eventReceiverService.getReceiversStatus(null)).willThrow(NoEventReceiverStatusFound())
        webClient
            .get()
            .uri("/event-receivers/status")
            .header("x-api-key", "primary-key")
            .exchange()
            .expectStatus()
            .isNotFound
            .expectBody(ProblemJsonDto::class.java)
            .isEqualTo(expectedProblemJsonDto)
    }

    @ParameterizedTest
    @ValueSource(strings = ["invalid-key"])
    @NullSource
    fun `Should return 401 for missing or invalid api key`(apiKey: String?) = runTest {
        webClient
            .post()
            .uri("/event-receivers/commands")
            .header("x-api-key", apiKey)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{}")
            .exchange()
            .expectStatus()
            .isUnauthorized
    }
}
