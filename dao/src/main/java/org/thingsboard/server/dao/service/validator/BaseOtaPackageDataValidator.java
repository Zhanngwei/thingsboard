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
package org.thingsboard.server.dao.service.validator;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.dao.device.DeviceProfileDao;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantService;

import java.util.Objects;

/**
 * 中文说明：
 * 1. 类目的：`BaseOtaPackageDataValidator` 是 ThingsBoard DAO 模块 中的DAO 服务测试或服务支撑类型，用于组织 DAO 层测试、共享服务夹具或持久化服务的公共执行流程。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Spring Test、DAO Service、SQL/NoSQL DAO、缓存、事务管理器和测试容器。
 * 4. 生命周期：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Template Method / Service。
 */
public abstract class BaseOtaPackageDataValidator<D extends BaseData<?>> extends DataValidator<D> {

    /**
     * 租户，提供当前类调用的业务操作。
     */
    @Autowired
    @Getter
    @Lazy
    private TenantService tenantService;

    /**
     * 设备配置，用于读取或保存对应领域对象。
     */
    @Autowired
    @Getter
    private DeviceProfileDao deviceProfileDao;

    /**
     * 功能：校验`Impl`。
     * 参数：
     * - `otaPackageInfo`：`otaPackageInfo` 参数。
     * 返回：无。
     */
    protected void validateImpl(OtaPackageInfo otaPackageInfo) {
        validateString("OtaPackage title", otaPackageInfo.getTitle());

        if (otaPackageInfo.getTenantId() == null) {
            throw new DataValidationException("OtaPackage should be assigned to tenant!");
        } else {
            if (!getTenantService().tenantExists(otaPackageInfo.getTenantId())) {
                throw new DataValidationException("OtaPackage is referencing to non-existent tenant!");
            }
        }

        if (otaPackageInfo.getDeviceProfileId() != null) {
            DeviceProfile deviceProfile = getDeviceProfileDao().findById(otaPackageInfo.getTenantId(), otaPackageInfo.getDeviceProfileId().getId());
            if (deviceProfile == null) {
                throw new DataValidationException("OtaPackage is referencing to non-existent device profile!");
            }
        }

        if (otaPackageInfo.getType() == null) {
            throw new DataValidationException("Type should be specified!");
        }

        if (StringUtils.isEmpty(otaPackageInfo.getVersion())) {
            throw new DataValidationException("OtaPackage version should be specified!");
        }

        if (otaPackageInfo.getTitle().length() > 255) {
            throw new DataValidationException("The length of title should be equal or shorter than 255");
        }

        if (otaPackageInfo.getVersion().length() > 255) {
            throw new DataValidationException("The length of version should be equal or shorter than 255");
        }
    }

    /**
     * 功能：校验`Update`。
     * 参数：
     * - `otaPackage`：`otaPackage` 参数。
     * - `otaPackageOld`：`otaPackageOld` 参数。
     * 返回：无。
     */
    protected void validateUpdate(OtaPackageInfo otaPackage, OtaPackageInfo otaPackageOld) {
        if (!otaPackageOld.getType().equals(otaPackage.getType())) {
            throw new DataValidationException("Updating type is prohibited!");
        }

        if (!otaPackageOld.getTitle().equals(otaPackage.getTitle())) {
            throw new DataValidationException("Updating otaPackage title is prohibited!");
        }

        if (!otaPackageOld.getVersion().equals(otaPackage.getVersion())) {
            throw new DataValidationException("Updating otaPackage version is prohibited!");
        }

        if (!Objects.equals(otaPackage.getTag(), otaPackageOld.getTag())) {
            throw new DataValidationException("Updating otaPackage tag is prohibited!");
        }

        if (!otaPackageOld.getDeviceProfileId().equals(otaPackage.getDeviceProfileId())) {
            throw new DataValidationException("Updating otaPackage deviceProfile is prohibited!");
        }

        if (otaPackageOld.getFileName() != null && !otaPackageOld.getFileName().equals(otaPackage.getFileName())) {
            throw new DataValidationException("Updating otaPackage file name is prohibited!");
        }

        if (otaPackageOld.getContentType() != null && !otaPackageOld.getContentType().equals(otaPackage.getContentType())) {
            throw new DataValidationException("Updating otaPackage content type is prohibited!");
        }

        if (otaPackageOld.getChecksumAlgorithm() != null && !otaPackageOld.getChecksumAlgorithm().equals(otaPackage.getChecksumAlgorithm())) {
            throw new DataValidationException("Updating otaPackage content type is prohibited!");
        }

        if (otaPackageOld.getChecksum() != null && !otaPackageOld.getChecksum().equals(otaPackage.getChecksum())) {
            throw new DataValidationException("Updating otaPackage content type is prohibited!");
        }

        if (otaPackageOld.getDataSize() != null && !otaPackageOld.getDataSize().equals(otaPackage.getDataSize())) {
            throw new DataValidationException("Updating otaPackage data size is prohibited!");
        }

        if (otaPackageOld.getUrl() != null && !otaPackageOld.getUrl().equals(otaPackage.getUrl())) {
            throw new DataValidationException("Updating otaPackage URL is prohibited!");
        }
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`BaseOtaPackageDataValidator` 在 ThingsBoard DAO 模块 中承担DAO 服务测试或服务支撑类型职责，核心目的是组织 DAO 层测试、共享服务夹具或持久化服务的公共执行流程。
 * 2. 核心流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Test、DAO Service、SQL/NoSQL DAO、缓存、事务管理器和测试容器。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
