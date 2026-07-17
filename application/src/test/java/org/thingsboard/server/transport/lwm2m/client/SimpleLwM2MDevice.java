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
import org.eclipse.leshan.core.Destroyable;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PrimitiveIterator;
import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 中文说明：
 * 1. `SimpleLwM2MDevice` 是 ThingsBoard Application 中负责设备接入或传输适配的类型。
 * 2. 它处理连接、会话、协议消息或平台传输消息之间的转换。
 * 3. 类中的状态和配置用于控制当前协议交互的具体行为。
 * 4. 直接依赖的类型边界包括 `BaseInstanceEnabler`、`Destroyable`。
 * 5. 单独的传输类型可以隔离协议细节，使平台内部继续使用统一消息模型。
 * 6. 阅读时重点关注入站消息入口、会话状态和消息提交位置。
 */
@Slf4j
public class SimpleLwM2MDevice extends BaseInstanceEnabler implements Destroyable {


    private static final Random RANDOM = new Random();
    /**
     * `min`常量，用于统一引用固定值。
     */
    private static final int min = 5;
    private static final int max = 50;
    private static final  PrimitiveIterator.OfInt randomIterator = new Random().ints(min,max + 1).iterator();
    private static final List<Integer> supportedResources = Arrays.asList(0, 1, 2, 3, 9, 10, 11, 13, 14, 15, 16, 17, 18, 19, 20, 21);


