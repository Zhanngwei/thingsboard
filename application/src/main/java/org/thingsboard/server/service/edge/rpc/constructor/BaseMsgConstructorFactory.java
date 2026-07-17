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
package org.thingsboard.server.service.edge.rpc.constructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.queue.util.TbCoreComponent;

/**
 * 中文说明：
 * 1. `BaseMsgConstructorFactory` 是 ThingsBoard Application 中创建或提供消息对象的构造组件。
 * 2. 它根据输入配置、类型或上下文选择合适的具体实现。
 * 3. 创建细节被集中在该类型中，调用方只依赖稳定的创建入口。
 * 4. 直接依赖的类型边界包括 `MsgConstructor`。
 * 5. 独立工厂可以避免调用方了解构造顺序和实现类选择规则。
 * 6. 阅读时重点关注实现选择条件、默认分支和对象初始化参数。
 */
@Component
@TbCoreComponent
public abstract class BaseMsgConstructorFactory<T extends MsgConstructor, U extends MsgConstructor> {

    /**
     * `v1Constructor` 字段，保存当前对象的对应属性。
     */
    @Autowired
    protected T v1Constructor;

    /**
     * `v2Constructor` 字段，保存当前对象的对应属性。
     */
    @Autowired
    protected U v2Constructor;

    /**
     * 功能：获取边缘节点。
     * 参数：
     * - `edgeVersion`：`edgeVersion` 参数。
     * 返回：处理结果。
     */
    public MsgConstructor getMsgConstructorByEdgeVersion(EdgeVersion edgeVersion) {
        switch (edgeVersion) {
            case V_3_3_0:
            case V_3_3_3:
            case V_3_4_0:
            case V_3_6_0:
            case V_3_6_1:
                return v1Constructor;
            case V_3_6_2:
            default:
                return v2Constructor;
        }
    }
}
