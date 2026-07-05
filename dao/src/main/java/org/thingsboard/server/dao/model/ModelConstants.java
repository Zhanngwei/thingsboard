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
package org.thingsboard.server.dao.model;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.apache.commons.lang3.ArrayUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.Aggregation;

import java.util.UUID;

/**
 * 中文说明：
 * 1. 类目的：`ModelConstants` 是 ThingsBoard DAO 模块 中的持久化实体映射类型，用于描述 ThingsBoard 领域对象与 SQL/Cassandra 存储结构之间的字段映射、索引关系和序列化边界。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括JPA/Hibernate、Repository、DAO Service、Common DTO、JSON 序列化和数据库迁移脚本。
 * 4. 生命周期：由 ORM、Repository 或 DAO 在读写数据库时创建，并随单次查询或持久化会话存在。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Entity / Mapper / Value Object。
 */
public class ModelConstants {

    /**
     * 功能：创建 `ModelConstants` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    private ModelConstants() {
    }

    public static final UUID NULL_UUID = Uuids.startOf(0);
    public static final TenantId SYSTEM_TENANT = TenantId.fromUUID(ModelConstants.NULL_UUID);

    // this is the difference between midnight October 15, 1582 UTC and midnight January 1, 1970 UTC as 100 nanosecond units
    /**
     * `EPOCH_DIFF`常量，用于统一引用固定值。
     */
    public static final long EPOCH_DIFF = 122192928000000000L;

    /**
     * Generic constants.
     */
    /**
     * `ID_PROPERTY`常量，用于统一引用固定值。
     */
    public static final String ID_PROPERTY = "id";
    public static final String CREATED_TIME_PROPERTY = "created_time";
    /**
     * 用户常量，用于统一引用固定值。
     */
    public static final String USER_ID_PROPERTY = "user_id";
    public static final String TENANT_ID_PROPERTY = "tenant_id";
    /**
     * 客户ID常量，用于统一引用固定值。
     */
    public static final String CUSTOMER_ID_PROPERTY = "customer_id";
    public static final String ASSIGNEE_ID_PROPERTY = "assignee_id";
    /**
     * 设备ID常量，用于统一引用固定值。
     */
    public static final String DEVICE_ID_PROPERTY = "device_id";
    public static final String TITLE_PROPERTY = "title";
    /**
     * 名称常量，用于统一引用固定值。
     */
    public static final String NAME_PROPERTY = "name";
    public static final String ALIAS_PROPERTY = "alias";
    /**
     * 搜索文本常量，用于统一引用固定值。
     */
    public static final String SEARCH_TEXT_PROPERTY = "search_text";
    public static final String ADDITIONAL_INFO_PROPERTY = "additional_info";
    /**
     * 实体常量，用于统一引用固定值。
     */
    public static final String ENTITY_TYPE_PROPERTY = "entity_type";

    /**
     * 实体常量，用于统一引用固定值。
     */
    public static final String ENTITY_TYPE_COLUMN = ENTITY_TYPE_PROPERTY;
    public static final String TENANT_ID_COLUMN = "tenant_id";
    /**
     * 实体ID常量，用于统一引用固定值。
     */
    public static final String ENTITY_ID_COLUMN = "entity_id";
    public static final String ATTRIBUTE_TYPE_COLUMN = "attribute_type";
    /**
     * 属性常量，用于统一引用固定值。
     */
    public static final String ATTRIBUTE_KEY_COLUMN = "attribute_key";
    public static final String LAST_UPDATE_TS_COLUMN = "last_update_ts";

    /**
     * User constants.
     */
    /**
     * 用户常量，用于统一引用固定值。
     */
    public static final String USER_PG_HIBERNATE_TABLE_NAME = "tb_user";
    public static final String USER_TENANT_ID_PROPERTY = TENANT_ID_PROPERTY;
    /**
     * 客户ID常量，用于统一引用固定值。
     */
    public static final String USER_CUSTOMER_ID_PROPERTY = CUSTOMER_ID_PROPERTY;
    public static final String USER_EMAIL_PROPERTY = "email";
    /**
     * 用户常量，用于统一引用固定值。
     */
    public static final String USER_AUTHORITY_PROPERTY = "authority";
    public static final String USER_FIRST_NAME_PROPERTY = "first_name";
    /**
     * 用户常量，用于统一引用固定值。
     */
    public static final String USER_LAST_NAME_PROPERTY = "last_name";
    public static final String USER_ADDITIONAL_INFO_PROPERTY = ADDITIONAL_INFO_PROPERTY;

    /**
     * User_credentials constants.
     */
    /**
     * 用户常量，用于统一引用固定值。
     */
    public static final String USER_CREDENTIALS_TABLE_NAME = "user_credentials";
    public static final String USER_CREDENTIALS_USER_ID_PROPERTY = USER_ID_PROPERTY;
    /**
     * 用户常量，用于统一引用固定值。
     */
    public static final String USER_CREDENTIALS_ENABLED_PROPERTY = "enabled";
    public static final String USER_CREDENTIALS_PASSWORD_PROPERTY = "password"; //NOSONAR, the constant used to identify password column name (not password value itself)
    /**
     * 用户常量，用于统一引用固定值。
     */
    public static final String USER_CREDENTIALS_ACTIVATE_TOKEN_PROPERTY = "activate_token";
    public static final String USER_CREDENTIALS_RESET_TOKEN_PROPERTY = "reset_token";
    /**
     * 用户常量，用于统一引用固定值。
     */
    public static final String USER_CREDENTIALS_ADDITIONAL_PROPERTY = "additional_info";

    /**
     * User settings constants.
     */
    /**
     * 用户常量，用于统一引用固定值。
     */
    public static final String USER_SETTINGS_TABLE_NAME = "user_settings";
    public static final String USER_SETTINGS_USER_ID_PROPERTY = USER_ID_PROPERTY;
    /**
     * 用户常量，用于统一引用固定值。
     */
    public static final String USER_SETTINGS_TYPE_PROPERTY = "type";
    public static final String USER_SETTINGS_SETTINGS = "settings";

    /**
     * Admin_settings constants.
     */
    /**
     * 配置常量，用于统一引用固定值。
     */
    public static final String ADMIN_SETTINGS_TABLE_NAME = "admin_settings";

    /**
     * 租户ID常量，用于统一引用固定值。
     */
    public static final String ADMIN_SETTINGS_TENANT_ID_PROPERTY = TENANT_ID_PROPERTY;
    public static final String ADMIN_SETTINGS_KEY_PROPERTY = "key";
    /**
     * 配置常量，用于统一引用固定值。
     */
    public static final String ADMIN_SETTINGS_JSON_VALUE_PROPERTY = "json_value";

    /**
     * Contact constants.
     */
    /**
     * `COUNTRY_PROPERTY`常量，用于统一引用固定值。
     */
    public static final String COUNTRY_PROPERTY = "country";
    public static final String STATE_PROPERTY = "state";
    /**
     * `CITY_PROPERTY`常量，用于统一引用固定值。
     */
    public static final String CITY_PROPERTY = "city";
    public static final String ADDRESS_PROPERTY = "address";
    /**
     * `ADDRESS2_PROPERTY`常量，用于统一引用固定值。
     */
    public static final String ADDRESS2_PROPERTY = "address2";
    public static final String ZIP_PROPERTY = "zip";
    /**
     * `PHONE_PROPERTY`常量，用于统一引用固定值。
     */
    public static final String PHONE_PROPERTY = "phone";
    public static final String EMAIL_PROPERTY = "email";

    /**
     * Tenant constants.
     */
    /**
     * 租户常量，用于统一引用固定值。
     */
    public static final String TENANT_TABLE_NAME = "tenant";
    public static final String TENANT_TITLE_PROPERTY = TITLE_PROPERTY;
    /**
     * 租户常量，用于统一引用固定值。
     */
    public static final String TENANT_REGION_PROPERTY = "region";
    public static final String TENANT_ADDITIONAL_INFO_PROPERTY = ADDITIONAL_INFO_PROPERTY;
    /**
     * 租户ID常量，用于统一引用固定值。
     */
    public static final String TENANT_TENANT_PROFILE_ID_PROPERTY = "tenant_profile_id";

