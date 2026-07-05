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
package org.thingsboard.server.dao.ota;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.cache.ota.OtaPackageDataCache;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.ota.ChecksumAlgorithm;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;

import java.nio.ByteBuffer;
import java.util.Optional;

import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;

/**
 * 中文说明：
 * 1. 类目的：`BaseOtaPackageService` 是 ThingsBoard DAO 模块 中的资源与 OTA 持久化服务类型，用于管理二进制资源、图片、OTA 包元数据和关联实体的数据库访问。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括ResourceService、ImageService、OtaPackageService、缓存、存储服务和设备配置流程。
 * 4. 生命周期：由资源上传、下载、删除、OTA 发布或设备配置读取流程调用，并随事务完成更新状态。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Service / Repository / Adapter。
 */
@Service("OtaPackageDaoService")
@Slf4j
@RequiredArgsConstructor
public class BaseOtaPackageService extends AbstractCachedEntityService<OtaPackageCacheKey, OtaPackageInfo, OtaPackageCacheEvictEvent> implements OtaPackageService {
    /**
     * `INCORRECT_OTA_PACKAGE_ID`常量，用于统一引用固定值。
     */
    public static final String INCORRECT_OTA_PACKAGE_ID = "Incorrect otaPackageId ";
    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";

    /**
     * 存取组件，用于读取或保存对应领域对象。
     */
    private final OtaPackageDao otaPackageDao;
    private final OtaPackageInfoDao otaPackageInfoDao;
    /**
     * 数据，保存当前步骤读取或计算得到的内容。
     */
    private final OtaPackageDataCache otaPackageDataCache;
    private final DataValidator<OtaPackageInfo> otaPackageInfoValidator;
    /**
     * 校验器，封装可复用的处理规则。
     */
    private final DataValidator<OtaPackage> otaPackageValidator;

    /**
     * 功能：处理事件。
     * 参数：
     * - `event`：`event` 参数。
     * 返回：无。
     */
    @TransactionalEventListener(classes = OtaPackageCacheEvictEvent.class)
    @Override
    public void handleEvictEvent(OtaPackageCacheEvictEvent event) {
        cache.evict(new OtaPackageCacheKey(event.getId()));
        otaPackageDataCache.evict(event.getId().toString());
    }

