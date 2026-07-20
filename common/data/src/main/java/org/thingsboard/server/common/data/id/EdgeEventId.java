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
package org.thingsboard.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * 中文说明：
 * 1. `EdgeEventId` 是 ThingsBoard Common Data 中承载事件信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `UUIDBased`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
public class EdgeEventId extends UUIDBased {

    /**
     * 版本号常量，用于统一引用固定值。
     */
    private static final long serialVersionUID = 1L;

    /**
     * 功能：创建 `EdgeEventId` 实例，并初始化必要字段。
     * 参数：
     * - `id`：`id`ID。
     * 返回：新创建的对象实例。
     */
    @JsonCreator
    public EdgeEventId(@JsonProperty("id") UUID id) {
        super(id);
    }

    /**
     * 功能：执行 `fromString` 对应的处理。
     * 参数：
     * - `edgeEventId`：边缘节点ID。
     * 返回：处理结果。
     */
    public static EdgeEventId fromString(String edgeEventId) {
        return new EdgeEventId(UUID.fromString(edgeEventId));
    }
}
