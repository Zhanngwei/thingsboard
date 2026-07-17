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
 * @author Andrew Shvayka
 */
/**
 * 中文说明：
 * 1. `DataConstants` 是 ThingsBoard Common Data 中处理 `Constants` 通用操作的工具类型。
 * 2. 它提供无状态或轻量的复用方法，减少多个调用点的重复实现。
 * 3. 方法通常完成格式化、校验、计算或简单对象构造。
 * 4. 它直接协作于方法参数和返回值所代表的数据类型。
 * 5. 集中工具方法可以统一边界行为，并降低细节变化对调用方的影响。
 * 6. 阅读时重点关注输入约束、边界值和方法是否修改传入对象。
 */
public class DataConstants {

    /**
     * 租户常量，用于统一引用固定值。
     */
    public static final String TENANT = "TENANT";
    public static final String CUSTOMER = "CUSTOMER";
    /**
     * 设备常量，用于统一引用固定值。
     */
    public static final String DEVICE = "DEVICE";

    /**
     * `SCOPE`常量，用于统一引用固定值。
     */
    public static final String SCOPE = "scope";
    public static final String CLIENT_SCOPE = "CLIENT_SCOPE";
    /**
     * 服务端常量，用于统一引用固定值。
     */
    public static final String SERVER_SCOPE = "SERVER_SCOPE";
    public static final String SHARED_SCOPE = "SHARED_SCOPE";
    /**
     * 设备常量，用于统一引用固定值。
     */
    public static final String NOTIFY_DEVICE_METADATA_KEY = "notifyDevice";
    public static final String LATEST_TS = "LATEST_TS";
    /**
     * 告警常量，用于统一引用固定值。
     */
    public static final String IS_NEW_ALARM = "isNewAlarm";
    public static final String IS_EXISTING_ALARM = "isExistingAlarm";
    /**
     * 告警常量，用于统一引用固定值。
     */
    public static final String IS_SEVERITY_UPDATED_ALARM = "isSeverityUpdated";
    public static final String IS_CLEARED_ALARM = "isClearedAlarm";
    /**
     * 告警常量，用于统一引用固定值。
     */
    public static final String ALARM_CONDITION_REPEATS = "alarmConditionRepeats";
    public static final String ALARM_CONDITION_DURATION = "alarmConditionDuration";
    /**
     * `PERSISTENT`常量，用于统一引用固定值。
     */
    public static final String PERSISTENT = "persistent";
    public static final String TIMEOUT = "timeout";
    /**
     * 过期时间常量，用于统一引用固定值。
     */
    public static final String EXPIRATION_TIME = "expirationTime";
    public static final String ADDITIONAL_INFO = "additionalInfo";
    /**
     * `RETRIES`常量，用于统一引用固定值。
     */
    public static final String RETRIES = "retries";
    public static final String EDGE_ID = "edgeId";
    /**
     * 设备ID常量，用于统一引用固定值。
     */
    public static final String DEVICE_ID = "deviceId";
    public static final String GATEWAY_PARAMETER = "gateway";
    /**
     * 名称常量，用于统一引用固定值。
     */
    public static final String COAP_TRANSPORT_NAME = "COAP";
    public static final String LWM2M_TRANSPORT_NAME = "LWM2M";
    /**
     * 名称常量，用于统一引用固定值。
     */
    public static final String MQTT_TRANSPORT_NAME = "MQTT";
    public static final String HTTP_TRANSPORT_NAME = "HTTP";
    /**
     * 名称常量，用于统一引用固定值。
     */
    public static final String SNMP_TRANSPORT_NAME = "SNMP";
    public static final String MAXIMUM_NUMBER_OF_DEVICES_REACHED = "Maximum number of devices reached!";
    /**
     * 图片资源常量，用于统一引用固定值。
     */
    public static final String TB_IMAGE_PREFIX = "tb-image;";


