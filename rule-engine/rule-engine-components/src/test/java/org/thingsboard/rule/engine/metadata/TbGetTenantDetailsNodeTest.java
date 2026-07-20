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
 * `TbGetTenantDetailsNodeTest` 测试类，用于验证 `TbGetTenantDetailsNode` 相关行为。
 */
@ExtendWith(MockitoExtension.class)
public class TbGetTenantDetailsNodeTest {

    /**
     * 设备常量，用于统一引用固定值。
     */
    private static final DeviceId DUMMY_DEVICE_ORIGINATOR = new DeviceId(UUID.randomUUID());
    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    @Mock
    private TbContext ctxMock;
    /**
     * 租户，提供当前类调用的业务操作。
     */
    @Mock
    private TenantService tenantServiceMock;
    /**
     * 节点实例，表示当前对象的对应属性。
     */
    private TbGetTenantDetailsNode node;
    /**
     * 配置，保存当前对象的配置选项。
     */
    private TbGetTenantDetailsNodeConfiguration config;
    /**
     * 节点实例，保存当前对象的配置选项。
     */
    private TbNodeConfiguration nodeConfiguration;
    /**
     * 消息，承载当前步骤需要处理的内容。
     */
    private TbMsg msg;
    /**
     * 租户对象，用于描述当前业务场景。
     */
    private Tenant tenant;

    /**
     * 功能：初始化当前测试或组件需要的对象。
     * 参数：无。
     * 返回：无。
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
        // THEN
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
     * 功能：验证 `givenSomeEntityDetailsAndFetchToMetadata_whenOnMsg_thenShouldTellSuccessAndFetchSomeToMetaData` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenSomeEntityDetailsAndFetchToMetadata_whenOnMsg_thenShouldTellSuccessAndFetchSomeToMetaData() {
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
     * 功能：验证 `givenNotPresentEntityDetailsAndFetchToData_whenOnMsg_thenShouldTellSuccessAndFetchNothingToData` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenNotPresentEntityDetailsAndFetchToData_whenOnMsg_thenShouldTellSuccessAndFetchNothingToData() {
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
     * 功能：验证 `givenDidNotFindTenant_whenOnMsg_thenShouldTellSuccessAndFetchNothingToData` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenDidNotFindTenant_whenOnMsg_thenShouldTellSuccessAndFetchNothingToData() {
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
     * 功能：验证 `givenNullDescriptionAndAddInfoEntityDetails_whenOnMsg_thenShouldTellSuccessAndFetchNothingToData` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenNullDescriptionAndAddInfoEntityDetails_whenOnMsg_thenShouldTellSuccessAndFetchNothingToData() {
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
     * 功能：验证 `givenOldConfig_whenUpgrade_thenShouldReturnTrueResultWithNewConfig` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenOldConfig_whenUpgrade_thenShouldReturnTrueResultWithNewConfig() throws Exception {
        var defaultConfig = new TbGetTenantDetailsNodeConfiguration().defaultConfiguration();
        var node = new TbGetTenantDetailsNode();
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
     * 返回：无。
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
     * 功能：执行 `mockFindTenant` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    private void mockFindTenant() {
        when(ctxMock.getTenantId()).thenReturn(tenant.getId());
        when(ctxMock.getTenantService()).thenReturn(tenantServiceMock);
        when(tenantServiceMock.findTenantByIdAsync(eq(tenant.getId()), eq(tenant.getId()))).thenReturn(Futures.immediateFuture(tenant));
    }

}
