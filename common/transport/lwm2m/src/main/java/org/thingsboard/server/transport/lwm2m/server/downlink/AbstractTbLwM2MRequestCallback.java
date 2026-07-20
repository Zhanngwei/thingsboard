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
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;
import org.thingsboard.server.transport.lwm2m.server.log.LwM2MTelemetryLogService;

import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.LOG_LWM2M_ERROR;

/**
 * 中文说明：
 * 1. `AbstractTbLwM2MRequestCallback` 是 ThingsBoard Common Transport 中处理请求的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 直接依赖的类型边界包括 `DownlinkRequestCallback`。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
@Slf4j
public abstract class AbstractTbLwM2MRequestCallback<R, T> implements DownlinkRequestCallback<R, T> {

    /**
     * 服务，提供当前类调用的业务操作。
     */
    protected final LwM2MTelemetryLogService logService;
    protected final LwM2mClient client;

    /**
     * 功能：创建 `AbstractTbLwM2MRequestCallback` 实例，并初始化必要字段。
     * 参数：
     * - `logService`：服务对象。
     * - `client`：客户端对象。
     * 返回：新创建的对象实例。
     */
    protected AbstractTbLwM2MRequestCallback(LwM2MTelemetryLogService logService, LwM2mClient client) {
        this.logService = logService;
        this.client = client;
    }

    /**
     * 功能：处理错误信息。
     * 参数：
     * - `params`：`params` 参数。
     * - `msg`：待处理消息。
     * 返回：无。
     */
    @Override
    public void onValidationError(String params, String msg) {
        log.trace("[{}] Request [{}] validation failed. Reason: {}", client.getEndpoint(), params, msg);
        logService.log(client, String.format("[%s]: Request [%s] validation failed. Reason: %s", LOG_LWM2M_ERROR, params, msg));
    }

    /**
     * 功能：处理错误信息。
     * 参数：
     * - `params`：`params` 参数。
     * - `e`：`e` 参数。
     * 返回：无。
     */
    @Override
    public void onError(String params, Exception e) {
        log.trace("[{}] Request [{}] processing failed", client.getEndpoint(), params, e);
        logService.log(client, String.format("[%s]: Request [%s] processing failed. Reason: %s", LOG_LWM2M_ERROR, params, e));
    }
}
