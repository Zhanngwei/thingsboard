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
package org.thingsboard.server.service.apiusage;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.thingsboard.server.common.data.ApiFeature;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.ApiUsageStateValue;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.tools.SchedulerUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 中文说明：
 * 1. 类目的：`BaseApiUsageState` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
public abstract class BaseApiUsageState {
    private final Map<ApiUsageRecordKey, Long> currentCycleValues = new ConcurrentHashMap<>();
    private final Map<ApiUsageRecordKey, Long> currentHourValues = new ConcurrentHashMap<>();

    private final Map<ApiUsageRecordKey, Map<String, Long>> lastGaugesByServiceId = new HashMap<>();
    private final Map<ApiUsageRecordKey, Long> gaugesReportCycles = new HashMap<>();

    /**
     * 状态，表示当前对象所处状态。
     */
    @Getter
    private final ApiUsageState apiUsageState;
    /**
     * 时间戳，用于标识当前数据或事件发生的时间。
     */
    @Getter
    private volatile long currentCycleTs;
    /**
     * 时间戳，用于标识当前数据或事件发生的时间。
     */
    @Getter
    private volatile long nextCycleTs;
    /**
     * 时间戳，用于标识当前数据或事件发生的时间。
     */
    @Getter
    private volatile long currentHourTs;

    /**
     * 时间间隔，用于控制时间范围或等待时长。
     */
    @Setter
    private long gaugeReportInterval;

    /**
     * 功能：创建 `BaseApiUsageState` 实例，并初始化必要字段。
     * 参数：
     * - `apiUsageState`：`apiUsageState` 参数。
     * 返回：新创建的对象实例。
     */
    public BaseApiUsageState(ApiUsageState apiUsageState) {
        this.apiUsageState = apiUsageState;
        this.currentCycleTs = SchedulerUtils.getStartOfCurrentMonth();
        this.nextCycleTs = SchedulerUtils.getStartOfNextMonth();
        this.currentHourTs = SchedulerUtils.getStartOfCurrentHour();
    }

    /**
     * 功能：执行 `calculate` 对应的处理。
     * 参数：
     * - `key`：键。
     * - `value`：值。
     * - `serviceId`：服务ID。
     * 返回：处理结果。
     */
    public StatsCalculationResult calculate(ApiUsageRecordKey key, long value, String serviceId) {
        long currentValue = get(key);
        long currentHourlyValue = getHourly(key);

        StatsCalculationResult result;
        if (key.isCounter()) {
            result = StatsCalculationResult.builder()
                    .newValue(currentValue + value).valueChanged(true)
                    .newHourlyValue(currentHourlyValue + value).hourlyValueChanged(true)
                    .build();
        } else {
            Long newGaugeValue = calculateGauge(key, value, serviceId);
            long newValue = newGaugeValue != null ? newGaugeValue : currentValue;
            long newHourlyValue = newGaugeValue != null ? Math.max(newGaugeValue, currentHourlyValue) : currentHourlyValue;
            result = StatsCalculationResult.builder()
                    .newValue(newValue).valueChanged(newValue != currentValue || !currentCycleValues.containsKey(key))
                    .newHourlyValue(newHourlyValue).hourlyValueChanged(newHourlyValue != currentHourlyValue || !currentHourValues.containsKey(key))
                    .build();
        }

        set(key, result.getNewValue());
        setHourly(key, result.getNewHourlyValue());
        return result;
    }

    /**
     * 功能：执行 `calculateGauge` 对应的处理。
     * 参数：
     * - `key`：键。
     * - `value`：值。
     * - `serviceId`：服务ID。
     * 返回：数值结果。
     */
    private Long calculateGauge(ApiUsageRecordKey key, long value, String serviceId) {
        Map<String, Long> lastByServiceId = lastGaugesByServiceId.computeIfAbsent(key, k -> {
            gaugesReportCycles.put(key, System.currentTimeMillis());
            return new HashMap<>();
        });
        lastByServiceId.put(serviceId, value);

        Long gaugeReportCycle = gaugesReportCycles.get(key);
        if (gaugeReportCycle <= System.currentTimeMillis() - gaugeReportInterval) {
            long newValue = lastByServiceId.values().stream().mapToLong(Long::longValue).sum();
            lastGaugesByServiceId.remove(key);
            gaugesReportCycles.remove(key);
            return newValue;
        } else {
            return null;
        }
    }

    /**
     * 功能：执行 `set` 对应的处理。
     * 参数：
     * - `key`：键。
     * - `value`：值。
     * 返回：无。
     */
    public void set(ApiUsageRecordKey key, Long value) {
        currentCycleValues.put(key, value);
    }

