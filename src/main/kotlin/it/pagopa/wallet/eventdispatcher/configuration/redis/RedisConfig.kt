package it.pagopa.wallet.eventdispatcher.configuration.redis

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import it.pagopa.wallet.eventdispatcher.repositories.redis.bean.ReceiversStatus
import it.pagopa.wallet.eventdispatcher.streams.commands.EventDispatcherReceiverCommand
import java.time.Duration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer

/** Redis templates wrapper configuration */
@Configuration
class RedisConfig {

    @Bean
    fun eventDispatcherCommandRedisTemplateWrapper(
        redisConnectionFactory: ReactiveRedisConnectionFactory
    ): EventDispatcherCommandsTemplateWrapper {
        // Setting up a ReactiveRedisTemplate for EventDispatcherReceiverCommand
        val jacksonSerializer =
            Jackson2JsonRedisSerializer(
                jacksonObjectMapper(),
                EventDispatcherReceiverCommand::class.java
            )
        val redisTemplate =
            ReactiveRedisTemplate<String, EventDispatcherReceiverCommand>(
                redisConnectionFactory,
                RedisSerializationContext.newSerializationContext<
                        String, EventDispatcherReceiverCommand
                    >(
                        StringRedisSerializer()
                    )
                    .value(jacksonSerializer)
                    .build()
            )

        return EventDispatcherCommandsTemplateWrapper(redisTemplate, Duration.ZERO)
    }

    @Bean
    fun eventDispatcherReceiverStatusTemplateWrapper(
        redisConnectionFactory: ReactiveRedisConnectionFactory
    ): EventDispatcherReceiverStatusTemplateWrapper {
        // Setting up a ReactiveRedisTemplate for ReceiversStatus
        val jacksonSerializer =
            Jackson2JsonRedisSerializer(jacksonObjectMapper(), ReceiversStatus::class.java)
        val redisTemplate =
            ReactiveRedisTemplate<String, ReceiversStatus>(
                redisConnectionFactory,
                RedisSerializationContext.newSerializationContext<String, ReceiversStatus>(
                        StringRedisSerializer()
                    )
                    .value(jacksonSerializer)
                    .build()
            )

        return EventDispatcherReceiverStatusTemplateWrapper(redisTemplate, Duration.ofMinutes(1))
    }
}
