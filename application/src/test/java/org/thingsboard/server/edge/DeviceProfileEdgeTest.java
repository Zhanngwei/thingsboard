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
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.device.data.PowerMode;
import org.thingsboard.server.common.data.device.data.PowerSavingConfiguration;
import org.thingsboard.server.common.data.device.profile.CoapDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultCoapDeviceTypeConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.device.profile.DeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.DisabledDeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.ProtoTransportPayloadConfiguration;
import org.thingsboard.server.common.data.device.profile.SnmpDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.lwm2m.OtherConfiguration;
import org.thingsboard.server.common.data.device.profile.lwm2m.TelemetryMappingConfiguration;
import org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap.AbstractLwM2MBootstrapServerCredential;
import org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap.LwM2MBootstrapServerCredential;
import org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap.NoSecLwM2MBootstrapServerCredential;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.kv.DataType;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.transport.snmp.SnmpMapping;
import org.thingsboard.server.common.data.transport.snmp.config.SnmpCommunicationConfig;
import org.thingsboard.server.common.data.transport.snmp.config.impl.TelemetryQueryingSnmpCommunicationConfig;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.edge.v1.DeviceProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.gen.edge.v1.UplinkResponseMsg;
import org.thingsboard.server.transport.AbstractTransportIntegrationTest;
import org.thingsboard.server.transport.lwm2m.AbstractLwM2MIntegrationTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 中文说明：
 * 1. 类目的：`DeviceProfileEdgeTest` 是ThingsBoard Application 测试模块中的测试支撑类型，用于验证 Application 模块的控制器、服务、Actor 或集成流程。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括JUnit、Mockito、Spring Test、DAO、缓存、Transport 和测试夹具。
 * 4. 生命周期：由测试框架按测试类和测试方法管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Test Fixture。
 */
@DaoSqlTest
public class DeviceProfileEdgeTest extends AbstractEdgeTest {

