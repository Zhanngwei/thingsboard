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
package org.thingsboard.server.service.edge.rpc.processor.telemetry;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EntityDataProto;
import org.thingsboard.server.queue.util.TbCoreComponent;

/**
 * 中文说明：
 * 1. `TelemetryEdgeProcessor` 是 ThingsBoard Application 中处理遥测数据的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 直接依赖的类型边界包括 `BaseTelemetryProcessor`。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
@Slf4j
@Component
@TbCoreComponent
public class TelemetryEdgeProcessor extends BaseTelemetryProcessor {

    /**
     * 功能：获取消息。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    protected String getMsgSourceKey() {
        return DataConstants.EDGE_MSG_SOURCE;
    }

    /**
     * 功能：转换事件。
     * 参数：
     * - `edgeEvent`：`edgeEvent` 参数。
     * 返回：处理结果。
     */
    public DownlinkMsg convertTelemetryEventToDownlink(EdgeEvent edgeEvent) {
        if (edgeEvent.getBody() != null) {
            String bodyStr = edgeEvent.getBody().toString();
            if (bodyStr.length() > 1000) {
                log.debug("[{}][{}][{}] Conversion to a DownlinkMsg telemetry event failed due to a size limit violation. " +
                                "Current size is {}, but the limit is 1000. {}", edgeEvent.getTenantId(), edgeEvent.getEdgeId(),
                        edgeEvent.getEntityId(), bodyStr.length(), StringUtils.truncate(bodyStr, 100));
                return null;
            }
        }
        EntityType entityType = EntityType.valueOf(edgeEvent.getType().name());
        EntityDataProto entityDataProto = convertTelemetryEventToEntityDataProto(
                edgeEvent.getTenantId(), entityType, edgeEvent.getEntityId(),
                edgeEvent.getAction(), edgeEvent.getBody());
        return DownlinkMsg.newBuilder()
                .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                .addEntityData(entityDataProto)
                .build();
    }
}
