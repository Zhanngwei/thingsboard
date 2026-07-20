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
package org.thingsboard.server.dao.audit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.audit.ActionType;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 中文说明：
 * 1. `AuditLogLevelFilter` 是 ThingsBoard DAO 中处理 `Audit Log Level` 的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 它直接协作于事件源、上下文对象和后续处理组件。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
@Component
@ConditionalOnProperty(prefix = "audit-log", value = "enabled", havingValue = "true")
public class AuditLogLevelFilter {

    private Map<EntityType, AuditLogLevelMask> entityTypeMask = new HashMap<>();

    /**
     * 功能：创建 `AuditLogLevelFilter` 实例，并初始化必要字段。
     * 参数：
     * - `auditLogLevelProperties`：`auditLogLevelProperties` 参数。
     * 返回：新创建的对象实例。
     */
    public AuditLogLevelFilter(AuditLogLevelProperties auditLogLevelProperties) {
        Map<String, String> mask = auditLogLevelProperties.getMask();
        entityTypeMask.clear();
        mask.forEach((entityTypeStr, logLevelMaskStr) -> {
            EntityType entityType = EntityType.valueOf(entityTypeStr.toUpperCase(Locale.ENGLISH));
            AuditLogLevelMask logLevelMask = AuditLogLevelMask.valueOf(logLevelMaskStr.toUpperCase());
            entityTypeMask.put(entityType, logLevelMask);
        });
    }

    /**
     * 功能：执行 `logEnabled` 对应的处理。
     * 参数：
     * - `entityType`：实体对象。
     * - `actionType`：类型。
     * 返回：判断结果。
     */
    public boolean logEnabled(EntityType entityType, ActionType actionType) {
        AuditLogLevelMask logLevelMask = entityTypeMask.get(entityType);
        if (logLevelMask != null) {
            return actionType.isRead() ? logLevelMask.isRead() : logLevelMask.isWrite();
        } else {
            return false;
        }
    }

}