    /**
     * 功能：执行 `get` 对应的处理。
     * 参数：
     * - `key`：键。
     * 返回：数值结果。
     */
    public long get(ApiUsageRecordKey key) {
        return currentCycleValues.getOrDefault(key, 0L);
    }

    /**
     * 功能：更新`Hourly`。
     * 参数：
     * - `key`：键。
     * - `value`：值。
     * 返回：无。
     */
    public void setHourly(ApiUsageRecordKey key, Long value) {
        currentHourValues.put(key, value);
    }

    /**
     * 功能：获取`Hourly`。
     * 参数：
     * - `key`：键。
     * 返回：数值结果。
     */
    public long getHourly(ApiUsageRecordKey key) {
        return currentHourValues.getOrDefault(key, 0L);
    }

    /**
     * 功能：更新`Hour`。
     * 参数：
     * - `currentHourTs`：时间戳。
     * 返回：无。
     */
    public void setHour(long currentHourTs) {
        this.currentHourTs = currentHourTs;
        currentHourValues.clear();
        lastGaugesByServiceId.clear();
        gaugesReportCycles.clear();
    }

    /**
     * 功能：更新`Cycles`。
     * 参数：
     * - `currentCycleTs`：时间戳。
     * - `nextCycleTs`：时间戳。
     * 返回：无。
     */
    public void setCycles(long currentCycleTs, long nextCycleTs) {
        this.currentCycleTs = currentCycleTs;
        this.nextCycleTs = nextCycleTs;
        currentCycleValues.clear();
    }

    /**
     * 功能：处理事件。
     * 参数：无。
     * 返回：无。
     */
    public void onRepartitionEvent() {
        lastGaugesByServiceId.clear();
        gaugesReportCycles.clear();
    }

    /**
     * 功能：获取值。
     * 参数：
     * - `feature`：`feature` 参数。
     * 返回：处理结果。
     */
    public ApiUsageStateValue getFeatureValue(ApiFeature feature) {
        switch (feature) {
            case TRANSPORT:
                return apiUsageState.getTransportState();
            case RE:
                return apiUsageState.getReExecState();
            case DB:
                return apiUsageState.getDbStorageState();
            case JS:
                return apiUsageState.getJsExecState();
            case TBEL:
                return apiUsageState.getTbelExecState();
            case EMAIL:
                return apiUsageState.getEmailExecState();
            case SMS:
                return apiUsageState.getSmsExecState();
            case ALARM:
                return apiUsageState.getAlarmExecState();
            default:
                return ApiUsageStateValue.ENABLED;
        }
    }

    /**
     * 功能：更新值。
     * 参数：
     * - `feature`：`feature` 参数。
     * - `value`：值。
     * 返回：判断结果。
     */
    public boolean setFeatureValue(ApiFeature feature, ApiUsageStateValue value) {
        ApiUsageStateValue currentValue = getFeatureValue(feature);
        switch (feature) {
            case TRANSPORT:
                apiUsageState.setTransportState(value);
                break;
            case RE:
                apiUsageState.setReExecState(value);
                break;
            case DB:
                apiUsageState.setDbStorageState(value);
                break;
            case JS:
                apiUsageState.setJsExecState(value);
                break;
            case TBEL:
                apiUsageState.setTbelExecState(value);
                break;
            case EMAIL:
                apiUsageState.setEmailExecState(value);
                break;
            case SMS:
                apiUsageState.setSmsExecState(value);
                break;
            case ALARM:
                apiUsageState.setAlarmExecState(value);
                break;
        }
        return !currentValue.equals(value);
    }

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：处理结果。
     */
    public abstract EntityType getEntityType();

    /**
     * 功能：获取租户ID。
     * 参数：无。
     * 返回：处理结果。
     */
    public TenantId getTenantId() {
        return getApiUsageState().getTenantId();
    }

    /**
     * 功能：获取实体ID。
     * 参数：无。
     * 返回：处理结果。
     */
    public EntityId getEntityId() {
        return getApiUsageState().getEntityId();
    }

    /**
     * 中文说明：
     * 1. 类目的：`StatsCalculationResult` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
     * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
     * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
     * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 Service / Facade。
     */
    @Data
    @Builder
    public static class StatsCalculationResult {
        /**
         * 值，保存当前处理得到的具体内容。
         */
        private final long newValue;
        private final boolean valueChanged;
        /**
         * 值，保存当前处理得到的具体内容。
         */
        private final long newHourlyValue;
        private final boolean hourlyValueChanged;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`BaseApiUsageState` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
