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
package org.thingsboard.script.api;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 中文说明：
 * 1. `BlockedScriptInfo` 是 ThingsBoard Common 中承载脚本执行信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
public class BlockedScriptInfo {
    /**
     * 持续时间，用于控制时间范围或等待时长。
     */
    private final long maxScriptBlockDurationMs;
    private final AtomicInteger counter;
    /**
     * 过期时间，用于判断当前对象是否仍然有效。
     */
    private long expirationTime;

    BlockedScriptInfo(int maxScriptBlockDuration) {
        this.maxScriptBlockDurationMs = TimeUnit.SECONDS.toMillis(maxScriptBlockDuration);
        this.counter = new AtomicInteger(0);
    }

    /**
     * 功能：执行 `get` 对应的处理。
     * 参数：无。
     * 返回：数值结果。
     */
    public int get() {
        return counter.get();
    }

    /**
     * 功能：执行 `incrementAndGet` 对应的处理。
     * 参数：无。
     * 返回：数值结果。
     */
    public int incrementAndGet() {
        int result = counter.incrementAndGet();
        expirationTime = System.currentTimeMillis() + maxScriptBlockDurationMs;
        return result;
    }

    /**
     * 功能：获取过期时间。
     * 参数：无。
     * 返回：数值结果。
     */
    public long getExpirationTime() {
        return expirationTime;
    }
}
