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
package org.thingsboard.server.queue.memory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.queue.TbQueueMsg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 中文说明：
 * 1. `DefaultInMemoryStorage` 是 ThingsBoard Common Queue 中围绕 `In Memory Storage` 提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 直接依赖的类型边界包括 `InMemoryStorage`。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
@Component
@Slf4j
public final class DefaultInMemoryStorage implements InMemoryStorage {
    private final ConcurrentHashMap<String, BlockingQueue<TbQueueMsg>> storage = new ConcurrentHashMap<>();

    /**
     * 功能：执行 `printStats` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void printStats() {
        if (log.isDebugEnabled()) {
            storage.forEach((topic, queue) -> {
                if (queue.size() > 0) {
                    log.debug("[{}] Queue Size [{}]", topic, queue.size());
                }
            });
        }
    }

    /**
     * 功能：获取`Lag Total`。
     * 参数：无。
     * 返回：数值结果。
     */
    @Override
    public int getLagTotal() {
        return storage.values().stream().map(BlockingQueue::size).reduce(0, Integer::sum);
    }

    /**
     * 功能：执行 `put` 对应的处理。
     * 参数：
     * - `topic`：主题名称或主题对象。
     * - `msg`：待处理消息。
     * 返回：判断结果。
     */
    @Override
    public boolean put(String topic, TbQueueMsg msg) {
        return storage.computeIfAbsent(topic, (t) -> new LinkedBlockingQueue<>()).add(msg);
    }

    /**
     * 功能：执行 `get` 对应的处理。
     * 参数：
     * - `topic`：主题名称或主题对象。
     * 返回：匹配的数据集合。
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T extends TbQueueMsg> List<T> get(String topic) throws InterruptedException {
        final BlockingQueue<TbQueueMsg> queue = storage.get(topic);
        if (queue != null) {
            final TbQueueMsg firstMsg = queue.poll();
            if (firstMsg != null) {
                final int queueSize = queue.size();
                if (queueSize > 0) {
                    final List<TbQueueMsg> entities = new ArrayList<>(Math.min(queueSize, 999) + 1);
                    entities.add(firstMsg);
                    queue.drainTo(entities, 999);
                    return (List<T>) entities;
                }
                return Collections.singletonList((T) firstMsg);
            }
        }
        return Collections.emptyList();
    }

}
