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

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.smpp.Connection;
import org.smpp.Data;
import org.smpp.Session;
import org.smpp.TCPIPConnection;
import org.smpp.TimeoutException;
import org.smpp.WrongSessionStateException;
import org.smpp.pdu.Address;
import org.smpp.pdu.BindReceiver;
import org.smpp.pdu.BindRequest;
import org.smpp.pdu.BindResponse;
import org.smpp.pdu.BindTransciever;
import org.smpp.pdu.BindTransmitter;
import org.smpp.pdu.PDUException;
import org.smpp.pdu.SubmitSM;
import org.smpp.pdu.SubmitSMResp;
import org.thingsboard.rule.engine.api.sms.exception.SmsException;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.sms.config.SmppSmsProviderConfiguration;
import org.thingsboard.server.service.sms.AbstractSmsSender;

import java.io.IOException;
import java.util.Optional;

/**
 * 中文说明：
 * 1. 类目的：`SmppSmsSender` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
@Slf4j
public class SmppSmsSender extends AbstractSmsSender {
    /**
     * 配置，保存当前对象的配置选项。
     */
    protected SmppSmsProviderConfiguration config;

    /**
     * 会话，保存当前连接或交互过程的会话信息。
     */
    protected Session smppSession;

    /**
     * 功能：创建 `SmppSmsSender` 实例，并初始化必要字段。
     * 参数：
     * - `config`：配置对象。
     * 返回：新创建的对象实例。
     */
    public SmppSmsSender(SmppSmsProviderConfiguration config) {
        if (config.getBindType() == null) {
            config.setBindType(SmppSmsProviderConfiguration.SmppBindType.TX);
        }
        if (StringUtils.isNotEmpty(config.getSourceAddress())) {
            if (config.getSourceTon() == null) {
                config.setSourceTon((byte) 5);
            }
            if (config.getSourceNpi() == null) {
                config.setSourceNpi((byte) 0);
            }
        }
        if (config.getDestinationTon() == null) {
            config.setDestinationTon((byte) 5);
        }
        if (config.getDestinationNpi() == null) {
            config.setDestinationNpi((byte) 0);
        }

        this.config = config;
        this.smppSession = initSmppSession();
    }

    /**
     * 功能：创建 `SmppSmsSender` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    private SmppSmsSender() {} // for testing purposes


    /**
     * 功能：发送或提交`Sms`。
     * 参数：
     * - `numberTo`：`numberTo` 参数。
     * - `message`：待处理消息。
     * 返回：数值结果。
     */
    @Override
    public int sendSms(String numberTo, String message) throws SmsException {
        try {
            checkSmppSession();

            SubmitSM request = new SubmitSM();
            if (StringUtils.isNotEmpty(config.getServiceType())) {
                request.setServiceType(config.getServiceType());
            }
            if (StringUtils.isNotEmpty(config.getSourceAddress())) {
                request.setSourceAddr(new Address(config.getSourceTon(), config.getSourceNpi(), config.getSourceAddress()));
            }
            request.setDestAddr(new Address(config.getDestinationTon(), config.getDestinationNpi(), prepareNumber(numberTo)));
            request.setShortMessage(message);
            request.setDataCoding(Optional.ofNullable(config.getCodingScheme()).orElse((byte) 0));
            request.setReplaceIfPresentFlag((byte) 0);
            request.setEsmClass((byte) 0);
            request.setProtocolId((byte) 0);
            request.setPriorityFlag((byte) 0);
            request.setRegisteredDelivery((byte) 0);
            request.setSmDefaultMsgId((byte) 0);

            SubmitSMResp response = smppSession.submit(request);

            log.debug("SMPP submit command status: {}", response.getCommandStatus());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return countMessageSegments(message);
    }

    /**
     * 功能：校验会话。
     * 参数：无。
     * 返回：无。
     */
    private synchronized void checkSmppSession() {
        if (smppSession == null || !smppSession.isOpened()) {
            smppSession = initSmppSession();
        }
    }

    /**
     * 功能：初始化或启动会话。
     * 参数：无。
     * 返回：处理结果。
     */
    protected Session initSmppSession() {
        try {
            Connection connection = new TCPIPConnection(config.getHost(), config.getPort());
            Session session = new Session(connection);

            BindRequest bindRequest;
            switch (config.getBindType()) {
                case TX:
                    bindRequest = new BindTransmitter();
                    break;
                case RX:
                    bindRequest = new BindReceiver();
                    break;
                case TRX:
                    bindRequest = new BindTransciever();
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported bind type " + config.getBindType());
            }

            bindRequest.setSystemId(config.getSystemId());
            bindRequest.setPassword(config.getPassword());

            byte interfaceVersion;
            switch (config.getProtocolVersion()) {
                case "3.3":
                    interfaceVersion = Data.SMPP_V33;
                    break;
                case "3.4":
                    interfaceVersion = Data.SMPP_V34;
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported SMPP version: " + config.getProtocolVersion());
            }
            bindRequest.setInterfaceVersion(interfaceVersion);

            if (StringUtils.isNotEmpty(config.getSystemType())) {
                bindRequest.setSystemType(config.getSystemType());
            }
            if (StringUtils.isNotEmpty(config.getAddressRange())) {
                bindRequest.setAddressRange(config.getDestinationTon(), config.getDestinationNpi(), config.getAddressRange());
            }

            BindResponse bindResponse = session.bind(bindRequest);
            log.debug("SMPP bind response: {}", bindResponse.debugString());

            if (bindResponse.getCommandStatus() != 0) {
                throw new IllegalStateException("Error status when binding: " + bindResponse.getCommandStatus());
            }

            return session;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to establish SMPP session: " + ExceptionUtils.getRootCauseMessage(e), e);
        }
    }

    /**
     * 功能：执行 `prepareNumber` 对应的处理。
     * 参数：
     * - `number`：`number` 参数。
     * 返回：文本结果。
     */
    private String prepareNumber(String number) {
        if (config.getDestinationTon() == Data.GSM_TON_INTERNATIONAL) {
            return StringUtils.removeStart(number, "+");
        }
        return number;
    }

    /**
     * 功能：执行 `destroy` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void destroy() {
        try {
            smppSession.unbind();
            smppSession.close();
        } catch (TimeoutException | PDUException | IOException | WrongSessionStateException e) {
            throw new RuntimeException(e);
        }

    }
}

/*
 * 本类总结：
 * 1. 核心职责：`SmppSmsSender` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
