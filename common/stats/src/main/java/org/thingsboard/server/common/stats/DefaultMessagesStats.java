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

/**
 * 中文说明：
 * 1. `DefaultMessagesStats` 是 ThingsBoard Common 中承载统计数据信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `MessagesStats`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
public class DefaultMessagesStats implements MessagesStats {
    /**
     * 计数器，用于控制处理规模或位置。
     */
    private final StatsCounter totalCounter;
    private final StatsCounter successfulCounter;
    /**
     * 计数器，用于控制处理规模或位置。
     */
    private final StatsCounter failedCounter;

    /**
     * 功能：创建 `DefaultMessagesStats` 实例，并初始化必要字段。
     * 参数：
     * - `totalCounter`：`totalCounter` 参数。
     * - `successfulCounter`：`successfulCounter` 参数。
     * - `failedCounter`：`failedCounter` 参数。
     * 返回：新创建的对象实例。
     */
    public DefaultMessagesStats(StatsCounter totalCounter, StatsCounter successfulCounter, StatsCounter failedCounter) {
        this.totalCounter = totalCounter;
        this.successfulCounter = successfulCounter;
        this.failedCounter = failedCounter;
    }

    /**
     * 功能：执行 `incrementTotal` 对应的处理。
     * 参数：
     * - `amount`：`amount` 参数。
     * 返回：无。
     */
    @Override
    public void incrementTotal(int amount) {
        totalCounter.add(amount);
    }

    /**
     * 功能：执行 `incrementSuccessful` 对应的处理。
     * 参数：
     * - `amount`：`amount` 参数。
     * 返回：无。
     */
    @Override
    public void incrementSuccessful(int amount) {
        successfulCounter.add(amount);
    }

    /**
     * 功能：执行 `incrementFailed` 对应的处理。
     * 参数：
     * - `amount`：`amount` 参数。
     * 返回：无。
     */
    @Override
    public void incrementFailed(int amount) {
        failedCounter.add(amount);
    }

    /**
     * 功能：获取`Total`。
     * 参数：无。
     * 返回：数值结果。
     */
    @Override
    public int getTotal() {
        return totalCounter.get();
    }

    /**
     * 功能：获取`Successful`。
     * 参数：无。
     * 返回：数值结果。
     */
    @Override
    public int getSuccessful() {
        return successfulCounter.get();
    }

    /**
     * 功能：获取`Failed`。
     * 参数：无。
     * 返回：数值结果。
     */
    @Override
    public int getFailed() {
        return failedCounter.get();
    }

    /**
     * 功能：执行 `reset` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void reset() {
        totalCounter.clear();
        successfulCounter.clear();
        failedCounter.clear();
    }
}
