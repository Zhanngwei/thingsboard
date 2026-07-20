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
package org.thingsboard.server.transport.mqtt.util;

/**
 * 中文说明：
 * 1. `ReturnCode` 是 ThingsBoard Common Transport 中定义 `Return Code` 固定取值的枚举类型。
 * 2. 它列出当前流程允许使用的有限状态、模式或类别。
 * 3. 枚举值可携带与该选项关联的标识、名称或处理参数。
 * 4. 它直接协作于使用该枚举进行分支判断或序列化的类型。
 * 5. 使用枚举可以限制非法取值，并让分支语义在源码中保持明确。
 * 6. 阅读时重点关注各枚举值含义、附加字段和反向查找方法。
 */
public enum ReturnCode {
    SUCCESS((byte) 0x00),
    //MQTT 3 codes
    UNACCEPTABLE_PROTOCOL_VERSION((byte) 0X01),
    IDENTIFIER_REJECTED((byte) 0x02),
    SERVER_UNAVAILABLE((byte) 0x03),
    BAD_USER_NAME_OR_PASSWORD((byte) 0x04),
    NOT_AUTHORIZED((byte) 0x05),
    //MQTT 5 codes
    NO_MATCHING_SUBSCRIBERS((byte) 0x10),
    NO_SUBSCRIPTION_EXISTED((byte) 0x11),
    CONTINUE_AUTHENTICATION((byte) 0x18),
    REAUTHENTICATE((byte) 0x19),
    UNSPECIFIED_ERROR((byte) 0x80),
    MALFORMED_PACKET((byte) 0x81),
    PROTOCOL_ERROR((byte) 0x82),
    IMPLEMENTATION_SPECIFIC((byte) 0x83),
    UNSUPPORTED_PROTOCOL_VERSION((byte) 0x84),
    CLIENT_IDENTIFIER_NOT_VALID((byte) 0x85),
    BAD_USERNAME_OR_PASSWORD((byte) 0x86),
    NOT_AUTHORIZED_5((byte) 0x87),
    SERVER_UNAVAILABLE_5((byte) 0x88),
    SERVER_BUSY((byte) 0x89),
    BANNED((byte) 0x8A),
    SERVER_SHUTTING_DOWN((byte) 0x8B),
    BAD_AUTHENTICATION_METHOD((byte) 0x8C),
    KEEP_ALIVE_TIMEOUT((byte) 0x8D),
    SESSION_TAKEN_OVER((byte) 0x8E),
    TOPIC_FILTER_INVALID((byte) 0x8F),
    TOPIC_NAME_INVALID((byte) 0x90),
    PACKET_IDENTIFIER_IN_USE((byte) 0x91),
    PACKET_IDENTIFIER_NOT_FOUND((byte) 0x92),
    RECEIVE_MAXIMUM_EXCEEDED((byte) 0x93),
    TOPIC_ALIAS_INVALID((byte) 0x94),
    PACKET_TOO_LARGE((byte) 0x95),
    MESSAGE_RATE_TOO_HIGH((byte) 0x96),
    QUOTA_EXCEEDED((byte) 0x97),
    ADMINISTRATIVE_ACTION((byte) 0x98),
    PAYLOAD_FORMAT_INVALID((byte) 0x99),
    RETAIN_NOT_SUPPORTED((byte) 0x9A),
    QOS_NOT_SUPPORTED((byte) 0x9B),
    USE_ANOTHER_SERVER((byte) 0x9C),
    SERVER_MOVED((byte) 0x9D),
    SHARED_SUBSCRIPTION_NOT_SUPPORTED((byte) 0x9E),
    CONNECTION_RATE_EXCEEDED((byte) 0x9F),
    MAXIMUM_CONNECT_TIME((byte) 0xA0),
    SUBSCRIPTION_IDENTIFIERS_NOT_SUPPORTED((byte) 0xA1),
    WILDCARD_SUBSCRIPTION_NOT_SUPPORTED((byte) 0xA2);

    /**
     * `VALUES`常量，用于统一引用固定值。
     */
    private static final ReturnCode[] VALUES;

    static {
        ReturnCode[] values = values();
        VALUES = new ReturnCode[163];
        for (ReturnCode code : values) {
            final int unsignedByte = code.byteValue & 0xFF;
            // Suppress a warning about out of bounds access since the enum contains only correct values
            VALUES[unsignedByte] = code;    // lgtm [java/index-out-of-bounds]
        }
    }

    /**
     * 值，保存当前处理得到的具体内容。
     */
    private final byte byteValue;

    ReturnCode(byte byteValue) {
        this.byteValue = byteValue;
    }

    /**
     * 功能：执行 `byteValue` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public byte byteValue() {
        return byteValue;
    }

    /**
     * 功能：执行 `shortValue` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public short shortValue(){return byteValue;}

    /**
     * 功能：执行 `valueOf` 对应的处理。
     * 参数：
     * - `b`：`b` 参数。
     * 返回：处理结果。
     */
    public static ReturnCode valueOf(byte b) {
        final int unsignedByte = b & 0xFF;
        ReturnCode mqttConnectReturnCode = null;
        try {
            mqttConnectReturnCode = VALUES[unsignedByte];
        } catch (ArrayIndexOutOfBoundsException ignored) {
            // no op
        }
        if (mqttConnectReturnCode == null) {
            throw new IllegalArgumentException("unknown connect return code: " + unsignedByte);
        }
        return mqttConnectReturnCode;
    }
}