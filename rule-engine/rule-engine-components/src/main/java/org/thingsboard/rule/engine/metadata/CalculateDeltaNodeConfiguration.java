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
package org.thingsboard.rule.engine.metadata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;

/**
 * 中文说明：
 * 1. `CalculateDeltaNodeConfiguration` 是 ThingsBoard Rule Engine Components 中描述 `Calculate Delta Node` 行为的配置类型。
 * 2. 它集中保存该组件启动或运行时需要的可配置选项。
 * 3. 字段值决定功能开关、限制条件、地址或处理策略等具体行为。
 * 4. 直接依赖的类型边界包括 `NodeConfiguration`。
 * 5. 独立配置对象可以避免大量零散参数在调用链中传递。
 * 6. 阅读时重点关注默认值、必填字段和配置项之间的约束关系。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CalculateDeltaNodeConfiguration implements NodeConfiguration<CalculateDeltaNodeConfiguration> {

    /**
     * 键，用于定位映射、配置或数据项。
     */
    private String inputValueKey;
    /**
     * 键，用于定位映射、配置或数据项。
     */
    private String outputValueKey;
    /**
     * 是否使用`cache`。
     */
    private boolean useCache;
    /**
     * 是否满足`addPeriodBetweenMsgs`条件。
     */
    private boolean addPeriodBetweenMsgs;
    /**
     * 键，用于定位映射、配置或数据项。
     */
    private String periodValueKey;
    /**
     * `round` 字段，保存当前对象的对应属性。
     */
    private Integer round;
    /**
     * 是否满足失败信息条件。
     */
    private boolean tellFailureIfDeltaIsNegative;

    /**
     * 功能：执行 `defaultConfiguration` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public CalculateDeltaNodeConfiguration defaultConfiguration() {
        var configuration = new CalculateDeltaNodeConfiguration();
        configuration.setInputValueKey("pulseCounter");
        configuration.setOutputValueKey("delta");
        configuration.setUseCache(true);
        configuration.setAddPeriodBetweenMsgs(false);
        configuration.setPeriodValueKey("periodInMs");
        configuration.setTellFailureIfDeltaIsNegative(true);
        return configuration;
    }
}
