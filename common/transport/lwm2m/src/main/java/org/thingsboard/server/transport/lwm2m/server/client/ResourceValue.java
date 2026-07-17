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
package org.thingsboard.server.transport.lwm2m.server.client;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.node.LwM2mMultipleResource;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mResourceInstance;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.request.WriteRequest.Mode;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 中文说明：
 * 1. `ResourceValue` 是 ThingsBoard Common Transport 中负责资源接入或传输适配的类型。
 * 2. 它处理连接、会话、协议消息或平台传输消息之间的转换。
 * 3. 类中的状态和配置用于控制当前协议交互的具体行为。
 * 4. 它直接协作于传输服务、会话对象、编解码器或网络处理器。
 * 5. 单独的传输类型可以隔离协议细节，使平台内部继续使用统一消息模型。
 * 6. 阅读时重点关注入站消息入口、会话状态和消息提交位置。
 */
@Slf4j
@Data
public class ResourceValue {

    /**
     * `lwM2mResource` 字段，保存当前对象的对应属性。
     */
    private LwM2mResource lwM2mResource;
    private ResourceModel resourceModel;

    /**
     * 功能：创建 `ResourceValue` 实例，并初始化必要字段。
     * 参数：
     * - `lwM2mResource`：`lwM2mResource` 参数。
     * - `resourceModel`：`resourceModel` 参数。
     * 返回：新创建的对象实例。
     */
    public ResourceValue(LwM2mResource lwM2mResource, ResourceModel resourceModel) {
        this.resourceModel = resourceModel;
        updateLwM2mResource(lwM2mResource, Mode.UPDATE);
    }

    /**
     * 功能：更新`Lw M2m Resource`。
     * 参数：
     * - `lwM2mResource`：`lwM2mResource` 参数。
     * - `mode`：`mode` 参数。
     * 返回：无。
     */
    public void updateLwM2mResource(LwM2mResource lwM2mResource, Mode mode) {
        if (lwM2mResource instanceof LwM2mSingleResource) {
            this.lwM2mResource = LwM2mSingleResource.newResource(lwM2mResource.getId(), lwM2mResource.getValue(), lwM2mResource.getType());
        } else if (lwM2mResource instanceof LwM2mMultipleResource) {
            if (lwM2mResource.getInstances().values().size() > 0) {
                Set<LwM2mResourceInstance> instancesSet = new HashSet<>(lwM2mResource.getInstances().values());
                if (Mode.REPLACE.equals(mode) && this.lwM2mResource != null) {
                    Map<Integer, LwM2mResourceInstance> oldInstances = this.lwM2mResource.getInstances();
                    oldInstances.values().forEach(v -> {
                        if (instancesSet.stream().noneMatch(vIns -> v.getId() == vIns.getId())) {
                            instancesSet.add(v);
                        }
                    });
                }
                LwM2mResourceInstance[] instances = instancesSet.toArray(new LwM2mResourceInstance[0]);
                this.lwM2mResource = new LwM2mMultipleResource(lwM2mResource.getId(), lwM2mResource.getType(), instances);
            }
        }
    }
}
