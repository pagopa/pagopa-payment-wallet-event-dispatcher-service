package it.pagopa.wallet.eventdispatcher.utils

import it.pagopa.wallet.eventdispatcher.common.queue.TracingInfo
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.given
import reactor.core.publisher.Mono

object TracingMock {

    fun mock(): Tracing {
        val mockedTracingUtils = Mockito.mock(Tracing::class.java)
        given {
                mockedTracingUtils.traceMonoWithRemoteSpan(
                    any<String>(),
                    anyOrNull<TracingInfo>(),
                    any<() -> Mono<Any>>()
                )
            }
            .willAnswer { it.getArgument<() -> Mono<*>>(2).invoke() }

        given { mockedTracingUtils.customizeSpan(any<Mono<*>>(), any()) }
            .willAnswer { it.getArgument<Mono<*>>(0) }

        return mockedTracingUtils
    }
}
