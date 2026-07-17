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
package org.thingsboard.server.transport.lwm2m.server.common;

import org.thingsboard.common.util.ThingsBoardExecutors;

import java.util.concurrent.ExecutorService;

/**
 * 中文说明：
 * 1. `LwM2MExecutorAwareService` 是 ThingsBoard Common Transport 中负责 LwM2M 的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 它直接协作于领域模型、存取接口和相关业务组件。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
public abstract class LwM2MExecutorAwareService {

    /**
     * 执行器，负责处理对应任务或消息。
     */
    protected ExecutorService executor;

    /**
     * 功能：获取执行器。
     * 参数：无。
     * 返回：数值结果。
     */
    protected abstract int getExecutorSize();

    /**
     * 功能：获取名称。
     * 参数：无。
     * 返回：文本结果。
     */
    protected abstract String getExecutorName();

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    protected void init() {
        this.executor = ThingsBoardExecutors.newWorkStealingPool(getExecutorSize(), getExecutorName());
    }

    /**
     * 功能：执行 `destroy` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void destroy() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

}
