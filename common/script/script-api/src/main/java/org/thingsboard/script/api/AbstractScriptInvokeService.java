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
package org.thingsboard.script.api;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.String.format;

/**
 * 中文说明：
 * 1. `AbstractScriptInvokeService` 是 ThingsBoard Common 中负责脚本执行的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `ScriptInvokeService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Slf4j
public abstract class AbstractScriptInvokeService implements ScriptInvokeService {

    protected final Map<UUID, BlockedScriptInfo> disabledScripts = new ConcurrentHashMap<>();
    private final AtomicInteger pushedMsgs = new AtomicInteger(0);
    private final AtomicInteger invokeMsgs = new AtomicInteger(0);
    private final AtomicInteger evalMsgs = new AtomicInteger(0);
    protected final AtomicInteger failedMsgs = new AtomicInteger(0);
    protected final AtomicInteger timeoutMsgs = new AtomicInteger(0);

    private final FutureCallback<UUID> evalCallback = new ScriptStatCallback<>(evalMsgs, timeoutMsgs, failedMsgs);
    private final FutureCallback<Object> invokeCallback = new ScriptStatCallback<>(invokeMsgs, timeoutMsgs, failedMsgs);

    /**
     * 服务，提供当前类调用的业务操作。
     */
    protected ScheduledExecutorService timeoutExecutorService;

    /**
     * 功能：获取超时时间。
     * 参数：无。
     * 返回：数值结果。
     */
    protected long getMaxEvalRequestsTimeout() {
        return getMaxInvokeRequestsTimeout();
    }

    /**
     * 功能：获取超时时间。
     * 参数：无。
     * 返回：数值结果。
     */
    protected abstract long getMaxInvokeRequestsTimeout();

    /**
     * 功能：获取`Max Script Body Size`。
     * 参数：无。
     * 返回：数值结果。
     */
    protected abstract long getMaxScriptBodySize();

    /**
     * 功能：获取参数。
     * 参数：无。
     * 返回：数值结果。
     */
    protected abstract long getMaxTotalArgsSize();

    /**
     * 功能：获取`Max Result Size`。
     * 参数：无。
     * 返回：数值结果。
     */
    protected abstract long getMaxResultSize();

    /**
     * 功能：获取持续时间。
     * 参数：无。
     * 返回：数值结果。
     */
    protected abstract int getMaxBlackListDurationSec();

    /**
     * 功能：获取`Max Errors`。
     * 参数：无。
     * 返回：数值结果。
     */
    protected abstract int getMaxErrors();

    /**
     * 功能：判断`Stats Enabled`。
     * 参数：无。
     * 返回：判断结果。
     */
    protected abstract boolean isStatsEnabled();

    /**
     * 功能：获取名称。
     * 参数：无。
     * 返回：文本结果。
     */
    protected abstract String getStatsName();

    /**
     * 功能：获取回调。
     * 参数：无。
     * 返回：处理结果。
     */
    protected abstract Executor getCallbackExecutor();

    /**
     * 功能：判断`Script Present`。
     * 参数：
     * - `scriptId`：`scriptId`ID。
     * 返回：判断结果。
     */
    protected abstract boolean isScriptPresent(UUID scriptId);

    /**
     * 功能：判断`Exec Enabled`。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：判断结果。
     */
    protected abstract boolean isExecEnabled(TenantId tenantId);
    /**
     * 功能：上报`Execution`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * 返回：无。
     */
    protected abstract void reportExecution(TenantId tenantId, CustomerId customerId);

    /**
     * 功能：执行 `doEvalScript` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `scriptType`：类型。
     * - `scriptBody`：`scriptBody` 参数。
     * - `scriptId`：`scriptId`ID。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    protected abstract ListenableFuture<UUID> doEvalScript(TenantId tenantId, ScriptType scriptType, String scriptBody, UUID scriptId, String[] argNames);

    /**
     * 功能：执行 `doInvokeFunction` 对应的处理。
     * 参数：
     * - `scriptId`：`scriptId`ID。
     * - `args`：传入程序的参数。
     * 返回：处理结果。
     */
    protected abstract TbScriptExecutionTask doInvokeFunction(UUID scriptId, Object[] args);

    /**
     * 功能：执行 `doRelease` 对应的处理。
     * 参数：
     * - `scriptId`：`scriptId`ID。
     * 返回：无。
     */
    protected abstract void doRelease(UUID scriptId) throws Exception;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void init() {
        if (getMaxEvalRequestsTimeout() > 0 || getMaxInvokeRequestsTimeout() > 0) {
            timeoutExecutorService = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("script-timeout"));
        }
    }

    /**
     * 功能：执行 `stop` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void stop() {
        if (timeoutExecutorService != null) {
            timeoutExecutorService.shutdownNow();
        }
    }

    /**
     * 功能：执行 `printStats` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void printStats() {
        if (isStatsEnabled()) {
            int pushed = pushedMsgs.getAndSet(0);
            int invoked = invokeMsgs.getAndSet(0);
            int evaluated = evalMsgs.getAndSet(0);
            int failed = failedMsgs.getAndSet(0);
            int timedOut = timeoutMsgs.getAndSet(0);
            if (pushed > 0 || invoked > 0 || evaluated > 0 || failed > 0 || timedOut > 0) {
                log.info("{}: pushed [{}] received [{}] invoke [{}] eval [{}] failed [{}] timedOut [{}]",
                        getStatsName(), pushed, invoked + evaluated, invoked, evaluated, failed, timedOut);
            }
        }
    }

    /**
     * 功能：执行 `eval` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `scriptType`：类型。
     * - `scriptBody`：`scriptBody` 参数。
     * - `argNames`：名称。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<UUID> eval(TenantId tenantId, ScriptType scriptType, String scriptBody, String... argNames) {
        if (isExecEnabled(tenantId)) {
            if (scriptBodySizeExceeded(scriptBody)) {
                return error(format("Script body exceeds maximum allowed size of %s symbols", getMaxScriptBodySize()));
            }
            UUID scriptId = UUID.randomUUID();
            pushedMsgs.incrementAndGet();
            return withTimeoutAndStatsCallback(scriptId, null,
                    doEvalScript(tenantId, scriptType, scriptBody, scriptId, argNames), evalCallback, getMaxEvalRequestsTimeout());
        } else {
            return error("Script Execution is disabled due to API limits!");
        }
    }

    /**
     * 功能：执行 `invokeScript` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `scriptId`：`scriptId`ID。
     * - `args`：传入程序的参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<Object> invokeScript(TenantId tenantId, CustomerId customerId, UUID scriptId, Object... args) {
        if (isExecEnabled(tenantId)) {
            if (!isScriptPresent(scriptId)) {
                return error("No compiled script found for scriptId: [" + scriptId + "]!");
            }
            if (!isDisabled(scriptId)) {
                if (argsSizeExceeded(args)) {
                    TbScriptException t = new TbScriptException(scriptId, TbScriptException.ErrorCode.OTHER, null, new IllegalArgumentException(
                            format("Script input arguments exceed maximum allowed total args size of %s symbols", getMaxTotalArgsSize())
                    ));
                    return Futures.immediateFailedFuture(handleScriptException(scriptId, null, t));
                }
                reportExecution(tenantId, customerId);
                pushedMsgs.incrementAndGet();
                log.trace("[{}] InvokeScript uuid {} with timeout {}ms", tenantId, scriptId, getMaxInvokeRequestsTimeout());
                var task = doInvokeFunction(scriptId, args);

                var resultFuture = Futures.transformAsync(task.getResultFuture(), output -> {
                    String result = JacksonUtil.toString(output);
                    if (resultSizeExceeded(result)) {
                        throw new TbScriptException(scriptId, TbScriptException.ErrorCode.OTHER, null, new RuntimeException(
                                format("Script invocation result exceeds maximum allowed size of %s symbols", getMaxResultSize())
                        ));
                    }
                    return Futures.immediateFuture(output);
                }, MoreExecutors.directExecutor());

                return withTimeoutAndStatsCallback(scriptId, task, resultFuture, invokeCallback, getMaxInvokeRequestsTimeout());
            } else {
                String message = "Script invocation is blocked due to maximum error count "
                        + getMaxErrors() + ", scriptId " + scriptId + "!";
                log.warn("[{}] " + message, tenantId);
                return error(message);
            }
        } else {
            return error("Script execution is disabled due to API limits!");
        }
    }

    /**
     * 功能：执行 `withTimeoutAndStatsCallback` 对应的处理。
     * 参数：
     * - `scriptId`：`scriptId`ID。
     * - `task`：`task` 参数。
     * - `future`：数据列表。
     * - `statsCallback`：处理完成后的回调。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    private <T extends V, V> ListenableFuture<T> withTimeoutAndStatsCallback(UUID scriptId, TbScriptExecutionTask task, ListenableFuture<T> future, FutureCallback<V> statsCallback, long timeout) {
        if (timeout > 0) {
            future = Futures.withTimeout(future, timeout, TimeUnit.MILLISECONDS, timeoutExecutorService);
        }
        Futures.addCallback(future, statsCallback, getCallbackExecutor());
        return Futures.catchingAsync(future, Exception.class,
                input -> Futures.immediateFailedFuture(handleScriptException(scriptId, task, input)),
                MoreExecutors.directExecutor());
    }

    /**
     * 功能：处理`Script Exception`。
     * 参数：
     * - `scriptId`：`scriptId`ID。
     * - `task`：`task` 参数。
     * - `t`：`t` 参数。
     * 返回：处理结果。
     */
    private Throwable handleScriptException(UUID scriptId, TbScriptExecutionTask task, Throwable t) {
        boolean timeout = t instanceof TimeoutException || (t.getCause() != null && t.getCause() instanceof TimeoutException);
        if (timeout && task != null) {
            task.stop();
        }
        boolean blockList = timeout;
        String scriptBody = null;
        if (t instanceof TbScriptException) {
            var scriptException = (TbScriptException) t;
            scriptBody = scriptException.getBody();
            var cause = scriptException.getCause();
            switch (scriptException.getErrorCode()) {
                case COMPILATION:
                    log.debug("[{}] Failed to compile script: {}", scriptId, scriptException.getBody(), cause);
                    break;
                case TIMEOUT:
                    log.debug("[{}] Timeout to execute script: {}", scriptId, scriptException.getBody(), cause);
                    break;
                case OTHER:
                case RUNTIME:
                    log.debug("[{}] Failed to execute script: {}", scriptId, scriptException.getBody(), cause);
                    break;
            }
            blockList = timeout || scriptException.getErrorCode() != TbScriptException.ErrorCode.RUNTIME;
        }
        if (blockList) {
            BlockedScriptInfo disableListInfo = disabledScripts.computeIfAbsent(scriptId, key -> new BlockedScriptInfo(getMaxBlackListDurationSec()));
            int counter = disableListInfo.incrementAndGet();
            if (log.isDebugEnabled()) {
                log.debug("Script has exception counter {} on disabledFunctions for id {}, exception {}, cause {}, scriptBody {}",
                        counter, scriptId, t, t.getCause(), scriptBody);
            } else {
                log.warn("Script has exception counter {} on disabledFunctions for id {}, exception {}",
                        counter, scriptId, t.getMessage());
            }
        }
        if (timeout) {
            return new TimeoutException("Script timeout!");
        } else {
            return t;
        }
    }

    /**
     * 功能：执行 `release` 对应的处理。
     * 参数：
     * - `scriptId`：`scriptId`ID。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<Void> release(UUID scriptId) {
        if (isScriptPresent(scriptId)) {
            try {
                disabledScripts.remove(scriptId);
                doRelease(scriptId);
            } catch (Exception e) {
                return Futures.immediateFailedFuture(e);
            }
        }
        return Futures.immediateFuture(null);
    }

    /**
     * 功能：判断`Disabled`。
     * 参数：
     * - `scriptId`：`scriptId`ID。
     * 返回：判断结果。
     */
    private boolean isDisabled(UUID scriptId) {
        BlockedScriptInfo errorCount = disabledScripts.get(scriptId);
        if (errorCount != null) {
            if (errorCount.getExpirationTime() <= System.currentTimeMillis()) {
                disabledScripts.remove(scriptId);
                return false;
            } else {
                return errorCount.get() >= getMaxErrors();
            }
        } else {
            return false;
        }
    }

    /**
     * 功能：执行 `scriptBodySizeExceeded` 对应的处理。
     * 参数：
     * - `scriptBody`：`scriptBody` 参数。
     * 返回：判断结果。
     */
    private boolean scriptBodySizeExceeded(String scriptBody) {
        if (getMaxScriptBodySize() <= 0) return false;
        return scriptBody.length() > getMaxScriptBodySize();
    }

    /**
     * 功能：执行 `argsSizeExceeded` 对应的处理。
     * 参数：
     * - `args`：传入程序的参数。
     * 返回：判断结果。
     */
    private boolean argsSizeExceeded(Object[] args) {
        if (getMaxTotalArgsSize() <= 0) return false;
        long totalArgsSize = 0;
        for (Object arg : args) {
            if (arg instanceof CharSequence) {
                totalArgsSize += ((CharSequence) arg).length();
            } else {
                var str = JacksonUtil.toString(arg);
                if (str != null) {
                    totalArgsSize += str.length();
                }
            }
        }
        return totalArgsSize > getMaxTotalArgsSize();
    }

    /**
     * 功能：执行 `resultSizeExceeded` 对应的处理。
     * 参数：
     * - `result`：`result` 参数。
     * 返回：判断结果。
     */
    private boolean resultSizeExceeded(String result) {
        if (getMaxResultSize() <= 0) return false;
        return result != null && result.length() > getMaxResultSize();
    }

    /**
     * 功能：执行 `error` 对应的处理。
     * 参数：
     * - `message`：待处理消息。
     * 返回：匹配的数据集合。
     */
    private <T> ListenableFuture<T> error(String message) {
        return Futures.immediateFailedFuture(new RuntimeException(message));
    }
}
