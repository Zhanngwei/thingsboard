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
package org.thingsboard.server.service.entitiy.edge;

import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rule.RuleChain;

/**
 * 中文说明：
 * 1. `TbEdgeService` 是 ThingsBoard Application 中定义边缘节点能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 它直接协作于实现类以及使用该接口的调用组件。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface TbEdgeService {
    /**
     * 功能：执行 `save` 对应的处理。
     * 参数：
     * - `edge`：`edge` 参数。
     * - `edgeTemplateRootRuleChain`：`edgeTemplateRootRuleChain` 参数。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    Edge save(Edge edge, RuleChain edgeTemplateRootRuleChain, User user) throws Exception;

    /**
     * 功能：执行 `delete` 对应的处理。
     * 参数：
     * - `edge`：`edge` 参数。
     * - `user`：`user` 参数。
     * 返回：无。
     */
    void delete(Edge edge, User user);

    /**
     * 功能：执行 `assignEdgeToCustomer` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * - `customer`：`customer` 参数。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    Edge assignEdgeToCustomer(TenantId tenantId, EdgeId edgeId, Customer customer, User user) throws ThingsboardException;

    /**
     * 功能：执行 `unassignEdgeFromCustomer` 对应的处理。
     * 参数：
     * - `edge`：`edge` 参数。
     * - `customer`：`customer` 参数。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    Edge unassignEdgeFromCustomer(Edge edge, Customer customer, User user) throws ThingsboardException;

    /**
     * 功能：执行 `assignEdgeToPublicCustomer` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    Edge assignEdgeToPublicCustomer(TenantId tenantId, EdgeId edgeId, User user) throws ThingsboardException;

    /**
     * 功能：更新规则链。
     * 参数：
     * - `edge`：`edge` 参数。
     * - `ruleChainId`：规则链ID。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    Edge setEdgeRootRuleChain(Edge edge, RuleChainId ruleChainId, User user) throws Exception;
}
