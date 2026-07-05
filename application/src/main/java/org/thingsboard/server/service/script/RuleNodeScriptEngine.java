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
package org.thingsboard.server.service.script;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.ScriptEngine;
import org.thingsboard.script.api.ScriptInvokeService;
import org.thingsboard.script.api.ScriptType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.TbMsg;

import javax.script.ScriptException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;


/**
 * 中文说明：
 * 1. 类目的：`RuleNodeScriptEngine` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
@Slf4j
public abstract class RuleNodeScriptEngine<T extends ScriptInvokeService, R> implements ScriptEngine {

    /**
     * 服务，提供当前类调用的业务操作。
     */
    private final T scriptInvokeService;

    /**
     * `scriptId`ID，用于定位对应业务对象。
     */
    private final UUID scriptId;
    private final TenantId tenantId;

    /**
     * 功能：创建 `RuleNodeScriptEngine` 实例，并初始化必要字段。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `scriptInvokeService`：服务对象。
     * - `script`：`script` 参数。
     * - `argNames`：名称。
     * 返回：新创建的对象实例。
     */
    public RuleNodeScriptEngine(TenantId tenantId, T scriptInvokeService, String script, String... argNames) {
        this.tenantId = tenantId;
        this.scriptInvokeService = scriptInvokeService;
        try {
            this.scriptId = this.scriptInvokeService.eval(tenantId, ScriptType.RULE_NODE_SCRIPT, script, argNames).get();
        } catch (Exception e) {
            Throwable t = e;
            if (e instanceof ExecutionException) {
                t = e.getCause();
            }
            throw new IllegalArgumentException("Can't compile script: " + t.getMessage(), t);
        }
    }

    /**
     * 功能：执行 `prepareArgs` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：处理结果。
     */
    protected abstract Object[] prepareArgs(TbMsg msg);

    /**
     * 功能：执行`Update Async`。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<List<TbMsg>> executeUpdateAsync(TbMsg msg) {
        ListenableFuture<R> result = executeScriptAsync(msg);
        return Futures.transformAsync(result,
                json -> executeUpdateTransform(msg, json),
                MoreExecutors.directExecutor());
    }

    /**
     * 功能：执行`Update Transform`。
     * 参数：
     * - `msg`：待处理消息。
     * - `result`：`result` 参数。
     * 返回：匹配的数据集合。
     */
    protected abstract ListenableFuture<List<TbMsg>> executeUpdateTransform(TbMsg msg, R result);

    /**
     * 功能：执行`Generate Async`。
     * 参数：
     * - `prevMsg`：待处理消息。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<TbMsg> executeGenerateAsync(TbMsg prevMsg) {
        return Futures.transformAsync(executeScriptAsync(prevMsg),
                result -> executeGenerateTransform(prevMsg, result),
                MoreExecutors.directExecutor());
    }

    /**
     * 功能：执行`Generate Transform`。
     * 参数：
     * - `prevMsg`：待处理消息。
     * - `result`：`result` 参数。
     * 返回：匹配的数据集合。
     */
    protected abstract ListenableFuture<TbMsg> executeGenerateTransform(TbMsg prevMsg, R result);

    /**
     * 功能：执行`To String Async`。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<String> executeToStringAsync(TbMsg msg) {
        return Futures.transformAsync(executeScriptAsync(msg), this::executeToStringTransform, MoreExecutors.directExecutor());
    }


    /**
     * 功能：执行`Filter Async`。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<Boolean> executeFilterAsync(TbMsg msg) {
        return Futures.transformAsync(executeScriptAsync(msg),
                this::executeFilterTransform,
                MoreExecutors.directExecutor());
    }

    /**
     * 功能：执行`To String Transform`。
     * 参数：
     * - `result`：`result` 参数。
     * 返回：匹配的数据集合。
     */
    protected abstract ListenableFuture<String> executeToStringTransform(R result);

    /**
     * 功能：执行`Filter Transform`。
     * 参数：
     * - `result`：`result` 参数。
     * 返回：匹配的数据集合。
     */
    protected abstract ListenableFuture<Boolean> executeFilterTransform(R result);

    /**
     * 功能：执行`Switch Transform`。
     * 参数：
     * - `result`：`result` 参数。
     * 返回：匹配的数据集合。
     */
    protected abstract ListenableFuture<Set<String>> executeSwitchTransform(R result);

    /**
     * 功能：执行`Switch Async`。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<Set<String>> executeSwitchAsync(TbMsg msg) {
        return Futures.transformAsync(executeScriptAsync(msg),
                this::executeSwitchTransform,
                MoreExecutors.directExecutor()); //usually runs in a callbackExecutor
    }

    /**
     * 功能：执行`Script Async`。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<R> executeScriptAsync(TbMsg msg) {
        log.trace("execute script async, msg {}", msg);
        Object[] inArgs = prepareArgs(msg);
        return executeScriptAsync(msg.getCustomerId(), inArgs[0], inArgs[1], inArgs[2]);
    }

    /**
     * 功能：执行`Script Async`。
     * 参数：
     * - `customerId`：客户IDID。
     * - `args`：传入程序的参数。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<R> executeScriptAsync(CustomerId customerId, Object... args) {
        return Futures.transformAsync(scriptInvokeService.invokeScript(tenantId, customerId, this.scriptId, args),
                o -> {
                    try {
                        return Futures.immediateFuture(convertResult(o));
                    } catch (Exception e) {
                        if (e.getCause() instanceof ScriptException) {
                            return Futures.immediateFailedFuture(e.getCause());
                        } else if (e.getCause() instanceof RuntimeException) {
                            return Futures.immediateFailedFuture(new ScriptException(e.getCause().getMessage()));
                        } else {
                            return Futures.immediateFailedFuture(new ScriptException(e));
                        }
                    }
                }, MoreExecutors.directExecutor());
    }

    /**
     * 功能：执行 `destroy` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void destroy() {
        scriptInvokeService.release(this.scriptId);
    }

    /**
     * 功能：转换`Result`。
     * 参数：
     * - `result`：`result` 参数。
     * 返回：处理结果。
     */
    protected abstract R convertResult(Object result);
}

/*
 * 本类总结：
 * 1. 核心职责：`RuleNodeScriptEngine` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
