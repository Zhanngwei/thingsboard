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
package org.thingsboard.server.service.sms.smpp;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.smpp.Session;
import org.smpp.pdu.SubmitSMResp;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.sms.config.SmppSmsProviderConfiguration;

import java.lang.reflect.Constructor;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 中文说明：
 * 1. 类目的：`SmppSmsSenderTest` 是ThingsBoard Application 测试模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
@RunWith(MockitoJUnitRunner.class)
public class SmppSmsSenderTest {

    /**
     * `smppSmsSender` 字段，保存当前对象的对应属性。
     */
    SmppSmsSender smppSmsSender;
    SmppSmsProviderConfiguration smppConfig;
    /**
     * 会话，保存当前连接或交互过程的会话信息。
     */
    Session smppSession;

    /**
     * 功能：执行 `beforeEach` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void beforeEach() throws Exception {
        Constructor<SmppSmsSender> constructor = SmppSmsSender.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        smppSmsSender = spy(constructor.newInstance());

        smppSession = mock(Session.class);
        smppSmsSender.smppSession = smppSession;

        smppConfig = new SmppSmsProviderConfiguration();
        smppSmsSender.config = smppConfig;
    }

    /**
     * 功能：验证`Send Sms`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSendSms() throws Exception {
        when(smppSession.isOpened()).thenReturn(true);
        when(smppSession.submit(any())).thenReturn(new SubmitSMResp());
        setDefaultSmppConfig();

        String number = "123545";
        String message = "message";
        smppSmsSender.sendSms(number, message);

        verify(smppSmsSender, never()).initSmppSession();
        verify(smppSession).submit(argThat(submitRequest -> {
            try {
                return submitRequest.getShortMessage().equals(message) &&
                        submitRequest.getDestAddr().getAddress().equals(number) &&
                        submitRequest.getServiceType().equals(smppConfig.getServiceType()) &&
                        (StringUtils.isEmpty(smppConfig.getSourceAddress()) ? submitRequest.getSourceAddr().getAddress().equals("")
                                : submitRequest.getSourceAddr().getAddress().equals(smppConfig.getSourceAddress()) &&
                                submitRequest.getSourceAddr().getTon() == smppConfig.getSourceTon() &&
                                submitRequest.getSourceAddr().getNpi() == smppConfig.getSourceNpi()) &&
                        submitRequest.getDestAddr().getTon() == smppConfig.getDestinationTon() &&
                        submitRequest.getDestAddr().getNpi() == smppConfig.getDestinationNpi() &&
                        submitRequest.getDataCoding() == smppConfig.getCodingScheme() &&
                        submitRequest.getReplaceIfPresentFlag() == 0 &&
                        submitRequest.getEsmClass() == 0 &&
                        submitRequest.getProtocolId() == 0 &&
                        submitRequest.getPriorityFlag() == 0 &&
                        submitRequest.getRegisteredDelivery() == 0 &&
                        submitRequest.getSmDefaultMsgId() == 0;
            } catch (Exception e) {
                fail(e.getMessage());
                return false;
            }
        }));
    }

    /**
     * 功能：更新配置。
     * 参数：无。
     * 返回：无。
     */
    private void setDefaultSmppConfig() {
        smppConfig.setProtocolVersion("3.3");
        smppConfig.setHost("smpphost");
        smppConfig.setPort(5687);
        smppConfig.setSystemId("213131");
        smppConfig.setPassword("35125q");

        smppConfig.setSystemType("");
        smppConfig.setBindType(SmppSmsProviderConfiguration.SmppBindType.TX);
        smppConfig.setServiceType("");

        smppConfig.setSourceAddress("");
        smppConfig.setSourceTon((byte) 5);
        smppConfig.setSourceNpi((byte) 0);

        smppConfig.setDestinationTon((byte) 5);
        smppConfig.setDestinationNpi((byte) 0);

        smppConfig.setAddressRange("");
        smppConfig.setCodingScheme((byte) 0);
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`SmppSmsSenderTest` 在 ThingsBoard Application 测试模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
