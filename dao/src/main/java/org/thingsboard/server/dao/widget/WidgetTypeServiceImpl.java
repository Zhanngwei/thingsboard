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
package org.thingsboard.server.dao.widget;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.widget.DeprecatedFilter;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.common.data.widget.WidgetTypeInfo;
import org.thingsboard.server.common.data.widget.WidgetsBundleWidget;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.resource.ImageService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.service.Validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.thingsboard.server.dao.service.Validator.validateIds;

/**
 * 中文说明：
 * 1. 类目的：`WidgetTypeServiceImpl` 是 ThingsBoard DAO 模块 中的持久化实现层类型，用于承载服务端实体、关系、属性、遥测、事件和配置数据的持久化访问实现。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括DAO API、Common 数据模型、Application 服务、Rule Engine、缓存、SQL/NoSQL 数据库和审计模块。
 * 4. 生命周期：由 Spring 容器创建为 DAO、Repository、Service 或配置 Bean，并随应用生命周期参与请求处理。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Repository / Service / Template。
 */
@Service("WidgetTypeDaoService")
@Slf4j
public class WidgetTypeServiceImpl implements WidgetTypeService {

    /**
     * 租户ID常量，用于统一引用固定值。
     */
    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_RESOURCE_ID = "Incorrect resourceId ";
    /**
     * 包常量，用于统一引用固定值。
     */
    public static final String INCORRECT_BUNDLE_ALIAS = "Incorrect bundleAlias ";
    public static final String INCORRECT_WIDGETS_BUNDLE_ID = "Incorrect widgetsBundleId ";

    /**
     * 部件类型，用于读取或保存对应领域对象。
     */
    @Autowired
    private WidgetTypeDao widgetTypeDao;

    /**
     * 部件类型，用于区分不同处理分支。
     */
    @Autowired
    private DataValidator<WidgetTypeDetails> widgetTypeValidator;

    /**
     * 事件，表示当前对象的对应属性。
     */
    @Autowired
    protected ApplicationEventPublisher eventPublisher;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    protected ImageService imageService;

    /**
     * 功能：获取部件类型。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `widgetTypeId`：部件类型ID。
     * 返回：处理结果。
     */
    @Override
    public WidgetType findWidgetTypeById(TenantId tenantId, WidgetTypeId widgetTypeId) {
        log.trace("Executing findWidgetTypeById [{}]", widgetTypeId);
        Validator.validateId(widgetTypeId, "Incorrect widgetTypeId " + widgetTypeId);
        return widgetTypeDao.findWidgetTypeById(tenantId, widgetTypeId.getId());
    }

    /**
     * 功能：获取部件类型。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `widgetTypeId`：部件类型ID。
     * 返回：处理结果。
     */
    @Override
    public WidgetTypeDetails findWidgetTypeDetailsById(TenantId tenantId, WidgetTypeId widgetTypeId) {
        log.trace("Executing findWidgetTypeDetailsById [{}]", widgetTypeId);
        Validator.validateId(widgetTypeId, "Incorrect widgetTypeId " + widgetTypeId);
        return widgetTypeDao.findById(tenantId, widgetTypeId.getId());
    }

    /**
     * 功能：执行 `widgetTypeExistsByTenantIdAndWidgetTypeId` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `widgetTypeId`：部件类型ID。
     * 返回：判断结果。
     */
    @Override
    public boolean widgetTypeExistsByTenantIdAndWidgetTypeId(TenantId tenantId, WidgetTypeId widgetTypeId) {
        log.trace("Executing widgetTypeExistsByTenantIdAndWidgetTypeId, tenantId [{}],  widgetTypeId [{}]", tenantId, widgetTypeId);
        Validator.validateId(widgetTypeId, "Incorrect widgetTypeId " + widgetTypeId);
        return widgetTypeDao.existsByTenantIdAndId(tenantId, widgetTypeId.getId());
    }

