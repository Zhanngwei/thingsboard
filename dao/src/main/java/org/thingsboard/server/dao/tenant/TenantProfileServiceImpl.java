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
package org.thingsboard.server.dao.tenant;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.service.Validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.thingsboard.server.dao.service.Validator.validateId;

@Service("TenantProfileDaoService")
@Slf4j
/**
 * 中文说明：
 * 1. 类目的：`TenantProfileServiceImpl` 是 ThingsBoard DAO 模块 中的租户、客户或用户持久化服务类型，用于管理多租户边界内的组织、客户、用户、权限和配置数据访问。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括TenantService、CustomerService、UserService、Security、缓存、审计和 Application API。
 * 4. 生命周期：由管理 API、安全流程或后台任务调用，随请求事务完成并触发必要的缓存失效。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Service / Repository。
 */
public class TenantProfileServiceImpl extends AbstractCachedEntityService<TenantProfileCacheKey, TenantProfile, TenantProfileEvictEvent> implements TenantProfileService {

    /**
     * 字段说明：
     * 1. 保存 `INCORRECT_TENANT_PROFILE_ID` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    private static final String INCORRECT_TENANT_PROFILE_ID = "Incorrect tenantProfileId ";

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `tenantProfileDao` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    private TenantProfileDao tenantProfileDao;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `tenantProfileValidator` 对应的 DAO 依赖、Repository、缓存、配置、上下文或测试状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、数据库查询结果、缓存事件或测试夹具。
     * 3. 生命周期与持有对象一致：单例 Bean 字段随 Spring 容器存在，查询/测试字段随单次调用或测试用例存在。
     * 4. 设计为字段是为了复用数据库访问组件、缓存组件或上下文，减少重复查找和跨方法参数传递。
     * 5. 线程安全取决于字段类型；Repository、DAO Bean 通常由 Spring 管理，可变集合或异步状态需要调用方保证并发边界。
     */
    private DataValidator<TenantProfile> tenantProfileValidator;

