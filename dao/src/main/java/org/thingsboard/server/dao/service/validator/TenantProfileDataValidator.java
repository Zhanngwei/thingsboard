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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.queue.ProcessingStrategy;
import org.thingsboard.server.common.data.queue.SubmitStrategy;
import org.thingsboard.server.common.data.queue.SubmitStrategyType;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileQueueConfiguration;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantProfileDao;
import org.thingsboard.server.dao.tenant.TenantProfileService;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 中文说明：
 * 1. 类目的：`TenantProfileDataValidator` 是 ThingsBoard DAO 模块 中的DAO 服务测试或服务支撑类型，用于组织 DAO 层测试、共享服务夹具或持久化服务的公共执行流程。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Spring Test、DAO Service、SQL/NoSQL DAO、缓存、事务管理器和测试容器。
 * 4. 生命周期：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Template Method / Service。
 */
@Component
public class TenantProfileDataValidator extends DataValidator<TenantProfile> {

    /**
     * 租户，用于读取或保存对应领域对象。
     */
    @Autowired
    private TenantProfileDao tenantProfileDao;

    /**
     * 租户，提供当前类调用的业务操作。
     */
    @Autowired
    @Lazy
    private TenantProfileService tenantProfileService;

    /**
     * 功能：校验数据。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `tenantProfile`：租户信息或租户标识。
     * 返回：无。
     */
    @Override
    protected void validateDataImpl(TenantId tenantId, TenantProfile tenantProfile) {
        validateString("Tenant profile name", tenantProfile.getName());
        if (tenantProfile.getProfileData() == null) {
            throw new DataValidationException("Tenant profile data should be specified!");
        }
        if (tenantProfile.getProfileData().getConfiguration() == null) {
            throw new DataValidationException("Tenant profile data configuration should be specified!");
        }
        if (tenantProfile.isDefault()) {
            TenantProfile defaultTenantProfile = tenantProfileService.findDefaultTenantProfile(tenantId);
            if (defaultTenantProfile != null && !defaultTenantProfile.getId().equals(tenantProfile.getId())) {
                throw new DataValidationException("Another default tenant profile is present!");
            }
        }

        if (tenantProfile.isIsolatedTbRuleEngine()) {
            List<TenantProfileQueueConfiguration> queueConfiguration = tenantProfile.getProfileData().getQueueConfiguration();
            if (queueConfiguration == null) {
                throw new DataValidationException("Tenant profile data queue configuration should be specified!");
            }

            Optional<TenantProfileQueueConfiguration> mainQueueConfig =
                    queueConfiguration
                            .stream()
                            .filter(q -> q.getName().equals(DataConstants.MAIN_QUEUE_NAME))
                            .findAny();
            if (mainQueueConfig.isEmpty()) {
                throw new DataValidationException("Main queue configuration should be specified!");
            }

            queueConfiguration.forEach(this::validateQueueConfiguration);

            Set<String> queueNames = new HashSet<>(queueConfiguration.size());

            queueConfiguration.forEach(q -> {
                String name = q.getName();
                if (queueNames.contains(name)) {
                    throw new DataValidationException(String.format("Queue configuration name '%s' already present!", name));
                } else {
                    queueNames.add(name);
                }
            });
        }
    }

    /**
     * 功能：校验`Update`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `tenantProfile`：租户信息或租户标识。
     * 返回：判断结果。
     */
    @Override
    protected TenantProfile validateUpdate(TenantId tenantId, TenantProfile tenantProfile) {
        TenantProfile old = tenantProfileDao.findById(TenantId.SYS_TENANT_ID, tenantProfile.getId().getId());
        if (old == null) {
            throw new DataValidationException("Can't update non existing tenant profile!");
        }
        return old;
    }

    /**
     * 功能：校验队列。
     * 参数：
     * - `queue`：队列名称或队列对象。
     * 返回：无。
     */
    private void validateQueueConfiguration(TenantProfileQueueConfiguration queue) {
        validateQueueName(queue.getName());
        validateQueueTopic(queue.getTopic());

        if (queue.getPollInterval() < 1) {
            throw new DataValidationException("Queue poll interval should be more then 0!");
        }
        if (queue.getPartitions() < 1) {
            throw new DataValidationException("Queue partitions should be more then 0!");
        }
        if (queue.getPackProcessingTimeout() < 1) {
            throw new DataValidationException("Queue pack processing timeout should be more then 0!");
        }

        SubmitStrategy submitStrategy = queue.getSubmitStrategy();
        if (submitStrategy == null) {
            throw new DataValidationException("Queue submit strategy can't be null!");
        }
        if (submitStrategy.getType() == null) {
            throw new DataValidationException("Queue submit strategy type can't be null!");
        }
        if (submitStrategy.getType() == SubmitStrategyType.BATCH && submitStrategy.getBatchSize() < 1) {
            throw new DataValidationException("Queue submit strategy batch size should be more then 0!");
        }
        ProcessingStrategy processingStrategy = queue.getProcessingStrategy();
        if (processingStrategy == null) {
            throw new DataValidationException("Queue processing strategy can't be null!");
        }
        if (processingStrategy.getType() == null) {
            throw new DataValidationException("Queue processing strategy type can't be null!");
        }
        if (processingStrategy.getRetries() < 0) {
            throw new DataValidationException("Queue processing strategy retries can't be less then 0!");
        }
        if (processingStrategy.getFailurePercentage() < 0 || processingStrategy.getFailurePercentage() > 100) {
            throw new DataValidationException("Queue processing strategy failure percentage should be in a range from 0 to 100!");
        }
        if (processingStrategy.getPauseBetweenRetries() < 0) {
            throw new DataValidationException("Queue processing strategy pause between retries can't be less then 0!");
        }
        if (processingStrategy.getMaxPauseBetweenRetries() < processingStrategy.getPauseBetweenRetries()) {
            throw new DataValidationException("Queue processing strategy MAX pause between retries can't be less then pause between retries!");
        }
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`TenantProfileDataValidator` 在 ThingsBoard DAO 模块 中承担DAO 服务测试或服务支撑类型职责，核心目的是组织 DAO 层测试、共享服务夹具或持久化服务的公共执行流程。
 * 2. 核心流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Test、DAO Service、SQL/NoSQL DAO、缓存、事务管理器和测试容器。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
