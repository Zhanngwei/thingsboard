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
package org.thingsboard.server.queue.common;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Created by ashvayka on 05.10.18.
 */
/**
 * 中文说明：
 * 1. 类目的：`AsyncCallbackTemplate` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
public class AsyncCallbackTemplate {

    /**
     * 功能：执行 `withCallbackAndTimeout` 对应的处理。
     * 参数：
     * - `future`：数据列表。
     * - `onSuccess`：`onSuccess` 参数。
     * - `onFailure`：`onFailure` 参数。
     * - `timeoutInMs`：`timeoutInMs` 参数。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    public static <T> void withCallbackAndTimeout(ListenableFuture<T> future,
                                                  Consumer<T> onSuccess,
                                                  Consumer<Throwable> onFailure,
                                                  long timeoutInMs,
                                                  ScheduledExecutorService timeoutExecutor,
                                                  Executor callbackExecutor) {
        future = Futures.withTimeout(future, timeoutInMs, TimeUnit.MILLISECONDS, timeoutExecutor);
        withCallback(future, onSuccess, onFailure, callbackExecutor);
    }

    /**
     * 功能：执行 `withCallback` 对应的处理。
     * 参数：
     * - `future`：数据列表。
     * - `onSuccess`：`onSuccess` 参数。
     * - `onFailure`：`onFailure` 参数。
     * - `executor`：`executor` 参数。
     * 返回：无。
     */
    public static <T> void withCallback(ListenableFuture<T> future, Consumer<T> onSuccess,
                                        Consumer<Throwable> onFailure, Executor executor) {
        FutureCallback<T> callback = new FutureCallback<T>() {
            @Override
            public void onSuccess(T result) {
                try {
                    onSuccess.accept(result);
                } catch (Throwable th) {
                    onFailure(th);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                onFailure.accept(t);
            }
        };
        if (executor != null) {
            Futures.addCallback(future, callback, executor);
        } else {
            Futures.addCallback(future, callback, MoreExecutors.directExecutor());
        }
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`AsyncCallbackTemplate` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
