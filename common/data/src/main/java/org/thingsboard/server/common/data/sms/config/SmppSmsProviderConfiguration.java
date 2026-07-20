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
 * 1. `SmppSmsProviderConfiguration` 是 ThingsBoard Common Data 中描述 `Smpp Sms Provider` 行为的配置类型。
 * 2. 它集中保存该组件启动或运行时需要的可配置选项。
 * 3. 字段值决定功能开关、限制条件、地址或处理策略等具体行为。
 * 4. 直接依赖的类型边界包括 `SmsProviderConfiguration`。
 * 5. 独立配置对象可以避免大量零散参数在调用链中传递。
 * 6. 阅读时重点关注默认值、必填字段和配置项之间的约束关系。
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
     * 1. `SmppBindType` 是 ThingsBoard Common Data 中定义 `Smpp Bind Type` 固定取值的枚举类型。
     * 2. 它列出当前流程允许使用的有限状态、模式或类别。
     * 3. 枚举值可携带与该选项关联的标识、名称或处理参数。
     * 4. 它直接协作于使用该枚举进行分支判断或序列化的类型。
     * 5. 使用枚举可以限制非法取值，并让分支语义在源码中保持明确。
     * 6. 阅读时重点关注各枚举值含义、附加字段和反向查找方法。
     */
    public enum SmppBindType {
        TX, RX, TRX
    }

}
