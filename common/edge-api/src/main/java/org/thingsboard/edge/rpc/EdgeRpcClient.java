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
package org.thingsboard.edge.rpc;

import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkResponseMsg;
import org.thingsboard.server.gen.edge.v1.EdgeConfiguration;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.gen.edge.v1.UplinkResponseMsg;

import java.util.function.Consumer;

/**
 * 中文说明：
 * 1. `EdgeRpcClient` 是 ThingsBoard Common 中定义边缘节点能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 它直接协作于实现类以及使用该接口的调用组件。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface EdgeRpcClient {

    /**
     * 功能：执行 `connect` 对应的处理。
     * 参数：
     * - `integrationKey`：键。
     * - `integrationSecret`：`integrationSecret` 参数。
     * - `onUplinkResponse`：响应对象。
     * - `onEdgeUpdate`：`onEdgeUpdate` 参数。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    void connect(String integrationKey,
                 String integrationSecret,
                 Consumer<UplinkResponseMsg> onUplinkResponse,
                 Consumer<EdgeConfiguration> onEdgeUpdate,
                 Consumer<DownlinkMsg> onDownlink,
                 Consumer<Exception> onError);

    /**
     * 功能：执行 `disconnect` 对应的处理。
     * 参数：
     * - `onError`：错误信息。
     * 返回：无。
     */
    void disconnect(boolean onError) throws InterruptedException;

    /**
     * 功能：发送或提交消息。
     * 参数：
     * - `fullSyncRequired`：`fullSyncRequired` 参数。
     * 返回：无。
     */
    void sendSyncRequestMsg(boolean fullSyncRequired);

    /**
     * 功能：发送或提交消息。
     * 参数：
     * - `uplinkMsg`：待处理消息。
     * 返回：无。
     */
    void sendUplinkMsg(UplinkMsg uplinkMsg);

    /**
     * 功能：发送或提交消息。
     * 参数：
     * - `downlinkResponseMsg`：响应对象。
     * 返回：无。
     */
    void sendDownlinkResponseMsg(DownlinkResponseMsg downlinkResponseMsg);

    /**
     * 功能：获取消息。
     * 参数：无。
     * 返回：数值结果。
     */
    int getServerMaxInboundMessageSize();
}
