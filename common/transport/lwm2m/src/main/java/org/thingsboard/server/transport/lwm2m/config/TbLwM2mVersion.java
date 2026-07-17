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
package org.thingsboard.server.transport.lwm2m.config;

import lombok.Getter;
import org.eclipse.leshan.core.LwM2m.LwM2mVersion;
import org.eclipse.leshan.core.request.ContentFormat;

/**
 * 中文说明：
 * 1. `TbLwM2mVersion` 是 ThingsBoard Common Transport 中定义 LwM2M 固定取值的枚举类型。
 * 2. 它列出当前流程允许使用的有限状态、模式或类别。
 * 3. 枚举值可携带与该选项关联的标识、名称或处理参数。
 * 4. 它直接协作于使用该枚举进行分支判断或序列化的类型。
 * 5. 使用枚举可以限制非法取值，并让分支语义在源码中保持明确。
 * 6. 阅读时重点关注各枚举值含义、附加字段和反向查找方法。
 */
public enum TbLwM2mVersion {
    VERSION_1_0(0, LwM2mVersion.V1_0, ContentFormat.TLV, false),
    VERSION_1_1(1, LwM2mVersion.V1_1, ContentFormat.TEXT, true);

    /**
     * 编码，表示当前对象的对应属性。
     */
    @Getter
    private final int code;
    /**
     * 版本号，表示当前对象的对应属性。
     */
    @Getter
    private final LwM2mVersion version;
    /**
     * 内容格式，表示当前对象的对应属性。
     */
    @Getter
    private final ContentFormat contentFormat;
    /**
     * 是否满足`composite`条件。
     */
    @Getter
    private final boolean composite;

    TbLwM2mVersion(int code, LwM2mVersion version, ContentFormat contentFormat, boolean composite) {
        this.code = code;
        this.version = version;
        this.contentFormat = contentFormat;
        this.composite = composite;
    }

    /**
     * 功能：执行 `fromVersion` 对应的处理。
     * 参数：
     * - `version`：`version` 参数。
     * 返回：处理结果。
     */
    public static TbLwM2mVersion fromVersion(LwM2mVersion version) {
        for (TbLwM2mVersion to : TbLwM2mVersion.values()) {
            if (to.version.equals(version)) {
                return to;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported typeLwM2mVersion type : %s", version));
    }

    /**
     * 功能：执行 `fromVersionStr` 对应的处理。
     * 参数：
     * - `versionStr`：`versionStr` 参数。
     * 返回：处理结果。
     */
    public static TbLwM2mVersion fromVersionStr(String versionStr) {
        for (TbLwM2mVersion to : TbLwM2mVersion.values()) {
            if (to.version.toString().equals(versionStr)) {
                return to;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported contentFormatLwM2mVersion version : %s", versionStr));
    }

    /**
     * 功能：执行 `fromCode` 对应的处理。
     * 参数：
     * - `code`：`code` 参数。
     * 返回：处理结果。
     */
    public static TbLwM2mVersion fromCode(int code) {
        for (TbLwM2mVersion to : TbLwM2mVersion.values()) {
            if (to.code == code) {
                return to;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported codeLwM2mVersion code : %d", code));
    }
}
