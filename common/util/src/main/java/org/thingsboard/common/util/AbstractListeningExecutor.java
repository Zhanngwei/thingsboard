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

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.Callable;

/**
 * Created by igor on 4/13/18.
 */
/**
 * 中文说明：
 * 1. 类目的：`AbstractListeningExecutor` 是ThingsBoard Common 模块中的公共工具类型，用于提供跨模块复用的纯函数、解析、转换或辅助逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括Application、DAO、Transport、Rule Engine、测试工具和第三方库。
 * 4. 生命周期：通常作为静态工具或轻量对象按需调用，不持有长生命周期业务状态。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Utility / Helper。
 */
public abstract class AbstractListeningExecutor implements ListeningExecutor {

    /**
     * 服务列表，用于保存一组待处理对象。
     */
    private ListeningExecutorService service;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    public void init() {
        this.service = MoreExecutors.listeningDecorator(ThingsBoardExecutors.newWorkStealingPool(getThreadPollSize(), getClass()));
    }

    /**
     * 功能：执行 `destroy` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PreDestroy
    public void destroy() {
        if (this.service != null) {
            this.service.shutdown();
        }
    }

    /**
     * 功能：执行`Async`。
     * 参数：
     * - `task`：`task` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public <T> ListenableFuture<T> executeAsync(Callable<T> task) {
        return service.submit(task);
    }

    /**
     * 功能：执行`Async`。
     * 参数：
     * - `task`：`task` 参数。
     * 返回：匹配的数据集合。
     */
    public ListenableFuture<?> executeAsync(Runnable task) {
        return service.submit(task);
    }

    /**
     * 功能：执行 `execute` 对应的处理。
     * 参数：
     * - `command`：`command` 参数。
     * 返回：无。
     */
    @Override
    public void execute(Runnable command) {
        service.execute(command);
    }

    /**
     * 功能：执行 `executor` 对应的处理。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    public ListeningExecutorService executor() {
        return service;
    }

    /**
     * 功能：获取`Thread Poll Size`。
     * 参数：无。
     * 返回：数值结果。
     */
    protected abstract int getThreadPollSize();

}

/*
 * 本类总结：
 * 1. 核心职责：`AbstractListeningExecutor` 在 ThingsBoard Common 模块 中承担公共工具类型职责，核心目的是提供跨模块复用的纯函数、解析、转换或辅助逻辑。
 * 2. 核心流程：接收输入参数后执行本地转换、校验或解析并返回结果。
 * 3. 关键依赖：主要依赖或协作对象包括Application、DAO、Transport、Rule Engine、测试工具和第三方库。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
