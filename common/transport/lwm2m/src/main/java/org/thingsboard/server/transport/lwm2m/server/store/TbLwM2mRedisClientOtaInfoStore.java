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

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.transport.lwm2m.server.ota.LwM2MClientOtaInfo;
import org.thingsboard.server.transport.lwm2m.server.ota.firmware.LwM2MClientFwOtaInfo;
import org.thingsboard.server.transport.lwm2m.server.ota.software.LwM2MClientSwOtaInfo;

/**
 * 中文说明：
 * 1. 类目的：`TbLwM2mRedisClientOtaInfoStore` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
public class TbLwM2mRedisClientOtaInfoStore implements TbLwM2MClientOtaInfoStore {
    /**
     * `OTA_EP`常量，用于统一引用固定值。
     */
    private static final String OTA_EP = "OTA#EP#";

    /**
     * 工厂，用于按场景创建或提供目标对象。
     */
    private final RedisConnectionFactory connectionFactory;

    /**
     * 功能：创建 `TbLwM2mRedisClientOtaInfoStore` 实例，并初始化必要字段。
     * 参数：
     * - `connectionFactory`：`connectionFactory` 参数。
     * 返回：新创建的对象实例。
     */
    public TbLwM2mRedisClientOtaInfoStore(RedisConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    /**
     * 功能：执行 `put` 对应的处理。
     * 参数：
     * - `type`：类型。
     * - `info`：`info` 参数。
     * 返回：无。
     */
    private void put(OtaPackageType type, LwM2MClientOtaInfo<?, ?, ?> info) {
        try (var connection = connectionFactory.getConnection()) {
            connection.set((OTA_EP + type + info.getEndpoint()).getBytes(), JacksonUtil.toString(info).getBytes());
        }
    }

    /**
     * 功能：获取`Fw`。
     * 参数：
     * - `endpoint`：`endpoint` 参数。
     * 返回：处理结果。
     */
    @Override
    public LwM2MClientFwOtaInfo getFw(String endpoint) {
        return getLwM2MClientOtaInfo(OtaPackageType.FIRMWARE, endpoint, LwM2MClientFwOtaInfo.class);
    }

    /**
     * 功能：执行 `putFw` 对应的处理。
     * 参数：
     * - `info`：`info` 参数。
     * 返回：无。
     */
    @Override
    public void putFw(LwM2MClientFwOtaInfo info) {
        put(OtaPackageType.FIRMWARE, info);
    }

    /**
     * 功能：获取`Sw`。
     * 参数：
     * - `endpoint`：`endpoint` 参数。
     * 返回：处理结果。
     */
    @Override
    public LwM2MClientSwOtaInfo getSw(String endpoint) {
        return getLwM2MClientOtaInfo(OtaPackageType.SOFTWARE, endpoint, LwM2MClientSwOtaInfo.class);
    }

    /**
     * 功能：执行 `putSw` 对应的处理。
     * 参数：
     * - `info`：`info` 参数。
     * 返回：无。
     */
    @Override
    public void putSw(LwM2MClientSwOtaInfo info) {
        put(OtaPackageType.SOFTWARE, info);
    }

    /**
     * 功能：获取客户端。
     * 参数：
     * - `type`：类型。
     * - `endpoint`：`endpoint` 参数。
     * - `clazz`：`clazz` 参数。
     * 返回：处理结果。
     */
    private <T extends LwM2MClientOtaInfo<?, ?, ?>> T getLwM2MClientOtaInfo(OtaPackageType type, String endpoint, Class<T> clazz) {
        try (var connection = connectionFactory.getConnection()) {
            byte[] data = connection.get((OTA_EP + type + endpoint).getBytes());
            return JacksonUtil.fromBytes(data, clazz);
        }
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`TbLwM2mRedisClientOtaInfoStore` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
