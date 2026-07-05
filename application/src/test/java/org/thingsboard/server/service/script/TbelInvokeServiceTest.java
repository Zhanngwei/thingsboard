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
@DaoSqlTest
@TestPropertySource(properties = {
        "tbel.max_script_body_size=100",
        "tbel.max_total_args_size=50",
        "tbel.max_result_size=50",
        "tbel.max_errors=2",
        "tbel.compiled_scripts_cache_size=100"
})
class TbelInvokeServiceTest extends AbstractControllerTest {

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    private TbelInvokeService invokeService;

    /**
     * `maxJsErrors` 字段，保存当前对象的对应属性。
     */
    @Value("${tbel.max_errors}")
    private int maxJsErrors;

    /**
     * 功能：验证 `givenSimpleScriptTestPerformance` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenSimpleScriptTestPerformance() throws ExecutionException, InterruptedException {
        int iterations = 100000;
        UUID scriptId = evalScript("return msg.temperature > 20");
        // warmup
        ObjectNode msg = JacksonUtil.newObjectNode();
        for (int i = 0; i < 100; i++) {
            msg.put("temperature", i);
            boolean expected = i > 20;
            boolean result = Boolean.valueOf(invokeScript(scriptId, JacksonUtil.toString(msg)));
            Assert.assertEquals(expected, result);
        }
        long startTs = System.currentTimeMillis();
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

    /**
     * 功能：验证 `givenTooBigScriptForEval_thenReturnError` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenTooBigScriptForEval_thenReturnError() {
        String hugeScript = "var a = 'qwertyqwertywertyqwabababerqwertyqwertywertyqwabababerqwertyqwertywertyqwabababerqwertyqwertywertyqwabababerqwertyqwertywertyqwabababer'; return {a: a};";

        assertThatThrownBy(() -> {
            evalScript(hugeScript);
        }).hasMessageContaining("body exceeds maximum allowed size");
    }

    /**
     * 功能：验证 `givenTooBigScriptInputArgs_thenReturnErrorAndReportScriptExecutionError` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenTooBigScriptInputArgs_thenReturnErrorAndReportScriptExecutionError() throws Exception {
        String script = "return { msg: msg };";
        String hugeMsg = "{\"input\":\"123456781234349\"}";
        UUID scriptId = evalScript(script);

        for (int i = 0; i < maxJsErrors; i++) {
            assertThatThrownBy(() -> {
                invokeScript(scriptId, hugeMsg);
            }).hasMessageContaining("input arguments exceed maximum");
        }
        assertThatScriptIsBlocked(scriptId);
    }

    /**
     * 功能：验证 `whenScriptInvocationResultIsTooBig_thenReturnErrorAndReportScriptExecutionError` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void whenScriptInvocationResultIsTooBig_thenReturnErrorAndReportScriptExecutionError() throws Exception {
        String script = "var s = 'a'; for(int i=0; i<50; i++){ s +='a';} return { s: s};";
        UUID scriptId = evalScript(script);

        for (int i = 0; i < maxJsErrors; i++) {
            assertThatThrownBy(() -> {
                invokeScript(scriptId, "{}");
            }).hasMessageContaining("result exceeds maximum allowed size");
        }
        assertThatScriptIsBlocked(scriptId);
    }

    /**
     * 功能：验证 `givenScriptsWithSameBody_thenCompileAndCacheOnlyOnce` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenScriptsWithSameBody_thenCompileAndCacheOnlyOnce() throws Exception {
        String script = "return msg.temperature > 20;";
        List<UUID> scriptsIds = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            UUID scriptId = evalScript(script);
            scriptsIds.add(scriptId);
        }

        Map<UUID, String> scriptIdToHash = getFieldValue(invokeService, "scriptIdToHash");
        Map<String, TbelScript> scriptMap = getFieldValue(invokeService, "scriptMap");
        Cache<String, Serializable> compiledScriptsCache = getFieldValue(invokeService, "compiledScriptsCache");

        String scriptHash = scriptIdToHash.get(scriptsIds.get(0));

        assertThat(scriptsIds.stream().map(scriptIdToHash::get)).containsOnly(scriptHash);
        assertThat(scriptMap).containsKey(scriptHash);
        assertThat(compiledScriptsCache.getIfPresent(scriptHash)).isNotNull();
    }

    /**
     * 功能：验证 `whenReleasingScript_thenCheckForScriptHashUsages` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void whenReleasingScript_thenCheckForScriptHashUsages() throws Exception {
        String script = "return msg.temperature > 20;";
        List<UUID> scriptsIds = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            UUID scriptId = evalScript(script);
            scriptsIds.add(scriptId);
        }

        Map<UUID, String> scriptIdToHash = getFieldValue(invokeService, "scriptIdToHash");
        Map<String, TbelScript> scriptMap = getFieldValue(invokeService, "scriptMap");
        Cache<String, Serializable> compiledScriptsCache = getFieldValue(invokeService, "compiledScriptsCache");

        String scriptHash = scriptIdToHash.get(scriptsIds.get(0));
        for (int i = 0; i < 9; i++) {
            UUID scriptId = scriptsIds.get(i);
            assertThat(scriptIdToHash).containsKey(scriptId);
            invokeService.release(scriptId);
            assertThat(scriptIdToHash).doesNotContainKey(scriptId);
        }
        assertThat(scriptMap).containsKey(scriptHash);
        assertThat(compiledScriptsCache.getIfPresent(scriptHash)).isNotNull();

        invokeService.release(scriptsIds.get(9));
        assertThat(scriptMap).doesNotContainKey(scriptHash);
        assertThat(compiledScriptsCache.getIfPresent(scriptHash)).isNull();
    }

    /**
     * 功能：验证 `whenCompiledScriptsCacheIsTooBig_thenRemoveRarelyUsedScripts` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    @Ignore("This test is based on assumption that Caffeine cache is LRU based but in fact it is based on " +
            "Tiny LFU which is the cause that the tests fail sometime: https://arxiv.org/pdf/1512.00727.pdf")
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
     * 功能：执行 `assertThatScriptIsBlocked` 对应的处理。
     * 参数：
     * - `scriptId`：`scriptId`ID。
     * 返回：无。
     */
    private void assertThatScriptIsBlocked(UUID scriptId) {
        assertThatThrownBy(() -> {
            invokeScript(scriptId, "{}");
        }).hasMessageContaining("invocation is blocked due to maximum error");
    }

    /**
     * 功能：执行 `evalScript` 对应的处理。
     * 参数：
     * - `script`：`script` 参数。
     * 返回：处理结果。
     */
    private UUID evalScript(String script) throws ExecutionException, InterruptedException {
        return invokeService.eval(TenantId.SYS_TENANT_ID, ScriptType.RULE_NODE_SCRIPT, script, "msg", "metadata", "msgType").get();
    }

    /**
     * 功能：执行 `invokeScript` 对应的处理。
     * 参数：
     * - `scriptId`：`scriptId`ID。
     * - `str`：`str` 参数。
     * 返回：文本结果。
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
