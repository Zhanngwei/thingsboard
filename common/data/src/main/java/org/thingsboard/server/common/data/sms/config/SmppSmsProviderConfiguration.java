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
package org.thingsboard.server.common.data.sms.config;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 中文说明：
 * 1. 类目的：`SmppSmsProviderConfiguration` 是ThingsBoard Common 模块中的公共数据模型类型，用于承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 生命周期：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Value Object / Builder。
 */
@Data
public class SmppSmsProviderConfiguration implements SmsProviderConfiguration {
    /**
     * 版本号，表示当前对象的对应属性。
     */
    @ApiModelProperty(value = "SMPP version", allowableValues = "3.3, 3.4", required = true)
    private String protocolVersion;

    /**
     * 主机地址，用于描述服务监听或访问地址。
     */
    @ApiModelProperty(value = "SMPP host", required = true)
    private String host;
    /**
     * 端口号，用于描述服务监听或访问地址。
     */
    @ApiModelProperty(value = "SMPP port", required = true)
    private Integer port;

    /**
     * Actor 系统ID，用于定位对应业务对象。
     */
    @ApiModelProperty(value = "System ID", required = true)
    private String systemId;
    /**
     * 密码，用于认证或安全校验。
     */
    @ApiModelProperty(value = "Password", required = true)
    private String password;

    /**
     * 类型，用于区分不同处理分支。
     */
    @ApiModelProperty(value = "System type", required = false)
    private String systemType;
    /**
     * 类型，用于区分不同处理分支。
     */
    @ApiModelProperty(value = "TX - Transmitter, RX - Receiver, TRX - Transciever. By default TX is used", required = false)
    private SmppBindType bindType;
    /**
     * 类型，提供当前类调用的业务操作。
     */
    @ApiModelProperty(value = "Service type", required = false)
    private String serviceType;

    /**
     * `sourceAddress` 字段，保存当前对象的对应属性。
     */
    @ApiModelProperty(value = "Source address", required = false)
    private String sourceAddress;
    /**
     * `sourceTon` 字段，保存当前对象的对应属性。
     */
    @ApiModelProperty(value = "Source TON (Type of Number). Needed is source address is set. 5 by default.\n" +
            "0 - Unknown\n" +
            "1 - International\n" +
            "2 - National\n" +
            "3 - Network Specific\n" +
            "4 - Subscriber Number\n" +
            "5 - Alphanumeric\n" +
            "6 - Abbreviated", required = false)
    private Byte sourceTon;
    /**
     * `sourceNpi` 字段，保存当前对象的对应属性。
     */
    @ApiModelProperty(value = "Source NPI (Numbering Plan Identification). Needed is source address is set. 0 by default.\n" +
            "0 - Unknown\n" +
            "1 - ISDN/telephone numbering plan (E163/E164)\n" +
            "3 - Data numbering plan (X.121)\n" +
            "4 - Telex numbering plan (F.69)\n" +
            "6 - Land Mobile (E.212) =6\n" +
            "8 - National numbering plan\n" +
            "9 - Private numbering plan\n" +
            "10 - ERMES numbering plan (ETSI DE/PS 3 01-3)\n" +
            "13 - Internet (IP)\n" +
            "18 - WAP Client Id (to be defined by WAP Forum)", required = false)
    private Byte sourceNpi;

    /**
     * `destinationTon` 字段，保存当前对象的对应属性。
     */
    @ApiModelProperty(value = "Destination TON (Type of Number). 5 by default.\n" +
            "0 - Unknown\n" +
            "1 - International\n" +
            "2 - National\n" +
            "3 - Network Specific\n" +
            "4 - Subscriber Number\n" +
            "5 - Alphanumeric\n" +
            "6 - Abbreviated", required = false)
    private Byte destinationTon;
    /**
     * `destinationNpi` 字段，保存当前对象的对应属性。
     */
    @ApiModelProperty(value = "Destination NPI (Numbering Plan Identification). 0 by default.\n" +
            "0 - Unknown\n" +
            "1 - ISDN/telephone numbering plan (E163/E164)\n" +
            "3 - Data numbering plan (X.121)\n" +
            "4 - Telex numbering plan (F.69)\n" +
            "6 - Land Mobile (E.212) =6\n" +
            "8 - National numbering plan\n" +
            "9 - Private numbering plan\n" +
            "10 - ERMES numbering plan (ETSI DE/PS 3 01-3)\n" +
            "13 - Internet (IP)\n" +
            "18 - WAP Client Id (to be defined by WAP Forum)", required = false)
    private Byte destinationNpi;

    /**
     * `addressRange` 字段，保存当前对象的对应属性。
     */
    @ApiModelProperty(value = "Address range", required = false)
    private String addressRange;

    /**
     * `codingScheme` 字段，保存当前对象的对应属性。
     */
    @ApiModelProperty(allowableValues = "0-10,13-14",
            value = "0 - SMSC Default Alphabet (ASCII for short and long code and to GSM for toll-free, used as default)\n" +
                    "1 - IA5 (ASCII for short and long code, Latin 9 for toll-free (ISO-8859-9))\n" +
                    "2 - Octet Unspecified (8-bit binary)\n" +
                    "3 - Latin 1 (ISO-8859-1)\n" +
                    "4 - Octet Unspecified (8-bit binary)\n" +
                    "5 - JIS (X 0208-1990)\n" +
                    "6 - Cyrillic (ISO-8859-5)\n" +
                    "7 - Latin/Hebrew (ISO-8859-8)\n" +
                    "8 - UCS2/UTF-16 (ISO/IEC-10646)\n" +
                    "9 - Pictogram Encoding\n" +
                    "10 - Music Codes (ISO-2022-JP)\n" +
                    "13 - Extended Kanji JIS (X 0212-1990)\n" +
                    "14 - Korean Graphic Character Set (KS C 5601/KS X 1001)", required = false)
    private Byte codingScheme;

    /**
     * 功能：获取类型。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public SmsProviderType getType() {
        return SmsProviderType.SMPP;
    }

    /**
     * 中文说明：
     * 1. 类目的：`SmppBindType` 是ThingsBoard Common 模块中的公共数据模型类型，用于承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
     * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
     * 3. 协作对象：主要协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
     * 4. 生命周期：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 DTO / Value Object / Builder。
     */
    public enum SmppBindType {
        TX, RX, TRX
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`SmppSmsProviderConfiguration` 在 ThingsBoard Common 模块 中承担公共数据模型类型职责，核心目的是承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 核心流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
 * 3. 关键依赖：主要依赖或协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
