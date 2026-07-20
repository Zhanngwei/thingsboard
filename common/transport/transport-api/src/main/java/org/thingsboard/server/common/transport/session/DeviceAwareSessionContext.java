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
package org.thingsboard.server.common.transport.session;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.device.profile.DeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.MqttDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.transport.auth.TransportDeviceInfo;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.Optional;
import java.util.UUID;

/**
 * @author Andrew Shvayka
 */
/**
 * 中文说明：
 * 1. `DeviceAwareSessionContext` 是 ThingsBoard Common Transport 中承载设备信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `SessionContext`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Data
public abstract class DeviceAwareSessionContext implements SessionContext {

    /**
     * 会话ID，用于定位对应业务对象。
     */
    @Getter
    protected final UUID sessionId;
    /**
     * 设备ID，用于定位对应业务对象。
     */
    @Getter
    private volatile DeviceId deviceId;
    /**
     * 设备对象，用于描述当前业务场景。
     */
    @Getter
    protected volatile TransportDeviceInfo deviceInfo;
    /**
     * 设备配置，保存当前对象的配置选项。
     */
    @Getter
    @Setter
    protected volatile DeviceProfile deviceProfile;
    /**
     * 会话，保存当前连接或交互过程的会话信息。
     */
    @Getter
    @Setter
    protected volatile TransportProtos.SessionInfoProto sessionInfo;

    /**
     * 当前连接是否已经建立。
     */
    @Setter
    private volatile boolean connected;

    /**
     * 功能：获取设备ID。
     * 参数：无。
     * 返回：处理结果。
     */
    public DeviceId getDeviceId() {
        return deviceId;
    }

    /**
     * 功能：更新设备。
     * 参数：
     * - `deviceInfo`：设备信息或设备标识。
     * 返回：无。
     */
    public void setDeviceInfo(TransportDeviceInfo deviceInfo) {
        this.deviceInfo = deviceInfo;
        this.deviceId = deviceInfo.getDeviceId();
    }

    /**
     * 功能：处理设备配置。
     * 参数：
     * - `sessionInfo`：会话对象。
     * - `deviceProfile`：设备信息或设备标识。
     * 返回：无。
     */
    @Override
    public void onDeviceProfileUpdate(TransportProtos.SessionInfoProto sessionInfo, DeviceProfile deviceProfile) {
        this.sessionInfo = sessionInfo;
        this.deviceProfile = deviceProfile;
        this.deviceInfo.setDeviceType(deviceProfile.getName());

    }

    /**
     * 功能：处理设备。
     * 参数：
     * - `sessionInfo`：会话对象。
     * - `device`：设备信息或设备标识。
     * - `deviceProfileOpt`：设备信息或设备标识。
     * 返回：无。
     */
    @Override
    public void onDeviceUpdate(TransportProtos.SessionInfoProto sessionInfo, Device device, Optional<DeviceProfile> deviceProfileOpt) {
        this.sessionInfo = sessionInfo;
        this.deviceInfo.setDeviceProfileId(device.getDeviceProfileId());
        this.deviceInfo.setDeviceType(device.getType());
        deviceProfileOpt.ifPresent(profile -> this.deviceProfile = profile);
    }

    /**
     * 功能：判断`Connected`。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean isConnected() {
        return connected;
    }

    /**
     * 功能：更新`Disconnected`。
     * 参数：无。
     * 返回：无。
     */
    public void setDisconnected() {
        this.connected = false;
    }

    /**
     * 功能：判断Sparkplug。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean isSparkplug() {
        DeviceProfileTransportConfiguration transportConfiguration = this.deviceProfile.getProfileData().getTransportConfiguration();
        if (transportConfiguration instanceof MqttDeviceProfileTransportConfiguration) {
            return ((MqttDeviceProfileTransportConfiguration) transportConfiguration).isSparkplug();
        } else {
            return false;
        }
    }

}
