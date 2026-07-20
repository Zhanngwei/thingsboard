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
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.script.api.TbScriptException;
import org.thingsboard.script.api.js.AbstractJsInvokeService;
import org.thingsboard.script.api.js.JsScriptInfo;
import org.thingsboard.server.common.stats.TbApiUsageReportClient;
import org.thingsboard.server.common.stats.TbApiUsageStateClient;
import org.thingsboard.server.gen.js.JsInvokeProtos;
import org.thingsboard.server.queue.TbQueueRequestTemplate;
import org.thingsboard.server.queue.common.TbProtoJsQueueMsg;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 中文说明：
 * 1. `RemoteJsInvokeService` 是 ThingsBoard Common 中负责 `Remote Js Invoke` 的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `AbstractJsInvokeService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Slf4j
@ConditionalOnExpression("'${js.evaluator:null}'=='remote' && ('${service.type:null}'=='monolith' || '${service.type:null}'=='tb-core' || '${service.type:null}'=='tb-rule-engine')")
@Service
public class RemoteJsInvokeService extends AbstractJsInvokeService {

    /**
     * 当前请求对象，封装本次处理需要的输入信息。
     */
    @Getter
    @Value("${queue.js.max_eval_requests_timeout}")
    private long maxEvalRequestsTimeout;

    /**
     * 当前请求对象，封装本次处理需要的输入信息。
     */
    @Getter
    @Value("${queue.js.max_requests_timeout}")
    private long maxInvokeRequestsTimeout;

    /**
     * 当前请求对象，封装本次处理需要的输入信息。
     */
    @Value("${queue.js.max_exec_requests_timeout:2000}")
    private long maxExecRequestsTimeout;

    /**
     * `maxErrors` 字段，保存当前对象的对应属性。
     */
    @Getter
    @Value("${js.remote.max_errors}")
    private int maxErrors;

    /**
     * 持续时间，用于控制时间范围或等待时长。
     */
    @Getter
    @Value("${js.remote.max_black_list_duration_sec:60}")
    private int maxBlackListDurationSec;

    /**
     * 是否启用`stats`。
     */
    @Getter
    @Value("${js.remote.stats.enabled:false}")
    private boolean statsEnabled;

    private final ExecutorService callbackExecutor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors(), ThingsBoardThreadFactory.forName("js-executor-remote-callback"));

    /**
     * 功能：创建 `RemoteJsInvokeService` 实例，并初始化必要字段。
     * 参数：
     * - `apiUsageStateClient`：客户端对象。
     * - `apiUsageClient`：客户端对象。
     * 返回：新创建的对象实例。
     */
    public RemoteJsInvokeService(Optional<TbApiUsageStateClient> apiUsageStateClient, Optional<TbApiUsageReportClient> apiUsageClient) {
        super(apiUsageStateClient, apiUsageClient);
    }

    /**
     * 功能：获取回调。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected Executor getCallbackExecutor() {
        return callbackExecutor;
    }

    /**
     * 功能：获取名称。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    protected String getStatsName() {
        return "Queue JS Invoke Stats";
    }

    /**
     * 功能：执行 `printStats` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Scheduled(fixedDelayString = "${js.remote.stats.print_interval_ms}")
    public void printStats() {
        super.printStats();
    }

    /**
     * 当前请求对象，封装本次处理需要的输入信息。
     */
    @Autowired
    protected TbQueueRequestTemplate<TbProtoJsQueueMsg<JsInvokeProtos.RemoteJsRequest>, TbProtoQueueMsg<JsInvokeProtos.RemoteJsResponse>> requestTemplate;

    protected final Map<String, String> scriptHashToBodysMap = new ConcurrentHashMap<>();
    private final Lock scriptsLock = new ReentrantLock();

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    @Override
    public void init() {
        super.init();
        requestTemplate.init();
    }

    /**
     * 功能：执行 `stop` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PreDestroy
    @Override
    public void stop() {
        super.stop();
        if (requestTemplate != null) {
            requestTemplate.stop();
        }
        callbackExecutor.shutdownNow();
    }

    /**
     * 功能：执行 `doEval` 对应的处理。
     * 参数：
     * - `scriptId`：`scriptId`ID。
     * - `jsInfo`：`jsInfo` 参数。
     * - `scriptBody`：`scriptBody` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    protected ListenableFuture<UUID> doEval(UUID scriptId, JsScriptInfo jsInfo, String scriptBody) {
        JsInvokeProtos.JsCompileRequest jsRequest = JsInvokeProtos.JsCompileRequest.newBuilder()
                .setScriptHash(jsInfo.getHash())
                .setFunctionName(jsInfo.getFunctionName())
                .setScriptBody(scriptBody).build();

        JsInvokeProtos.RemoteJsRequest jsRequestWrapper = JsInvokeProtos.RemoteJsRequest.newBuilder()
                .setCompileRequest(jsRequest)
                .build();

        log.trace("Post compile request for scriptId [{}] (hash: {})", scriptId, jsInfo.getHash());
        ListenableFuture<TbProtoQueueMsg<JsInvokeProtos.RemoteJsResponse>> future = requestTemplate.send(new TbProtoJsQueueMsg<>(UUID.randomUUID(), jsRequestWrapper));
        return Futures.transform(future, response -> {
            JsInvokeProtos.JsCompileResponse compilationResult = response.getValue().getCompileResponse();
            if (compilationResult.getSuccess()) {
                scriptsLock.lock();
                try {
                    scriptInfoMap.put(scriptId, jsInfo);
                    scriptHashToBodysMap.put(jsInfo.getHash(), scriptBody);
                } finally {
                    scriptsLock.unlock();
                }
                return scriptId;
            } else {
                log.debug("[{}] (hash: {}) Failed to compile script due to [{}]: {}", scriptId, compilationResult.getScriptHash(),
                        compilationResult.getErrorCode().name(), compilationResult.getErrorDetails());
                throw new TbScriptException(scriptId, TbScriptException.ErrorCode.COMPILATION, scriptBody, new RuntimeException(compilationResult.getErrorDetails()));
            }
        }, callbackExecutor);
    }

    /**
     * 功能：执行 `doInvokeFunction` 对应的处理。
     * 参数：
     * - `scriptId`：`scriptId`ID。
     * - `jsInfo`：`jsInfo` 参数。
     * - `args`：传入程序的参数。
     * 返回：匹配的数据集合。
     */
    @Override
    protected ListenableFuture<Object> doInvokeFunction(UUID scriptId, JsScriptInfo jsInfo, Object[] args) {
        var scriptHash = jsInfo.getHash();
        String scriptBody = scriptHashToBodysMap.get(scriptHash);
        if (scriptBody == null) {
            return Futures.immediateFailedFuture(new RuntimeException("No script body found for script hash [" + scriptHash + "] (script id: [" + scriptId + "])"));
        }

        JsInvokeProtos.RemoteJsRequest jsRequestWrapper = buildJsInvokeRequest(jsInfo, args, false, null);

        StopWatch stopWatch;
        if (log.isTraceEnabled()) {
            stopWatch = new StopWatch();
            stopWatch.start();
        } else {
            stopWatch = null;
        }

        UUID requestKey = UUID.randomUUID();
        ListenableFuture<TbProtoQueueMsg<JsInvokeProtos.RemoteJsResponse>> future = requestTemplate.send(new TbProtoJsQueueMsg<>(requestKey, jsRequestWrapper));
        return Futures.transformAsync(future, response -> {
            if (log.isTraceEnabled()) {
                stopWatch.stop();
                log.trace("doInvokeFunction js-response took {}ms for uuid {}", stopWatch.getTotalTimeMillis(), response.getKey());
            }
            JsInvokeProtos.JsInvokeResponse invokeResult = response.getValue().getInvokeResponse();
            if (invokeResult.getSuccess()) {
                return Futures.immediateFuture(invokeResult.getResult());
            } else {
                return handleInvokeError(requestKey, scriptId, jsInfo, invokeResult.getErrorCode(), invokeResult.getErrorDetails(), scriptBody, args);
            }
        }, callbackExecutor);
    }

    /**
     * 功能：构建请求。
     * 参数：
     * - `jsInfo`：`jsInfo` 参数。
     * - `args`：传入程序的参数。
     * - `includeScriptBody`：`includeScriptBody` 参数。
     * - `scriptBody`：`scriptBody` 参数。
     * 返回：处理结果。
     */
    private JsInvokeProtos.RemoteJsRequest buildJsInvokeRequest(JsScriptInfo jsInfo, Object[] args, boolean includeScriptBody, String scriptBody) {
        JsInvokeProtos.JsInvokeRequest.Builder jsRequestBuilder = JsInvokeProtos.JsInvokeRequest.newBuilder()
                .setScriptHash(jsInfo.getHash())
                .setFunctionName(jsInfo.getFunctionName())
                .setTimeout((int) maxExecRequestsTimeout);
        if (includeScriptBody) {
            jsRequestBuilder.setScriptBody(scriptBody);
        }

        for (Object arg : args) {
            jsRequestBuilder.addArgs(arg.toString());
        }

        JsInvokeProtos.RemoteJsRequest jsRequestWrapper = JsInvokeProtos.RemoteJsRequest.newBuilder()
                .setInvokeRequest(jsRequestBuilder.build())
                .build();
        return jsRequestWrapper;
    }

    /**
     * 功能：处理错误信息。
     * 参数：
     * - `requestKey`：请求对象。
     * - `scriptId`：`scriptId`ID。
     * - `jsInfo`：`jsInfo` 参数。
     * - `errorCode`：错误信息。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<Object> handleInvokeError(UUID requestKey, UUID scriptId, JsScriptInfo jsInfo,
                                                       JsInvokeProtos.JsInvokeErrorCode errorCode, String errorDetails,
                                                       String scriptBody, Object[] args) {
        final RuntimeException e = new RuntimeException(errorDetails);
        log.debug("[{}] Failed to invoke function due to [{}]: {}", scriptId, errorCode.name(), errorDetails);
        if (JsInvokeProtos.JsInvokeErrorCode.TIMEOUT_ERROR.equals(errorCode)) {
            throw new TbScriptException(scriptId, TbScriptException.ErrorCode.TIMEOUT, scriptBody, new TimeoutException());
        } else if (JsInvokeProtos.JsInvokeErrorCode.COMPILATION_ERROR.equals(errorCode)) {
            throw new TbScriptException(scriptId, TbScriptException.ErrorCode.COMPILATION, scriptBody, e);
        } else if (JsInvokeProtos.JsInvokeErrorCode.NOT_FOUND_ERROR.equals(errorCode)) {
            log.debug("[{}] Remote JS executor couldn't find the script", scriptId);
            if (scriptBody != null) {
                JsInvokeProtos.RemoteJsRequest invokeRequestWithScriptBody = buildJsInvokeRequest(jsInfo, args, true, scriptBody);
                log.debug("[{}] Sending invoke request again with script body", scriptId);
                ListenableFuture<TbProtoQueueMsg<JsInvokeProtos.RemoteJsResponse>> future = requestTemplate.send(new TbProtoJsQueueMsg<>(requestKey, invokeRequestWithScriptBody));
                return Futures.transformAsync(future, response -> {
                    JsInvokeProtos.JsInvokeResponse result = response.getValue().getInvokeResponse();
                    if (result.getSuccess()) {
                        return Futures.immediateFuture(result.getResult());
                    } else {
                        return handleInvokeError(requestKey, scriptId, jsInfo, result.getErrorCode(), result.getErrorDetails(), null, args);
                    }
                }, MoreExecutors.directExecutor());
            }
        }
        throw new TbScriptException(scriptId, TbScriptException.ErrorCode.RUNTIME, scriptBody, e);
    }

    /**
     * 功能：执行 `doRelease` 对应的处理。
     * 参数：
     * - `scriptId`：`scriptId`ID。
     * - `jsInfo`：`jsInfo` 参数。
     * 返回：无。
     */
    @Override
    protected void doRelease(UUID scriptId, JsScriptInfo jsInfo) throws Exception {
        String scriptHash = jsInfo.getHash();
        if (scriptInfoMap.values().stream().map(JsScriptInfo::getHash).anyMatch(hash -> hash.equals(scriptHash))) {
            return;
        }

        JsInvokeProtos.JsReleaseRequest jsRequest = JsInvokeProtos.JsReleaseRequest.newBuilder()
                .setScriptHash(scriptHash)
                .setFunctionName(jsInfo.getFunctionName()).build();

        JsInvokeProtos.RemoteJsRequest jsRequestWrapper = JsInvokeProtos.RemoteJsRequest.newBuilder()
                .setReleaseRequest(jsRequest)
                .build();

        ListenableFuture<TbProtoQueueMsg<JsInvokeProtos.RemoteJsResponse>> future = requestTemplate.send(new TbProtoJsQueueMsg<>(UUID.randomUUID(), jsRequestWrapper));
        if (getMaxInvokeRequestsTimeout() > 0) {
            future = Futures.withTimeout(future, getMaxInvokeRequestsTimeout(), TimeUnit.MILLISECONDS, timeoutExecutorService);
        }
        JsInvokeProtos.RemoteJsResponse response = future.get().getValue();

        JsInvokeProtos.JsReleaseResponse releaseResponse = response.getReleaseResponse();
        if (releaseResponse.getSuccess()) {
            scriptsLock.lock();
            try {
                if (scriptInfoMap.values().stream().map(JsScriptInfo::getHash).noneMatch(hash -> hash.equals(scriptHash))) {
                    scriptHashToBodysMap.remove(scriptHash);
                }
            } finally {
                scriptsLock.unlock();
            }
        } else {
            log.debug("[{}] Failed to release script", scriptHash);
        }
    }

    /**
     * 功能：执行 `constructFunctionName` 对应的处理。
     * 参数：
     * - `scriptId`：`scriptId`ID。
     * - `scriptHash`：`scriptHash` 参数。
     * 返回：文本结果。
     */
    protected String constructFunctionName(UUID scriptId, String scriptHash) {
        return "invokeInternal_" + scriptHash;
    }

    /**
     * 功能：获取`Script Hash`。
     * 参数：
     * - `scriptId`：`scriptId`ID。
     * 返回：文本结果。
     */
    protected String getScriptHash(UUID scriptId) {
        JsScriptInfo jsScriptInfo = scriptInfoMap.get(scriptId);
        return jsScriptInfo != null ? jsScriptInfo.getHash() : null;
    }

}