    /**
     * 功能：验证设备相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testDeviceProfiles() throws Exception {
        RuleChainId thermostatsRuleChainId = createEdgeRuleChainAndAssignToEdge("Thermostats Rule Chain");

        // create device profile
        DeviceProfile deviceProfile = this.createDeviceProfile("ONE_MORE_DEVICE_PROFILE", null);
        deviceProfile.setDefaultEdgeRuleChainId(thermostatsRuleChainId);
        extendDeviceProfileData(deviceProfile);
        edgeImitator.expectMessageAmount(1);
        deviceProfile = doPost("/api/deviceProfile", deviceProfile, DeviceProfile.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DeviceProfileUpdateMsg);
        DeviceProfileUpdateMsg deviceProfileUpdateMsg = (DeviceProfileUpdateMsg) latestMessage;
        DeviceProfile deviceProfileMsg = JacksonUtil.fromString(deviceProfileUpdateMsg.getEntity(), DeviceProfile.class, true);
        Assert.assertNotNull(deviceProfileMsg);
        Assert.assertEquals(deviceProfile, deviceProfileMsg);
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, deviceProfileUpdateMsg.getMsgType());

        // update device profile
        edgeImitator.expectMessageAmount(1);
        OtaPackageInfo firmwareOtaPackageInfo = saveOtaPackageInfo(deviceProfile.getId(), OtaPackageType.FIRMWARE);
        Assert.assertTrue(edgeImitator.waitForMessages());

        edgeImitator.expectMessageAmount(1);
        OtaPackageInfo softwareOtaPackageInfo = saveOtaPackageInfo(deviceProfile.getId(), OtaPackageType.SOFTWARE);
        Assert.assertTrue(edgeImitator.waitForMessages());

        DashboardId thermostatsDashboardId = createDashboardAndAssignToEdge("Thermostats Dashboard");

        deviceProfile.setFirmwareId(firmwareOtaPackageInfo.getId());
        deviceProfile.setSoftwareId(softwareOtaPackageInfo.getId());
        deviceProfile.setDefaultDashboardId(thermostatsDashboardId);
        edgeImitator.expectMessageAmount(1);
        deviceProfile = doPost("/api/deviceProfile", deviceProfile, DeviceProfile.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DeviceProfileUpdateMsg);
        deviceProfileUpdateMsg = (DeviceProfileUpdateMsg) latestMessage;
        deviceProfileMsg = JacksonUtil.fromString(deviceProfileUpdateMsg.getEntity(), DeviceProfile.class, true);
        Assert.assertNotNull(deviceProfileMsg);
        Assert.assertEquals(deviceProfile, deviceProfileMsg);

        // delete profile
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/deviceProfile/" + deviceProfile.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DeviceProfileUpdateMsg);
        deviceProfileUpdateMsg = (DeviceProfileUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, deviceProfileUpdateMsg.getMsgType());
        Assert.assertEquals(deviceProfile.getUuidId().getMostSignificantBits(), deviceProfileUpdateMsg.getIdMSB());
        Assert.assertEquals(deviceProfile.getUuidId().getLeastSignificantBits(), deviceProfileUpdateMsg.getIdLSB());

        unAssignFromEdgeAndDeleteRuleChain(thermostatsRuleChainId);
        unAssignFromEdgeAndDeleteDashboard(thermostatsDashboardId);
    }

    /**
     * 功能：验证设备相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testDeviceProfiles_snmp() throws Exception {
        DeviceProfile deviceProfile = createDeviceProfileAndDoBasicAssert("SNMP", createSnmpDeviceProfileTransportConfiguration());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DeviceProfileUpdateMsg);
        DeviceProfileUpdateMsg deviceProfileUpdateMsg = (DeviceProfileUpdateMsg) latestMessage;
        DeviceProfile deviceProfileMsg = JacksonUtil.fromString(deviceProfileUpdateMsg.getEntity(), DeviceProfile.class, true);
        Assert.assertNotNull(deviceProfileMsg);
        Assert.assertEquals(deviceProfile, deviceProfileMsg);
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, deviceProfileUpdateMsg.getMsgType());
        Assert.assertEquals(DeviceTransportType.SNMP, deviceProfileMsg.getTransportType());

        DeviceProfileData deviceProfileData = deviceProfileMsg.getProfileData();

        Assert.assertTrue(deviceProfileData.getTransportConfiguration() instanceof SnmpDeviceProfileTransportConfiguration);
        SnmpDeviceProfileTransportConfiguration transportConfiguration =
                (SnmpDeviceProfileTransportConfiguration) deviceProfileData.getTransportConfiguration();
        Assert.assertEquals(Integer.valueOf(1000), transportConfiguration.getTimeoutMs());
        Assert.assertEquals(Integer.valueOf(3), transportConfiguration.getRetries());

        Assert.assertFalse(transportConfiguration.getCommunicationConfigs().isEmpty());
        SnmpCommunicationConfig communicationConfig = transportConfiguration.getCommunicationConfigs().get(0);
        Assert.assertTrue(communicationConfig instanceof TelemetryQueryingSnmpCommunicationConfig);
        TelemetryQueryingSnmpCommunicationConfig snmpCommunicationConfig =
                (TelemetryQueryingSnmpCommunicationConfig) communicationConfig;

        Assert.assertEquals(Long.valueOf(500L), snmpCommunicationConfig.getQueryingFrequencyMs());
        Assert.assertFalse(snmpCommunicationConfig.getMappings().isEmpty());

        SnmpMapping snmpMapping = snmpCommunicationConfig.getMappings().get(0);
        Assert.assertEquals("temperature", snmpMapping.getKey());
        Assert.assertEquals("1.3.3.5.6.7.8.9.1", snmpMapping.getOid());
        Assert.assertEquals(DataType.DOUBLE, snmpMapping.getDataType());

        removeDeviceProfileAndDoBasicAssert(deviceProfile);
    }

    /**
     * 功能：验证设备相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testDeviceProfiles_lwm2m() throws Exception {
        DeviceProfile deviceProfile = createDeviceProfileAndDoBasicAssert("LWM2M", createLwm2mDeviceProfileTransportConfiguration());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DeviceProfileUpdateMsg);
        DeviceProfileUpdateMsg deviceProfileUpdateMsg = (DeviceProfileUpdateMsg) latestMessage;
        DeviceProfile deviceProfileMsg = JacksonUtil.fromString(deviceProfileUpdateMsg.getEntity(), DeviceProfile.class, true);
        Assert.assertNotNull(deviceProfileMsg);
        Assert.assertEquals(deviceProfile, deviceProfileMsg);
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, deviceProfileUpdateMsg.getMsgType());
        Assert.assertEquals(DeviceTransportType.LWM2M, deviceProfileMsg.getTransportType());

        DeviceProfileData deviceProfileData = deviceProfileMsg.getProfileData();

        Assert.assertTrue(deviceProfileData.getTransportConfiguration() instanceof Lwm2mDeviceProfileTransportConfiguration);
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration =
                (Lwm2mDeviceProfileTransportConfiguration) deviceProfileData.getTransportConfiguration();

        OtherConfiguration clientLwM2mSettings = transportConfiguration.getClientLwM2mSettings();
        Assert.assertEquals(PowerMode.DRX, clientLwM2mSettings.getPowerMode());
        Assert.assertEquals(Integer.valueOf(1), clientLwM2mSettings.getFwUpdateStrategy());
        Assert.assertEquals(Integer.valueOf(1), clientLwM2mSettings.getSwUpdateStrategy());
        Assert.assertEquals(Integer.valueOf(1), clientLwM2mSettings.getClientOnlyObserveAfterConnect());

        Assert.assertTrue(transportConfiguration.isBootstrapServerUpdateEnable());

        Assert.assertFalse(transportConfiguration.getBootstrap().isEmpty());
        LwM2MBootstrapServerCredential lwM2MBootstrapServerCredential = transportConfiguration.getBootstrap().get(0);
        Assert.assertTrue(lwM2MBootstrapServerCredential instanceof NoSecLwM2MBootstrapServerCredential);
        NoSecLwM2MBootstrapServerCredential noSecLwM2MBootstrapServerCredential = (NoSecLwM2MBootstrapServerCredential) lwM2MBootstrapServerCredential;

        Assert.assertEquals("PUBLIC_KEY", noSecLwM2MBootstrapServerCredential.getServerPublicKey());
        Assert.assertEquals(Integer.valueOf(123), noSecLwM2MBootstrapServerCredential.getShortServerId());
        Assert.assertTrue(noSecLwM2MBootstrapServerCredential.isBootstrapServerIs());
        Assert.assertEquals("localhost", noSecLwM2MBootstrapServerCredential.getHost());
        Assert.assertEquals(Integer.valueOf(5687), noSecLwM2MBootstrapServerCredential.getPort());

        TelemetryMappingConfiguration observeAttr = transportConfiguration.getObserveAttr();
        Assert.assertEquals("batteryLevel", observeAttr.getKeyName().get("/3_1.0/0/9"));
        Assert.assertTrue(observeAttr.getObserve().isEmpty());
        Assert.assertTrue(observeAttr.getAttribute().isEmpty());
        Assert.assertFalse(observeAttr.getTelemetry().isEmpty());
        Assert.assertTrue(observeAttr.getTelemetry().contains("/3_1.0/0/9"));
        Assert.assertTrue(observeAttr.getAttributeLwm2m().isEmpty());

        removeDeviceProfileAndDoBasicAssert(deviceProfile);
    }

    /**
     * 功能：验证设备相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testDeviceProfiles_coap() throws Exception {
        DeviceProfile deviceProfile = createDeviceProfileAndDoBasicAssert("COAP", createCoapDeviceProfileTransportConfiguration());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DeviceProfileUpdateMsg);
        DeviceProfileUpdateMsg deviceProfileUpdateMsg = (DeviceProfileUpdateMsg) latestMessage;
        DeviceProfile deviceProfileMsg = JacksonUtil.fromString(deviceProfileUpdateMsg.getEntity(), DeviceProfile.class, true);
        Assert.assertNotNull(deviceProfileMsg);
        Assert.assertEquals(deviceProfile, deviceProfileMsg);
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, deviceProfileUpdateMsg.getMsgType());
        Assert.assertEquals(DeviceTransportType.COAP, deviceProfileMsg.getTransportType());

        DeviceProfileData deviceProfileData = deviceProfileMsg.getProfileData();

        Assert.assertTrue(deviceProfileData.getTransportConfiguration() instanceof CoapDeviceProfileTransportConfiguration);
        CoapDeviceProfileTransportConfiguration transportConfiguration =
                (CoapDeviceProfileTransportConfiguration) deviceProfileData.getTransportConfiguration();

        PowerSavingConfiguration clientSettings = transportConfiguration.getClientSettings();

        Assert.assertEquals(PowerMode.DRX, clientSettings.getPowerMode());
        Assert.assertEquals(Long.valueOf(1L), clientSettings.getEdrxCycle());
        Assert.assertEquals(Long.valueOf(1L), clientSettings.getPsmActivityTimer());
        Assert.assertEquals(Long.valueOf(1L), clientSettings.getPagingTransmissionWindow());

        Assert.assertTrue(transportConfiguration.getCoapDeviceTypeConfiguration() instanceof DefaultCoapDeviceTypeConfiguration);
        DefaultCoapDeviceTypeConfiguration coapDeviceTypeConfiguration =
                (DefaultCoapDeviceTypeConfiguration) transportConfiguration.getCoapDeviceTypeConfiguration();

        Assert.assertTrue(coapDeviceTypeConfiguration.getTransportPayloadTypeConfiguration() instanceof ProtoTransportPayloadConfiguration);

        ProtoTransportPayloadConfiguration protoTransportPayloadConfiguration =
                (ProtoTransportPayloadConfiguration) coapDeviceTypeConfiguration.getTransportPayloadTypeConfiguration();

        Assert.assertEquals(AbstractTransportIntegrationTest.DEVICE_TELEMETRY_PROTO_SCHEMA, protoTransportPayloadConfiguration.getDeviceTelemetryProtoSchema());
        Assert.assertEquals(AbstractTransportIntegrationTest.DEVICE_ATTRIBUTES_PROTO_SCHEMA, protoTransportPayloadConfiguration.getDeviceAttributesProtoSchema());
        Assert.assertEquals(AbstractTransportIntegrationTest.DEVICE_RPC_RESPONSE_PROTO_SCHEMA, protoTransportPayloadConfiguration.getDeviceRpcResponseProtoSchema());
        Assert.assertEquals(AbstractTransportIntegrationTest.DEVICE_RPC_REQUEST_PROTO_SCHEMA, protoTransportPayloadConfiguration.getDeviceRpcRequestProtoSchema());

        removeDeviceProfileAndDoBasicAssert(deviceProfile);
    }

    /**
     * 功能：验证设备配置相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSendDeviceProfileToCloud() throws Exception {
        RuleChainId ruleChainId = createEdgeRuleChainAndAssignToEdge("Device Profile Rule Chain");
        DashboardId dashboardId = createDashboardAndAssignToEdge("Device Profile Dashboard");

        DeviceProfile deviceProfileMsg = buildDeviceProfileForUplinkMsg("Device Profile On Edge");
        deviceProfileMsg.setDefaultRuleChainId(ruleChainId);
        deviceProfileMsg.setDefaultDashboardId(dashboardId);

        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        DeviceProfileUpdateMsg.Builder deviceProfileUpdateMsgBuilder = DeviceProfileUpdateMsg.newBuilder();
        deviceProfileUpdateMsgBuilder.setIdMSB(deviceProfileMsg.getUuidId().getMostSignificantBits());
        deviceProfileUpdateMsgBuilder.setIdLSB(deviceProfileMsg.getUuidId().getLeastSignificantBits());
        deviceProfileUpdateMsgBuilder.setEntity(JacksonUtil.toString(deviceProfileMsg));
        deviceProfileUpdateMsgBuilder.setMsgType(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE);
        testAutoGeneratedCodeByProtobuf(deviceProfileUpdateMsgBuilder);
        uplinkMsgBuilder.addDeviceProfileUpdateMsg(deviceProfileUpdateMsgBuilder.build());

        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());

        Assert.assertTrue(edgeImitator.waitForResponses());

        UplinkResponseMsg latestResponseMsg = edgeImitator.getLatestResponseMsg();
        Assert.assertTrue(latestResponseMsg.getSuccess());

        DeviceProfile deviceProfile = doGet("/api/deviceProfile/" + deviceProfileMsg.getUuidId(), DeviceProfile.class);
        Assert.assertNotNull(deviceProfile);
        Assert.assertEquals("Device Profile On Edge", deviceProfile.getName());

        // delete profile
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/deviceProfile/" + deviceProfile.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DeviceProfileUpdateMsg);
        DeviceProfileUpdateMsg deviceProfileUpdateMsg = (DeviceProfileUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, deviceProfileUpdateMsg.getMsgType());
        Assert.assertEquals(deviceProfile.getUuidId().getMostSignificantBits(), deviceProfileUpdateMsg.getIdMSB());
        Assert.assertEquals(deviceProfile.getUuidId().getLeastSignificantBits(), deviceProfileUpdateMsg.getIdLSB());

        // cleanup
        unAssignFromEdgeAndDeleteDashboard(dashboardId);
        unAssignFromEdgeAndDeleteRuleChain(ruleChainId);
    }

    /**
     * 功能：保存或创建配置。
     * 参数：无。
     * 返回：处理结果。
     */
    private DeviceProfileData createProfileData() {
        DeviceProfileData deviceProfileData = new DeviceProfileData();
        deviceProfileData.setConfiguration(new DefaultDeviceProfileConfiguration());
        deviceProfileData.setTransportConfiguration(new DefaultDeviceProfileTransportConfiguration());
        deviceProfileData.setProvisionConfiguration(new DisabledDeviceProfileProvisionConfiguration("Device Secret"));
        return deviceProfileData;
    }