    /**
     * 功能：执行 `allScopes` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public static final String[] allScopes() {
        return new String[]{CLIENT_SCOPE, SHARED_SCOPE, SERVER_SCOPE};
    }

    /**
     * 告警常量，用于统一引用固定值。
     */
    public static final String ALARM = "ALARM";
    public static final String IN = "IN";
    /**
     * `OUT`常量，用于统一引用固定值。
     */
    public static final String OUT = "OUT";

    /**
     * 事件常量，用于统一引用固定值。
     */
    public static final String INACTIVITY_EVENT = "INACTIVITY_EVENT";
    public static final String CONNECT_EVENT = "CONNECT_EVENT";
    /**
     * 事件常量，用于统一引用固定值。
     */
    public static final String DISCONNECT_EVENT = "DISCONNECT_EVENT";
    public static final String ACTIVITY_EVENT = "ACTIVITY_EVENT";

    /**
     * 实体常量，用于统一引用固定值。
     */
    public static final String ENTITY_CREATED = "ENTITY_CREATED";
    public static final String ENTITY_UPDATED = "ENTITY_UPDATED";
    /**
     * 实体常量，用于统一引用固定值。
     */
    public static final String ENTITY_DELETED = "ENTITY_DELETED";
    public static final String ENTITY_ASSIGNED = "ENTITY_ASSIGNED";
    /**
     * 实体常量，用于统一引用固定值。
     */
    public static final String ENTITY_UNASSIGNED = "ENTITY_UNASSIGNED";
    public static final String ATTRIBUTES_UPDATED = "ATTRIBUTES_UPDATED";
    /**
     * `ATTRIBUTES_DELETED`常量，用于统一引用固定值。
     */
    public static final String ATTRIBUTES_DELETED = "ATTRIBUTES_DELETED";
    public static final String TIMESERIES_UPDATED = "TIMESERIES_UPDATED";
    /**
     * 时序数据常量，用于统一引用固定值。
     */
    public static final String TIMESERIES_DELETED = "TIMESERIES_DELETED";
    public static final String ALARM_ACK = "ALARM_ACK";
    /**
     * 告警常量，用于统一引用固定值。
     */
    public static final String ALARM_CLEAR = "ALARM_CLEAR";
    public static final String ALARM_ASSIGNED = "ALARM_ASSIGNED";
    /**
     * 告警常量，用于统一引用固定值。
     */
    public static final String ALARM_UNASSIGNED = "ALARM_UNASSIGNED";
    public static final String ALARM_DELETE = "ALARM_DELETE";
    /**
     * `COMMENT_CREATED`常量，用于统一引用固定值。
     */
    public static final String COMMENT_CREATED = "COMMENT_CREATED";
    public static final String COMMENT_UPDATED = "COMMENT_UPDATED";
    /**
     * 租户常量，用于统一引用固定值。
     */
    public static final String ENTITY_ASSIGNED_FROM_TENANT = "ENTITY_ASSIGNED_FROM_TENANT";
    public static final String ENTITY_ASSIGNED_TO_TENANT = "ENTITY_ASSIGNED_TO_TENANT";
    /**
     * `PROVISION_SUCCESS`常量，用于统一引用固定值。
     */
    public static final String PROVISION_SUCCESS = "PROVISION_SUCCESS";
    public static final String PROVISION_FAILURE = "PROVISION_FAILURE";
    /**
     * 实体常量，用于统一引用固定值。
     */
    public static final String ENTITY_ASSIGNED_TO_EDGE = "ENTITY_ASSIGNED_TO_EDGE";
    public static final String ENTITY_UNASSIGNED_FROM_EDGE = "ENTITY_UNASSIGNED_FROM_EDGE";

    /**
     * 关系常量，用于统一引用固定值。
     */
    public static final String RELATION_ADD_OR_UPDATE = "RELATION_ADD_OR_UPDATE";
    public static final String RELATION_DELETED = "RELATION_DELETED";
    /**
     * `RELATIONS_DELETED`常量，用于统一引用固定值。
     */
    public static final String RELATIONS_DELETED = "RELATIONS_DELETED";

