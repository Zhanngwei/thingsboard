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
package org.thingsboard.server.transport.lwm2m.server.downlink;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.util.Hex;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;
import org.thingsboard.server.transport.lwm2m.server.log.LwM2MTelemetryLogService;
import org.thingsboard.server.transport.lwm2m.server.uplink.LwM2mUplinkMsgHandler;

/**
 * 中文说明：
 * 1. `TbLwM2MReadCallback` 是 ThingsBoard Common Transport 中处理 LwM2M 的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 直接依赖的类型边界包括 `TbLwM2MUplinkTargetedCallback`。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
@Slf4j
public class TbLwM2MReadCallback extends TbLwM2MUplinkTargetedCallback<ReadRequest, ReadResponse> {

    /**
     * 功能：创建 `TbLwM2MReadCallback` 实例，并初始化必要字段。
     * 参数：
     * - `handler`：处理器对象。
     * - `logService`：服务对象。
     * - `client`：客户端对象。
     * - `targetId`：目标对象ID。
     * 返回：新创建的对象实例。
     */
    public TbLwM2MReadCallback(LwM2mUplinkMsgHandler handler, LwM2MTelemetryLogService logService, LwM2mClient client, String targetId) {
        super(handler, logService, client, targetId);
    }

    /**
     * 功能：处理`on Success`。
     * 参数：
     * - `request`：请求对象。
     * - `response`：响应对象。
     * 返回：无。
     */
    @Override
    public void onSuccess(ReadRequest request, ReadResponse response) {
        logForBadResponse(response.getCode().getCode(), responseToString(response), request.getClass().getSimpleName());
        handler.onUpdateValueAfterReadResponse(client.getRegistration(), versionedId, response);
    }

    /**
     * 功能：执行 `responseToString` 对应的处理。
     * 参数：
     * - `response`：响应对象。
     * 返回：文本结果。
     */
    private String responseToString(ReadResponse response) {
        if (response.getContent() instanceof LwM2mSingleResource) {
            LwM2mSingleResource singleResource = (LwM2mSingleResource) response.getContent();
            if (ResourceModel.Type.OPAQUE.equals(singleResource.getType())) {
                byte[] valueInBytes = (byte[]) singleResource.getValue();
                int len = valueInBytes.length;
                if (len > 0) {
                    String valueReplace = len + "Bytes";
                    String valueStr = Hex.encodeHexString(valueInBytes);
                    return response.toString().replace(valueReplace, valueStr);
                }
            }
        }
        return response.toString();
    }

}
