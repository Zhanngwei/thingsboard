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
package org.thingsboard.server.dao.cassandra.guava;

import com.datastax.oss.driver.api.core.session.Request;
import com.datastax.oss.driver.api.core.type.reflect.GenericType;
import com.datastax.oss.driver.internal.core.context.InternalDriverContext;
import com.datastax.oss.driver.internal.core.session.DefaultSession;
import com.datastax.oss.driver.internal.core.session.RequestProcessor;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import java.util.concurrent.CompletionStage;

/**
 * Wraps a {@link RequestProcessor} that returns {@link CompletionStage}s and converts them to a
 * {@link ListenableFuture}s.
 *
 * @param <T> The type of request
 * @param <U> The type of responses enclosed in the future response.
 */
/**
 * 中文说明：
 * 1. 类目的：`GuavaRequestAsyncProcessor` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
public class GuavaRequestAsyncProcessor<T extends Request, U>
        implements RequestProcessor<T, ListenableFuture<U>> {

    /**
     * 处理器，负责处理对应任务或消息。
     */
    private final RequestProcessor<T, CompletionStage<U>> subProcessor;

    /**
     * 类型，用于区分不同处理分支。
     */
    private final GenericType resultType;

    /**
     * 当前请求对象，封装本次处理需要的输入信息。
     */
    private final Class<?> requestClass;

    GuavaRequestAsyncProcessor(
            RequestProcessor<T, CompletionStage<U>> subProcessor,
            Class<?> requestClass,
            GenericType resultType) {
        this.subProcessor = subProcessor;
        this.requestClass = requestClass;
        this.resultType = resultType;
    }

    /**
     * 功能：执行 `canProcess` 对应的处理。
     * 参数：
     * - `request`：请求对象。
     * - `resultType`：类型。
     * 返回：判断结果。
     */
    @Override
    public boolean canProcess(Request request, GenericType resultType) {
        return requestClass.isInstance(request) && resultType.equals(this.resultType);
    }

    /**
     * 功能：执行 `process` 对应的处理。
     * 参数：
     * - `request`：请求对象。
     * - `session`：会话对象。
     * - `context`：处理上下文。
     * - `sessionLogPrefix`：会话对象。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<U> process(
            T request, DefaultSession session, InternalDriverContext context, String sessionLogPrefix) {
        SettableFuture<U> future = SettableFuture.create();
        subProcessor
                .process(request, session, context, sessionLogPrefix)
                .whenComplete(
                        (r, ex) -> {
                            if (ex != null) {
                                future.setException(ex);
                            } else {
                                future.set(r);
                            }
                        });
        return future;
    }

    /**
     * 功能：执行 `newFailure` 对应的处理。
     * 参数：
     * - `error`：错误信息。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<U> newFailure(RuntimeException error) {
        return Futures.immediateFailedFuture(error);
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`GuavaRequestAsyncProcessor` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
