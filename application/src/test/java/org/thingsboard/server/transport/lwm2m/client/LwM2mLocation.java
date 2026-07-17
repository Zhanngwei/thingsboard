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
 * 1. `LwM2mLocation` 是 ThingsBoard Application 中负责 LwM2M 接入或传输适配的类型。
 * 2. 它处理连接、会话、协议消息或平台传输消息之间的转换。
 * 3. 类中的状态和配置用于控制当前协议交互的具体行为。
 * 4. 直接依赖的类型边界包括 `BaseInstanceEnabler`、`Destroyable`。
 * 5. 单独的传输类型可以隔离协议细节，使平台内部继续使用统一消息模型。
 * 6. 阅读时重点关注入站消息入口、会话状态和消息提交位置。
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
