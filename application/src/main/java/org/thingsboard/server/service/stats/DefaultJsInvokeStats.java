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
package org.thingsboard.server.service.stats;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.actors.JsInvokeStats;
import org.thingsboard.server.common.stats.StatsCounter;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.common.stats.StatsType;

import javax.annotation.PostConstruct;

/**
 * 中文说明：
 * 1. `DefaultJsInvokeStats` 是 ThingsBoard Application 中围绕统计数据提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 直接依赖的类型边界包括 `JsInvokeStats`。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
@Service
public class DefaultJsInvokeStats implements JsInvokeStats {
    /**
     * `REQUESTS`常量，用于统一引用固定值。
     */
    private static final String REQUESTS = "requests";
    private static final String RESPONSES = "responses";
    /**
     * 失败信息常量，用于统一引用固定值。
     */
    private static final String FAILURES = "failures";

    /**
     * 当前请求对象，封装本次处理需要的输入信息。
     */
    private StatsCounter requestsCounter;
    private StatsCounter responsesCounter;
    /**
     * 失败信息，表示当前对象的对应属性。
     */
    private StatsCounter failuresCounter;

    /**
     * 工厂，用于按场景创建或提供目标对象。
     */
    @Autowired
    private StatsFactory statsFactory;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    public void init() {
        String key = StatsType.JS_INVOKE.getName();
        this.requestsCounter = statsFactory.createStatsCounter(key, REQUESTS);
        this.responsesCounter = statsFactory.createStatsCounter(key, RESPONSES);
        this.failuresCounter = statsFactory.createStatsCounter(key, FAILURES);
    }

    /**
     * 功能：执行 `incrementRequests` 对应的处理。
     * 参数：
     * - `amount`：`amount` 参数。
     * 返回：无。
     */
    @Override
    public void incrementRequests(int amount) {
        requestsCounter.add(amount);
    }

    /**
     * 功能：执行 `incrementResponses` 对应的处理。
     * 参数：
     * - `amount`：`amount` 参数。
     * 返回：无。
     */
    @Override
    public void incrementResponses(int amount) {
        responsesCounter.add(amount);
    }

    /**
     * 功能：执行 `incrementFailures` 对应的处理。
     * 参数：
     * - `amount`：`amount` 参数。
     * 返回：无。
     */
    @Override
    public void incrementFailures(int amount) {
        failuresCounter.add(amount);
    }

    /**
     * 功能：获取`Requests`。
     * 参数：无。
     * 返回：数值结果。
     */
    @Override
    public int getRequests() {
        return requestsCounter.get();
    }

    /**
     * 功能：获取`Responses`。
     * 参数：无。
     * 返回：数值结果。
     */
    @Override
    public int getResponses() {
        return responsesCounter.get();
    }

    /**
     * 功能：获取失败信息。
     * 参数：无。
     * 返回：数值结果。
     */
    @Override
    public int getFailures() {
        return failuresCounter.get();
    }

    /**
     * 功能：执行 `reset` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void reset() {
        requestsCounter.clear();
        responsesCounter.clear();
        failuresCounter.clear();
    }
}
