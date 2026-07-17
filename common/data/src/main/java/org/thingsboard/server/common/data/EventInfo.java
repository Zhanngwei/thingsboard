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
package org.thingsboard.server.common.data;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EventId;
import org.thingsboard.server.common.data.id.TenantId;

/**
 * @author Andrew Shvayka
 */
/**
 * 中文说明：
 * 1. `EventInfo` 是 ThingsBoard Common Data 中承载事件信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `BaseData`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Data
@ApiModel
public class EventInfo extends BaseData<EventId> {

    /**
     * 租户ID，用于定位对应业务对象。
     */
    @ApiModelProperty(position = 1, value = "JSON object with Tenant Id.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private TenantId tenantId;
    /**
     * 类型，用于区分不同处理分支。
     */
    @ApiModelProperty(position = 2, value = "Event type", example = "STATS")
    private String type;
    /**
     * `uid`ID，用于定位对应业务对象。
     */
    @ApiModelProperty(position = 3, value = "string", example = "784f394c-42b6-435a-983c-b7beff2784f9")
    private String uid;
    /**
     * 实体ID，用于定位对应业务对象。
     */
    @ApiModelProperty(position = 4, value = "JSON object with Entity Id for which event is created.", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    private EntityId entityId;
    /**
     * `body` 字段，保存当前对象的对应属性。
     */
    @ApiModelProperty(position = 5, value = "Event body.", dataType = "com.fasterxml.jackson.databind.JsonNode")
    private transient JsonNode body;

    /**
     * 功能：创建 `EventInfo` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public EventInfo() {
        super();
    }

    /**
     * 功能：创建 `EventInfo` 实例，并初始化必要字段。
     * 参数：
     * - `id`：`id`ID。
     * 返回：新创建的对象实例。
     */
    public EventInfo(EventId id) {
        super(id);
    }

    /**
     * 功能：创建 `EventInfo` 实例，并初始化必要字段。
     * 参数：
     * - `event`：`event` 参数。
     * 返回：新创建的对象实例。
     */
    public EventInfo(EventInfo event) {
        super(event);
    }

    /**
     * 功能：获取创建时间。
     * 参数：无。
     * 返回：数值结果。
     */
    @ApiModelProperty(position = 6, value = "Timestamp of the event creation, in milliseconds", example = "1609459200000", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }
}
