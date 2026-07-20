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
package org.thingsboard.server.transport.coap;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;

/**
 * 中文说明：
 * 1. `CoapTestCallback` 是 ThingsBoard Application 中处理 CoAP 的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 直接依赖的类型边界包括 `CoapHandler`。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
@Slf4j
@Data
public class CoapTestCallback implements CoapHandler {

    /**
     * `observe` 字段，保存当前对象的对应属性。
     */
    protected volatile Integer observe;
    protected volatile byte[] payloadBytes;
    /**
     * 当前响应对象，封装处理完成后的返回信息。
     */
    protected volatile CoAP.ResponseCode responseCode;

    /**
     * 功能：获取`Observe`。
     * 参数：无。
     * 返回：数值结果。
     */
    public Integer getObserve() {
        return observe;
    }

    /**
     * 功能：获取消息载荷。
     * 参数：无。
     * 返回：处理结果。
     */
    public byte[] getPayloadBytes() {
        return payloadBytes;
    }

    /**
     * 功能：获取响应。
     * 参数：无。
     * 返回：处理结果。
     */
    public CoAP.ResponseCode getResponseCode() {
        return responseCode;
    }

    /**
     * 功能：处理`on Load`。
     * 参数：
     * - `response`：响应对象。
     * 返回：无。
     */
    @Override
    public void onLoad(CoapResponse response) {
        observe = response.getOptions().getObserve();
        payloadBytes = response.getPayload();
        responseCode = response.getCode();
    }

    /**
     * 功能：处理错误信息。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void onError() {
        log.warn("Command Response Ack Error, No connect");
    }

}
