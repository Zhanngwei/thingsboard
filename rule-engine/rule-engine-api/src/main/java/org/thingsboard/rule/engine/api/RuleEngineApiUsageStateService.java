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
package org.thingsboard.rule.engine.api;

import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.id.ApiUsageStateId;
import org.thingsboard.server.common.data.id.TenantId;

/**
 * 中文说明：
 * 1. 职责：为 Rule Engine 提供 API 使用状态查询入口。
 * 2. 所属模块：属于 ThingsBoard Rule Engine API 的租户配额和 API 使用状态子系统。
 * 3. 协作对象：与 API 使用状态存储、租户限额逻辑、通知流程和 {@link TbContext} 协作。
 * 4. 生命周期：由 Spring 实现类长期存在，规则节点或系统流程需要读取状态时调用。
 * 5. 设计原因：规则引擎只需要查询契约，不应直接依赖 API 使用状态 DAO 实现。
 * 6. 技术关联：接口本身不直接涉及事务、缓存、MQTT、Actor、数据库；实现通常访问数据库或缓存，间接服务 Rule Engine。
 */
public interface RuleEngineApiUsageStateService {

    /**
     * 中文说明：
     * 1. 方法职责：按租户和状态 ID 查询 API 使用状态。
     * 2. 输入参数：tenantId 是租户边界，id 是 API 使用状态标识。
     * 3. 返回值：匹配的 ApiUsageState，未找到时由实现决定返回 null 或抛出异常。
     * 4. 调用时机：规则节点或系统流程需要判断租户 API 使用状态时调用。
     * 5. 调用方：Rule Engine 相关服务、限额和通知流程。
     * 6. 使用流程：属于 API 使用状态查询流程。
     * 7. 线程安全：接口无状态，实现需保证 DAO/缓存并发安全。
     * 8. 事务/缓存/MQTT/Actor/数据库/Rule Engine：接口不直接涉及 MQTT/Actor；实现可能访问数据库和缓存，直接服务 Rule Engine。
     */
    ApiUsageState findApiUsageStateById(TenantId tenantId, ApiUsageStateId id);

}

/*
 * 本类总结：
 * 1. 核心职责：抽象 API 使用状态查询能力。
 * 2. 核心流程：调用方传入租户和状态 ID，服务实现返回对应的 API 使用状态。
 * 3. 关键依赖：ApiUsageState、ApiUsageStateId、TenantId 和状态存储实现。
 * 4. 学习重点：Rule Engine 通过专用服务接口读取租户配额状态，而不是直接访问 DAO。
 */
