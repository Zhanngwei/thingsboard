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
package org.thingsboard.common.util;

import lombok.NonNull;
import lombok.ToString;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 中文说明：
 * 1. 类目的：`ThingsBoardForkJoinWorkerThreadFactory` 是ThingsBoard Common 模块中的公共工具类型，用于提供跨模块复用的纯函数、解析、转换或辅助逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括Application、DAO、Transport、Rule Engine、测试工具和第三方库。
 * 4. 生命周期：通常作为静态工具或轻量对象按需调用，不持有长生命周期业务状态。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Utility / Helper。
 */
@ToString
public class ThingsBoardForkJoinWorkerThreadFactory implements ForkJoinPool.ForkJoinWorkerThreadFactory {
    /**
     * 名称，用于展示或标识当前对象。
     */
    private final String namePrefix;
    private final AtomicLong threadNumber = new AtomicLong(1);

    /**
     * 功能：创建 `ThingsBoardForkJoinWorkerThreadFactory` 实例，并初始化必要字段。
     * 参数：
     * - `namePrefix`：名称。
     * 返回：新创建的对象实例。
     */
    public ThingsBoardForkJoinWorkerThreadFactory(@NonNull String namePrefix) {
        this.namePrefix = namePrefix;
    }

    /**
     * 功能：执行 `newThread` 对应的处理。
     * 参数：
     * - `pool`：`pool` 参数。
     * 返回：处理结果。
     */
    @Override
    public final ForkJoinWorkerThread newThread(ForkJoinPool pool) {
        ForkJoinWorkerThread thread = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
        thread.setContextClassLoader(this.getClass().getClassLoader());
        thread.setName(namePrefix +"-"+thread.getPoolIndex()+"-"+threadNumber.getAndIncrement());
        return thread;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`ThingsBoardForkJoinWorkerThreadFactory` 在 ThingsBoard Common 模块 中承担公共工具类型职责，核心目的是提供跨模块复用的纯函数、解析、转换或辅助逻辑。
 * 2. 核心流程：接收输入参数后执行本地转换、校验或解析并返回结果。
 * 3. 关键依赖：主要依赖或协作对象包括Application、DAO、Transport、Rule Engine、测试工具和第三方库。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
