package it.pagopa.wallet.eventdispatcher.utils

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import io.opentelemetry.context.Context
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.context.propagation.TextMapSetter
import it.pagopa.wallet.eventdispatcher.common.queue.TracingInfo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import reactor.core.publisher.Mono
import reactor.kotlin.test.test

class TracingTest {
    private val openTelemetry = spy(GlobalOpenTelemetry.get())
    private val tracer = openTelemetry.getTracer("test")
    private val tracing = Tracing(openTelemetry, tracer)

    companion object {
        val MOCK_TRACING_INFO: TracingInfo =
            TracingInfo("mock_traceparent", "mock_tracestate", "mock_baggage")
    }

    @BeforeEach
    fun setup() {
        val textMapPropagator = spy(W3CTraceContextPropagator.getInstance())
        val contextPropagator = mock<ContextPropagators>()
        given {
                textMapPropagator.inject(
                    any<Context>(),
                    any<Map<Any, Any>>(),
                    any<TextMapSetter<Map<Any, Any>>>()
                )
            }
            .willAnswer {
                val map = it.getArgument<Map<Any, Any>>(1)
                val setter = it.getArgument<TextMapSetter<Map<Any, Any>>>(2)
                setter.set(map, Tracing.TRACEPARENT, "mock_traceparent")
                setter.set(map, Tracing.TRACESTATE, "mock_tracestate")
                setter.set(map, Tracing.BAGGAGE, "mock_baggage")
                return@willAnswer null
            }

        given { contextPropagator.textMapPropagator }.willReturn(textMapPropagator)
        given { openTelemetry.propagators }.willReturn(contextPropagator)
    }

    @Test
    fun traceMonoWithRemoteSpanWithMonoValueReturnsValue() {
        val expected = 0
        val operation = Mono.just(expected)

        tracing
            .traceMonoWithRemoteSpan("test", MOCK_TRACING_INFO) { operation }
            .test()
            .expectNext(expected)
            .verifyComplete()
    }

    @Test
    fun traceMonoWithRemoteSpanWithMonoValueReturnsValueEventTracingNull() {
        val expected = 0
        val operation = Mono.just(expected)

        tracing
            .traceMonoWithRemoteSpan("test", null) { operation }
            .test()
            .expectNext(expected)
            .verifyComplete()
    }

    @Test
    fun traceMonoWithMonoErrorReturnsError() {
        val expected = RuntimeException("error!")
        val operation = Mono.error<Int>(expected)

        tracing
            .traceMonoWithRemoteSpan("test", MOCK_TRACING_INFO) { operation }
            .test()
            .expectErrorMatches { it.equals(expected) }
            .verify()
    }
}
