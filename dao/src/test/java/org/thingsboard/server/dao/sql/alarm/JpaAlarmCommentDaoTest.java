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
package org.thingsboard.server.dao.sql.alarm;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.alarm.AlarmCommentType;
import org.thingsboard.server.common.data.id.AlarmCommentId;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.alarm.AlarmCommentDao;
import org.thingsboard.server.dao.alarm.AlarmDao;

import java.util.UUID;

import static org.junit.Assert.assertEquals;

/**
 * 中文说明：
 * 1. 类目的：`JpaAlarmCommentDaoTest` 是 ThingsBoard DAO 测试模块 中的SQL/JPA 持久化实现类型，用于把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 生命周期：由 Spring 容器创建为 SQL DAO 或 Repository Bean，随事务上下文执行查询、保存、删除和分页读取。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Repository / DAO / Adapter。
 */
@Slf4j
public class JpaAlarmCommentDaoTest extends AbstractJpaDaoTest {

    /**
     * 告警，用于读取或保存对应领域对象。
     */
    @Autowired
    private AlarmCommentDao alarmCommentDao;
    /**
     * 告警，用于读取或保存对应领域对象。
     */
    @Autowired
    private AlarmDao alarmDao;


    /**
     * 功能：验证告警ID相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindAlarmCommentsByAlarmId() {
        log.info("Current system time in millis = {}", System.currentTimeMillis());
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID alarmId1 = UUID.randomUUID();
        UUID alarmId2 = UUID.randomUUID();
        UUID commentId1 = UUID.randomUUID();
        UUID commentId2 = UUID.randomUUID();
        UUID commentId3 = UUID.randomUUID();
        saveAlarm(alarmId1, UUID.randomUUID(), UUID.randomUUID(), "TEST_ALARM");
        saveAlarm(alarmId2, UUID.randomUUID(), UUID.randomUUID(), "TEST_ALARM");

        saveAlarmComment(commentId1, alarmId1, userId, AlarmCommentType.OTHER);
        saveAlarmComment(commentId2, alarmId1, userId, AlarmCommentType.OTHER);
        saveAlarmComment(commentId3, alarmId2, userId, AlarmCommentType.OTHER);

        int count = alarmCommentDao.findAlarmComments(TenantId.fromUUID(tenantId), new AlarmId(alarmId1), new PageLink(10, 0)).getData().size();
        assertEquals(2, count);
    }

    /**
     * 功能：保存或创建告警。
     * 参数：
     * - `id`：`id`ID。
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * - `type`：类型。
     * 返回：无。
     */
    private void saveAlarm(UUID id, UUID tenantId, UUID deviceId, String type) {
        Alarm alarm = new Alarm();
        alarm.setId(new AlarmId(id));
        alarm.setTenantId(TenantId.fromUUID(tenantId));
        alarm.setOriginator(new DeviceId(deviceId));
        alarm.setType(type);
        alarm.setPropagate(true);
        alarm.setStartTs(System.currentTimeMillis());
        alarm.setEndTs(System.currentTimeMillis());
        alarmDao.save(TenantId.fromUUID(tenantId), alarm);
    }
    /**
     * 功能：保存或创建告警。
     * 参数：
     * - `id`：`id`ID。
     * - `alarmId`：告警IDID。
     * - `userId`：用户ID。
     * - `type`：类型。
     * 返回：无。
     */
    private void saveAlarmComment(UUID id, UUID alarmId, UUID userId, AlarmCommentType type) {
        AlarmComment alarmComment = new AlarmComment();
        alarmComment.setId(new AlarmCommentId(id));
        alarmComment.setAlarmId(TenantId.fromUUID(alarmId));
        alarmComment.setUserId(new UserId(userId));
        alarmComment.setType(type);
        alarmComment.setComment(JacksonUtil.newObjectNode().put("text", RandomStringUtils.randomAlphanumeric(10)));
        alarmCommentDao.save(TenantId.fromUUID(UUID.randomUUID()), alarmComment);
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`JpaAlarmCommentDaoTest` 在 ThingsBoard DAO 测试模块 中承担SQL/JPA 持久化实现类型职责，核心目的是把 DAO API 的领域操作落到 PostgreSQL、TimescaleDB 或 JPA Repository 的具体 SQL 访问路径。
 * 2. 核心流程：将领域查询参数转换为 Repository 或原生 SQL 调用，再把数据库记录映射回 Common 数据对象。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Data Repository、JPA Entity、Hibernate、PostgreSQL、TimescaleDB、缓存和领域服务。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
