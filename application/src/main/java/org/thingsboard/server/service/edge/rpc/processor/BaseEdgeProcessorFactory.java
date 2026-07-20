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
package org.thingsboard.server.service.edge.rpc.processor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.queue.util.TbCoreComponent;

/**
 * 中文说明：
 * 1. `BaseEdgeProcessorFactory` 是 ThingsBoard Application 中处理边缘节点的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 直接依赖的类型边界包括 `EdgeProcessor`。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
@Component
@TbCoreComponent
public abstract class BaseEdgeProcessorFactory<T extends EdgeProcessor, U extends EdgeProcessor> {

    /**
     * 处理器，负责处理对应任务或消息。
     */
    @Autowired
    protected T v1Processor;

    /**
     * 处理器，负责处理对应任务或消息。
     */
    @Autowired
    protected U v2Processor;

    /**
     * 功能：获取边缘节点。
     * 参数：
     * - `edgeVersion`：`edgeVersion` 参数。
     * 返回：处理结果。
     */
    public EdgeProcessor getProcessorByEdgeVersion(EdgeVersion edgeVersion) {
        switch (edgeVersion) {
            case V_3_3_0:
            case V_3_3_3:
            case V_3_4_0:
            case V_3_6_0:
            case V_3_6_1:
                return v1Processor;
            case V_3_6_2:
            default:
                return v2Processor;
        }
    }
}
