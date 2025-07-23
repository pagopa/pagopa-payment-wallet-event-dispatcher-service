package it.pagopa.wallet.eventdispatcher.utils

import java.time.Duration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import org.springframework.data.redis.connection.stream.ObjectRecord
import org.springframework.data.redis.connection.stream.RecordId
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.ReactiveStreamOperations
import org.springframework.data.redis.core.ReactiveValueOperations
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class RedisTemplateWrapperTest {

    private val defaultTtl = Duration.ofSeconds(1)
    private val keyspace = "keyspace"
    private val reactiveRedisTemplate: ReactiveRedisTemplate<String, String> = mock()
    private val opsForValue: ReactiveValueOperations<String, String> = mock()
    private val opsForStream: ReactiveStreamOperations<String, String, String> = mock()

    class MockedRedisTemplateWrapper(
        defaultTtl: Duration,
        keyspace: String,
        reactiveRedisTemplate: ReactiveRedisTemplate<String, String>
    ) :
        ReactiveRedisTemplateWrapper<String>(
            defaultTTL = defaultTtl,
            reactiveRedisTemplate = reactiveRedisTemplate,
            keyspace = keyspace
        ) {
        override fun getKeyFromEntity(value: String): String = value
    }

    private val mockedRedisTemplateWrapper =
        MockedRedisTemplateWrapper(
            defaultTtl = defaultTtl,
            reactiveRedisTemplate = reactiveRedisTemplate,
            keyspace = keyspace
        )

    @Test
    fun `Should save entity successfully with default TTL`() {
        // pre-requisites
        given(reactiveRedisTemplate.opsForValue()).willReturn(opsForValue)
        given(opsForValue.set(any(), any(), any<Duration>())).willReturn(Mono.just(true))
        val valueToSet = "test"
        val expectedKey = "$keyspace:$valueToSet"
        // test
        mockedRedisTemplateWrapper.save(valueToSet).block()
        // assertions
        verify(reactiveRedisTemplate, times(1)).opsForValue()
        verify(opsForValue, times(1)).set(expectedKey, valueToSet, defaultTtl)
    }

    @Test
    fun `Should save entity successfully with custom TTL`() {
        // pre-requisites
        given(reactiveRedisTemplate.opsForValue()).willReturn(opsForValue)
        given(opsForValue.set(any(), any(), any<Duration>())).willReturn(Mono.just(true))
        val valueToSet = "test"
        val expectedKey = "$keyspace:$valueToSet"
        val customTTL = defaultTtl + Duration.ofSeconds(1)
        // test
        mockedRedisTemplateWrapper.save(value = valueToSet, ttl = customTTL).block()
        // assertions
        verify(reactiveRedisTemplate, times(1)).opsForValue()
        verify(opsForValue, times(1)).set(expectedKey, valueToSet, customTTL)
    }

    @Test
    fun `Should write event to stream trimming old events`() {
        // pre-requisites
        val streamKey = "streamKey"
        val event = "event"
        val streamSize = 1L
        val recordId = RecordId.of(0, 0)
        given(reactiveRedisTemplate.opsForStream<String, String>()).willReturn(opsForStream)
        given(opsForStream.trim(any(), any())).willReturn(Mono.just(0L))
        given(opsForStream.add(any())).willReturn(Mono.just(recordId))
        // test
        mockedRedisTemplateWrapper
            .writeEventToStreamTrimmingEvents(
                streamKey = streamKey,
                event = event,
                streamSize = streamSize
            )
            .block()
        // assertions
        verify(reactiveRedisTemplate, times(2)).opsForStream<String, String>()
        verify(opsForStream, times(1)).trim(streamKey, streamSize)
        verify(opsForStream, times(1)).add(ObjectRecord.create(streamKey, event))
    }

    @Test
    fun `Should throw IllegalArgumentException write event to stream with invalid trimming event size`() {
        // pre-requisites
        val streamKey = "streamKey"
        val event = "event"
        val streamSize = -1L
        val recordId = RecordId.of(0, 0)
        given(reactiveRedisTemplate.opsForStream<String, String>()).willReturn(opsForStream)
        given(opsForStream.trim(any(), any())).willReturn(Mono.just(0L))
        given(opsForStream.add(any())).willReturn(Mono.just(recordId))
        // test
        val exception =
            assertThrows<IllegalArgumentException> {
                mockedRedisTemplateWrapper
                    .writeEventToStreamTrimmingEvents(
                        streamKey = streamKey,
                        event = event,
                        streamSize = streamSize
                    )
                    .block()
            }
        // assertions
        assertEquals("Invalid input $streamSize events to trim, it must be >=0", exception.message)
        verify(reactiveRedisTemplate, times(0)).opsForStream<String, String>()
        verify(opsForStream, times(0)).trim(streamKey, streamSize)
        verify(opsForStream, times(0)).add(ObjectRecord.create(streamKey, event))
    }

    @Test
    fun `Should find all keys in keyspace`() {
        // pre-requisites
        given(reactiveRedisTemplate.keys(any())).willReturn(Flux.empty())
        // test
        mockedRedisTemplateWrapper.keysInKeyspace().collectList().block()
        // assertions
        verify(reactiveRedisTemplate, times(1)).keys("$keyspace*")
    }

    @Test
    fun `Should find all values in keyspace`() {
        // pre-requisites
        given(reactiveRedisTemplate.keys(any())).willReturn(Flux.just("key1", "key2"))
        given(reactiveRedisTemplate.opsForValue()).willReturn(opsForValue)
        given(opsForValue.multiGet(any())).willReturn(Mono.just(listOf<String>()))
        // test
        mockedRedisTemplateWrapper.allValuesInKeySpace().collectList().block()
        // assertions
        verify(reactiveRedisTemplate, times(1)).keys("$keyspace*")
        verify(reactiveRedisTemplate, times(1)).opsForValue()
        verify(opsForValue, times(1)).multiGet(setOf("key1", "key2"))
    }
}
