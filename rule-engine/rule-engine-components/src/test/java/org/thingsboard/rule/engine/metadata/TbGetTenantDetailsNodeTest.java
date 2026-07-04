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
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.util.ContactBasedEntityDetails;
import org.thingsboard.rule.engine.util.TbMsgSource;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.tenant.TenantService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
 * 测试目标：验证 {@code TbGetTenantDetailsNodeTest} 覆盖的 元数据增强节点 行为，重点说明配置、消息和断言路径。
 * 所属生产节点/组件：{@code TbGetTenantDetailsNode}，用于守护对应 Rule Engine 组件的兼容性和边界条件。
 * Mock 依赖来源：字段上的 Mockito 注解、Mockito.mock/spy、setUp/before/init 中的 stub 和内存 fixture；测试不启动真实外部服务。
 * 被验证流程：准备 fixture，初始化节点或工具对象，触发被测调用，再断言输出、异常或 Mock 交互。
 * 存在原因：防止规则引擎组件在升级、消息处理、异步回调或数据映射场景中发生回归。
 */
@ExtendWith(MockitoExtension.class)
public class TbGetTenantDetailsNodeTest {

    /** 测试常量字段：{@code DUMMY_DEVICE_ORIGINATOR} 保存 {@code DeviceId} 测试数据或依赖，来源：由类加载时构造，生命周期覆盖整个测试类执行过程。 */
    private static final DeviceId DUMMY_DEVICE_ORIGINATOR = new DeviceId(UUID.randomUUID());
    /** Mock 依赖字段：{@code ctxMock} 保存 {@code TbContext} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private TbContext ctxMock;
    /** Mock 依赖字段：{@code tenantServiceMock} 保存 {@code TenantService} 测试数据或依赖，来源：由 Mockito 注解在测试实例初始化时创建，生命周期随单个测试实例或 runner 管理。 */
    @Mock
    private TenantService tenantServiceMock;
    /** 可变 fixture 字段：{@code node} 保存 {@code TbGetTenantDetailsNode} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private TbGetTenantDetailsNode node;
    /** 可变 fixture 字段：{@code config} 保存 {@code TbGetTenantDetailsNodeConfiguration} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private TbGetTenantDetailsNodeConfiguration config;
    /** 可变 fixture 字段：{@code nodeConfiguration} 保存 {@code TbNodeConfiguration} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private TbNodeConfiguration nodeConfiguration;
    /** 可变 fixture 字段：{@code msg} 保存 {@code TbMsg} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private TbMsg msg;
    /** 可变 fixture 字段：{@code tenant} 保存 {@code Tenant} 测试数据或依赖，来源：通常由 setUp/before/init 或测试体赋值，生命周期随单个测试实例。 */
    private Tenant tenant;