    /**
     * 功能：保存或创建设备配置。
     * 参数：
     * - `deviceProfileName`：设备信息或设备标识。
     * - `deviceProfileTransportConfiguration`：设备信息或设备标识。
     * 返回：处理结果。
     */
    private DeviceProfile createDeviceProfileAndDoBasicAssert(String deviceProfileName, DeviceProfileTransportConfiguration deviceProfileTransportConfiguration) throws Exception {
        DeviceProfile deviceProfile = this.createDeviceProfile(deviceProfileName, deviceProfileTransportConfiguration);
        extendDeviceProfileData(deviceProfile);
        edgeImitator.expectMessageAmount(1);
        deviceProfile = doPost("/api/deviceProfile", deviceProfile, DeviceProfile.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        return deviceProfile;
    }

    /**
     * 功能：删除或清理设备配置。
     * 参数：
     * - `deviceProfile`：设备信息或设备标识。
     * 返回：无。
     */
    private void removeDeviceProfileAndDoBasicAssert(DeviceProfile deviceProfile) throws Exception {
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/deviceProfile/" + deviceProfile.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());
        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof DeviceProfileUpdateMsg);
        DeviceProfileUpdateMsg deviceProfileUpdateMsg = (DeviceProfileUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, deviceProfileUpdateMsg.getMsgType());
        Assert.assertEquals(deviceProfile.getUuidId().getMostSignificantBits(), deviceProfileUpdateMsg.getIdMSB());
        Assert.assertEquals(deviceProfile.getUuidId().getLeastSignificantBits(), deviceProfileUpdateMsg.getIdLSB());
    }

