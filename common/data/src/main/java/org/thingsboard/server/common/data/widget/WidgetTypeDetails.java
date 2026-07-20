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

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.HasImage;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

/**
 * 中文说明：
 * 1. `WidgetTypeDetails` 是 ThingsBoard Common Data 中承载部件信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `WidgetType`、`HasName`、`HasTenantId`、`HasImage`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Data
@JsonPropertyOrder({ "fqn", "name", "deprecated", "image", "description", "descriptor", "externalId" })
public class WidgetTypeDetails extends WidgetType implements HasName, HasTenantId, HasImage, ExportableEntity<WidgetTypeId> {

    /**
     * 图片资源，表示当前对象的对应属性。
     */
    @ApiModelProperty(position = 9, value = "Relative or external image URL. Replaced with image data URL (Base64) in case of relative URL and 'inlineImages' option enabled.")
    private String image;
    /**
     * 描述信息，用于展示或标识当前对象。
     */
    @NoXss
    @Length(fieldName = "description", max = 1024)
    @ApiModelProperty(position = 10, value = "Description of the widget")
    private String description;
    /**
     * `tags`列表，用于保存一组待处理对象。
     */
    @NoXss
    @ApiModelProperty(position = 11, value = "Tags of the widget type")
    private String[] tags;

    /**
     * `externalId`ID，用于定位对应业务对象。
     */
    @Getter
    @Setter
    private WidgetTypeId externalId;

    /**
     * 功能：创建 `WidgetTypeDetails` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public WidgetTypeDetails() {
        super();
    }

    /**
     * 功能：创建 `WidgetTypeDetails` 实例，并初始化必要字段。
     * 参数：
     * - `id`：`id`ID。
     * 返回：新创建的对象实例。
     */
    public WidgetTypeDetails(WidgetTypeId id) {
        super(id);
    }

    /**
     * 功能：创建 `WidgetTypeDetails` 实例，并初始化必要字段。
     * 参数：
     * - `baseWidgetType`：类型。
     * 返回：新创建的对象实例。
     */
    public WidgetTypeDetails(BaseWidgetType baseWidgetType) {
        super(baseWidgetType);
    }

    /**
     * 功能：创建 `WidgetTypeDetails` 实例，并初始化必要字段。
     * 参数：
     * - `widgetTypeDetails`：类型。
     * 返回：新创建的对象实例。
     */
    public WidgetTypeDetails(WidgetTypeDetails widgetTypeDetails) {
        super(widgetTypeDetails);
        this.image = widgetTypeDetails.getImage();
        this.description = widgetTypeDetails.getDescription();
        this.tags = widgetTypeDetails.getTags();
        this.externalId = widgetTypeDetails.getExternalId();
    }
}
