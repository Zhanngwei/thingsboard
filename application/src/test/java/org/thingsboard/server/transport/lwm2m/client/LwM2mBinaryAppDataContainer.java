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
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mMultipleResource;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;

import javax.security.auth.Destroyable;
import java.sql.Time;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * 中文说明：
 * 1. 类目的：`LwM2mBinaryAppDataContainer` 是ThingsBoard Application 测试模块中的传输层测试或适配类型，用于验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 生命周期：由 JUnit 测试生命周期创建，随单个测试方法准备和清理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Integration Test / Fixture。
 */
@Slf4j
public class LwM2mBinaryAppDataContainer extends BaseInstanceEnabler implements Destroyable {

    /**
     * id = 0
     * Multiple
     * base64
     */

    /**
     * Example1:
     * InNlcnZpY2VJZCI6Ik1ldGVyIiwNCiJzZXJ2aWNlRGF0YSI6ew0KImN1cnJlbnRSZWFka
     * W5nIjoiNDYuMyIsDQoic2lnbmFsU3RyZW5ndGgiOjE2LA0KImRhaWx5QWN0aXZpdHlUaW1lIjo1NzA2DQo=
     * "serviceId":"Meter",
     * "serviceData":{
     * "currentReading":"46.3",
     * "signalStrength":16,
     * "dailyActivityTime":5706
     */

    /**
     * Example2:
     * InNlcnZpY2VJZCI6IldhdGVyTWV0ZXIiLA0KImNtZCI6IlNFVF9URU1QRVJBVFVSRV9SRUFEX
     * 1BFUklPRCIsDQoicGFyYXMiOnsNCiJ2YWx1ZSI6NA0KICAgIH0sDQoNCg0K
     * "serviceId":"WaterMeter",
     * "cmd":"SET_TEMPERATURE_READ_PERIOD",
     * "paras":{
     * "value":4
     * },
     */

    /**
     * 数据列表，用于保存一组待处理对象。
     */
    Map<Integer, byte[]> data;
    private Integer priority = 0;
    /**
     * 时间戳，用于标识当前数据或事件发生的时间。
     */
    private Time timestamp;
    private String description;
    /**
     * 数据，保存当前步骤读取或计算得到的内容。
     */
    private String dataFormat;
    private Integer appID = -1;
    private static final List<Integer> supportedResources = Arrays.asList(0, 1, 2, 3, 4, 5);

