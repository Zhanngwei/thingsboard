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
package org.thingsboard.server.common.data.device.profile.lwm2m;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.device.data.PowerMode;
import org.thingsboard.server.common.data.device.data.PowerSavingConfiguration;

/**
 * 中文说明：
 * 1. `OtherConfiguration` 是 ThingsBoard Common Data 中描述 `Other` 行为的配置类型。
 * 2. 它集中保存该组件启动或运行时需要的可配置选项。
 * 3. 字段值决定功能开关、限制条件、地址或处理策略等具体行为。
 * 4. 直接依赖的类型边界包括 `PowerSavingConfiguration`。
 * 5. 独立配置对象可以避免大量零散参数在调用链中传递。
 * 6. 阅读时重点关注默认值、必填字段和配置项之间的约束关系。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OtherConfiguration extends PowerSavingConfiguration {

    /**
     * 策略对象，封装可复用的处理规则。
     */
    private Integer fwUpdateStrategy;
    private Integer swUpdateStrategy;
    /**
     * 客户端，用于发起外部调用或协议交互。
     */
    private Integer clientOnlyObserveAfterConnect;
    private PowerMode powerMode;
    /**
     * 定时器，用于安排延迟任务或周期任务。
     */
    private Long psmActivityTimer;
    private Long edrxCycle;
    /**
     * `pagingTransmissionWindow` 字段，保存当前对象的对应属性。
     */
    private Long pagingTransmissionWindow;
    private String fwUpdateResource;
    /**
     * `swUpdateResource` 字段，保存当前对象的对应属性。
     */
    private String swUpdateResource;
}
