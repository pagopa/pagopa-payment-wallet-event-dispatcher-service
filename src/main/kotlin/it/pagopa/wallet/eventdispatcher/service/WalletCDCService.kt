package it.pagopa.wallet.eventdispatcher.service

import it.pagopa.wallet.eventdispatcher.common.cdc.WalletLoggingEvent
import it.pagopa.wallet.eventdispatcher.configuration.properties.RetrySendPolicyConfig
import java.time.Duration
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.util.retry.Retry

@Component
class WalletCDCService(
    private val cdcKafkaTemplate: ReactiveKafkaProducerTemplate<String, Any>,
    private val cdcTopicName: String,
    private val retrySendPolicyConfig: RetrySendPolicyConfig
) {

    private val log = LoggerFactory.getLogger(WalletCDCService::class.java.name)

    fun sendToKafka(event: WalletLoggingEvent): Mono<Unit> {

        log.info(
            "Process event with id [{}] of type [{}] with walletId [{}] published on [{}]",
            event.id,
            event.type,
            event.walletId,
            event.timestamp
        )

        return Mono.defer {
                cdcKafkaTemplate
                    .send(cdcTopicName, event.walletId, event)
                    .doOnSuccess {
                        log.info(
                            "Succesfully sent event with id [{}] of type [{}] with walletId [{}] published on [{}]",
                            event.id,
                            event.type,
                            event.walletId,
                            event.timestamp
                        )
                    }
                    .doOnError {
                        log.error(
                            "Error while processing event with id [{}] of type [{}] with walletId [{}] published on [{}]",
                            event.id,
                            event.type,
                            event.walletId,
                            event.timestamp
                        )
                    }
            }
            .retryWhen(
                Retry.fixedDelay(
                        retrySendPolicyConfig.maxAttempts,
                        Duration.ofMillis(retrySendPolicyConfig.intervalInMs)
                    )
                    .doBeforeRetry {
                        log.warn(
                            "Retry send event with id [{}] of type [{}] with walletId [{}] published on [{}]",
                            event.id,
                            event.type,
                            event.walletId,
                            event.timestamp
                        )
                    }
            )
            .thenReturn(Unit)
    }
}