    /**
     * 功能：创建 `LwM2mBinaryAppDataContainer` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public LwM2mBinaryAppDataContainer() {
    }

    /**
     * 功能：创建 `LwM2mBinaryAppDataContainer` 实例，并初始化必要字段。
     * 参数：
     * - `executorService`：服务对象。
     * - `id`：`id`ID。
     * 返回：新创建的对象实例。
     */
    public LwM2mBinaryAppDataContainer(ScheduledExecutorService executorService, Integer id) {
        try {
            if (id != null) this.setId(id);
            executorService.scheduleWithFixedDelay(() -> {
                        fireResourceChange(0);
                        fireResourceChange(2);
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
        try {
            switch (resourceId) {
                case 0:
                    ReadResponse response = ReadResponse.success(resourceId, getData(), ResourceModel.Type.OPAQUE);
                    return response;
                case 1:
                    return ReadResponse.success(resourceId, getPriority());
                case 2:
                    return ReadResponse.success(resourceId, getTimestamp());
                case 3:
                    return ReadResponse.success(resourceId, getDescription());
                case 4:
                    return ReadResponse.success(resourceId, getDataFormat());
                case 5:
                    return ReadResponse.success(resourceId, getAppID());
                default:
                    return super.read(identity, resourceId);
            }
        } catch (Exception e) {
            return ReadResponse.badRequest(e.getMessage());
        }
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
        log.info("Write on Device resource /[{}]/[{}]/[{}]", getModel().id, getId(), resourceId);
        switch (resourceId) {
            case 0:
                if (setData(value, replace)) {
                    return WriteResponse.success();
                } else {
                    WriteResponse.badRequest("Invalidate value ...");
                }
            case 1:
                setPriority((Integer) (value.getValue() instanceof Long ? ((Long) value.getValue()).intValue() : value.getValue()));
                fireResourceChange(resourceId);
                return WriteResponse.success();
            case 2:
                setTimestamp(((Date) value.getValue()).getTime());
                fireResourceChange(resourceId);
                return WriteResponse.success();
            case 3:
                setDescription((String) value.getValue());
                fireResourceChange(resourceId);
                return WriteResponse.success();
            case 4:
                setDataFormat((String) value.getValue());
                fireResourceChange(resourceId);
                return WriteResponse.success();
            case 5:
                setAppID((Integer) value.getValue());
                fireResourceChange(resourceId);
                return WriteResponse.success();
            default:
                return super.write(identity, replace, resourceId, value);
        }
    }

    /**
     * 功能：获取`App ID`。
     * 参数：无。
     * 返回：数值结果。
     */
    private Integer getAppID() {
        return this.appID;
    }

    /**
     * 功能：更新`App ID`。
     * 参数：
     * - `appId`：`appId`ID。
     * 返回：无。
     */
    private void setAppID(Integer appId) {
        this.appID = appId;
    }

    /**
     * 功能：更新数据。
     * 参数：
     * - `value`：值。
     * 返回：无。
     */
    private void setDataFormat(String value) {
        this.dataFormat = value;
    }

    /**
     * 功能：获取数据。
     * 参数：无。
     * 返回：文本结果。
     */
    private String getDataFormat() {
        return this.dataFormat == null ? "OPAQUE" : this.dataFormat;
    }

    /**
     * 功能：更新描述信息。
     * 参数：
     * - `value`：值。
     * 返回：无。
     */
    private void setDescription(String value) {
        this.description = value;
    }

    /**
     * 功能：获取描述信息。
     * 参数：无。
     * 返回：文本结果。
     */
    private String getDescription() {
//        return this.description == null ? "meter reading" : this.description;
        return this.description;
    }

    /**
     * 功能：设置当前对象记录的时间戳。
     * 参数：
     * - `time`：`time` 参数。
     * 返回：无。
     */
    private void setTimestamp(long time) {
        this.timestamp = new Time(time);
    }

    /**
     * 功能：获取当前对象记录的时间戳。
     * 参数：无。
     * 返回：处理结果。
     */
    private Time getTimestamp() {
        return this.timestamp != null ? this.timestamp : new Time(new Date().getTime());
    }

    /**
     * 功能：更新数据。
     * 参数：
     * - `value`：值。
     * - `replace`：`replace` 参数。
     * 返回：判断结果。
     */
    private boolean setData(LwM2mResource value, boolean replace) {
        try {
            if (value instanceof LwM2mMultipleResource) {
                if (replace || this.data == null) {
                    this.data = new HashMap<>();
                }
                value.getInstances().values().forEach(v -> {
                    this.data.put(v.getId(), (byte[]) v.getValue());
                });
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 功能：获取数据。
     * 参数：无。
     * 返回：处理结果。
     */
    private Map<Integer, byte[]> getData() {
        if (data == null) {
            this.data = new HashMap<>();
            this.data.put(0, new byte[]{(byte) 0xAC});
        }
        return data;
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

    /**
     * 功能：获取`Priority`。
     * 参数：无。
     * 返回：数值结果。
     */
    private int getPriority() {
        return this.priority;
    }

    /**
     * 功能：更新`Priority`。
     * 参数：
     * - `value`：值。
     * 返回：无。
     */
    private void setPriority(int value) {
        this.priority = value;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`LwM2mBinaryAppDataContainer` 在 ThingsBoard Application 测试模块 中承担传输层测试或适配类型职责，核心目的是验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 核心流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
 * 3. 关键依赖：主要依赖或协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