    /**
     * 设备常量，用于统一引用固定值。
     */
    public static final String RPC_CALL_FROM_SERVER_TO_DEVICE = "RPC_CALL_FROM_SERVER_TO_DEVICE";

    /**
     * RPC常量，用于统一引用固定值。
     */
    public static final String RPC_QUEUED = "RPC_QUEUED";
    public static final String RPC_SENT = "RPC_SENT";
    /**
     * RPC常量，用于统一引用固定值。
     */
    public static final String RPC_DELIVERED = "RPC_DELIVERED";
    public static final String RPC_SUCCESSFUL = "RPC_SUCCESSFUL";
    /**
     * RPC常量，用于统一引用固定值。
     */
    public static final String RPC_TIMEOUT = "RPC_TIMEOUT";
    public static final String RPC_EXPIRED = "RPC_EXPIRED";
    /**
     * RPC常量，用于统一引用固定值。
     */
    public static final String RPC_FAILED = "RPC_FAILED";
    public static final String RPC_DELETED = "RPC_DELETED";

    /**
     * 键常量，用于统一引用固定值。
     */
    public static final String DEFAULT_SECRET_KEY = "";
    public static final String SECRET_KEY_FIELD_NAME = "secretKey";
    /**
     * 名称常量，用于统一引用固定值。
     */
    public static final String DURATION_MS_FIELD_NAME = "durationMs";

    /**
     * `PROVISION`常量，用于统一引用固定值。
     */
    public static final String PROVISION = "provision";
    public static final String PROVISION_KEY = "provisionDeviceKey";
    /**
     * 密钥常量，用于统一引用固定值。
     */
    public static final String PROVISION_SECRET = "provisionDeviceSecret";

    /**
     * 设备常量，用于统一引用固定值。
     */
    public static final String DEFAULT_DEVICE_TYPE = "default";
    public static final String DEVICE_NAME = "deviceName";
    /**
     * 设备常量，用于统一引用固定值。
     */
    public static final String DEVICE_TYPE = "deviceType";
    public static final String CERT_PUB_KEY = "x509CertPubKey";
    /**
     * 凭据常量，用于统一引用固定值。
     */
    public static final String CREDENTIALS_TYPE = "credentialsType";
    public static final String TOKEN = "token";
    /**
     * `HASH`常量，用于统一引用固定值。
     */
    public static final String HASH = "hash";
    public static final String CLIENT_ID = "clientId";
    /**
     * 用户名常量，用于统一引用固定值。
     */
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    /**
     * 边缘节点常量，用于统一引用固定值。
     */
    public static final String EDGE_MSG_SOURCE = "edge";
    public static final String MSG_SOURCE_KEY = "source";
    /**
     * 边缘节点常量，用于统一引用固定值。
     */
    public static final String EDGE_VERSION_ATTR_KEY = "edgeVersion";

    /**
     * `LAST_CONNECTED_GATEWAY`常量，用于统一引用固定值。
     */
    public static final String LAST_CONNECTED_GATEWAY = "lastConnectedGateway";

    /**
     * 主题常量，用于统一引用固定值。
     */
    public static final String MQTT_TOPIC = "mqttTopic";

    /**
     * 队列名称常量，用于统一引用固定值。
     */
    public static final String MAIN_QUEUE_NAME = "Main";
    public static final String MAIN_QUEUE_TOPIC = "tb_rule_engine.main";
    /**
     * 队列名称常量，用于统一引用固定值。
     */
    public static final String HP_QUEUE_NAME = "HighPriority";
    public static final String HP_QUEUE_TOPIC = "tb_rule_engine.hp";
    /**
     * 队列名称常量，用于统一引用固定值。
     */
    public static final String SQ_QUEUE_NAME = "SequentialByOriginator";
    public static final String SQ_QUEUE_TOPIC = "tb_rule_engine.sq";
    /**
     * 队列名称常量，用于统一引用固定值。
     */
    public static final String QUEUE_NAME = "queueName";

}
