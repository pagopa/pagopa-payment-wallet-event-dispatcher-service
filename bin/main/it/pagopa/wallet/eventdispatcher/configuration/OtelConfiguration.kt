package it.pagopa.wallet.eventdispatcher.configuration

import io.opentelemetry.api.GlobalOpenTelemetry
import it.pagopa.wallet.eventdispatcher.utils.Tracing
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OtelConfiguration {

    @Bean
    fun tracing(): Tracing {
        val openTelemetry = GlobalOpenTelemetry.get()
        val tracer = openTelemetry.getTracer("pagopa-payment-wallet-event-dispatcher-service")
        return Tracing(openTelemetry, tracer)
    }
}
