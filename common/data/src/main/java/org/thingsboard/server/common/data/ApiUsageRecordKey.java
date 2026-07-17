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
 * 1. `ApiUsageRecordKey` 是 ThingsBoard Common Data 中定义用量统计固定取值的枚举类型。
 * 2. 它列出当前流程允许使用的有限状态、模式或类别。
 * 3. 枚举值可携带与该选项关联的标识、名称或处理参数。
 * 4. 它直接协作于使用该枚举进行分支判断或序列化的类型。
 * 5. 使用枚举可以限制非法取值，并让分支语义在源码中保持明确。
 * 6. 阅读时重点关注各枚举值含义、附加字段和反向查找方法。
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
