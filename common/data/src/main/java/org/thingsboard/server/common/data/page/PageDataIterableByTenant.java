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
package org.thingsboard.server.common.data.page;

import org.thingsboard.server.common.data.id.TenantId;

/**
 * 中文说明：
 * 1. `PageDataIterableByTenant` 是 ThingsBoard Common Data 中承载租户信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `BasePageDataIterable`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
public class PageDataIterableByTenant<T> extends BasePageDataIterable<T> {

    /**
     * `function` 字段，保存当前对象的对应属性。
     */
    private final FetchFunction<T> function;
    private final TenantId tenantId;

    /**
     * 功能：创建 `PageDataIterableByTenant` 实例，并初始化必要字段。
     * 参数：
     * - `function`：`function` 参数。
     * - `tenantId`：租户IDID。
     * - `fetchSize`：`fetchSize` 参数。
     * 返回：新创建的对象实例。
     */
    public PageDataIterableByTenant(FetchFunction<T> function, TenantId tenantId, int fetchSize) {
        super(fetchSize);
        this.function = function;
        this.tenantId = tenantId;
    }

    /**
     * 功能：获取数据。
     * 参数：
     * - `link`：`link` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    PageData<T> fetchPageData(PageLink link) {
        return function.fetch(tenantId, link);
    }

    /**
     * 中文说明：
     * 1. `FetchFunction` 是 ThingsBoard Common Data 中定义 `Fetch Function` 能力边界的接口。
     * 2. 它声明实现方必须提供的核心操作和输入输出约定。
     * 3. 接口方法共同定义该能力的输入、输出和行为边界。
     * 4. 它直接协作于实现类以及使用该接口的调用组件。
     * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
     * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
     */
    public interface FetchFunction<T> {
        /**
         * 功能：执行 `fetch` 对应的处理。
         * 参数：
         * - `tenantId`：租户IDID。
         * - `link`：`link` 参数。
         * 返回：匹配的数据集合。
         */
        PageData<T> fetch(TenantId tenantId, PageLink link);
    }
}
