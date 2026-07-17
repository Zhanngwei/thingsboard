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
package org.thingsboard.server.service.ws;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Optional;

/**
 * 中文说明：
 * 1. `WebSocketSessionType` 是 ThingsBoard Application 中定义会话固定取值的枚举类型。
 * 2. 它列出当前流程允许使用的有限状态、模式或类别。
 * 3. 枚举值可携带与该选项关联的标识、名称或处理参数。
 * 4. 它直接协作于使用该枚举进行分支判断或序列化的类型。
 * 5. 使用枚举可以限制非法取值，并让分支语义在源码中保持明确。
 * 6. 阅读时重点关注各枚举值含义、附加字段和反向查找方法。
 */
@NoArgsConstructor
@AllArgsConstructor
public enum WebSocketSessionType {
    GENERAL(),
    TELEMETRY("telemetry"), // deprecated
    NOTIFICATIONS("notifications"); // deprecated

    /**
     * 名称，用于标识或展示当前对象。
     */
    private String name;

    /**
     * 功能：执行 `forName` 对应的处理。
     * 参数：
     * - `name`：名称。
     * 返回：可能存在的结果。
     */
    public static Optional<WebSocketSessionType> forName(String name) {
        return Arrays.stream(values())
                .filter(sessionType -> StringUtils.equals(sessionType.name, name))
                .findFirst();
    }

}
