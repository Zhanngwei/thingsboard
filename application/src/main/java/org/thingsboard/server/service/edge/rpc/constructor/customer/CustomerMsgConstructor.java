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
package org.thingsboard.server.service.edge.rpc.constructor.customer;

import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.gen.edge.v1.CustomerUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.service.edge.rpc.constructor.MsgConstructor;

/**
 * 中文说明：
 * 1. `CustomerMsgConstructor` 是 ThingsBoard Application 中定义客户能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 直接依赖的类型边界包括 `MsgConstructor`。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface CustomerMsgConstructor extends MsgConstructor {

    /**
     * 功能：执行 `constructCustomerUpdatedMsg` 对应的处理。
     * 参数：
     * - `msgType`：待处理消息。
     * - `customer`：`customer` 参数。
     * 返回：处理结果。
     */
    CustomerUpdateMsg constructCustomerUpdatedMsg(UpdateMsgType msgType, Customer customer);

    /**
     * 功能：执行 `constructCustomerDeleteMsg` 对应的处理。
     * 参数：
     * - `customerId`：客户IDID。
     * 返回：处理结果。
     */
    CustomerUpdateMsg constructCustomerDeleteMsg(CustomerId customerId);
}
