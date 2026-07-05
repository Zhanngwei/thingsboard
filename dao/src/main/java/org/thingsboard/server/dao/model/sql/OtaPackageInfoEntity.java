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
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.OtaPackageInfo;
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
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.OTA_PACKAGE_CHECKSUM_ALGORITHM_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.OTA_PACKAGE_CHECKSUM_COLUMN;
import static org.thingsboard.server.dao.model.ModelConstants.OTA_PACKAGE_CONTENT_TYPE_COLUMN;
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
 * 1. 类目的：`OtaPackageInfoEntity` 是 ThingsBoard DAO 模块 中的持久化实体映射类型，用于描述 ThingsBoard 领域对象与 SQL/Cassandra 存储结构之间的字段映射、索引关系和序列化边界。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括JPA/Hibernate、Repository、DAO Service、Common DTO、JSON 序列化和数据库迁移脚本。
 * 4. 生命周期：由 ORM、Repository 或 DAO 在读写数据库时创建，并随单次查询或持久化会话存在。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Entity / Mapper / Value Object。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@TypeDef(name = "json", typeClass = JsonStringType.class)
@Table(name = OTA_PACKAGE_TABLE_NAME)
public class OtaPackageInfoEntity extends BaseSqlEntity<OtaPackageInfo> {

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
     * 是否包含数据。
     */
    @Transient
    private boolean hasData;

    /**
     * 功能：创建 `OtaPackageInfoEntity` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public OtaPackageInfoEntity() {
        super();
    }

    /**
     * 功能：创建 `OtaPackageInfoEntity` 实例，并初始化必要字段。
     * 参数：
     * - `otaPackageInfo`：`otaPackageInfo` 参数。
     * 返回：新创建的对象实例。
     */
    public OtaPackageInfoEntity(OtaPackageInfo otaPackageInfo) {
        this.createdTime = otaPackageInfo.getCreatedTime();
        this.setUuid(otaPackageInfo.getUuidId());
        this.tenantId = otaPackageInfo.getTenantId().getId();
        this.type = otaPackageInfo.getType();
        if (otaPackageInfo.getDeviceProfileId() != null) {
            this.deviceProfileId = otaPackageInfo.getDeviceProfileId().getId();
        }
        this.title = otaPackageInfo.getTitle();
        this.version = otaPackageInfo.getVersion();
        this.tag = otaPackageInfo.getTag();
        this.url = otaPackageInfo.getUrl();
        this.fileName = otaPackageInfo.getFileName();
        this.contentType = otaPackageInfo.getContentType();
        this.checksumAlgorithm = otaPackageInfo.getChecksumAlgorithm();
        this.checksum = otaPackageInfo.getChecksum();
        this.dataSize = otaPackageInfo.getDataSize();
        this.additionalInfo = otaPackageInfo.getAdditionalInfo();
    }

    /**
     * 功能：创建 `OtaPackageInfoEntity` 实例，并初始化必要字段。
     * 参数：
     * - `id`：`id`ID。
     * - `createdTime`：`createdTime` 参数。
     * - `tenantId`：租户IDID。
     * - `deviceProfileId`：设备配置ID。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public OtaPackageInfoEntity(UUID id, long createdTime, UUID tenantId, UUID deviceProfileId, OtaPackageType type, String title, String version, String tag,
                                String url, String fileName, String contentType, ChecksumAlgorithm checksumAlgorithm, String checksum, Long dataSize,
                                Object additionalInfo, boolean hasData) {
        this.id = id;
        this.createdTime = createdTime;
        this.tenantId = tenantId;
        this.deviceProfileId = deviceProfileId;
        this.type = type;
        this.title = title;
        this.version = version;
        this.tag = tag;
        this.url = url;
        this.fileName = fileName;
        this.contentType = contentType;
        this.checksumAlgorithm = checksumAlgorithm;
        this.checksum = checksum;
        this.dataSize = dataSize;
        this.hasData = hasData;
        this.additionalInfo = JacksonUtil.convertValue(additionalInfo, JsonNode.class);
    }

    /**
     * 功能：执行 `toData` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public OtaPackageInfo toData() {
        OtaPackageInfo otaPackageInfo = new OtaPackageInfo(new OtaPackageId(id));
        otaPackageInfo.setCreatedTime(createdTime);
        otaPackageInfo.setTenantId(TenantId.fromUUID(tenantId));
        if (deviceProfileId != null) {
            otaPackageInfo.setDeviceProfileId(new DeviceProfileId(deviceProfileId));
        }
        otaPackageInfo.setType(type);
        otaPackageInfo.setTitle(title);
        otaPackageInfo.setVersion(version);
        otaPackageInfo.setTag(tag);
        otaPackageInfo.setUrl(url);
        otaPackageInfo.setFileName(fileName);
        otaPackageInfo.setContentType(contentType);
        otaPackageInfo.setChecksumAlgorithm(checksumAlgorithm);
        otaPackageInfo.setChecksum(checksum);
        otaPackageInfo.setDataSize(dataSize);
        otaPackageInfo.setAdditionalInfo(additionalInfo);
        otaPackageInfo.setHasData(hasData);
        return otaPackageInfo;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`OtaPackageInfoEntity` 在 ThingsBoard DAO 模块 中承担持久化实体映射类型职责，核心目的是描述 ThingsBoard 领域对象与 SQL/Cassandra 存储结构之间的字段映射、索引关系和序列化边界。
 * 2. 核心流程：从数据库行或 Common DTO 构造实体对象，经过 ORM 管理后再转换回上层数据契约。
 * 3. 关键依赖：主要依赖或协作对象包括JPA/Hibernate、Repository、DAO Service、Common DTO、JSON 序列化和数据库迁移脚本。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
