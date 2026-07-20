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

import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;

/**
 * 中文说明：
 * 1. `DeviceInfoFilter` 是 ThingsBoard Common Data 中处理设备的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 它直接协作于事件源、上下文对象和后续处理组件。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
@Data
@Builder
public class DeviceInfoFilter {

    /**
     * 租户ID，用于定位对应业务对象。
     */
    private TenantId tenantId;
    private CustomerId customerId;
    /**
     * 边缘节点ID，用于定位对应业务对象。
     */
    private EdgeId edgeId;
    private String type;
    /**
     * 设备配置ID，用于定位对应业务对象。
     */
    private DeviceProfileId deviceProfileId;
    private Boolean active;

}
