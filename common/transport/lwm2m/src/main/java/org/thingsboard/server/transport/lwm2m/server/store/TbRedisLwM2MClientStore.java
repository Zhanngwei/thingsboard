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
package org.thingsboard.server.transport.lwm2m.server.store;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.springframework.data.redis.connection.RedisClusterConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2MClientState;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.thingsboard.server.transport.lwm2m.server.store.util.LwM2MClientSerDes.deserialize;
import static org.thingsboard.server.transport.lwm2m.server.store.util.LwM2MClientSerDes.serialize;

/**
 * 中文说明：
 * 1. 类目的：`TbRedisLwM2MClientStore` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Slf4j
public class TbRedisLwM2MClientStore implements TbLwM2MClientStore {

    /**
     * 客户端常量，用于统一引用固定值。
     */
    private static final String CLIENT_EP = "CLIENT#EP#";
    private final RedisConnectionFactory connectionFactory;

    /**
     * 功能：创建 `TbRedisLwM2MClientStore` 实例，并初始化必要字段。
     * 参数：
     * - `redisConnectionFactory`：`redisConnectionFactory` 参数。
     * 返回：新创建的对象实例。
     */
    public TbRedisLwM2MClientStore(RedisConnectionFactory redisConnectionFactory) {
        this.connectionFactory = redisConnectionFactory;
    }

    /**
     * 功能：执行 `get` 对应的处理。
     * 参数：
     * - `endpoint`：`endpoint` 参数。
     * 返回：处理结果。
     */
    @Override
    public LwM2mClient get(String endpoint) {
        try (var connection = connectionFactory.getConnection()) {
            byte[] data = connection.get(getKey(endpoint));
            if (data == null) {
                return null;
            } else {
                try {
                    return deserialize(data);
                } catch (Exception e) {
                    log.warn("[{}] Failed to deserialize client from data: {}", endpoint, Hex.encodeHexString(data), e);
                    return null;
                }
            }
        }
    }

    /**
     * 功能：获取`All`。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    @Override
    public Set<LwM2mClient> getAll() {
        try (var connection = connectionFactory.getConnection()) {
            Set<LwM2mClient> clients = new HashSet<>();
            ScanOptions scanOptions = ScanOptions.scanOptions().count(100).match(CLIENT_EP + "*").build();
            List<Cursor<byte[]>> scans = new ArrayList<>();
            if (connection instanceof RedisClusterConnection) {
                ((RedisClusterConnection) connection).clusterGetNodes().forEach(node -> {
                    scans.add(((RedisClusterConnection) connection).scan(node, scanOptions));
                });
            } else {
                scans.add(connection.scan(scanOptions));
            }

            scans.forEach(scan -> {
                scan.forEachRemaining(key -> {
                    byte[] element = connection.get(key);
                    if (element != null) {
                        try {
                            clients.add(deserialize(element));
                        } catch (Exception e) {
                            log.warn("[{}] Failed to deserialize client from data: {}", Hex.encodeHexString(key), Hex.encodeHexString(element), e);
                        }
                    }
                });
            });
            return clients;
        }
    }

    /**
     * 功能：执行 `put` 对应的处理。
     * 参数：
     * - `client`：客户端对象。
     * 返回：无。
     */
    @Override
    public void put(LwM2mClient client) {
        if (client.getState().equals(LwM2MClientState.UNREGISTERED)) {
            log.error("[{}] Client is in invalid state: {}!", client.getEndpoint(), client.getState(), new Exception());
        } else {
            byte[] clientSerialized = serialize(client);
            try (var connection = connectionFactory.getConnection()) {
                connection.getSet(getKey(client.getEndpoint()), clientSerialized);
            }
        }
    }

    /**
     * 功能：执行 `remove` 对应的处理。
     * 参数：
     * - `endpoint`：`endpoint` 参数。
     * 返回：无。
     */
    @Override
    public void remove(String endpoint) {
        try (var connection = connectionFactory.getConnection()) {
            connection.del(getKey(endpoint));
        }
    }

    /**
     * 功能：获取键。
     * 参数：
     * - `endpoint`：`endpoint` 参数。
     * 返回：处理结果。
     */
    private byte[] getKey(String endpoint) {
        return (CLIENT_EP + endpoint).getBytes();
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`TbRedisLwM2MClientStore` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
