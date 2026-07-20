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
package org.thingsboard.server.transport.lwm2m.client;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ReadResponse;

import javax.security.auth.Destroyable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 中文说明：
 * 1. `LwM2mTemperatureSensor` 是 ThingsBoard Application 中负责 LwM2M 接入或传输适配的类型。
 * 2. 它处理连接、会话、协议消息或平台传输消息之间的转换。
 * 3. 类中的状态和配置用于控制当前协议交互的具体行为。
 * 4. 直接依赖的类型边界包括 `BaseInstanceEnabler`、`Destroyable`。
 * 5. 单独的传输类型可以隔离协议细节，使平台内部继续使用统一消息模型。
 * 6. 阅读时重点关注入站消息入口、会话状态和消息提交位置。
 */
@Slf4j
public class LwM2mTemperatureSensor extends BaseInstanceEnabler implements Destroyable {

    /**
     * 单位常量，用于统一引用固定值。
     */
    private static final String UNIT_CELSIUS = "cel";
    private double currentTemp = 20d;
    /**
     * 值，保存当前处理得到的具体内容。
     */
    private double minMeasuredValue = currentTemp;
    private double maxMeasuredValue = currentTemp;
    protected static final Random RANDOM = new Random();
    private static final List<Integer> supportedResources = Arrays.asList(5601, 5602, 5700, 5701);

    /**
     * 功能：创建 `LwM2mTemperatureSensor` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public LwM2mTemperatureSensor() {

    }

    /**
     * 功能：创建 `LwM2mTemperatureSensor` 实例，并初始化必要字段。
     * 参数：
     * - `executorService`：服务对象。
     * - `id`：`id`ID。
     * 返回：新创建的对象实例。
     */
    public LwM2mTemperatureSensor(ScheduledExecutorService executorService, Integer id) {
        try {
            if (id != null) this.setId(id);
        executorService.scheduleWithFixedDelay(this::adjustTemperature, 2000, 2000, TimeUnit.MILLISECONDS);
        } catch (Throwable e) {
            log.error("[{}]Throwable", e.toString());
            e.printStackTrace();
        }
    }

    /**
     * 功能：执行 `read` 对应的处理。
     * 参数：
     * - `identity`：实体对象。
     * - `resourceId`：`resourceId`ID。
     * 返回：处理结果。
     */
    @Override
    public synchronized ReadResponse read(ServerIdentity identity, int resourceId) {
        log.info("Read on Temperature resource /[{}]/[{}]/[{}]", getModel().id, getId(), resourceId);
        switch (resourceId) {
            case 5601:
                return ReadResponse.success(resourceId, getTwoDigitValue(minMeasuredValue));
            case 5602:
                return ReadResponse.success(resourceId, getTwoDigitValue(maxMeasuredValue));
            case 5700:
                return ReadResponse.success(resourceId, getTwoDigitValue(currentTemp));
            case 5701:
                return ReadResponse.success(resourceId, UNIT_CELSIUS);
            default:
                return super.read(identity, resourceId);
        }
    }

    /**
     * 功能：执行 `execute` 对应的处理。
     * 参数：
     * - `identity`：实体对象。
     * - `resourceId`：`resourceId`ID。
     * - `params`：`params` 参数。
     * 返回：处理结果。
     */
    @Override
    public synchronized ExecuteResponse execute(ServerIdentity identity, int resourceId, String params) {
        log.info("Execute on Temperature resource /[{}]/[{}]/[{}]", getModel().id, getId(), resourceId);
        switch (resourceId) {
            case 5605:
                resetMinMaxMeasuredValues();
                return ExecuteResponse.success();
            default:
                return super.execute(identity, resourceId, params);
        }
    }

    /**
     * 功能：获取值。
     * 参数：
     * - `value`：值。
     * 返回：数值结果。
     */
    private double getTwoDigitValue(double value) {
        BigDecimal toBeTruncated = BigDecimal.valueOf(value);
        return toBeTruncated.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * 功能：执行 `adjustTemperature` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    private void adjustTemperature() {
        float delta = (RANDOM.nextInt(20) - 10) / 10f;
        currentTemp += delta;
        Integer changedResource = adjustMinMaxMeasuredValue(currentTemp);
        fireResourceChange(5700);
        if (changedResource != null) {
            fireResourceChange(changedResource);
        }
    }

    /**
     * 功能：执行 `adjustMinMaxMeasuredValue` 对应的处理。
     * 参数：
     * - `newTemperature`：`newTemperature` 参数。
     * 返回：数值结果。
     */
    private synchronized Integer adjustMinMaxMeasuredValue(double newTemperature) {
        if (newTemperature > maxMeasuredValue) {
            maxMeasuredValue = newTemperature;
            return 5602;
        } else if (newTemperature < minMeasuredValue) {
            minMeasuredValue = newTemperature;
            return 5601;
        } else {
            return null;
        }
    }

    /**
     * 功能：执行 `resetMinMaxMeasuredValues` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    private void resetMinMaxMeasuredValues() {
        minMeasuredValue = currentTemp;
        maxMeasuredValue = currentTemp;
    }

    /**
     * 功能：获取`Available Resource Ids`。
     * 参数：
     * - `model`：`model` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<Integer> getAvailableResourceIds(ObjectModel model) {
        return supportedResources;
    }

    /**
     * 功能：执行 `destroy` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void destroy() {
    }


}
