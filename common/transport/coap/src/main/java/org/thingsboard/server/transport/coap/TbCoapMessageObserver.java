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

import lombok.RequiredArgsConstructor;
import org.eclipse.californium.core.coap.MessageObserver;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.elements.EndpointContext;

import java.util.function.Consumer;

/**
 * 中文说明：
 * 1. `TbCoapMessageObserver` 是 ThingsBoard Common Transport 中承载消息信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `MessageObserver`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@RequiredArgsConstructor
public class TbCoapMessageObserver implements MessageObserver {

    /**
     * 消息ID，用于定位对应业务对象。
     */
    private final int msgId;
    private final Consumer<Integer> onAcknowledge;
    /**
     * 超时时间，用于控制时间范围或等待时长。
     */
    private final Consumer<Integer> onTimeout;

    /**
     * 功能：判断`Internal`。
     * 参数：无。
     * 返回：判断结果。
     */
    @Override
    public boolean isInternal() {
        return false;
    }

    /**
     * 功能：处理`on Retransmission`。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void onRetransmission() {

    }

    /**
     * 功能：处理响应。
     * 参数：
     * - `response`：响应对象。
     * 返回：无。
     */
    @Override
    public void onResponse(Response response) {

    }

    /**
     * 功能：处理`on Acknowledgement`。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void onAcknowledgement() {
        onAcknowledge.accept(msgId);
    }

    /**
     * 功能：处理`on Reject`。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void onReject() {

    }

    /**
     * 功能：处理超时时间。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void onTimeout() {
        if (onTimeout != null) {
            onTimeout.accept(msgId);
        }
    }

    /**
     * 功能：处理`on Cancel`。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void onCancel() {

    }

    /**
     * 功能：处理`on Ready To Send`。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void onReadyToSend() {

    }

    /**
     * 功能：处理`on Connecting`。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void onConnecting() {

    }

    /**
     * 功能：处理`on Dtls Retransmission`。
     * 参数：
     * - `flight`：`flight` 参数。
     * 返回：无。
     */
    @Override
    public void onDtlsRetransmission(int flight) {

    }

    /**
     * 功能：处理`on Sent`。
     * 参数：
     * - `retransmission`：`retransmission` 参数。
     * 返回：无。
     */
    @Override
    public void onSent(boolean retransmission) {

    }

    /**
     * 功能：处理错误信息。
     * 参数：
     * - `error`：错误信息。
     * 返回：无。
     */
    @Override
    public void onSendError(Throwable error) {

    }

    /**
     * 功能：处理响应。
     * 参数：
     * - `cause`：`cause` 参数。
     * 返回：无。
     */
    @Override
    public void onResponseHandlingError(Throwable cause) {

    }

    /**
     * 功能：处理上下文。
     * 参数：
     * - `endpointContext`：处理上下文。
     * 返回：无。
     */
    @Override
    public void onContextEstablished(EndpointContext endpointContext) {

    }

    /**
     * 功能：处理`on Transfer Complete`。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void onTransferComplete() {

    }
}
