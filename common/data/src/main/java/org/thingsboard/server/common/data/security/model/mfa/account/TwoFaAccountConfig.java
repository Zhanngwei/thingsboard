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
package org.thingsboard.server.common.data.security.model.mfa.account;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import org.thingsboard.server.common.data.security.model.mfa.provider.TwoFaProviderType;

import java.io.Serializable;

/**
 * 中文说明：
 * 1. `TwoFaAccountConfig` 是 ThingsBoard Common Data 中描述 `Two Fa Account` 行为的配置类型。
 * 2. 它集中保存该组件启动或运行时需要的可配置选项。
 * 3. 字段值决定功能开关、限制条件、地址或处理策略等具体行为。
 * 4. 直接依赖的类型边界包括 `Serializable`。
 * 5. 独立配置对象可以避免大量零散参数在调用链中传递。
 * 6. 阅读时重点关注默认值、必填字段和配置项之间的约束关系。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "providerType")
@JsonSubTypes({
        @Type(name = "TOTP", value = TotpTwoFaAccountConfig.class),
        @Type(name = "SMS", value = SmsTwoFaAccountConfig.class),
        @Type(name = "EMAIL", value = EmailTwoFaAccountConfig.class),
        @Type(name = "BACKUP_CODE", value = BackupCodeTwoFaAccountConfig.class)
})
@Data
public abstract class TwoFaAccountConfig implements Serializable {

    /**
     * 当前对象是否为默认项。
     */
    private boolean useByDefault;

    /**
     * 是否满足`serializeHiddenFields`条件。
     */
    @JsonIgnore
    protected transient boolean serializeHiddenFields;

    /**
     * 功能：获取类型。
     * 参数：无。
     * 返回：处理结果。
     */
    @JsonIgnore
    public abstract TwoFaProviderType getProviderType();

}
