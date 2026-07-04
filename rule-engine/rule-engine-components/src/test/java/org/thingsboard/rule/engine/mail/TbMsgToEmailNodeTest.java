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
package org.thingsboard.rule.engine.mail;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbEmail;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 测试目标：验证 {@code TbMsgToEmailNodeTest} 覆盖的 邮件转换节点 行为，重点说明配置、消息和断言路径。
 * 所属生产节点/组件：{@code TbMsgToEmailNode}，用于守护对应 Rule Engine 组件的兼容性和边界条件。
 * Mock 依赖来源：字段上的 Mockito 注解、Mockito.mock/spy、setUp/before/init 中的 stub 和内存 fixture；测试不启动真实外部服务。
 * 被验证流程：准备 fixture，初始化节点或工具对象，触发被测调用，再断言输出、异常或 Mock 交互。
 * 存在原因：防止规则引擎组件在升级、消息处理、异步回调或数据映射场景中发生回归。
 */
public class TbMsgToEmailNodeTest {

    /** 测试常量字段：{@code EXPECTED_TEMPERATURE} 保存 {@code int} 测试数据或依赖，来源：由类加载时构造，生命周期覆盖整个测试类执行过程。 */
    private static final int EXPECTED_TEMPERATURE = 30;
    /** 测试常量字段：{@code EXPECTED_DEVICE_NAME} 保存 {@code String} 测试数据或依赖，来源：由类加载时构造，生命周期覆盖整个测试类执行过程。 */
    private static final String EXPECTED_DEVICE_NAME = "TH-001";
    /** 测试常量字段：{@code EXPECTED_DEVICE_TYPE} 保存 {@code String} 测试数据或依赖，来源：由类加载时构造，生命周期覆盖整个测试类执行过程。 */
    private static final String EXPECTED_DEVICE_TYPE = "thermostat";
    /** 测试常量字段：{@code EXPECTED_SUBJECT} 保存 {@code String} 测试数据或依赖，来源：由类加载时构造，生命周期覆盖整个测试类执行过程。 */
    private static final String EXPECTED_SUBJECT = "Device " + EXPECTED_DEVICE_TYPE + " temperature high";
    /** 测试常量字段：{@code EXPECTED_BODY} 保存 {@code String} 测试数据或依赖，来源：由类加载时构造，生命周期覆盖整个测试类执行过程。 */
    private static final String EXPECTED_BODY = "Device " + EXPECTED_DEVICE_NAME + " has high temperature " + EXPECTED_TEMPERATURE;
    /** 测试常量字段：{@code EXPECTED_TO_EMAIL} 保存 {@code String} 测试数据或依赖，来源：由类加载时构造，生命周期覆盖整个测试类执行过程。 */
    private static final String EXPECTED_TO_EMAIL = "user@email.io";
    /** 测试常量字段：{@code DYNAMIC_MAIL_BODY_TYPE} 保存 {@code String} 测试数据或依赖，来源：由类加载时构造，生命周期覆盖整个测试类执行过程。 */
    private static final String DYNAMIC_MAIL_BODY_TYPE = "dynamic";

