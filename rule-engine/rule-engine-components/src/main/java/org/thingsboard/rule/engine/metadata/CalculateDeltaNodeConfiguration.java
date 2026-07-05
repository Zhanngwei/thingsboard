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
 * 中文说明：`CalculateDeltaNodeConfiguration` 是计算增量节点配置对象，用于承载规则节点 JSON 中的配置项和默认值。
 * 配置来源：实例字段通常由前端规则节点配置 JSON 反序列化得到，`defaultConfiguration` 提供缺省配置。
 * 调用边界：本类本身不直接涉及数据库、缓存、MQTT、Actor 或事务；具体实现和调用链可能在使用这些配置的节点中涉及。
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

    /*
     * 本类总结：`CalculateDeltaNodeConfiguration` 负责读取、补充或映射消息元数据、实体字段、属性和遥测上下文信息；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
