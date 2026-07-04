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
package org.thingsboard.rule.engine.credentials;

/**
 * 中文说明：`CredentialsType` 是凭据类型枚举，用于限定描述客户端认证方式以及证书、Basic、匿名等凭据初始化资料时可选择的固定值。
 * 调用边界：本枚举本身不直接涉及数据库、缓存、Rule Engine、Actor、MQTT 或事务，只作为配置或流程判断的类型值。
 */
public enum CredentialsType {
    /**
     * 枚举项说明：本行枚举常量定义 `CredentialsType` 支持的取值，用于配置或处理流程中的分支判断。
     */
    ANONYMOUS("anonymous"),
    /**
     * 枚举项说明：本行枚举常量定义 `CredentialsType` 支持的取值，用于配置或处理流程中的分支判断。
     */
    BASIC("basic"),
    /**
     * 枚举项说明：本行枚举常量定义 `CredentialsType` 支持的取值，用于配置或处理流程中的分支判断。
     */
    SAS("sas"),
    /**
     * 枚举项说明：本行枚举常量定义 `CredentialsType` 支持的取值，用于配置或处理流程中的分支判断。
     */
    CERT_PEM("cert.PEM");

    /**
     * 字段说明：保存 `label`，表示与本类处理流程相关的运行时值，供本类方法在规则节点处理流程中使用。
     */
    private final String label;

    /**
     * 方法说明：构造 `CredentialsType` 实例并初始化必要字段。
     * 调用边界：构造过程本身不直接参与 Rule Engine 消息投递，不直接发布 MQTT，也不直接开启事务。
     */
    CredentialsType(String label) {
        this.label = label;
    }
    /*
     * 本类总结：`CredentialsType` 负责描述客户端认证方式以及证书、Basic、匿名等凭据初始化资料；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
