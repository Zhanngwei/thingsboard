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
package org.thingsboard.server.dao.alarm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.alarm.AlarmCommentInfo;
import org.thingsboard.server.common.data.alarm.AlarmCommentType;
import org.thingsboard.server.common.data.id.AlarmCommentId;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.service.DataValidator;

import static org.thingsboard.server.dao.service.Validator.validateId;

/**
 * 中文说明：
 * 1. 类目的：`BaseAlarmCommentService` 是 ThingsBoard DAO 模块 中的持久化实现层类型，用于承载服务端实体、关系、属性、遥测、事件和配置数据的持久化访问实现。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括DAO API、Common 数据模型、Application 服务、Rule Engine、缓存、SQL/NoSQL 数据库和审计模块。
 * 4. 生命周期：由 Spring 容器创建为 DAO、Repository、Service 或配置 Bean，并随应用生命周期参与请求处理。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Repository / Service / Template。
 */
@Service
@Slf4j
public class BaseAlarmCommentService extends AbstractEntityService implements AlarmCommentService {

    /**
     * 告警，用于读取或保存对应领域对象。
     */
    @Autowired
    private AlarmCommentDao alarmCommentDao;

    /**
     * 告警，保存当前步骤读取或计算得到的内容。
     */
    @Autowired
    private DataValidator<AlarmComment> alarmCommentDataValidator;

    /**
     * 功能：保存或创建告警。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `alarmComment`：`alarmComment` 参数。
     * 返回：处理结果。
     */
    @Override
    public AlarmComment createOrUpdateAlarmComment(TenantId tenantId, AlarmComment alarmComment) {
        alarmCommentDataValidator.validate(alarmComment, c -> tenantId);
        boolean isCreated = alarmComment.getId() == null;
        AlarmComment result;
        if (isCreated) {
            result = createAlarmComment(tenantId, alarmComment);
        } else {
            result = updateAlarmComment(tenantId, alarmComment);
        }
        if (result != null) {
            eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(tenantId).entity(result)
                    .entityId(result.getAlarmId()).created(isCreated).build());
        }
        return result;
    }

    /**
     * 功能：保存或创建告警。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `alarmComment`：`alarmComment` 参数。
     * 返回：处理结果。
     */
    @Override
    public AlarmComment saveAlarmComment(TenantId tenantId, AlarmComment alarmComment) {
        log.debug("Deleting Alarm Comment: {}", alarmComment);
        alarmCommentDataValidator.validate(alarmComment, c -> tenantId);
        AlarmComment result = alarmCommentDao.save(tenantId, alarmComment);
        eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entity(result)
                .entityId(result.getAlarmId()).build());
        return result;
    }

    /**
     * 功能：获取告警。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `alarmId`：告警IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<AlarmCommentInfo> findAlarmComments(TenantId tenantId, AlarmId alarmId, PageLink pageLink) {
        log.trace("Executing findAlarmComments by alarmId [{}]", alarmId);
        return alarmCommentDao.findAlarmComments(tenantId, alarmId, pageLink);
    }

    /**
     * 功能：获取告警ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `alarmCommentId`：告警IDID。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<AlarmComment> findAlarmCommentByIdAsync(TenantId tenantId, AlarmCommentId alarmCommentId) {
        log.trace("Executing findAlarmCommentByIdAsync by alarmCommentId [{}]", alarmCommentId);
        validateId(alarmCommentId, "Incorrect alarmCommentId " + alarmCommentId);
        return alarmCommentDao.findAlarmCommentByIdAsync(tenantId, alarmCommentId.getId());
    }

    /**
     * 功能：获取告警ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `alarmCommentId`：告警IDID。
     * 返回：处理结果。
     */
    @Override
    public AlarmComment findAlarmCommentById(TenantId tenantId, AlarmCommentId alarmCommentId) {
        log.trace("Executing findAlarmCommentByIdAsync by alarmCommentId [{}]", alarmCommentId);
        validateId(alarmCommentId, "Incorrect alarmCommentId " + alarmCommentId);
        return alarmCommentDao.findById(tenantId, alarmCommentId.getId());
    }

    /**
     * 功能：保存或创建告警。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `alarmComment`：`alarmComment` 参数。
     * 返回：处理结果。
     */
    private AlarmComment createAlarmComment(TenantId tenantId, AlarmComment alarmComment) {
        log.debug("New Alarm comment : {}", alarmComment);
        if (alarmComment.getType() == null) {
            alarmComment.setType(AlarmCommentType.OTHER);
        }
        return alarmCommentDao.save(tenantId, alarmComment);
    }

    /**
     * 功能：更新告警。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `newAlarmComment`：`newAlarmComment` 参数。
     * 返回：处理结果。
     */
    private AlarmComment updateAlarmComment(TenantId tenantId, AlarmComment newAlarmComment) {
        log.debug("Update Alarm comment : {}", newAlarmComment);

        AlarmComment existing = alarmCommentDao.findAlarmCommentById(tenantId, newAlarmComment.getId().getId());
        if (existing != null) {
            if (newAlarmComment.getComment() != null) {
                JsonNode comment = newAlarmComment.getComment();
                ((ObjectNode) comment).put("edited", "true");
                ((ObjectNode) comment).put("editedOn", System.currentTimeMillis());
                existing.setComment(comment);
            }
            return alarmCommentDao.save(tenantId, existing);
        }
        return null;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`BaseAlarmCommentService` 在 ThingsBoard DAO 模块 中承担持久化实现层类型职责，核心目的是承载服务端实体、关系、属性、遥测、事件和配置数据的持久化访问实现。
 * 2. 核心流程：接收上层服务的租户、实体和查询上下文，完成校验、缓存处理、数据库读写或测试断言后返回结果。
 * 3. 关键依赖：主要依赖或协作对象包括DAO API、Common 数据模型、Application 服务、Rule Engine、缓存、SQL/NoSQL 数据库和审计模块。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
