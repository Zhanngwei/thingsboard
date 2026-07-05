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
import org.eclipse.leshan.core.response.ReadResponse;

import javax.security.auth.Destroyable;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 中文说明：
 * 1. 类目的：`LwM2mLocation` 是ThingsBoard Application 测试模块中的传输层测试或适配类型，用于验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 生命周期：由 JUnit 测试生命周期创建，随单个测试方法准备和清理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Integration Test / Fixture。
 */
@Slf4j
public class LwM2mLocation extends BaseInstanceEnabler implements Destroyable {

    /**
     * `latitude` 字段，保存当前对象的对应属性。
     */
    private float latitude;
    private float longitude;
    /**
     * `scaleFactor` 字段，保存当前对象的对应属性。
     */
    private float scaleFactor;
    private Date timestamp;
    protected static final Random RANDOM = new Random();
    private static final List<Integer> supportedResources = Arrays.asList(0, 1, 5);

    /**
     * 功能：创建 `LwM2mLocation` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public LwM2mLocation() {
        this(null, null, 1.0f);
    }

    /**
     * 功能：创建 `LwM2mLocation` 实例，并初始化必要字段。
     * 参数：
     * - `latitude`：`latitude` 参数。
     * - `longitude`：`longitude` 参数。
     * - `scaleFactor`：`scaleFactor` 参数。
     * 返回：新创建的对象实例。
     */
    public LwM2mLocation(Float latitude, Float longitude, float scaleFactor) {

        if (latitude != null) {
            this.latitude = latitude + 90f;
        } else {
            this.latitude = RANDOM.nextInt(180);
        }
        if (longitude != null) {
            this.longitude = longitude + 180f;
        } else {
            this.longitude = RANDOM.nextInt(360);
        }
        this.scaleFactor = scaleFactor;
        timestamp = new Date();
    }

    /**
     * 功能：创建 `LwM2mLocation` 实例，并初始化必要字段。
     * 参数：
     * - `latitude`：`latitude` 参数。
     * - `longitude`：`longitude` 参数。
     * - `scaleFactor`：`scaleFactor` 参数。
     * - `executorService`：服务对象。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public LwM2mLocation(Float latitude, Float longitude, float scaleFactor, ScheduledExecutorService executorService, Integer id) {
        try {
            if (id != null) this.setId(id);
            if (latitude != null) {
                this.latitude = latitude + 90f;
            } else {
                this.latitude = RANDOM.nextInt(180);
            }
            if (longitude != null) {
                this.longitude = longitude + 180f;
            } else {
                this.longitude = RANDOM.nextInt(360);
            }
            this.scaleFactor = scaleFactor;
            timestamp = new Date();
            executorService.scheduleWithFixedDelay(() -> {
                fireResourceChange(0);
                fireResourceChange(1);
            }, 10000, 10000, TimeUnit.MILLISECONDS);
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
        log.info("Read on Location resource /[{}]/[{}]/[{}]", getModel().id, getId(), resourceId);
        switch (resourceId) {
            case 0:
                return ReadResponse.success(resourceId, getLatitude());
            case 1:
                return ReadResponse.success(resourceId, getLongitude());
            case 5:
                return ReadResponse.success(resourceId, getTimestamp());
            default:
                return super.read(identity, resourceId);
        }
    }

    /**
     * 功能：执行 `moveLocation` 对应的处理。
     * 参数：
     * - `nextMove`：`nextMove` 参数。
     * 返回：无。
     */
    public void moveLocation(String nextMove) {
        switch (nextMove.charAt(0)) {
            case 'w':
                moveLatitude(1.0f);
                break;
            case 'a':
                moveLongitude(-1.0f);
                break;
            case 's':
                moveLatitude(-1.0f);
                break;
            case 'd':
                moveLongitude(1.0f);
                break;
        }
    }

    /**
     * 功能：执行 `moveLatitude` 对应的处理。
     * 参数：
     * - `delta`：`delta` 参数。
     * 返回：无。
     */
    private void moveLatitude(float delta) {
        latitude = latitude + delta * scaleFactor;
        timestamp = new Date();
        fireResourceChange(0);
        fireResourceChange(5);
    }

    /**
     * 功能：执行 `moveLongitude` 对应的处理。
     * 参数：
     * - `delta`：`delta` 参数。
     * 返回：无。
     */
    private void moveLongitude(float delta) {
        longitude = longitude + delta * scaleFactor;
        timestamp = new Date();
        fireResourceChange(1);
        fireResourceChange(5);

    }

    /**
     * 功能：获取`Latitude`。
     * 参数：无。
     * 返回：数值结果。
     */
    public float getLatitude() {
        return latitude - 90.0f;
    }

    /**
     * 功能：获取`Longitude`。
     * 参数：无。
     * 返回：数值结果。
     */
    public float getLongitude() {
        return longitude - 180.f;
    }

    /**
     * 功能：获取当前对象记录的时间戳。
     * 参数：无。
     * 返回：处理结果。
     */
    public Date getTimestamp() {
        return timestamp;
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

}

/*
 * 本类总结：
 * 1. 核心职责：`LwM2mLocation` 在 ThingsBoard Application 测试模块 中承担传输层测试或适配类型职责，核心目的是验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 核心流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
 * 3. 关键依赖：主要依赖或协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
