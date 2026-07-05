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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * 中文说明：`BasicCredentials` 是Basic凭据辅助类，用于描述客户端认证方式以及证书、Basic、匿名等凭据初始化资料。
 * 调用边界：本类本身不一定直接触发数据库、缓存、Rule Engine、Actor、MQTT 或事务；是否涉及取决于具体方法和调用链。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BasicCredentials implements ClientCredentials {
    /**
     * 用户名，用于认证或安全校验。
     */
    private String username;
    /**
     * 密码，用于认证或安全校验。
     */
    private String password;

    /**
     * 功能：获取类型。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public CredentialsType getType() {
        return CredentialsType.BASIC;
    }
    /*
     * 本类总结：`BasicCredentials` 负责描述客户端认证方式以及证书、Basic、匿名等凭据初始化资料；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
