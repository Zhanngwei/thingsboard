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

import lombok.Getter;

/**
 * 中文说明：
 * 1. 类目的：`ApiUsageRecordKey` 是ThingsBoard Common 模块中的公共数据模型类型，用于承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 生命周期：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Value Object / Builder。
 */
public enum ApiUsageRecordKey {

    TRANSPORT_MSG_COUNT(ApiFeature.TRANSPORT, "transportMsgCount", "transportMsgLimit", "message"),
    TRANSPORT_DP_COUNT(ApiFeature.TRANSPORT, "transportDataPointsCount", "transportDataPointsLimit", "data point"),
    STORAGE_DP_COUNT(ApiFeature.DB, "storageDataPointsCount", "storageDataPointsLimit", "data point"),
    RE_EXEC_COUNT(ApiFeature.RE, "ruleEngineExecutionCount", "ruleEngineExecutionLimit", "Rule Engine execution"),
    JS_EXEC_COUNT(ApiFeature.JS, "jsExecutionCount", "jsExecutionLimit", "JavaScript execution"),
    TBEL_EXEC_COUNT(ApiFeature.TBEL, "tbelExecutionCount", "tbelExecutionLimit", "Tbel execution"),
    EMAIL_EXEC_COUNT(ApiFeature.EMAIL, "emailCount", "emailLimit", "email message"),
    SMS_EXEC_COUNT(ApiFeature.SMS, "smsCount", "smsLimit", "SMS message"),
    CREATED_ALARMS_COUNT(ApiFeature.ALARM, "createdAlarmsCount", "createdAlarmsLimit", "alarm"),
    ACTIVE_DEVICES("activeDevicesCount"),
    INACTIVE_DEVICES("inactiveDevicesCount");

    /**
     * `JS_RECORD_KEYS`常量，用于统一引用固定值。
     */
    private static final ApiUsageRecordKey[] JS_RECORD_KEYS = {JS_EXEC_COUNT};
    private static final ApiUsageRecordKey[] TBEL_RECORD_KEYS = {TBEL_EXEC_COUNT};
    /**
     * `RE_RECORD_KEYS`常量，用于统一引用固定值。
     */
    private static final ApiUsageRecordKey[] RE_RECORD_KEYS = {RE_EXEC_COUNT};
    private static final ApiUsageRecordKey[] DB_RECORD_KEYS = {STORAGE_DP_COUNT};
    /**
     * 传输层常量，用于统一引用固定值。
     */
    private static final ApiUsageRecordKey[] TRANSPORT_RECORD_KEYS = {TRANSPORT_MSG_COUNT, TRANSPORT_DP_COUNT};
    private static final ApiUsageRecordKey[] EMAIL_RECORD_KEYS = {EMAIL_EXEC_COUNT};
    /**
     * `SMS_RECORD_KEYS`常量，用于统一引用固定值。
     */
    private static final ApiUsageRecordKey[] SMS_RECORD_KEYS = {SMS_EXEC_COUNT};
    private static final ApiUsageRecordKey[] ALARM_RECORD_KEYS = {CREATED_ALARMS_COUNT};

    /**
     * 功能项，表示当前对象的对应属性。
     */
    @Getter
    private final ApiFeature apiFeature;
    /**
     * 键，用于定位映射、配置或数据项。
     */
    @Getter
    private final String apiCountKey;
    /**
     * 键，用于定位映射、配置或数据项。
     */
    @Getter
    private final String apiLimitKey;
    /**
     * 显示标签，用于展示或标识当前对象。
     */
    @Getter
    private final String unitLabel;
    /**
     * 是否满足计数器条件。
     */
    @Getter
    private final boolean counter;

    ApiUsageRecordKey(ApiFeature apiFeature, String apiCountKey, String apiLimitKey, String unitLabel) {
        this(apiFeature, apiCountKey, apiLimitKey, unitLabel, true);
    }

    ApiUsageRecordKey(String apiCountKey) {
        this(null, apiCountKey, null, null, false);
    }

    ApiUsageRecordKey(ApiFeature apiFeature, String apiCountKey, String apiLimitKey, String unitLabel, boolean counter) {
        this.apiFeature = apiFeature;
        this.apiCountKey = apiCountKey;
        this.apiLimitKey = apiLimitKey;
        this.unitLabel = unitLabel;
        this.counter = counter;
    }

    /**
     * 功能：获取`Keys`。
     * 参数：
     * - `feature`：`feature` 参数。
     * 返回：处理结果。
     */
    public static ApiUsageRecordKey[] getKeys(ApiFeature feature) {
        switch (feature) {
            case TRANSPORT:
                return TRANSPORT_RECORD_KEYS;
            case DB:
                return DB_RECORD_KEYS;
            case RE:
                return RE_RECORD_KEYS;
            case JS:
                return JS_RECORD_KEYS;
            case TBEL:
                return TBEL_RECORD_KEYS;
            case EMAIL:
                return EMAIL_RECORD_KEYS;
            case SMS:
                return SMS_RECORD_KEYS;
            case ALARM:
                return ALARM_RECORD_KEYS;
            default:
                return new ApiUsageRecordKey[]{};
        }
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`ApiUsageRecordKey` 在 ThingsBoard Common 模块 中承担公共数据模型类型职责，核心目的是承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 核心流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
 * 3. 关键依赖：主要依赖或协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
