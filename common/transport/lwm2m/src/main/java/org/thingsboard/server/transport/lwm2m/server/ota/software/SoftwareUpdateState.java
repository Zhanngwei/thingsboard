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
 * SW Update State R
 * 0: INITIAL Before downloading. (see 5.1.2.1)
 * 1: DOWNLOAD STARTED The downloading process has started and is on-going. (see 5.1.2.2)
 * 2: DOWNLOADED The package has been completely downloaded  (see 5.1.2.3)
 * 3: DELIVERED In that state, the package has been correctly downloaded and is ready to be installed.  (see 5.1.2.4)
 * If executing the Install Resource failed, the state remains at DELIVERED.
 * If executing the Install Resource was successful, the state changes from DELIVERED to INSTALLED.
 * After executing the UnInstall Resource, the state changes to INITIAL.
 * 4: INSTALLED
 */
/**
 * 中文说明：
 * 1. `SoftwareUpdateState` 是 ThingsBoard Common Transport 中定义 `Software Update State` 固定取值的枚举类型。
 * 2. 它列出当前流程允许使用的有限状态、模式或类别。
 * 3. 枚举值可携带与该选项关联的标识、名称或处理参数。
 * 4. 它直接协作于使用该枚举进行分支判断或序列化的类型。
 * 5. 使用枚举可以限制非法取值，并让分支语义在源码中保持明确。
 * 6. 阅读时重点关注各枚举值含义、附加字段和反向查找方法。
 */
public enum SoftwareUpdateState {
    INITIAL(0, "Initial"),
    DOWNLOAD_STARTED(1, "DownloadStarted"),
    DOWNLOADED(2, "Downloaded"),
    DELIVERED(3, "Delivered"),
    INSTALLED(4, "Installed");

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

    SoftwareUpdateState(int code, String type) {
        this.code = code;
        this.type = type;
    }

    /**
     * 功能：执行 `fromUpdateStateSwByType` 对应的处理。
     * 参数：
     * - `type`：类型。
     * 返回：处理结果。
     */
    public static SoftwareUpdateState fromUpdateStateSwByType(String type) {
        for (SoftwareUpdateState to : SoftwareUpdateState.values()) {
            if (to.type.equals(type)) {
                return to;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported SW State type  : %s", type));
    }

    /**
     * 功能：执行 `fromUpdateStateSwByCode` 对应的处理。
     * 参数：
     * - `code`：`code` 参数。
     * 返回：处理结果。
     */
    public static SoftwareUpdateState fromUpdateStateSwByCode(int code) {
        for (SoftwareUpdateState to : SoftwareUpdateState.values()) {
            if (to.code == code) {
                return to;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported SW State type  : %s", code));
    }
}
