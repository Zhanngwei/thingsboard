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
package org.thingsboard.rule.engine.geo;

import lombok.Data;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.HashMap;
import java.util.Map;

/**
 * `GpsGeofencingActionTestCase` 类，封装当前模块中的一组相关职责。
 */
@Data
public class GpsGeofencingActionTestCase {

    /**
     * 实体ID，用于定位对应业务对象。
     */
    private EntityId entityId;
    /**
     * 实体映射关系，用于按键查找对应值。
     */
    private Map<EntityId, EntityGeofencingState> entityStates;
    /**
     * 是否满足消息条件。
     */
    private boolean msgInside;
    /**
     * 是否满足消息条件。
     */
    private boolean reportPresenceStatusOnEachMessage;

    /**
     * 功能：创建 `GpsGeofencingActionTestCase` 实例，并初始化必要字段。
     * 参数：
     * - `entityId`：实体IDID。
     * - `msgInside`：待处理消息。
     * - `reportPresenceStatusOnEachMessage`：待处理消息。
     * - `entityGeofencingState`：实体对象。
     * 返回：新创建的对象实例。
     */
    public GpsGeofencingActionTestCase(EntityId entityId, boolean msgInside, boolean reportPresenceStatusOnEachMessage, EntityGeofencingState entityGeofencingState) {
        this.entityId = entityId;
        this.msgInside = msgInside;
        this.reportPresenceStatusOnEachMessage = reportPresenceStatusOnEachMessage;
        this.entityStates = new HashMap<>();
        this.entityStates.put(entityId, entityGeofencingState);
    }
}
/*
 * 本类总结：{@code GpsGeofencingActionTestCase} 为 {@code GpsGeofencingAction} 的 地理围栏组件 测试提供中文注释，说明测试目标、fixture 生命周期、Mock 来源和断言流程。
 * 本文件中的数据库、缓存、MQTT、Actor 或完整 Rule Engine 运行时均不由测试本身直接启动；相关行为通过 Mock、内存 fixture 或被测生产逻辑间接覆盖。
 */
