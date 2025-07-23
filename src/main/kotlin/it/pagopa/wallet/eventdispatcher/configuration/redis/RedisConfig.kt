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

    //    @Bean
    //    fun eventDispatcherCommandRedisTemplateWrapper(
    //        redisConnectionFactory: RedisConnectionFactory
    //    ): EventDispatcherCommandsTemplateWrapper {
    //        val redisTemplate = RedisTemplate<String, EventDispatcherReceiverCommand>()
    //        redisTemplate.connectionFactory = redisConnectionFactory
    //        redisTemplate.defaultSerializer = StringRedisSerializer()
    //        redisTemplate.afterPropertiesSet()
    //        /*
    //         * This redis template instance is to write events to Redis Stream through
    // opsForStreams apis.
    //         * No document is written into cache.
    //         * Set TTL to 0 here will throw an error during writing operation to cache to enforce
    // the fact that this
    //         * wrapper has to be used only to write to Redis Streams
    //         */
    //        return EventDispatcherCommandsTemplateWrapper(redisTemplate, Duration.ZERO)
    //    }

    @Bean
    fun eventDispatcherCommandRedisTemplateWrapper(
        redisConnectionFactory: ReactiveRedisConnectionFactory
    ): EventDispatcherCommandsTemplateWrapper {
        // Setting up a ReactiveRedisTemplate for EventDispatcherReceiverCommand
        val jacksonSerializer =
            Jackson2JsonRedisSerializer(EventDispatcherReceiverCommand::class.java)
        val redisTemplate =
            ReactiveRedisTemplate<String, EventDispatcherReceiverCommand>(
                redisConnectionFactory,
                RedisSerializationContext.newSerializationContext<
                        String, EventDispatcherReceiverCommand
                    >()
                    .key(StringRedisSerializer())
                    .value(jacksonSerializer)
                    .build()
            )

        return EventDispatcherCommandsTemplateWrapper(redisTemplate, Duration.ZERO)
    }

    //    @Bean
    //    fun eventDispatcherReceiverStatusTemplateWrapper(
    //        redisConnectionFactory: RedisConnectionFactory
    //    ): EventDispatcherReceiverStatusTemplateWrapper {
    //        val jacksonSerializer =
    //            Jackson2JsonRedisSerializer(jacksonObjectMapper(), ReceiversStatus::class.java)
    //        val redisTemplate = RedisTemplate<String, ReceiversStatus>()
    //        redisTemplate.connectionFactory = redisConnectionFactory
    //        redisTemplate.keySerializer = StringRedisSerializer()
    //        redisTemplate.valueSerializer = jacksonSerializer
    //        redisTemplate.afterPropertiesSet()
    //        return EventDispatcherReceiverStatusTemplateWrapper(redisTemplate,
    // Duration.ofMinutes(1))
    //    }

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
                RedisSerializationContext.newSerializationContext<String, ReceiversStatus>()
                    .key(StringRedisSerializer())
                    .value(jacksonSerializer)
                    .build()
            )

        return EventDispatcherReceiverStatusTemplateWrapper(redisTemplate, Duration.ofMinutes(1))
    }
}
