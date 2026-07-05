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
 * 1. 类目的：`DefaultJsInvokeStats` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
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

/*
 * 本类总结：
 * 1. 核心职责：`DefaultJsInvokeStats` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
