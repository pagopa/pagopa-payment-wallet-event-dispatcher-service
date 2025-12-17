package it.pagopa.wallet.eventdispatcher.queues

import com.azure.spring.messaging.checkpoint.Checkpointer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono

private object CommonsLogger {
    val logger: Logger = LoggerFactory.getLogger(CommonsLogger.javaClass)
}

fun Checkpointer.successWithLog(message: String? = null): Mono<Unit> {
    return this.success()
        .doOnSuccess { CommonsLogger.logger.info(message ?: "Checkpoint successfully") }
        .doOnError { CommonsLogger.logger.error("Error performing checkpoint", it) }
        .thenReturn(Unit)
}
