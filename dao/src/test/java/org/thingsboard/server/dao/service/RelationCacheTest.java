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
package org.thingsboard.server.dao.service;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.relation.RelationDao;
import org.thingsboard.server.dao.relation.RelationService;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.thingsboard.server.common.data.CacheConstants.RELATIONS_CACHE;

/**
 * 中文说明：
 * 1. 类目的：`RelationCacheTest` 是 ThingsBoard DAO 测试模块 中的DAO 服务测试或服务支撑类型，用于组织 DAO 层测试、共享服务夹具或持久化服务的公共执行流程。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Spring Test、DAO Service、SQL/NoSQL DAO、缓存、事务管理器和测试容器。
 * 4. 生命周期：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Template Method / Service。
 */
@DaoSqlTest
public class RelationCacheTest extends AbstractServiceTest {

    private static final EntityId ENTITY_ID_FROM = new DeviceId(UUID.randomUUID());
    private static final EntityId ENTITY_ID_TO = new DeviceId(UUID.randomUUID());
    /**
     * 关系常量，用于统一引用固定值。
     */
    private static final String RELATION_TYPE = "Contains";

    /**
     * 关系，提供当前类调用的业务操作。
     */
    @Autowired
    private RelationService relationService;
    /**
     * 管理器，负责处理对应任务或消息。
     */
    @Autowired
    private CacheManager cacheManager;

    /**
     * 关系，用于读取或保存对应领域对象。
     */
    private RelationDao relationDao;

    /**
     * 功能：初始化当前测试或组件需要的对象。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void setup() throws Exception {
        relationDao = mock(RelationDao.class);
        ReflectionTestUtils.setField(unwrapRelationService(), "relationDao", relationDao);
    }

    /**
     * 功能：执行 `cleanup` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @After
    public void cleanup() {
        cacheManager.getCache(RELATIONS_CACHE).clear();
    }

    /**
     * 功能：执行 `unwrapRelationService` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    private RelationService unwrapRelationService() throws Exception {
        if (AopUtils.isAopProxy(relationService) && relationService instanceof Advised) {
            Object target = ((Advised) relationService).getTargetSource().getTarget();
            return (RelationService) target;
        }
        return relationService;
    }

    /**
     * 功能：验证关系相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindRelationByFrom_Cached() throws ExecutionException, InterruptedException {
        when(relationDao.getRelation(SYSTEM_TENANT_ID, ENTITY_ID_FROM, ENTITY_ID_TO, RELATION_TYPE, RelationTypeGroup.COMMON))
                .thenReturn(new EntityRelation(ENTITY_ID_FROM, ENTITY_ID_TO, RELATION_TYPE));

        relationService.getRelation(SYSTEM_TENANT_ID, ENTITY_ID_FROM, ENTITY_ID_TO, RELATION_TYPE, RelationTypeGroup.COMMON);
        relationService.getRelation(SYSTEM_TENANT_ID, ENTITY_ID_FROM, ENTITY_ID_TO, RELATION_TYPE, RelationTypeGroup.COMMON);

        verify(relationDao, times(1)).getRelation(SYSTEM_TENANT_ID, ENTITY_ID_FROM, ENTITY_ID_TO, RELATION_TYPE, RelationTypeGroup.COMMON);
    }

    /**
     * 功能：验证`Delete Relations Evicts Cache`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testDeleteRelations_EvictsCache() {
        when(relationDao.getRelation(SYSTEM_TENANT_ID, ENTITY_ID_FROM, ENTITY_ID_TO, RELATION_TYPE, RelationTypeGroup.COMMON))
                .thenReturn(new EntityRelation(ENTITY_ID_FROM, ENTITY_ID_TO, RELATION_TYPE));

        relationService.getRelation(SYSTEM_TENANT_ID, ENTITY_ID_FROM, ENTITY_ID_TO, RELATION_TYPE, RelationTypeGroup.COMMON);
        relationService.getRelation(SYSTEM_TENANT_ID, ENTITY_ID_FROM, ENTITY_ID_TO, RELATION_TYPE, RelationTypeGroup.COMMON);

        verify(relationDao, times(1)).getRelation(SYSTEM_TENANT_ID, ENTITY_ID_FROM, ENTITY_ID_TO, RELATION_TYPE, RelationTypeGroup.COMMON);

        relationService.deleteRelation(SYSTEM_TENANT_ID, ENTITY_ID_FROM, ENTITY_ID_TO, RELATION_TYPE, RelationTypeGroup.COMMON);

        relationService.getRelation(SYSTEM_TENANT_ID, ENTITY_ID_FROM, ENTITY_ID_TO, RELATION_TYPE, RelationTypeGroup.COMMON);
        relationService.getRelation(SYSTEM_TENANT_ID, ENTITY_ID_FROM, ENTITY_ID_TO, RELATION_TYPE, RelationTypeGroup.COMMON);

        verify(relationDao, times(2)).getRelation(SYSTEM_TENANT_ID, ENTITY_ID_FROM, ENTITY_ID_TO, RELATION_TYPE, RelationTypeGroup.COMMON);
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`RelationCacheTest` 在 ThingsBoard DAO 测试模块 中承担DAO 服务测试或服务支撑类型职责，核心目的是组织 DAO 层测试、共享服务夹具或持久化服务的公共执行流程。
 * 2. 核心流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Test、DAO Service、SQL/NoSQL DAO、缓存、事务管理器和测试容器。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
