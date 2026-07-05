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
package org.thingsboard.script.api.tbel;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.mvel2.CompileException;
import org.mvel2.ExecutionContext;
import org.mvel2.MVEL;
import org.mvel2.ParserContext;
import org.mvel2.SandboxedParserConfiguration;
import org.mvel2.ScriptMemoryOverflowException;
import org.mvel2.optimizers.OptimizerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.script.api.AbstractScriptInvokeService;
import org.thingsboard.script.api.ScriptType;
import org.thingsboard.script.api.TbScriptException;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.stats.TbApiUsageReportClient;
import org.thingsboard.server.common.stats.TbApiUsageStateClient;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 中文说明：
 * 1. 类目的：`DefaultTbelInvokeService` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Slf4j
@ConditionalOnProperty(prefix = "tbel", value = "enabled", havingValue = "true", matchIfMissing = true)
@Service
public class DefaultTbelInvokeService extends AbstractScriptInvokeService implements TbelInvokeService {

    protected final Map<UUID, String> scriptIdToHash = new ConcurrentHashMap<>();
    protected final Map<String, TbelScript> scriptMap = new ConcurrentHashMap<>();
    /**
     * `compiledScriptsCache` 字段，保存当前对象的对应属性。
     */
    protected Cache<String, Serializable> compiledScriptsCache;

    /**
     * 配置，保存当前对象的配置选项。
     */
    private SandboxedParserConfiguration parserConfig;
    private final Optional<TbApiUsageStateClient> apiUsageStateClient;
    /**
     * 客户端，用于发起外部调用或协议交互。
     */
    private final Optional<TbApiUsageReportClient> apiUsageReportClient;

    /**
     * 参数，表示当前对象的对应属性。
     */
    @Getter
    @Value("${tbel.max_total_args_size:100000}")
    private long maxTotalArgsSize;
    /**
     * `maxResultSize` 字段，保存当前对象的对应属性。
     */
    @Getter
    @Value("${tbel.max_result_size:300000}")
    private long maxResultSize;
    /**
     * `maxScriptBodySize` 字段，保存当前对象的对应属性。
     */
    @Getter
    @Value("${tbel.max_script_body_size:50000}")
    private long maxScriptBodySize;

    /**
     * `maxErrors` 字段，保存当前对象的对应属性。
     */
    @Getter
    @Value("${tbel.max_errors:3}")
    private int maxErrors;

    /**
     * 持续时间，用于控制时间范围或等待时长。
     */
    @Getter
    @Value("${tbel.max_black_list_duration_sec:60}")
    private int maxBlackListDurationSec;

    /**
     * 当前请求对象，封装本次处理需要的输入信息。
     */
    @Getter
    @Value("${tbel.max_requests_timeout:0}")
    private long maxInvokeRequestsTimeout;

    /**
     * 是否启用`stats`。
     */
    @Getter
    @Value("${tbel.stats.enabled:false}")
    private boolean statsEnabled;

    /**
     * 线程池大小，用于控制处理规模或位置。
     */
    @Value("${tbel.thread_pool_size:50}")
    private int threadPoolSize;

    /**
     * 数量限制，用于控制数量、位置或分页范围。
     */
    @Value("${tbel.max_memory_limit_mb:8}")
    private long maxMemoryLimitMb;

    /**
     * `compiledScriptsCacheSize` 字段，保存当前对象的对应属性。
     */
    @Value("${tbel.compiled_scripts_cache_size:1000}")
    private int compiledScriptsCacheSize;

    /**
     * 执行器列表，用于保存一组待处理对象。
     */
    private ListeningExecutorService executor;

    private final Lock lock = new ReentrantLock();

    /**
     * 功能：创建 `DefaultTbelInvokeService` 实例，并初始化必要字段。
     * 参数：
     * - `apiUsageStateClient`：客户端对象。
     * - `apiUsageReportClient`：客户端对象。
     * 返回：新创建的对象实例。
     */
    protected DefaultTbelInvokeService(Optional<TbApiUsageStateClient> apiUsageStateClient, Optional<TbApiUsageReportClient> apiUsageReportClient) {
        this.apiUsageStateClient = apiUsageStateClient;
        this.apiUsageReportClient = apiUsageReportClient;
    }

    /**
     * 功能：执行 `printStats` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Scheduled(fixedDelayString = "${tbel.stats.print_interval_ms:10000}")
    public void printStats() {
        super.printStats();
    }

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @SneakyThrows
    @PostConstruct
    @Override
    public void init() {
        super.init();
        OptimizerFactory.setDefaultOptimizer(OptimizerFactory.SAFE_REFLECTIVE);
        parserConfig = ParserContext.enableSandboxedMode();
        parserConfig.addImport("JSON", TbJson.class);
        parserConfig.registerDataType("Date", TbDate.class, date -> 8L);
        parserConfig.registerDataType("Random", Random.class, date -> 8L);
        parserConfig.registerDataType("Calendar", Calendar.class, date -> 8L);
        TbUtils.register(parserConfig);
        executor = MoreExecutors.listeningDecorator(ThingsBoardExecutors.newWorkStealingPool(threadPoolSize, "tbel-executor"));
        try {
            // Special command to warm up TBEL engine
            Serializable script = compileScript("var warmUp = {}; warmUp");
            MVEL.executeTbExpression(script, new ExecutionContext(parserConfig), Collections.emptyMap());
        } catch (Exception e) {
            // do nothing
        }
        compiledScriptsCache = Caffeine.newBuilder()
                .maximumSize(compiledScriptsCacheSize)
                .build();
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
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    /**
     * 功能：获取名称。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    protected String getStatsName() {
        return "TBEL Scripts Stats";
    }

    /**
     * 功能：获取回调。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected Executor getCallbackExecutor() {
        return MoreExecutors.directExecutor();
    }

    /**
     * 功能：判断`Script Present`。
     * 参数：
     * - `scriptId`：`scriptId`ID。
     * 返回：判断结果。
     */
    @Override
    protected boolean isScriptPresent(UUID scriptId) {
        return scriptIdToHash.containsKey(scriptId);
    }