    /**
     * Tenant profile constants.
     */
    /**
     * 租户常量，用于统一引用固定值。
     */
    public static final String TENANT_PROFILE_TABLE_NAME = "tenant_profile";
    public static final String TENANT_PROFILE_NAME_PROPERTY = "name";
    /**
     * 租户常量，用于统一引用固定值。
     */
    public static final String TENANT_PROFILE_PROFILE_DATA_PROPERTY = "profile_data";
    public static final String TENANT_PROFILE_DESCRIPTION_PROPERTY = "description";
    /**
     * 租户常量，用于统一引用固定值。
     */
    public static final String TENANT_PROFILE_IS_DEFAULT_PROPERTY = "is_default";
    public static final String TENANT_PROFILE_ISOLATED_TB_RULE_ENGINE = "isolated_tb_rule_engine";

    /**
     * Customer constants.
     */
    /**
     * 客户常量，用于统一引用固定值。
     */
    public static final String CUSTOMER_TABLE_NAME = "customer";
    public static final String CUSTOMER_TENANT_ID_PROPERTY = TENANT_ID_PROPERTY;
    /**
     * 客户常量，用于统一引用固定值。
     */
    public static final String CUSTOMER_TITLE_PROPERTY = TITLE_PROPERTY;
    public static final String CUSTOMER_ADDITIONAL_INFO_PROPERTY = ADDITIONAL_INFO_PROPERTY;

    /**
     * Device constants.
     */
    /**
     * 设备常量，用于统一引用固定值。
     */
    public static final String DEVICE_TABLE_NAME = "device";
    public static final String DEVICE_TENANT_ID_PROPERTY = TENANT_ID_PROPERTY;
    /**
     * 客户ID常量，用于统一引用固定值。
     */
    public static final String DEVICE_CUSTOMER_ID_PROPERTY = CUSTOMER_ID_PROPERTY;
    public static final String DEVICE_NAME_PROPERTY = "name";
    /**
     * 设备常量，用于统一引用固定值。
     */
    public static final String DEVICE_TYPE_PROPERTY = "type";
    public static final String DEVICE_LABEL_PROPERTY = "label";
    /**
     * 扩展信息常量，用于统一引用固定值。
     */
    public static final String DEVICE_ADDITIONAL_INFO_PROPERTY = ADDITIONAL_INFO_PROPERTY;
    public static final String DEVICE_DEVICE_PROFILE_ID_PROPERTY = "device_profile_id";
    /**
     * 设备常量，用于统一引用固定值。
     */
    public static final String DEVICE_DEVICE_DATA_PROPERTY = "device_data";
    public static final String DEVICE_FIRMWARE_ID_PROPERTY = "firmware_id";
    /**
     * 设备ID常量，用于统一引用固定值。
     */
    public static final String DEVICE_SOFTWARE_ID_PROPERTY = "software_id";

    /**
     * 客户常量，用于统一引用固定值。
     */
    public static final String DEVICE_CUSTOMER_TITLE_PROPERTY = "customer_title";
    public static final String DEVICE_CUSTOMER_IS_PUBLIC_PROPERTY = "customer_is_public";
    /**
     * 设备配置常量，用于统一引用固定值。
     */
    public static final String DEVICE_DEVICE_PROFILE_NAME_PROPERTY = "device_profile_name";
    public static final String DEVICE_ACTIVE_PROPERTY = "active";

    /**
     * 设备常量，用于统一引用固定值。
     */
    public static final String DEVICE_INFO_VIEW_TABLE_NAME = "device_info_view";

    /**
     * Device profile constants.
     */
    /**
     * 设备配置常量，用于统一引用固定值。
     */
    public static final String DEVICE_PROFILE_TABLE_NAME = "device_profile";
    public static final String DEVICE_PROFILE_TENANT_ID_PROPERTY = TENANT_ID_PROPERTY;
    /**
     * 设备配置常量，用于统一引用固定值。
     */
    public static final String DEVICE_PROFILE_NAME_PROPERTY = "name";
    public static final String DEVICE_PROFILE_TYPE_PROPERTY = "type";
    /**
     * 设备配置常量，用于统一引用固定值。
     */
    public static final String DEVICE_PROFILE_IMAGE_PROPERTY = "image";
    public static final String DEVICE_PROFILE_TRANSPORT_TYPE_PROPERTY = "transport_type";
    /**
     * 设备配置常量，用于统一引用固定值。
     */
    public static final String DEVICE_PROFILE_PROVISION_TYPE_PROPERTY = "provision_type";
    public static final String DEVICE_PROFILE_PROFILE_DATA_PROPERTY = "profile_data";
    /**
     * 设备配置常量，用于统一引用固定值。
     */
    public static final String DEVICE_PROFILE_DESCRIPTION_PROPERTY = "description";
    public static final String DEVICE_PROFILE_IS_DEFAULT_PROPERTY = "is_default";
    /**
     * 设备配置常量，用于统一引用固定值。
     */
    public static final String DEVICE_PROFILE_DEFAULT_RULE_CHAIN_ID_PROPERTY = "default_rule_chain_id";
    public static final String DEVICE_PROFILE_DEFAULT_DASHBOARD_ID_PROPERTY = "default_dashboard_id";
    /**
     * 设备配置常量，用于统一引用固定值。
     */
    public static final String DEVICE_PROFILE_DEFAULT_QUEUE_NAME_PROPERTY = "default_queue_name";
    public static final String DEVICE_PROFILE_PROVISION_DEVICE_KEY = "provision_device_key";
    /**
     * 设备配置常量，用于统一引用固定值。
     */
    public static final String DEVICE_PROFILE_FIRMWARE_ID_PROPERTY = "firmware_id";
    public static final String DEVICE_PROFILE_SOFTWARE_ID_PROPERTY = "software_id";
    /**
     * 设备配置常量，用于统一引用固定值。
     */
    public static final String DEVICE_PROFILE_DEFAULT_EDGE_RULE_CHAIN_ID_PROPERTY = "default_edge_rule_chain_id";

    /**
     * Asset profile constants.
     */
    /**
     * 资产配置常量，用于统一引用固定值。
     */
    public static final String ASSET_PROFILE_TABLE_NAME = "asset_profile";
    public static final String ASSET_PROFILE_TENANT_ID_PROPERTY = TENANT_ID_PROPERTY;
    /**
     * 资产配置常量，用于统一引用固定值。
     */
    public static final String ASSET_PROFILE_NAME_PROPERTY = "name";
    public static final String ASSET_PROFILE_IMAGE_PROPERTY = "image";
    /**
     * 资产配置常量，用于统一引用固定值。
     */
    public static final String ASSET_PROFILE_DESCRIPTION_PROPERTY = "description";
    public static final String ASSET_PROFILE_IS_DEFAULT_PROPERTY = "is_default";
    /**
     * 资产配置常量，用于统一引用固定值。
     */
    public static final String ASSET_PROFILE_DEFAULT_RULE_CHAIN_ID_PROPERTY = "default_rule_chain_id";
    public static final String ASSET_PROFILE_DEFAULT_DASHBOARD_ID_PROPERTY = "default_dashboard_id";
    /**
     * 资产配置常量，用于统一引用固定值。
     */
    public static final String ASSET_PROFILE_DEFAULT_QUEUE_NAME_PROPERTY = "default_queue_name";
    public static final String ASSET_PROFILE_DEFAULT_EDGE_RULE_CHAIN_ID_PROPERTY = "default_edge_rule_chain_id";

