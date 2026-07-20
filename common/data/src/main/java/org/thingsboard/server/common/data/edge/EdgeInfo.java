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
package org.thingsboard.server.common.data.edge;

import lombok.Data;
import org.thingsboard.server.common.data.id.EdgeId;

/**
 * 中文说明：
 * 1. `EdgeInfo` 是 ThingsBoard Common Data 中承载边缘节点信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `Edge`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Data
public class EdgeInfo extends Edge {

    /**
     * 客户对象，用于描述当前业务场景。
     */
    private String customerTitle;
    private boolean customerIsPublic;

    /**
     * 功能：创建 `EdgeInfo` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public EdgeInfo() {
        super();
    }

    /**
     * 功能：创建 `EdgeInfo` 实例，并初始化必要字段。
     * 参数：
     * - `edgeId`：边缘节点ID。
     * 返回：新创建的对象实例。
     */
    public EdgeInfo(EdgeId edgeId) {
        super(edgeId);
    }

    /**
     * 功能：创建 `EdgeInfo` 实例，并初始化必要字段。
     * 参数：
     * - `edge`：`edge` 参数。
     * - `customerTitle`：`customerTitle` 参数。
     * - `customerIsPublic`：`customerIsPublic` 参数。
     * 返回：新创建的对象实例。
     */
    public EdgeInfo(Edge edge, String customerTitle, boolean customerIsPublic) {
        super(edge);
        this.customerTitle = customerTitle;
        this.customerIsPublic = customerIsPublic;
    }
}