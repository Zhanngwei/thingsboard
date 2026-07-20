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
package org.thingsboard.script.api.js;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import delight.nashornsandbox.NashornSandbox;
import delight.nashornsandbox.NashornSandboxes;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.script.api.TbScriptException;
import org.thingsboard.server.common.stats.TbApiUsageReportClient;
import org.thingsboard.server.common.stats.TbApiUsageStateClient;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 中文说明：
 * 1. `NashornJsInvokeService` 是 ThingsBoard Common 中负责 `Nashorn Js Invoke` 的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `AbstractJsInvokeService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Slf4j
@ConditionalOnProperty(prefix = "js", value = "evaluator", havingValue = "local", matchIfMissing = true)
@Service
public class NashornJsInvokeService extends AbstractJsInvokeService {

    /**
     * `sandbox` 字段，保存当前对象的对应属性。
     */
    private NashornSandbox sandbox;
    private ScriptEngine engine;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    private ExecutorService monitorExecutorService;
    private ListeningExecutorService jsExecutor;

    private final ReentrantLock evalLock = new ReentrantLock();

    /**
     * 是否使用`js sandbox`。
     */
    @Value("${js.local.use_js_sandbox}")
    private boolean useJsSandbox;

    /**
     * 线程池大小，用于控制处理规模或位置。
     */
    @Value("${js.local.monitor_thread_pool_size}")
    private int monitorThreadPoolSize;

    /**
     * 时间，用于控制时间范围或等待时长。
     */
    @Value("${js.local.max_cpu_time}")
    private long maxCpuTime;

    /**
     * `maxErrors` 字段，保存当前对象的对应属性。
     */
    @Getter
    @Value("${js.local.max_errors}")
    private int maxErrors;

    /**
     * 持续时间，用于控制时间范围或等待时长。
     */
    @Getter
    @Value("${js.local.max_black_list_duration_sec:60}")
    private int maxBlackListDurationSec;

    /**
     * 当前请求对象，封装本次处理需要的输入信息。
     */
    @Getter
    @Value("${js.local.max_requests_timeout:0}")
    private long maxInvokeRequestsTimeout;

    /**
     * 是否启用`stats`。
     */
    @Getter
    @Value("${js.local.stats.enabled:false}")
    private boolean statsEnabled;

    /**
     * 线程池大小，负责处理对应任务或消息。
     */
    @Value("${js.local.js_thread_pool_size:50}")
    private int jsExecutorThreadPoolSize;

    /**
     * 功能：创建 `NashornJsInvokeService` 实例，并初始化必要字段。
     * 参数：
     * - `apiUsageStateClient`：客户端对象。
     * - `apiUsageReportClient`：客户端对象。
     * 返回：新创建的对象实例。
     */
    public NashornJsInvokeService(Optional<TbApiUsageStateClient> apiUsageStateClient, Optional<TbApiUsageReportClient> apiUsageReportClient) {
        super(apiUsageStateClient, apiUsageReportClient);
    }

    /**
     * 功能：获取名称。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    protected String getStatsName() {
        return "Nashorn JS Invoke Stats";
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
     * 功能：执行 `printStats` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Scheduled(fixedDelayString = "${js.local.stats.print_interval_ms:10000}")
    public void printStats() {
        super.printStats();
    }

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    @Override
    public void init() {
        super.init();
        jsExecutor = MoreExecutors.listeningDecorator(Executors.newWorkStealingPool(jsExecutorThreadPoolSize));
        if (useJsSandbox) {
            sandbox = NashornSandboxes.create();
            monitorExecutorService = ThingsBoardExecutors.newWorkStealingPool(monitorThreadPoolSize, "nashorn-js-monitor");
            sandbox.setExecutor(monitorExecutorService);
            sandbox.setMaxCPUTime(maxCpuTime);
            sandbox.allowNoBraces(false);
            sandbox.allowLoadFunctions(true);
            sandbox.setMaxPreparedStatements(30);
        } else {
            ScriptEngineManager factory = new ScriptEngineManager();
            engine = factory.getEngineByName("nashorn");
        }
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
        if (monitorExecutorService != null) {
            monitorExecutorService.shutdownNow();
        }
        if (jsExecutor != null) {
            jsExecutor.shutdownNow();
        }
    }

    /**
     * 功能：执行 `doEval` 对应的处理。
     * 参数：
     * - `scriptId`：`scriptId`ID。
     * - `scriptInfo`：`scriptInfo` 参数。
     * - `jsScript`：`jsScript` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    protected ListenableFuture<UUID> doEval(UUID scriptId, JsScriptInfo scriptInfo, String jsScript) {
        return jsExecutor.submit(() -> {
            try {
                evalLock.lock();
                try {
                    if (useJsSandbox) {
                        sandbox.eval(jsScript);
                    } else {
                        engine.eval(jsScript);
                    }
                } finally {
                    evalLock.unlock();
                }
                scriptInfoMap.put(scriptId, scriptInfo);
                return scriptId;
            } catch (Exception e) {
                throw new TbScriptException(scriptId, TbScriptException.ErrorCode.COMPILATION, jsScript, e);
            }
        });
    }

    /**
     * 功能：执行 `doInvokeFunction` 对应的处理。
     * 参数：
     * - `scriptId`：`scriptId`ID。
     * - `scriptInfo`：`scriptInfo` 参数。
     * - `args`：传入程序的参数。
     * 返回：匹配的数据集合。
     */
    @Override
    protected ListenableFuture<Object> doInvokeFunction(UUID scriptId, JsScriptInfo scriptInfo, Object[] args) {
        return jsExecutor.submit(() -> {
            try {
                if (useJsSandbox) {
                    return sandbox.getSandboxedInvocable().invokeFunction(scriptInfo.getFunctionName(), args);
                } else {
                    return ((Invocable) engine).invokeFunction(scriptInfo.getFunctionName(), args);
                }
            } catch (ScriptException e) {
                throw new TbScriptException(scriptId, TbScriptException.ErrorCode.RUNTIME, null, e);
            } catch (Exception e) {
                throw new TbScriptException(scriptId, TbScriptException.ErrorCode.OTHER, null, e);
            }
        });
    }

    /**
     * 功能：执行 `doRelease` 对应的处理。
     * 参数：
     * - `scriptId`：`scriptId`ID。
     * - `scriptInfo`：`scriptInfo` 参数。
     * 返回：无。
     */
    protected void doRelease(UUID scriptId, JsScriptInfo scriptInfo) throws ScriptException {
        if (useJsSandbox) {
            sandbox.eval(scriptInfo.getFunctionName() + " = undefined;");
        } else {
            engine.eval(scriptInfo.getFunctionName() + " = undefined;");
        }
    }

}