    /**
     * Entity view constants.
     */
    /**
     * 实体视图常量，用于统一引用固定值。
     */
    public static final String ENTITY_VIEW_TABLE_NAME = "entity_view";
    public static final String ENTITY_VIEW_ENTITY_ID_PROPERTY = ENTITY_ID_COLUMN;
    /**
     * 实体视图常量，用于统一引用固定值。
     */
    public static final String ENTITY_VIEW_TENANT_ID_PROPERTY = TENANT_ID_PROPERTY;
    public static final String ENTITY_VIEW_CUSTOMER_ID_PROPERTY = CUSTOMER_ID_PROPERTY;
    /**
     * 实体视图常量，用于统一引用固定值。
     */
    public static final String ENTITY_VIEW_NAME_PROPERTY = DEVICE_NAME_PROPERTY;
    public static final String ENTITY_VIEW_KEYS_PROPERTY = "keys";
    /**
     * 实体视图常量，用于统一引用固定值。
     */
    public static final String ENTITY_VIEW_START_TS_PROPERTY = "start_ts";
    public static final String ENTITY_VIEW_END_TS_PROPERTY = "end_ts";
    /**
     * 实体视图常量，用于统一引用固定值。
     */
    public static final String ENTITY_VIEW_ADDITIONAL_INFO_PROPERTY = ADDITIONAL_INFO_PROPERTY;

    /**
     * Audit log constants.
     */
    /**
     * 名称常量，用于统一引用固定值。
     */
    public static final String AUDIT_LOG_TABLE_NAME = "audit_log";
    public static final String AUDIT_LOG_TENANT_ID_PROPERTY = TENANT_ID_PROPERTY;
    /**
     * 客户ID常量，用于统一引用固定值。
     */
    public static final String AUDIT_LOG_CUSTOMER_ID_PROPERTY = CUSTOMER_ID_PROPERTY;
    public static final String AUDIT_LOG_ENTITY_TYPE_PROPERTY = ENTITY_TYPE_PROPERTY;
    /**
     * 实体ID常量，用于统一引用固定值。
     */
    public static final String AUDIT_LOG_ENTITY_ID_PROPERTY = ENTITY_ID_COLUMN;
    public static final String AUDIT_LOG_ENTITY_NAME_PROPERTY = "entity_name";
    /**
     * 用户常量，用于统一引用固定值。
     */
    public static final String AUDIT_LOG_USER_ID_PROPERTY = USER_ID_PROPERTY;
    public static final String AUDIT_LOG_USER_NAME_PROPERTY = "user_name";
    /**
     * 类型常量，用于统一引用固定值。
     */
    public static final String AUDIT_LOG_ACTION_TYPE_PROPERTY = "action_type";
    public static final String AUDIT_LOG_ACTION_DATA_PROPERTY = "action_data";
    /**
     * 状态常量，用于统一引用固定值。
     */
    public static final String AUDIT_LOG_ACTION_STATUS_PROPERTY = "action_status";
    public static final String AUDIT_LOG_ACTION_FAILURE_DETAILS_PROPERTY = "action_failure_details";

    /**
     * Asset constants.
     */
    /**
     * 资产常量，用于统一引用固定值。
     */
    public static final String ASSET_TABLE_NAME = "asset";
    public static final String ASSET_TENANT_ID_PROPERTY = TENANT_ID_PROPERTY;
    /**
     * 客户ID常量，用于统一引用固定值。
     */
    public static final String ASSET_CUSTOMER_ID_PROPERTY = CUSTOMER_ID_PROPERTY;
    public static final String ASSET_NAME_PROPERTY = "name";
    /**
     * 资产常量，用于统一引用固定值。
     */
    public static final String ASSET_TYPE_PROPERTY = "type";
    public static final String ASSET_LABEL_PROPERTY = "label";
    /**
     * 扩展信息常量，用于统一引用固定值。
     */
    public static final String ASSET_ADDITIONAL_INFO_PROPERTY = ADDITIONAL_INFO_PROPERTY;

    /**
     * 资产配置常量，用于统一引用固定值。
     */
    public static final String ASSET_ASSET_PROFILE_ID_PROPERTY = "asset_profile_id";

    /**
     * Alarm constants.
     */
    /**
     * 实体常量，用于统一引用固定值。
     */
    public static final String ENTITY_ALARM_TABLE_NAME = "entity_alarm";
    public static final String ALARM_TABLE_NAME = "alarm";
    /**
     * 告警常量，用于统一引用固定值。
     */
    public static final String ALARM_VIEW_NAME = "alarm_info";
    public static final String ALARM_TENANT_ID_PROPERTY = TENANT_ID_PROPERTY;
    /**
     * 客户ID常量，用于统一引用固定值。
     */
    public static final String ALARM_CUSTOMER_ID_PROPERTY = CUSTOMER_ID_PROPERTY;
    public static final String ALARM_TYPE_PROPERTY = "type";
    /**
     * 告警常量，用于统一引用固定值。
     */
    public static final String ALARM_DETAILS_PROPERTY = ADDITIONAL_INFO_PROPERTY;
    public static final String ALARM_STATUS_PROPERTY = "status";
    /**
     * 告警ID常量，用于统一引用固定值。
     */
    public static final String ALARM_ORIGINATOR_ID_PROPERTY = "originator_id";
    public static final String ALARM_ORIGINATOR_NAME_PROPERTY = "originator_name";
    /**
     * 告警常量，用于统一引用固定值。
     */
    public static final String ALARM_ORIGINATOR_LABEL_PROPERTY = "originator_label";
    public static final String ALARM_ORIGINATOR_TYPE_PROPERTY = "originator_type";
    /**
     * 告警常量，用于统一引用固定值。
     */
    public static final String ALARM_SEVERITY_PROPERTY = "severity";
    public static final String ALARM_ASSIGNEE_ID_PROPERTY = "assignee_id";
    /**
     * 告警常量，用于统一引用固定值。
     */
    public static final String ALARM_ASSIGNEE_FIRST_NAME_PROPERTY = "assignee_first_name";
    public static final String ALARM_ASSIGNEE_LAST_NAME_PROPERTY = "assignee_last_name";
    /**
     * 告警常量，用于统一引用固定值。
     */
    public static final String ALARM_ASSIGNEE_EMAIL_PROPERTY = "assignee_email";
    public static final String ALARM_START_TS_PROPERTY = "start_ts";
    /**
     * 结束时间戳常量，用于统一引用固定值。
     */
    public static final String ALARM_END_TS_PROPERTY = "end_ts";
    public static final String ALARM_ACKNOWLEDGED_PROPERTY = "acknowledged";
    /**
     * 告警常量，用于统一引用固定值。
     */
    public static final String ALARM_ACK_TS_PROPERTY = "ack_ts";
    public static final String ALARM_CLEARED_PROPERTY = "cleared";
    /**
     * 告警常量，用于统一引用固定值。
     */
    public static final String ALARM_CLEAR_TS_PROPERTY = "clear_ts";
    public static final String ALARM_ASSIGN_TS_PROPERTY = "assign_ts";
    /**
     * 告警常量，用于统一引用固定值。
     */
    public static final String ALARM_PROPAGATE_PROPERTY = "propagate";
    public static final String ALARM_PROPAGATE_TO_OWNER_PROPERTY = "propagate_to_owner";
    /**
     * 租户常量，用于统一引用固定值。
     */
    public static final String ALARM_PROPAGATE_TO_TENANT_PROPERTY = "propagate_to_tenant";
    public static final String ALARM_PROPAGATE_RELATION_TYPES = "propagate_relation_types";

    /**
     * 告警常量，用于统一引用固定值。
     */
    public static final String ALARM_COMMENT_TABLE_NAME = "alarm_comment";
    public static final String ALARM_COMMENT_ALARM_ID = "alarm_id";
    /**
     * 告警ID常量，用于统一引用固定值。
     */
    public static final String ALARM_COMMENT_USER_ID = USER_ID_PROPERTY;
    public static final String ALARM_COMMENT_TYPE = "type";
    /**
     * 告警常量，用于统一引用固定值。
     */
    public static final String ALARM_COMMENT_COMMENT = "comment";

    /**
     * Entity relation constants.
     */
    /**
     * 关系常量，用于统一引用固定值。
     */
    public static final String RELATION_TABLE_NAME = "relation";
    public static final String RELATION_FROM_ID_PROPERTY = "from_id";
    /**
     * 关系常量，用于统一引用固定值。
     */
    public static final String RELATION_FROM_TYPE_PROPERTY = "from_type";
    public static final String RELATION_TO_ID_PROPERTY = "to_id";
    /**
     * 关系常量，用于统一引用固定值。
     */
    public static final String RELATION_TO_TYPE_PROPERTY = "to_type";
    public static final String RELATION_TYPE_PROPERTY = "relation_type";
    /**
     * 关系常量，用于统一引用固定值。
     */
    public static final String RELATION_TYPE_GROUP_PROPERTY = "relation_type_group";

