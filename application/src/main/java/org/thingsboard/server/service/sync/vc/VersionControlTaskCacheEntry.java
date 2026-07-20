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
package org.thingsboard.server.service.sync.vc;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.thingsboard.server.common.data.sync.vc.VersionCreationResult;
import org.thingsboard.server.common.data.sync.vc.VersionLoadResult;

import java.io.Serializable;

/**
 * 中文说明：
 * 1. `VersionControlTaskCacheEntry` 是 ThingsBoard Application 中管理缓存缓存内容或失效事件的类型。
 * 2. 它保存缓存键、缓存值或触发清理所需的最小业务信息。
 * 3. 相关方法负责读取、更新或移除当前领域的缓存条目。
 * 4. 直接依赖的类型边界包括 `Serializable`。
 * 5. 独立缓存边界可以统一键规则和失效行为，避免各调用点自行维护。
 * 6. 阅读时重点关注缓存键组成、命中后的返回值和失效触发条件。
 */
@Data
@AllArgsConstructor
public class VersionControlTaskCacheEntry implements Serializable {

    /**
     * 版本号常量，用于统一引用固定值。
     */
    private static final long serialVersionUID = -7875992200801588119L;

    /**
     * `exportResult` 字段，保存当前对象的对应属性。
     */
    private VersionCreationResult exportResult;
    private VersionLoadResult importResult;

    /**
     * 功能：执行 `newForExport` 对应的处理。
     * 参数：
     * - `result`：`result` 参数。
     * 返回：处理结果。
     */
    public static VersionControlTaskCacheEntry newForExport(VersionCreationResult result) {
        return new VersionControlTaskCacheEntry(result, null);
    }

    /**
     * 功能：执行 `newForImport` 对应的处理。
     * 参数：
     * - `result`：`result` 参数。
     * 返回：处理结果。
     */
    public static VersionControlTaskCacheEntry newForImport(VersionLoadResult result) {
        return new VersionControlTaskCacheEntry(null, result);
    }


}
