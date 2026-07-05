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
package org.thingsboard.rule.engine.metadata;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.Futures;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.TestDbCallbackExecutor;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.util.ContactBasedEntityDetails;
import org.thingsboard.rule.engine.util.TbMsgSource;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.user.UserService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * `TbGetCustomerDetailsNodeTest` 测试类，用于验证 `TbGetCustomerDetailsNode` 相关行为。
 */
@ExtendWith(MockitoExtension.class)
public class TbGetCustomerDetailsNodeTest {

    /**
     * 设备常量，用于统一引用固定值。
     */
    private static final DeviceId DUMMY_DEVICE_ORIGINATOR = new DeviceId(UUID.randomUUID());
    /**
     * 租户ID常量，用于统一引用固定值。
     */
    private static final TenantId TENANT_ID = new TenantId(UUID.randomUUID());
    /**
     * 执行器常量，用于统一引用固定值。
     */
    private static final ListeningExecutor DB_EXECUTOR = new TestDbCallbackExecutor();
    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    @Mock
    private TbContext ctxMock;
    /**
     * 客户，提供当前类调用的业务操作。
     */
    @Mock
    private CustomerService customerServiceMock;
    /**
     * 设备，提供当前类调用的业务操作。
     */
    @Mock
    private DeviceService deviceServiceMock;
    /**
     * 资产集合，用于去重保存或快速判断对象是否存在。
     */
    @Mock
    private AssetService assetServiceMock;
    /**
     * 实体视图，提供当前类调用的业务操作。
     */
    @Mock
    private EntityViewService entityViewServiceMock;
    /**
     * 用户，提供当前类调用的业务操作。
     */
    @Mock
    private UserService userServiceMock;
    /**
     * 边缘节点，提供当前类调用的业务操作。
     */
    @Mock
    private EdgeService edgeServiceMock;
    /**
     * 节点实例，表示当前对象的对应属性。
     */
    private TbGetCustomerDetailsNode node;
    /**
     * 配置，保存当前对象的配置选项。
     */
    private TbGetCustomerDetailsNodeConfiguration config;
    /**
     * 节点实例，保存当前对象的配置选项。
     */
    private TbNodeConfiguration nodeConfiguration;
    /**
     * 消息，承载当前步骤需要处理的内容。
     */
    private TbMsg msg;
    /**
     * 客户对象，用于描述当前业务场景。
     */
    private Customer customer;

    /**
     * 功能：初始化当前测试或组件需要的对象。
     * 参数：无。
     * 返回：无。
     */
    @BeforeEach
    public void setUp() {
        node = new TbGetCustomerDetailsNode();
        config = new TbGetCustomerDetailsNodeConfiguration().defaultConfiguration();
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        customer = new Customer();
        customer.setId(new CustomerId(UUID.randomUUID()));
        customer.setTitle("Customer title");
        customer.setCountry("Customer country");
        customer.setCity("Customer city");
        customer.setState("Customer state");
        customer.setZip("123456");
        customer.setAddress("Customer address 1");
        customer.setAddress2("Customer address 2");
        customer.setPhone("+123456789");
        customer.setEmail("email@tenant.com");
        customer.setAdditionalInfo(JacksonUtil.toJsonNode("{\"someProperty\":\"someValue\",\"description\":\"Customer description\"}"));
    }

