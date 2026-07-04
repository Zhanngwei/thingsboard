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

@Slf4j
@ConditionalOnExpression("'${js.evaluator:null}'=='remote' && ('${service.type:null}'=='monolith' || '${service.type:null}'=='tb-core' || '${service.type:null}'=='tb-rule-engine')")
@Service
/**
 * 中文说明：
 * 1. 类目的：`RemoteJsInvokeService` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
public class RemoteJsInvokeService extends AbstractJsInvokeService {

    @Getter
    @Value("${queue.js.max_eval_requests_timeout}")
    /**
     * 字段说明：
     * 1. 保存 `maxEvalRequestsTimeout` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private long maxEvalRequestsTimeout;

    @Getter
    @Value("${queue.js.max_requests_timeout}")
    /**
     * 字段说明：
     * 1. 保存 `maxInvokeRequestsTimeout` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private long maxInvokeRequestsTimeout;

    @Value("${queue.js.max_exec_requests_timeout:2000}")
    /**
     * 字段说明：
     * 1. 保存 `maxExecRequestsTimeout` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private long maxExecRequestsTimeout;

    @Getter
    @Value("${js.remote.max_errors}")
    /**
     * 字段说明：
     * 1. 保存 `maxErrors` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private int maxErrors;

    @Getter
    @Value("${js.remote.max_black_list_duration_sec:60}")
    /**
     * 字段说明：
     * 1. 保存 `maxBlackListDurationSec` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private int maxBlackListDurationSec;

    @Getter
    @Value("${js.remote.stats.enabled:false}")
    /**
     * 字段说明：
     * 1. 保存 `statsEnabled` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private boolean statsEnabled;

    private final ExecutorService callbackExecutor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors(), ThingsBoardThreadFactory.forName("js-executor-remote-callback"));

    /**
     * 方法说明：
     * 1. 职责：执行 `RemoteJsInvokeService` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public RemoteJsInvokeService(Optional<TbApiUsageStateClient> apiUsageStateClient, Optional<TbApiUsageReportClient> apiUsageClient) {
        super(apiUsageStateClient, apiUsageClient);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `getCallbackExecutor` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected Executor getCallbackExecutor() {
        return callbackExecutor;
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `getStatsName` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected String getStatsName() {
        return "Queue JS Invoke Stats";
    }

    @Scheduled(fixedDelayString = "${js.remote.stats.print_interval_ms}")
    /**
     * 方法说明：
     * 1. 职责：执行 `printStats` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void printStats() {
        super.printStats();
    }

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `requestTemplate` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    protected TbQueueRequestTemplate<TbProtoJsQueueMsg<JsInvokeProtos.RemoteJsRequest>, TbProtoQueueMsg<JsInvokeProtos.RemoteJsResponse>> requestTemplate;

    protected final Map<String, String> scriptHashToBodysMap = new ConcurrentHashMap<>();
    private final Lock scriptsLock = new ReentrantLock();

    @PostConstruct
    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `init` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void init() {
        super.init();
        requestTemplate.init();
    }

    @PreDestroy
    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `stop` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void stop() {
        super.stop();
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (requestTemplate != null) {
            requestTemplate.stop();
        }
        callbackExecutor.shutdownNow();
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `doEval` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected ListenableFuture<UUID> doEval(UUID scriptId, JsScriptInfo jsInfo, String scriptBody) {
        JsInvokeProtos.JsCompileRequest jsRequest = JsInvokeProtos.JsCompileRequest.newBuilder()
                .setScriptHash(jsInfo.getHash())
                .setFunctionName(jsInfo.getFunctionName())
                .setScriptBody(scriptBody).build();

        JsInvokeProtos.RemoteJsRequest jsRequestWrapper = JsInvokeProtos.RemoteJsRequest.newBuilder()
                .setCompileRequest(jsRequest)
                .build();

        log.trace("Post compile request for scriptId [{}] (hash: {})", scriptId, jsInfo.getHash());
        // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
        ListenableFuture<TbProtoQueueMsg<JsInvokeProtos.RemoteJsResponse>> future = requestTemplate.send(new TbProtoJsQueueMsg<>(UUID.randomUUID(), jsRequestWrapper));
        // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
        return Futures.transform(future, response -> {
            JsInvokeProtos.JsCompileResponse compilationResult = response.getValue().getCompileResponse();
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
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

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `doInvokeFunction` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected ListenableFuture<Object> doInvokeFunction(UUID scriptId, JsScriptInfo jsInfo, Object[] args) {
        var scriptHash = jsInfo.getHash();
        String scriptBody = scriptHashToBodysMap.get(scriptHash);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (scriptBody == null) {
            // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
            return Futures.immediateFailedFuture(new RuntimeException("No script body found for script hash [" + scriptHash + "] (script id: [" + scriptId + "])"));
        }

        JsInvokeProtos.RemoteJsRequest jsRequestWrapper = buildJsInvokeRequest(jsInfo, args, false, null);

        StopWatch stopWatch;
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (log.isTraceEnabled()) {
            stopWatch = new StopWatch();
            stopWatch.start();
        } else {
            stopWatch = null;
        }

        UUID requestKey = UUID.randomUUID();
        // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
        ListenableFuture<TbProtoQueueMsg<JsInvokeProtos.RemoteJsResponse>> future = requestTemplate.send(new TbProtoJsQueueMsg<>(requestKey, jsRequestWrapper));
        // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
        return Futures.transformAsync(future, response -> {
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (log.isTraceEnabled()) {
                stopWatch.stop();
                log.trace("doInvokeFunction js-response took {}ms for uuid {}", stopWatch.getTotalTimeMillis(), response.getKey());
            }
            JsInvokeProtos.JsInvokeResponse invokeResult = response.getValue().getInvokeResponse();
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (invokeResult.getSuccess()) {
                // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
                return Futures.immediateFuture(invokeResult.getResult());
            } else {
                return handleInvokeError(requestKey, scriptId, jsInfo, invokeResult.getErrorCode(), invokeResult.getErrorDetails(), scriptBody, args);
            }
        }, callbackExecutor);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `buildJsInvokeRequest` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
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
     * 方法说明：
     * 1. 职责：执行 `handleInvokeError` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
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

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `doRelease` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
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
     * 方法说明：
     * 1. 职责：执行 `constructFunctionName` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected String constructFunctionName(UUID scriptId, String scriptHash) {
        return "invokeInternal_" + scriptHash;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getScriptHash` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected String getScriptHash(UUID scriptId) {
        JsScriptInfo jsScriptInfo = scriptInfoMap.get(scriptId);
        return jsScriptInfo != null ? jsScriptInfo.getHash() : null;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`RemoteJsInvokeService` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
