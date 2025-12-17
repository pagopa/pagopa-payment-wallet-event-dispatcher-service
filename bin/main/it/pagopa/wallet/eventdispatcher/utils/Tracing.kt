package it.pagopa.wallet.eventdispatcher.utils

import com.azure.core.util.tracing.Tracer.PARENT_TRACE_CONTEXT_KEY
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.context.Context
import io.opentelemetry.context.propagation.TextMapGetter
import io.opentelemetry.instrumentation.reactor.v3_1.ContextPropagationOperator
import it.pagopa.wallet.eventdispatcher.common.queue.TracingInfo
import org.slf4j.LoggerFactory
import reactor.core.CoreSubscriber
import reactor.core.publisher.Mono

class Tracing(private val openTelemetry: OpenTelemetry, private val tracer: Tracer) {

    private val logger = LoggerFactory.getLogger(Tracing::class.java)

    companion object {
        const val MDC_WALLET_ID = "walletId"

        /** Constant for traceparent header name */
        const val TRACEPARENT: String = "traceparent"

        /** Constant for tracestate header name */
        const val TRACESTATE: String = "tracestate"

        /** Constant for baggage header name */
        const val BAGGAGE: String = "baggage"
    }

    fun <T> customizeSpan(mono: Mono<out T>, f: Span.() -> Unit): Mono<T> {
        return Mono.using(
            { Span.fromContext(Context.current()) },
            { span -> f(span).let { mono } },
            {}
        )
    }

    fun <T> traceMonoWithRemoteSpan(
        spanName: String,
        tracingInfo: TracingInfo?,
        operation: () -> Mono<T>
    ): Mono<T> {
        return Mono.using(
            { createSpanWithRemoteLink(spanName, tracingInfo) },
            { span ->
                val context = Context.current().with(span)
                val tracedOperation =
                    wrapMonoWithOpenTelemetryContext(
                        context,
                        operation()
                            .contextWrite(
                                reactor.util.context.Context.of(PARENT_TRACE_CONTEXT_KEY, context)
                            ),
                    )
                ContextPropagationOperator.runWithContext(tracedOperation, context)
            },
            { span -> span.end() }
        )
    }

    /**
     * As suggested here webclient use context available at subscribe time. This wrapper makes OTEL
     * context active at subscription time. The side effect is that any child operation now is
     * related to current active Span as a parent. see
     * https://github.com/open-telemetry/opentelemetry-java-instrumentation/issues/10011
     */
    private fun <T> wrapMonoWithOpenTelemetryContext(context: Context, mono: Mono<T>): Mono<T> {
        return object : Mono<T>() {
            override fun subscribe(coreSubscriber: CoreSubscriber<in T>) {
                context.makeCurrent().use { mono.subscribe(coreSubscriber) }
            }
        }
    }

    private fun createSpanWithRemoteLink(spanName: String, tracingInfo: TracingInfo?): Span {
        logger.debug("Creating Span with remote tracing context: {}", tracingInfo)
        val traceBuilder =
            tracer
                .spanBuilder(spanName)
                .setSpanKind(SpanKind.CONSUMER)
                .setParent(Context.current().with(Span.current()))
        if (tracingInfo != null) {
            val linkedContext =
                openTelemetry.propagators.textMapPropagator.extract(
                    Context.current(),
                    tracingInfo,
                    textMapGetter
                )
            traceBuilder.addLink(Span.fromContext(linkedContext).spanContext)
        }
        return traceBuilder.startSpan()
    }

    private val textMapGetter =
        object : TextMapGetter<TracingInfo> {
            override fun keys(trace: TracingInfo): MutableIterable<String> =
                mutableSetOf(TRACEPARENT, TRACESTATE, BAGGAGE)

            override fun get(trace: TracingInfo?, key: String): String? =
                when (key) {
                    TRACEPARENT -> trace?.traceparent
                    TRACESTATE -> trace?.tracestate
                    BAGGAGE -> trace?.baggage
                    else -> null
                }
        }
}