    /**
     * 功能：保存或创建部件类型。
     * 参数：
     * - `widgetTypeDetails`：类型。
     * 返回：处理结果。
     */
    @Override
    public WidgetTypeDetails saveWidgetType(WidgetTypeDetails widgetTypeDetails) {
        log.trace("Executing saveWidgetType [{}]", widgetTypeDetails);
        widgetTypeValidator.validate(widgetTypeDetails, WidgetType::getTenantId);
        try {
            imageService.replaceBase64WithImageUrl(widgetTypeDetails);
            WidgetTypeDetails result = widgetTypeDao.save(widgetTypeDetails.getTenantId(), widgetTypeDetails);
            eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(result.getTenantId())
                    .entityId(result.getId()).created(widgetTypeDetails.getId() == null).build());
            return result;
        } catch (Exception t) {
            AbstractCachedEntityService.checkConstraintViolation(t,
                    "uq_widget_type_fqn", "Widget type with such fqn already exists!");
            AbstractCachedEntityService.checkConstraintViolation(t, "widget_type_external_id_unq_key", "Widget type with such external id already exists!");
            throw t;
        }
    }

    /**
     * 功能：删除或清理部件类型。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `widgetTypeId`：部件类型ID。
     * 返回：无。
     */
    @Override
    public void deleteWidgetType(TenantId tenantId, WidgetTypeId widgetTypeId) {
        log.trace("Executing deleteWidgetType [{}]", widgetTypeId);
        Validator.validateId(widgetTypeId, "Incorrect widgetTypeId " + widgetTypeId);
        widgetTypeDao.removeById(tenantId, widgetTypeId.getId());
        eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(widgetTypeId).build());
    }

    /**
     * 功能：获取分页查询条件。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `fullSearch`：`fullSearch` 参数。
     * - `deprecatedFilter`：`deprecatedFilter` 参数。
     * - `widgetTypes`：类型。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<WidgetTypeInfo> findSystemWidgetTypesByPageLink(TenantId tenantId, boolean fullSearch, DeprecatedFilter deprecatedFilter, List<String> widgetTypes, PageLink pageLink) {
        log.trace("Executing findSystemWidgetTypesByPageLink, fullSearch [{}], deprecatedFilter [{}], widgetTypes [{}], pageLink [{}]", fullSearch, deprecatedFilter, widgetTypes, pageLink);
        Validator.validatePageLink(pageLink);
        return widgetTypeDao.findSystemWidgetTypes(tenantId, fullSearch, deprecatedFilter, widgetTypes, pageLink);
    }

    /**
     * 功能：获取分页查询条件。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `fullSearch`：`fullSearch` 参数。
     * - `deprecatedFilter`：`deprecatedFilter` 参数。
     * - `widgetTypes`：类型。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<WidgetTypeInfo> findAllTenantWidgetTypesByTenantIdAndPageLink(TenantId tenantId, boolean fullSearch, DeprecatedFilter deprecatedFilter, List<String> widgetTypes, PageLink pageLink) {
        log.trace("Executing findAllTenantWidgetTypesByTenantIdAndPageLink, tenantId [{}], fullSearch [{}], deprecatedFilter [{}], widgetTypes [{}], pageLink [{}]",
                tenantId, fullSearch, deprecatedFilter, widgetTypes, pageLink);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validatePageLink(pageLink);
        return widgetTypeDao.findAllTenantWidgetTypesByTenantId(tenantId.getId(), fullSearch, deprecatedFilter, widgetTypes, pageLink);
    }

    /**
     * 功能：获取分页查询条件。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `fullSearch`：`fullSearch` 参数。
     * - `deprecatedFilter`：`deprecatedFilter` 参数。
     * - `widgetTypes`：类型。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<WidgetTypeInfo> findTenantWidgetTypesByTenantIdAndPageLink(TenantId tenantId, boolean fullSearch, DeprecatedFilter deprecatedFilter, List<String> widgetTypes, PageLink pageLink) {
        log.trace("Executing findTenantWidgetTypesByTenantIdAndPageLink, tenantId [{}], fullSearch [{}], deprecatedFilter [{}], widgetTypes [{}], pageLink [{}]",
                tenantId, fullSearch, deprecatedFilter, widgetTypes, pageLink);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validatePageLink(pageLink);
        return widgetTypeDao.findTenantWidgetTypesByTenantId(tenantId.getId(), fullSearch, deprecatedFilter, widgetTypes, pageLink);
    }

    /**
     * 功能：获取部件包。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `widgetsBundleId`：部件包ID。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<WidgetType> findWidgetTypesByWidgetsBundleId(TenantId tenantId, WidgetsBundleId widgetsBundleId) {
        log.trace("Executing findWidgetTypesByWidgetsBundleId, tenantId [{}], widgetsBundleId [{}]", tenantId, widgetsBundleId);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validateId(widgetsBundleId, INCORRECT_WIDGETS_BUNDLE_ID + widgetsBundleId);
        return widgetTypeDao.findWidgetTypesByWidgetsBundleId(tenantId.getId(), widgetsBundleId.getId());
    }

    /**
     * 功能：获取部件包。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `widgetsBundleId`：部件包ID。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<WidgetTypeDetails> findWidgetTypesDetailsByWidgetsBundleId(TenantId tenantId, WidgetsBundleId widgetsBundleId) {
        log.trace("Executing findWidgetTypesDetailsByWidgetsBundleId, tenantId [{}], widgetsBundleId [{}]", tenantId, widgetsBundleId);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validateId(widgetsBundleId, INCORRECT_WIDGETS_BUNDLE_ID + widgetsBundleId);
        return widgetTypeDao.findWidgetTypesDetailsByWidgetsBundleId(tenantId.getId(), widgetsBundleId.getId());

    }

    /**
     * 功能：获取部件包。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `widgetsBundleId`：部件包ID。
     * - `fullSearch`：`fullSearch` 参数。
     * - `deprecatedFilter`：`deprecatedFilter` 参数。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<WidgetTypeInfo> findWidgetTypesInfosByWidgetsBundleId(TenantId tenantId, WidgetsBundleId widgetsBundleId, boolean fullSearch,
                                                                          DeprecatedFilter deprecatedFilter, List<String> widgetTypes, PageLink pageLink) {
        log.trace("Executing findWidgetTypesInfosByWidgetsBundleId, tenantId [{}], widgetsBundleId [{}], fullSearch [{}], deprecatedFilter [{}], widgetTypes [{}], pageLink [{}]",
                tenantId, widgetsBundleId, fullSearch, deprecatedFilter, widgetTypes, pageLink);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validateId(widgetsBundleId, INCORRECT_WIDGETS_BUNDLE_ID + widgetsBundleId);
        Validator.validatePageLink(pageLink);
        return widgetTypeDao.findWidgetTypesInfosByWidgetsBundleId(tenantId.getId(), widgetsBundleId.getId(), fullSearch, deprecatedFilter, widgetTypes, pageLink);
    }

    /**
     * 功能：获取部件包。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `widgetsBundleId`：部件包ID。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<String> findWidgetFqnsByWidgetsBundleId(TenantId tenantId, WidgetsBundleId widgetsBundleId) {
        log.trace("Executing findWidgetTypesInfosByWidgetsBundleId, tenantId [{}], widgetsBundleId [{}]", tenantId, widgetsBundleId);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validateId(widgetsBundleId, INCORRECT_WIDGETS_BUNDLE_ID + widgetsBundleId);
        return widgetTypeDao.findWidgetFqnsByWidgetsBundleId(tenantId.getId(), widgetsBundleId.getId());
    }

    /**
     * 功能：获取部件类型。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `fqn`：`fqn` 参数。
     * 返回：处理结果。
     */
    @Override
    public WidgetType findWidgetTypeByTenantIdAndFqn(TenantId tenantId, String fqn) {
        log.trace("Executing findWidgetTypeByTenantIdAndFqn, tenantId [{}], fqn [{}]", tenantId, fqn);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validateString(fqn, "Incorrect fqn " + fqn);
        return widgetTypeDao.findByTenantIdAndFqn(tenantId.getId(), fqn);
    }

    /**
     * 功能：更新部件包。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `widgetsBundleId`：部件包ID。
     * - `widgetTypeIds`：类型。
     * 返回：无。
     */
    @Override
    public void updateWidgetsBundleWidgetTypes(TenantId tenantId, WidgetsBundleId widgetsBundleId, List<WidgetTypeId> widgetTypeIds) {
        log.trace("Executing updateWidgetsBundleWidgetTypes, tenantId [{}], widgetsBundleId [{}], widgetTypeIds [{}]", tenantId, widgetsBundleId, widgetTypeIds);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validateId(widgetsBundleId, INCORRECT_WIDGETS_BUNDLE_ID + widgetsBundleId);
        Validator.checkNotNull(widgetTypeIds, "Incorrect widgetTypeIds " + widgetTypeIds);
        if (!widgetTypeIds.isEmpty()) {
            validateIds(widgetTypeIds, "Incorrect widgetTypeIds " + widgetTypeIds);
        }
        List<WidgetsBundleWidget> bundleWidgets = new ArrayList<>();
        for (int index = 0; index < widgetTypeIds.size(); index++) {
            bundleWidgets.add(new WidgetsBundleWidget(widgetsBundleId, widgetTypeIds.get(index), index));
        }
        List<WidgetsBundleWidget> existingBundleWidgets = widgetTypeDao.findWidgetsBundleWidgetsByWidgetsBundleId(tenantId.getId(), widgetsBundleId.getId());
        List<WidgetTypeId> toRemove = existingBundleWidgets.stream()
                .map(WidgetsBundleWidget::getWidgetTypeId)
                .filter(widgetTypeId -> bundleWidgets.stream().noneMatch(newBundleWidget ->
                        newBundleWidget.getWidgetTypeId().equals(widgetTypeId))).collect(Collectors.toList());
        for (WidgetTypeId widgetTypeId : toRemove) {
            widgetTypeDao.removeWidgetTypeFromWidgetsBundle(widgetsBundleId.getId(), widgetTypeId.getId());
        }
        for (WidgetsBundleWidget widgetsBundleWidget : bundleWidgets) {
            widgetTypeDao.saveWidgetsBundleWidget(widgetsBundleWidget);
        }
        eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(tenantId)
                .entityId(widgetsBundleId).created(false).build());
    }

    /**
     * 功能：更新部件包。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `widgetsBundleId`：部件包ID。
     * - `widgetFqns`：数据列表。
     * 返回：无。
     */
    @Override
    public void updateWidgetsBundleWidgetFqns(TenantId tenantId, WidgetsBundleId widgetsBundleId, List<String> widgetFqns) {
        log.trace("Executing updateWidgetsBundleWidgetFqns, tenantId [{}], widgetsBundleId [{}], widgetFqns [{}]", tenantId, widgetsBundleId, widgetFqns);
        List<WidgetTypeId> widgetTypeIds = widgetTypeDao.findWidgetTypeIdsByTenantIdAndFqns(tenantId.getId(), widgetFqns);
        this.updateWidgetsBundleWidgetTypes(tenantId, widgetsBundleId, widgetTypeIds);
    }

    /**
     * 功能：删除或清理租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    @Override
    public void deleteWidgetTypesByTenantId(TenantId tenantId) {
        log.trace("Executing deleteWidgetTypesByTenantId, tenantId [{}]", tenantId);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        tenantWidgetTypeRemover.removeEntities(tenantId, tenantId);
    }

    /**
     * 功能：获取实体。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * 返回：可能存在的结果。
     */
    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findWidgetTypeById(tenantId, new WidgetTypeId(entityId.getId())));
    }

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public EntityType getEntityType() {
        return EntityType.WIDGET_TYPE;
    }

    private PaginatedRemover<TenantId, WidgetTypeInfo> tenantWidgetTypeRemover =
            new PaginatedRemover<>() {

                @Override
                protected PageData<WidgetTypeInfo> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
                    return widgetTypeDao.findTenantWidgetTypesByTenantId(id.getId(), false, DeprecatedFilter.ALL, null, pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, WidgetTypeInfo entity) {
                    deleteWidgetType(tenantId, new WidgetTypeId(entity.getUuidId()));
                }
            };

}

/*
 * 本类总结：
 * 1. 核心职责：`WidgetTypeServiceImpl` 在 ThingsBoard DAO 模块 中承担持久化实现层类型职责，核心目的是承载服务端实体、关系、属性、遥测、事件和配置数据的持久化访问实现。
 * 2. 核心流程：接收上层服务的租户、实体和查询上下文，完成校验、缓存处理、数据库读写或测试断言后返回结果。
 * 3. 关键依赖：主要依赖或协作对象包括DAO API、Common 数据模型、Application 服务、Rule Engine、缓存、SQL/NoSQL 数据库和审计模块。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
