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
package org.thingsboard.server.dao.model.sql;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.notification.NotificationDeliveryMethod;
import org.thingsboard.server.common.data.notification.rule.NotificationRule;
import org.thingsboard.server.common.data.notification.rule.NotificationRuleInfo;
import org.thingsboard.server.common.data.notification.template.NotificationTemplateConfig;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. `NotificationRuleInfoEntity` 是 ThingsBoard DAO 中表示通知持久化结构的实体类型。
 * 2. 它保存与存储表或查询结果对应的字段。
 * 3. 字段映射用于在数据库记录和平台领域对象之间传递数据。
 * 4. 直接依赖的类型边界包括 `NotificationRuleEntity`。
 * 5. 单独的持久化实体可以把存储结构与对外业务模型分开演进。
 * 6. 阅读时重点关注字段映射、主键组成和领域对象转换方法。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class NotificationRuleInfoEntity extends NotificationRuleEntity {

    /**
     * 名称，用于标识或展示当前对象。
     */
    private String templateName;
    private JsonNode templateConfig;

    /**
     * 功能：创建 `NotificationRuleInfoEntity` 实例，并初始化必要字段。
     * 参数：
     * - `ruleEntity`：实体对象。
     * - `templateName`：名称。
     * - `templateConfig`：配置对象。
     * 返回：新创建的对象实例。
     */
    public NotificationRuleInfoEntity(NotificationRuleEntity ruleEntity, String templateName, Object templateConfig) {
        super(ruleEntity);
        this.templateName = templateName;
        this.templateConfig = (JsonNode) templateConfig;
    }

    /**
     * 功能：执行 `toData` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public NotificationRuleInfo toData() {
        NotificationRule rule = super.toData();
        List<NotificationDeliveryMethod> deliveryMethods = fromJson(templateConfig, NotificationTemplateConfig.class)
                .getDeliveryMethodsTemplates().entrySet().stream()
                .filter(entry -> entry.getValue().isEnabled())
                .map(Map.Entry::getKey).collect(Collectors.toList());
        return new NotificationRuleInfo(rule, templateName, deliveryMethods);
    }

}
