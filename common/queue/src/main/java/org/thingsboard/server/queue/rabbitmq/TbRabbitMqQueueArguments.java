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
package org.thingsboard.server.queue.rabbitmq;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 中文说明：
 * 1. `TbRabbitMqQueueArguments` 是 ThingsBoard Common Queue 中围绕队列提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 它直接协作于构造参数、字段类型和公开方法涉及的对象。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
@Component
@ConditionalOnExpression("'${queue.type:null}'=='rabbitmq'")
public class TbRabbitMqQueueArguments {
    /**
     * `coreProperties` 字段，保存当前对象的对应属性。
     */
    @Value("${queue.rabbitmq.queue-properties.core:}")
    private String coreProperties;
    /**
     * 规则引擎，表示当前对象的对应属性。
     */
    @Value("${queue.rabbitmq.queue-properties.rule-engine:}")
    private String ruleEngineProperties;
    /**
     * 传输层，表示当前对象的对应属性。
     */
    @Value("${queue.rabbitmq.queue-properties.transport-api:}")
    private String transportApiProperties;
    /**
     * `notificationsProperties` 字段，保存当前对象的对应属性。
     */
    @Value("${queue.rabbitmq.queue-properties.notifications:}")
    private String notificationsProperties;
    /**
     * 执行器，负责处理对应任务或消息。
     */
    @Value("${queue.rabbitmq.queue-properties.js-executor:}")
    private String jsExecutorProperties;
    /**
     * `vcProperties` 字段，保存当前对象的对应属性。
     */
    @Value("${queue.rabbitmq.queue-properties.version-control:}")
    private String vcProperties;

    /**
     * 参数映射关系，用于按键查找对应值。
     */
    @Getter
    private Map<String, Object> coreArgs;
    /**
     * 规则引擎映射关系，用于按键查找对应值。
     */
    @Getter
    private Map<String, Object> ruleEngineArgs;
    /**
     * 参数映射关系，用于按键查找对应值。
     */
    @Getter
    private Map<String, Object> transportApiArgs;
    /**
     * 参数映射关系，用于按键查找对应值。
     */
    @Getter
    private Map<String, Object> notificationsArgs;
    /**
     * 参数映射关系，用于按键查找对应值。
     */
    @Getter
    private Map<String, Object> jsExecutorArgs;
    /**
     * 参数映射关系，用于按键查找对应值。
     */
    @Getter
    private Map<String, Object> vcArgs;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    private void init() {
        coreArgs = getArgs(coreProperties);
        ruleEngineArgs = getArgs(ruleEngineProperties);
        transportApiArgs = getArgs(transportApiProperties);
        notificationsArgs = getArgs(notificationsProperties);
        jsExecutorArgs = getArgs(jsExecutorProperties);
        vcArgs = getArgs(vcProperties);
    }

    /**
     * 功能：获取参数。
     * 参数：
     * - `properties`：`properties` 参数。
     * 返回：处理结果。
     */
    public static Map<String, Object> getArgs(String properties) {
        Map<String, Object> configs = new HashMap<>();
        if (StringUtils.isNotEmpty(properties)) {
            for (String property : properties.split(";")) {
                int delimiterPosition = property.indexOf(":");
                String key = property.substring(0, delimiterPosition);
                String strValue = property.substring(delimiterPosition + 1);
                configs.put(key, getObjectValue(strValue));
            }
        }
        return configs;
    }

    /**
     * 功能：获取值。
     * 参数：
     * - `str`：`str` 参数。
     * 返回：处理结果。
     */
    private static Object getObjectValue(String str) {
        if (str.equalsIgnoreCase("true") || str.equalsIgnoreCase("false")) {
            return Boolean.valueOf(str);
        } else if (isNumeric(str)) {
            return getNumericValue(str);
        }
        return str;
    }

    /**
     * 功能：获取值。
     * 参数：
     * - `str`：`str` 参数。
     * 返回：处理结果。
     */
    private static Object getNumericValue(String str) {
        if (str.contains(".")) {
            return Double.valueOf(str);
        } else {
            return Long.valueOf(str);
        }
    }

    private static final Pattern PATTERN = Pattern.compile("-?\\d+(\\.\\d+)?");

    /**
     * 功能：判断`Numeric`。
     * 参数：
     * - `strNum`：`strNum` 参数。
     * 返回：判断结果。
     */
    private static boolean isNumeric(String strNum) {
        if (strNum == null) {
            return false;
        }
        return PATTERN.matcher(strNum).matches();
    }
}
