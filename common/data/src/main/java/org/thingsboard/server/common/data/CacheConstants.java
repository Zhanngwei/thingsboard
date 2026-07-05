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
package org.thingsboard.server.common.data;

/**
 * 中文说明：
 * 1. 类目的：`CacheConstants` 是ThingsBoard Common 模块中的公共数据模型类型，用于承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 生命周期：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Value Object / Builder。
 */
public class CacheConstants {
    /**
     * 设备凭据常量，用于统一引用固定值。
     */
    public static final String DEVICE_CREDENTIALS_CACHE = "deviceCredentials";
    public static final String RELATIONS_CACHE = "relations";
    /**
     * 设备常量，用于统一引用固定值。
     */
    public static final String DEVICE_CACHE = "devices";
    public static final String SESSIONS_CACHE = "sessions";
    /**
     * 资产常量，用于统一引用固定值。
     */
    public static final String ASSET_CACHE = "assets";
    public static final String ENTITY_VIEW_CACHE = "entityViews";
    /**
     * 边缘节点常量，用于统一引用固定值。
     */
    public static final String EDGE_CACHE = "edges";
    public static final String CLAIM_DEVICES_CACHE = "claimDevices";
    /**
     * 配置常量，用于统一引用固定值。
     */
    public static final String SECURITY_SETTINGS_CACHE = "securitySettings";
    public static final String TENANT_PROFILE_CACHE = "tenantProfiles";
    /**
     * `TENANTS_CACHE`常量，用于统一引用固定值。
     */
    public static final String TENANTS_CACHE = "tenants";
    public static final String TENANTS_EXIST_CACHE = "tenantsExist";
    /**
     * 设备配置常量，用于统一引用固定值。
     */
    public static final String DEVICE_PROFILE_CACHE = "deviceProfiles";
    public static final String NOTIFICATION_SETTINGS_CACHE = "notificationSettings";
    /**
     * `SENT_NOTIFICATIONS_CACHE`常量，用于统一引用固定值。
     */
    public static final String SENT_NOTIFICATIONS_CACHE = "sentNotifications";

    /**
     * 资产配置常量，用于统一引用固定值。
     */
    public static final String ASSET_PROFILE_CACHE = "assetProfiles";
    public static final String ATTRIBUTES_CACHE = "attributes";
    /**
     * 会话常量，用于统一引用固定值。
     */
    public static final String USERS_SESSION_INVALIDATION_CACHE = "userSessionsInvalidation";
    public static final String OTA_PACKAGE_CACHE = "otaPackages";
    /**
     * 数据常量，用于统一引用固定值。
     */
    public static final String OTA_PACKAGE_DATA_CACHE = "otaPackagesData";
    public static final String REPOSITORY_SETTINGS_CACHE = "repositorySettings";
    /**
     * 配置常量，用于统一引用固定值。
     */
    public static final String AUTO_COMMIT_SETTINGS_CACHE = "autoCommitSettings";
    public static final String TWO_FA_VERIFICATION_CODES_CACHE = "twoFaVerificationCodes";
    /**
     * 版本号常量，用于统一引用固定值。
     */
    public static final String VERSION_CONTROL_TASK_CACHE = "versionControlTask";
    public static final String USER_SETTINGS_CACHE = "userSettings";
    /**
     * 仪表盘常量，用于统一引用固定值。
     */
    public static final String DASHBOARD_TITLES_CACHE = "dashboardTitles";
    public static final String ENTITY_COUNT_CACHE = "entityCount";
    /**
     * 信息对象常量，用于统一引用固定值。
     */
    public static final String RESOURCE_INFO_CACHE = "resourceInfo";
    public static final String ALARM_TYPES_CACHE = "alarmTypes";
}

/*
 * 本类总结：
 * 1. 核心职责：`CacheConstants` 在 ThingsBoard Common 模块 中承担公共数据模型类型职责，核心目的是承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 核心流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
 * 3. 关键依赖：主要依赖或协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
