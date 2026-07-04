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
package org.thingsboard.rule.engine.action;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.script.ScriptLanguage;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * 测试目标：验证 {@code TbLogNodeTest} 覆盖的 动作节点 行为，重点说明配置、消息和断言路径。
 * 所属生产节点/组件：{@code TbLogNode}，用于守护对应 Rule Engine 组件的兼容性和边界条件。
 * Mock 依赖来源：字段上的 Mockito 注解、Mockito.mock/spy、setUp/before/init 中的 stub 和内存 fixture；测试不启动真实外部服务。
 * 被验证流程：准备 fixture，初始化节点或工具对象，触发被测调用，再断言输出、异常或 Mock 交互。
 * 存在原因：防止规则引擎组件在升级、消息处理、异步回调或数据映射场景中发生回归。
 */
@Slf4j
public class TbLogNodeTest {

    /**
     * 测试方法：覆盖 {@code givenMsg_whenToLog_thenReturnString} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    void givenMsg_whenToLog_thenReturnString() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        TbLogNode node = new TbLogNode();
        String data = "{\"key\": \"value\"}";
        TbMsgMetaData metaData = new TbMsgMetaData(Map.of("mdKey1", "mdValue1", "mdKey2", "23"));
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, TenantId.SYS_TENANT_ID, metaData, data);

        String logMessage = node.toLogMessage(msg);
        log.info(logMessage);

        assertThat(logMessage).isEqualTo("\n" +
                "Incoming message:\n" +
                "{\"key\": \"value\"}\n" +
                "Incoming metadata:\n" +
                "{\"mdKey1\":\"mdValue1\",\"mdKey2\":\"23\"}");
    }

    /**
     * 测试方法：覆盖 {@code givenEmptyDataMsg_whenToLog_thenReturnString} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    void givenEmptyDataMsg_whenToLog_thenReturnString() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        TbLogNode node = new TbLogNode();
        TbMsgMetaData metaData = new TbMsgMetaData(Collections.emptyMap());
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, TenantId.SYS_TENANT_ID, metaData, "");

        String logMessage = node.toLogMessage(msg);
        log.info(logMessage);

        assertThat(logMessage).isEqualTo("\n" +
                "Incoming message:\n" +
                "\n" +
                "Incoming metadata:\n" +
                "{}");
    }

    /**
     * 测试方法：覆盖 {@code givenNullDataMsg_whenToLog_thenReturnString} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    void givenNullDataMsg_whenToLog_thenReturnString() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        TbLogNode node = new TbLogNode();
        TbMsgMetaData metaData = new TbMsgMetaData(Collections.emptyMap());
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, TenantId.SYS_TENANT_ID, metaData, null);

        String logMessage = node.toLogMessage(msg);
        log.info(logMessage);

        assertThat(logMessage).isEqualTo("\n" +
                "Incoming message:\n" +
                "null\n" +
                "Incoming metadata:\n" +
                "{}");
    }

    /**
     * 测试方法：覆盖 {@code givenDefaultConfig_whenIsStandardForEachScriptLanguage_thenTrue} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @ParameterizedTest
    @EnumSource(ScriptLanguage.class)
    void givenDefaultConfig_whenIsStandardForEachScriptLanguage_thenTrue(ScriptLanguage scriptLanguage) throws TbNodeException {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。

        TbLogNodeConfiguration config = new TbLogNodeConfiguration().defaultConfiguration();
        config.setScriptLang(scriptLanguage);
        TbLogNode node = spy(new TbLogNode());
        TbNodeConfiguration tbNodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        TbContext ctx = mock(TbContext.class);
        node.init(ctx, tbNodeConfiguration);

        assertThat(node.isStandard(config)).as("Script is standard for language " + scriptLanguage).isTrue();
        verify(node, never()).createScriptEngine(any(), any());
        verify(ctx, never()).createScriptEngine(any(), anyString());

    }

    /**
     * 测试方法：覆盖 {@code backwardCompatibility_whenScriptLangIsNull} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    void backwardCompatibility_whenScriptLangIsNull() throws TbNodeException {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        TbLogNodeConfiguration config = new TbLogNodeConfiguration().defaultConfiguration();
        TbLogNode node = spy(new TbLogNode());
        TbNodeConfiguration tbNodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        TbContext ctx = mock(TbContext.class);
        node.init(ctx, tbNodeConfiguration);

        assertThat(node.isStandard(config)).as("Script is standard for language JS").isTrue();
        verify(node, never()).createScriptEngine(any(), any());
        verify(ctx, never()).createScriptEngine(any(), anyString());
    }

    /**
     * 测试方法：覆盖 {@code givenScriptEngineEnum_whenNewAdded_thenFailed} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    void givenScriptEngineEnum_whenNewAdded_thenFailed() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        assertThat(ScriptLanguage.values().length).as("only two ScriptLanguage supported").isEqualTo(2);
    }

    /**
     * 测试方法：覆盖 {@code givenScriptEngineLangJs_whenCreateScriptEngine_thenSupplyJsScript} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    void givenScriptEngineLangJs_whenCreateScriptEngine_thenSupplyJsScript(){
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        TbLogNodeConfiguration configJs = new TbLogNodeConfiguration().defaultConfiguration();
        configJs.setScriptLang(ScriptLanguage.JS);
        configJs.setJsScript(configJs.getJsScript() + " // This is JS script " + UUID.randomUUID());
        TbLogNode node = new TbLogNode();
        TbContext ctx = mock(TbContext.class);
        node.createScriptEngine(ctx, configJs);
        verify(ctx).createScriptEngine(ScriptLanguage.JS, configJs.getJsScript());
        verifyNoMoreInteractions(ctx);
    }

    /**
     * 测试方法：覆盖 {@code givenScriptEngineLangTbel_whenCreateScriptEngine_thenSupplyTbelScript} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    void givenScriptEngineLangTbel_whenCreateScriptEngine_thenSupplyTbelScript(){
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        TbLogNodeConfiguration configTbel = new TbLogNodeConfiguration().defaultConfiguration();
        configTbel.setScriptLang(ScriptLanguage.TBEL);
        configTbel.setTbelScript(configTbel.getTbelScript() + " // This is TBEL script " + UUID.randomUUID());
        TbLogNode node = new TbLogNode();
        TbContext ctx = mock(TbContext.class);
        node.createScriptEngine(ctx, configTbel);
        verify(ctx).createScriptEngine(ScriptLanguage.TBEL, configTbel.getTbelScript());
        verifyNoMoreInteractions(ctx);
    }

}
/*
 * 本类总结：{@code TbLogNodeTest} 为 {@code TbLogNode} 的 动作节点 测试提供中文注释，说明测试目标、fixture 生命周期、Mock 来源和断言流程。
 * 本文件中的数据库、缓存、MQTT、Actor 或完整 Rule Engine 运行时均不由测试本身直接启动；相关行为通过 Mock、内存 fixture 或被测生产逻辑间接覆盖。
 */
