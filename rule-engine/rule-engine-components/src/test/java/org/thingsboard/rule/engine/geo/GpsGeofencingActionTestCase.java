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
 * 测试目标：验证 {@code GpsGeofencingActionTestCase} 覆盖的 地理围栏组件 行为，重点说明配置、消息和断言路径。
 * 所属生产节点/组件：{@code GpsGeofencingAction}，用于守护对应 Rule Engine 组件的兼容性和边界条件。
 * Mock 依赖来源：内存 fixture、参数化数据源，以及测试体按需创建的 Mockito mock/spy；测试不启动真实外部服务。
 * 被验证流程：准备 fixture，初始化节点或工具对象，触发被测调用，再断言输出、异常或 Mock 交互。
 * 存在原因：防止规则引擎组件在升级、消息处理、异步回调或数据映射场景中发生回归。
 */
@Data
public class GpsGeofencingActionTestCase {

    /** 可变 fixture 字段：{@code entityId} 保存 {@code EntityId} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private EntityId entityId;
    /** 可变 fixture 字段：{@code entityStates} 保存 {@code Map<EntityId, EntityGeofencingState>} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private Map<EntityId, EntityGeofencingState> entityStates;
    /** 可变 fixture 字段：{@code msgInside} 保存 {@code boolean} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private boolean msgInside;
    /** 可变 fixture 字段：{@code reportPresenceStatusOnEachMessage} 保存 {@code boolean} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private boolean reportPresenceStatusOnEachMessage;

    /**
     * 辅助方法：{@code GpsGeofencingActionTestCase} 复用本类测试的 fixture 构造、Mock 配置或断言逻辑。
     * 输入数据：来自调用方参数、类字段和内存对象；输出影响由调用它的测试方法验证。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
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
