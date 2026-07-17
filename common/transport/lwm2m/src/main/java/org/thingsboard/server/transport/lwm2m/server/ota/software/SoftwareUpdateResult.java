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
package org.thingsboard.server.transport.lwm2m.server.ota.software;

import lombok.Getter;

/**
 * SW Update Result
 * Contains the result of downloading or installing/uninstalling the software
 * 0: Initial value.
 * - Prior to download any new package in the Device, Update Result MUST be reset to this initial value.
 * - One side effect of executing the Uninstall resource is to reset Update Result to this initial value "0".
 * 1: Downloading.
 * - The package downloading process is on-going.
 * 2: Software successfully installed.
 * 3: Successfully Downloaded and package integrity verified
 * (( 4-49, for expansion, of other scenarios))
 * ** Failed
 * 50: Not enough storage for the new software package.
 * 51: Out of memory during downloading process.
 * 52: Connection lost during downloading process.
 * 53: Package integrity check failure.
 * 54: Unsupported package type.
 * 56: Invalid URI
 * 57: Device defined update error
 * 58: Software installation failure
 * 59: Uninstallation Failure during forUpdate(arg=0)
 * 60-200 : (for expansion, selection to be in blocks depending on new introduction of features)
 * This Resource MAY be reported by sending Observe operation.
 */
/**
 * 中文说明：
 * 1. `SoftwareUpdateResult` 是 ThingsBoard Common Transport 中定义 `Software Update Result` 固定取值的枚举类型。
 * 2. 它列出当前流程允许使用的有限状态、模式或类别。
 * 3. 枚举值可携带与该选项关联的标识、名称或处理参数。
 * 4. 它直接协作于使用该枚举进行分支判断或序列化的类型。
 * 5. 使用枚举可以限制非法取值，并让分支语义在源码中保持明确。
 * 6. 阅读时重点关注各枚举值含义、附加字段和反向查找方法。
 */
public enum SoftwareUpdateResult {
    INITIAL(0, "Initial value", false),
    DOWNLOADING(1, "Downloading", false),
    SUCCESSFULLY_INSTALLED(2, "Software successfully installed", false),
    SUCCESSFULLY_DOWNLOADED_VERIFIED(3, "Successfully Downloaded and package integrity verified", false),
    NOT_ENOUGH_STORAGE(50, "Not enough storage for the new software package", true),
    OUT_OFF_MEMORY(51, "Out of memory during downloading process", true),
    CONNECTION_LOST(52, "Connection lost during downloading process", false),
    PACKAGE_CHECK_FAILURE(53, "Package integrity check failure.", false),
    UNSUPPORTED_PACKAGE_TYPE(54, "Unsupported package type", false),
    INVALID_URI(56, "Invalid URI", true),
    UPDATE_ERROR(57, "Device defined update error", true),
    INSTALL_FAILURE(58, "Software installation failure", true),
    UN_INSTALL_FAILURE(59, "Uninstallation Failure during forUpdate(arg=0)", true);

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
     * 是否为`again`。
     */
    @Getter
    private boolean isAgain;

    SoftwareUpdateResult(int code, String type, boolean isAgain) {
        this.code = code;
        this.type = type;
        this.isAgain = isAgain;
    }

    /**
     * 功能：执行 `fromUpdateResultSwByType` 对应的处理。
     * 参数：
     * - `type`：类型。
     * 返回：处理结果。
     */
    public static SoftwareUpdateResult fromUpdateResultSwByType(String type) {
        for (SoftwareUpdateResult to : SoftwareUpdateResult.values()) {
            if (to.type.equals(type)) {
                return to;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported SW Update Result type  : %s", type));
    }

    /**
     * 功能：执行 `fromUpdateResultSwByCode` 对应的处理。
     * 参数：
     * - `code`：`code` 参数。
     * 返回：处理结果。
     */
    public static SoftwareUpdateResult fromUpdateResultSwByCode(int code) {
        for (SoftwareUpdateResult to : SoftwareUpdateResult.values()) {
            if (to.code == code) {
                return to;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported SW Update Result code  : %s", code));
    }
}
