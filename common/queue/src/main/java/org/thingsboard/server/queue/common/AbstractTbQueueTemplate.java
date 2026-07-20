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
package org.thingsboard.server.queue.common;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 中文说明：
 * 1. `AbstractTbQueueTemplate` 是 ThingsBoard Common Queue 中围绕队列提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 它直接协作于构造参数、字段类型和公开方法涉及的对象。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
public class AbstractTbQueueTemplate {
    /**
     * 请求常量，用于统一引用固定值。
     */
    protected static final String REQUEST_ID_HEADER = "requestId";
    protected static final String RESPONSE_TOPIC_HEADER = "responseTopic";
    /**
     * 时间戳常量，用于统一引用固定值。
     */
    protected static final String EXPIRE_TS_HEADER = "expireTs";

    /**
     * 功能：执行 `uuidToBytes` 对应的处理。
     * 参数：
     * - `uuid`：`uuid`ID。
     * 返回：处理结果。
     */
    protected byte[] uuidToBytes(UUID uuid) {
        ByteBuffer buf = ByteBuffer.allocate(16);
        buf.putLong(uuid.getMostSignificantBits());
        buf.putLong(uuid.getLeastSignificantBits());
        return buf.array();
    }

    /**
     * 功能：执行 `bytesToUuid` 对应的处理。
     * 参数：
     * - `bytes`：`bytes` 参数。
     * 返回：处理结果。
     */
    protected static UUID bytesToUuid(byte[] bytes) {
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        long firstLong = bb.getLong();
        long secondLong = bb.getLong();
        return new UUID(firstLong, secondLong);
    }

    /**
     * 功能：执行 `stringToBytes` 对应的处理。
     * 参数：
     * - `string`：`string` 参数。
     * 返回：处理结果。
     */
    protected byte[] stringToBytes(String string) {
        return string.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 功能：执行 `bytesToString` 对应的处理。
     * 参数：
     * - `data`：待处理数据。
     * 返回：文本结果。
     */
    protected String bytesToString(byte[] data) {
        return new String(data, StandardCharsets.UTF_8);
    }

    /**
     * 功能：执行 `longToBytes` 对应的处理。
     * 参数：
     * - `x`：`x` 参数。
     * 返回：处理结果。
     */
    protected static byte[] longToBytes(long x) {
        ByteBuffer longBuffer = ByteBuffer.allocate(Long.BYTES);
        longBuffer.putLong(0, x);
        return longBuffer.array();
    }

    /**
     * 功能：执行 `bytesToLong` 对应的处理。
     * 参数：
     * - `bytes`：`bytes` 参数。
     * 返回：数值结果。
     */
    protected static long bytesToLong(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getLong();
    }
}
