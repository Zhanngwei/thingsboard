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
package org.thingsboard.server.dao.nosql;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by ashvayka on 24.10.18.
 */
/**
 * 中文说明：
 * 1. 类目的：`TbResultSetFuture` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
public class TbResultSetFuture implements ListenableFuture<TbResultSet> {

    /**
     * 异步结果集合，用于去重保存或快速判断对象是否存在。
     */
    private final SettableFuture<TbResultSet> mainFuture;

    /**
     * 功能：创建 `TbResultSetFuture` 实例，并初始化必要字段。
     * 参数：
     * - `mainFuture`：`mainFuture` 参数。
     * 返回：新创建的对象实例。
     */
    public TbResultSetFuture(SettableFuture<TbResultSet> mainFuture) {
        this.mainFuture = mainFuture;
    }

    /**
     * 功能：获取`Uninterruptibly`。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    public TbResultSet getUninterruptibly() {
        return getSafe();
    }

    /**
     * 功能：获取`Uninterruptibly`。
     * 参数：
     * - `timeout`：`timeout` 参数。
     * - `unit`：`unit` 参数。
     * 返回：匹配的数据集合。
     */
    public TbResultSet getUninterruptibly(long timeout, TimeUnit unit) throws TimeoutException {
        return getSafe(timeout, unit);
    }

    /**
     * 功能：执行 `cancel` 对应的处理。
     * 参数：
     * - `mayInterruptIfRunning`：`mayInterruptIfRunning` 参数。
     * 返回：判断结果。
     */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return mainFuture.cancel(mayInterruptIfRunning);
    }

    /**
     * 功能：判断`Cancelled`。
     * 参数：无。
     * 返回：判断结果。
     */
    @Override
    public boolean isCancelled() {
        return mainFuture.isCancelled();
    }

    /**
     * 功能：判断`Done`。
     * 参数：无。
     * 返回：判断结果。
     */
    @Override
    public boolean isDone() {
        return mainFuture.isDone();
    }

    /**
     * 功能：执行 `get` 对应的处理。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    @Override
    public TbResultSet get() throws InterruptedException, ExecutionException {
        return mainFuture.get();
    }

    /**
     * 功能：执行 `get` 对应的处理。
     * 参数：
     * - `timeout`：`timeout` 参数。
     * - `unit`：`unit` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public TbResultSet get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return mainFuture.get(timeout, unit);
    }

    /**
     * 功能：保存或创建监听器。
     * 参数：
     * - `listener`：`listener` 参数。
     * - `executor`：`executor` 参数。
     * 返回：无。
     */
    @Override
    public void addListener(Runnable listener, Executor executor) {
        mainFuture.addListener(listener, executor);
    }

    /**
     * 功能：获取`Safe`。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    private TbResultSet getSafe() {
        try {
            return mainFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * 功能：获取`Safe`。
     * 参数：
     * - `timeout`：`timeout` 参数。
     * - `unit`：`unit` 参数。
     * 返回：匹配的数据集合。
     */
    private TbResultSet getSafe(long timeout, TimeUnit unit) throws TimeoutException {
        try {
            return mainFuture.get(timeout, unit);
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`TbResultSetFuture` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
