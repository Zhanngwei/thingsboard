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
package org.thingsboard.server.service.queue;

import lombok.extern.slf4j.Slf4j;

import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 中文说明：
 * 1. `TbPackProcessingContext` 是 ThingsBoard Application 中承载 `Tb Pack Processing` 信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Slf4j
public class TbPackProcessingContext<T> {

    /**
     * 数量，用于控制数量、位置或分页范围。
     */
    private final AtomicInteger pendingCount;
    private final CountDownLatch processingTimeoutLatch;
    /**
     * `ackMap`映射关系，用于按键查找对应值。
     */
    private final ConcurrentMap<UUID, T> ackMap;
    private final ConcurrentMap<UUID, T> failedMap;

    /**
     * 功能：创建 `TbPackProcessingContext` 实例，并初始化必要字段。
     * 参数：
     * - `processingTimeoutLatch`：`processingTimeoutLatch` 参数。
     * - `ackMap`：键值映射。
     * - `failedMap`：键值映射。
     * 返回：新创建的对象实例。
     */
    public TbPackProcessingContext(CountDownLatch processingTimeoutLatch,
                                   ConcurrentMap<UUID, T> ackMap,
                                   ConcurrentMap<UUID, T> failedMap) {
        this.processingTimeoutLatch = processingTimeoutLatch;
        this.pendingCount = new AtomicInteger(ackMap.size());
        this.ackMap = ackMap;
        this.failedMap = failedMap;
    }

    /**
     * 功能：执行 `await` 对应的处理。
     * 参数：
     * - `packProcessingTimeout`：`packProcessingTimeout` 参数。
     * - `milliseconds`：`milliseconds` 参数。
     * 返回：判断结果。
     */
    public boolean await(long packProcessingTimeout, TimeUnit milliseconds) throws InterruptedException {
        return processingTimeoutLatch.await(packProcessingTimeout, milliseconds);
    }

    /**
     * 功能：处理`on Success`。
     * 参数：
     * - `id`：`id`ID。
     * 返回：无。
     */
    public void onSuccess(UUID id) {
        boolean empty = false;
        T msg = ackMap.remove(id);
        if (msg != null) {
            empty = pendingCount.decrementAndGet() == 0;
        }
        if (empty) {
            processingTimeoutLatch.countDown();
        } else {
            if (log.isTraceEnabled()) {
                log.trace("Items left: {}", ackMap.size());
                for (T t : ackMap.values()) {
                    log.trace("left item: {}", t);
                }
            }
        }
    }

    /**
     * 功能：处理失败信息。
     * 参数：
     * - `id`：`id`ID。
     * - `t`：`t` 参数。
     * 返回：无。
     */
    public void onFailure(UUID id, Throwable t) {
        boolean empty = false;
        T msg = ackMap.remove(id);
        if (msg != null) {
            empty = pendingCount.decrementAndGet() == 0;
            failedMap.put(id, msg);
            if (log.isTraceEnabled()) {
                log.trace("Items left: {}", ackMap.size());
                for (T v : ackMap.values()) {
                    log.trace("left item: {}", v);
                }
            }
        }
        if (empty) {
            processingTimeoutLatch.countDown();
        }
    }

    /**
     * 功能：获取`Ack Map`。
     * 参数：无。
     * 返回：处理结果。
     */
    public ConcurrentMap<UUID, T> getAckMap() {
        return ackMap;
    }

    /**
     * 功能：获取`Failed Map`。
     * 参数：无。
     * 返回：处理结果。
     */
    public ConcurrentMap<UUID, T> getFailedMap() {
        return failedMap;
    }
}
