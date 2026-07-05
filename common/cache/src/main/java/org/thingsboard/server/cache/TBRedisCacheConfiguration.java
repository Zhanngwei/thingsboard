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

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.util.Assert;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.EntityId;
import redis.clients.jedis.JedisPoolConfig;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 中文说明：
 * 1. 类目的：`TBRedisCacheConfiguration` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Configuration
@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
@EnableCaching
@Data
public abstract class TBRedisCacheConfiguration {

    /**
     * `COMMA`常量，用于统一引用固定值。
     */
    private static final String COMMA = ",";
    private static final String COLON = ":";

    /**
     * `evictTtlInMs` 字段，保存当前对象的对应属性。
     */
    @Value("${redis.evictTtlInMs:60000}")
    private int evictTtlInMs;

    /**
     * `maxTotal` 字段，保存当前对象的对应属性。
     */
    @Value("${redis.pool_config.maxTotal:128}")
    private int maxTotal;

    /**
     * `maxIdle` 字段，保存当前对象的对应属性。
     */
    @Value("${redis.pool_config.maxIdle:128}")
    private int maxIdle;

    /**
     * `minIdle` 字段，保存当前对象的对应属性。
     */
    @Value("${redis.pool_config.minIdle:16}")
    private int minIdle;

    /**
     * 是否满足`testOnBorrow`条件。
     */
    @Value("${redis.pool_config.testOnBorrow:true}")
    private boolean testOnBorrow;

    /**
     * 是否满足`testOnReturn`条件。
     */
    @Value("${redis.pool_config.testOnReturn:true}")
    private boolean testOnReturn;

    /**
     * 是否满足`testWhileIdle`条件。
     */
    @Value("${redis.pool_config.testWhileIdle:true}")
    private boolean testWhileIdle;

    /**
     * `minEvictableMs` 字段，保存当前对象的对应属性。
     */
    @Value("${redis.pool_config.minEvictableMs:60000}")
    private long minEvictableMs;

    /**
     * `evictionRunsMs` 字段，保存当前对象的对应属性。
     */
    @Value("${redis.pool_config.evictionRunsMs:30000}")
    private long evictionRunsMs;

    /**
     * `maxWaitMills` 字段，保存当前对象的对应属性。
     */
    @Value("${redis.pool_config.maxWaitMills:60000}")
    private long maxWaitMills;

    /**
     * `numberTestsPerEvictionRun` 字段，保存当前对象的对应属性。
     */
    @Value("${redis.pool_config.numberTestsPerEvictionRun:3}")
    private int numberTestsPerEvictionRun;

    /**
     * 是否满足`blockWhenExhausted`条件。
     */
    @Value("${redis.pool_config.blockWhenExhausted:true}")
    private boolean blockWhenExhausted;

    /**
     * 功能：执行 `redisConnectionFactory` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return loadFactory();
    }

    /**
     * 功能：获取工厂。
     * 参数：无。
     * 返回：处理结果。
     */
    protected abstract JedisConnectionFactory loadFactory();

    /**
     * Transaction aware RedisCacheManager.
     * Enable RedisCaches to synchronize cache put/evict operations with ongoing Spring-managed transactions.
     */
    /**
     * 功能：执行 `cacheManager` 对应的处理。
     * 参数：
     * - `cf`：`cf` 参数。
     * 返回：处理结果。
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory cf) {
        DefaultFormattingConversionService redisConversionService = new DefaultFormattingConversionService();
        RedisCacheConfiguration.registerDefaultConverters(redisConversionService);
        registerDefaultConverters(redisConversionService);
        RedisCacheConfiguration configuration = RedisCacheConfiguration.defaultCacheConfig().withConversionService(redisConversionService);
        return RedisCacheManager.builder(cf).cacheDefaults(configuration)
                .transactionAware()
                .build();
    }

    /**
     * 功能：执行 `redisTemplate` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory());
        return template;
    }

    /**
     * 功能：保存或创建`Default Converters`。
     * 参数：
     * - `registry`：`registry` 参数。
     * 返回：无。
     */
    private static void registerDefaultConverters(ConverterRegistry registry) {
        Assert.notNull(registry, "ConverterRegistry must not be null!");
        registry.addConverter(EntityId.class, String.class, EntityId::toString);
    }

    /**
     * 功能：构建配置。
     * 参数：无。
     * 返回：处理结果。
     */
    protected JedisPoolConfig buildPoolConfig() {
        final JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(maxTotal);
        poolConfig.setMaxIdle(maxIdle);
        poolConfig.setMinIdle(minIdle);
        poolConfig.setTestOnBorrow(testOnBorrow);
        poolConfig.setTestOnReturn(testOnReturn);
        poolConfig.setTestWhileIdle(testWhileIdle);
        poolConfig.setSoftMinEvictableIdleTime(Duration.ofMillis(minEvictableMs));
        poolConfig.setTimeBetweenEvictionRuns(Duration.ofMillis(evictionRunsMs));
        poolConfig.setMaxWaitMillis(maxWaitMills);
        poolConfig.setNumTestsPerEvictionRun(numberTestsPerEvictionRun);
        poolConfig.setBlockWhenExhausted(blockWhenExhausted);
        return poolConfig;
    }

    /**
     * 功能：获取`Nodes`。
     * 参数：
     * - `nodes`：`nodes` 参数。
     * 返回：匹配的数据集合。
     */
    protected List<RedisNode> getNodes(String nodes) {
        List<RedisNode> result;
        if (StringUtils.isBlank(nodes)) {
            result = Collections.emptyList();
        } else {
            result = new ArrayList<>();
            for (String hostPort : nodes.split(COMMA)) {
                String host = hostPort.split(COLON)[0];
                int port = Integer.parseInt(hostPort.split(COLON)[1]);
                result.add(new RedisNode(host, port));
            }
        }
        return result;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`TBRedisCacheConfiguration` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
