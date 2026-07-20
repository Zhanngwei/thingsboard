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
 * 1. `SwLwM2MDevice` 是 ThingsBoard Application 中负责设备接入或传输适配的类型。
 * 2. 它处理连接、会话、协议消息或平台传输消息之间的转换。
 * 3. 类中的状态和配置用于控制当前协议交互的具体行为。
 * 4. 直接依赖的类型边界包括 `BaseInstanceEnabler`、`Destroyable`。
 * 5. 单独的传输类型可以隔离协议细节，使平台内部继续使用统一消息模型。
 * 6. 阅读时重点关注入站消息入口、会话状态和消息提交位置。
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
