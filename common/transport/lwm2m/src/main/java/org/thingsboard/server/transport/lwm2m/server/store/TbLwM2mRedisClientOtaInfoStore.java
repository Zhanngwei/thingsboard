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
 * 1. `TbLwM2mRedisClientOtaInfoStore` 是 ThingsBoard Common Transport 中承载 LwM2M 信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `TbLwM2MClientOtaInfoStore`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
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