    /**
     * 功能：判断`Exec Enabled`。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：判断结果。
     */
    @Override
    protected boolean isExecEnabled(TenantId tenantId) {
        return !apiUsageStateClient.isPresent() || apiUsageStateClient.get().getApiUsageState(tenantId).isTbelExecEnabled();
    }

    /**
     * 功能：上报`Execution`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * 返回：无。
     */
    @Override
    protected void reportExecution(TenantId tenantId, CustomerId customerId) {
        apiUsageReportClient.ifPresent(client -> client.report(tenantId, customerId, ApiUsageRecordKey.TBEL_EXEC_COUNT, 1));
    }

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
    @Override
    protected ListenableFuture<UUID> doEvalScript(TenantId tenantId, ScriptType scriptType, String scriptBody, UUID scriptId, String[] argNames) {
        return executor.submit(() -> {
            try {
                String scriptHash = hash(scriptBody, argNames);
                compiledScriptsCache.get(scriptHash, k -> compileScript(scriptBody));
                lock.lock();
                try {
                    scriptIdToHash.put(scriptId, scriptHash);
                    scriptMap.computeIfAbsent(scriptHash, k -> new TbelScript(scriptBody, argNames));
                } finally {
                    lock.unlock();
                }
                return scriptId;
            } catch (Exception e) {
                throw new TbScriptException(scriptId, TbScriptException.ErrorCode.COMPILATION, scriptBody, e);
            }
        });
    }

    /**
     * 功能：执行 `doInvokeFunction` 对应的处理。
     * 参数：
     * - `scriptId`：`scriptId`ID。
     * - `args`：传入程序的参数。
     * 返回：处理结果。
     */
    @Override
    protected TbelScriptExecutionTask doInvokeFunction(UUID scriptId, Object[] args) {
        ExecutionContext executionContext = new ExecutionContext(this.parserConfig, maxMemoryLimitMb * 1024 * 1024);
        return new TbelScriptExecutionTask(executionContext, executor.submit(() -> {
            String scriptHash = scriptIdToHash.get(scriptId);
            if (scriptHash == null) {
                throw new TbScriptException(scriptId, TbScriptException.ErrorCode.OTHER, null, new RuntimeException("Script not found!"));
            }
            TbelScript script = scriptMap.get(scriptHash);
            Serializable compiledScript = compiledScriptsCache.get(scriptHash, k -> compileScript(script.getScriptBody()));
            try {
                return MVEL.executeTbExpression(compiledScript, executionContext, script.createVars(args));
            } catch (ScriptMemoryOverflowException e) {
                throw new TbScriptException(scriptId, TbScriptException.ErrorCode.OTHER, script.getScriptBody(), new RuntimeException("Script memory overflow!"));
            } catch (Exception e) {
                throw new TbScriptException(scriptId, TbScriptException.ErrorCode.RUNTIME, script.getScriptBody(), e);
            }
        }));
    }

    /**
     * 功能：执行 `doRelease` 对应的处理。
     * 参数：
     * - `scriptId`：`scriptId`ID。
     * 返回：无。
     */
    @Override
    protected void doRelease(UUID scriptId) {
        String scriptHash = scriptIdToHash.remove(scriptId);
        if (scriptHash != null) {
            lock.lock();
            try {
                if (!scriptIdToHash.containsValue(scriptHash)) {
                    scriptMap.remove(scriptHash);
                    compiledScriptsCache.invalidate(scriptHash);
                }
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * 功能：执行 `compileScript` 对应的处理。
     * 参数：
     * - `scriptBody`：`scriptBody` 参数。
     * 返回：处理结果。
     */
    private Serializable compileScript(String scriptBody) {
        return MVEL.compileExpression(scriptBody, new ParserContext());
    }

    /**
     * 功能：执行 `hash` 对应的处理。
     * 参数：
     * - `scriptBody`：`scriptBody` 参数。
     * - `argNames`：名称。
     * 返回：文本结果。
     */
    @SuppressWarnings("UnstableApiUsage")
    protected String hash(String scriptBody, String[] argNames) {
        Hasher hasher = Hashing.murmur3_128().newHasher();
        hasher.putUnencodedChars(scriptBody);
        for (String argName : argNames) {
            hasher.putString(argName, StandardCharsets.UTF_8);
        }
        return hasher.hash().toString();
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultTbelInvokeService` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
