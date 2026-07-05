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
 * `TbMsgToEmailNodeTest` 测试类，用于验证 `TbMsgToEmailNode` 相关行为。
 */
public class TbMsgToEmailNodeTest {

    /**
     * `EXPECTED_TEMPERATURE`常量，用于统一引用固定值。
     */
    private static final int EXPECTED_TEMPERATURE = 30;
    /**
     * 设备常量，用于统一引用固定值。
     */
    private static final String EXPECTED_DEVICE_NAME = "TH-001";
    /**
     * 设备常量，用于统一引用固定值。
     */
    private static final String EXPECTED_DEVICE_TYPE = "thermostat";
    /**
     * `EXPECTED_SUBJECT`常量，用于统一引用固定值。
     */
    private static final String EXPECTED_SUBJECT = "Device " + EXPECTED_DEVICE_TYPE + " temperature high";
    /**
     * `EXPECTED_BODY`常量，用于统一引用固定值。
     */
    private static final String EXPECTED_BODY = "Device " + EXPECTED_DEVICE_NAME + " has high temperature " + EXPECTED_TEMPERATURE;
    /**
     * 邮箱常量，用于统一引用固定值。
     */
    private static final String EXPECTED_TO_EMAIL = "user@email.io";
    /**
     * 类型常量，用于统一引用固定值。
     */
    private static final String DYNAMIC_MAIL_BODY_TYPE = "dynamic";

    /**
     * `originator` 字段，保存当前对象的对应属性。
     */
    private EntityId originator;
    /**
     * 节点实例，表示当前对象的对应属性。
     */
    private TbMsgToEmailNode node;
    /**
     * 配置，保存当前对象的配置选项。
     */
    private TbMsgToEmailNodeConfiguration config;

    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    private TbContext ctxMock;

    /**
     * 功能：初始化当前测试或组件需要的对象。
     * 参数：无。
     * 返回：无。
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
     * 功能：执行 `tearDown` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @AfterEach
    void tearDown() {
        node.destroy();
    }

    /**
     * 功能：验证 `givenDefaultConfig_whenVerify_thenOK` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenDefaultConfig_whenVerify_thenOK() {
        // GIVEN-WHEN-THEN
        assertThat(config.getFromTemplate()).isEqualTo("info@testmail.org");
        assertThat(config.getToTemplate()).isEqualTo("${userEmail}");
        assertThat(config.getSubjectTemplate()).isEqualTo("Device ${deviceType} temperature high");
        assertThat(config.getBodyTemplate()).isEqualTo("Device ${deviceName} has high temperature $[temperature]");
    }

    /**
     * 功能：验证 `givenMailBodyTypeTestConfig_whenOnMsg_thenVerify` 描述的测试场景。
     * 参数：
     * - `testConfig`：配置对象。
     * 返回：无。
     */
    @ParameterizedTest
    @MethodSource("MailBodyTypeTestConfig")
    public void givenMailBodyTypeTestConfig_whenOnMsg_thenVerify(MailBodyTypeTestConfig testConfig) throws TbNodeException {
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
     * 功能：获取邮箱。
     * 参数：
     * - `html`：`html` 参数。
     * 返回：处理结果。
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

    /**
     * 配置，保存当前对象的配置选项。
     */
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
     * `MailBodyTypeTestConfig` 类，封装当前模块中的一组相关职责。
     */
    @Data
    @RequiredArgsConstructor
    static class MailBodyTypeTestConfig {
        /**
         * 是否满足值条件。
         */
        private final boolean expectedHtmlValue;
        /**
         * 类型，用于区分不同处理分支。
         */
        private final String mailBodyType;
        /**
         * 是否为值。
         */
        private final String isHtmlTemplateMdValue;
    }

}
/*
 * 本类总结：{@code TbMsgToEmailNodeTest} 为 {@code TbMsgToEmailNode} 的 邮件转换节点 测试提供中文注释，说明测试目标、fixture 生命周期、Mock 来源和断言流程。
 * 本文件中的数据库、缓存、MQTT、Actor 或完整 Rule Engine 运行时均不由测试本身直接启动；相关行为通过 Mock、内存 fixture 或被测生产逻辑间接覆盖。
 */
