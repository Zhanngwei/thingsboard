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
package org.thingsboard.server.transport.lwm2m.server;

import lombok.Getter;

/**
 * Define the behavior of a write request.
 */
/**
 * 中文说明：
 * 1. `LwM2MOperationType` 是 ThingsBoard Common Transport 中定义 LwM2M 固定取值的枚举类型。
 * 2. 它列出当前流程允许使用的有限状态、模式或类别。
 * 3. 枚举值可携带与该选项关联的标识、名称或处理参数。
 * 4. 它直接协作于使用该枚举进行分支判断或序列化的类型。
 * 5. 使用枚举可以限制非法取值，并让分支语义在源码中保持明确。
 * 6. 阅读时重点关注各枚举值含义、附加字段和反向查找方法。
 */
public enum LwM2MOperationType {

    READ(0, "Read", true),
    READ_COMPOSITE(1, "ReadComposite", false, true),
    DISCOVER(2, "Discover", true),
    DISCOVER_ALL(3, "DiscoverAll", false),
    OBSERVE_READ_ALL(4, "ObserveReadAll", false),

    OBSERVE(5, "Observe", true),
    OBSERVE_COMPOSITE(6, "ObserveComposite", false, true),
    OBSERVE_CANCEL(7, "ObserveCancel", true),
    OBSERVE_COMPOSITE_CANCEL(8, "ObserveCompositeCancel", false, true),
    OBSERVE_CANCEL_ALL(9, "ObserveCancelAll", false),
    EXECUTE(10, "Execute", true),
    /**
     * Replaces the Object Instance or the Resource(s) with the new value provided in the “Write” operation. (see
     * section 5.3.3 of the LW M2M spec).
     * if all resources are to be replaced
     */
    WRITE_REPLACE(11, "WriteReplace", true),

    /**
     * Adds or updates Resources provided in the new value and leaves other existing Resources unchanged. (see section
     * 5.3.3 of the LW M2M spec).
     * if this is a partial update request
     */
    WRITE_UPDATE(12, "WriteUpdate", true),
    WRITE_COMPOSITE(14, "WriteComposite", false, true),
    WRITE_ATTRIBUTES(15, "WriteAttributes", true),
    DELETE(16, "Delete", true),
    CREATE(17, "Create", true);
    // only for RPC - future
//    FW_UPDATE(18, "FirmwareUpdate", false),
//    FW_READ_INFO(19, "FirmwareReadInfo", false),
//    SW_UPDATE(20, "SoftwareUpdate", false),
//    SW_READ_INFO(21, "SoftwareReadInfo", false),
//    SW_UNINSTALL(22, "SoftwareUninstall", false);


    /**
     * 编码，表示当前对象的对应属性。
     */
    @Getter
    private final int code;
    /**
     * 类型，用于区分不同处理分支。
     */
    @Getter
    private final String type;
    /**
     * `hasObjectId`ID，用于定位对应业务对象。
     */
    @Getter
    private final boolean hasObjectId;

    /**
     * 是否满足`composite`条件。
     */
    @Getter
    private final boolean composite;

    LwM2MOperationType(int code, String type, boolean hasObjectId) {
        this(code, type, hasObjectId, false);
    }

    LwM2MOperationType(int code, String type, boolean hasObjectId, boolean composite) {
        this.code = code;
        this.type = type;
        this.hasObjectId = hasObjectId;
        this.composite = composite;
        if (hasObjectId && composite) {
            throw new IllegalArgumentException("Can't set both Composite and hasObjectId for the same operation!");
        }
    }

    /**
     * 功能：执行 `fromType` 对应的处理。
     * 参数：
     * - `type`：类型。
     * 返回：处理结果。
     */
    public static LwM2MOperationType fromType(String type) {
        for (LwM2MOperationType to : LwM2MOperationType.values()) {
            if (to.type.equals(type)) {
                return to;
            }
        }
        return null;
    }
}
