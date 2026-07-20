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
package org.thingsboard.server.dao.attributes;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.dao.service.Validator;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.thingsboard.server.dao.attributes.AttributeUtils.validate;

/**
 * @author Andrew Shvayka
 */
/**
 * 中文说明：
 * 1. `BaseAttributesService` 是 ThingsBoard DAO 中负责 `Attributes` 的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `AttributesService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Service
@ConditionalOnProperty(prefix = "cache.attributes", value = "enabled", havingValue = "false", matchIfMissing = true)
@Primary
@Slf4j
public class BaseAttributesService implements AttributesService {
    /**
     * 存取组件，用于读取或保存对应领域对象。
     */
    private final AttributesDao attributesDao;

    /**
     * 是否满足值条件。
     */
    @Value("${sql.attributes.value_no_xss_validation:false}")
    private boolean valueNoXssValidation;

    /**
     * 功能：创建 `BaseAttributesService` 实例，并初始化必要字段。
     * 参数：
     * - `attributesDao`：`attributesDao` 参数。
     * 返回：新创建的对象实例。
     */
    public BaseAttributesService(AttributesDao attributesDao) {
        this.attributesDao = attributesDao;
    }

    /**
     * 功能：执行 `find` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * - `attributeKey`：键。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<Optional<AttributeKvEntry>> find(TenantId tenantId, EntityId entityId, String scope, String attributeKey) {
        validate(entityId, scope);
        Validator.validateString(attributeKey, "Incorrect attribute key " + attributeKey);
        return Futures.immediateFuture(attributesDao.find(tenantId, entityId, scope, attributeKey));
    }

    /**
     * 功能：执行 `find` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * - `attributeKeys`：键。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<List<AttributeKvEntry>> find(TenantId tenantId, EntityId entityId, String scope, Collection<String> attributeKeys) {
        validate(entityId, scope);
        attributeKeys.forEach(attributeKey -> Validator.validateString(attributeKey, "Incorrect attribute key " + attributeKey));
        return Futures.immediateFuture(attributesDao.find(tenantId, entityId, scope, attributeKeys));
    }

    /**
     * 功能：获取`All`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<List<AttributeKvEntry>> findAll(TenantId tenantId, EntityId entityId, String scope) {
        validate(entityId, scope);
        return Futures.immediateFuture(attributesDao.findAll(tenantId, entityId, scope));
    }

    /**
     * 功能：获取设备配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceProfileId`：设备配置ID。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<String> findAllKeysByDeviceProfileId(TenantId tenantId, DeviceProfileId deviceProfileId) {
        return attributesDao.findAllKeysByDeviceProfileId(tenantId, deviceProfileId);
    }

    /**
     * 功能：获取实体。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityType`：实体对象。
     * - `entityIds`：实体对象。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<String> findAllKeysByEntityIds(TenantId tenantId, EntityType entityType, List<EntityId> entityIds) {
        return attributesDao.findAllKeysByEntityIds(tenantId, entityType, entityIds);
    }

    /**
     * 功能：获取实体。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityType`：实体对象。
     * - `entityIds`：实体对象。
     * - `scope`：`scope` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<String> findAllKeysByEntityIds(TenantId tenantId, EntityType entityType, List<EntityId> entityIds, String scope) {
        if (StringUtils.isEmpty(scope)) {
            return attributesDao.findAllKeysByEntityIds(tenantId, entityType, entityIds);
        } else {
            return attributesDao.findAllKeysByEntityIdsAndAttributeType(tenantId, entityType, entityIds, scope);
        }
    }

    /**
     * 功能：执行 `save` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * - `attribute`：`attribute` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<String> save(TenantId tenantId, EntityId entityId, String scope, AttributeKvEntry attribute) {
        validate(entityId, scope);
        AttributeUtils.validate(attribute, valueNoXssValidation);
        return attributesDao.save(tenantId, entityId, scope, attribute);
    }

    /**
     * 功能：执行 `save` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * - `attributes`：数据列表。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<List<String>> save(TenantId tenantId, EntityId entityId, String scope, List<AttributeKvEntry> attributes) {
        validate(entityId, scope);
        AttributeUtils.validate(attributes, valueNoXssValidation);
        List<ListenableFuture<String>> saveFutures = attributes.stream().map(attribute -> attributesDao.save(tenantId, entityId, scope, attribute)).collect(Collectors.toList());
        return Futures.allAsList(saveFutures);
    }

    /**
     * 功能：删除或清理`All`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * - `attributeKeys`：键。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<List<String>> removeAll(TenantId tenantId, EntityId entityId, String scope, List<String> attributeKeys) {
        validate(entityId, scope);
        return Futures.allAsList(attributesDao.removeAll(tenantId, entityId, scope, attributeKeys));
    }
}