    /**
     * 功能：验证 `givenConfigWithNullFetchTo_whenInit_thenException` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenConfigWithNullFetchTo_whenInit_thenException() {
        // GIVEN
        config.setDetailsList(List.of(ContactBasedEntityDetails.ID));
        config.setFetchTo(null);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        // WHEN
        var exception = assertThrows(TbNodeException.class, () -> node.init(ctxMock, nodeConfiguration));

        // THEN
        assertThat(exception.getMessage()).isEqualTo("FetchTo option can't be null! Allowed values: " + Arrays.toString(TbMsgSource.values()));
        verify(ctxMock, never()).tellSuccess(any());
    }

    /**
     * 功能：验证 `givenDefaultConfig_whenInit_thenOK` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenDefaultConfig_whenInit_thenOK() {
        assertThat(config.getDetailsList()).isEqualTo(Collections.emptyList());
        assertThat(config.getFetchTo()).isEqualTo(TbMsgSource.DATA);
    }

    /**
     * 功能：验证 `givenCustomConfig_whenInit_thenOK` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenCustomConfig_whenInit_thenOK() throws TbNodeException {
        // GIVEN
        config.setDetailsList(List.of(ContactBasedEntityDetails.ID, ContactBasedEntityDetails.PHONE));
        config.setFetchTo(TbMsgSource.METADATA);
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));

        // WHEN
        node.init(ctxMock, nodeConfiguration);

        // THEN
        assertThat(node.config).isEqualTo(config);
        assertThat(config.getDetailsList()).isEqualTo(List.of(ContactBasedEntityDetails.ID, ContactBasedEntityDetails.PHONE));
        assertThat(config.getFetchTo()).isEqualTo(TbMsgSource.METADATA);
        assertThat(node.fetchTo).isEqualTo(TbMsgSource.METADATA);
    }

    /**
     * 功能：验证 `givenMsgDataIsNotAnJsonObjectAndFetchToData_whenOnMsg_thenException` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenMsgDataIsNotAnJsonObjectAndFetchToData_whenOnMsg_thenException() {
        // GIVEN
        node.fetchTo = TbMsgSource.DATA;
        msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DUMMY_DEVICE_ORIGINATOR, TbMsgMetaData.EMPTY, TbMsg.EMPTY_JSON_ARRAY);

        // WHEN
        var exception = assertThrows(IllegalArgumentException.class, () -> node.onMsg(ctxMock, msg));

        // THEN
        assertThat(exception.getMessage()).isEqualTo("Message body is not an object!");
        verify(ctxMock, never()).tellSuccess(any());
    }

    /**
     * 功能：验证 `givenAllEntityDetailsAndFetchToData_whenOnMsg_thenShouldTellSuccessAndFetchAllToData` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenAllEntityDetailsAndFetchToData_whenOnMsg_thenShouldTellSuccessAndFetchAllToData() {
        // GIVEN
        var device = new Device();
        device.setId(new DeviceId(UUID.randomUUID()));
        device.setCustomerId(customer.getId());

        prepareMsgAndConfig(TbMsgSource.DATA, List.of(ContactBasedEntityDetails.values()), device.getId());

        when(ctxMock.getDeviceService()).thenReturn(deviceServiceMock);
        when(deviceServiceMock.findDeviceByIdAsync(eq(TENANT_ID), eq(device.getId()))).thenReturn(Futures.immediateFuture(device));

        mockFindCustomer();

        when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var actualMessageCaptor = ArgumentCaptor.forClass(TbMsg.class);

        verify(ctxMock, times(1)).tellSuccess(actualMessageCaptor.capture());
        verify(ctxMock, never()).tellFailure(any(), any());

        var expectedMsgData = "{\"dataKey1\":123,\"dataKey2\":\"dataValue2\"," +
                "\"customer_id\":\"" + customer.getId() + "\"," +
                "\"customer_title\":\"" + customer.getTitle() + "\"," +
                "\"customer_country\":\"" + customer.getCountry() + "\"," +
                "\"customer_city\":\"" + customer.getCity() + "\"," +
                "\"customer_state\":\"" + customer.getState() + "\"," +
                "\"customer_zip\":\"" + customer.getZip() + "\"," +
                "\"customer_address\":\"" + customer.getAddress() + "\"," +
                "\"customer_address2\":\"" + customer.getAddress2() + "\"," +
                "\"customer_phone\":\"" + customer.getPhone() + "\"," +
                "\"customer_email\":\"" + customer.getEmail() + "\"," +
                "\"customer_additionalInfo\":\"" + customer.getAdditionalInfo().get("description").asText() + "\"}";

        assertThat(actualMessageCaptor.getValue().getData()).isEqualTo(expectedMsgData);
        assertThat(actualMessageCaptor.getValue().getMetaData()).isEqualTo(msg.getMetaData());
    }

    /**
     * 功能：验证 `givenSomeEntityDetailsAndFetchToMetadata_whenOnMsg_thenShouldTellSuccessAndFetchSomeToMetaData` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenSomeEntityDetailsAndFetchToMetadata_whenOnMsg_thenShouldTellSuccessAndFetchSomeToMetaData() {
        // GIVEN
        var asset = new Asset();
        asset.setId(new AssetId(UUID.randomUUID()));
        asset.setCustomerId(customer.getId());

        prepareMsgAndConfig(TbMsgSource.METADATA, List.of(ContactBasedEntityDetails.ID, ContactBasedEntityDetails.TITLE, ContactBasedEntityDetails.PHONE), asset.getId());

        when(ctxMock.getAssetService()).thenReturn(assetServiceMock);
        when(assetServiceMock.findAssetByIdAsync(eq(TENANT_ID), eq(asset.getId()))).thenReturn(Futures.immediateFuture(asset));

        mockFindCustomer();

        when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var actualMessageCaptor = ArgumentCaptor.forClass(TbMsg.class);

        verify(ctxMock, times(1)).tellSuccess(actualMessageCaptor.capture());
        verify(ctxMock, never()).tellFailure(any(), any());

        var expectedMsgMetaData = new TbMsgMetaData(msg.getMetaData().getData());
        expectedMsgMetaData.putValue("customer_id", customer.getId().getId().toString());
        expectedMsgMetaData.putValue("customer_title", customer.getTitle());
        expectedMsgMetaData.putValue("customer_phone", customer.getPhone());

        assertThat(actualMessageCaptor.getValue().getData()).isEqualTo(msg.getData());
        assertThat(actualMessageCaptor.getValue().getMetaData()).isEqualTo(expectedMsgMetaData);
    }

    /**
     * 功能：验证 `givenNotPresentEntityDetailsAndFetchToData_whenOnMsg_thenShouldTellSuccessAndFetchNothingToData` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenNotPresentEntityDetailsAndFetchToData_whenOnMsg_thenShouldTellSuccessAndFetchNothingToData() {
        // GIVEN
        customer.setZip(null);
        customer.setAddress(null);
        customer.setAddress2(null);

        var user = new User();
        user.setId(new UserId(UUID.randomUUID()));
        user.setCustomerId(customer.getId());

        prepareMsgAndConfig(TbMsgSource.DATA, List.of(ContactBasedEntityDetails.ZIP, ContactBasedEntityDetails.ADDRESS, ContactBasedEntityDetails.ADDRESS2), user.getId());

        when(ctxMock.getUserService()).thenReturn(userServiceMock);
        when(userServiceMock.findUserByIdAsync(eq(TENANT_ID), eq(user.getId()))).thenReturn(Futures.immediateFuture(user));

        when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);

        mockFindCustomer();

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var actualMessageCaptor = ArgumentCaptor.forClass(TbMsg.class);

        verify(ctxMock, times(1)).tellSuccess(actualMessageCaptor.capture());
        verify(ctxMock, never()).tellFailure(any(), any());

        assertThat(actualMessageCaptor.getValue().getData()).isEqualTo(msg.getData());
        assertThat(actualMessageCaptor.getValue().getMetaData()).isEqualTo(msg.getMetaData());
    }

    /**
     * 功能：验证 `givenDidNotFindCustomer_whenOnMsg_thenShouldTellSuccessAndFetchNothingToData` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenDidNotFindCustomer_whenOnMsg_thenShouldTellSuccessAndFetchNothingToData() {
        // GIVEN
        var edge = new Edge();
        edge.setId(new EdgeId(UUID.randomUUID()));
        edge.setCustomerId(customer.getId());

        prepareMsgAndConfig(TbMsgSource.DATA, List.of(ContactBasedEntityDetails.ZIP, ContactBasedEntityDetails.ADDRESS, ContactBasedEntityDetails.ADDRESS2), edge.getId());

        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);

        when(ctxMock.getEdgeService()).thenReturn(edgeServiceMock);
        when(edgeServiceMock.findEdgeByIdAsync(eq(TENANT_ID), eq(edge.getId()))).thenReturn(Futures.immediateFuture(edge));

        when(ctxMock.getCustomerService()).thenReturn(customerServiceMock);
        when(customerServiceMock.findCustomerByIdAsync(eq(TENANT_ID), eq(customer.getId()))).thenReturn(Futures.immediateFuture(null));

        when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var actualMessageCaptor = ArgumentCaptor.forClass(TbMsg.class);

        verify(ctxMock, times(1)).tellSuccess(actualMessageCaptor.capture());
        verify(ctxMock, never()).tellFailure(any(), any());

        assertThat(actualMessageCaptor.getValue().getData()).isEqualTo(msg.getData());
        assertThat(actualMessageCaptor.getValue().getMetaData()).isEqualTo(msg.getMetaData());
    }

    /**
     * 功能：验证 `givenDidNotFindOriginator_whenOnMsg_thenShouldTellSuccessAndFetchNothingToData` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenDidNotFindOriginator_whenOnMsg_thenShouldTellSuccessAndFetchNothingToData() {
        // GIVEN
        var edge = new Edge();
        edge.setId(new EdgeId(UUID.randomUUID()));
        edge.setCustomerId(customer.getId());

        prepareMsgAndConfig(TbMsgSource.DATA, List.of(ContactBasedEntityDetails.ZIP, ContactBasedEntityDetails.ADDRESS, ContactBasedEntityDetails.ADDRESS2), edge.getId());

        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);

        when(ctxMock.getEdgeService()).thenReturn(edgeServiceMock);
        when(edgeServiceMock.findEdgeByIdAsync(eq(TENANT_ID), eq(edge.getId()))).thenReturn(Futures.immediateFuture(null));

        when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var actualMessageCaptor = ArgumentCaptor.forClass(TbMsg.class);

        verify(ctxMock, times(1)).tellSuccess(actualMessageCaptor.capture());
        verify(ctxMock, never()).tellFailure(any(), any());

        assertThat(actualMessageCaptor.getValue().getData()).isEqualTo(msg.getData());
        assertThat(actualMessageCaptor.getValue().getMetaData()).isEqualTo(msg.getMetaData());
    }

    /**
     * 功能：验证 `givenOriginatorNotAssignedToCustomer_whenOnMsg_thenShouldTellFailureAndFetchNothingToData` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenOriginatorNotAssignedToCustomer_whenOnMsg_thenShouldTellFailureAndFetchNothingToData() {
        // GIVEN
        var device = new Device();
        device.setId(new DeviceId(UUID.randomUUID()));
        device.setName("Thermostat");

        prepareMsgAndConfig(TbMsgSource.DATA, List.of(ContactBasedEntityDetails.ZIP, ContactBasedEntityDetails.ADDRESS, ContactBasedEntityDetails.ADDRESS2), device.getId());

        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);

        when(ctxMock.getDeviceService()).thenReturn(deviceServiceMock);
        when(deviceServiceMock.findDeviceByIdAsync(eq(TENANT_ID), eq(device.getId()))).thenReturn(Futures.immediateFuture(device));

        when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var actualMessageCaptor = ArgumentCaptor.forClass(TbMsg.class);
        var actualExceptionCaptor = ArgumentCaptor.forClass(Exception.class);

        verify(ctxMock, times(1)).tellFailure(actualMessageCaptor.capture(), actualExceptionCaptor.capture());
        verify(ctxMock, never()).tellSuccess(any());

        var actualMsg = actualMessageCaptor.getValue();
        var actualException = actualExceptionCaptor.getValue();

        assertThat(actualMsg.getData()).isEqualTo(msg.getData());
        assertThat(actualMsg.getMetaData()).isEqualTo(msg.getMetaData());

        assertThat(actualException).isInstanceOf(RuntimeException.class);
        assertThat(actualException.getMessage()).isEqualTo("Device with name 'Thermostat' is not assigned to Customer!");
    }

    /**
     * 功能：验证 `givenNullDescriptionAndAddInfoEntityDetails_whenOnMsg_thenShouldTellSuccessAndFetchNothingToData` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenNullDescriptionAndAddInfoEntityDetails_whenOnMsg_thenShouldTellSuccessAndFetchNothingToData() {
        // GIVEN
        customer.setAdditionalInfo(JacksonUtil.toJsonNode("{\"someProperty\":\"someValue\",\"description\":null}"));

        var device = new Device();
        device.setId(new DeviceId(UUID.randomUUID()));
        device.setCustomerId(customer.getId());

        prepareMsgAndConfig(TbMsgSource.DATA, List.of(ContactBasedEntityDetails.ADDITIONAL_INFO), device.getId());

        when(ctxMock.getDeviceService()).thenReturn(deviceServiceMock);
        when(deviceServiceMock.findDeviceByIdAsync(eq(TENANT_ID), eq(device.getId()))).thenReturn(Futures.immediateFuture(device));

        when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);

        mockFindCustomer();

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var actualMessageCaptor = ArgumentCaptor.forClass(TbMsg.class);

        verify(ctxMock, times(1)).tellSuccess(actualMessageCaptor.capture());
        verify(ctxMock, never()).tellFailure(any(), any());

        assertThat(actualMessageCaptor.getValue().getData()).isEqualTo(msg.getData());
        assertThat(actualMessageCaptor.getValue().getMetaData()).isEqualTo(msg.getMetaData());
    }

    /**
     * 功能：验证 `givenUnsupportedEntityType_whenOnMsg_thenShouldTellFailureAndFetchNothingToMetaData` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenUnsupportedEntityType_whenOnMsg_thenShouldTellFailureAndFetchNothingToMetaData() {
        // GIVEN
        var dashboard = new Dashboard();
        dashboard.setId(new DashboardId(UUID.randomUUID()));

        prepareMsgAndConfig(TbMsgSource.METADATA, List.of(ContactBasedEntityDetails.STATE), dashboard.getId());

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var actualMessageCaptor = ArgumentCaptor.forClass(TbMsg.class);
        var actualExceptionCaptor = ArgumentCaptor.forClass(Exception.class);

        verify(ctxMock, times(1)).tellFailure(actualMessageCaptor.capture(), actualExceptionCaptor.capture());
        verify(ctxMock, never()).tellSuccess(any());

        var actualMsg = actualMessageCaptor.getValue();
        var actualException = actualExceptionCaptor.getValue();

        assertThat(actualMsg.getData()).isEqualTo(msg.getData());
        assertThat(actualMsg.getMetaData()).isEqualTo(msg.getMetaData());

        assertThat(actualException).isInstanceOf(NoSuchElementException.class);
        assertThat(actualException.getMessage()).isEqualTo("Entity with entityType 'DASHBOARD' is not supported.");
    }

    /**
     * 功能：验证 `givenOldConfig_whenUpgrade_thenShouldReturnTrueResultWithNewConfig` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenOldConfig_whenUpgrade_thenShouldReturnTrueResultWithNewConfig() throws Exception {
        var defaultConfig = new TbGetCustomerDetailsNodeConfiguration().defaultConfiguration();
        var node = new TbGetCustomerDetailsNode();
        String oldConfig = "{\"detailsList\":[],\"addToMetadata\":false}";
        JsonNode configJson = JacksonUtil.toJsonNode(oldConfig);
        TbPair<Boolean, JsonNode> upgrade = node.upgrade(0, configJson);
        Assertions.assertTrue(upgrade.getFirst());
        Assertions.assertEquals(defaultConfig, JacksonUtil.treeToValue(upgrade.getSecond(), defaultConfig.getClass()));
    }

    /**
     * 功能：执行 `prepareMsgAndConfig` 对应的处理。
     * 参数：
     * - `fetchTo`：`fetchTo` 参数。
     * - `detailsList`：数据列表。
     * - `originator`：`originator` 参数。
     * 返回：无。
     */
    private void prepareMsgAndConfig(TbMsgSource fetchTo, List<ContactBasedEntityDetails> detailsList, EntityId originator) {
        config.setDetailsList(detailsList);
        config.setFetchTo(fetchTo);

        node.config = config;
        node.fetchTo = fetchTo;

        var msgMetaData = new TbMsgMetaData();
        msgMetaData.putValue("metaKey1", "metaValue1");
        msgMetaData.putValue("metaKey2", "metaValue2");

        var msgData = "{\"dataKey1\":123,\"dataKey2\":\"dataValue2\"}";

        msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, originator, msgMetaData, msgData);
    }

    /**
     * 功能：执行 `mockFindCustomer` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    private void mockFindCustomer() {
        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(ctxMock.getCustomerService()).thenReturn(customerServiceMock);
        when(customerServiceMock.findCustomerByIdAsync(eq(TENANT_ID), eq(customer.getId()))).thenReturn(Futures.immediateFuture(customer));
    }

}
/*
 * 本类总结：{@code TbGetCustomerDetailsNodeTest} 为 {@code TbGetCustomerDetailsNode} 的 元数据增强节点 测试提供中文注释，说明测试目标、fixture 生命周期、Mock 来源和断言流程。
 * 本文件中的数据库、缓存、MQTT、Actor 或完整 Rule Engine 运行时均不由测试本身直接启动；相关行为通过 Mock、内存 fixture 或被测生产逻辑间接覆盖。
 */
