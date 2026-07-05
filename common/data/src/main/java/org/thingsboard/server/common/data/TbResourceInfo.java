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
 * 1. 类目的：`TbResourceInfo` 是ThingsBoard Common 模块中的公共数据模型类型，用于承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 生命周期：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Value Object / Builder。
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

/*
 * 本类总结：
 * 1. 核心职责：`TbResourceInfo` 在 ThingsBoard Common 模块 中承担公共数据模型类型职责，核心目的是承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 核心流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
 * 3. 关键依赖：主要依赖或协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
