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
package org.thingsboard.server.queue.discovery;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Created by ashvayka on 23.09.18.
 */
/**
 * 中文说明：
 * 1. `ConsistentHashCircle` 是 ThingsBoard Common Queue 中围绕 `Consistent Hash Circle` 提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 它直接协作于构造参数、字段类型和公开方法涉及的对象。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
@Slf4j
public class ConsistentHashCircle<T> {
    private final ConcurrentNavigableMap<Long, T> circle = new ConcurrentSkipListMap<>();

    /**
     * 功能：执行 `put` 对应的处理。
     * 参数：
     * - `hash`：`hash` 参数。
     * - `instance`：`instance` 参数。
     * 返回：无。
     */
    public void put(long hash, T instance) {
        circle.put(hash, instance);
    }

    /**
     * 功能：执行 `remove` 对应的处理。
     * 参数：
     * - `hash`：`hash` 参数。
     * 返回：无。
     */
    public void remove(long hash) {
        circle.remove(hash);
    }

    /**
     * 功能：判断`Empty`。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean isEmpty() {
        return circle.isEmpty();
    }

    /**
     * 功能：执行 `containsKey` 对应的处理。
     * 参数：
     * - `hash`：`hash` 参数。
     * 返回：判断结果。
     */
    public boolean containsKey(Long hash) {
        return circle.containsKey(hash);
    }

    /**
     * 功能：执行 `tailMap` 对应的处理。
     * 参数：
     * - `hash`：`hash` 参数。
     * 返回：处理结果。
     */
    public ConcurrentNavigableMap<Long, T> tailMap(Long hash) {
        return circle.tailMap(hash);
    }

    /**
     * 功能：执行 `firstKey` 对应的处理。
     * 参数：无。
     * 返回：数值结果。
     */
    public Long firstKey() {
        return circle.firstKey();
    }

    /**
     * 功能：执行 `get` 对应的处理。
     * 参数：
     * - `hash`：`hash` 参数。
     * 返回：处理结果。
     */
    public T get(Long hash) {
        return circle.get(hash);
    }

    /**
     * 功能：执行 `log` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void log() {
        circle.forEach((key, value) -> log.debug("{} -> {}", key, value));
    }
}
