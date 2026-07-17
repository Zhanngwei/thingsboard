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

import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.gen.edge.v1.CustomerUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.util.TbCoreComponent;

/**
 * 中文说明：
 * 1. `CustomerMsgConstructorV1` 是 ThingsBoard Application 中创建或提供客户对象的构造组件。
 * 2. 它根据输入配置、类型或上下文选择合适的具体实现。
 * 3. 创建细节被集中在该类型中，调用方只依赖稳定的创建入口。
 * 4. 直接依赖的类型边界包括 `BaseCustomerMsgConstructor`。
 * 5. 独立工厂可以避免调用方了解构造顺序和实现类选择规则。
 * 6. 阅读时重点关注实现选择条件、默认分支和对象初始化参数。
 */
@Component
@TbCoreComponent
public class CustomerMsgConstructorV1 extends BaseCustomerMsgConstructor {

    /**
     * 功能：执行 `constructCustomerUpdatedMsg` 对应的处理。
     * 参数：
     * - `msgType`：待处理消息。
     * - `customer`：`customer` 参数。
     * 返回：处理结果。
     */
    @Override
    public CustomerUpdateMsg constructCustomerUpdatedMsg(UpdateMsgType msgType, Customer customer) {
        CustomerUpdateMsg.Builder builder = CustomerUpdateMsg.newBuilder()
                .setMsgType(msgType)
                .setIdMSB(customer.getId().getId().getMostSignificantBits())
                .setIdLSB(customer.getId().getId().getLeastSignificantBits())
                .setTitle(customer.getTitle());
        if (customer.getCountry() != null) {
            builder.setCountry(customer.getCountry());
        }
        if (customer.getState() != null) {
            builder.setState(customer.getState());
        }
        if (customer.getCity() != null) {
            builder.setCity(customer.getCity());
        }
        if (customer.getAddress() != null) {
            builder.setAddress(customer.getAddress());
        }
        if (customer.getAddress2() != null) {
            builder.setAddress2(customer.getAddress2());
        }
        if (customer.getZip() != null) {
            builder.setZip(customer.getZip());
        }
        if (customer.getPhone() != null) {
            builder.setPhone(customer.getPhone());
        }
        if (customer.getEmail() != null) {
            builder.setEmail(customer.getEmail());
        }
        if (customer.getAdditionalInfo() != null) {
            builder.setAdditionalInfo(JacksonUtil.toString(customer.getAdditionalInfo()));
        }
        return builder.build();
    }
}
