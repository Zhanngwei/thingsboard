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

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisClusterConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.transport.lwm2m.server.model.LwM2MModelConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * 中文说明：
 * 1. `TbRedisLwM2MModelConfigStore` 是 ThingsBoard Common Transport 中负责 LwM2M 接入或传输适配的类型。
 * 2. 它处理连接、会话、协议消息或平台传输消息之间的转换。
 * 3. 类中的状态和配置用于控制当前协议交互的具体行为。
 * 4. 直接依赖的类型边界包括 `TbLwM2MModelConfigStore`。
 * 5. 单独的传输类型可以隔离协议细节，使平台内部继续使用统一消息模型。
 * 6. 阅读时重点关注入站消息入口、会话状态和消息提交位置。
 */
@Slf4j
@AllArgsConstructor
public class TbRedisLwM2MModelConfigStore implements TbLwM2MModelConfigStore {
    /**
     * `MODEL_EP`常量，用于统一引用固定值。
     */
    private static final String MODEL_EP = "MODEL#EP#";
    private final RedisConnectionFactory connectionFactory;

    /**
     * 功能：获取`All`。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<LwM2MModelConfig> getAll() {
        try (var connection = connectionFactory.getConnection()) {
            List<LwM2MModelConfig> configs = new ArrayList<>();
            ScanOptions scanOptions = ScanOptions.scanOptions().count(100).match(MODEL_EP + "*").build();
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
                    configs.add(JacksonUtil.fromBytes(element, LwM2MModelConfig.class));
                });
            });
            return configs;
        }
    }

    /**
     * 功能：执行 `put` 对应的处理。
     * 参数：
     * - `modelConfig`：配置对象。
     * 返回：无。
     */
    @Override
    public void put(LwM2MModelConfig modelConfig) {
        byte[] clientSerialized = JacksonUtil.writeValueAsBytes(modelConfig);
        try (var connection = connectionFactory.getConnection()) {
            connection.getSet(getKey(modelConfig.getEndpoint()), clientSerialized);
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
        return (MODEL_EP + endpoint).getBytes();
    }

}
