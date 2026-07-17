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
package org.thingsboard.server.common.data.sync.vc;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 中文说明：
 * 1. `VersionLoadResult` 是 ThingsBoard Common Data 中承载版本控制信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `Serializable`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VersionLoadResult implements Serializable {

    /**
     * 版本号常量，用于统一引用固定值。
     */
    private static final long serialVersionUID = -1386093599856747449L;

    /**
     * `result`列表，用于保存一组待处理对象。
     */
    private List<EntityTypeLoadResult> result;
    private EntityLoadError error;
    /**
     * 当前处理是否已经完成。
     */
    private boolean done;

    /**
     * 功能：执行 `empty` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public static VersionLoadResult empty() {
        return VersionLoadResult.builder().result(Collections.emptyList()).build();
    }

    /**
     * 功能：执行 `success` 对应的处理。
     * 参数：
     * - `result`：数据列表。
     * 返回：处理结果。
     */
    public static VersionLoadResult success(List<EntityTypeLoadResult> result) {
        return VersionLoadResult.builder().result(result).build();
    }

    /**
     * 功能：执行 `success` 对应的处理。
     * 参数：
     * - `result`：`result` 参数。
     * 返回：处理结果。
     */
    public static VersionLoadResult success(EntityTypeLoadResult result) {
        return VersionLoadResult.builder().result(List.of(result)).build();
    }

    /**
     * 功能：执行 `error` 对应的处理。
     * 参数：
     * - `error`：错误信息。
     * 返回：处理结果。
     */
    public static VersionLoadResult error(EntityLoadError error) {
        return VersionLoadResult.builder().error(error).done(true).build();
    }

}
