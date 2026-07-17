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
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.UUID;

/**
 * 中文说明：
 * 1. `WidgetsBundleEntity` 是 ThingsBoard DAO 中表示实体持久化结构的实体类型。
 * 2. 它保存与存储表或查询结果对应的字段。
 * 3. 字段映射用于在数据库记录和平台领域对象之间传递数据。
 * 4. 直接依赖的类型边界包括 `BaseSqlEntity`。
 * 5. 单独的持久化实体可以把存储结构与对外业务模型分开演进。
 * 6. 阅读时重点关注字段映射、主键组成和领域对象转换方法。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = ModelConstants.WIDGETS_BUNDLE_TABLE_NAME)
public final class WidgetsBundleEntity extends BaseSqlEntity<WidgetsBundle> {

    /**
     * 租户ID，用于定位对应业务对象。
     */
    @Column(name = ModelConstants.WIDGETS_BUNDLE_TENANT_ID_PROPERTY)
    private UUID tenantId;

    /**
     * `alias` 字段，保存当前对象的对应属性。
     */
    @Column(name = ModelConstants.WIDGETS_BUNDLE_ALIAS_PROPERTY)
    private String alias;

    /**
     * `title` 字段，保存当前对象的对应属性。
     */
    @Column(name = ModelConstants.WIDGETS_BUNDLE_TITLE_PROPERTY)
    private String title;

    /**
     * 图片资源，表示当前对象的对应属性。
     */
    @Column(name = ModelConstants.WIDGETS_BUNDLE_IMAGE_PROPERTY)
    private String image;

    /**
     * 描述信息，用于展示或标识当前对象。
     */
    @Column(name = ModelConstants.WIDGETS_BUNDLE_DESCRIPTION)
    private String description;

    /**
     * `order` 字段，保存当前对象的对应属性。
     */
    @Column(name = ModelConstants.WIDGETS_BUNDLE_ORDER)
    private Integer order;

    /**
     * `externalId`ID，用于定位对应业务对象。
     */
    @Column(name = ModelConstants.EXTERNAL_ID_PROPERTY)
    private UUID externalId;

    /**
     * 功能：创建 `WidgetsBundleEntity` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public WidgetsBundleEntity() {
        super();
    }

    /**
     * 功能：创建 `WidgetsBundleEntity` 实例，并初始化必要字段。
     * 参数：
     * - `widgetsBundle`：`widgetsBundle` 参数。
     * 返回：新创建的对象实例。
     */
    public WidgetsBundleEntity(WidgetsBundle widgetsBundle) {
        if (widgetsBundle.getId() != null) {
            this.setUuid(widgetsBundle.getId().getId());
        }
        this.setCreatedTime(widgetsBundle.getCreatedTime());
        if (widgetsBundle.getTenantId() != null) {
            this.tenantId = widgetsBundle.getTenantId().getId();
        }
        this.alias = widgetsBundle.getAlias();
        this.title = widgetsBundle.getTitle();
        this.image = widgetsBundle.getImage();
        this.description = widgetsBundle.getDescription();
        this.order = widgetsBundle.getOrder();
        if (widgetsBundle.getExternalId() != null) {
            this.externalId = widgetsBundle.getExternalId().getId();
        }
    }

    /**
     * 功能：执行 `toData` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public WidgetsBundle toData() {
        WidgetsBundle widgetsBundle = new WidgetsBundle(new WidgetsBundleId(id));
        widgetsBundle.setCreatedTime(createdTime);
        if (tenantId != null) {
            widgetsBundle.setTenantId(TenantId.fromUUID(tenantId));
        }
        widgetsBundle.setAlias(alias);
        widgetsBundle.setTitle(title);
        widgetsBundle.setImage(image);
        widgetsBundle.setDescription(description);
        widgetsBundle.setOrder(order);
        if (externalId != null) {
            widgetsBundle.setExternalId(new WidgetsBundleId(externalId));
        }
        return widgetsBundle;
    }
}