    /**
     * 功能：保存或创建信息对象。
     * 参数：
     * - `otaPackageInfo`：`otaPackageInfo` 参数。
     * - `isUrl`：`isUrl` 参数。
     * 返回：处理结果。
     */
    @Override
    public OtaPackageInfo saveOtaPackageInfo(OtaPackageInfo otaPackageInfo, boolean isUrl) {
        log.trace("Executing saveOtaPackageInfo [{}]", otaPackageInfo);
        if (isUrl && (StringUtils.isEmpty(otaPackageInfo.getUrl()) || otaPackageInfo.getUrl().trim().length() == 0)) {
            throw new DataValidationException("Ota package URL should be specified!");
        }
        otaPackageInfoValidator.validate(otaPackageInfo, OtaPackageInfo::getTenantId);
        OtaPackageId otaPackageId = otaPackageInfo.getId();
        try {
            OtaPackageInfo result = otaPackageInfoDao.save(otaPackageInfo.getTenantId(), otaPackageInfo);
            if (otaPackageId != null) {
                publishEvictEvent(new OtaPackageCacheEvictEvent(otaPackageId));
            }
            eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(result.getTenantId()).entity(result)
                    .entityId(result.getId()).created(otaPackageId == null).build());
            return result;
        } catch (Exception t) {
            if (otaPackageId != null) {
                handleEvictEvent(new OtaPackageCacheEvictEvent(otaPackageId));
            }
            ConstraintViolationException e = extractConstraintViolationException(t).orElse(null);
            if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("ota_package_tenant_title_version_unq_key")) {
                throw new DataValidationException("OtaPackage with such title and version already exists!");
            } else {
                throw t;
            }
        }
    }

    /**
     * 功能：保存或创建`Ota Package`。
     * 参数：
     * - `otaPackage`：`otaPackage` 参数。
     * 返回：处理结果。
     */
    @Override
    public OtaPackage saveOtaPackage(OtaPackage otaPackage) {
        log.trace("Executing saveOtaPackage [{}]", otaPackage);
        otaPackageValidator.validate(otaPackage, OtaPackageInfo::getTenantId);
        OtaPackageId otaPackageId = otaPackage.getId();
        try {
            var result = otaPackageDao.save(otaPackage.getTenantId(), otaPackage);
            if (otaPackageId != null) {
                publishEvictEvent(new OtaPackageCacheEvictEvent(otaPackageId));
            }
            eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(result.getTenantId())
                    .entityId(result.getId()).created(otaPackageId == null).build());
            return result;
        } catch (Exception t) {
            if (otaPackageId != null) {
                handleEvictEvent(new OtaPackageCacheEvictEvent(otaPackageId));
            }
            ConstraintViolationException e = extractConstraintViolationException(t).orElse(null);
            if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("ota_package_tenant_title_version_unq_key")) {
                throw new DataValidationException("OtaPackage with such title and version already exists!");
            } else {
                throw t;
            }
        }
    }

    /**
     * 功能：执行 `generateChecksum` 对应的处理。
     * 参数：
     * - `checksumAlgorithm`：`checksumAlgorithm` 参数。
     * - `data`：待处理数据。
     * 返回：文本结果。
     */
    @Override
    public String generateChecksum(ChecksumAlgorithm checksumAlgorithm, ByteBuffer data) {
        if (data == null || !data.hasArray() || data.array().length == 0) {
            throw new DataValidationException("OtaPackage data should be specified!");
        }

        return getHashFunction(checksumAlgorithm).hashBytes(data.array()).toString();
    }

    /**
     * 功能：获取`Hash Function`。
     * 参数：
     * - `checksumAlgorithm`：`checksumAlgorithm` 参数。
     * 返回：处理结果。
     */
    @SuppressWarnings("deprecation")
    private HashFunction getHashFunction(ChecksumAlgorithm checksumAlgorithm) {
        switch (checksumAlgorithm) {
            case MD5:
                return Hashing.md5();
            case SHA256:
                return Hashing.sha256();
            case SHA384:
                return Hashing.sha384();
            case SHA512:
                return Hashing.sha512();
            case CRC32:
                return Hashing.crc32();
            case MURMUR3_32:
                return Hashing.murmur3_32();
            case MURMUR3_128:
                return Hashing.murmur3_128();
            default:
                throw new DataValidationException("Unknown checksum algorithm!");
        }
    }

    /**
     * 功能：获取`Ota Package By Id`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `otaPackageId`：`otaPackageId`ID。
     * 返回：处理结果。
     */
    @Override
    public OtaPackage findOtaPackageById(TenantId tenantId, OtaPackageId otaPackageId) {
        log.trace("Executing findOtaPackageById [{}]", otaPackageId);
        validateId(otaPackageId, INCORRECT_OTA_PACKAGE_ID + otaPackageId);
        return otaPackageDao.findById(tenantId, otaPackageId.getId());
    }

    /**
     * 功能：获取信息对象。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `otaPackageId`：`otaPackageId`ID。
     * 返回：处理结果。
     */
    @Override
    public OtaPackageInfo findOtaPackageInfoById(TenantId tenantId, OtaPackageId otaPackageId) {
        log.trace("Executing findOtaPackageInfoById [{}]", otaPackageId);
        validateId(otaPackageId, INCORRECT_OTA_PACKAGE_ID + otaPackageId);
        return cache.getAndPutInTransaction(new OtaPackageCacheKey(otaPackageId),
                () -> otaPackageInfoDao.findById(tenantId, otaPackageId.getId()), true);
    }

    /**
     * 功能：获取信息对象。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `otaPackageId`：`otaPackageId`ID。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<OtaPackageInfo> findOtaPackageInfoByIdAsync(TenantId tenantId, OtaPackageId otaPackageId) {
        log.trace("Executing findOtaPackageInfoByIdAsync [{}]", otaPackageId);
        validateId(otaPackageId, INCORRECT_OTA_PACKAGE_ID + otaPackageId);
        return otaPackageInfoDao.findByIdAsync(tenantId, otaPackageId.getId());
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<OtaPackageInfo> findTenantOtaPackagesByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findTenantOtaPackagesByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validatePageLink(pageLink);
        return otaPackageInfoDao.findOtaPackageInfoByTenantId(tenantId, pageLink);
    }

    /**
     * 功能：获取设备配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceProfileId`：设备配置ID。
     * - `otaPackageType`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<OtaPackageInfo> findTenantOtaPackagesByTenantIdAndDeviceProfileIdAndTypeAndHasData(TenantId tenantId, DeviceProfileId deviceProfileId, OtaPackageType otaPackageType, PageLink pageLink) {
        log.trace("Executing findTenantOtaPackagesByTenantIdAndHasData, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validatePageLink(pageLink);
        return otaPackageInfoDao.findOtaPackageInfoByTenantIdAndDeviceProfileIdAndTypeAndHasData(tenantId, deviceProfileId, otaPackageType, pageLink);
    }

    /**
     * 功能：删除或清理`Ota Package`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `otaPackageId`：`otaPackageId`ID。
     * 返回：无。
     */
    @Override
    public void deleteOtaPackage(TenantId tenantId, OtaPackageId otaPackageId) {
        log.trace("Executing deleteOtaPackage [{}]", otaPackageId);
        validateId(otaPackageId, INCORRECT_OTA_PACKAGE_ID + otaPackageId);
        try {
            otaPackageDao.removeById(tenantId, otaPackageId.getId());
            publishEvictEvent(new OtaPackageCacheEvictEvent(otaPackageId));
            eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(otaPackageId).build());
        } catch (Exception t) {
            ConstraintViolationException e = extractConstraintViolationException(t).orElse(null);
            if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("fk_firmware_device")) {
                throw new DataValidationException("The otaPackage referenced by the devices cannot be deleted!");
            } else if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("fk_firmware_device_profile")) {
                throw new DataValidationException("The otaPackage referenced by the device profile cannot be deleted!");
            } else if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("fk_software_device")) {
                throw new DataValidationException("The software referenced by the devices cannot be deleted!");
            } else if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("fk_software_device_profile")) {
                throw new DataValidationException("The software referenced by the device profile cannot be deleted!");
            } else {
                throw t;
            }
        }
    }

    /**
     * 功能：执行 `sumDataSizeByTenantId` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：数值结果。
     */
    @Override
    public long sumDataSizeByTenantId(TenantId tenantId) {
        return otaPackageDao.sumDataSizeByTenantId(tenantId);
    }

    /**
     * 功能：删除或清理租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    @Override
    public void deleteOtaPackagesByTenantId(TenantId tenantId) {
        log.trace("Executing deleteOtaPackagesByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        tenantOtaPackageRemover.removeEntities(tenantId, tenantId);
    }

    private PaginatedRemover<TenantId, OtaPackageInfo> tenantOtaPackageRemover =
            new PaginatedRemover<>() {

                @Override
                protected PageData<OtaPackageInfo> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
                    return otaPackageInfoDao.findOtaPackageInfoByTenantId(id, pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, OtaPackageInfo entity) {
                    deleteOtaPackage(tenantId, entity.getId());
                }
            };

    /**
     * 功能：获取实体。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * 返回：可能存在的结果。
     */
    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findOtaPackageInfoById(tenantId, new OtaPackageId(entityId.getId())));
    }

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public EntityType getEntityType() {
        return EntityType.OTA_PACKAGE;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`BaseOtaPackageService` 在 ThingsBoard DAO 模块 中承担资源与 OTA 持久化服务类型职责，核心目的是管理二进制资源、图片、OTA 包元数据和关联实体的数据库访问。
 * 2. 核心流程：校验租户和资源归属后保存元数据或读取内容引用，必要时同步缓存和设备配置。
 * 3. 关键依赖：主要依赖或协作对象包括ResourceService、ImageService、OtaPackageService、缓存、存储服务和设备配置流程。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
