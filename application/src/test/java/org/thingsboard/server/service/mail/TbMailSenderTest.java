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
package org.thingsboard.server.service.mail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willCallRealMethod;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

/**
 * 中文说明：
 * 1. 类目的：`TbMailSenderTest` 是ThingsBoard Application 测试模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
public class TbMailSenderTest {

    /**
     * `tbMailSender` 字段，保存当前对象的对应属性。
     */
    private TbMailSender tbMailSender;

    /**
     * 功能：初始化当前测试或组件需要的对象。
     * 参数：无。
     * 返回：无。
     */
    @BeforeEach
    void setUp() {
        tbMailSender = mock(TbMailSender.class);
    }

    /**
     * 功能：验证`Do Send Send Mail`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testDoSendSendMail() {
        MimeMessage mimeMsg = new MimeMessage(Session.getInstance(new Properties()));
        List<MimeMessage> mimeMessages = new ArrayList<>(1);
        mimeMessages.add(mimeMsg);

        willCallRealMethod().given(tbMailSender).doSend(any(), any());
        tbMailSender.doSend(mimeMessages.toArray(new MimeMessage[0]), null);

        Mockito.verify(tbMailSender, times(1)).updateOauth2PasswordIfExpired();
        Mockito.verify(tbMailSender, times(1)).doSendSuper(any(), any());
    }

    /**
     * 功能：验证`Test Connection`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testTestConnection() throws MessagingException {
        willCallRealMethod().given(tbMailSender).testConnection();
        tbMailSender.testConnection();

        Mockito.verify(tbMailSender, times(1)).updateOauth2PasswordIfExpired();
        Mockito.verify(tbMailSender, times(1)).testConnectionSuper();
    }

    /**
     * 功能：验证密码相关场景。
     * 参数：
     * - `oauth2`：`oauth2` 参数。
     * - `expiresIn`：`expiresIn` 参数。
     * - `passwordUpdateNeeded`：`passwordUpdateNeeded` 参数。
     * 返回：无。
     */
    @ParameterizedTest
    @MethodSource("provideSenderConfiguration")
    public void testUpdateOauth2PasswordIfExpiredIfOauth2Enabled(boolean oauth2, long expiresIn, boolean passwordUpdateNeeded) {
        willReturn(oauth2).given(tbMailSender).getOauth2Enabled();
        willReturn(expiresIn).given(tbMailSender).getTokenExpires();

        willCallRealMethod().given(tbMailSender).updateOauth2PasswordIfExpired();
        tbMailSender.updateOauth2PasswordIfExpired();

        if (passwordUpdateNeeded) {
            Mockito.verify(tbMailSender, times(1)).refreshAccessToken();
            Mockito.verify(tbMailSender, times(1)).setPassword(any());
        } else {
            Mockito.verify(tbMailSender, Mockito.never()).refreshAccessToken();
            Mockito.verify(tbMailSender, Mockito.never()).setPassword(any());
        }
    }

    /**
     * 功能：执行 `provideSenderConfiguration` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    private static Stream<Arguments> provideSenderConfiguration() {
        return Stream.of(
                Arguments.of(true, 0L, true),
                Arguments.of(true, System.currentTimeMillis() + 5000, false),
                Arguments.of(false, 0L, false),
                Arguments.of(false, System.currentTimeMillis() + 5000, false)
        );
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`TbMailSenderTest` 在 ThingsBoard Application 测试模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
