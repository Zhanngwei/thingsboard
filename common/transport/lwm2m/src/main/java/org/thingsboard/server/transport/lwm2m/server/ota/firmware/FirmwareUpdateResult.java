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
package org.thingsboard.server.transport.lwm2m.server.ota.firmware;

import lombok.Getter;

/**
 * FW Update Result
 * 0: Initial value. Once the updating process is initiated (Download /Update), this Resource MUST be reset to Initial value.
 * 1: Firmware updated successfully.
 * 2: Not enough flash memory for the new firmware package.
 * 3: Out of RAM during downloading process.
 * 4: Connection lost during downloading process.
 * 5: Integrity check failure for new downloaded package.
 * 6: Unsupported package type.
 * 7: Invalid URI.
 * 8: Firmware update failed.
 * 9: Unsupported protocol.
 */
/**
 * 中文说明：
 * 1. `FirmwareUpdateResult` 是 ThingsBoard Common Transport 中定义 `Firmware Update Result` 固定取值的枚举类型。
 * 2. 它列出当前流程允许使用的有限状态、模式或类别。
 * 3. 枚举值可携带与该选项关联的标识、名称或处理参数。
 * 4. 它直接协作于使用该枚举进行分支判断或序列化的类型。
 * 5. 使用枚举可以限制非法取值，并让分支语义在源码中保持明确。
 * 6. 阅读时重点关注各枚举值含义、附加字段和反向查找方法。
 */
public enum FirmwareUpdateResult {
    INITIAL(0, "Initial value", false),
    UPDATE_SUCCESSFULLY(1, "Firmware updated successfully", false),
    NOT_ENOUGH(2, "Not enough flash memory for the new firmware package", false),
    OUT_OFF_MEMORY(3, "Out of RAM during downloading process", false),
    CONNECTION_LOST(4, "Connection lost during downloading process", true),
    INTEGRITY_CHECK_FAILURE(5, "Integrity check failure for new downloaded package", true),
    UNSUPPORTED_TYPE(6, "Unsupported package type", false),
    INVALID_URI(7, "Invalid URI", false),
    UPDATE_FAILED(8, "Firmware update failed", false),
    UNSUPPORTED_PROTOCOL(9, "Unsupported protocol", false);

    /**
     * 编码，表示当前对象的对应属性。
     */
    @Getter
    private int code;
    /**
     * 类型，用于区分不同处理分支。
     */
    @Getter
    private String type;
    /**
     * 是否满足`again`条件。
     */
    @Getter
    private boolean again;

    FirmwareUpdateResult(int code, String type, boolean isAgain) {
        this.code = code;
        this.type = type;
        this.again = isAgain;
    }

    /**
     * 功能：执行 `fromUpdateResultFwByType` 对应的处理。
     * 参数：
     * - `type`：类型。
     * 返回：处理结果。
     */
    public static FirmwareUpdateResult fromUpdateResultFwByType(String type) {
        for (FirmwareUpdateResult to : FirmwareUpdateResult.values()) {
            if (to.type.equals(type)) {
                return to;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported FW Update Result type  : %s", type));
    }

    /**
     * 功能：执行 `fromUpdateResultFwByCode` 对应的处理。
     * 参数：
     * - `code`：`code` 参数。
     * 返回：处理结果。
     */
    public static FirmwareUpdateResult fromUpdateResultFwByCode(int code) {
        for (FirmwareUpdateResult to : FirmwareUpdateResult.values()) {
            if (to.code == code) {
                return to;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported FW Update Result code  : %s", code));
    }
}
