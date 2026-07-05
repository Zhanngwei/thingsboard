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
package org.thingsboard.server.common.transport.limits;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.queue.util.TbTransportComponent;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 中文说明：
 * 1. 类目的：`DefaultEntityLimitsCache` 是ThingsBoard Common 模块中的传输协议契约或适配类型，用于抽象 MQTT、HTTP、CoAP、LwM2M、SNMP 与 ThingsBoard 核心消息之间的协议边界。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括Transport Service、设备会话、队列、Actor、Rule Engine、遥测服务和协议客户端。
 * 4. 生命周期：由传输层组件在连接建立、消息上报、RPC、属性读写或测试流程中创建和调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Adapter / Strategy / Command。
 */
@Service
@TbTransportComponent
@Slf4j
public class DefaultEntityLimitsCache implements EntityLimitsCache {

    /**
     * `DEVIATION`常量，用于统一引用固定值。
     */
    private static final int DEVIATION = 10;
    private final Cache<EntityLimitKey, Boolean> cache;

    /**
     * 功能：创建 `DefaultEntityLimitsCache` 实例，并初始化必要字段。
     * 参数：
     * - `ttl`：`ttl` 参数。
     * - `maxSize`：`maxSize` 参数。
     * 返回：新创建的对象实例。
     */
    public DefaultEntityLimitsCache(@Value("${cache.entityLimits.timeToLiveInMinutes:5}") int ttl,
                                    @Value("${cache.entityLimits.maxSize:100000}") int maxSize) {
        // We use the 'random' expiration time to avoid peak loads.
        long mainPart = (TimeUnit.MINUTES.toNanos(ttl) / 100) * (100 - DEVIATION);
        long randomPart = (TimeUnit.MINUTES.toNanos(ttl) / 100) * DEVIATION;
        cache = Caffeine.newBuilder()
                .expireAfter(new Expiry<EntityLimitKey, Boolean>() {
                    @Override
                    public long expireAfterCreate(@NotNull EntityLimitKey key, @NotNull Boolean value, long currentTime) {
                        return mainPart + (long) (randomPart * ThreadLocalRandom.current().nextDouble());
                    }

                    @Override
                    public long expireAfterUpdate(@NotNull EntityLimitKey key, @NotNull Boolean value, long currentTime, long currentDuration) {
                        return currentDuration;
                    }

                    @Override
                    public long expireAfterRead(@NotNull EntityLimitKey key, @NotNull Boolean value, long currentTime, long currentDuration) {
                        return currentDuration;
                    }
                })
                .maximumSize(maxSize)
                .build();
    }

    /**
     * 功能：执行 `get` 对应的处理。
     * 参数：
     * - `key`：键。
     * 返回：判断结果。
     */
    @Override
    public boolean get(EntityLimitKey key) {
        var result = cache.getIfPresent(key);
        return result != null ? result : false;
    }

    /**
     * 功能：执行 `put` 对应的处理。
     * 参数：
     * - `key`：键。
     * - `value`：值。
     * 返回：无。
     */
    @Override
    public void put(EntityLimitKey key, boolean value) {
        cache.put(key, value);
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultEntityLimitsCache` 在 ThingsBoard Common 模块 中承担传输协议契约或适配类型职责，核心目的是抽象 MQTT、HTTP、CoAP、LwM2M、SNMP 与 ThingsBoard 核心消息之间的协议边界。
 * 2. 核心流程：解析协议输入，转换为核心消息或响应对象，再交给队列、Actor 或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括Transport Service、设备会话、队列、Actor、Rule Engine、遥测服务和协议客户端。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
