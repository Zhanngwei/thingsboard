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

import com.google.common.hash.Hashing;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.thingsboard.script.api.AbstractScriptInvokeService;
import org.thingsboard.script.api.RuleNodeScriptFactory;
import org.thingsboard.script.api.ScriptType;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.stats.TbApiUsageReportClient;
import org.thingsboard.server.common.stats.TbApiUsageStateClient;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ashvayka on 26.09.18.
 */
/**
 * 中文说明：
 * 1. 类目的：`AbstractJsInvokeService` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Slf4j
public abstract class AbstractJsInvokeService extends AbstractScriptInvokeService implements JsInvokeService {

    protected final Map<UUID, JsScriptInfo> scriptInfoMap = new ConcurrentHashMap<>();
    /**
     * 状态，用于发起外部调用或协议交互。
     */
    private final Optional<TbApiUsageStateClient> apiUsageStateClient;
    private final Optional<TbApiUsageReportClient> apiUsageReportClient;

    /**
     * 参数，表示当前对象的对应属性。
     */
    @Getter
    @Value("${js.max_total_args_size:100000}")
    private long maxTotalArgsSize;
    /**
     * `maxResultSize` 字段，保存当前对象的对应属性。
     */
    @Getter
    @Value("${js.max_result_size:300000}")
    private long maxResultSize;
    /**
     * `maxScriptBodySize` 字段，保存当前对象的对应属性。
     */
    @Getter
    @Value("${js.max_script_body_size:50000}")
    private long maxScriptBodySize;

    /**
     * 功能：创建 `AbstractJsInvokeService` 实例，并初始化必要字段。
     * 参数：
     * - `apiUsageStateClient`：客户端对象。
     * - `apiUsageReportClient`：客户端对象。
     * 返回：新创建的对象实例。
     */
    protected AbstractJsInvokeService(Optional<TbApiUsageStateClient> apiUsageStateClient, Optional<TbApiUsageReportClient> apiUsageReportClient) {
        this.apiUsageStateClient = apiUsageStateClient;
        this.apiUsageReportClient = apiUsageReportClient;
    }

    /**
     * 功能：判断`Script Present`。
     * 参数：
     * - `scriptId`：`scriptId`ID。
     * 返回：判断结果。
     */
    @Override
    protected boolean isScriptPresent(UUID scriptId) {
        return scriptInfoMap.containsKey(scriptId);
    }

    /**
     * 功能：判断`Exec Enabled`。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：判断结果。
     */
    @Override
    protected boolean isExecEnabled(TenantId tenantId) {
        return !apiUsageStateClient.isPresent() || apiUsageStateClient.get().getApiUsageState(tenantId).isJsExecEnabled();
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
        apiUsageReportClient.ifPresent(client -> client.report(tenantId, customerId, ApiUsageRecordKey.JS_EXEC_COUNT, 1));
    }

    /**
     * 功能：执行 `doInvokeFunction` 对应的处理。
     * 参数：
     * - `scriptId`：`scriptId`ID。
     * - `args`：传入程序的参数。
     * 返回：处理结果。
     */
    @Override
    protected JsScriptExecutionTask doInvokeFunction(UUID scriptId, Object[] args) {
        return new JsScriptExecutionTask(doInvokeFunction(scriptId, scriptInfoMap.get(scriptId), args));
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
        String scriptHash = hash(tenantId, scriptBody);
        String functionName = constructFunctionName(scriptId, scriptHash);
        String jsScript = generateJsScript(scriptType, functionName, scriptBody, argNames);
        return doEval(scriptId, new JsScriptInfo(scriptHash, functionName), jsScript);
    }

    /**
     * 功能：执行 `doRelease` 对应的处理。
     * 参数：
     * - `scriptId`：`scriptId`ID。
     * 返回：无。
     */
    @Override
    protected void doRelease(UUID scriptId) throws Exception {
        doRelease(scriptId, scriptInfoMap.remove(scriptId));
    }

    /**
     * 功能：执行 `doEval` 对应的处理。
     * 参数：
     * - `scriptId`：`scriptId`ID。
     * - `jsInfo`：`jsInfo` 参数。
     * - `scriptBody`：`scriptBody` 参数。
     * 返回：匹配的数据集合。
     */
    protected abstract ListenableFuture<UUID> doEval(UUID scriptId, JsScriptInfo jsInfo, String scriptBody);

    /**
     * 功能：执行 `doInvokeFunction` 对应的处理。
     * 参数：
     * - `scriptId`：`scriptId`ID。
     * - `jsInfo`：`jsInfo` 参数。
     * - `args`：传入程序的参数。
     * 返回：匹配的数据集合。
     */
    protected abstract ListenableFuture<Object> doInvokeFunction(UUID scriptId, JsScriptInfo jsInfo, Object[] args);

    /**
     * 功能：执行 `doRelease` 对应的处理。
     * 参数：
     * - `scriptId`：`scriptId`ID。
     * - `scriptInfo`：`scriptInfo` 参数。
     * 返回：无。
     */
    protected abstract void doRelease(UUID scriptId, JsScriptInfo scriptInfo) throws Exception;

    /**
     * 功能：执行 `generateJsScript` 对应的处理。
     * 参数：
     * - `scriptType`：类型。
     * - `functionName`：名称。
     * - `scriptBody`：`scriptBody` 参数。
     * - `argNames`：名称。
     * 返回：文本结果。
     */
    private String generateJsScript(ScriptType scriptType, String functionName, String scriptBody, String... argNames) {
        if (scriptType == ScriptType.RULE_NODE_SCRIPT) {
            return RuleNodeScriptFactory.generateRuleNodeScript(functionName, scriptBody, argNames);
        }
        throw new RuntimeException("No script factory implemented for scriptType: " + scriptType);
    }

    /**
     * 功能：执行 `constructFunctionName` 对应的处理。
     * 参数：
     * - `scriptId`：`scriptId`ID。
     * - `scriptHash`：`scriptHash` 参数。
     * 返回：文本结果。
     */
    protected String constructFunctionName(UUID scriptId, String scriptHash) {
        return "invokeInternal_" + scriptId.toString().replace('-', '_');
    }

    /**
     * 功能：执行 `hash` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `scriptBody`：`scriptBody` 参数。
     * 返回：文本结果。
     */
    protected String hash(TenantId tenantId, String scriptBody) {
        return Hashing.murmur3_128().newHasher()
                .putLong(tenantId.getId().getMostSignificantBits())
                .putLong(tenantId.getId().getLeastSignificantBits())
                .putUnencodedChars(scriptBody)
                .hash().toString();
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`AbstractJsInvokeService` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
