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
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thingsboard.script.api.ScriptType;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.stats.TbApiUsageReportClient;
import org.thingsboard.server.common.stats.TbApiUsageStateClient;
import org.thingsboard.server.gen.js.JsInvokeProtos;
import org.thingsboard.server.gen.js.JsInvokeProtos.RemoteJsRequest;
import org.thingsboard.server.gen.js.JsInvokeProtos.RemoteJsResponse;
import org.thingsboard.server.queue.TbQueueRequestTemplate;
import org.thingsboard.server.queue.common.TbProtoJsQueueMsg;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 中文说明：
 * 1. `RemoteJsInvokeServiceTest` 是 ThingsBoard Application 中验证 `RemoteJsInvokeService` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 它直接协作于被测类型、测试框架和必要的模拟依赖。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
class RemoteJsInvokeServiceTest {

    /**
     * 服务，提供当前类调用的业务操作。
     */
    private RemoteJsInvokeService remoteJsInvokeService;
    private TbQueueRequestTemplate<TbProtoJsQueueMsg<RemoteJsRequest>, TbProtoQueueMsg<RemoteJsResponse>> jsRequestTemplate;


    /**
     * 功能：执行 `beforeEach` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @BeforeEach
    public void beforeEach() {
        TbApiUsageStateClient apiUsageStateClient = mock(TbApiUsageStateClient.class);
        ApiUsageState apiUsageState = mock(ApiUsageState.class);
        when(apiUsageState.isJsExecEnabled()).thenReturn(true);
        when(apiUsageStateClient.getApiUsageState(any())).thenReturn(apiUsageState);
        TbApiUsageReportClient apiUsageReportClient = mock(TbApiUsageReportClient.class);

        remoteJsInvokeService = new RemoteJsInvokeService(Optional.of(apiUsageStateClient), Optional.of(apiUsageReportClient));
        jsRequestTemplate = mock(TbQueueRequestTemplate.class);
        remoteJsInvokeService.requestTemplate = jsRequestTemplate;
    }

    /**
     * 功能：执行 `afterEach` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @AfterEach
    public void afterEach() {
        reset(jsRequestTemplate);
    }

    /**
     * 功能：验证 `whenInvokingFunction_thenDoNotSendScriptBody` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void whenInvokingFunction_thenDoNotSendScriptBody() throws Exception {
        mockJsEvalResponse();
        String scriptBody = "return { a: 'b'};";
        UUID scriptId = remoteJsInvokeService.eval(TenantId.SYS_TENANT_ID, ScriptType.RULE_NODE_SCRIPT, scriptBody).get();
        reset(jsRequestTemplate);

        String expectedInvocationResult = "scriptInvocationResult";
        doReturn(Futures.immediateFuture(new TbProtoJsQueueMsg<>(UUID.randomUUID(), RemoteJsResponse.newBuilder()
                .setInvokeResponse(JsInvokeProtos.JsInvokeResponse.newBuilder()
                        .setSuccess(true)
                        .setResult(expectedInvocationResult)
                        .build())
                .build())))
                .when(jsRequestTemplate).send(any());

        ArgumentCaptor<TbProtoJsQueueMsg<RemoteJsRequest>> jsRequestCaptor = ArgumentCaptor.forClass(TbProtoJsQueueMsg.class);
        Object invocationResult = remoteJsInvokeService.invokeScript(TenantId.SYS_TENANT_ID, null, scriptId, "{}").get();
        verify(jsRequestTemplate).send(jsRequestCaptor.capture());

        JsInvokeProtos.JsInvokeRequest jsInvokeRequestMade = jsRequestCaptor.getValue().getValue().getInvokeRequest();
        assertThat(jsInvokeRequestMade.getScriptBody()).isNullOrEmpty();
        assertThat(jsInvokeRequestMade.getScriptHash()).isEqualTo(getScriptHash(scriptId));
        assertThat(invocationResult).isEqualTo(expectedInvocationResult);
    }

    /**
     * 功能：验证 `whenInvokingFunctionAndRemoteJsExecutorRemovedScript_thenHandleNotFoundErrorAndMakeInvokeRequestWithScriptBody` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void whenInvokingFunctionAndRemoteJsExecutorRemovedScript_thenHandleNotFoundErrorAndMakeInvokeRequestWithScriptBody() throws Exception {
        mockJsEvalResponse();
        String scriptBody = "return { a: 'b'};";
        UUID scriptId = remoteJsInvokeService.eval(TenantId.SYS_TENANT_ID, ScriptType.RULE_NODE_SCRIPT, scriptBody).get();
        reset(jsRequestTemplate);

        doReturn(Futures.immediateFuture(new TbProtoJsQueueMsg<>(UUID.randomUUID(), RemoteJsResponse.newBuilder()
                .setInvokeResponse(JsInvokeProtos.JsInvokeResponse.newBuilder()
                        .setSuccess(false)
                        .setErrorCode(JsInvokeProtos.JsInvokeErrorCode.NOT_FOUND_ERROR)
                        .build())
                .build())))
                .when(jsRequestTemplate).send(argThat(jsQueueMsg -> {
                    return StringUtils.isEmpty(jsQueueMsg.getValue().getInvokeRequest().getScriptBody());
                }));

        String expectedInvocationResult = "invocationResult";
        doReturn(Futures.immediateFuture(new TbProtoJsQueueMsg<>(UUID.randomUUID(), RemoteJsResponse.newBuilder()
                .setInvokeResponse(JsInvokeProtos.JsInvokeResponse.newBuilder()
                        .setSuccess(true)
                        .setResult(expectedInvocationResult)
                        .build())
                .build())))
                .when(jsRequestTemplate).send(argThat(jsQueueMsg -> {
                    return StringUtils.isNotEmpty(jsQueueMsg.getValue().getInvokeRequest().getScriptBody());
                }));

        ArgumentCaptor<TbProtoJsQueueMsg<RemoteJsRequest>> jsRequestsCaptor = ArgumentCaptor.forClass(TbProtoJsQueueMsg.class);
        Object invocationResult = remoteJsInvokeService.invokeScript(TenantId.SYS_TENANT_ID, null, scriptId, "{}").get();
        verify(jsRequestTemplate, times(2)).send(jsRequestsCaptor.capture());

        List<TbProtoJsQueueMsg<RemoteJsRequest>> jsInvokeRequestsMade = jsRequestsCaptor.getAllValues();

        JsInvokeProtos.JsInvokeRequest firstRequestMade = jsInvokeRequestsMade.get(0).getValue().getInvokeRequest();
        assertThat(firstRequestMade.getScriptBody()).isNullOrEmpty();

        JsInvokeProtos.JsInvokeRequest secondRequestMade = jsInvokeRequestsMade.get(1).getValue().getInvokeRequest();
        assertThat(secondRequestMade.getScriptBody()).contains(scriptBody);

        assertThat(jsInvokeRequestsMade.stream().map(TbProtoQueueMsg::getKey).distinct().count()).as("partition keys are same")
                .isOne();

        assertThat(invocationResult).isEqualTo(expectedInvocationResult);
    }

    /**
     * 功能：验证 `whenDoingEval_thenSaveScriptByHashOfTenantIdAndScriptBody` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void whenDoingEval_thenSaveScriptByHashOfTenantIdAndScriptBody() throws Exception {
        mockJsEvalResponse();

        TenantId tenantId1 = TenantId.fromUUID(UUID.randomUUID());
        String scriptBody1 = "var msg = { temp: 42, humidity: 77 };\n" +
                "var metadata = { data: 40 };\n" +
                "var msgType = \"POST_TELEMETRY_REQUEST\";\n" +
                "\n" +
                "return { msg: msg, metadata: metadata, msgType: msgType };";

        Set<String> scriptHashes = new HashSet<>();
        String tenant1Script1Hash = null;
        for (int i = 0; i < 3; i++) {
            UUID scriptUuid = remoteJsInvokeService.eval(tenantId1, ScriptType.RULE_NODE_SCRIPT, scriptBody1).get();
            tenant1Script1Hash = getScriptHash(scriptUuid);
            scriptHashes.add(tenant1Script1Hash);
        }
        assertThat(scriptHashes).as("Unique scripts ids").size().isOne();

        TenantId tenantId2 = TenantId.fromUUID(UUID.randomUUID());
        UUID scriptUuid = remoteJsInvokeService.eval(tenantId2, ScriptType.RULE_NODE_SCRIPT, scriptBody1).get();
        String tenant2Script1Id = getScriptHash(scriptUuid);
        assertThat(tenant2Script1Id).isNotEqualTo(tenant1Script1Hash);

        String scriptBody2 = scriptBody1 + ";;";
        scriptUuid = remoteJsInvokeService.eval(tenantId2, ScriptType.RULE_NODE_SCRIPT, scriptBody2).get();
        String tenant2Script2Id = getScriptHash(scriptUuid);
        assertThat(tenant2Script2Id).isNotEqualTo(tenant2Script1Id);
    }

    /**
     * 功能：验证 `whenReleasingScript_thenCheckForHashUsages` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void whenReleasingScript_thenCheckForHashUsages() throws Exception {
        mockJsEvalResponse();
        String scriptBody = "return { a: 'b'};";
        UUID scriptId1 = remoteJsInvokeService.eval(TenantId.SYS_TENANT_ID, ScriptType.RULE_NODE_SCRIPT, scriptBody).get();
        UUID scriptId2 = remoteJsInvokeService.eval(TenantId.SYS_TENANT_ID, ScriptType.RULE_NODE_SCRIPT, scriptBody).get();
        String scriptHash = getScriptHash(scriptId1);
        assertThat(scriptHash).isEqualTo(getScriptHash(scriptId2));
        reset(jsRequestTemplate);

        doReturn(Futures.immediateFuture(new TbProtoQueueMsg<>(UUID.randomUUID(), RemoteJsResponse.newBuilder()
                .setReleaseResponse(JsInvokeProtos.JsReleaseResponse.newBuilder()
                        .setSuccess(true)
                        .build())
                .build())))
                .when(jsRequestTemplate).send(any());

        remoteJsInvokeService.release(scriptId1).get();
        verifyNoInteractions(jsRequestTemplate);
        assertThat(remoteJsInvokeService.scriptHashToBodysMap).containsKey(scriptHash);

        remoteJsInvokeService.release(scriptId2).get();
        verify(jsRequestTemplate).send(any());
        assertThat(remoteJsInvokeService.scriptHashToBodysMap).isEmpty();
    }

    /**
     * 功能：获取`Script Hash`。
     * 参数：
     * - `scriptUuid`：`scriptUuid`ID。
     * 返回：文本结果。
     */
    private String getScriptHash(UUID scriptUuid) {
        return remoteJsInvokeService.getScriptHash(scriptUuid);
    }

    /**
     * 功能：执行 `mockJsEvalResponse` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    private void mockJsEvalResponse() {
        doAnswer(methodCall -> Futures.immediateFuture(new TbProtoJsQueueMsg<>(UUID.randomUUID(), RemoteJsResponse.newBuilder()
                .setCompileResponse(JsInvokeProtos.JsCompileResponse.newBuilder()
                        .setSuccess(true)
                        .setScriptHash(methodCall.<TbProtoQueueMsg<RemoteJsRequest>>getArgument(0).getValue().getCompileRequest().getScriptHash())
                        .build())
                .build())))
                .when(jsRequestTemplate).send(argThat(jsQueueMsg -> jsQueueMsg.getValue().hasCompileRequest()));
    }

}