    /**
     * 功能：保存或创建设备配置。
     * 参数：无。
     * 返回：处理结果。
     */
    private SnmpDeviceProfileTransportConfiguration createSnmpDeviceProfileTransportConfiguration() {
        SnmpDeviceProfileTransportConfiguration transportConfiguration = new SnmpDeviceProfileTransportConfiguration();
        List<SnmpCommunicationConfig> communicationConfigs = new ArrayList<>();
        TelemetryQueryingSnmpCommunicationConfig communicationConfig = new TelemetryQueryingSnmpCommunicationConfig();
        communicationConfig.setQueryingFrequencyMs(500L);
        List<SnmpMapping> mappings = new ArrayList<>();
        mappings.add(new SnmpMapping("1.3.3.5.6.7.8.9.1", "temperature", DataType.DOUBLE));
        communicationConfig.setMappings(mappings);
        communicationConfigs.add(communicationConfig);
        transportConfiguration.setCommunicationConfigs(communicationConfigs);
        transportConfiguration.setTimeoutMs(1000);
        transportConfiguration.setRetries(3);
        return transportConfiguration;
    }

    /**
     * 功能：保存或创建设备配置。
     * 参数：无。
     * 返回：处理结果。
     */
    private Lwm2mDeviceProfileTransportConfiguration createLwm2mDeviceProfileTransportConfiguration() {
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = new Lwm2mDeviceProfileTransportConfiguration();

        OtherConfiguration clientLwM2mSettings = JacksonUtil.fromString(AbstractLwM2MIntegrationTest.CLIENT_LWM2M_SETTINGS, OtherConfiguration.class);
        transportConfiguration.setClientLwM2mSettings(clientLwM2mSettings);

        transportConfiguration.setBootstrapServerUpdateEnable(true);

        TelemetryMappingConfiguration observeAttrConfiguration =
                JacksonUtil.fromString(AbstractLwM2MIntegrationTest.OBSERVE_ATTRIBUTES_WITH_PARAMS, TelemetryMappingConfiguration.class);
        transportConfiguration.setObserveAttr(observeAttrConfiguration);

        List<LwM2MBootstrapServerCredential> bootstrap = new ArrayList<>();
        AbstractLwM2MBootstrapServerCredential bootstrapServerCredential = new NoSecLwM2MBootstrapServerCredential();
        bootstrapServerCredential.setServerPublicKey("PUBLIC_KEY");
        bootstrapServerCredential.setShortServerId(123);
        bootstrapServerCredential.setBootstrapServerIs(true);
        bootstrapServerCredential.setHost("localhost");
        bootstrapServerCredential.setPort(5687);
        bootstrap.add(bootstrapServerCredential);
        transportConfiguration.setBootstrap(bootstrap);

        return transportConfiguration;
    }

