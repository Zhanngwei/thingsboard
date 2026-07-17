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
package org.thingsboard.server.queue.sqs;

import com.amazonaws.services.sqs.model.QueueAttributeName;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * 中文说明：
 * 1. `TbAwsSqsQueueAttributes` 是 ThingsBoard Common Queue 中围绕队列提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 它直接协作于构造参数、字段类型和公开方法涉及的对象。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
@Component
@ConditionalOnExpression("'${queue.type:null}'=='aws-sqs'")
public class TbAwsSqsQueueAttributes {
    /**
     * `coreProperties` 字段，保存当前对象的对应属性。
     */
    @Value("${queue.aws-sqs.queue-properties.core:}")
    private String coreProperties;
    /**
     * 规则引擎，表示当前对象的对应属性。
     */
    @Value("${queue.aws-sqs.queue-properties.rule-engine:}")
    private String ruleEngineProperties;
    /**
     * 传输层，表示当前对象的对应属性。
     */
    @Value("${queue.aws-sqs.queue-properties.transport-api:}")
    private String transportApiProperties;
    /**
     * `notificationsProperties` 字段，保存当前对象的对应属性。
     */
    @Value("${queue.aws-sqs.queue-properties.notifications:}")
    private String notificationsProperties;
    /**
     * 执行器，负责处理对应任务或消息。
     */
    @Value("${queue.aws-sqs.queue-properties.js-executor:}")
    private String jsExecutorProperties;
    /**
     * `otaProperties` 字段，保存当前对象的对应属性。
     */
    @Value("${queue.aws-sqs.queue-properties.ota-updates:}")
    private String otaProperties;
    /**
     * `vcProperties` 字段，保存当前对象的对应属性。
     */
    @Value("${queue.aws-sqs.queue-properties.version-control:}")
    private String vcProperties;

    /**
     * `coreAttributes`映射关系，用于按键查找对应值。
     */
    @Getter
    private Map<String, String> coreAttributes;
    /**
     * 规则引擎映射关系，用于按键查找对应值。
     */
    @Getter
    private Map<String, String> ruleEngineAttributes;
    /**
     * 传输层映射关系，用于按键查找对应值。
     */
    @Getter
    private Map<String, String> transportApiAttributes;
    /**
     * `notificationsAttributes`映射关系，用于按键查找对应值。
     */
    @Getter
    private Map<String, String> notificationsAttributes;
    /**
     * 执行器映射关系，用于按键查找对应值。
     */
    @Getter
    private Map<String, String> jsExecutorAttributes;
    /**
     * `otaAttributes`映射关系，用于按键查找对应值。
     */
    @Getter
    private Map<String, String> otaAttributes;
    /**
     * `vcAttributes`映射关系，用于按键查找对应值。
     */
    @Getter
    private Map<String, String> vcAttributes;

    private final Map<String, String> defaultAttributes = new HashMap<>();

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    private void init() {
        defaultAttributes.put(QueueAttributeName.FifoQueue.toString(), "true");

        coreAttributes = getConfigs(coreProperties);
        ruleEngineAttributes = getConfigs(ruleEngineProperties);
        transportApiAttributes = getConfigs(transportApiProperties);
        notificationsAttributes = getConfigs(notificationsProperties);
        jsExecutorAttributes = getConfigs(jsExecutorProperties);
        otaAttributes = getConfigs(otaProperties);
        vcAttributes = getConfigs(vcProperties);
    }

    /**
     * 功能：获取`Configs`。
     * 参数：
     * - `properties`：`properties` 参数。
     * 返回：处理结果。
     */
    private Map<String, String> getConfigs(String properties) {
        Map<String, String> configs = new HashMap<>(defaultAttributes);
       configs.putAll(toConfigs(properties));
        return configs;
    }

    /**
     * 功能：执行 `toConfigs` 对应的处理。
     * 参数：
     * - `properties`：`properties` 参数。
     * 返回：处理结果。
     */
    public static Map<String, String> toConfigs(String properties) {
        Map<String, String> configs = new HashMap<>();
        if (StringUtils.isNotEmpty(properties)) {
            for (String property : properties.split(";")) {
                int delimiterPosition = property.indexOf(":");
                String key = property.substring(0, delimiterPosition);
                String value = property.substring(delimiterPosition + 1);
                validateAttributeName(key);
                configs.put(key, value);
            }
        }
        return configs;
    }

    /**
     * 功能：校验属性。
     * 参数：
     * - `key`：键。
     * 返回：无。
     */
    private static void validateAttributeName(String key) {
        QueueAttributeName.fromValue(key);
    }
}
