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
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.ota.ChecksumAlgorithm;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

/**
 * 中文说明：
 * 1. 类目的：`OtaPackageInfo` 是ThingsBoard Common 模块中的公共数据模型类型，用于承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
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
public class OtaPackageInfo extends BaseDataWithAdditionalInfo<OtaPackageId> implements HasName, HasTenantId, HasTitle {

    /**
     * 版本号常量，用于统一引用固定值。
     */
    private static final long serialVersionUID = 3168391583570815419L;

    /**
     * 租户ID，用于定位对应业务对象。
     */
    @ApiModelProperty(position = 3, value = "JSON object with Tenant Id. Tenant Id of the ota package can't be changed.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private TenantId tenantId;
    /**
     * 设备配置ID，用于定位对应业务对象。
     */
    @ApiModelProperty(position = 4, value = "JSON object with Device Profile Id. Device Profile Id of the ota package can't be changed.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private DeviceProfileId deviceProfileId;
    /**
     * 类型，用于区分不同处理分支。
     */
    @ApiModelProperty(position = 5, value = "OTA Package type.", example = "FIRMWARE", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private OtaPackageType type;
    /**
     * `title` 字段，保存当前对象的对应属性。
     */
    @Length(fieldName = "title")
    @NoXss
    @ApiModelProperty(position = 6, value = "OTA Package title.", example = "fw", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private String title;
    /**
     * 版本号，表示当前对象的对应属性。
     */
    @Length(fieldName = "version")
    @NoXss
    @ApiModelProperty(position = 7, value = "OTA Package version.", example = "1.0", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private String version;
    /**
     * `tag` 字段，保存当前对象的对应属性。
     */
    @Length(fieldName = "tag")
    @NoXss
    @ApiModelProperty(position = 8, value = "OTA Package tag.", example = "fw_1.0", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private String tag;
    /**
     * URL 地址，用于定位外部资源或本地资源。
     */
    @Length(fieldName = "url")
    @NoXss
    @ApiModelProperty(position = 9, value = "OTA Package url.", example = "http://thingsboard.org/fw/1", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private String url;
    /**
     * 是否包含数据。
     */
    @ApiModelProperty(position = 10, value = "Indicates OTA Package 'has data'. Field is returned from DB ('true' if data exists or url is set).  If OTA Package 'has data' is 'false' we can not assign the OTA Package to the Device or Device Profile.", example = "true", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private boolean hasData;
    /**
     * 名称，用于标识或展示当前对象。
     */
    @Length(fieldName = "file name")
    @NoXss
    @ApiModelProperty(position = 11, value = "OTA Package file name.", example = "fw_1.0", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private String fileName;
    /**
     * 类型，用于区分不同处理分支。
     */
    @NoXss
    @Length(fieldName = "contentType")
    @ApiModelProperty(position = 12, value = "OTA Package content type.", example = "APPLICATION_OCTET_STREAM", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private String contentType;
    /**
     * `checksumAlgorithm` 字段，保存当前对象的对应属性。
     */
    @ApiModelProperty(position = 13, value = "OTA Package checksum algorithm.", example = "CRC32", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private ChecksumAlgorithm checksumAlgorithm;
    /**
     * `checksum` 字段，保存当前对象的对应属性。
     */
    @Length(fieldName = "checksum", max = 1020)
    @ApiModelProperty(position = 14, value = "OTA Package checksum.", example = "0xd87f7e0c", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private String checksum;
    /**
     * 数据，保存当前步骤读取或计算得到的内容。
     */
    @ApiModelProperty(position = 15, value = "OTA Package data size.", example = "8", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private Long dataSize;

    /**
     * 功能：创建 `OtaPackageInfo` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public OtaPackageInfo() {
        super();
    }

    /**
     * 功能：创建 `OtaPackageInfo` 实例，并初始化必要字段。
     * 参数：
     * - `id`：`id`ID。
     * 返回：新创建的对象实例。
     */
    public OtaPackageInfo(OtaPackageId id) {
        super(id);
    }

    /**
     * 功能：创建 `OtaPackageInfo` 实例，并初始化必要字段。
     * 参数：
     * - `otaPackageInfo`：`otaPackageInfo` 参数。
     * 返回：新创建的对象实例。
     */
    public OtaPackageInfo(OtaPackageInfo otaPackageInfo) {
        super(otaPackageInfo);
        this.tenantId = otaPackageInfo.getTenantId();
        this.deviceProfileId = otaPackageInfo.getDeviceProfileId();
        this.type = otaPackageInfo.getType();
        this.title = otaPackageInfo.getTitle();
        this.version = otaPackageInfo.getVersion();
        this.tag = otaPackageInfo.getTag();
        this.url = otaPackageInfo.getUrl();
        this.hasData = otaPackageInfo.isHasData();
        this.fileName = otaPackageInfo.getFileName();
        this.contentType = otaPackageInfo.getContentType();
        this.checksumAlgorithm = otaPackageInfo.getChecksumAlgorithm();
        this.checksum = otaPackageInfo.getChecksum();
        this.dataSize = otaPackageInfo.getDataSize();
    }

    /**
     * 功能：获取`Id`。
     * 参数：无。
     * 返回：处理结果。
     */
    @ApiModelProperty(position = 1, value = "JSON object with the ota package Id. " +
            "Specify existing ota package Id to update the ota package. " +
            "Referencing non-existing ota package id will cause error. " +
            "Omit this field to create new ota package.")
    @Override
    public OtaPackageId getId() {
        return super.getId();
    }

    /**
     * 功能：获取创建时间。
     * 参数：无。
     * 返回：数值结果。
     */
    @ApiModelProperty(position = 2, value = "Timestamp of the ota package creation, in milliseconds", example = "1609459200000", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
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
    @JsonIgnore
    public String getName() {
        return title;
    }

    /**
     * 功能：判断URL 地址。
     * 参数：无。
     * 返回：判断结果。
     */
    @JsonIgnore
    public boolean hasUrl() {
        return StringUtils.isNotEmpty(url);
    }

    /**
     * 功能：获取扩展信息。
     * 参数：无。
     * 返回：处理结果。
     */
    @ApiModelProperty(position = 17, value = "OTA Package description.", example = "Description for the OTA Package fw_1.0")
    @Override
    public JsonNode getAdditionalInfo() {
        return super.getAdditionalInfo();
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`OtaPackageInfo` 在 ThingsBoard Common 模块 中承担公共数据模型类型职责，核心目的是承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 核心流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
 * 3. 关键依赖：主要依赖或协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
