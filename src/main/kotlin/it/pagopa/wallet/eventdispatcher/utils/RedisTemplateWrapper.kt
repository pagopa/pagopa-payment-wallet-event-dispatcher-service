package it.pagopa.wallet.eventdispatcher.utils

import java.time.Duration
import org.springframework.data.redis.connection.stream.ObjectRecord
import org.springframework.data.redis.connection.stream.RecordId
import org.springframework.data.redis.core.RedisTemplate

/**
 * This class is a [RedisTemplate] wrapper class, used to centralize commons RedisTemplate
 * operations
 *
 * @param <V> - the RedisTemplate value type </V>
 */
abstract class RedisTemplateWrapper<V>(
    private val redisTemplate: RedisTemplate<String, V>,
    private val keyspace: String,
    private val defaultTTL: Duration
) {

    /**
     * Save the input entity into Redis. The entity TTL will be set to the default configured one
     *
     * @param value - the entity to be saved
     * @param ttl - the TTL to be used for save operation (with default ttl as value)
     */
    fun save(value: V, ttl: Duration = defaultTTL) {
        redisTemplate.opsForValue()[compoundKeyWithKeyspace(getKeyFromEntity(value)), value] = ttl
    }

    /**
     * Write an event to the stream with the specified key trimming events before writing the new
     * events so that stream has the wanted size
     *
     * @param streamKey the stream key where send the event to
     * @param event the event to be sent
     * @param streamSize the wanted length of the stream
     * @return the [RecordId] associated to the written event
     */
    fun writeEventToStreamTrimmingEvents(streamKey: String, event: V, streamSize: Long): RecordId? {
        require(streamSize >= 0) { "Invalid input $streamSize events to trim, it must be >=0" }
        redisTemplate.opsForStream<Any, Any>().trim(streamKey, streamSize)
        return redisTemplate.opsForStream<Any, Any>().add(ObjectRecord.create(streamKey, event))
    }

    /**
     * Get all the keys in keyspace
     *
     * @return a set populated with all the keys in keyspace
     */
    fun keysInKeyspace(): Set<String> = redisTemplate.keys("$keyspace*")

    /**
     * Get all the values in keyspace
     *
     * @return a list populated with all the entries in keyspace
     */
    fun allValuesInKeySpace(): MutableList<V>? =
        redisTemplate.opsForValue().multiGet(keysInKeyspace())

    /**
     * Get the Redis key from the input entity
     *
     * @param value - the entity value from which retrieve the Redis key
     * @return the key associated to the input entity
     */
    protected abstract fun getKeyFromEntity(value: V): String

    private fun compoundKeyWithKeyspace(key: String): String {
        return "$keyspace:$key"
    }
}