    /**
     * Device_credentials constants.
     */
    /**
     * 设备凭据常量，用于统一引用固定值。
     */
    public static final String DEVICE_CREDENTIALS_TABLE_NAME = "device_credentials";
    public static final String DEVICE_CREDENTIALS_DEVICE_ID_PROPERTY = DEVICE_ID_PROPERTY;
    /**
     * 设备凭据常量，用于统一引用固定值。
     */
    public static final String DEVICE_CREDENTIALS_CREDENTIALS_TYPE_PROPERTY = "credentials_type";
    public static final String DEVICE_CREDENTIALS_CREDENTIALS_ID_PROPERTY = "credentials_id";
    /**
     * 设备凭据常量，用于统一引用固定值。
     */
    public static final String DEVICE_CREDENTIALS_CREDENTIALS_VALUE_PROPERTY = "credentials_value";

    /**
     * Widgets_bundle constants.
     */
    /**
     * 部件包常量，用于统一引用固定值。
     */
    public static final String WIDGETS_BUNDLE_TABLE_NAME = "widgets_bundle";
    public static final String WIDGETS_BUNDLE_TENANT_ID_PROPERTY = TENANT_ID_PROPERTY;
    /**
     * 部件包常量，用于统一引用固定值。
     */
    public static final String WIDGETS_BUNDLE_ALIAS_PROPERTY = ALIAS_PROPERTY;
    public static final String WIDGETS_BUNDLE_TITLE_PROPERTY = TITLE_PROPERTY;
    /**
     * 部件包常量，用于统一引用固定值。
     */
    public static final String WIDGETS_BUNDLE_IMAGE_PROPERTY = "image";
    public static final String WIDGETS_BUNDLE_DESCRIPTION = "description";
    /**
     * 部件包常量，用于统一引用固定值。
     */
    public static final String WIDGETS_BUNDLE_ORDER = "widgets_bundle_order";

    /**
     * Widget_type constants.
     */
    /**
     * 部件类型常量，用于统一引用固定值。
     */
    public static final String WIDGET_TYPE_TABLE_NAME = "widget_type";
    public static final String WIDGET_TYPE_TENANT_ID_PROPERTY = TENANT_ID_PROPERTY;

    /**
     * 部件类型常量，用于统一引用固定值。
     */
    public static final String WIDGET_TYPE_FQN_PROPERTY = "fqn";
    public static final String WIDGET_TYPE_NAME_PROPERTY = "name";
    /**
     * 部件类型常量，用于统一引用固定值。
     */
    public static final String WIDGET_TYPE_IMAGE_PROPERTY = "image";
    public static final String WIDGET_TYPE_DESCRIPTION_PROPERTY = "description";
    /**
     * 部件类型常量，用于统一引用固定值。
     */
    public static final String WIDGET_TYPE_TAGS_PROPERTY = "tags";
    public static final String WIDGET_TYPE_DESCRIPTOR_PROPERTY = "descriptor";

    /**
     * 部件类型常量，用于统一引用固定值。
     */
    public static final String WIDGET_TYPE_DEPRECATED_PROPERTY = "deprecated";

    /**
     * 部件类型常量，用于统一引用固定值。
     */
    public static final String WIDGET_TYPE_WIDGET_TYPE_PROPERTY = "widget_type";

    /**
     * 部件类型常量，用于统一引用固定值。
     */
    public static final String WIDGET_TYPE_INFO_VIEW_TABLE_NAME = "widget_type_info_view";

    /**
     * Widgets bundle widget constants.
     */
    /**
     * 部件包常量，用于统一引用固定值。
     */
    public static final String WIDGETS_BUNDLE_WIDGET_TABLE_NAME = "widgets_bundle_widget";

    /**
     * 部件类型常量，用于统一引用固定值。
     */
    public static final String WIDGET_TYPE_ORDER_PROPERTY = "widget_type_order";

    /**
     * Dashboard constants.
     */
    /**
     * 仪表盘常量，用于统一引用固定值。
     */
    public static final String DASHBOARD_TABLE_NAME = "dashboard";
    public static final String DASHBOARD_TENANT_ID_PROPERTY = TENANT_ID_PROPERTY;
    /**
     * 仪表盘常量，用于统一引用固定值。
     */
    public static final String DASHBOARD_TITLE_PROPERTY = TITLE_PROPERTY;
    public static final String DASHBOARD_IMAGE_PROPERTY = "image";
    /**
     * 仪表盘常量，用于统一引用固定值。
     */
    public static final String DASHBOARD_CONFIGURATION_PROPERTY = "configuration";
    public static final String DASHBOARD_ASSIGNED_CUSTOMERS_PROPERTY = "assigned_customers";
    /**
     * 仪表盘常量，用于统一引用固定值。
     */
    public static final String DASHBOARD_MOBILE_HIDE_PROPERTY = "mobile_hide";
    public static final String DASHBOARD_MOBILE_ORDER_PROPERTY = "mobile_order";

    /**
     * Plugin component metadata constants.
     */
    /**
     * 名称常量，用于统一引用固定值。
     */
    public static final String COMPONENT_DESCRIPTOR_TABLE_NAME = "component_descriptor";
    public static final String COMPONENT_DESCRIPTOR_TYPE_PROPERTY = "type";
    /**
     * `COMPONENT_DESCRIPTOR_SCOPE_PROPERTY`常量，用于统一引用固定值。
     */
    public static final String COMPONENT_DESCRIPTOR_SCOPE_PROPERTY = "scope";
    public static final String COMPONENT_DESCRIPTOR_CLUSTERING_MODE_PROPERTY = "clustering_mode";
    /**
     * 名称常量，用于统一引用固定值。
     */
    public static final String COMPONENT_DESCRIPTOR_NAME_PROPERTY = "name";
    public static final String COMPONENT_DESCRIPTOR_CLASS_PROPERTY = "clazz";
    /**
     * `COMPONENT_DESCRIPTOR_CONFIGURATION_DESCRIPTOR_PROPERTY`常量，用于统一引用固定值。
     */
    public static final String COMPONENT_DESCRIPTOR_CONFIGURATION_DESCRIPTOR_PROPERTY = "configuration_descriptor";
    public static final String COMPONENT_DESCRIPTOR_CONFIGURATION_VERSION_PROPERTY = "configuration_version";
    /**
     * `COMPONENT_DESCRIPTOR_ACTIONS_PROPERTY`常量，用于统一引用固定值。
     */
    public static final String COMPONENT_DESCRIPTOR_ACTIONS_PROPERTY = "actions";
    public static final String COMPONENT_DESCRIPTOR_HAS_QUEUE_NAME_PROPERTY = "has_queue_name";

    /**
     * Event constants.
     */
    /**
     * 事件常量，用于统一引用固定值。
     */
    public static final String ERROR_EVENT_TABLE_NAME = "error_event";
    public static final String LC_EVENT_TABLE_NAME = "lc_event";
    /**
     * 事件常量，用于统一引用固定值。
     */
    public static final String STATS_EVENT_TABLE_NAME = "stats_event";
    public static final String RULE_NODE_DEBUG_EVENT_TABLE_NAME = "rule_node_debug_event";
    /**
     * 规则链常量，用于统一引用固定值。
     */
    public static final String RULE_CHAIN_DEBUG_EVENT_TABLE_NAME = "rule_chain_debug_event";

    /**
     * 租户ID常量，用于统一引用固定值。
     */
    public static final String EVENT_TENANT_ID_PROPERTY = TENANT_ID_PROPERTY;
    public static final String EVENT_SERVICE_ID_PROPERTY = "service_id";
    /**
     * 实体ID常量，用于统一引用固定值。
     */
    public static final String EVENT_ENTITY_ID_PROPERTY = "entity_id";

