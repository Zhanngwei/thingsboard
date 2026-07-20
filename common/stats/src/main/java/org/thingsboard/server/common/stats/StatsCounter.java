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
package org.thingsboard.server.common.stats;

import io.micrometer.core.instrument.Counter;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 中文说明：
 * 1. `StatsCounter` 是 ThingsBoard Common 中围绕统计数据提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 直接依赖的类型边界包括 `DefaultCounter`。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
public class StatsCounter extends DefaultCounter {
    /**
     * 名称，用于标识或展示当前对象。
     */
    private final String name;

    /**
     * 功能：创建 `StatsCounter` 实例，并初始化必要字段。
     * 参数：
     * - `aiCounter`：`aiCounter` 参数。
     * - `micrometerCounter`：`micrometerCounter` 参数。
     * - `name`：名称。
     * 返回：新创建的对象实例。
     */
    public StatsCounter(AtomicInteger aiCounter, Counter micrometerCounter, String name) {
        super(aiCounter, micrometerCounter);
        this.name = name;
    }

    /**
     * 功能：获取名称。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getName() {
        return name;
    }
}
