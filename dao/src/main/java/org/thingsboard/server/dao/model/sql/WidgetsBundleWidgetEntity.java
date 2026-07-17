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
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.widget.WidgetsBundleWidget;
import org.thingsboard.server.dao.model.ToData;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.WIDGETS_BUNDLE_WIDGET_TABLE_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.WIDGET_TYPE_ORDER_PROPERTY;

/**
 * 中文说明：
 * 1. `WidgetsBundleWidgetEntity` 是 ThingsBoard DAO 中表示实体持久化结构的实体类型。
 * 2. 它保存与存储表或查询结果对应的字段。
 * 3. 字段映射用于在数据库记录和平台领域对象之间传递数据。
 * 4. 直接依赖的类型边界包括 `ToData`。
 * 5. 单独的持久化实体可以把存储结构与对外业务模型分开演进。
 * 6. 阅读时重点关注字段映射、主键组成和领域对象转换方法。
 */
@Data
@Entity
@Table(name = WIDGETS_BUNDLE_WIDGET_TABLE_NAME)
@IdClass(WidgetsBundleWidgetCompositeKey.class)
public final class WidgetsBundleWidgetEntity implements ToData<WidgetsBundleWidget> {

    /**
     * 部件包ID，用于定位对应业务对象。
     */
    @Id
    @Column(name = "widgets_bundle_id", columnDefinition = "uuid")
    private UUID widgetsBundleId;

    /**
     * 部件类型ID，用于定位对应业务对象。
     */
    @Id
    @Column(name = "widget_type_id", columnDefinition = "uuid")
    private UUID widgetTypeId;

    /**
     * 部件类型，用于区分不同处理分支。
     */
    @Column(name = WIDGET_TYPE_ORDER_PROPERTY)
    private int widgetTypeOrder;

    /**
     * 功能：创建 `WidgetsBundleWidgetEntity` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public WidgetsBundleWidgetEntity() {
        super();
    }

    /**
     * 功能：创建 `WidgetsBundleWidgetEntity` 实例，并初始化必要字段。
     * 参数：
     * - `widgetsBundleWidget`：`widgetsBundleWidget` 参数。
     * 返回：新创建的对象实例。
     */
    public WidgetsBundleWidgetEntity(WidgetsBundleWidget widgetsBundleWidget) {
        widgetsBundleId = widgetsBundleWidget.getWidgetsBundleId().getId();
        widgetTypeId = widgetsBundleWidget.getWidgetTypeId().getId();
        widgetTypeOrder = widgetsBundleWidget.getWidgetTypeOrder();
    }

    /**
     * 功能：创建 `WidgetsBundleWidgetEntity` 实例，并初始化必要字段。
     * 参数：
     * - `widgetsBundleId`：部件包ID。
     * - `widgetTypeId`：部件类型ID。
     * - `widgetTypeOrder`：类型。
     * 返回：新创建的对象实例。
     */
    public WidgetsBundleWidgetEntity(UUID widgetsBundleId, UUID widgetTypeId, int widgetTypeOrder) {
        this.widgetsBundleId = widgetsBundleId;
        this.widgetTypeId = widgetTypeId;
        this.widgetTypeOrder = widgetTypeOrder;
    }

    /**
     * 功能：执行 `toData` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public WidgetsBundleWidget toData() {
        WidgetsBundleWidget result = new WidgetsBundleWidget();
        result.setWidgetsBundleId(new WidgetsBundleId(widgetsBundleId));
        result.setWidgetTypeId(new WidgetTypeId(widgetTypeId));
        result.setWidgetTypeOrder(widgetTypeOrder);
        return result;
    }
}
