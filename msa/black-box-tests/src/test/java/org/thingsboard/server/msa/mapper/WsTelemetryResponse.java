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
package org.thingsboard.server.msa.mapper;

import lombok.Data;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. `WsTelemetryResponse` 是 ThingsBoard Microservices 中承载响应信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `Serializable`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Data
public class WsTelemetryResponse implements Serializable {
    /**
     * 订阅ID，用于定位对应业务对象。
     */
    private int subscriptionId;
    private int errorCode;
    /**
     * 消息，承载当前步骤需要处理的内容。
     */
    private String errorMsg;
    private Map<String, List<List<Object>>> data;
    /**
     * `latestValues`映射关系，用于按键查找对应值。
     */
    private Map<String, Object> latestValues;

    /**
     * 功能：获取数据。
     * 参数：
     * - `key`：键。
     * 返回：匹配的数据集合。
     */
    public List<Object> getDataValuesByKey(String key) {
        return data.entrySet().stream()
                .filter(e -> e.getKey().equals(key))
                .flatMap(e -> e.getValue().stream().flatMap(Collection::stream))
                .collect(Collectors.toList());
    }
}
