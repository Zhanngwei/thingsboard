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

import io.hypersistence.utils.hibernate.type.array.StringArrayType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.widget.BaseWidgetType;
import org.thingsboard.server.common.data.widget.WidgetTypeInfo;
import org.thingsboard.server.dao.model.ModelConstants;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.HashMap;
import java.util.Map;

/**
 * 中文说明：
 * 1. `WidgetTypeInfoEntity` 是 ThingsBoard DAO 中表示实体持久化结构的实体类型。
 * 2. 它保存与存储表或查询结果对应的字段。
 * 3. 字段映射用于在数据库记录和平台领域对象之间传递数据。
 * 4. 直接依赖的类型边界包括 `AbstractWidgetTypeEntity`。
 * 5. 单独的持久化实体可以把存储结构与对外业务模型分开演进。
 * 6. 阅读时重点关注字段映射、主键组成和领域对象转换方法。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "string-array", typeClass = StringArrayType.class)
@Table(name = ModelConstants.WIDGET_TYPE_INFO_VIEW_TABLE_NAME)
public class WidgetTypeInfoEntity extends AbstractWidgetTypeEntity<WidgetTypeInfo> {

    public static final Map<String, String> SEARCH_COLUMNS_MAP = new HashMap<>();

    static {
        SEARCH_COLUMNS_MAP.put("createdTime", "created_time");
        SEARCH_COLUMNS_MAP.put("tenantId", "tenant_id");
        SEARCH_COLUMNS_MAP.put("widgetType", "widget_type");
    }

    /**
     * 图片资源，表示当前对象的对应属性。
     */
    @Column(name = ModelConstants.WIDGET_TYPE_IMAGE_PROPERTY)
    private String image;

    /**
     * 描述信息，用于展示或标识当前对象。
     */
    @Column(name = ModelConstants.WIDGET_TYPE_DESCRIPTION_PROPERTY)
    private String description;

    /**
     * `tags`列表，用于保存一组待处理对象。
     */
    @Type(type = "string-array")
    @Column(name = ModelConstants.WIDGET_TYPE_TAGS_PROPERTY, columnDefinition = "text[]")
    private String[] tags;

    /**
     * 部件类型，用于区分不同处理分支。
     */
    @Column(name = ModelConstants.WIDGET_TYPE_WIDGET_TYPE_PROPERTY)
    private String widgetType;

    /**
     * 功能：创建 `WidgetTypeInfoEntity` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public WidgetTypeInfoEntity() {
        super();
    }

    /**
     * 功能：执行 `toData` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public WidgetTypeInfo toData() {
        BaseWidgetType baseWidgetType = super.toBaseWidgetType();
        WidgetTypeInfo widgetTypeInfo = new WidgetTypeInfo(baseWidgetType);
        widgetTypeInfo.setImage(image);
        widgetTypeInfo.setDescription(description);
        widgetTypeInfo.setTags(tags);
        widgetTypeInfo.setWidgetType(widgetType);
        return widgetTypeInfo;
    }

}
