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
 * 1. `RuleNodeScriptEngine` 是 ThingsBoard Application 中围绕规则节点提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 直接依赖的类型边界包括 `ScriptInvokeService`、`ScriptEngine`。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
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
