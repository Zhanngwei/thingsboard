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
import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;
import org.thingsboard.server.transport.lwm2m.server.log.LwM2MTelemetryLogService;

import java.util.Arrays;

import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.LOG_LWM2M_ERROR;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.LOG_LWM2M_INFO;

/**
 * 中文说明：
 * 1. `TbLwM2MTargetedCallback` 是 ThingsBoard Common Transport 中处理 LwM2M 的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 直接依赖的类型边界包括 `AbstractTbLwM2MRequestCallback`。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
@Slf4j
public abstract class TbLwM2MTargetedCallback<R, T> extends AbstractTbLwM2MRequestCallback<R, T> {

    /**
     * `versionedId`ID，用于定位对应业务对象。
     */
    protected final String versionedId;
    protected final String[] versionedIds;

    /**
     * 功能：创建 `TbLwM2MTargetedCallback` 实例，并初始化必要字段。
     * 参数：
     * - `logService`：服务对象。
     * - `client`：客户端对象。
     * - `versionedId`：`versionedId`ID。
     * 返回：新创建的对象实例。
     */
    public TbLwM2MTargetedCallback(LwM2MTelemetryLogService logService, LwM2mClient client, String versionedId) {
        super(logService, client);
        this.versionedId = versionedId;
        this.versionedIds = null;
    }

    /**
     * 功能：创建 `TbLwM2MTargetedCallback` 实例，并初始化必要字段。
     * 参数：
     * - `logService`：服务对象。
     * - `client`：客户端对象。
     * - `versionedIds`：`versionedIds` 参数。
     * 返回：新创建的对象实例。
     */
    public TbLwM2MTargetedCallback(LwM2MTelemetryLogService logService, LwM2mClient client, String[] versionedIds) {
        super(logService, client);
        this.versionedId = null;
        this.versionedIds = versionedIds;
    }

    /**
     * 功能：处理`on Success`。
     * 参数：
     * - `request`：请求对象。
     * - `response`：响应对象。
     * 返回：无。
     */
    @Override
    public void onSuccess(R request, T response) {
        //TODO convert camelCase to "camel case" using .split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])")
        if (response instanceof LwM2mResponse) {
            logForBadResponse(((LwM2mResponse) response).getCode().getCode(), response.toString(), request.getClass().getSimpleName());
        }
    }

    /**
     * 功能：执行 `logForBadResponse` 对应的处理。
     * 参数：
     * - `code`：`code` 参数。
     * - `responseStr`：响应对象。
     * - `requestName`：请求对象。
     * 返回：无。
     */
    public void logForBadResponse(int code, String responseStr, String requestName) {
        if (code > ResponseCode.CONTENT_CODE) {
            log.error("[{}] [{}] [{}] failed to process successful response [{}] ", client.getEndpoint(), requestName,
                    versionedId != null ? versionedId : Arrays.toString(versionedIds), responseStr);
            logService.log(client, String.format("[%s]: %s [%s] failed to process successful. Result: %s", LOG_LWM2M_ERROR,
                    requestName, versionedId != null ? versionedId : Arrays.toString(versionedIds), responseStr));
        } else {
            log.trace("[{}] {} [{}] successful: {}", client.getEndpoint(), requestName, versionedId != null ?
                    versionedId : versionedIds, responseStr);
            logService.log(client, String.format("[%s]: %s [%s] successful. Result: %s", LOG_LWM2M_INFO, requestName,
                    versionedId != null ? versionedId : Arrays.toString(versionedIds), responseStr));
        }
    }
}
