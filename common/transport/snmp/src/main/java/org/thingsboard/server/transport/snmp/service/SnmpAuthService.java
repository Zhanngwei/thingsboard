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
package org.thingsboard.server.transport.snmp.service;

import lombok.RequiredArgsConstructor;
import org.snmp4j.AbstractTarget;
import org.snmp4j.CommunityTarget;
import org.snmp4j.Target;
import org.snmp4j.UserTarget;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.security.SecurityModel;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.security.USM;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.device.data.SnmpDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.SnmpDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.transport.snmp.SnmpProtocolVersion;
import org.thingsboard.server.queue.util.TbSnmpTransportComponent;
import org.thingsboard.server.transport.snmp.session.DeviceSessionContext;

import java.util.Optional;

@Service
@TbSnmpTransportComponent
@RequiredArgsConstructor
/**
 * 中文说明：
 * 1. 类目的：`SnmpAuthService` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
public class SnmpAuthService {
    /**
     * 字段说明：
     * 1. 保存 `snmpTransportService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final SnmpTransportService snmpTransportService;

    @Value("${transport.snmp.underlying_protocol}")
    /**
     * 字段说明：
     * 1. 保存 `snmpUnderlyingProtocol` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private String snmpUnderlyingProtocol;

    /**
     * 方法说明：
     * 1. 职责：执行 `setUpSnmpTarget` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Target setUpSnmpTarget(SnmpDeviceProfileTransportConfiguration profileTransportConfig, SnmpDeviceTransportConfiguration deviceTransportConfig) {
        AbstractTarget target;

        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        SnmpProtocolVersion protocolVersion = deviceTransportConfig.getProtocolVersion();
        // 根据枚举、状态或协议版本分支，保持不同业务路径的处理语义独立。
        switch (protocolVersion) {
            case V1:
                CommunityTarget communityTargetV1 = new CommunityTarget();
                communityTargetV1.setSecurityModel(SecurityModel.SECURITY_MODEL_SNMPv1);
                communityTargetV1.setSecurityLevel(SecurityLevel.NOAUTH_NOPRIV);
                // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
                communityTargetV1.setCommunity(new OctetString(deviceTransportConfig.getCommunity()));
                target = communityTargetV1;
                break;
            case V2C:
                CommunityTarget communityTargetV2 = new CommunityTarget();
                communityTargetV2.setSecurityModel(SecurityModel.SECURITY_MODEL_SNMPv2c);
                communityTargetV2.setSecurityLevel(SecurityLevel.NOAUTH_NOPRIV);
                // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
                communityTargetV2.setCommunity(new OctetString(deviceTransportConfig.getCommunity()));
                target = communityTargetV2;
                break;
            case V3:
                // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
                OctetString username = new OctetString(deviceTransportConfig.getUsername());
                // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
                OctetString securityName = new OctetString(deviceTransportConfig.getSecurityName());
                // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
                OctetString engineId = new OctetString(deviceTransportConfig.getEngineId());

                // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
                OID authenticationProtocol = new OID(deviceTransportConfig.getAuthenticationProtocol().getOid());
                // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
                OID privacyProtocol = new OID(deviceTransportConfig.getPrivacyProtocol().getOid());
                // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
                OctetString authenticationPassphrase = new OctetString(deviceTransportConfig.getAuthenticationPassphrase());
                authenticationPassphrase = new OctetString(SecurityProtocols.getInstance().passwordToKey(authenticationProtocol, authenticationPassphrase, engineId.getValue()));
                // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
                OctetString privacyPassphrase = new OctetString(deviceTransportConfig.getPrivacyPassphrase());
                privacyPassphrase = new OctetString(SecurityProtocols.getInstance().passwordToKey(privacyProtocol, authenticationProtocol, privacyPassphrase, engineId.getValue()));

                // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
                USM usm = snmpTransportService.getSnmp().getUSM();
                if (usm.hasUser(engineId, securityName)) {
                    usm.removeAllUsers(username, engineId);
                }
                usm.addLocalizedUser(
                        engineId.getValue(), username,
                        authenticationProtocol, authenticationPassphrase.getValue(),
                        privacyProtocol, privacyPassphrase.getValue()
                );

                UserTarget userTarget = new UserTarget();
                userTarget.setSecurityName(securityName);
                userTarget.setAuthoritativeEngineID(engineId.getValue());
                userTarget.setSecurityModel(SecurityModel.SECURITY_MODEL_USM);
                userTarget.setSecurityLevel(SecurityLevel.AUTH_PRIV);
                target = userTarget;
                break;
            default:
                throw new UnsupportedOperationException("SNMP protocol version " + protocolVersion + " is not supported");
        }

        Address address = GenericAddress.parse(snmpUnderlyingProtocol + ":" + deviceTransportConfig.getHost() + "/" + deviceTransportConfig.getPort());
        target.setAddress(Optional.ofNullable(address).orElseThrow(() -> new IllegalArgumentException("Address of the SNMP device is invalid")));
        target.setTimeout(profileTransportConfig.getTimeoutMs());
        target.setRetries(profileTransportConfig.getRetries());
        target.setVersion(protocolVersion.getCode());

        return target;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `cleanUpSnmpAuthInfo` 对应的公共基础设施类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void cleanUpSnmpAuthInfo(DeviceSessionContext sessionContext) {
        SnmpDeviceTransportConfiguration deviceTransportConfiguration = sessionContext.getDeviceTransportConfiguration();
        if (deviceTransportConfiguration.getProtocolVersion() == SnmpProtocolVersion.V3) {
            OctetString username = new OctetString(deviceTransportConfiguration.getUsername());
            OctetString engineId = new OctetString(deviceTransportConfiguration.getEngineId());
            snmpTransportService.getSnmp().getUSM().removeAllUsers(username, engineId);
        }
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`SnmpAuthService` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
