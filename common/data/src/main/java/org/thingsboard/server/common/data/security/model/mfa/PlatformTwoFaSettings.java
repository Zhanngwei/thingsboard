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
package org.thingsboard.server.common.data.security.model.mfa;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.thingsboard.server.common.data.security.model.mfa.provider.TwoFaProviderConfig;
import org.thingsboard.server.common.data.security.model.mfa.provider.TwoFaProviderType;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.List;
import java.util.Optional;

/**
 * 中文说明：
 * 1. `PlatformTwoFaSettings` 是 ThingsBoard Common Data 中描述 `Platform Two Fa` 行为的配置类型。
 * 2. 它集中保存该组件启动或运行时需要的可配置选项。
 * 3. 字段值决定功能开关、限制条件、地址或处理策略等具体行为。
 * 4. 它直接协作于配置加载组件和使用这些配置的运行类型。
 * 5. 独立配置对象可以避免大量零散参数在调用链中传递。
 * 6. 阅读时重点关注默认值、必填字段和配置项之间的约束关系。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformTwoFaSettings {

    /**
     * `providers`列表，用于保存一组待处理对象。
     */
    @Valid
    @NotNull
    private List<TwoFaProviderConfig> providers;

    /**
     * 编码，表示当前对象的对应属性。
     */
    @NotNull
    @Min(value = 5)
    private Integer minVerificationCodeSendPeriod;
    /**
     * 数量限制，用于控制数量、位置或分页范围。
     */
    @Pattern(regexp = "[1-9]\\d*:[1-9]\\d*", message = "is invalid")
    private String verificationCodeCheckRateLimit;
    /**
     * 用户对象，用于描述当前业务场景。
     */
    @Min(value = 0, message = "must be positive")
    private Integer maxVerificationFailuresBeforeUserLockout;
    /**
     * 时间，用于控制时间范围或等待时长。
     */
    @NotNull
    @Min(value = 60)
    private Integer totalAllowedTimeForVerification;


    /**
     * 功能：获取配置。
     * 参数：
     * - `providerType`：类型。
     * 返回：可能存在的结果。
     */
    public Optional<TwoFaProviderConfig> getProviderConfig(TwoFaProviderType providerType) {
        return Optional.ofNullable(providers)
                .flatMap(providersConfigs -> providersConfigs.stream()
                        .filter(providerConfig -> providerConfig.getProviderType() == providerType)
                        .findFirst());
    }

}