    /**
     * 事件常量，用于统一引用固定值。
     */
    public static final String EVENT_MESSAGES_PROCESSED_COLUMN_NAME = "e_messages_processed";
    public static final String EVENT_ERRORS_OCCURRED_COLUMN_NAME = "e_errors_occurred";

    /**
     * 事件常量，用于统一引用固定值。
     */
    public static final String EVENT_METHOD_COLUMN_NAME = "e_method";

    /**
     * 事件常量，用于统一引用固定值。
     */
    public static final String EVENT_TYPE_COLUMN_NAME = "e_type";
    public static final String EVENT_ERROR_COLUMN_NAME = "e_error";
    /**
     * 事件常量，用于统一引用固定值。
     */
    public static final String EVENT_SUCCESS_COLUMN_NAME = "e_success";

    /**
     * 实体ID常量，用于统一引用固定值。
     */
    public static final String EVENT_ENTITY_ID_COLUMN_NAME = "e_entity_id";
    public static final String EVENT_ENTITY_TYPE_COLUMN_NAME = "e_entity_type";
    /**
     * 事件常量，用于统一引用固定值。
     */
    public static final String EVENT_MSG_ID_COLUMN_NAME = "e_msg_id";
    public static final String EVENT_MSG_TYPE_COLUMN_NAME = "e_msg_type";
    /**
     * 事件常量，用于统一引用固定值。
     */
    public static final String EVENT_DATA_TYPE_COLUMN_NAME = "e_data_type";
    public static final String EVENT_RELATION_TYPE_COLUMN_NAME = "e_relation_type";
    /**
     * 事件常量，用于统一引用固定值。
     */
    public static final String EVENT_DATA_COLUMN_NAME = "e_data";
    public static final String EVENT_METADATA_COLUMN_NAME = "e_metadata";
    /**
     * 事件常量，用于统一引用固定值。
     */
    public static final String EVENT_MESSAGE_COLUMN_NAME = "e_message";

    /**
     * `DEBUG_MODE`常量，用于统一引用固定值。
     */
    public static final String DEBUG_MODE = "debug_mode";
    public static final String SINGLETON_MODE = "singleton_mode";
    /**
     * 队列名称常量，用于统一引用固定值。
     */
    public static final String QUEUE_NAME = "queue_name";

    /**
     * Rule chain constants.
     */
    /**
     * 规则链常量，用于统一引用固定值。
     */
    public static final String RULE_CHAIN_TABLE_NAME = "rule_chain";
    public static final String RULE_CHAIN_TENANT_ID_PROPERTY = TENANT_ID_PROPERTY;
    /**
     * 规则链常量，用于统一引用固定值。
     */
    public static final String RULE_CHAIN_NAME_PROPERTY = "name";
    public static final String RULE_CHAIN_TYPE_PROPERTY = "type";
    /**
     * 规则链常量，用于统一引用固定值。
     */
    public static final String RULE_CHAIN_FIRST_RULE_NODE_ID_PROPERTY = "first_rule_node_id";
    public static final String RULE_CHAIN_ROOT_PROPERTY = "root";
    /**
     * 规则链常量，用于统一引用固定值。
     */
    public static final String RULE_CHAIN_CONFIGURATION_PROPERTY = "configuration";

    /**
     * Rule node constants.
     */
    /**
     * 规则节点常量，用于统一引用固定值。
     */
    public static final String RULE_NODE_TABLE_NAME = "rule_node";
    public static final String RULE_NODE_CHAIN_ID_PROPERTY = "rule_chain_id";
    /**
     * 规则节点常量，用于统一引用固定值。
     */
    public static final String RULE_NODE_TYPE_PROPERTY = "type";
    public static final String RULE_NODE_NAME_PROPERTY = "name";
    /**
     * 规则节点常量，用于统一引用固定值。
     */
    public static final String RULE_NODE_VERSION_PROPERTY = "configuration_version";
    public static final String RULE_NODE_CONFIGURATION_PROPERTY = "configuration";

    /**
     * Node state constants.
     */
    /**
     * 规则节点常量，用于统一引用固定值。
     */
    public static final String RULE_NODE_STATE_TABLE_NAME = "rule_node_state";
    public static final String RULE_NODE_STATE_NODE_ID_PROPERTY = "rule_node_id";
    /**
     * 规则节点常量，用于统一引用固定值。
     */
    public static final String RULE_NODE_STATE_ENTITY_TYPE_PROPERTY = "entity_type";
    public static final String RULE_NODE_STATE_ENTITY_ID_PROPERTY = "entity_id";
    /**
     * 规则节点常量，用于统一引用固定值。
     */
    public static final String RULE_NODE_STATE_DATA_PROPERTY = "state_data";

    /**
     * OAuth2 client registration constants.
     */
    /**
     * 名称常量，用于统一引用固定值。
     */
    public static final String OAUTH2_PARAMS_TABLE_NAME = "oauth2_params";
    public static final String OAUTH2_PARAMS_ENABLED_PROPERTY = "enabled";
    /**
     * 租户ID常量，用于统一引用固定值。
     */
    public static final String OAUTH2_PARAMS_TENANT_ID_PROPERTY = TENANT_ID_PROPERTY;

    /**
     * 名称常量，用于统一引用固定值。
     */
    public static final String OAUTH2_REGISTRATION_TABLE_NAME = "oauth2_registration";
    public static final String OAUTH2_DOMAIN_TABLE_NAME = "oauth2_domain";
    /**
     * 名称常量，用于统一引用固定值。
     */
    public static final String OAUTH2_MOBILE_TABLE_NAME = "oauth2_mobile";
    public static final String OAUTH2_PARAMS_ID_PROPERTY = "oauth2_params_id";
    /**
     * 名称常量，用于统一引用固定值。
     */
    public static final String OAUTH2_PKG_NAME_PROPERTY = "pkg_name";
    public static final String OAUTH2_APP_SECRET_PROPERTY = "app_secret";

