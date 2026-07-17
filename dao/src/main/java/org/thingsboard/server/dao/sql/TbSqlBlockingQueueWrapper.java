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
package org.thingsboard.server.dao.sql;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.stats.MessagesStats;
import org.thingsboard.server.common.stats.StatsFactory;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 中文说明：
 * 1. `TbSqlBlockingQueueWrapper` 是 ThingsBoard DAO 中负责队列存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 它直接协作于持久化模型、查询实现和对应领域服务。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
 */
@Slf4j
@Data
public class TbSqlBlockingQueueWrapper<E> {
    private final CopyOnWriteArrayList<TbSqlBlockingQueue<E>> queues = new CopyOnWriteArrayList<>();
    /**
     * `params` 字段，保存当前对象的对应属性。
     */
    private final TbSqlBlockingQueueParams params;
    private ScheduledLogExecutorComponent logExecutor;
    /**
     * 哈希函数，表示当前对象的对应属性。
     */
    private final Function<E, Integer> hashCodeFunction;
    private final int maxThreads;
    /**
     * 工厂，用于按场景创建或提供目标对象。
     */
    private final StatsFactory statsFactory;

    /**
     * Starts TbSqlBlockingQueues.
     *
     * @param  logExecutor  executor that will be printing logs and statistics
     * @param  saveFunction function to save entities in database
     * @param  batchUpdateComparator comparator to sort entities by primary key to avoid deadlocks in cluster mode
     *                               NOTE: you must use all of primary key parts in your comparator
     */
    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：
     * - `logExecutor`：`logExecutor` 参数。
     * - `saveFunction`：数据列表。
     * - `batchUpdateComparator`：`batchUpdateComparator` 参数。
     * 返回：无。
     */
    public void init(ScheduledLogExecutorComponent logExecutor, Consumer<List<E>> saveFunction, Comparator<E> batchUpdateComparator) {
        for (int i = 0; i < maxThreads; i++) {
            MessagesStats stats = statsFactory.createMessagesStats(params.getStatsNamePrefix() + ".queue." + i);
            TbSqlBlockingQueue<E> queue = new TbSqlBlockingQueue<>(params, stats);
            queues.add(queue);
            queue.init(logExecutor, saveFunction, batchUpdateComparator, i);
        }
    }

    /**
     * 功能：执行 `add` 对应的处理。
     * 参数：
     * - `element`：`element` 参数。
     * 返回：匹配的数据集合。
     */
    public ListenableFuture<Void> add(E element) {
        int queueIndex = element != null ? (hashCodeFunction.apply(element) & 0x7FFFFFFF) % maxThreads : 0;
        return queues.get(queueIndex).add(element);
    }

    /**
     * 功能：执行 `destroy` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void destroy() {
        queues.forEach(TbSqlBlockingQueue::destroy);
    }
}
