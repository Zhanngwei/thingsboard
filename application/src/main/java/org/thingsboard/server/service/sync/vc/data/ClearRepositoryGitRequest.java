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
package org.thingsboard.server.service.sync.vc.data;

import org.thingsboard.server.common.data.id.TenantId;

/**
 * 中文说明：
 * 1. `ClearRepositoryGitRequest` 是 ThingsBoard Application 中承载请求信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `VoidGitRequest`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
public class ClearRepositoryGitRequest extends VoidGitRequest {

    /**
     * 功能：创建 `ClearRepositoryGitRequest` 实例，并初始化必要字段。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：新创建的对象实例。
     */
    public ClearRepositoryGitRequest(TenantId tenantId) {
        super(tenantId);
    }

    /**
     * 功能：执行 `requiresSettings` 对应的处理。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean requiresSettings() {
        return false;
    }

}
