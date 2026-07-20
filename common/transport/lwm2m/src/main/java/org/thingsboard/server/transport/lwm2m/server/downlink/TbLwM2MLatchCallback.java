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

import lombok.RequiredArgsConstructor;

import java.util.concurrent.CountDownLatch;

/**
 * 中文说明：
 * 1. `TbLwM2MLatchCallback` 是 ThingsBoard Common Transport 中处理 LwM2M 的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 直接依赖的类型边界包括 `DownlinkRequestCallback`。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
@RequiredArgsConstructor
public class TbLwM2MLatchCallback<R, T> implements DownlinkRequestCallback<R, T> {

    /**
     * 等待器，用于在测试或异步流程中等待结果。
     */
    private final CountDownLatch countDownLatch;
    private final DownlinkRequestCallback<R, T> callback;

    /**
     * 功能：处理`on Success`。
     * 参数：
     * - `request`：请求对象。
     * - `response`：响应对象。
     * 返回：无。
     */
    @Override
    public void onSuccess(R request, T response) {
        callback.onSuccess(request, response);
        countDownLatch.countDown();
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
        callback.onValidationError(params, msg);
        countDownLatch.countDown();
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
        callback.onError(params, e);
        countDownLatch.countDown();
    }
}
