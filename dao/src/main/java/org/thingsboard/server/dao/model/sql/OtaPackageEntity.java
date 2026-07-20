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

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.ota.ChecksumAlgorithm;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.util.mapping.JsonStringType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Lob;
import javax.persistence.Table;
import java.nio.ByteBuffer;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.OTA_PACKAGE_CHECKSUM_ALGORITHM_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.OTA_PACKAGE_CHECKSUM_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.OTA_PACKAGE_CONTENT_TYPE_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.OTA_PACKAGE_DATA_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.OTA_PACKAGE_DATA_SIZE_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.OTA_PACKAGE_DEVICE_PROFILE_ID_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.OTA_PACKAGE_FILE_NAME_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.OTA_PACKAGE_TABLE_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.OTA_PACKAGE_TAG_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.OTA_PACKAGE_TENANT_ID_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.OTA_PACKAGE_TILE_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.OTA_PACKAGE_TYPE_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.OTA_PACKAGE_URL_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.OTA_PACKAGE_VERSION_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.SEARCH_TEXT_PROPERTY;

/**
 * 中文说明：
 * 1. `OtaPackageEntity` 是 ThingsBoard DAO 中表示实体持久化结构的实体类型。
 * 2. 它保存与存储表或查询结果对应的字段。
 * 3. 字段映射用于在数据库记录和平台领域对象之间传递数据。
 * 4. 直接依赖的类型边界包括 `BaseSqlEntity`。
 * 5. 单独的持久化实体可以把存储结构与对外业务模型分开演进。
 * 6. 阅读时重点关注字段映射、主键组成和领域对象转换方法。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = OTA_PACKAGE_TABLE_NAME)
public class OtaPackageEntity extends BaseSqlEntity<OtaPackage> {

    /**
     * 租户ID，用于定位对应业务对象。
     */
    @Column(name = OTA_PACKAGE_TENANT_ID_COLUMN)
    private UUID tenantId;

    /**
     * 设备配置ID，用于定位对应业务对象。
     */
    @Column(name = OTA_PACKAGE_DEVICE_PROFILE_ID_COLUMN)
    private UUID deviceProfileId;

    /**
     * 类型，用于区分不同处理分支。
     */
    @Enumerated(EnumType.STRING)
    @Column(name = OTA_PACKAGE_TYPE_COLUMN)
    private OtaPackageType type;

    /**
     * `title` 字段，保存当前对象的对应属性。
     */
    @Column(name = OTA_PACKAGE_TILE_COLUMN)
    private String title;

    /**
     * 版本号，表示当前对象的对应属性。
     */
    @Column(name = OTA_PACKAGE_VERSION_COLUMN)
    private String version;

    /**
     * `tag` 字段，保存当前对象的对应属性。
     */
    @Column(name = OTA_PACKAGE_TAG_COLUMN)
    private String tag;

    /**
     * URL 地址，用于定位外部资源或本地资源。
     */
    @Column(name = OTA_PACKAGE_URL_COLUMN)
    private String url;

    /**
     * 名称，用于标识或展示当前对象。
     */
    @Column(name = OTA_PACKAGE_FILE_NAME_COLUMN)
    private String fileName;

    /**
     * 类型，用于区分不同处理分支。
     */
    @Column(name = OTA_PACKAGE_CONTENT_TYPE_COLUMN)
    private String contentType;

    /**
     * `checksumAlgorithm` 字段，保存当前对象的对应属性。
     */
    @Enumerated(EnumType.STRING)
    @Column(name = OTA_PACKAGE_CHECKSUM_ALGORITHM_COLUMN)
    private ChecksumAlgorithm checksumAlgorithm;

    /**
     * `checksum` 字段，保存当前对象的对应属性。
     */
    @Column(name = OTA_PACKAGE_CHECKSUM_COLUMN)
    private String checksum;

    /**
     * 数据列表，用于保存一组待处理对象。
     */
    @Lob
    @Column(name = OTA_PACKAGE_DATA_COLUMN, columnDefinition = "BINARY")
    private byte[] data;

    /**
     * 数据，保存当前步骤读取或计算得到的内容。
     */
    @Column(name = OTA_PACKAGE_DATA_SIZE_COLUMN)
    private Long dataSize;

    /**
     * 扩展信息，表示当前对象的对应属性。
     */
    @Type(type = "json")
    @Column(name = ModelConstants.OTA_PACKAGE_ADDITIONAL_INFO_COLUMN)
    private JsonNode additionalInfo;

    /**
     * 功能：创建 `OtaPackageEntity` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public OtaPackageEntity() {
        super();
    }

    /**
     * 功能：创建 `OtaPackageEntity` 实例，并初始化必要字段。
     * 参数：
     * - `otaPackage`：`otaPackage` 参数。
     * 返回：新创建的对象实例。
     */
    public OtaPackageEntity(OtaPackage otaPackage) {
        this.createdTime = otaPackage.getCreatedTime();
        this.setUuid(otaPackage.getUuidId());
        this.tenantId = otaPackage.getTenantId().getId();
        if (otaPackage.getDeviceProfileId() != null) {
            this.deviceProfileId = otaPackage.getDeviceProfileId().getId();
        }
        this.type = otaPackage.getType();
        this.title = otaPackage.getTitle();
        this.version = otaPackage.getVersion();
        this.tag = otaPackage.getTag();
        this.url = otaPackage.getUrl();
        this.fileName = otaPackage.getFileName();
        this.contentType = otaPackage.getContentType();
        this.checksumAlgorithm = otaPackage.getChecksumAlgorithm();
        this.checksum = otaPackage.getChecksum();
        this.data = otaPackage.getData().array();
        this.dataSize = otaPackage.getDataSize();
        this.additionalInfo = otaPackage.getAdditionalInfo();
    }

    /**
     * 功能：执行 `toData` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public OtaPackage toData() {
        OtaPackage otaPackage = new OtaPackage(new OtaPackageId(id));
        otaPackage.setCreatedTime(createdTime);
        otaPackage.setTenantId(TenantId.fromUUID(tenantId));
        if (deviceProfileId != null) {
            otaPackage.setDeviceProfileId(new DeviceProfileId(deviceProfileId));
        }
        otaPackage.setType(type);
        otaPackage.setTitle(title);
        otaPackage.setVersion(version);
        otaPackage.setTag(tag);
        otaPackage.setUrl(url);
        otaPackage.setFileName(fileName);
        otaPackage.setContentType(contentType);
        otaPackage.setChecksumAlgorithm(checksumAlgorithm);
        otaPackage.setChecksum(checksum);
        otaPackage.setDataSize(dataSize);
        if (data != null) {
            otaPackage.setData(ByteBuffer.wrap(data));
            otaPackage.setHasData(true);
        }
        otaPackage.setAdditionalInfo(additionalInfo);
        return otaPackage;
    }
}