    /**
     * 功能：保存或创建设备配置。
     * 参数：无。
     * 返回：处理结果。
     */
    private CoapDeviceProfileTransportConfiguration createCoapDeviceProfileTransportConfiguration() {
        CoapDeviceProfileTransportConfiguration transportConfiguration = new CoapDeviceProfileTransportConfiguration();
        PowerSavingConfiguration clientSettings = new PowerSavingConfiguration();
        clientSettings.setPowerMode(PowerMode.DRX);
        clientSettings.setEdrxCycle(1L);
        clientSettings.setPsmActivityTimer(1L);
        clientSettings.setPagingTransmissionWindow(1L);
        transportConfiguration.setClientSettings(clientSettings);
        DefaultCoapDeviceTypeConfiguration coapDeviceTypeConfiguration = new DefaultCoapDeviceTypeConfiguration();
        ProtoTransportPayloadConfiguration transportPayloadTypeConfiguration = new ProtoTransportPayloadConfiguration();
        transportPayloadTypeConfiguration.setDeviceTelemetryProtoSchema(AbstractTransportIntegrationTest.DEVICE_TELEMETRY_PROTO_SCHEMA);
        transportPayloadTypeConfiguration.setDeviceAttributesProtoSchema(AbstractTransportIntegrationTest.DEVICE_ATTRIBUTES_PROTO_SCHEMA);
        transportPayloadTypeConfiguration.setDeviceRpcResponseProtoSchema(AbstractTransportIntegrationTest.DEVICE_RPC_RESPONSE_PROTO_SCHEMA);
        transportPayloadTypeConfiguration.setDeviceRpcRequestProtoSchema(AbstractTransportIntegrationTest.DEVICE_RPC_REQUEST_PROTO_SCHEMA);
        coapDeviceTypeConfiguration.setTransportPayloadTypeConfiguration(transportPayloadTypeConfiguration);
        transportConfiguration.setCoapDeviceTypeConfiguration(coapDeviceTypeConfiguration);
        return transportConfiguration;
    }

