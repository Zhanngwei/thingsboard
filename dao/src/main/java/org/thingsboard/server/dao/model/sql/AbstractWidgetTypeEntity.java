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

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.widget.BaseWidgetType;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import java.util.UUID;

/**
 * 中文说明：
 * 1. `AbstractWidgetTypeEntity` 是 ThingsBoard DAO 中表示实体持久化结构的实体类型。
 * 2. 它保存与存储表或查询结果对应的字段。
 * 3. 字段映射用于在数据库记录和平台领域对象之间传递数据。
 * 4. 直接依赖的类型边界包括 `BaseWidgetType`。
 * 5. 单独的持久化实体可以把存储结构与对外业务模型分开演进。
 * 6. 阅读时重点关注字段映射、主键组成和领域对象转换方法。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@MappedSuperclass
public abstract class AbstractWidgetTypeEntity<T extends BaseWidgetType> extends BaseSqlEntity<T> {

    /**
     * 租户ID，用于定位对应业务对象。
     */
    @Column(name = ModelConstants.WIDGET_TYPE_TENANT_ID_PROPERTY)
    private UUID tenantId;

    /**
     * `fqn` 字段，保存当前对象的对应属性。
     */
    @Column(name = ModelConstants.WIDGET_TYPE_FQN_PROPERTY)
    private String fqn;

    /**
     * 名称，用于标识或展示当前对象。
     */
    @Column(name = ModelConstants.WIDGET_TYPE_NAME_PROPERTY)
    private String name;

    /**
     * 是否满足`deprecated`条件。
     */
    @Column(name = ModelConstants.WIDGET_TYPE_DEPRECATED_PROPERTY)
    private boolean deprecated;

    /**
     * 功能：创建 `AbstractWidgetTypeEntity` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public AbstractWidgetTypeEntity() {
        super();
    }

    /**
     * 功能：创建 `AbstractWidgetTypeEntity` 实例，并初始化必要字段。
     * 参数：
     * - `widgetType`：类型。
     * 返回：新创建的对象实例。
     */
    public AbstractWidgetTypeEntity(BaseWidgetType widgetType) {
        if (widgetType.getId() != null) {
            this.setUuid(widgetType.getId().getId());
        }
        this.setCreatedTime(widgetType.getCreatedTime());
        if (widgetType.getTenantId() != null) {
            this.tenantId = widgetType.getTenantId().getId();
        }
        this.fqn = widgetType.getFqn();
        this.name = widgetType.getName();
        this.deprecated = widgetType.isDeprecated();
    }

    /**
     * 功能：创建 `AbstractWidgetTypeEntity` 实例，并初始化必要字段。
     * 参数：
     * - `widgetTypeEntity`：实体对象。
     * 返回：新创建的对象实例。
     */
    public AbstractWidgetTypeEntity(AbstractWidgetTypeEntity widgetTypeEntity) {
        this.setId(widgetTypeEntity.getId());
        this.setCreatedTime(widgetTypeEntity.getCreatedTime());
        this.tenantId = widgetTypeEntity.getTenantId();
        this.fqn = widgetTypeEntity.getFqn();
        this.name = widgetTypeEntity.getName();
        this.deprecated = widgetTypeEntity.isDeprecated();
    }

    /**
     * 功能：执行 `toBaseWidgetType` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    protected BaseWidgetType toBaseWidgetType() {
        BaseWidgetType widgetType = new BaseWidgetType(new WidgetTypeId(getUuid()));
        widgetType.setCreatedTime(createdTime);
        if (tenantId != null) {
            widgetType.setTenantId(TenantId.fromUUID(tenantId));
        }
        widgetType.setFqn(fqn);
        widgetType.setName(name);
        widgetType.setDeprecated(deprecated);
        return widgetType;
    }

}
