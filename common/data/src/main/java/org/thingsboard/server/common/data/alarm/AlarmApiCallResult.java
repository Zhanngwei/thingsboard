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
package org.thingsboard.server.common.data.alarm;

import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.id.EntityId;

import java.io.Serializable;
import java.util.List;


/**
 * 中文说明：
 * 1. 类目的：`AlarmApiCallResult` 是ThingsBoard Common 模块中的公共数据模型类型，用于承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 生命周期：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Value Object / Builder。
 */
@Data
public class AlarmApiCallResult implements Serializable {

    /**
     * 当前操作是否成功。
     */
    private final boolean successful;
    private final boolean created;
    /**
     * 当前对象是否发生变更。
     */
    private final boolean modified;
    private final boolean cleared;
    /**
     * 当前对象是否已经删除。
     */
    private final boolean deleted;
    private final AlarmInfo alarm;
    /**
     * `old` 字段，保存当前对象的对应属性。
     */
    private final Alarm old;
    private final List<EntityId> propagatedEntitiesList;

    /**
     * 功能：创建 `AlarmApiCallResult` 实例，并初始化必要字段。
     * 参数：
     * - `successful`：`successful` 参数。
     * - `created`：`created` 参数。
     * - `modified`：`modified` 参数。
     * - `cleared`：`cleared` 参数。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    @Builder
    private AlarmApiCallResult(boolean successful, boolean created, boolean modified, boolean cleared, boolean deleted, AlarmInfo alarm, Alarm old, List<EntityId> propagatedEntitiesList) {
        this.successful = successful;
        this.created = created;
        this.modified = modified;
        this.cleared = cleared;
        this.deleted = deleted;
        this.alarm = alarm;
        this.old = old;
        this.propagatedEntitiesList = propagatedEntitiesList;
    }

    /**
     * 功能：创建 `AlarmApiCallResult` 实例，并初始化必要字段。
     * 参数：
     * - `other`：键值映射。
     * - `propagatedEntitiesList`：数据列表。
     * 返回：新创建的对象实例。
     */
    public AlarmApiCallResult(AlarmApiCallResult other, List<EntityId> propagatedEntitiesList) {
        this.successful = other.successful;
        this.created = other.created;
        this.modified = other.modified;
        this.cleared = other.cleared;
        this.deleted = other.deleted;
        this.alarm = other.alarm;
        this.old = other.old;
        this.propagatedEntitiesList = propagatedEntitiesList;
    }

    /**
     * 功能：判断`Severity Changed`。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean isSeverityChanged() {
        if (alarm == null || old == null) {
            return false;
        } else {
            return !alarm.getSeverity().equals(old.getSeverity());
        }
    }

    /**
     * 功能：判断`Acknowledged`。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean isAcknowledged() {
        if (alarm == null || old == null) {
            return false;
        } else {
            return alarm.isAcknowledged() != old.isAcknowledged();
        }
    }

    /**
     * 功能：获取`Old Severity`。
     * 参数：无。
     * 返回：处理结果。
     */
    public AlarmSeverity getOldSeverity() {
        return isSeverityChanged() ? old.getSeverity() : null;
    }

    /**
     * 功能：判断`Propagation Changed`。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean isPropagationChanged() {
        if (created) {
            return true;
        }
        if (alarm == null || old == null) {
            return false;
        }
        return (alarm.isPropagate() != old.isPropagate()) ||
                (alarm.isPropagateToOwner() != old.isPropagateToOwner()) ||
                (alarm.isPropagateToTenant() != old.isPropagateToTenant()) ||
                (!alarm.getPropagateRelationTypes().equals(old.getPropagateRelationTypes()));
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`AlarmApiCallResult` 在 ThingsBoard Common 模块 中承担公共数据模型类型职责，核心目的是承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 核心流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
 * 3. 关键依赖：主要依赖或协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
