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
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;

import java.util.Map;

/**
 * 中文说明：
 * 1. `ModelObject` 是 ThingsBoard Common Transport 中负责 `Model Object` 接入或传输适配的类型。
 * 2. 它处理连接、会话、协议消息或平台传输消息之间的转换。
 * 3. 类中的状态和配置用于控制当前协议交互的具体行为。
 * 4. 直接依赖的类型边界包括 `Cloneable`。
 * 5. 单独的传输类型可以隔离协议细节，使平台内部继续使用统一消息模型。
 * 6. 阅读时重点关注入站消息入口、会话状态和消息提交位置。
 */
@Data
public class ModelObject  implements Cloneable {
    /**
     * model one on all instance
     * for each instance only id resource with parameters of resources (observe, attr, telemetry)
     */
    /**
     * `objectModel` 字段，保存当前对象的对应属性。
     */
    private ObjectModel objectModel;
    private Map<Integer, LwM2mObjectInstance> instances;

     /**
      * 功能：创建 `ModelObject` 实例，并初始化必要字段。
      * 参数：
      * - `objectModel`：`objectModel` 参数。
      * - `instances`：键值映射。
      * 返回：新创建的对象实例。
      */
     public ModelObject(ObjectModel objectModel, Map<Integer, LwM2mObjectInstance> instances) {
        this.objectModel = objectModel;
        this.instances = instances;
    }

    /**
     * 功能：删除或清理`Instance`。
     * 参数：
     * - `id`：`id`ID。
     * 返回：判断结果。
     */
    public boolean removeInstance (int id ) {
        LwM2mObjectInstance instance = this.instances.get(id);
         return this.instances.remove(id, instance);
    }

    /**
     * 功能：执行 `clone` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public ModelObject clone() throws CloneNotSupportedException {
        return (ModelObject) super.clone();
    }
}
