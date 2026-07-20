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
 * 1. `CacheConstants` 是 ThingsBoard Common Data 中处理缓存通用操作的工具类型。
 * 2. 它提供无状态或轻量的复用方法，减少多个调用点的重复实现。
 * 3. 方法通常完成格式化、校验、计算或简单对象构造。
 * 4. 它直接协作于方法参数和返回值所代表的数据类型。
 * 5. 集中工具方法可以统一边界行为，并降低细节变化对调用方的影响。
 * 6. 阅读时重点关注输入约束、边界值和方法是否修改传入对象。
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
