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
package org.thingsboard.server.queue.pubsub;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.queue.util.PropertyUtils;

import javax.annotation.PostConstruct;
import java.util.Map;

/**
 * 中文说明：
 * 1. `TbPubSubSubscriptionSettings` 是 ThingsBoard Common Queue 中描述 `Tb Pub Sub` 行为的配置类型。
 * 2. 它集中保存该组件启动或运行时需要的可配置选项。
 * 3. 字段值决定功能开关、限制条件、地址或处理策略等具体行为。
 * 4. 它直接协作于配置加载组件和使用这些配置的运行类型。
 * 5. 独立配置对象可以避免大量零散参数在调用链中传递。
 * 6. 阅读时重点关注默认值、必填字段和配置项之间的约束关系。
 */
@Component
@ConditionalOnExpression("'${queue.type:null}'=='pubsub'")
public class TbPubSubSubscriptionSettings {
    /**
     * `coreProperties` 字段，保存当前对象的对应属性。
     */
    @Value("${queue.pubsub.queue-properties.core:}")
    private String coreProperties;
    /**
     * 规则引擎，表示当前对象的对应属性。
     */
    @Value("${queue.pubsub.queue-properties.rule-engine:}")
    private String ruleEngineProperties;
    /**
     * 传输层，表示当前对象的对应属性。
     */
    @Value("${queue.pubsub.queue-properties.transport-api:}")
    private String transportApiProperties;
    /**
     * `notificationsProperties` 字段，保存当前对象的对应属性。
     */
    @Value("${queue.pubsub.queue-properties.notifications:}")
    private String notificationsProperties;
    /**
     * 执行器，负责处理对应任务或消息。
     */
    @Value("${queue.pubsub.queue-properties.js-executor:}")
    private String jsExecutorProperties;
    /**
     * `vcProperties` 字段，保存当前对象的对应属性。
     */
    @Value("${queue.pubsub.queue-properties.version-control:}")
    private String vcProperties;

    /**
     * 配置映射关系，用于按键查找对应值。
     */
    @Getter
    private Map<String, String> coreSettings;
    /**
     * 规则引擎映射关系，用于按键查找对应值。
     */
    @Getter
    private Map<String, String> ruleEngineSettings;
    /**
     * 配置映射关系，用于按键查找对应值。
     */
    @Getter
    private Map<String, String> transportApiSettings;
    /**
     * 配置映射关系，用于按键查找对应值。
     */
    @Getter
    private Map<String, String> notificationsSettings;
    /**
     * 配置映射关系，用于按键查找对应值。
     */
    @Getter
    private Map<String, String> jsExecutorSettings;
    /**
     * 配置映射关系，用于按键查找对应值。
     */
    @Getter
    private Map<String, String> vcSettings;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    private void init() {
        coreSettings = PropertyUtils.getProps(coreProperties);
        ruleEngineSettings = PropertyUtils.getProps(ruleEngineProperties);
        transportApiSettings = PropertyUtils.getProps(transportApiProperties);
        notificationsSettings = PropertyUtils.getProps(notificationsProperties);
        jsExecutorSettings = PropertyUtils.getProps(jsExecutorProperties);
        vcSettings = PropertyUtils.getProps(vcProperties);
    }

}
