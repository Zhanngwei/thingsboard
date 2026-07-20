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
package org.thingsboard.server.edge;

import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.edge.v1.CustomerUpdateMsg;
import org.thingsboard.server.gen.edge.v1.EdgeConfiguration;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;

import java.util.Optional;
import java.util.UUID;

/**
 * 中文说明：
 * 1. `EdgeTest` 是 ThingsBoard Application 中验证 `Edge` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractEdgeTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@DaoSqlTest
public class EdgeTest extends AbstractEdgeTest {

    /**
     * 功能：验证客户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testEdge_assignToCustomer_unassignFromCustomer() throws Exception {
        // assign edge to customer
        edgeImitator.expectMessageAmount(1);
        Customer customer = new Customer();
        customer.setTitle("Edge Customer");
        Customer savedCustomer = doPost("/api/customer", customer, Customer.class);
        Assert.assertFalse(edgeImitator.waitForMessages(5));

        // assign edge to customer
        edgeImitator.expectMessageAmount(2);
        doPost("/api/customer/" + savedCustomer.getUuidId()
                + "/edge/" + edge.getUuidId(), Edge.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        Optional<EdgeConfiguration> edgeConfigurationOpt = edgeImitator.findMessageByType(EdgeConfiguration.class);
        Assert.assertTrue(edgeConfigurationOpt.isPresent());
        EdgeConfiguration edgeConfiguration = edgeConfigurationOpt.get();
        Assert.assertEquals(savedCustomer.getUuidId().getMostSignificantBits(), edgeConfiguration.getCustomerIdMSB());
        Assert.assertEquals(savedCustomer.getUuidId().getLeastSignificantBits(), edgeConfiguration.getCustomerIdLSB());
        Optional<CustomerUpdateMsg> customerUpdateOpt = edgeImitator.findMessageByType(CustomerUpdateMsg.class);
        Assert.assertTrue(customerUpdateOpt.isPresent());
        CustomerUpdateMsg customerUpdateMsg = customerUpdateOpt.get();
        Customer customerMsg = JacksonUtil.fromString(customerUpdateMsg.getEntity(), Customer.class, true);
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, customerUpdateMsg.getMsgType());
        Assert.assertEquals(savedCustomer, customerMsg);

        // unassign edge from customer
        edgeImitator.expectMessageAmount(2);
        doDelete("/api/customer/edge/" + edge.getUuidId(), Edge.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        edgeConfigurationOpt = edgeImitator.findMessageByType(EdgeConfiguration.class);
        Assert.assertTrue(edgeConfigurationOpt.isPresent());
        edgeConfiguration = edgeConfigurationOpt.get();
        Assert.assertEquals(
                new CustomerId(EntityId.NULL_UUID),
                new CustomerId(new UUID(edgeConfiguration.getCustomerIdMSB(), edgeConfiguration.getCustomerIdLSB())));
        customerUpdateOpt = edgeImitator.findMessageByType(CustomerUpdateMsg.class);
        Assert.assertTrue(customerUpdateOpt.isPresent());
        customerUpdateMsg = customerUpdateOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, customerUpdateMsg.getMsgType());
        Assert.assertEquals(savedCustomer.getUuidId().getMostSignificantBits(), customerUpdateMsg.getIdMSB());
        Assert.assertEquals(savedCustomer.getUuidId().getLeastSignificantBits(), customerUpdateMsg.getIdLSB());
    }
}
