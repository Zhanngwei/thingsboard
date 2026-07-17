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
package org.thingsboard.server.common.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

import java.util.function.UnaryOperator;

/**
 * 中文说明：
 * 1. `TbResourceInfo` 是 ThingsBoard Common Data 中承载资源信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `BaseData`、`HasName`、`HasTenantId`、`ExportableEntity`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@ApiModel
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class TbResourceInfo extends BaseData<TbResourceId> implements HasName, HasTenantId, ExportableEntity<TbResourceId> {

    /**
     * 版本号常量，用于统一引用固定值。
     */
    private static final long serialVersionUID = 7282664529021651736L;

    /**
     * 租户ID，用于定位对应业务对象。
     */
    @ApiModelProperty(position = 3, value = "JSON object with Tenant Id. Tenant Id of the resource can't be changed.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private TenantId tenantId;
    /**
     * `title` 字段，保存当前对象的对应属性。
     */
    @NoXss
    @Length(fieldName = "title")
    @ApiModelProperty(position = 4, value = "Resource title.", example = "BinaryAppDataContainer id=19 v1.0")
    private String title;
    /**
     * 类型，用于区分不同处理分支。
     */
    @ApiModelProperty(position = 5, value = "Resource type.", example = "LWM2M_MODEL", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private ResourceType resourceType;
    /**
     * 键，用于定位映射、配置或数据项。
     */
    @NoXss
    @Length(fieldName = "resourceKey")
    @ApiModelProperty(position = 6, value = "Resource key.", example = "19_1.0", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private String resourceKey;
    private boolean isPublic;
    /**
     * 公钥，用于定位映射、配置或数据项。
     */
    private String publicResourceKey;
    /**
     * 搜索文本，表示当前对象的对应属性。
     */
    @ApiModelProperty(position = 7, value = "Resource search text.", example = "19_1.0:binaryappdatacontainer", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private String searchText;

    /**
     * `etag` 字段，保存当前对象的对应属性。
     */
    @ApiModelProperty(position = 8, value = "Resource etag.", example = "33a64df551425fcc55e4d42a148795d9f25f89d4", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private String etag;
    /**
     * 名称，用于标识或展示当前对象。
     */
    @NoXss
    @Length(fieldName = "file name")
    @ApiModelProperty(position = 9, value = "Resource file name.", example = "19.xml", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private String fileName;
    private JsonNode descriptor;

    /**
     * `externalId`ID，用于定位对应业务对象。
     */
    private TbResourceId externalId;

    /**
     * 功能：创建 `TbResourceInfo` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public TbResourceInfo() {
        super();
    }

    /**
     * 功能：创建 `TbResourceInfo` 实例，并初始化必要字段。
     * 参数：
     * - `id`：`id`ID。
     * 返回：新创建的对象实例。
     */
    public TbResourceInfo(TbResourceId id) {
        super(id);
    }

    /**
     * 功能：创建 `TbResourceInfo` 实例，并初始化必要字段。
     * 参数：
     * - `resourceInfo`：`resourceInfo` 参数。
     * 返回：新创建的对象实例。
     */
    public TbResourceInfo(TbResourceInfo resourceInfo) {
        super(resourceInfo);
        this.tenantId = resourceInfo.tenantId;
        this.title = resourceInfo.title;
        this.resourceType = resourceInfo.resourceType;
        this.resourceKey = resourceInfo.resourceKey;
        this.searchText = resourceInfo.searchText;
        this.isPublic = resourceInfo.isPublic;
        this.publicResourceKey = resourceInfo.publicResourceKey;
        this.etag = resourceInfo.etag;
        this.fileName = resourceInfo.fileName;
        this.descriptor = resourceInfo.descriptor != null ? resourceInfo.descriptor.deepCopy() : null;
        this.externalId = resourceInfo.externalId;
    }

    /**
     * 功能：获取`Id`。
     * 参数：无。
     * 返回：处理结果。
     */
    @ApiModelProperty(position = 1, value = "JSON object with the Resource Id. " +
            "Specify this field to update the Resource. " +
            "Referencing non-existing Resource Id will cause error. " +
            "Omit this field to create new Resource.")
    @Override
    public TbResourceId getId() {
        return super.getId();
    }

    /**
     * 功能：获取创建时间。
     * 参数：无。
     * 返回：数值结果。
     */
    @ApiModelProperty(position = 2, value = "Timestamp of the resource creation, in milliseconds", example = "1609459200000", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    /**
     * 功能：获取名称。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getName() {
        return title;
    }

    /**
     * 功能：获取`Link`。
     * 参数：无。
     * 返回：文本结果。
     */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getLink() {
        if (resourceType == ResourceType.IMAGE) {
            String type = (tenantId != null && tenantId.isSysTenantId()) ? "system" : "tenant"; // tenantId is null in case of export to git
            return "/api/images/" + type + "/" + resourceKey;
        }
        return null;
    }

    /**
     * 功能：获取`Public Link`。
     * 参数：无。
     * 返回：文本结果。
     */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getPublicLink() {
        if (resourceType == ResourceType.IMAGE && isPublic) {
            return "/api/images/public/" + getPublicResourceKey();
        }
        return null;
    }

    /**
     * 功能：获取搜索文本。
     * 参数：无。
     * 返回：文本结果。
     */
    @JsonIgnore
    public String getSearchText() {
        return title;
    }

    /**
     * 功能：获取`Descriptor`。
     * 参数：
     * - `type`：类型。
     * 返回：处理结果。
     */
    public <T> T getDescriptor(Class<T> type) throws JsonProcessingException {
        return descriptor != null ? mapper.treeToValue(descriptor, type) : null;
    }

    /**
     * 功能：更新`Descriptor`。
     * 参数：
     * - `type`：类型。
     * - `updater`：`updater` 参数。
     * 返回：无。
     */
    public <T> void updateDescriptor(Class<T> type, UnaryOperator<T> updater) throws JsonProcessingException {
        T descriptor = getDescriptor(type);
        descriptor = updater.apply(descriptor);
        setDescriptorValue(descriptor);
    }

    /**
     * 功能：更新值。
     * 参数：
     * - `value`：值。
     * 返回：无。
     */
    @JsonIgnore
    public void setDescriptorValue(Object value) {
        this.descriptor = value != null ? mapper.valueToTree(value) : null;
    }

}
