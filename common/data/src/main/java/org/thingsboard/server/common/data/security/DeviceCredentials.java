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
package org.thingsboard.server.common.data.security;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.id.DeviceCredentialsId;
import org.thingsboard.server.common.data.id.DeviceId;

/**
 * 中文说明：
 * 1. `DeviceCredentials` 是 ThingsBoard Common Data 中承载设备凭据信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `BaseData`、`DeviceCredentialsFilter`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@ApiModel
@EqualsAndHashCode(callSuper = true)
public class DeviceCredentials extends BaseData<DeviceCredentialsId> implements DeviceCredentialsFilter {

    /**
     * 版本号常量，用于统一引用固定值。
     */
    private static final long serialVersionUID = -7869261127032877765L;
    private DeviceId deviceId;
    /**
     * 凭据，用于区分不同处理分支。
     */
    private DeviceCredentialsType credentialsType;
    private String credentialsId;
    /**
     * 凭据，保存当前处理得到的具体内容。
     */
    private String credentialsValue;
    
    /**
     * 功能：创建 `DeviceCredentials` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public DeviceCredentials() {
        super();
    }

    /**
     * 功能：创建 `DeviceCredentials` 实例，并初始化必要字段。
     * 参数：
     * - `id`：`id`ID。
     * 返回：新创建的对象实例。
     */
    public DeviceCredentials(DeviceCredentialsId id) {
        super(id);
    }

    /**
     * 功能：创建 `DeviceCredentials` 实例，并初始化必要字段。
     * 参数：
     * - `deviceCredentials`：设备信息或设备标识。
     * 返回：新创建的对象实例。
     */
    public DeviceCredentials(DeviceCredentials deviceCredentials) {
        super(deviceCredentials);
        this.deviceId = deviceCredentials.getDeviceId();
        this.credentialsType = deviceCredentials.getCredentialsType();
        this.credentialsId = deviceCredentials.getCredentialsId();
        this.credentialsValue = deviceCredentials.getCredentialsValue();
    }

    /**
     * 功能：获取`Id`。
     * 参数：无。
     * 返回：处理结果。
     */
    @ApiModelProperty(position = 1, required = true, accessMode = ApiModelProperty.AccessMode.READ_ONLY, value = "The Id is automatically generated during device creation. " +
            "Use 'getDeviceCredentialsByDeviceId' to obtain the id based on device id. " +
            "Use 'updateDeviceCredentials' to update device credentials. ", example = "784f394c-42b6-435a-983c-b7beff2784f9")
    @Override
    public DeviceCredentialsId getId() {
        return super.getId();
    }

    /**
     * 功能：获取创建时间。
     * 参数：无。
     * 返回：数值结果。
     */
    @ApiModelProperty(position = 2, value = "Timestamp of the device credentials creation, in milliseconds", example = "1609459200000")
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    /**
     * 功能：获取设备ID。
     * 参数：无。
     * 返回：处理结果。
     */
    @ApiModelProperty(position = 3, required = true, value = "JSON object with the device Id.")
    public DeviceId getDeviceId() {
        return deviceId;
    }

    /**
     * 功能：更新设备ID。
     * 参数：
     * - `deviceId`：设备IDID。
     * 返回：无。
     */
    public void setDeviceId(DeviceId deviceId) {
        this.deviceId = deviceId;
    }

    /**
     * 功能：获取凭据。
     * 参数：无。
     * 返回：处理结果。
     */
    @ApiModelProperty(position = 4, value = "Type of the credentials", allowableValues="ACCESS_TOKEN, X509_CERTIFICATE, MQTT_BASIC, LWM2M_CREDENTIALS")
    @Override
    public DeviceCredentialsType getCredentialsType() {
        return credentialsType;
    }

    /**
     * 功能：更新凭据。
     * 参数：
     * - `credentialsType`：类型。
     * 返回：无。
     */
    public void setCredentialsType(DeviceCredentialsType credentialsType) {
        this.credentialsType = credentialsType;
    }

    /**
     * 功能：获取凭据。
     * 参数：无。
     * 返回：文本结果。
     */
    @ApiModelProperty(position = 5, required = true, value = "Unique Credentials Id per platform instance. " +
            "Used to lookup credentials from the database. " +
            "By default, new access token for your device. " +
            "Depends on the type of the credentials."
            , example = "Access token or other value that depends on the credentials type")
    @Override
    public String getCredentialsId() {
        return credentialsId;
    }

    /**
     * 功能：更新凭据。
     * 参数：
     * - `credentialsId`：凭据ID。
     * 返回：无。
     */
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    /**
     * 功能：获取凭据。
     * 参数：无。
     * 返回：文本结果。
     */
    @ApiModelProperty(position = 6, value = "Value of the credentials. " +
            "Null in case of ACCESS_TOKEN credentials type. Base64 value in case of X509_CERTIFICATE. " +
            "Complex object in case of MQTT_BASIC and LWM2M_CREDENTIALS", example = "Null in case of ACCESS_TOKEN. See model definition.")
    public String getCredentialsValue() {
        return credentialsValue;
    }

    /**
     * 功能：更新凭据。
     * 参数：
     * - `credentialsValue`：值。
     * 返回：无。
     */
    public void setCredentialsValue(String credentialsValue) {
        this.credentialsValue = credentialsValue;
    }

    /**
     * 功能：生成当前对象的文本表示。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    public String toString() {
        return "DeviceCredentials [deviceId=" + deviceId + ", credentialsType=" + credentialsType + ", credentialsId="
                + credentialsId + ", credentialsValue=" + credentialsValue + ", createdTime=" + createdTime + ", id="
                + id + "]";
    }
}
