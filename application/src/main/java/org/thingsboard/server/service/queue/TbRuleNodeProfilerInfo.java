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

import lombok.Getter;
import org.thingsboard.server.common.msg.queue.RuleNodeInfo;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 中文说明：
 * 1. `TbRuleNodeProfilerInfo` 是 ThingsBoard Application 中承载规则节点信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
public class TbRuleNodeProfilerInfo {
    /**
     * 规则节点ID，用于定位对应业务对象。
     */
    @Getter
    private final UUID ruleNodeId;
    /**
     * 显示标签，用于展示或标识当前对象。
     */
    @Getter
    private final String label;
    private AtomicInteger executionCount = new AtomicInteger(0);
    private AtomicLong executionTime = new AtomicLong(0);
    private AtomicLong maxExecutionTime = new AtomicLong(0);

    /**
     * 功能：创建 `TbRuleNodeProfilerInfo` 实例，并初始化必要字段。
     * 参数：
     * - `ruleNodeInfo`：`ruleNodeInfo` 参数。
     * 返回：新创建的对象实例。
     */
    public TbRuleNodeProfilerInfo(RuleNodeInfo ruleNodeInfo) {
        this.ruleNodeId = ruleNodeInfo.getRuleNodeId().getId();
        this.label = ruleNodeInfo.toString();
    }

    /**
     * 功能：创建 `TbRuleNodeProfilerInfo` 实例，并初始化必要字段。
     * 参数：
     * - `ruleNodeId`：规则节点ID。
     * 返回：新创建的对象实例。
     */
    public TbRuleNodeProfilerInfo(UUID ruleNodeId) {
        this.ruleNodeId = ruleNodeId;
        this.label = "";
    }

    /**
     * 功能：执行 `record` 对应的处理。
     * 参数：
     * - `processingTime`：`processingTime` 参数。
     * 返回：无。
     */
    public void record(long processingTime) {
        executionCount.incrementAndGet();
        executionTime.addAndGet(processingTime);
        while (true) {
            long value = maxExecutionTime.get();
            if (value >= processingTime) {
                break;
            }
            if (maxExecutionTime.compareAndSet(value, processingTime)) {
                break;
            }
        }
    }

    /**
     * 功能：获取数量。
     * 参数：无。
     * 返回：数值结果。
     */
    int getExecutionCount() {
        return executionCount.get();
    }

    /**
     * 功能：获取时间。
     * 参数：无。
     * 返回：数值结果。
     */
    long getMaxExecutionTime() {
        return maxExecutionTime.get();
    }

    /**
     * 功能：获取时间。
     * 参数：无。
     * 返回：数值结果。
     */
    double getAvgExecutionTime() {
        double executionCnt = (double) executionCount.get();
        if (executionCnt > 0) {
            return executionTime.get() / executionCnt;
        } else {
            return 0.0;
        }
    }
}