    @TransactionalEventListener(classes = TenantProfileEvictEvent.class)
    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `handleEvictEvent` 对应的租户、客户或用户持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由管理 API、安全流程或后台任务调用，随请求事务完成并触发必要的缓存失效时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：校验租户/客户/用户作用域后执行数据库读写，并把结果返回给 Web 或后台服务。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void handleEvictEvent(TenantProfileEvictEvent event) {
        // 缓存读写或失效用于降低重复数据库访问成本，必须和实体变更顺序保持一致。
        List<TenantProfileCacheKey> keys = new ArrayList<>(2);
        // 审计或事件记录用于保留业务变更轨迹，便于后续查询、告警或外部系统消费。
        if (event.getTenantProfileId() != null) {
            // 缓存读写或失效用于降低重复数据库访问成本，必须和实体变更顺序保持一致。
            keys.add(TenantProfileCacheKey.fromId(event.getTenantProfileId()));
        }
        // 审计或事件记录用于保留业务变更轨迹，便于后续查询、告警或外部系统消费。
        if (event.isDefaultProfile()) {
            // 缓存读写或失效用于降低重复数据库访问成本，必须和实体变更顺序保持一致。
            keys.add(TenantProfileCacheKey.defaultProfile());
        }
        // 缓存读写或失效用于降低重复数据库访问成本，必须和实体变更顺序保持一致。
        cache.evict(keys);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `findTenantProfileById` 对应的租户、客户或用户持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由管理 API、安全流程或后台任务调用，随请求事务完成并触发必要的缓存失效时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：校验租户/客户/用户作用域后执行数据库读写，并把结果返回给 Web 或后台服务。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public TenantProfile findTenantProfileById(TenantId tenantId, TenantProfileId tenantProfileId) {
        log.trace("Executing findTenantProfileById [{}]", tenantProfileId);
        Validator.validateId(tenantProfileId, INCORRECT_TENANT_PROFILE_ID + tenantProfileId);
        // 事务边界决定本次数据库写入与缓存失效是否作为一个原子业务步骤提交。
        return cache.getAndPutInTransaction(TenantProfileCacheKey.fromId(tenantProfileId),
                // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
                () -> tenantProfileDao.findById(tenantId, tenantProfileId.getId()), true);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `findTenantProfileInfoById` 对应的租户、客户或用户持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由管理 API、安全流程或后台任务调用，随请求事务完成并触发必要的缓存失效时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：校验租户/客户/用户作用域后执行数据库读写，并把结果返回给 Web 或后台服务。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public EntityInfo findTenantProfileInfoById(TenantId tenantId, TenantProfileId tenantProfileId) {
        log.trace("Executing findTenantProfileInfoById [{}]", tenantProfileId);
        TenantProfile profile = findTenantProfileById(tenantId, tenantProfileId);
        return profile == null ? null : new EntityInfo(profile.getId(), profile.getName());
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `saveTenantProfile` 对应的租户、客户或用户持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由管理 API、安全流程或后台任务调用，随请求事务完成并触发必要的缓存失效时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：校验租户/客户/用户作用域后执行数据库读写，并把结果返回给 Web 或后台服务。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public TenantProfile saveTenantProfile(TenantId tenantId, TenantProfile tenantProfile) {
        log.trace("Executing saveTenantProfile [{}]", tenantProfile);
        tenantProfileValidator.validate(tenantProfile, (tenantProfile1) -> TenantId.SYS_TENANT_ID);
        TenantProfile savedTenantProfile;
        try {
            // DAO 委派用于复用底层持久化实现，上层方法只保留领域校验和流程编排职责。
            savedTenantProfile = tenantProfileDao.save(tenantId, tenantProfile);
            // 缓存读写或失效用于降低重复数据库访问成本，必须和实体变更顺序保持一致。
            publishEvictEvent(new TenantProfileEvictEvent(savedTenantProfile.getId(), savedTenantProfile.isDefault()));
            // 审计或事件记录用于保留业务变更轨迹，便于后续查询、告警或外部系统消费。
            eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(tenantId)
                    .entityId(savedTenantProfile.getId()).created(tenantProfile.getId() == null).build());
        // 异常在这里被转换为 DAO 层统一失败路径，避免数据库或底层驱动异常直接泄漏给上层调用方。
        } catch (Exception t) {
            // 缓存读写或失效用于降低重复数据库访问成本，必须和实体变更顺序保持一致。
            handleEvictEvent(new TenantProfileEvictEvent(null, tenantProfile.isDefault()));
            ConstraintViolationException e = extractConstraintViolationException(t).orElse(null);
            // 条件分支用于保护租户、实体状态、参数合法性或数据库结果边界，避免无效数据继续流转。
            if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("tenant_profile_name_unq_key")) {
                throw new DataValidationException("Tenant profile with such name already exists!");
            } else {
                throw t;
            }
        }
        return savedTenantProfile;
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `deleteTenantProfile` 对应的租户、客户或用户持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由管理 API、安全流程或后台任务调用，随请求事务完成并触发必要的缓存失效时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：校验租户/客户/用户作用域后执行数据库读写，并把结果返回给 Web 或后台服务。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void deleteTenantProfile(TenantId tenantId, TenantProfileId tenantProfileId) {
        log.trace("Executing deleteTenantProfile [{}]", tenantProfileId);
        validateId(tenantId, INCORRECT_TENANT_PROFILE_ID + tenantProfileId);
        TenantProfile tenantProfile = tenantProfileDao.findById(tenantId, tenantProfileId.getId());
        if (tenantProfile != null && tenantProfile.isDefault()) {
            throw new DataValidationException("Deletion of Default Tenant Profile is prohibited!");
        }
        this.removeTenantProfile(tenantId, tenantProfileId, false);
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `removeTenantProfile` 对应的租户、客户或用户持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由管理 API、安全流程或后台任务调用，随请求事务完成并触发必要的缓存失效时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：校验租户/客户/用户作用域后执行数据库读写，并把结果返回给 Web 或后台服务。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    private void removeTenantProfile(TenantId tenantId, TenantProfileId tenantProfileId, boolean isDefault) {
        try {
            tenantProfileDao.removeById(tenantId, tenantProfileId.getId());
        } catch (Exception t) {
            ConstraintViolationException e = extractConstraintViolationException(t).orElse(null);
            if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("fk_tenant_profile")) {
                throw new DataValidationException("The tenant profile referenced by the tenants cannot be deleted!");
            } else {
                throw t;
            }
        }
        deleteEntityRelations(tenantId, tenantProfileId);
        publishEvictEvent(new TenantProfileEvictEvent(tenantProfileId, isDefault));
        eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(tenantProfileId).build());
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `findTenantProfiles` 对应的租户、客户或用户持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由管理 API、安全流程或后台任务调用，随请求事务完成并触发必要的缓存失效时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：校验租户/客户/用户作用域后执行数据库读写，并把结果返回给 Web 或后台服务。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public PageData<TenantProfile> findTenantProfiles(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findTenantProfiles pageLink [{}]", pageLink);
        Validator.validatePageLink(pageLink);
        return tenantProfileDao.findTenantProfiles(tenantId, pageLink);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `findTenantProfileInfos` 对应的租户、客户或用户持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由管理 API、安全流程或后台任务调用，随请求事务完成并触发必要的缓存失效时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：校验租户/客户/用户作用域后执行数据库读写，并把结果返回给 Web 或后台服务。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public PageData<EntityInfo> findTenantProfileInfos(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findTenantProfileInfos pageLink [{}]", pageLink);
        Validator.validatePageLink(pageLink);
        return tenantProfileDao.findTenantProfileInfos(tenantId, pageLink);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `findOrCreateDefaultTenantProfile` 对应的租户、客户或用户持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由管理 API、安全流程或后台任务调用，随请求事务完成并触发必要的缓存失效时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：校验租户/客户/用户作用域后执行数据库读写，并把结果返回给 Web 或后台服务。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public TenantProfile findOrCreateDefaultTenantProfile(TenantId tenantId) {
        log.trace("Executing findOrCreateDefaultTenantProfile");
        TenantProfile defaultTenantProfile = findDefaultTenantProfile(tenantId);
        if (defaultTenantProfile == null) {
            defaultTenantProfile = new TenantProfile();
            defaultTenantProfile.setDefault(true);
            defaultTenantProfile.setName("Default");
            TenantProfileData profileData = new TenantProfileData();
            profileData.setConfiguration(new DefaultTenantProfileConfiguration());
            defaultTenantProfile.setProfileData(profileData);
            defaultTenantProfile.setDescription("Default tenant profile");
            defaultTenantProfile.setIsolatedTbRuleEngine(false);
            defaultTenantProfile = saveTenantProfile(tenantId, defaultTenantProfile);
        }
        return defaultTenantProfile;
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `findDefaultTenantProfile` 对应的租户、客户或用户持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由管理 API、安全流程或后台任务调用，随请求事务完成并触发必要的缓存失效时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：校验租户/客户/用户作用域后执行数据库读写，并把结果返回给 Web 或后台服务。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public TenantProfile findDefaultTenantProfile(TenantId tenantId) {
        log.trace("Executing findDefaultTenantProfile");
        return cache.getAndPutInTransaction(TenantProfileCacheKey.defaultProfile(),
                () -> tenantProfileDao.findDefaultTenantProfile(tenantId), true);

    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `findDefaultTenantProfileInfo` 对应的租户、客户或用户持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由管理 API、安全流程或后台任务调用，随请求事务完成并触发必要的缓存失效时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：校验租户/客户/用户作用域后执行数据库读写，并把结果返回给 Web 或后台服务。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public EntityInfo findDefaultTenantProfileInfo(TenantId tenantId) {
        log.trace("Executing findDefaultTenantProfileInfo");
        var tenantProfile = findDefaultTenantProfile(tenantId);
        return tenantProfile == null ? null : new EntityInfo(tenantProfile.getId(), tenantProfile.getName());
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `setDefaultTenantProfile` 对应的租户、客户或用户持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由管理 API、安全流程或后台任务调用，随请求事务完成并触发必要的缓存失效时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：校验租户/客户/用户作用域后执行数据库读写，并把结果返回给 Web 或后台服务。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public boolean setDefaultTenantProfile(TenantId tenantId, TenantProfileId tenantProfileId) {
        log.trace("Executing setDefaultTenantProfile [{}]", tenantProfileId);
        validateId(tenantId, INCORRECT_TENANT_PROFILE_ID + tenantProfileId);
        TenantProfile tenantProfile = tenantProfileDao.findById(tenantId, tenantProfileId.getId());
        if (!tenantProfile.isDefault()) {
            tenantProfile.setDefault(true);
            TenantProfile previousDefaultTenantProfile = findDefaultTenantProfile(tenantId);
            boolean changed = false;
            if (previousDefaultTenantProfile == null) {
                tenantProfileDao.save(tenantId, tenantProfile);
                publishEvictEvent(new TenantProfileEvictEvent(tenantProfileId, true));
                changed = true;
            } else if (!previousDefaultTenantProfile.getId().equals(tenantProfile.getId())) {
                previousDefaultTenantProfile.setDefault(false);
                tenantProfileDao.save(tenantId, previousDefaultTenantProfile);
                tenantProfileDao.save(tenantId, tenantProfile);
                publishEvictEvent(new TenantProfileEvictEvent(previousDefaultTenantProfile.getId(), false));
                publishEvictEvent(new TenantProfileEvictEvent(tenantProfileId, true));
                changed = true;
            }
            return changed;
        }
        return false;
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `findTenantProfilesByIds` 对应的租户、客户或用户持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由管理 API、安全流程或后台任务调用，随请求事务完成并触发必要的缓存失效时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：校验租户/客户/用户作用域后执行数据库读写，并把结果返回给 Web 或后台服务。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public List<TenantProfile> findTenantProfilesByIds(TenantId tenantId, UUID[] ids) {
        return tenantProfileDao.findTenantProfilesByIds(tenantId, ids);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `deleteTenantProfiles` 对应的租户、客户或用户持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由管理 API、安全流程或后台任务调用，随请求事务完成并触发必要的缓存失效时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：校验租户/客户/用户作用域后执行数据库读写，并把结果返回给 Web 或后台服务。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public void deleteTenantProfiles(TenantId tenantId) {
        log.trace("Executing deleteTenantProfiles");
        tenantProfilesRemover.removeEntities(tenantId, null);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `findEntity` 对应的租户、客户或用户持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由管理 API、安全流程或后台任务调用，随请求事务完成并触发必要的缓存失效时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：校验租户/客户/用户作用域后执行数据库读写，并把结果返回给 Web 或后台服务。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findTenantProfileById(tenantId, new TenantProfileId(entityId.getId())));
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `getEntityType` 对应的租户、客户或用户持久化服务类型流程，完成参数校验、作用域判断、缓存处理、数据库访问或测试断言。
     * 2. 参数：输入参数通常代表租户、客户、实体标识、查询条件、分页信息、领域 DTO、回调句柄或测试数据。
     * 3. 返回值：返回持久化实体、DTO、分页结果、异步句柄、布尔状态或 `void`；`void` 方法通常通过数据库副作用、缓存失效、事件或断言表达结果。
     * 4. 调用时机：由管理 API、安全流程或后台任务调用，随请求事务完成并触发必要的缓存失效时，由 Application 服务、DAO Service、Repository、定时任务、Rule Engine 相关服务或测试框架调用。
     * 5. 使用流程：校验租户/客户/用户作用域后执行数据库读写，并把结果返回给 Web 或后台服务。
     * 6. 线程安全：方法本身不额外声明线程安全；单例 DAO 依赖 Spring、数据库连接池、事务管理器和不可变参数约束并发行为。
     * 7. 事务：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；有 `@Transactional` 或服务层事务时参与同一事务，否则按底层 DAO/Repository 调用语义执行。
     * 8. 缓存：是否涉及缓存取决于方法体中的 cache、evict、Redis、Caffeine 或缓存服务调用。
     * 9. MQTT/Actor/数据库/Rule Engine：方法通常直接涉及数据库，通常不直接处理 MQTT/Actor；设备、遥测、属性或规则链数据会被 Transport、Actor 和 Rule Engine 间接使用。
     */
    public EntityType getEntityType() {
        return EntityType.TENANT_PROFILE;
    }

    private final PaginatedRemover<String, TenantProfile> tenantProfilesRemover =
            new PaginatedRemover<>() {

                @Override
                protected PageData<TenantProfile> findEntities(TenantId tenantId, String id, PageLink pageLink) {
                    return tenantProfileDao.findTenantProfiles(tenantId, pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, TenantProfile entity) {
                    removeTenantProfile(tenantId, entity.getId(), entity.isDefault());
                }
            };

}

/*
 * 本类总结：
 * 1. 核心职责：`TenantProfileServiceImpl` 在 ThingsBoard DAO 模块 中承担租户、客户或用户持久化服务类型职责，核心目的是管理多租户边界内的组织、客户、用户、权限和配置数据访问。
 * 2. 核心流程：校验租户/客户/用户作用域后执行数据库读写，并把结果返回给 Web 或后台服务。
 * 3. 关键依赖：主要依赖或协作对象包括TenantService、CustomerService、UserService、Security、缓存、审计和 Application API。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
