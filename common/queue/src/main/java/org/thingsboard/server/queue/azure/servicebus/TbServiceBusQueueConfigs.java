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
package org.thingsboard.server.queue.azure.servicebus;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.queue.util.PropertyUtils;

import javax.annotation.PostConstruct;
import java.util.Map;

/**
 * 中文说明：
 * 1. `TbServiceBusQueueConfigs` 是 ThingsBoard Common Queue 中围绕队列提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 它直接协作于构造参数、字段类型和公开方法涉及的对象。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
@Component
@ConditionalOnExpression("'${queue.type:null}'=='service-bus'")
public class TbServiceBusQueueConfigs {
    /**
     * `coreProperties` 字段，保存当前对象的对应属性。
     */
    @Value("${queue.service-bus.queue-properties.core:}")
    private String coreProperties;
    /**
     * 规则引擎，表示当前对象的对应属性。
     */
    @Value("${queue.service-bus.queue-properties.rule-engine:}")
    private String ruleEngineProperties;
    /**
     * 传输层，表示当前对象的对应属性。
     */
    @Value("${queue.service-bus.queue-properties.transport-api:}")
    private String transportApiProperties;
    /**
     * `notificationsProperties` 字段，保存当前对象的对应属性。
     */
    @Value("${queue.service-bus.queue-properties.notifications:}")
    private String notificationsProperties;
    /**
     * 执行器，负责处理对应任务或消息。
     */
    @Value("${queue.service-bus.queue-properties.js-executor:}")
    private String jsExecutorProperties;
    /**
     * `vcProperties` 字段，保存当前对象的对应属性。
     */
    @Value("${queue.service-bus.queue-properties.version-control:}")
    private String vcProperties;
    /**
     * `coreConfigs`映射关系，用于按键查找对应值。
     */
    @Getter
    private Map<String, String> coreConfigs;
    /**
     * 规则引擎映射关系，用于按键查找对应值。
     */
    @Getter
    private Map<String, String> ruleEngineConfigs;
    /**
     * 传输层映射关系，用于按键查找对应值。
     */
    @Getter
    private Map<String, String> transportApiConfigs;
    /**
     * `notificationsConfigs`映射关系，用于按键查找对应值。
     */
    @Getter
    private Map<String, String> notificationsConfigs;
    /**
     * 执行器映射关系，用于按键查找对应值。
     */
    @Getter
    private Map<String, String> jsExecutorConfigs;
    /**
     * `vcConfigs`映射关系，用于按键查找对应值。
     */
    @Getter
    private Map<String, String> vcConfigs;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    private void init() {
        coreConfigs = PropertyUtils.getProps(coreProperties);
        ruleEngineConfigs = PropertyUtils.getProps(ruleEngineProperties);
        transportApiConfigs = PropertyUtils.getProps(transportApiProperties);
        notificationsConfigs = PropertyUtils.getProps(notificationsProperties);
        jsExecutorConfigs = PropertyUtils.getProps(jsExecutorProperties);
        vcConfigs = PropertyUtils.getProps(vcProperties);
    }

}
