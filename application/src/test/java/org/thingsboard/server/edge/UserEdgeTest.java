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

import com.google.protobuf.AbstractMessage;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.gen.edge.v1.UserCredentialsRequestMsg;
import org.thingsboard.server.gen.edge.v1.UserCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UserUpdateMsg;
import org.thingsboard.server.service.security.model.ChangePasswordRequest;

import java.util.Optional;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 中文说明：
 * 1. 类目的：`UserEdgeTest` 是ThingsBoard Application 测试模块中的测试支撑类型，用于验证 Application 模块的控制器、服务、Actor 或集成流程。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括JUnit、Mockito、Spring Test、DAO、缓存、Transport 和测试夹具。
 * 4. 生命周期：由测试框架按测试类和测试方法管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Test Fixture。
 */
@DaoSqlTest
public class UserEdgeTest extends AbstractEdgeTest {

    /**
     * 密码，用于认证或安全校验。
     */
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    /**
     * 功能：验证租户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testCreateUpdateDeleteTenantUser() throws Exception {
        // create user
        edgeImitator.expectMessageAmount(3);
        User newTenantAdmin = new User();
        newTenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        newTenantAdmin.setTenantId(tenantId);
        newTenantAdmin.setEmail("tenantAdmin@thingsboard.org");
        newTenantAdmin.setFirstName("Boris");
        newTenantAdmin.setLastName("Johnson");
        User savedTenantAdmin = createUser(newTenantAdmin, "tenant");
        Assert.assertTrue(edgeImitator.waitForMessages()); // wait 3 messages - user update msg and x2 user credentials update msgs
        Optional<UserUpdateMsg> userUpdateMsgOpt = edgeImitator.findMessageByType(UserUpdateMsg.class);
        Assert.assertTrue(userUpdateMsgOpt.isPresent());
        UserUpdateMsg userUpdateMsg = userUpdateMsgOpt.get();
        User userMsg = JacksonUtil.fromString(userUpdateMsg.getEntity(), User.class, true);
        Assert.assertNotNull(userMsg);
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, userUpdateMsg.getMsgType());
        Assert.assertEquals(savedTenantAdmin.getId(), userMsg.getId());
        Assert.assertEquals(savedTenantAdmin.getAuthority(), userMsg.getAuthority());
        Assert.assertEquals(savedTenantAdmin.getEmail(), userMsg.getEmail());
        Assert.assertEquals(savedTenantAdmin.getFirstName(), userMsg.getFirstName());
        Assert.assertEquals(savedTenantAdmin.getLastName(), userMsg.getLastName());
        Optional<UserCredentialsUpdateMsg> userCredentialsUpdateMsgOpt = edgeImitator.findMessageByType(UserCredentialsUpdateMsg.class);
        Assert.assertTrue(userCredentialsUpdateMsgOpt.isPresent());

        // update user
        edgeImitator.expectMessageAmount(1);
        savedTenantAdmin.setLastName("Borisov");
        savedTenantAdmin = doPost("/api/user", savedTenantAdmin, User.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof UserUpdateMsg);
        userUpdateMsg = (UserUpdateMsg) latestMessage;
        userMsg = JacksonUtil.fromString(userUpdateMsg.getEntity(), User.class, true);
        Assert.assertNotNull(userMsg);
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, userUpdateMsg.getMsgType());
        Assert.assertEquals(savedTenantAdmin.getLastName(), userMsg.getLastName());

        // update user credentials
        login(savedTenantAdmin.getEmail(), "tenant");

        edgeImitator.expectMessageAmount(1);
        ChangePasswordRequest changePasswordRequest = new ChangePasswordRequest();
        changePasswordRequest.setCurrentPassword("tenant");
        changePasswordRequest.setNewPassword("newTenant");
        doPost("/api/auth/changePassword", changePasswordRequest);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof UserCredentialsUpdateMsg);
        UserCredentialsUpdateMsg userCredentialsUpdateMsg = (UserCredentialsUpdateMsg) latestMessage;
        UserCredentials userCredentialsMsg = JacksonUtil.fromString(userCredentialsUpdateMsg.getEntity(), UserCredentials.class, true);
        Assert.assertNotNull(userCredentialsMsg);
        Assert.assertEquals(savedTenantAdmin.getId(), userCredentialsMsg.getUserId());
        Assert.assertTrue(passwordEncoder.matches(changePasswordRequest.getNewPassword(), userCredentialsMsg.getPassword()));

        loginTenantAdmin();

        // delete user
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/user/" + savedTenantAdmin.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof UserUpdateMsg);
        userUpdateMsg = (UserUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, userUpdateMsg.getMsgType());
        Assert.assertEquals(savedTenantAdmin.getUuidId().getMostSignificantBits(), userUpdateMsg.getIdMSB());
        Assert.assertEquals(savedTenantAdmin.getUuidId().getLeastSignificantBits(), userUpdateMsg.getIdLSB());
    }

    /**
     * 功能：验证客户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testCreateUpdateDeleteCustomerUser() throws Exception {
        // create customer
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

        // create user
        edgeImitator.expectMessageAmount(3);
        User customerUser = new User();
        customerUser.setAuthority(Authority.CUSTOMER_USER);
        customerUser.setTenantId(tenantId);
        customerUser.setCustomerId(savedCustomer.getId());
        customerUser.setEmail("customerUser@thingsboard.org");
        customerUser.setFirstName("John");
        customerUser.setLastName("Edwards");
        User savedCustomerUser = createUser(customerUser, "customer");
        Assert.assertTrue(edgeImitator.waitForMessages());  // wait 3 messages - user update msg and x2 user credentials update msgs
        Optional<UserUpdateMsg> userUpdateMsgOpt = edgeImitator.findMessageByType(UserUpdateMsg.class);
        Assert.assertTrue(userUpdateMsgOpt.isPresent());
        UserUpdateMsg userUpdateMsg = userUpdateMsgOpt.get();
        User userMsg = JacksonUtil.fromString(userUpdateMsg.getEntity(), User.class, true);
        Assert.assertNotNull(userMsg);
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, userUpdateMsg.getMsgType());
        Assert.assertEquals(savedCustomerUser.getId(), userMsg.getId());
        Assert.assertEquals(savedCustomerUser.getCustomerId(), userMsg.getCustomerId());
        Assert.assertEquals(savedCustomerUser.getAuthority(), userMsg.getAuthority());
        Assert.assertEquals(savedCustomerUser.getEmail(), userMsg.getEmail());
        Assert.assertEquals(savedCustomerUser.getFirstName(), userMsg.getFirstName());
        Assert.assertEquals(savedCustomerUser.getLastName(), userMsg.getLastName());

        // update user
        edgeImitator.expectMessageAmount(1);
        savedCustomerUser.setLastName("Addams");
        savedCustomerUser = doPost("/api/user", savedCustomerUser, User.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof UserUpdateMsg);
        userUpdateMsg = (UserUpdateMsg) latestMessage;
        userMsg = JacksonUtil.fromString(userUpdateMsg.getEntity(), User.class, true);
        Assert.assertNotNull(userMsg);
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, userUpdateMsg.getMsgType());
        Assert.assertEquals(savedCustomerUser.getLastName(), userMsg.getLastName());

        // update user credentials
        login(savedCustomerUser.getEmail(), "customer");

        edgeImitator.expectMessageAmount(1);
        ChangePasswordRequest changePasswordRequest = new ChangePasswordRequest();
        changePasswordRequest.setCurrentPassword("customer");
        changePasswordRequest.setNewPassword("newCustomer");
        doPost("/api/auth/changePassword", changePasswordRequest);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof UserCredentialsUpdateMsg);
        UserCredentialsUpdateMsg userCredentialsUpdateMsg = (UserCredentialsUpdateMsg) latestMessage;
        UserCredentials userCredentialsMsg = JacksonUtil.fromString(userCredentialsUpdateMsg.getEntity(), UserCredentials.class, true);
        Assert.assertNotNull(userCredentialsMsg);
        Assert.assertEquals(savedCustomerUser.getId(), userCredentialsMsg.getUserId());
        Assert.assertTrue(passwordEncoder.matches(changePasswordRequest.getNewPassword(), userCredentialsMsg.getPassword()));

        loginTenantAdmin();

        // delete user
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/user/" + savedCustomerUser.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof UserUpdateMsg);
        userUpdateMsg = (UserUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, userUpdateMsg.getMsgType());
        Assert.assertEquals(savedCustomerUser.getUuidId().getMostSignificantBits(), userUpdateMsg.getIdMSB());
        Assert.assertEquals(savedCustomerUser.getUuidId().getLeastSignificantBits(), userUpdateMsg.getIdLSB());

    }

    /**
     * 功能：验证用户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSendUserCredentialsRequestToCloud() throws Exception {
        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        UserCredentialsRequestMsg.Builder userCredentialsRequestMsgBuilder = UserCredentialsRequestMsg.newBuilder();
        userCredentialsRequestMsgBuilder.setUserIdMSB(tenantAdminUserId.getId().getMostSignificantBits());
        userCredentialsRequestMsgBuilder.setUserIdLSB(tenantAdminUserId.getId().getLeastSignificantBits());
        testAutoGeneratedCodeByProtobuf(userCredentialsRequestMsgBuilder);
        uplinkMsgBuilder.addUserCredentialsRequestMsg(userCredentialsRequestMsgBuilder.build());

        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.expectMessageAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
        Assert.assertTrue(edgeImitator.waitForResponses());
        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof UserCredentialsUpdateMsg);
        UserCredentialsUpdateMsg userCredentialsUpdateMsg = (UserCredentialsUpdateMsg) latestMessage;
        UserCredentials userCredentialsMsg = JacksonUtil.fromString(userCredentialsUpdateMsg.getEntity(), UserCredentials.class, true);
        Assert.assertNotNull(userCredentialsMsg);
        Assert.assertEquals(tenantAdminUserId, userCredentialsMsg.getUserId());
    }

    /**
     * 功能：发送或提交用户。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void sendUserCredentialsRequest() throws Exception {
        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        UserCredentialsRequestMsg.Builder userCredentialsRequestMsgBuilder = UserCredentialsRequestMsg.newBuilder();
        userCredentialsRequestMsgBuilder.setUserIdMSB(tenantAdminUserId.getId().getMostSignificantBits());
        userCredentialsRequestMsgBuilder.setUserIdLSB(tenantAdminUserId.getId().getLeastSignificantBits());
        testAutoGeneratedCodeByProtobuf(userCredentialsRequestMsgBuilder);
        uplinkMsgBuilder.addUserCredentialsRequestMsg(userCredentialsRequestMsgBuilder.build());

        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.expectMessageAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
        Assert.assertTrue(edgeImitator.waitForResponses());
        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof UserCredentialsUpdateMsg);
        UserCredentialsUpdateMsg userCredentialsUpdateMsg = (UserCredentialsUpdateMsg) latestMessage;
        UserCredentials userCredentialsMsg = JacksonUtil.fromString(userCredentialsUpdateMsg.getEntity(), UserCredentials.class, true);
        Assert.assertNotNull(userCredentialsMsg);
        Assert.assertEquals(tenantAdminUserId, userCredentialsMsg.getUserId());

        testAutoGeneratedCodeByProtobuf(userCredentialsUpdateMsg);
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`UserEdgeTest` 在 ThingsBoard Application 测试模块 中承担测试支撑类型职责，核心目的是验证 Application 模块的控制器、服务、Actor 或集成流程。
 * 2. 核心流程：准备输入和依赖，调用被测对象并断言结果。
 * 3. 关键依赖：主要依赖或协作对象包括JUnit、Mockito、Spring Test、DAO、缓存、Transport 和测试夹具。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
