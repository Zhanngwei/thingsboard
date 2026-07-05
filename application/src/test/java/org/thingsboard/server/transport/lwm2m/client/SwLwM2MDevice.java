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
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.thingsboard.common.util.ThingsBoardThreadFactory;

import javax.security.auth.Destroyable;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 中文说明：
 * 1. 类目的：`SwLwM2MDevice` 是ThingsBoard Application 测试模块中的传输层测试或适配类型，用于验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 生命周期：由 JUnit 测试生命周期创建，随单个测试方法准备和清理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Integration Test / Fixture。
 */
@Slf4j
public class SwLwM2MDevice extends BaseInstanceEnabler implements Destroyable {

    private static final List<Integer> supportedResources = Arrays.asList(0, 1, 2, 3, 4, 6, 7, 9);

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName(getClass().getSimpleName() + "-test-scope"));

    private final AtomicInteger state = new AtomicInteger(0);

    private final AtomicInteger updateResult = new AtomicInteger(0);

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
                return ReadResponse.success(resourceId, getPkgName());
            case 1:
                return ReadResponse.success(resourceId, getPkgVersion());
            case 7:
                return ReadResponse.success(resourceId, getUpdateState());
            case 9:
                return ReadResponse.success(resourceId, getUpdateResult());
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

        switch (resourceId) {
            case 4:
                startUpdating();
                return ExecuteResponse.success();
            case 6:
                return ExecuteResponse.success();
            default:
                return super.execute(identity, resourceId, params);
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
        log.info("Write on Device resource /{}/{}/{}", getModel().id, getId(), resourceId);

        switch (resourceId) {
            case 2:
                startDownloading();
                return WriteResponse.success();
            case 3:
                startDownloading();
                return WriteResponse.success();
            default:
                return super.write(identity, replace, resourceId, value);
        }
    }

    /**
     * 功能：获取状态。
     * 参数：无。
     * 返回：数值结果。
     */
    private int getUpdateState() {
        return state.get();
    }

    /**
     * 功能：获取`Update Result`。
     * 参数：无。
     * 返回：数值结果。
     */
    private int getUpdateResult() {
        return updateResult.get();
    }

    /**
     * 功能：获取名称。
     * 参数：无。
     * 返回：文本结果。
     */
    private String getPkgName() {
        return "software";
    }

    /**
     * 功能：获取版本号。
     * 参数：无。
     * 返回：文本结果。
     */
    private String getPkgVersion() {
        return "1.0.0";
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
        scheduler.shutdownNow();
    }

    /**
     * 功能：初始化或启动`Downloading`。
     * 参数：无。
     * 返回：无。
     */
    private void startDownloading() {
        scheduler.schedule(() -> {
            try {
                state.set(1);
                updateResult.set(1);
                fireResourceChange(7);
                fireResourceChange(9);
                Thread.sleep(100);
                state.set(2);
                fireResourceChange(7);
                Thread.sleep(100);
                state.set(3);
                fireResourceChange(7);
                Thread.sleep(100);
                updateResult.set(3);
                fireResourceChange(9);
            } catch (Exception e) {

            }
        }, 100, TimeUnit.MILLISECONDS);
    }

    /**
     * 功能：初始化或启动`Updating`。
     * 参数：无。
     * 返回：无。
     */
    private void startUpdating() {
        scheduler.schedule(() -> {
            state.set(4);
            updateResult.set(2);
            fireResourceChange(7);
            fireResourceChange(9);
        }, 100, TimeUnit.MILLISECONDS);
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`SwLwM2MDevice` 在 ThingsBoard Application 测试模块 中承担传输层测试或适配类型职责，核心目的是验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 核心流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
 * 3. 关键依赖：主要依赖或协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
