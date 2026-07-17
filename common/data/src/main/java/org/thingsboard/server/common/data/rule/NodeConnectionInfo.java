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
package org.thingsboard.server.common.data.rule;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * Created by ashvayka on 21.03.18.
 */
/**
 * 中文说明：
 * 1. `NodeConnectionInfo` 是 ThingsBoard Common Data 中承载 `Node Connection` 信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@ApiModel
@Data
public class NodeConnectionInfo {
    /**
     * 索引，用于控制数量、位置或分页范围。
     */
    @ApiModelProperty(position = 1, required = true, value = "Index of rule node in the 'nodes' array of the RuleChainMetaData. Indicates the 'from' part of the connection.")
    private int fromIndex;
    /**
     * 索引，用于控制数量、位置或分页范围。
     */
    @ApiModelProperty(position = 2, required = true, value = "Index of rule node in the 'nodes' array of the RuleChainMetaData. Indicates the 'to' part of the connection.")
    private int toIndex;
    /**
     * 类型，用于区分不同处理分支。
     */
    @ApiModelProperty(position = 3, required = true, value = "Type of the relation. Typically indicated the result of processing by the 'from' rule node. For example, 'Success' or 'Failure'")
    private String type;
}
