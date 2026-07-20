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
package org.thingsboard.server.service.edge.rpc.constructor.rule;

import org.thingsboard.server.gen.edge.v1.EdgeVersion;

/**
 * 中文说明：
 * 1. `RuleChainMetadataConstructorFactory` 是 ThingsBoard Application 中创建或提供规则链对象的构造组件。
 * 2. 它根据输入配置、类型或上下文选择合适的具体实现。
 * 3. 创建细节被集中在该类型中，调用方只依赖稳定的创建入口。
 * 4. 它直接协作于目标接口、具体实现和创建所需配置。
 * 5. 独立工厂可以避免调用方了解构造顺序和实现类选择规则。
 * 6. 阅读时重点关注实现选择条件、默认分支和对象初始化参数。
 */
public final class RuleChainMetadataConstructorFactory {

    /**
     * 功能：获取边缘节点。
     * 参数：
     * - `edgeVersion`：`edgeVersion` 参数。
     * 返回：处理结果。
     */
    public static RuleChainMetadataConstructor getByEdgeVersion(EdgeVersion edgeVersion) {
        switch (edgeVersion) {
            case V_3_3_0:
                return new RuleChainMetadataConstructorV330();
            case V_3_3_3:
            case V_3_4_0:
            case V_3_6_0:
            case V_3_6_1:
                return new RuleChainMetadataConstructorV340();
            case V_3_6_2:
            default:
                return new RuleChainMetadataConstructorV362();
        }
    }
}
