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
package org.thingsboard.server.common.transport.auth;

import org.thingsboard.server.common.data.id.DeviceId;

/**
 * 中文说明：
 * 1. `DeviceAuthResult` 是 ThingsBoard Common Transport 中承载设备信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
public class DeviceAuthResult {

    /**
     * 当前操作是否成功。
     */
    private final boolean success;
    private final DeviceId deviceId;
    /**
     * 消息，承载当前步骤需要处理的内容。
     */
    private final String errorMsg;

    /**
     * 功能：执行 `of` 对应的处理。
     * 参数：
     * - `deviceId`：设备IDID。
     * 返回：处理结果。
     */
    public static DeviceAuthResult of(DeviceId deviceId) {
        return new DeviceAuthResult(true, deviceId, null);
    }

    /**
     * 功能：执行 `of` 对应的处理。
     * 参数：
     * - `errorMsg`：待处理消息。
     * 返回：处理结果。
     */
    public static DeviceAuthResult of(String errorMsg) {
        return new DeviceAuthResult(false, null, errorMsg);
    }

    /**
     * 功能：创建 `DeviceAuthResult` 实例，并初始化必要字段。
     * 参数：
     * - `success`：`success` 参数。
     * - `deviceId`：设备IDID。
     * - `errorMsg`：待处理消息。
     * 返回：新创建的对象实例。
     */
    private DeviceAuthResult(boolean success, DeviceId deviceId, String errorMsg) {
        super();
        this.success = success;
        this.deviceId = deviceId;
        this.errorMsg = errorMsg;
    }

    /**
     * 功能：判断`Success`。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * 功能：获取设备ID。
     * 参数：无。
     * 返回：处理结果。
     */
    public DeviceId getDeviceId() {
        return deviceId;
    }

    /**
     * 功能：获取消息。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getErrorMsg() {
        return errorMsg;
    }

    /**
     * 功能：生成当前对象的文本表示。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    public String toString() {
        return "DeviceAuthResult [success=" + success + ", deviceId=" + deviceId + ", errorMsg=" + errorMsg + "]";
    }

}
