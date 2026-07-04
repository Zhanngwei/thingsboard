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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.benmanes.caffeine.cache.Cache;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.script.api.ScriptType;
import org.thingsboard.script.api.tbel.TbelInvokeService;
import org.thingsboard.script.api.tbel.TbelScript;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.thingsboard.server.common.data.msg.TbMsgType.POST_TELEMETRY_REQUEST;

@DaoSqlTest
@TestPropertySource(properties = {
        "tbel.max_script_body_size=100",
        "tbel.max_total_args_size=50",
        "tbel.max_result_size=50",
        "tbel.max_errors=2",
        "tbel.compiled_scripts_cache_size=100"
})
/**
 * 中文说明：
 * 1. 类目的：`TbelInvokeServiceTest` 是ThingsBoard Application 测试模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
class TbelInvokeServiceTest extends AbstractControllerTest {

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `invokeService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private TbelInvokeService invokeService;

    @Value("${tbel.max_errors}")
    /**
     * 字段说明：
     * 1. 保存 `maxJsErrors` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private int maxJsErrors;

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `givenSimpleScriptTestPerformance` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    void givenSimpleScriptTestPerformance() throws ExecutionException, InterruptedException {
        int iterations = 100000;
        UUID scriptId = evalScript("return msg.temperature > 20");
        // warmup
        ObjectNode msg = JacksonUtil.newObjectNode();
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (int i = 0; i < 100; i++) {
            msg.put("temperature", i);
            boolean expected = i > 20;
            boolean result = Boolean.valueOf(invokeScript(scriptId, JacksonUtil.toString(msg)));
            Assert.assertEquals(expected, result);
        }
        long startTs = System.currentTimeMillis();
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (int i = 0; i < iterations; i++) {
            msg.put("temperature", i);
            boolean expected = i > 20;
            boolean result = Boolean.valueOf(invokeScript(scriptId, JacksonUtil.toString(msg)));
            Assert.assertEquals(expected, result);
        }
        long duration = System.currentTimeMillis() - startTs;
        System.out.println(iterations + " invocations took: " + duration + "ms");
        Assert.assertTrue(duration < TimeUnit.MINUTES.toMillis(1));
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `givenTooBigScriptForEval_thenReturnError` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    void givenTooBigScriptForEval_thenReturnError() {
        String hugeScript = "var a = 'qwertyqwertywertyqwabababerqwertyqwertywertyqwabababerqwertyqwertywertyqwabababerqwertyqwertywertyqwabababerqwertyqwertywertyqwabababer'; return {a: a};";

        assertThatThrownBy(() -> {
            evalScript(hugeScript);
        }).hasMessageContaining("body exceeds maximum allowed size");
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `givenTooBigScriptInputArgs_thenReturnErrorAndReportScriptExecutionError` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    void givenTooBigScriptInputArgs_thenReturnErrorAndReportScriptExecutionError() throws Exception {
        String script = "return { msg: msg };";
        String hugeMsg = "{\"input\":\"123456781234349\"}";
        UUID scriptId = evalScript(script);

        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (int i = 0; i < maxJsErrors; i++) {
            assertThatThrownBy(() -> {
                invokeScript(scriptId, hugeMsg);
            }).hasMessageContaining("input arguments exceed maximum");
        }
        assertThatScriptIsBlocked(scriptId);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `whenScriptInvocationResultIsTooBig_thenReturnErrorAndReportScriptExecutionError` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    void whenScriptInvocationResultIsTooBig_thenReturnErrorAndReportScriptExecutionError() throws Exception {
        String script = "var s = 'a'; for(int i=0; i<50; i++){ s +='a';} return { s: s};";
        UUID scriptId = evalScript(script);

        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (int i = 0; i < maxJsErrors; i++) {
            assertThatThrownBy(() -> {
                invokeScript(scriptId, "{}");
            }).hasMessageContaining("result exceeds maximum allowed size");
        }
        assertThatScriptIsBlocked(scriptId);
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `givenScriptsWithSameBody_thenCompileAndCacheOnlyOnce` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    void givenScriptsWithSameBody_thenCompileAndCacheOnlyOnce() throws Exception {
        String script = "return msg.temperature > 20;";
        List<UUID> scriptsIds = new ArrayList<>();
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (int i = 0; i < 100; i++) {
            UUID scriptId = evalScript(script);
            scriptsIds.add(scriptId);
        }

        Map<UUID, String> scriptIdToHash = getFieldValue(invokeService, "scriptIdToHash");
        Map<String, TbelScript> scriptMap = getFieldValue(invokeService, "scriptMap");
        // 缓存读写用于降低重复查询成本，需要注意失效策略和多节点一致性。
        Cache<String, Serializable> compiledScriptsCache = getFieldValue(invokeService, "compiledScriptsCache");

        String scriptHash = scriptIdToHash.get(scriptsIds.get(0));

        assertThat(scriptsIds.stream().map(scriptIdToHash::get)).containsOnly(scriptHash);
        assertThat(scriptMap).containsKey(scriptHash);
        // 缓存读写用于降低重复查询成本，需要注意失效策略和多节点一致性。
        assertThat(compiledScriptsCache.getIfPresent(scriptHash)).isNotNull();
    }

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `whenReleasingScript_thenCheckForScriptHashUsages` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void whenReleasingScript_thenCheckForScriptHashUsages() throws Exception {
        String script = "return msg.temperature > 20;";
        List<UUID> scriptsIds = new ArrayList<>();
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (int i = 0; i < 10; i++) {
            UUID scriptId = evalScript(script);
            scriptsIds.add(scriptId);
        }

        Map<UUID, String> scriptIdToHash = getFieldValue(invokeService, "scriptIdToHash");
        Map<String, TbelScript> scriptMap = getFieldValue(invokeService, "scriptMap");
        // 缓存读写用于降低重复查询成本，需要注意失效策略和多节点一致性。
        Cache<String, Serializable> compiledScriptsCache = getFieldValue(invokeService, "compiledScriptsCache");

        String scriptHash = scriptIdToHash.get(scriptsIds.get(0));
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (int i = 0; i < 9; i++) {
            UUID scriptId = scriptsIds.get(i);
            assertThat(scriptIdToHash).containsKey(scriptId);
            invokeService.release(scriptId);
            assertThat(scriptIdToHash).doesNotContainKey(scriptId);
        }
        assertThat(scriptMap).containsKey(scriptHash);
        // 缓存读写用于降低重复查询成本，需要注意失效策略和多节点一致性。
        assertThat(compiledScriptsCache.getIfPresent(scriptHash)).isNotNull();

        invokeService.release(scriptsIds.get(9));
        assertThat(scriptMap).doesNotContainKey(scriptHash);
        // 缓存读写用于降低重复查询成本，需要注意失效策略和多节点一致性。
        assertThat(compiledScriptsCache.getIfPresent(scriptHash)).isNull();
    }

    @Test
    @Ignore("This test is based on assumption that Caffeine cache is LRU based but in fact it is based on " +
            "Tiny LFU which is the cause that the tests fail sometime: https://arxiv.org/pdf/1512.00727.pdf")
    /**
     * 方法说明：
     * 1. 职责：执行 `whenCompiledScriptsCacheIsTooBig_thenRemoveRarelyUsedScripts` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void whenCompiledScriptsCacheIsTooBig_thenRemoveRarelyUsedScripts() throws Exception {
        Map<UUID, String> scriptIdToHash = getFieldValue(invokeService, "scriptIdToHash");
        Cache<String, Serializable> compiledScriptsCache = getFieldValue(invokeService, "compiledScriptsCache");

        List<UUID> scriptsIds = new ArrayList<>();
        for (int i = 0; i < 110; i++) { // tbel.compiled_scripts_cache_size = 100
            String script = "return msg.temperature > " + i;
            UUID scriptId = evalScript(script);
            scriptsIds.add(scriptId);

            for (int j = 0; j < i; j++) {
                invokeScript(scriptId, "{ \"temperature\": 12 }"); // so that scriptsIds is ordered by number of invocations
            }
        }

        ConcurrentMap<String, Serializable> cache = compiledScriptsCache.asMap();

        for (int i = 0; i < 10; i++) { // iterating rarely used scripts
            UUID scriptId = scriptsIds.get(i);
            String scriptHash = scriptIdToHash.get(scriptId);
            assertThat(cache).doesNotContainKey(scriptHash);
        }
        for (int i = 10; i < 110; i++) {
            UUID scriptId = scriptsIds.get(i);
            String scriptHash = scriptIdToHash.get(scriptId);
            assertThat(cache).containsKey(scriptHash);
        }

        UUID scriptRemovedFromCache = scriptsIds.get(0);
        assertThat(compiledScriptsCache.getIfPresent(scriptIdToHash.get(scriptRemovedFromCache))).isNull();
        invokeScript(scriptRemovedFromCache, "{ \"temperature\": 12 }");
        assertThat(compiledScriptsCache.getIfPresent(scriptIdToHash.get(scriptRemovedFromCache))).isNotNull();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `assertThatScriptIsBlocked` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void assertThatScriptIsBlocked(UUID scriptId) {
        assertThatThrownBy(() -> {
            invokeScript(scriptId, "{}");
        }).hasMessageContaining("invocation is blocked due to maximum error");
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `evalScript` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private UUID evalScript(String script) throws ExecutionException, InterruptedException {
        return invokeService.eval(TenantId.SYS_TENANT_ID, ScriptType.RULE_NODE_SCRIPT, script, "msg", "metadata", "msgType").get();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `invokeScript` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private String invokeScript(UUID scriptId, String str) throws ExecutionException, InterruptedException {
        var msg = JacksonUtil.fromString(str, Map.class);
        return invokeService.invokeScript(TenantId.SYS_TENANT_ID, null, scriptId, msg, "{}", POST_TELEMETRY_REQUEST.name()).get().toString();
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`TbelInvokeServiceTest` 在 ThingsBoard Application 测试模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