    /**
     * 名称常量，用于统一引用固定值。
     */
    public static final String OAUTH2_CLIENT_REGISTRATION_TEMPLATE_TABLE_NAME = "oauth2_client_registration_template";
    public static final String OAUTH2_TEMPLATE_PROVIDER_ID_PROPERTY = "provider_id";
    /**
     * 名称常量，用于统一引用固定值。
     */
    public static final String OAUTH2_DOMAIN_NAME_PROPERTY = "domain_name";
    public static final String OAUTH2_DOMAIN_SCHEME_PROPERTY = "domain_scheme";
    /**
     * 客户端常量，用于统一引用固定值。
     */
    public static final String OAUTH2_CLIENT_ID_PROPERTY = "client_id";
    public static final String OAUTH2_CLIENT_SECRET_PROPERTY = "client_secret";
    /**
     * URI 地址常量，用于统一引用固定值。
     */
    public static final String OAUTH2_AUTHORIZATION_URI_PROPERTY = "authorization_uri";
    public static final String OAUTH2_TOKEN_URI_PROPERTY = "token_uri";
    /**
     * `OAUTH2_SCOPE_PROPERTY`常量，用于统一引用固定值。
     */
    public static final String OAUTH2_SCOPE_PROPERTY = "scope";
    public static final String OAUTH2_PLATFORMS_PROPERTY = "platforms";
    /**
     * 用户常量，用于统一引用固定值。
     */
    public static final String OAUTH2_USER_INFO_URI_PROPERTY = "user_info_uri";
    public static final String OAUTH2_USER_NAME_ATTRIBUTE_NAME_PROPERTY = "user_name_attribute_name";
    /**
     * URI 地址常量，用于统一引用固定值。
     */
    public static final String OAUTH2_JWK_SET_URI_PROPERTY = "jwk_set_uri";
    public static final String OAUTH2_CLIENT_AUTHENTICATION_METHOD_PROPERTY = "client_authentication_method";
    /**
     * 显示标签常量，用于统一引用固定值。
     */
    public static final String OAUTH2_LOGIN_BUTTON_LABEL_PROPERTY = "login_button_label";
    public static final String OAUTH2_LOGIN_BUTTON_ICON_PROPERTY = "login_button_icon";
    /**
     * 用户常量，用于统一引用固定值。
     */
    public static final String OAUTH2_ALLOW_USER_CREATION_PROPERTY = "allow_user_creation";
    public static final String OAUTH2_ACTIVATE_USER_PROPERTY = "activate_user";
    /**
     * 类型常量，用于统一引用固定值。
     */
    public static final String OAUTH2_MAPPER_TYPE_PROPERTY = "type";
    public static final String OAUTH2_EMAIL_ATTRIBUTE_KEY_PROPERTY = "basic_email_attribute_key";
    /**
     * 属性常量，用于统一引用固定值。
     */
    public static final String OAUTH2_FIRST_NAME_ATTRIBUTE_KEY_PROPERTY = "basic_first_name_attribute_key";
    public static final String OAUTH2_LAST_NAME_ATTRIBUTE_KEY_PROPERTY = "basic_last_name_attribute_key";
    /**
     * 租户常量，用于统一引用固定值。
     */
    public static final String OAUTH2_TENANT_NAME_STRATEGY_PROPERTY = "basic_tenant_name_strategy";
    public static final String OAUTH2_TENANT_NAME_PATTERN_PROPERTY = "basic_tenant_name_pattern";
    /**
     * 客户常量，用于统一引用固定值。
     */
    public static final String OAUTH2_CUSTOMER_NAME_PATTERN_PROPERTY = "basic_customer_name_pattern";
    public static final String OAUTH2_DEFAULT_DASHBOARD_NAME_PROPERTY = "basic_default_dashboard_name";
    /**
     * `OAUTH2_ALWAYS_FULL_SCREEN_PROPERTY`常量，用于统一引用固定值。
     */
    public static final String OAUTH2_ALWAYS_FULL_SCREEN_PROPERTY = "basic_always_full_screen";
    public static final String OAUTH2_MAPPER_URL_PROPERTY = "custom_url";
    /**
     * 用户名常量，用于统一引用固定值。
     */
    public static final String OAUTH2_MAPPER_USERNAME_PROPERTY = "custom_username";
    public static final String OAUTH2_MAPPER_PASSWORD_PROPERTY = "custom_password";
    /**
     * 映射器常量，用于统一引用固定值。
     */
    public static final String OAUTH2_MAPPER_SEND_TOKEN_PROPERTY = "custom_send_token";
    public static final String OAUTH2_TEMPLATE_COMMENT_PROPERTY = "comment";
    /**
     * 扩展信息常量，用于统一引用固定值。
     */
    public static final String OAUTH2_ADDITIONAL_INFO_PROPERTY = ADDITIONAL_INFO_PROPERTY;
    public static final String OAUTH2_TEMPLATE_ADDITIONAL_INFO_PROPERTY = ADDITIONAL_INFO_PROPERTY;
    /**
     * `OAUTH2_TEMPLATE_LOGIN_BUTTON_ICON_PROPERTY`常量，用于统一引用固定值。
     */
    public static final String OAUTH2_TEMPLATE_LOGIN_BUTTON_ICON_PROPERTY = OAUTH2_LOGIN_BUTTON_ICON_PROPERTY;
    public static final String OAUTH2_TEMPLATE_LOGIN_BUTTON_LABEL_PROPERTY = OAUTH2_LOGIN_BUTTON_LABEL_PROPERTY;
    /**
     * `OAUTH2_TEMPLATE_HELP_LINK_PROPERTY`常量，用于统一引用固定值。
     */
    public static final String OAUTH2_TEMPLATE_HELP_LINK_PROPERTY = "help_link";

    /**
     * Usage Record constants.
     */
    /**
     * 名称常量，用于统一引用固定值。
     */
    public static final String API_USAGE_STATE_TABLE_NAME = "api_usage_state";
    public static final String API_USAGE_STATE_TENANT_ID_COLUMN = TENANT_ID_PROPERTY;
    /**
     * 实体常量，用于统一引用固定值。
     */
    public static final String API_USAGE_STATE_ENTITY_TYPE_COLUMN = ENTITY_TYPE_COLUMN;
    public static final String API_USAGE_STATE_ENTITY_ID_COLUMN = ENTITY_ID_COLUMN;
    /**
     * 状态常量，用于统一引用固定值。
     */
    public static final String API_USAGE_STATE_TRANSPORT_COLUMN = "transport";
    public static final String API_USAGE_STATE_DB_STORAGE_COLUMN = "db_storage";
    /**
     * 状态常量，用于统一引用固定值。
     */
    public static final String API_USAGE_STATE_RE_EXEC_COLUMN = "re_exec";
    public static final String API_USAGE_STATE_JS_EXEC_COLUMN = "js_exec";
    /**
     * 状态常量，用于统一引用固定值。
     */
    public static final String API_USAGE_STATE_TBEL_EXEC_COLUMN = "tbel_exec";
    public static final String API_USAGE_STATE_EMAIL_EXEC_COLUMN = "email_exec";
    /**
     * 状态常量，用于统一引用固定值。
     */
    public static final String API_USAGE_STATE_SMS_EXEC_COLUMN = "sms_exec";
    public static final String API_USAGE_STATE_ALARM_EXEC_COLUMN = "alarm_exec";

    /**
     * Resource constants.
     */
    /**
     * 名称常量，用于统一引用固定值。
     */
    public static final String RESOURCE_TABLE_NAME = "resource";
    public static final String RESOURCE_TENANT_ID_COLUMN = TENANT_ID_COLUMN;
    /**
     * 类型常量，用于统一引用固定值。
     */
    public static final String RESOURCE_TYPE_COLUMN = "resource_type";
    public static final String RESOURCE_KEY_COLUMN = "resource_key";
    /**
     * `RESOURCE_TITLE_COLUMN`常量，用于统一引用固定值。
     */
    public static final String RESOURCE_TITLE_COLUMN = TITLE_PROPERTY;
    public static final String RESOURCE_FILE_NAME_COLUMN = "file_name";
    /**
     * 数据常量，用于统一引用固定值。
     */
    public static final String RESOURCE_DATA_COLUMN = "data";
    public static final String RESOURCE_ETAG_COLUMN = "etag";
    /**
     * `RESOURCE_DESCRIPTOR_COLUMN`常量，用于统一引用固定值。
     */
    public static final String RESOURCE_DESCRIPTOR_COLUMN = "descriptor";
    public static final String RESOURCE_PREVIEW_COLUMN = "preview";
    /**
     * `RESOURCE_IS_PUBLIC_COLUMN`常量，用于统一引用固定值。
     */
    public static final String RESOURCE_IS_PUBLIC_COLUMN = "is_public";
    public static final String PUBLIC_RESOURCE_KEY_COLUMN = "public_resource_key";

    /**
     * Ota Package constants.
     */
    /**
     * 名称常量，用于统一引用固定值。
     */
    public static final String OTA_PACKAGE_TABLE_NAME = "ota_package";
    public static final String OTA_PACKAGE_TENANT_ID_COLUMN = TENANT_ID_COLUMN;
    /**
     * 设备配置常量，用于统一引用固定值。
     */
    public static final String OTA_PACKAGE_DEVICE_PROFILE_ID_COLUMN = "device_profile_id";
    public static final String OTA_PACKAGE_TYPE_COLUMN = "type";
    /**
     * `OTA_PACKAGE_TILE_COLUMN`常量，用于统一引用固定值。
     */
    public static final String OTA_PACKAGE_TILE_COLUMN = TITLE_PROPERTY;
    public static final String OTA_PACKAGE_VERSION_COLUMN = "version";
    /**
     * `OTA_PACKAGE_TAG_COLUMN`常量，用于统一引用固定值。
     */
    public static final String OTA_PACKAGE_TAG_COLUMN = "tag";
    public static final String OTA_PACKAGE_URL_COLUMN = "url";
    /**
     * 名称常量，用于统一引用固定值。
     */
    public static final String OTA_PACKAGE_FILE_NAME_COLUMN = "file_name";
    public static final String OTA_PACKAGE_CONTENT_TYPE_COLUMN = "content_type";
    /**
     * `OTA_PACKAGE_CHECKSUM_ALGORITHM_COLUMN`常量，用于统一引用固定值。
     */
    public static final String OTA_PACKAGE_CHECKSUM_ALGORITHM_COLUMN = "checksum_algorithm";
    public static final String OTA_PACKAGE_CHECKSUM_COLUMN = "checksum";
    /**
     * 数据常量，用于统一引用固定值。
     */
    public static final String OTA_PACKAGE_DATA_COLUMN = "data";
    public static final String OTA_PACKAGE_DATA_SIZE_COLUMN = "data_size";
    /**
     * 扩展信息常量，用于统一引用固定值。
     */
    public static final String OTA_PACKAGE_ADDITIONAL_INFO_COLUMN = ADDITIONAL_INFO_PROPERTY;

