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
package org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MSecurityMode;

import java.io.Serializable;

/**
 * 中文说明：
 * 1. `LwM2MBootstrapServerCredential` 是 ThingsBoard Common Data 中定义 LwM2M 能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 直接依赖的类型边界包括 `Serializable`。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "securityMode")
@JsonSubTypes({
        @JsonSubTypes.Type(value = NoSecLwM2MBootstrapServerCredential.class, name = "NO_SEC"),
        @JsonSubTypes.Type(value = PSKLwM2MBootstrapServerCredential.class, name = "PSK"),
        @JsonSubTypes.Type(value = RPKLwM2MBootstrapServerCredential.class, name = "RPK"),
        @JsonSubTypes.Type(value = X509LwM2MBootstrapServerCredential.class, name = "X509")
})
@JsonIgnoreProperties(ignoreUnknown = true)
public interface LwM2MBootstrapServerCredential extends Serializable {
    /**
     * 功能：获取安全模式。
     * 参数：无。
     * 返回：处理结果。
     */
    @JsonIgnore
    LwM2MSecurityMode getSecurityMode();
}
