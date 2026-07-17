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
package org.thingsboard.server.common.data.widget;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.HasImage;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.HasTitle;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

/**
 * 中文说明：
 * 1. `WidgetsBundle` 是 ThingsBoard Common Data 中承载 `Widgets Bundle` 信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `BaseData`、`HasName`、`HasTenantId`、`ExportableEntity`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@ApiModel
@EqualsAndHashCode(callSuper = true)
public class WidgetsBundle extends BaseData<WidgetsBundleId> implements HasName, HasTenantId, ExportableEntity<WidgetsBundleId>, HasTitle, HasImage {

    /**
     * 版本号常量，用于统一引用固定值。
     */
    private static final long serialVersionUID = -7627368878362410489L;

    /**
     * 租户ID，用于定位对应业务对象。
     */
    @Getter
    @Setter
    @ApiModelProperty(position = 3, value = "JSON object with Tenant Id.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private TenantId tenantId;

    /**
     * `alias` 字段，保存当前对象的对应属性。
     */
    @NoXss
    @Length(fieldName = "alias")
    @Getter
    @Setter
    @ApiModelProperty(position = 4, value = "Unique alias that is used in widget types as a reference widget bundle", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private String alias;

    /**
     * `title` 字段，保存当前对象的对应属性。
     */
    @NoXss
    @Length(fieldName = "title")
    @Getter
    @Setter
    @ApiModelProperty(position = 5, value = "Title used in search and UI", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private String title;

    /**
     * 图片资源，表示当前对象的对应属性。
     */
    @Getter
    @Setter
    @ApiModelProperty(position = 6, value = "Relative or external image URL. Replaced with image data URL (Base64) in case of relative URL and 'inlineImages' option enabled.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private String image;

    /**
     * 描述信息，用于展示或标识当前对象。
     */
    @NoXss
    @Length(fieldName = "description", max = 1024)
    @Getter
    @Setter
    @ApiModelProperty(position = 7, value = "Description", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private String description;

    /**
     * `order` 字段，保存当前对象的对应属性。
     */
    @Getter
    @Setter
    @ApiModelProperty(position = 8, value = "Order", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private Integer order;

    /**
     * `externalId`ID，用于定位对应业务对象。
     */
    @Getter
    @Setter
    private WidgetsBundleId externalId;

    /**
     * 功能：创建 `WidgetsBundle` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public WidgetsBundle() {
        super();
    }

    /**
     * 功能：创建 `WidgetsBundle` 实例，并初始化必要字段。
     * 参数：
     * - `id`：`id`ID。
     * 返回：新创建的对象实例。
     */
    public WidgetsBundle(WidgetsBundleId id) {
        super(id);
    }

    /**
     * 功能：创建 `WidgetsBundle` 实例，并初始化必要字段。
     * 参数：
     * - `widgetsBundle`：`widgetsBundle` 参数。
     * 返回：新创建的对象实例。
     */
    public WidgetsBundle(WidgetsBundle widgetsBundle) {
        super(widgetsBundle);
        this.tenantId = widgetsBundle.getTenantId();
        this.alias = widgetsBundle.getAlias();
        this.title = widgetsBundle.getTitle();
        this.image = widgetsBundle.getImage();
        this.description = widgetsBundle.getDescription();
        this.order = widgetsBundle.getOrder();
        this.externalId = widgetsBundle.getExternalId();
    }

    /**
     * 功能：获取`Id`。
     * 参数：无。
     * 返回：处理结果。
     */
    @ApiModelProperty(position = 1, value = "JSON object with the Widget Bundle Id. " +
            "Specify this field to update the Widget Bundle. " +
            "Referencing non-existing Widget Bundle Id will cause error. " +
            "Omit this field to create new Widget Bundle." )
    @Override
    public WidgetsBundleId getId() {
        return super.getId();
    }

    /**
     * 功能：获取创建时间。
     * 参数：无。
     * 返回：数值结果。
     */
    @ApiModelProperty(position = 2, value = "Timestamp of the Widget Bundle creation, in milliseconds", example = "1609459200000", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    /**
     * 功能：获取名称。
     * 参数：无。
     * 返回：文本结果。
     */
    @ApiModelProperty(position = 3, value = "Same as title of the Widget Bundle. Read-only field. Update the 'title' to change the 'name' of the Widget Bundle.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getName() {
        return title;
    }

    /**
     * 功能：生成当前对象的文本表示。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("WidgetsBundle{");
        sb.append("tenantId=").append(tenantId);
        sb.append(", alias='").append(alias).append('\'');
        sb.append(", title='").append(title).append('\'');
        sb.append(", description='").append(description).append('\'');
        sb.append('}');
        return sb.toString();
    }

}