    /**
     * Persisted RPC constants.
     */
    /**
     * RPC常量，用于统一引用固定值。
     */
    public static final String RPC_TABLE_NAME = "rpc";
    public static final String RPC_TENANT_ID_COLUMN = TENANT_ID_COLUMN;
    /**
     * 设备ID常量，用于统一引用固定值。
     */
    public static final String RPC_DEVICE_ID = "device_id";
    public static final String RPC_EXPIRATION_TIME = "expiration_time";
    /**
     * RPC常量，用于统一引用固定值。
     */
    public static final String RPC_REQUEST = "request";
    public static final String RPC_RESPONSE = "response";
    /**
     * RPC常量，用于统一引用固定值。
     */
    public static final String RPC_STATUS = "status";
    public static final String RPC_ADDITIONAL_INFO = ADDITIONAL_INFO_PROPERTY;

    /**
     * Edge constants.
     */
    /**
     * 边缘节点常量，用于统一引用固定值。
     */
    public static final String EDGE_TABLE_NAME = "edge";
    public static final String EDGE_TENANT_ID_PROPERTY = TENANT_ID_PROPERTY;
    /**
     * 客户ID常量，用于统一引用固定值。
     */
    public static final String EDGE_CUSTOMER_ID_PROPERTY = CUSTOMER_ID_PROPERTY;
    public static final String EDGE_ROOT_RULE_CHAIN_ID_PROPERTY = "root_rule_chain_id";
    /**
     * 边缘节点常量，用于统一引用固定值。
     */
    public static final String EDGE_NAME_PROPERTY = "name";
    public static final String EDGE_LABEL_PROPERTY = "label";
    /**
     * 边缘节点常量，用于统一引用固定值。
     */
    public static final String EDGE_TYPE_PROPERTY = "type";
    public static final String EDGE_ADDITIONAL_INFO_PROPERTY = ADDITIONAL_INFO_PROPERTY;

    /**
     * 边缘节点常量，用于统一引用固定值。
     */
    public static final String EDGE_ROUTING_KEY_PROPERTY = "routing_key";
    public static final String EDGE_SECRET_PROPERTY = "secret";

    /**
     * Edge queue constants.
     */
    /**
     * 边缘节点常量，用于统一引用固定值。
     */
    public static final String EDGE_EVENT_TABLE_NAME = "edge_event";
    public static final String EDGE_EVENT_TENANT_ID_PROPERTY = TENANT_ID_PROPERTY;
    /**
     * 边缘节点常量，用于统一引用固定值。
     */
    public static final String EDGE_EVENT_SEQUENTIAL_ID_PROPERTY = "seq_id";
    public static final String EDGE_EVENT_EDGE_ID_PROPERTY = "edge_id";
    /**
     * 边缘节点常量，用于统一引用固定值。
     */
    public static final String EDGE_EVENT_TYPE_PROPERTY = "edge_event_type";
    public static final String EDGE_EVENT_ACTION_PROPERTY = "edge_event_action";
    /**
     * 边缘节点常量，用于统一引用固定值。
     */
    public static final String EDGE_EVENT_UID_PROPERTY = "edge_event_uid";
    public static final String EDGE_EVENT_ENTITY_ID_PROPERTY = "entity_id";
    /**
     * 边缘节点常量，用于统一引用固定值。
     */
    public static final String EDGE_EVENT_BODY_PROPERTY = "body";

    /**
     * `EXTERNAL_ID_PROPERTY`常量，用于统一引用固定值。
     */
    public static final String EXTERNAL_ID_PROPERTY = "external_id";

    /**
     * User auth settings constants.
     */
    /**
     * 用户常量，用于统一引用固定值。
     */
    public static final String USER_AUTH_SETTINGS_TABLE_NAME = "user_auth_settings";
    public static final String USER_AUTH_SETTINGS_USER_ID_PROPERTY = USER_ID_PROPERTY;
    /**
     * 用户常量，用于统一引用固定值。
     */
    public static final String USER_AUTH_SETTINGS_TWO_FA_SETTINGS = "two_fa_settings";

    /**
     * Cassandra attributes and timeseries constants.
     */
    /**
     * 时间戳常量，用于统一引用固定值。
     */
    public static final String TS_KV_CF = "ts_kv_cf";
    public static final String TS_KV_PARTITIONS_CF = "ts_kv_partitions_cf";
    /**
     * 时间戳常量，用于统一引用固定值。
     */
    public static final String TS_KV_LATEST_CF = "ts_kv_latest_cf";

    /**
     * 分区常量，用于统一引用固定值。
     */
    public static final String PARTITION_COLUMN = "partition";
    public static final String KEY_COLUMN = "key";
    /**
     * 键常量，用于统一引用固定值。
     */
    public static final String KEY_ID_COLUMN = "key_id";
    public static final String TS_COLUMN = "ts";

    /**
     * Main names of cassandra key-value columns storage.
     */
    /**
     * 值常量，用于统一引用固定值。
     */
    public static final String BOOLEAN_VALUE_COLUMN = "bool_v";
    public static final String STRING_VALUE_COLUMN = "str_v";
    /**
     * 值常量，用于统一引用固定值。
     */
    public static final String LONG_VALUE_COLUMN = "long_v";
    public static final String DOUBLE_VALUE_COLUMN = "dbl_v";
    /**
     * 值常量，用于统一引用固定值。
     */
    public static final String JSON_VALUE_COLUMN = "json_v";

    /**
     * Queue constants.
     */
    /**
     * 租户ID常量，用于统一引用固定值。
     */
    public static final String QUEUE_TENANT_ID_PROPERTY = TENANT_ID_PROPERTY;
    public static final String QUEUE_NAME_PROPERTY = "name";
    /**
     * 队列常量，用于统一引用固定值。
     */
    public static final String QUEUE_TOPIC_PROPERTY = "topic";
    public static final String QUEUE_POLL_INTERVAL_PROPERTY = "poll_interval";
    /**
     * 队列常量，用于统一引用固定值。
     */
    public static final String QUEUE_PARTITIONS_PROPERTY = "partitions";
    public static final String QUEUE_CONSUMER_PER_PARTITION = "consumer_per_partition";
    /**
     * 版本包处理超时时间常量，用于统一引用固定值。
     */
    public static final String QUEUE_PACK_PROCESSING_TIMEOUT_PROPERTY = "pack_processing_timeout";
    public static final String QUEUE_SUBMIT_STRATEGY_PROPERTY = "submit_strategy";
    /**
     * 队列常量，用于统一引用固定值。
     */
    public static final String QUEUE_PROCESSING_STRATEGY_PROPERTY = "processing_strategy";
    public static final String QUEUE_TABLE_NAME = "queue";
    /**
     * 扩展信息常量，用于统一引用固定值。
     */
    public static final String QUEUE_ADDITIONAL_INFO_PROPERTY = ADDITIONAL_INFO_PROPERTY;

    /**
     * Notification constants
     */
    /**
     * 名称常量，用于统一引用固定值。
     */
    public static final String NOTIFICATION_TARGET_TABLE_NAME = "notification_target";
    public static final String NOTIFICATION_TARGET_CONFIGURATION_PROPERTY = "configuration";

