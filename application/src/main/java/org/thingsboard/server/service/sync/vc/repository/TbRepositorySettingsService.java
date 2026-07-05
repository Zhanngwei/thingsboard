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
package org.thingsboard.server.service.sync.vc.repository;

import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.sync.vc.RepositorySettings;

/**
 * 中文说明：
 * 1. 类目的：`TbRepositorySettingsService` 是ThingsBoard Application 模块中的版本同步服务类型，用于处理实体版本控制、同步事件和跨实例状态一致性。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Version Control、DAO、队列、缓存和事件监听器。
 * 4. 生命周期：由 Spring 服务、事件监听或同步任务触发。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Observer。
 */
public interface TbRepositorySettingsService {

    /**
     * 功能：执行 `restore` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `versionControlSettings`：配置对象。
     * 返回：匹配的数据集合。
     */
    RepositorySettings restore(TenantId tenantId, RepositorySettings versionControlSettings);

    /**
     * 功能：执行 `get` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：匹配的数据集合。
     */
    RepositorySettings get(TenantId tenantId);

    /**
     * 功能：执行 `save` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `versionControlSettings`：配置对象。
     * 返回：匹配的数据集合。
     */
    RepositorySettings save(TenantId tenantId, RepositorySettings versionControlSettings);

    /**
     * 功能：执行 `delete` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：判断结果。
     */
    boolean delete(TenantId tenantId);

}

/*
 * 本类总结：
 * 1. 核心职责：`TbRepositorySettingsService` 在 ThingsBoard Application 模块 中承担版本同步服务类型职责，核心目的是处理实体版本控制、同步事件和跨实例状态一致性。
 * 2. 核心流程：接收同步请求后加载实体状态，转换为事件并写入目标存储或队列。
 * 3. 关键依赖：主要依赖或协作对象包括Version Control、DAO、队列、缓存和事件监听器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
