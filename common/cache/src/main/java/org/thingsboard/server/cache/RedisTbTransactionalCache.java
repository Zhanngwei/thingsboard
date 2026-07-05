/**
 * Copyright © 2016-2024 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.cache;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.support.NullValue;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.connection.jedis.JedisClusterConnection;
import org.springframework.data.redis.connection.jedis.JedisConnection;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.thingsboard.server.common.data.FstStatsService;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.util.JedisClusterCRC16;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 中文说明：
 * 1. 类目的：`RedisTbTransactionalCache` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Slf4j
public abstract class RedisTbTransactionalCache<K extends Serializable, V extends Serializable> implements TbTransactionalCache<K, V> {

    private static final byte[] BINARY_NULL_VALUE = RedisSerializer.java().serialize(NullValue.INSTANCE);
    static final JedisPool MOCK_POOL = new JedisPool(); //non-null pool required for JedisConnection to trigger closing jedis connection

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    private FstStatsService fstStatsService;

    /**
     * 名称，用于标识或展示当前对象。
     */
    @Getter
    private final String cacheName;
    private final JedisConnectionFactory connectionFactory;
    /**
     * 键，用于定位映射、配置或数据项。
     */
    private final RedisSerializer<String> keySerializer = StringRedisSerializer.UTF_8;
    private final TbRedisSerializer<K, V> valueSerializer;
    /**
     * `evictExpiration` 字段，保存当前对象的对应属性。
     */
    private final Expiration evictExpiration;
    private final Expiration cacheTtl;

    /**
     * 功能：创建 `RedisTbTransactionalCache` 实例，并初始化必要字段。
     * 参数：
     * - `cacheName`：名称。
     * - `cacheSpecsMap`：键值映射。
     * - `connectionFactory`：`connectionFactory` 参数。
     * - `configuration`：配置对象。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public RedisTbTransactionalCache(String cacheName,
                                     CacheSpecsMap cacheSpecsMap,
                                     RedisConnectionFactory connectionFactory,
                                     TBRedisCacheConfiguration configuration,
                                     TbRedisSerializer<K, V> valueSerializer) {
        this.cacheName = cacheName;
        this.connectionFactory = (JedisConnectionFactory) connectionFactory;
        this.valueSerializer = valueSerializer;
        this.evictExpiration = Expiration.from(configuration.getEvictTtlInMs(), TimeUnit.MILLISECONDS);
        this.cacheTtl = Optional.ofNullable(cacheSpecsMap)
                .map(CacheSpecsMap::getSpecs)
                .map(x -> x.get(cacheName))
                .map(CacheSpecs::getTimeToLiveInMinutes)
                .map(t -> Expiration.from(t, TimeUnit.MINUTES))
                .orElseGet(Expiration::persistent);
    }

    /**
     * 功能：执行 `get` 对应的处理。
     * 参数：
     * - `key`：键。
     * 返回：处理结果。
     */
    @Override
    public TbCacheValueWrapper<V> get(K key) {
        try (var connection = connectionFactory.getConnection()) {
            byte[] rawKey = getRawKey(key);
            byte[] rawValue = connection.get(rawKey);
            if (rawValue == null) {
                return null;
            } else if (Arrays.equals(rawValue, BINARY_NULL_VALUE)) {
                return SimpleTbCacheValueWrapper.empty();
            } else {
                long startTime = System.nanoTime();
                V value = valueSerializer.deserialize(key, rawValue);
                if (value != null) {
                    fstStatsService.recordDecodeTime(value.getClass(), startTime);
                    fstStatsService.incrementDecode(value.getClass());
                }
                return SimpleTbCacheValueWrapper.wrap(value);
            }
        }
    }

    /**
     * 功能：执行 `put` 对应的处理。
     * 参数：
     * - `key`：键。
     * - `value`：值。
     * 返回：无。
     */
    @Override
    public void put(K key, V value) {
        try (var connection = connectionFactory.getConnection()) {
            put(connection, key, value, RedisStringCommands.SetOption.UPSERT);
        }
    }

    /**
     * 功能：执行 `putIfAbsent` 对应的处理。
     * 参数：
     * - `key`：键。
     * - `value`：值。
     * 返回：无。
     */
    @Override
    public void putIfAbsent(K key, V value) {
        try (var connection = connectionFactory.getConnection()) {
            put(connection, key, value, RedisStringCommands.SetOption.SET_IF_ABSENT);
        }
    }

    /**
     * 功能：执行 `evict` 对应的处理。
     * 参数：
     * - `key`：键。
     * 返回：无。
     */
    @Override
    public void evict(K key) {
        try (var connection = connectionFactory.getConnection()) {
            connection.del(getRawKey(key));
        }
    }

    /**
     * 功能：执行 `evict` 对应的处理。
     * 参数：
     * - `keys`：键。
     * 返回：无。
     */
    @Override
    public void evict(Collection<K> keys) {
        //Redis expects at least 1 key to delete. Otherwise - ERR wrong number of arguments for 'del' command
        if (keys.isEmpty()) {
            return;
        }
        try (var connection = connectionFactory.getConnection()) {
            connection.del(keys.stream().map(this::getRawKey).toArray(byte[][]::new));
        }
    }

    /**
     * 功能：删除或清理`Or Put`。
     * 参数：
     * - `key`：键。
     * - `value`：值。
     * 返回：无。
     */
    @Override
    public void evictOrPut(K key, V value) {
        try (var connection = connectionFactory.getConnection()) {
            var rawKey = getRawKey(key);
            var records = connection.del(rawKey);
            if (records == null || records == 0) {
                //We need to put the value in case of Redis, because evict will NOT cancel concurrent transaction used to "get" the missing value from cache.
                connection.set(rawKey, getRawValue(value), evictExpiration, RedisStringCommands.SetOption.UPSERT);
            }
        }
    }

    /**
     * 功能：执行 `newTransactionForKey` 对应的处理。
     * 参数：
     * - `key`：键。
     * 返回：处理结果。
     */
    @Override
    public TbCacheTransaction<K, V> newTransactionForKey(K key) {
        byte[][] rawKey = new byte[][]{getRawKey(key)};
        RedisConnection connection = watch(rawKey);
        return new RedisTbCacheTransaction<>(this, connection);
    }

    /**
     * 功能：执行 `newTransactionForKeys` 对应的处理。
     * 参数：
     * - `keys`：键。
     * 返回：处理结果。
     */
    @Override
    public TbCacheTransaction<K, V> newTransactionForKeys(List<K> keys) {
        RedisConnection connection = watch(keys.stream().map(this::getRawKey).toArray(byte[][]::new));
        return new RedisTbCacheTransaction<>(this, connection);
    }

    /**
     * 功能：获取`Connection`。
     * 参数：
     * - `rawKey`：键。
     * 返回：处理结果。
     */
    private RedisConnection getConnection(byte[] rawKey) {
        if (!connectionFactory.isRedisClusterAware()) {
            return connectionFactory.getConnection();
        }
        RedisConnection connection = connectionFactory.getClusterConnection();

        int slotNum = JedisClusterCRC16.getSlot(rawKey);
        Jedis jedis = ((JedisClusterConnection) connection).getNativeConnection().getConnectionFromSlot(slotNum);

        JedisConnection jedisConnection = new JedisConnection(jedis, MOCK_POOL, jedis.getDB());
        jedisConnection.setConvertPipelineAndTxResults(connectionFactory.getConvertPipelineAndTxResults());

        return jedisConnection;
    }

    /**
     * 功能：执行 `watch` 对应的处理。
     * 参数：
     * - `rawKeysList`：键。
     * 返回：处理结果。
     */
    private RedisConnection watch(byte[][] rawKeysList) {
        RedisConnection connection = getConnection(rawKeysList[0]);
        try {
            connection.watch(rawKeysList);
            connection.multi();
        } catch (Exception e) {
            connection.close();
            throw e;
        }
        return connection;
    }

    /**
     * 功能：获取键。
     * 参数：
     * - `key`：键。
     * 返回：处理结果。
     */
    private byte[] getRawKey(K key) {
        String keyString = cacheName + key.toString();
        byte[] rawKey;
        try {
            rawKey = keySerializer.serialize(keyString);
        } catch (Exception e) {
            log.warn("Failed to serialize the cache key: {}", key, e);
            throw new RuntimeException(e);
        }
        if (rawKey == null) {
            log.warn("Failed to serialize the cache key: {}", key);
            throw new IllegalArgumentException("Failed to serialize the cache key!");
        }
        return rawKey;
    }

    /**
     * 功能：获取值。
     * 参数：
     * - `value`：值。
     * 返回：处理结果。
     */
    private byte[] getRawValue(V value) {
        if (value == null) {
            return BINARY_NULL_VALUE;
        } else {
            try {
                long startTime = System.nanoTime();
                var bytes = valueSerializer.serialize(value);
                fstStatsService.recordEncodeTime(value.getClass(), startTime);
                fstStatsService.incrementEncode(value.getClass());
                return bytes;
            } catch (Exception e) {
                log.warn("Failed to serialize the cache value: {}", value, e);
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * 功能：执行 `put` 对应的处理。
     * 参数：
     * - `connection`：`connection` 参数。
     * - `key`：键。
     * - `value`：值。
     * - `setOption`：`setOption` 参数。
     * 返回：无。
     */
    public void put(RedisConnection connection, K key, V value, RedisStringCommands.SetOption setOption) {
        byte[] rawKey = getRawKey(key);
        byte[] rawValue = getRawValue(value);
        connection.set(rawKey, rawValue, cacheTtl, setOption);
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`RedisTbTransactionalCache` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