    /**
     * 功能：创建 `SimpleLwM2MDevice` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public SimpleLwM2MDevice() {
    }

    /**
     * 功能：创建 `SimpleLwM2MDevice` 实例，并初始化必要字段。
     * 参数：
     * - `executorService`：服务对象。
     * 返回：新创建的对象实例。
     */
    public SimpleLwM2MDevice(ScheduledExecutorService executorService) {
        try {
            executorService.scheduleWithFixedDelay(() -> {
                        fireResourceChange(9);
                    }
                    , 1800000, 1800000, TimeUnit.MILLISECONDS); // 30 MIN
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
    public ReadResponse read(ServerIdentity identity, int resourceId) {
        if (!identity.isSystem())
            log.info("Read on Device resource /{}/{}/{}", getModel().id, getId(), resourceId);
        switch (resourceId) {
            case 0:
                return ReadResponse.success(resourceId, getManufacturer());
            case 1:
                return ReadResponse.success(resourceId, getModelNumber());
            case 2:
                return ReadResponse.success(resourceId, getSerialNumber());
            case 3:
                return ReadResponse.success(resourceId, getFirmwareVersion());
            case 9:
                return ReadResponse.success(resourceId, getBatteryLevel());
            case 10:
                return ReadResponse.success(resourceId, getMemoryFree());
            case 11:
                Map<Integer, Long> errorCodes = new HashMap<>();
                errorCodes.put(0, getErrorCode());
                return ReadResponse.success(resourceId, errorCodes, ResourceModel.Type.INTEGER);
            case 14:
                return ReadResponse.success(resourceId, getUtcOffset());
            case 15:
                return ReadResponse.success(resourceId, getTimezone());
            case 16:
                return ReadResponse.success(resourceId, getSupportedBinding());
            case 17:
                return ReadResponse.success(resourceId, getDeviceType());
            case 18:
                return ReadResponse.success(resourceId, getHardwareVersion());
            case 19:
                return ReadResponse.success(resourceId, getSoftwareVersion());
            case 20:
                return ReadResponse.success(resourceId, getBatteryStatus());
            case 21:
                return ReadResponse.success(resourceId, getMemoryTotal());
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
    public ExecuteResponse execute(ServerIdentity identity, int resourceId, String params) {
        String withParams = null;
        if (params != null && params.length() != 0) {
            withParams = " with params " + params;
        }
        log.info("Execute on Device resource /{}/{}/{} {}", getModel().id, getId(), resourceId, withParams != null ? withParams : "");
        return ExecuteResponse.success();
    }

    /**
     * 功能：执行 `write` 对应的处理。
     * 参数：
     * - `identity`：实体对象。
     * - `replace`：`replace` 参数。
     * - `resourceId`：`resourceId`ID。
     * - `value`：值。
     * 返回：处理结果。
     */
    @Override
    public WriteResponse write(ServerIdentity identity, boolean replace, int resourceId, LwM2mResource value) {
        log.info("Write on Device resource /{}/{}/{}", getModel().id, getId(), resourceId);

        switch (resourceId) {
            case 13:
                return WriteResponse.notFound();
            case 14:
                setUtcOffset((String) value.getValue());
                fireResourceChange(resourceId);
                return WriteResponse.success();
            case 15:
                setTimezone((String) value.getValue());
                fireResourceChange(resourceId);
                return WriteResponse.success();
            default:
                return super.write(identity, replace, resourceId, value);
        }
    }

    /**
     * 功能：获取`Manufacturer`。
     * 参数：无。
     * 返回：文本结果。
     */
    private String getManufacturer() {
        return "Thingsboard Demo Lwm2mDevice";
    }

    /**
     * 功能：获取`Model Number`。
     * 参数：无。
     * 返回：文本结果。
     */
    private String getModelNumber() {
        return "Model 500";
    }

    /**
     * 功能：获取`Serial Number`。
     * 参数：无。
     * 返回：文本结果。
     */
    private String getSerialNumber() {
        return "Thingsboard-500-000-0001";
    }

    /**
     * 功能：获取版本号。
     * 参数：无。
     * 返回：文本结果。
     */
    private String getFirmwareVersion() {
        return "1.0.2";
    }

    /**
     * 功能：获取错误码。
     * 参数：无。
     * 返回：数值结果。
     */
    private long getErrorCode() {
        return 0;
    }

    /**
     * 功能：获取`Battery Level`。
     * 参数：无。
     * 返回：数值结果。
     */
    private int getBatteryLevel() {
        return randomIterator.nextInt();
//        return 42;
    }

    /**
     * 功能：获取`Memory Free`。
     * 参数：无。
     * 返回：数值结果。
     */
    private long getMemoryFree() {
        return Runtime.getRuntime().freeMemory() / 1024;
    }

    private String utcOffset = new SimpleDateFormat("X").format(Calendar.getInstance().getTime());

    /**
     * 功能：获取偏移量。
     * 参数：无。
     * 返回：文本结果。
     */
    private String getUtcOffset() {
        return utcOffset;
    }

    /**
     * 功能：更新偏移量。
     * 参数：
     * - `t`：`t` 参数。
     * 返回：无。
     */
    private void setUtcOffset(String t) {
        utcOffset = t;
    }

    private String timeZone = TimeZone.getDefault().getID();

    /**
     * 功能：获取`Timezone`。
     * 参数：无。
     * 返回：文本结果。
     */
    private String getTimezone() {
        return timeZone;
    }

    /**
     * 功能：更新`Timezone`。
     * 参数：
     * - `t`：`t` 参数。
     * 返回：无。
     */
    private void setTimezone(String t) {
        timeZone = t;
    }

    /**
     * 功能：获取绑定模式。
     * 参数：无。
     * 返回：文本结果。
     */
    private String getSupportedBinding() {
        return "U";
    }

    /**
     * 功能：获取设备。
     * 参数：无。
     * 返回：文本结果。
     */
    private String getDeviceType() {
        return "Demo";
    }

    /**
     * 功能：获取版本号。
     * 参数：无。
     * 返回：文本结果。
     */
    private String getHardwareVersion() {
        return "1.0.1";
    }

    /**
     * 功能：获取版本号。
     * 参数：无。
     * 返回：文本结果。
     */
    private String getSoftwareVersion() {
        return "1.0.2";
    }

    /**
     * 功能：获取状态。
     * 参数：无。
     * 返回：数值结果。
     */
    private int getBatteryStatus() {
        return RANDOM.nextInt(7);
    }

    /**
     * 功能：获取`Memory Total`。
     * 参数：无。
     * 返回：数值结果。
     */
    private long getMemoryTotal() {
        return Runtime.getRuntime().totalMemory() / 1024;
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
