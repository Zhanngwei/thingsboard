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
package org.thingsboard.server.service.ttl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.server.dao.audit.AuditLogDao;
import org.thingsboard.server.dao.sqlts.insert.sql.SqlPartitioningRepository;
import org.thingsboard.server.queue.discovery.PartitionService;

import java.util.concurrent.TimeUnit;

import static org.thingsboard.server.dao.model.ModelConstants.AUDIT_LOG_TABLE_NAME;

/**
 * 中文说明：
 * 1. 类目的：`AuditLogsCleanUpService` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
@Service
@ConditionalOnExpression("${sql.ttl.audit_logs.enabled:true} && ${sql.ttl.audit_logs.ttl:0} > 0")
@Slf4j
public class AuditLogsCleanUpService extends AbstractCleanUpService {

    /**
     * 存取组件，用于读取或保存对应领域对象。
     */
    private final AuditLogDao auditLogDao;
    private final SqlPartitioningRepository partitioningRepository;

    /**
     * `ttlInSec` 字段，保存当前对象的对应属性。
     */
    @Value("${sql.ttl.audit_logs.ttl:0}")
    private long ttlInSec;
    /**
     * 分区，用于定位消息或数据所属分区。
     */
    @Value("${sql.audit_logs.partition_size:168}")
    private int partitionSizeInHours;

    /**
     * 功能：创建 `AuditLogsCleanUpService` 实例，并初始化必要字段。
     * 参数：
     * - `partitionService`：服务对象。
     * - `auditLogDao`：`auditLogDao` 参数。
     * - `partitioningRepository`：分区标识或分区信息。
     * 返回：新创建的对象实例。
     */
    public AuditLogsCleanUpService(PartitionService partitionService, AuditLogDao auditLogDao, SqlPartitioningRepository partitioningRepository) {
        super(partitionService);
        this.auditLogDao = auditLogDao;
        this.partitioningRepository = partitioningRepository;
    }

    /**
     * 功能：删除或清理`Up`。
     * 参数：无。
     * 返回：无。
     */
    @Scheduled(initialDelayString = "#{T(org.apache.commons.lang3.RandomUtils).nextLong(0, ${sql.ttl.audit_logs.checking_interval_ms})}",
            fixedDelayString = "${sql.ttl.audit_logs.checking_interval_ms}")
    public void cleanUp() {
        long auditLogsExpTime = System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(ttlInSec);
        if (isSystemTenantPartitionMine()) {
            auditLogDao.cleanUpAuditLogs(auditLogsExpTime);
        } else {
            partitioningRepository.cleanupPartitionsCache(AUDIT_LOG_TABLE_NAME, auditLogsExpTime, TimeUnit.HOURS.toMillis(partitionSizeInHours));
        }
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`AuditLogsCleanUpService` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
