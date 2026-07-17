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
package org.thingsboard.server.common.data;

import lombok.extern.slf4j.Slf4j;
import org.nustaq.serialization.FSTConfiguration;

/**
 * 中文说明：
 * 1. `FSTUtils` 是 ThingsBoard Common Data 中处理 `FST Utils` 通用操作的工具类型。
 * 2. 它提供无状态或轻量的复用方法，减少多个调用点的重复实现。
 * 3. 方法通常完成格式化、校验、计算或简单对象构造。
 * 4. 它直接协作于方法参数和返回值所代表的数据类型。
 * 5. 集中工具方法可以统一边界行为，并降低细节变化对调用方的影响。
 * 6. 阅读时重点关注输入约束、边界值和方法是否修改传入对象。
 */
@Slf4j
public class FSTUtils {

    public static final FSTConfiguration CONFIG = FSTConfiguration.createDefaultConfiguration();

    /**
     * 功能：执行 `decode` 对应的处理。
     * 参数：
     * - `byteArray`：`byteArray` 参数。
     * 返回：处理结果。
     */
    @SuppressWarnings("unchecked")
    public static <T> T decode(byte[] byteArray) {
        return byteArray != null && byteArray.length > 0 ? (T) CONFIG.asObject(byteArray) : null;
    }

    /**
     * 功能：执行 `encode` 对应的处理。
     * 参数：
     * - `msq`：`msq` 参数。
     * 返回：处理结果。
     */
    public static <T> byte[] encode(T msq) {
        return CONFIG.asByteArray(msq);
    }

}
