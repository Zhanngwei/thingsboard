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
 * 测试目标：验证 {@code TbGetCustomerDetailsNodeTest} 覆盖的 元数据增强节点 行为，重点说明配置、消息和断言路径。
 * 所属生产节点/组件：{@code TbGetCustomerDetailsNode}，用于守护对应 Rule Engine 组件的兼容性和边界条件。
 * Mock 依赖来源：字段上的 Mockito 注解、Mockito.mock/spy、setUp/before/init 中的 stub 和内存 fixture；测试不启动真实外部服务。
 * 被验证流程：准备 fixture，初始化节点或工具对象，触发被测调用，再断言输出、异常或 Mock 交互。
 * 存在原因：防止规则引擎组件在升级、消息处理、异步回调或数据映射场景中发生回归。
 */
@ExtendWith(MockitoExtension.class)
public class TbGetCustomerDetailsNodeTest {

    /** 测试常量字段：{@code DUMMY_DEVICE_ORIGINATOR} 保存 {@code DeviceId} 测试数据或依赖，来源：由类加载时构造，生命周期覆盖整个测试类执行过程。 */
    private static final DeviceId DUMMY_DEVICE_ORIGINATOR = new DeviceId(UUID.randomUUID());
    /** 测试常量字段：{@code TENANT_ID} 保存 {@code TenantId} 测试数据或依赖，来源：由类加载时构造，生命周期覆盖整个测试类执行过程。 */
    private static final TenantId TENANT_ID = new TenantId(UUID.randomUUID());
    /** 测试常量字段：{@code DB_EXECUTOR} 保存 {@code ListeningExecutor} 测试数据或依赖，来源：由类加载时构造，生命周期覆盖整个测试类执行过程。 */
    private static final ListeningExecutor DB_EXECUTOR = new TestDbCallbackExecutor();
    /** Mock 依赖字段：{@code ctxMock} 保存 {@code TbContext} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private TbContext ctxMock;
    /** Mock 依赖字段：{@code customerServiceMock} 保存 {@code CustomerService} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private CustomerService customerServiceMock;
    /** Mock 依赖字段：{@code deviceServiceMock} 保存 {@code DeviceService} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private DeviceService deviceServiceMock;
    /** Mock 依赖字段：{@code assetServiceMock} 保存 {@code AssetService} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private AssetService assetServiceMock;
    /** Mock 依赖字段：{@code entityViewServiceMock} 保存 {@code EntityViewService} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private EntityViewService entityViewServiceMock;
    /** Mock 依赖字段：{@code userServiceMock} 保存 {@code UserService} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private UserService userServiceMock;
    /** Mock 依赖字段：{@code edgeServiceMock} 保存 {@code EdgeService} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private EdgeService edgeServiceMock;
    /** 可变 fixture 字段：{@code node} 保存 {@code TbGetCustomerDetailsNode} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private TbGetCustomerDetailsNode node;
    /** 可变 fixture 字段：{@code config} 保存 {@code TbGetCustomerDetailsNodeConfiguration} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private TbGetCustomerDetailsNodeConfiguration config;
    /** 可变 fixture 字段：{@code nodeConfiguration} 保存 {@code TbNodeConfiguration} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private TbNodeConfiguration nodeConfiguration;
    /** 可变 fixture 字段：{@code msg} 保存 {@code TbMsg} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private TbMsg msg;
    /** 可变 fixture 字段：{@code customer} 保存 {@code Customer} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private Customer customer;

    /**
     * 生命周期方法：{@code setUp} 在 JUnit 用例前后准备或清理测试环境。
     * 输入数据：来自 Mockito 注解、类字段和内存 fixture；输出影响是初始化节点、Mock、执行器或清理资源。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
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
     * 测试方法：覆盖 {@code givenConfigWithNullFetchTo_whenInit_thenException} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void givenConfigWithNullFetchTo_whenInit_thenException() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
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
     * 测试方法：覆盖 {@code givenDefaultConfig_whenInit_thenOK} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void givenDefaultConfig_whenInit_thenOK() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        assertThat(config.getDetailsList()).isEqualTo(Collections.emptyList());
        assertThat(config.getFetchTo()).isEqualTo(TbMsgSource.DATA);
    }

    /**
     * 测试方法：覆盖 {@code givenCustomConfig_whenInit_thenOK} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void givenCustomConfig_whenInit_thenOK() throws TbNodeException {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
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
     * 测试方法：覆盖 {@code givenMsgDataIsNotAnJsonObjectAndFetchToData_whenOnMsg_thenException} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void givenMsgDataIsNotAnJsonObjectAndFetchToData_whenOnMsg_thenException() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
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
     * 测试方法：覆盖 {@code givenAllEntityDetailsAndFetchToData_whenOnMsg_thenShouldTellSuccessAndFetchAllToData} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void givenAllEntityDetailsAndFetchToData_whenOnMsg_thenShouldTellSuccessAndFetchAllToData() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
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
     * 测试方法：覆盖 {@code givenSomeEntityDetailsAndFetchToMetadata_whenOnMsg_thenShouldTellSuccessAndFetchSomeToMetaData} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void givenSomeEntityDetailsAndFetchToMetadata_whenOnMsg_thenShouldTellSuccessAndFetchSomeToMetaData() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
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
     * 测试方法：覆盖 {@code givenNotPresentEntityDetailsAndFetchToData_whenOnMsg_thenShouldTellSuccessAndFetchNothingToData} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void givenNotPresentEntityDetailsAndFetchToData_whenOnMsg_thenShouldTellSuccessAndFetchNothingToData() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
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
     * 测试方法：覆盖 {@code givenDidNotFindCustomer_whenOnMsg_thenShouldTellSuccessAndFetchNothingToData} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void givenDidNotFindCustomer_whenOnMsg_thenShouldTellSuccessAndFetchNothingToData() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
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
     * 测试方法：覆盖 {@code givenDidNotFindOriginator_whenOnMsg_thenShouldTellSuccessAndFetchNothingToData} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void givenDidNotFindOriginator_whenOnMsg_thenShouldTellSuccessAndFetchNothingToData() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
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
     * 测试方法：覆盖 {@code givenOriginatorNotAssignedToCustomer_whenOnMsg_thenShouldTellFailureAndFetchNothingToData} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void givenOriginatorNotAssignedToCustomer_whenOnMsg_thenShouldTellFailureAndFetchNothingToData() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
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
     * 测试方法：覆盖 {@code givenNullDescriptionAndAddInfoEntityDetails_whenOnMsg_thenShouldTellSuccessAndFetchNothingToData} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void givenNullDescriptionAndAddInfoEntityDetails_whenOnMsg_thenShouldTellSuccessAndFetchNothingToData() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
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
     * 测试方法：覆盖 {@code givenUnsupportedEntityType_whenOnMsg_thenShouldTellFailureAndFetchNothingToMetaData} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void givenUnsupportedEntityType_whenOnMsg_thenShouldTellFailureAndFetchNothingToMetaData() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
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
     * 测试方法：覆盖 {@code givenOldConfig_whenUpgrade_thenShouldReturnTrueResultWithNewConfig} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void givenOldConfig_whenUpgrade_thenShouldReturnTrueResultWithNewConfig() throws Exception {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        var defaultConfig = new TbGetCustomerDetailsNodeConfiguration().defaultConfiguration();
        var node = new TbGetCustomerDetailsNode();
        String oldConfig = "{\"detailsList\":[],\"addToMetadata\":false}";
        JsonNode configJson = JacksonUtil.toJsonNode(oldConfig);
        TbPair<Boolean, JsonNode> upgrade = node.upgrade(0, configJson);
        Assertions.assertTrue(upgrade.getFirst());
        Assertions.assertEquals(defaultConfig, JacksonUtil.treeToValue(upgrade.getSecond(), defaultConfig.getClass()));
    }

    /**
     * 辅助方法：{@code prepareMsgAndConfig} 复用本类测试的 fixture 构造、Mock 配置或断言逻辑。
     * 输入数据：来自调用方参数、类字段和内存对象；输出影响由调用它的测试方法验证。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
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
     * 辅助方法：{@code mockFindCustomer} 复用本类测试的 fixture 构造、Mock 配置或断言逻辑。
     * 输入数据：来自调用方参数、类字段和内存对象；输出影响由调用它的测试方法验证。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
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
