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

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 中文说明：
 * 1. `VersionCreationResult` 是 ThingsBoard Common Data 中承载版本控制信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `Serializable`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Data
@NoArgsConstructor
public class VersionCreationResult implements Serializable {
    /**
     * 版本号常量，用于统一引用固定值。
     */
    private static final long serialVersionUID = 8032189124530267838L;

    /**
     * 版本号，表示当前对象的对应属性。
     */
    private EntityVersion version;
    private int added;
    /**
     * `modified` 字段，保存当前对象的对应属性。
     */
    private int modified;
    private int removed;

    /**
     * 错误信息，记录当前处理过程中的失败原因。
     */
    private String error;
    private boolean done;


    /**
     * 功能：创建 `VersionCreationResult` 实例，并初始化必要字段。
     * 参数：
     * - `version`：`version` 参数。
     * - `added`：`added` 参数。
     * - `modified`：`modified` 参数。
     * - `removed`：`removed` 参数。
     * 返回：新创建的对象实例。
     */
    public VersionCreationResult(EntityVersion version, int added, int modified, int removed) {
        this.version = version;
        this.added = added;
        this.modified = modified;
        this.removed = removed;
        this.done = true;
    }

    /**
     * 功能：创建 `VersionCreationResult` 实例，并初始化必要字段。
     * 参数：
     * - `error`：错误信息。
     * 返回：新创建的对象实例。
     */
    public VersionCreationResult(String error) {
        this.error = error;
        this.done = true;
    }
}