    /**
     * 功能：验证设备配置相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSendDeviceProfileToCloudWithNameThatAlreadyExistsOnCloud() throws Exception {
        String deviceProfileOnCloudName = StringUtils.randomAlphanumeric(15);

        edgeImitator.expectMessageAmount(1);
        DeviceProfile deviceProfileOnCloud = this.createDeviceProfile(deviceProfileOnCloudName);
        deviceProfileOnCloud = doPost("/api/deviceProfile", deviceProfileOnCloud, DeviceProfile.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        DeviceProfile deviceProfileMsg = buildDeviceProfileForUplinkMsg(deviceProfileOnCloudName);
        deviceProfileMsg.setProfileData(deviceProfileOnCloud.getProfileData());

        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        DeviceProfileUpdateMsg.Builder deviceProfileUpdateMsgBuilder = DeviceProfileUpdateMsg.newBuilder();
        deviceProfileUpdateMsgBuilder.setIdMSB(deviceProfileMsg.getUuidId().getMostSignificantBits());
        deviceProfileUpdateMsgBuilder.setIdLSB(deviceProfileMsg.getUuidId().getLeastSignificantBits());
        deviceProfileUpdateMsgBuilder.setEntity(JacksonUtil.toString(deviceProfileMsg));
        deviceProfileUpdateMsgBuilder.setMsgType(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE);
        uplinkMsgBuilder.addDeviceProfileUpdateMsg(deviceProfileUpdateMsgBuilder.build());

        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.expectMessageAmount(1);

        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());

        Assert.assertTrue(edgeImitator.waitForResponses());
        Assert.assertTrue(edgeImitator.waitForMessages());

        Optional<DeviceProfileUpdateMsg> deviceProfileUpdateMsgOpt = edgeImitator.findMessageByType(DeviceProfileUpdateMsg.class);
        Assert.assertTrue(deviceProfileUpdateMsgOpt.isPresent());
        DeviceProfileUpdateMsg latestDeviceProfileUpdateMsg = deviceProfileUpdateMsgOpt.get();
        deviceProfileMsg = JacksonUtil.fromString(latestDeviceProfileUpdateMsg.getEntity(), DeviceProfile.class, true);
        Assert.assertNotNull(deviceProfileMsg);
        Assert.assertNotEquals(deviceProfileOnCloudName, deviceProfileMsg.getName());

        Assert.assertNotEquals(deviceProfileOnCloud.getId(), deviceProfileMsg.getId());

        DeviceProfile deviceProfile = doGet("/api/deviceProfile/" + deviceProfileMsg.getUuidId(), DeviceProfile.class);
        Assert.assertNotNull(deviceProfile);
        Assert.assertNotEquals(deviceProfileOnCloudName, deviceProfile.getName());
    }

    /**
     * 功能：构建设备配置。
     * 参数：
     * - `name`：名称。
     * 返回：处理结果。
     */
    private DeviceProfile buildDeviceProfileForUplinkMsg(String name) {
        DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setId(new DeviceProfileId(UUID.randomUUID()));
        deviceProfile.setTenantId(tenantId);
        deviceProfile.setName(name);
        deviceProfile.setDefault(false);
        deviceProfile.setType(DeviceProfileType.DEFAULT);
        deviceProfile.setTransportType(DeviceTransportType.DEFAULT);
        deviceProfile.setProfileData(createProfileData());
        return deviceProfile;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`DeviceProfileEdgeTest` 在 ThingsBoard Application 测试模块 中承担测试支撑类型职责，核心目的是验证 Application 模块的控制器、服务、Actor 或集成流程。
 * 2. 核心流程：准备输入和依赖，调用被测对象并断言结果。
 * 3. 关键依赖：主要依赖或协作对象包括JUnit、Mockito、Spring Test、DAO、缓存、Transport 和测试夹具。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