    /** 可变 fixture 字段：{@code originator} 保存 {@code EntityId} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private EntityId originator;
    /** 可变 fixture 字段：{@code node} 保存 {@code TbMsgToEmailNode} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private TbMsgToEmailNode node;
    /** 可变 fixture 字段：{@code config} 保存 {@code TbMsgToEmailNodeConfiguration} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private TbMsgToEmailNodeConfiguration config;

    /** 可变 fixture 字段：{@code ctxMock} 保存 {@code TbContext} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private TbContext ctxMock;

    /**
     * 生命周期方法：{@code setUp} 在 JUnit 用例前后准备或清理测试环境。
     * 输入数据：来自 Mockito 注解、类字段和内存 fixture；输出影响是初始化节点、Mock、执行器或清理资源。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    @BeforeEach
    void setUp() throws TbNodeException {
        ctxMock = mock(TbContext.class);
        originator = new DeviceId(UUID.randomUUID());
        config = new TbMsgToEmailNodeConfiguration().defaultConfiguration();
        node = new TbMsgToEmailNode();
        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));
    }

    /**
     * 生命周期方法：{@code tearDown} 在 JUnit 用例前后准备或清理测试环境。
     * 输入数据：来自 Mockito 注解、类字段和内存 fixture；输出影响是初始化节点、Mock、执行器或清理资源。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    @AfterEach
    void tearDown() {
        node.destroy();
    }

    /**
     * 测试方法：覆盖 {@code givenDefaultConfig_whenVerify_thenOK} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    void givenDefaultConfig_whenVerify_thenOK() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN-WHEN-THEN
        assertThat(config.getFromTemplate()).isEqualTo("info@testmail.org");
        assertThat(config.getToTemplate()).isEqualTo("${userEmail}");
        assertThat(config.getSubjectTemplate()).isEqualTo("Device ${deviceType} temperature high");
        assertThat(config.getBodyTemplate()).isEqualTo("Device ${deviceName} has high temperature $[temperature]");
    }

    /**
     * 测试方法：覆盖 {@code givenMailBodyTypeTestConfig_whenOnMsg_thenVerify} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @ParameterizedTest
    @MethodSource("MailBodyTypeTestConfig")
    public void givenMailBodyTypeTestConfig_whenOnMsg_thenVerify(MailBodyTypeTestConfig testConfig) throws TbNodeException {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        String mailBodyType = testConfig.getMailBodyType();
        config.setMailBodyType(mailBodyType);
        if (DYNAMIC_MAIL_BODY_TYPE.equals(mailBodyType)) {
            config.setIsHtmlTemplate("${html}");
        }
        node.init(ctxMock, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        var md = new TbMsgMetaData();
        md.putValue("userEmail", EXPECTED_TO_EMAIL);
        md.putValue("deviceType", EXPECTED_DEVICE_TYPE);
        md.putValue("deviceName", EXPECTED_DEVICE_NAME);
        if (testConfig.getIsHtmlTemplateMdValue() != null) {
            md.putValue("html", testConfig.getIsHtmlTemplateMdValue());
        }

        var msgDataStr = "{\"temperature\": " + EXPECTED_TEMPERATURE + "}";
        TbMsg msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, originator, md, msgDataStr);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        var typeCaptor = ArgumentCaptor.forClass(TbMsgType.class);
        var originatorCaptor = ArgumentCaptor.forClass(EntityId.class);
        var metadataCaptor = ArgumentCaptor.forClass(TbMsgMetaData.class);
        var dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(ctxMock).transformMsg(
                msgCaptor.capture(),
                typeCaptor.capture(),
                originatorCaptor.capture(),
                metadataCaptor.capture(),
                dataCaptor.capture()
        );
        verify(ctxMock, never()).tellFailure(any(), any());

        Assertions.assertEquals(TbMsgType.SEND_EMAIL, typeCaptor.getValue());
        Assertions.assertEquals(originator, originatorCaptor.getValue());
        Assertions.assertNotSame(md, metadataCaptor.getValue());

        var actual = JacksonUtil.fromBytes(dataCaptor.getValue().getBytes(), TbEmail.class);
        var expected = getExpectedTbEmail(testConfig.isExpectedHtmlValue());

        Assertions.assertEquals(expected, actual);
    }

    /**
     * 辅助方法：{@code getExpectedTbEmail} 复用本类测试的 fixture 构造、Mock 配置或断言逻辑。
     * 输入数据：来自调用方参数、类字段和内存对象；输出影响由调用它的测试方法验证。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    private TbEmail getExpectedTbEmail(boolean html) {
        return TbEmail.builder()
                .from(config.getFromTemplate())
                .to(EXPECTED_TO_EMAIL)
                .subject(EXPECTED_SUBJECT)
                .body(EXPECTED_BODY)
                .html(html)
                .build();
    }

    /** 参数源方法：{@code MailBodyTypeTestConfig} 生成参数化测试输入组合，期望由消费它的测试方法断言。 */
    static Stream<MailBodyTypeTestConfig> MailBodyTypeTestConfig() {
        return Stream.of(
                new MailBodyTypeTestConfig(false, "false", null),
                new MailBodyTypeTestConfig(false, null, null),
                new MailBodyTypeTestConfig(false, DYNAMIC_MAIL_BODY_TYPE, "false"),
                new MailBodyTypeTestConfig(true, DYNAMIC_MAIL_BODY_TYPE, "true"),
                new MailBodyTypeTestConfig(true, "true", null)
        );
    }

    /**
     * 测试目标：验证 {@code MailBodyTypeTestConfig} 覆盖的 邮件转换节点 行为，重点说明配置、消息和断言路径。
     * 所属生产节点/组件：{@code MailBodyTypeTestConfig}，用于守护对应 Rule Engine 组件的兼容性和边界条件。
     * Mock 依赖来源：字段上的 Mockito 注解、Mockito.mock/spy、setUp/before/init 中的 stub 和内存 fixture；测试不启动真实外部服务。
     * 被验证流程：准备 fixture，初始化节点或工具对象，触发被测调用，再断言输出、异常或 Mock 交互。
     * 存在原因：防止规则引擎组件在升级、消息处理、异步回调或数据映射场景中发生回归。
     */
    @Data
    @RequiredArgsConstructor
    static class MailBodyTypeTestConfig {
        /** 固定 fixture 字段：{@code expectedHtmlValue} 保存 {@code boolean} 测试数据或依赖，来源：由测试实例构造时创建，生命周期随单个测试实例。 */
        private final boolean expectedHtmlValue;
        /** 固定 fixture 字段：{@code mailBodyType} 保存 {@code String} 测试数据或依赖，来源：由测试实例构造时创建，生命周期随单个测试实例。 */
        private final String mailBodyType;
        /** 固定 fixture 字段：{@code isHtmlTemplateMdValue} 保存 {@code String} 测试数据或依赖，来源：由测试实例构造时创建，生命周期随单个测试实例。 */
        private final String isHtmlTemplateMdValue;
    }

}
/*
 * 本类总结：{@code TbMsgToEmailNodeTest} 为 {@code TbMsgToEmailNode} 的 邮件转换节点 测试提供中文注释，说明测试目标、fixture 生命周期、Mock 来源和断言流程。
 * 本文件中的数据库、缓存、MQTT、Actor 或完整 Rule Engine 运行时均不由测试本身直接启动；相关行为通过 Mock、内存 fixture 或被测生产逻辑间接覆盖。
 */
