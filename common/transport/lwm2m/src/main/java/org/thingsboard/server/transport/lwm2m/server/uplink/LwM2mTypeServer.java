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
package org.thingsboard.server.transport.lwm2m.server.uplink;

/**
 * 中文说明：
 * 1. `LwM2mTypeServer` 是 ThingsBoard Common Transport 中定义 LwM2M 固定取值的枚举类型。
 * 2. 它列出当前流程允许使用的有限状态、模式或类别。
 * 3. 枚举值可携带与该选项关联的标识、名称或处理参数。
 * 4. 它直接协作于使用该枚举进行分支判断或序列化的类型。
 * 5. 使用枚举可以限制非法取值，并让分支语义在源码中保持明确。
 * 6. 阅读时重点关注各枚举值含义、附加字段和反向查找方法。
 */
public enum LwM2mTypeServer {
    BOOTSTRAP(0, "bootstrap"),
    CLIENT(1, "client");

    /**
     * 编码，表示当前对象的对应属性。
     */
    public int code;
    public String type;

    LwM2mTypeServer(int code, String type) {
        this.code = code;
        this.type = type;
    }

    /**
     * 功能：执行 `fromLwM2mTypeServer` 对应的处理。
     * 参数：
     * - `type`：类型。
     * 返回：处理结果。
     */
    public static LwM2mTypeServer fromLwM2mTypeServer(String type) {
        for (LwM2mTypeServer sm : LwM2mTypeServer.values()) {
            if (sm.type.equals(type)) {
                return sm;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported typeServer type : %d", type));
    }
}