    /**
     * 生命周期方法：{@code setUp} 在 JUnit 用例前后准备或清理测试环境。
     * 输入数据：来自 Mockito 注解、类字段和内存 fixture；输出影响是初始化节点、Mock、执行器或清理资源。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    @BeforeEach
    public void setUp() {
        node = new TbGetTenantDetailsNode();
        config = new TbGetTenantDetailsNodeConfiguration().defaultConfiguration();
        nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        tenant = new Tenant();
        tenant.setId(new TenantId(UUID.randomUUID()));
        tenant.setTitle("Tenant title");
        tenant.setCountry("Tenant country");
        tenant.setCity("Tenant city");
        tenant.setState("Tenant state");
        tenant.setZip("123456");
        tenant.setAddress("Tenant address 1");
        tenant.setAddress2("Tenant address 2");
        tenant.setPhone("+123456789");
        tenant.setEmail("email@tenant.com");
        tenant.setAdditionalInfo(JacksonUtil.toJsonNode("{\"someProperty\":\"someValue\",\"description\":\"Tenant description\"}"));
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
        // THEN
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
        prepareMsgAndConfig(TbMsgSource.DATA, List.of(ContactBasedEntityDetails.values()));

        mockFindTenant();

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var actualMessageCaptor = ArgumentCaptor.forClass(TbMsg.class);

        verify(ctxMock, times(1)).tellSuccess(actualMessageCaptor.capture());
        verify(ctxMock, never()).tellFailure(any(), any());

        var expectedMsgData = "{\"dataKey1\":123,\"dataKey2\":\"dataValue2\"," +
                "\"tenant_id\":\"" + tenant.getId() + "\"," +
                "\"tenant_title\":\"" + tenant.getTitle() + "\"," +
                "\"tenant_country\":\"" + tenant.getCountry() + "\"," +
                "\"tenant_city\":\"" + tenant.getCity() + "\"," +
                "\"tenant_state\":\"" + tenant.getState() + "\"," +
                "\"tenant_zip\":\"" + tenant.getZip() + "\"," +
                "\"tenant_address\":\"" + tenant.getAddress() + "\"," +
                "\"tenant_address2\":\"" + tenant.getAddress2() + "\"," +
                "\"tenant_phone\":\"" + tenant.getPhone() + "\"," +
                "\"tenant_email\":\"" + tenant.getEmail() + "\"," +
                "\"tenant_additionalInfo\":\"" + tenant.getAdditionalInfo().get("description").asText() + "\"}";

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
        prepareMsgAndConfig(TbMsgSource.METADATA, List.of(ContactBasedEntityDetails.ID, ContactBasedEntityDetails.TITLE, ContactBasedEntityDetails.PHONE));

        mockFindTenant();

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var actualMessageCaptor = ArgumentCaptor.forClass(TbMsg.class);

        verify(ctxMock, times(1)).tellSuccess(actualMessageCaptor.capture());
        verify(ctxMock, never()).tellFailure(any(), any());

        var expectedMsgMetaData = new TbMsgMetaData(msg.getMetaData().getData());
        expectedMsgMetaData.putValue("tenant_id", tenant.getId().getId().toString());
        expectedMsgMetaData.putValue("tenant_title", tenant.getTitle());
        expectedMsgMetaData.putValue("tenant_phone", tenant.getPhone());

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
        tenant.setZip(null);
        tenant.setAddress(null);
        tenant.setAddress2(null);

        prepareMsgAndConfig(TbMsgSource.DATA, List.of(ContactBasedEntityDetails.ZIP, ContactBasedEntityDetails.ADDRESS, ContactBasedEntityDetails.ADDRESS2));

        mockFindTenant();

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
     * 测试方法：覆盖 {@code givenDidNotFindTenant_whenOnMsg_thenShouldTellSuccessAndFetchNothingToData} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void givenDidNotFindTenant_whenOnMsg_thenShouldTellSuccessAndFetchNothingToData() {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        // GIVEN
        prepareMsgAndConfig(TbMsgSource.DATA, List.of(ContactBasedEntityDetails.ZIP, ContactBasedEntityDetails.ADDRESS, ContactBasedEntityDetails.ADDRESS2));

        when(ctxMock.getTenantId()).thenReturn(tenant.getId());
        when(ctxMock.getTenantService()).thenReturn(tenantServiceMock);
        when(tenantServiceMock.findTenantByIdAsync(eq(tenant.getId()), eq(tenant.getId()))).thenReturn(Futures.immediateFuture(null));

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
        tenant.setAdditionalInfo(JacksonUtil.toJsonNode("{\"someProperty\":\"someValue\",\"description\":null}"));

        prepareMsgAndConfig(TbMsgSource.DATA, List.of(ContactBasedEntityDetails.ADDITIONAL_INFO));

        mockFindTenant();

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
     * 测试方法：覆盖 {@code givenOldConfig_whenUpgrade_thenShouldReturnTrueResultWithNewConfig} 场景，方法名中的 given/when/then 描述输入、触发动作和期望结果。
     * 输入数据：由方法体、参数化来源、类级 fixture 和 Mockito stub 共同构造，重点服务当前场景。
     * 期望输出：断言返回值、异常、转发关系、消息内容或 Mock 交互符合当前场景的 then 语义。
     * 调用时机：JUnit 在 before/setUp 完成后执行；若调用 init/onMsg/upgrade，则模拟规则节点初始化、消息处理或配置升级时机。
     * 外部系统：数据库、缓存、MQTT、Actor 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及；Rule Engine：通过 TbContext、TbMsg、节点初始化或节点处理方法模拟规则链流程。
     */
    @Test
    public void givenOldConfig_whenUpgrade_thenShouldReturnTrueResultWithNewConfig() throws Exception {
        // 流程说明：准备输入与 Mock，触发被测逻辑，再验证输出、异常或交互。
        var defaultConfig = new TbGetTenantDetailsNodeConfiguration().defaultConfiguration();
        var node = new TbGetTenantDetailsNode();
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
    private void prepareMsgAndConfig(TbMsgSource fetchTo, List<ContactBasedEntityDetails> detailsList) {
        config.setDetailsList(detailsList);
        config.setFetchTo(fetchTo);

        node.config = config;
        node.fetchTo = fetchTo;

        var msgMetaData = new TbMsgMetaData();
        msgMetaData.putValue("metaKey1", "metaValue1");
        msgMetaData.putValue("metaKey2", "metaValue2");

        var msgData = "{\"dataKey1\":123,\"dataKey2\":\"dataValue2\"}";

        msg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST, DUMMY_DEVICE_ORIGINATOR, msgMetaData, msgData);
    }

    /**
     * 辅助方法：{@code mockFindTenant} 复用本类测试的 fixture 构造、Mock 配置或断言逻辑。
     * 输入数据：来自调用方参数、类字段和内存对象；输出影响由调用它的测试方法验证。
     * 外部系统：数据库、缓存、MQTT、Actor、Rule Engine 测试本身不直接涉及，Mock 或被测生产逻辑可能涉及。
     */
    private void mockFindTenant() {
        when(ctxMock.getTenantId()).thenReturn(tenant.getId());
        when(ctxMock.getTenantService()).thenReturn(tenantServiceMock);
        when(tenantServiceMock.findTenantByIdAsync(eq(tenant.getId()), eq(tenant.getId()))).thenReturn(Futures.immediateFuture(tenant));
    }

}
/*
 * 本类总结：{@code TbGetTenantDetailsNodeTest} 为 {@code TbGetTenantDetailsNode} 的 元数据增强节点 测试提供中文注释，说明测试目标、fixture 生命周期、Mock 来源和断言流程。
 * 本文件中的数据库、缓存、MQTT、Actor 或完整 Rule Engine 运行时均不由测试本身直接启动；相关行为通过 Mock、内存 fixture 或被测生产逻辑间接覆盖。
 */
