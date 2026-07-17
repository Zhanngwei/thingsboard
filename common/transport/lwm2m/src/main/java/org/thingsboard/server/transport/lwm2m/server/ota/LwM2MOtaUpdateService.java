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
package org.thingsboard.server.transport.lwm2m.server.ota;

import org.thingsboard.server.common.data.device.profile.lwm2m.OtherConfiguration;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;

import java.util.Optional;

/**
 * 中文说明：
 * 1. `LwM2MOtaUpdateService` 是 ThingsBoard Common Transport 中定义 LwM2M 能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 它直接协作于实现类以及使用该接口的调用组件。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface LwM2MOtaUpdateService {

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：
     * - `client`：客户端对象。
     * 返回：无。
     */
    void init(LwM2mClient client);

    /**
     * 功能：执行 `forceFirmwareUpdate` 对应的处理。
     * 参数：
     * - `client`：客户端对象。
     * 返回：无。
     */
    void forceFirmwareUpdate(LwM2mClient client);

    /**
     * 功能：处理目标对象。
     * 参数：
     * - `client`：客户端对象。
     * - `newFwTitle`：`newFwTitle` 参数。
     * - `newFwVersion`：`newFwVersion` 参数。
     * - `newFwUrl`：`newFwUrl` 参数。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    void onTargetFirmwareUpdate(LwM2mClient client, String newFwTitle, String newFwVersion, Optional<String> newFwUrl, Optional<String> newFwTag);

    /**
     * 功能：处理目标对象。
     * 参数：
     * - `client`：客户端对象。
     * - `newSwTitle`：`newSwTitle` 参数。
     * - `newSwVersion`：`newSwVersion` 参数。
     * - `newSwUrl`：`newSwUrl` 参数。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    void onTargetSoftwareUpdate(LwM2mClient client, String newSwTitle, String newSwVersion, Optional<String> newSwUrl, Optional<String> newSwTag);

    /**
     * 功能：处理名称。
     * 参数：
     * - `client`：客户端对象。
     * - `name`：名称。
     * 返回：无。
     */
    void onCurrentFirmwareNameUpdate(LwM2mClient client, String name);

    /**
     * 功能：处理策略对象。
     * 参数：
     * - `client`：客户端对象。
     * - `configuration`：配置对象。
     * 返回：无。
     */
    void onFirmwareStrategyUpdate(LwM2mClient client, OtherConfiguration configuration);

    /**
     * 功能：处理策略对象。
     * 参数：
     * - `client`：客户端对象。
     * - `configuration`：配置对象。
     * 返回：无。
     */
    void onCurrentSoftwareStrategyUpdate(LwM2mClient client, OtherConfiguration configuration);

    /**
     * 功能：处理`on Current Firmware Version3 Update`。
     * 参数：
     * - `client`：客户端对象。
     * - `version`：`version` 参数。
     * 返回：无。
     */
    void onCurrentFirmwareVersion3Update(LwM2mClient client, String version);

    /**
     * 功能：处理版本号。
     * 参数：
     * - `client`：客户端对象。
     * - `version`：`version` 参数。
     * 返回：无。
     */
    void onCurrentFirmwareVersionUpdate(LwM2mClient client, String version);

    /**
     * 功能：处理状态。
     * 参数：
     * - `client`：客户端对象。
     * - `state`：`state` 参数。
     * 返回：无。
     */
    void onCurrentFirmwareStateUpdate(LwM2mClient client, Long state);

    /**
     * 功能：处理`on Current Firmware Result Update`。
     * 参数：
     * - `client`：客户端对象。
     * - `result`：`result` 参数。
     * 返回：无。
     */
    void onCurrentFirmwareResultUpdate(LwM2mClient client, Long result);

    /**
     * 功能：处理`on Current Firmware Delivery Method Update`。
     * 参数：
     * - `lwM2MClient`：客户端对象。
     * - `value`：值。
     * 返回：无。
     */
    void onCurrentFirmwareDeliveryMethodUpdate(LwM2mClient lwM2MClient, Long value);

    /**
     * 功能：处理名称。
     * 参数：
     * - `lwM2MClient`：客户端对象。
     * - `name`：名称。
     * 返回：无。
     */
    void onCurrentSoftwareNameUpdate(LwM2mClient lwM2MClient, String name);

    /**
     * 功能：处理`on Current Software Version3 Update`。
     * 参数：
     * - `lwM2MClient`：客户端对象。
     * - `version`：`version` 参数。
     * 返回：无。
     */
    void onCurrentSoftwareVersion3Update(LwM2mClient lwM2MClient, String version);

    /**
     * 功能：处理版本号。
     * 参数：
     * - `client`：客户端对象。
     * - `version`：`version` 参数。
     * 返回：无。
     */
    void onCurrentSoftwareVersionUpdate(LwM2mClient client, String version);

    /**
     * 功能：处理状态。
     * 参数：
     * - `lwM2MClient`：客户端对象。
     * - `value`：值。
     * 返回：无。
     */
    void onCurrentSoftwareStateUpdate(LwM2mClient lwM2MClient, Long value);

    /**
     * 功能：处理`on Current Software Result Update`。
     * 参数：
     * - `client`：客户端对象。
     * - `result`：`result` 参数。
     * 返回：无。
     */
    void onCurrentSoftwareResultUpdate(LwM2mClient client, Long result);

    /**
     * 功能：判断`Ota Downloading`。
     * 参数：
     * - `client`：客户端对象。
     * 返回：判断结果。
     */
    boolean isOtaDownloading(LwM2mClient client);
}
