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

@Slf4j
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
public class SmppSmsSender extends AbstractSmsSender {
    /**
     * 字段说明：
     * 1. 保存 `config` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    protected SmppSmsProviderConfiguration config;

    /**
     * 字段说明：
     * 1. 保存 `smppSession` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    protected Session smppSession;

    /**
     * 方法说明：
     * 1. 职责：执行 `SmppSmsSender` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public SmppSmsSender(SmppSmsProviderConfiguration config) {
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (config.getBindType() == null) {
            config.setBindType(SmppSmsProviderConfiguration.SmppBindType.TX);
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (StringUtils.isNotEmpty(config.getSourceAddress())) {
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (config.getSourceTon() == null) {
                config.setSourceTon((byte) 5);
            }
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (config.getSourceNpi() == null) {
                config.setSourceNpi((byte) 0);
            }
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (config.getDestinationTon() == null) {
            config.setDestinationTon((byte) 5);
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (config.getDestinationNpi() == null) {
            config.setDestinationNpi((byte) 0);
        }

        this.config = config;
        this.smppSession = initSmppSession();
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `SmppSmsSender` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private SmppSmsSender() {} // for testing purposes


    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `sendSms` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public int sendSms(String numberTo, String message) throws SmsException {
        try {
            checkSmppSession();

            SubmitSM request = new SubmitSM();
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
            if (StringUtils.isNotEmpty(config.getServiceType())) {
                request.setServiceType(config.getServiceType());
            }
            // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
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
        // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return countMessageSegments(message);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `checkSmppSession` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private synchronized void checkSmppSession() {
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (smppSession == null || !smppSession.isOpened()) {
            smppSession = initSmppSession();
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `initSmppSession` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    protected Session initSmppSession() {
        try {
            Connection connection = new TCPIPConnection(config.getHost(), config.getPort());
            Session session = new Session(connection);

            BindRequest bindRequest;
            // 根据枚举、状态或协议版本分支，保持不同业务路径的处理语义独立。
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
            // 根据枚举、状态或协议版本分支，保持不同业务路径的处理语义独立。
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
     * 方法说明：
     * 1. 职责：执行 `prepareNumber` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private String prepareNumber(String number) {
        if (config.getDestinationTon() == Data.GSM_TON_INTERNATIONAL) {
            return StringUtils.removeStart(number, "+");
        }
        return number;
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `destroy` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
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
