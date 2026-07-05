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
package org.thingsboard.server.service.security.auth.mfa.config;

import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.model.mfa.PlatformTwoFaSettings;
import org.thingsboard.server.common.data.security.model.mfa.account.AccountTwoFaSettings;
import org.thingsboard.server.common.data.security.model.mfa.account.TwoFaAccountConfig;
import org.thingsboard.server.common.data.security.model.mfa.provider.TwoFaProviderType;

import java.util.Optional;

/**
 * 中文说明：
 * 1. 类目的：`TwoFaConfigManager` 是ThingsBoard Application 模块中的Spring 配置类型，用于声明应用启动、Web、安全、跨域、Swagger 或调度相关 Bean。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring Boot 自动配置、Environment、Filter、Security、Scheduler 和 WebSocket 组件。
 * 4. 生命周期：应用启动时由 Spring 创建，运行期通常作为单例配置对象存在。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Configuration / Factory。
 */
public interface TwoFaConfigManager {

    /**
     * 功能：获取配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `userId`：用户ID。
     * 返回：匹配的数据集合。
     */
    Optional<AccountTwoFaSettings> getAccountTwoFaSettings(TenantId tenantId, UserId userId);


    /**
     * 功能：获取配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `userId`：用户ID。
     * - `providerType`：类型。
     * 返回：可能存在的结果。
     */
    Optional<TwoFaAccountConfig> getTwoFaAccountConfig(TenantId tenantId, UserId userId, TwoFaProviderType providerType);

    /**
     * 功能：保存或创建配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `userId`：用户ID。
     * - `accountConfig`：配置对象。
     * 返回：匹配的数据集合。
     */
    AccountTwoFaSettings saveTwoFaAccountConfig(TenantId tenantId, UserId userId, TwoFaAccountConfig accountConfig);

    /**
     * 功能：删除或清理配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `userId`：用户ID。
     * - `providerType`：类型。
     * 返回：匹配的数据集合。
     */
    AccountTwoFaSettings deleteTwoFaAccountConfig(TenantId tenantId, UserId userId, TwoFaProviderType providerType);


    /**
     * 功能：获取配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `sysadminSettingsAsDefault`：配置对象。
     * 返回：匹配的数据集合。
     */
    Optional<PlatformTwoFaSettings> getPlatformTwoFaSettings(TenantId tenantId, boolean sysadminSettingsAsDefault);

    /**
     * 功能：保存或创建配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `twoFactorAuthSettings`：配置对象。
     * 返回：匹配的数据集合。
     */
    PlatformTwoFaSettings savePlatformTwoFaSettings(TenantId tenantId, PlatformTwoFaSettings twoFactorAuthSettings) throws ThingsboardException;

    /**
     * 功能：删除或清理配置。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    void deletePlatformTwoFaSettings(TenantId tenantId);

}

/*
 * 本类总结：
 * 1. 核心职责：`TwoFaConfigManager` 在 ThingsBoard Application 模块 中承担Spring 配置类型职责，核心目的是声明应用启动、Web、安全、跨域、Swagger 或调度相关 Bean。
 * 2. 核心流程：读取配置属性并创建 Bean，供后续 Web、安全或后台任务流程使用。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Boot 自动配置、Environment、Filter、Security、Scheduler 和 WebSocket 组件。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
