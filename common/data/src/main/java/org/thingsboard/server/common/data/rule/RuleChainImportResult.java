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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;

/**
 * 中文说明：
 * 1. `RuleChainImportResult` 是 ThingsBoard Common Data 中承载规则链信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Data
public class RuleChainImportResult {

    /**
     * 租户ID，用于定位对应业务对象。
     */
    @JsonIgnore
    private TenantId tenantId;
    private RuleChainId ruleChainId;
    /**
     * 规则链，用于标识或展示当前对象。
     */
    private String ruleChainName;
    /**
     * 是否满足`updated`条件。
     */
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    private boolean updated;
    /**
     * 错误信息，记录当前处理过程中的失败原因。
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String error;

}