    /**
     * 名称常量，用于统一引用固定值。
     */
    public static final String NOTIFICATION_TABLE_NAME = "notification";
    public static final String NOTIFICATION_REQUEST_ID_PROPERTY = "request_id";
    /**
     * 通知常量，用于统一引用固定值。
     */
    public static final String NOTIFICATION_RECIPIENT_ID_PROPERTY = "recipient_id";
    public static final String NOTIFICATION_TYPE_PROPERTY = "type";
    /**
     * 通知常量，用于统一引用固定值。
     */
    public static final String NOTIFICATION_DELIVERY_METHOD_PROPERTY = "delivery_method";
    public static final String NOTIFICATION_SUBJECT_PROPERTY = "subject";
    /**
     * 通知常量，用于统一引用固定值。
     */
    public static final String NOTIFICATION_TEXT_PROPERTY = "body";
    public static final String NOTIFICATION_ADDITIONAL_CONFIG_PROPERTY = "additional_config";
    /**
     * 状态常量，用于统一引用固定值。
     */
    public static final String NOTIFICATION_STATUS_PROPERTY = "status";

    /**
     * 请求常量，用于统一引用固定值。
     */
    public static final String NOTIFICATION_REQUEST_TABLE_NAME = "notification_request";
    public static final String NOTIFICATION_REQUEST_TARGETS_PROPERTY = "targets";
    /**
     * 请求常量，用于统一引用固定值。
     */
    public static final String NOTIFICATION_REQUEST_TEMPLATE_ID_PROPERTY = "template_id";
    public static final String NOTIFICATION_REQUEST_TEMPLATE_PROPERTY = "template";
    /**
     * 请求常量，用于统一引用固定值。
     */
    public static final String NOTIFICATION_REQUEST_INFO_PROPERTY = "info";
    public static final String NOTIFICATION_REQUEST_ORIGINATOR_ENTITY_ID_PROPERTY = "originator_entity_id";
    /**
     * 实体常量，用于统一引用固定值。
     */
    public static final String NOTIFICATION_REQUEST_ORIGINATOR_ENTITY_TYPE_PROPERTY = "originator_entity_type";
    public static final String NOTIFICATION_REQUEST_ADDITIONAL_CONFIG_PROPERTY = "additional_config";
    /**
     * 请求常量，用于统一引用固定值。
     */
    public static final String NOTIFICATION_REQUEST_STATUS_PROPERTY = "status";
    public static final String NOTIFICATION_REQUEST_RULE_ID_PROPERTY = "rule_id";
    /**
     * 请求常量，用于统一引用固定值。
     */
    public static final String NOTIFICATION_REQUEST_STATS_PROPERTY = "stats";

    /**
     * 名称常量，用于统一引用固定值。
     */
    public static final String NOTIFICATION_RULE_TABLE_NAME = "notification_rule";
    public static final String NOTIFICATION_RULE_ENABLED_PROPERTY = "enabled";
    /**
     * 通知常量，用于统一引用固定值。
     */
    public static final String NOTIFICATION_RULE_TEMPLATE_ID_PROPERTY = "template_id";
    public static final String NOTIFICATION_RULE_TRIGGER_TYPE_PROPERTY = "trigger_type";
    /**
     * 配置常量，用于统一引用固定值。
     */
    public static final String NOTIFICATION_RULE_TRIGGER_CONFIG_PROPERTY = "trigger_config";
    public static final String NOTIFICATION_RULE_RECIPIENTS_CONFIG_PROPERTY = "recipients_config";
    /**
     * 配置常量，用于统一引用固定值。
     */
    public static final String NOTIFICATION_RULE_ADDITIONAL_CONFIG_PROPERTY = "additional_config";

    /**
     * 名称常量，用于统一引用固定值。
     */
    public static final String NOTIFICATION_TEMPLATE_TABLE_NAME = "notification_template";
    public static final String NOTIFICATION_TEMPLATE_NOTIFICATION_TYPE_PROPERTY = "notification_type";
    /**
     * 通知常量，用于统一引用固定值。
     */
    public static final String NOTIFICATION_TEMPLATE_CONFIGURATION_PROPERTY = "configuration";

    /**
     * `NONE_AGGREGATION_COLUMNS`常量，用于统一引用固定值。
     */
    protected static final String[] NONE_AGGREGATION_COLUMNS = new String[]{LONG_VALUE_COLUMN, DOUBLE_VALUE_COLUMN, BOOLEAN_VALUE_COLUMN, STRING_VALUE_COLUMN, JSON_VALUE_COLUMN, KEY_COLUMN, TS_COLUMN};

    protected static final String[] COUNT_AGGREGATION_COLUMNS = new String[]{count(LONG_VALUE_COLUMN), count(DOUBLE_VALUE_COLUMN), count(BOOLEAN_VALUE_COLUMN), count(STRING_VALUE_COLUMN), count(JSON_VALUE_COLUMN), max(TS_COLUMN)};

    protected static final String[] MIN_AGGREGATION_COLUMNS =
            ArrayUtils.addAll(COUNT_AGGREGATION_COLUMNS, new String[]{min(LONG_VALUE_COLUMN), min(DOUBLE_VALUE_COLUMN), min(BOOLEAN_VALUE_COLUMN), min(STRING_VALUE_COLUMN), min(JSON_VALUE_COLUMN)});
    protected static final String[] MAX_AGGREGATION_COLUMNS =
            ArrayUtils.addAll(COUNT_AGGREGATION_COLUMNS, new String[]{max(LONG_VALUE_COLUMN), max(DOUBLE_VALUE_COLUMN), max(BOOLEAN_VALUE_COLUMN), max(STRING_VALUE_COLUMN), max(JSON_VALUE_COLUMN)});
    protected static final String[] SUM_AGGREGATION_COLUMNS =
            ArrayUtils.addAll(COUNT_AGGREGATION_COLUMNS, new String[]{sum(LONG_VALUE_COLUMN), sum(DOUBLE_VALUE_COLUMN)});
    /**
     * `AVG_AGGREGATION_COLUMNS`常量，用于统一引用固定值。
     */
    protected static final String[] AVG_AGGREGATION_COLUMNS = SUM_AGGREGATION_COLUMNS;

    /**
     * 功能：执行 `min` 对应的处理。
     * 参数：
     * - `s`：`s` 参数。
     * 返回：文本结果。
     */
    public static String min(String s) {
        return "min(" + s + ")";
    }

    /**
     * 功能：执行 `max` 对应的处理。
     * 参数：
     * - `s`：`s` 参数。
     * 返回：文本结果。
     */
    public static String max(String s) {
        return "max(" + s + ")";
    }

    /**
     * 功能：执行 `sum` 对应的处理。
     * 参数：
     * - `s`：`s` 参数。
     * 返回：文本结果。
     */
    public static String sum(String s) {
        return "sum(" + s + ")";
    }

    /**
     * 功能：执行 `count` 对应的处理。
     * 参数：
     * - `s`：`s` 参数。
     * 返回：文本结果。
     */
    public static String count(String s) {
        return "count(" + s + ")";
    }

    /**
     * 功能：获取`Fetch Column Names`。
     * 参数：
     * - `aggregation`：`aggregation` 参数。
     * 返回：处理结果。
     */
    public static String[] getFetchColumnNames(Aggregation aggregation) {
        switch (aggregation) {
            case NONE:
                return NONE_AGGREGATION_COLUMNS;
            case MIN:
                return MIN_AGGREGATION_COLUMNS;
            case MAX:
                return MAX_AGGREGATION_COLUMNS;
            case SUM:
                return SUM_AGGREGATION_COLUMNS;
            case COUNT:
                return COUNT_AGGREGATION_COLUMNS;
            case AVG:
                return AVG_AGGREGATION_COLUMNS;
            default:
                throw new RuntimeException("Aggregation type: " + aggregation + " is not supported!");
        }
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`ModelConstants` 在 ThingsBoard DAO 模块 中承担持久化实体映射类型职责，核心目的是描述 ThingsBoard 领域对象与 SQL/Cassandra 存储结构之间的字段映射、索引关系和序列化边界。
 * 2. 核心流程：从数据库行或 Common DTO 构造实体对象，经过 ORM 管理后再转换回上层数据契约。
 * 3. 关键依赖：主要依赖或协作对象包括JPA/Hibernate、Repository、DAO Service、Common DTO、JSON 序列化和数据库迁移脚本。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
