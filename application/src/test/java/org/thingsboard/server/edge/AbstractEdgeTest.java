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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.protobuf.AbstractMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.AdminSettings;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceInfo;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.SaveOtaPackageInfoRequest;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.device.profile.AlarmCondition;
import org.thingsboard.server.common.data.device.profile.AlarmConditionFilter;
import org.thingsboard.server.common.data.device.profile.AlarmConditionFilterKey;
import org.thingsboard.server.common.data.device.profile.AlarmConditionKeyType;
import org.thingsboard.server.common.data.device.profile.AlarmRule;
import org.thingsboard.server.common.data.device.profile.AllowCreateNewDevicesDeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileAlarm;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.device.profile.JsonTransportPayloadConfiguration;
import org.thingsboard.server.common.data.device.profile.SimpleAlarmConditionSpec;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.ota.ChecksumAlgorithm;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.query.EntityKeyValueType;
import org.thingsboard.server.common.data.query.FilterPredicateValue;
import org.thingsboard.server.common.data.query.NumericFilterPredicate;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.security.model.JwtSettings;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.edge.EdgeEventService;
import org.thingsboard.server.edge.imitator.EdgeImitator;
import org.thingsboard.server.gen.edge.v1.AdminSettingsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AssetProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AssetUpdateMsg;
import org.thingsboard.server.gen.edge.v1.CustomerUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.v1.EdgeConfiguration;
import org.thingsboard.server.gen.edge.v1.QueueUpdateMsg;
import org.thingsboard.server.gen.edge.v1.RuleChainMetadataRequestMsg;
import org.thingsboard.server.gen.edge.v1.RuleChainMetadataUpdateMsg;
import org.thingsboard.server.gen.edge.v1.RuleChainUpdateMsg;
import org.thingsboard.server.gen.edge.v1.SyncCompletedMsg;
import org.thingsboard.server.gen.edge.v1.TenantProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.TenantUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.gen.edge.v1.UserUpdateMsg;
import org.thingsboard.server.queue.util.DataDecodingEncodingService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 中文说明：
 * 1. 类目的：`AbstractEdgeTest` 是ThingsBoard Application 测试模块中的测试支撑类型，用于验证 Application 模块的控制器、服务、Actor 或集成流程。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括JUnit、Mockito、Spring Test、DAO、缓存、Transport 和测试夹具。
 * 4. 生命周期：由测试框架按测试类和测试方法管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Test Fixture。
 */
@TestPropertySource(properties = {
        "edges.enabled=true",
        "queue.rule-engine.stats.enabled=false",
        "edges.storage.sleep_between_batches=1000"
})
@Slf4j
abstract public class AbstractEdgeTest extends AbstractControllerTest {

    /**
     * 设备配置常量，用于统一引用固定值。
     */
    private static final String THERMOSTAT_DEVICE_PROFILE_NAME = "Thermostat";

    /**
     * 设备配置，保存当前对象的配置选项。
     */
    protected DeviceProfile thermostatDeviceProfile;

    /**
     * 边缘节点对象，用于描述当前业务场景。
     */
    protected EdgeImitator edgeImitator;
    protected Edge edge;

    /**
     * 边缘节点，提供当前类调用的业务操作。
     */
    @Autowired
    protected EdgeEventService edgeEventService;

    /**
     * 数据，提供当前类调用的业务操作。
     */
    @Autowired
    protected DataDecodingEncodingService dataDecodingEncodingService;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    protected TbClusterService clusterService;

    /**
     * 功能：执行 `setupEdgeTest` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void setupEdgeTest() throws Exception {
        loginSysAdmin();

        // get jwt settings from yaml config
        JwtSettings settings = doGet("/api/admin/jwtSettings", JwtSettings.class);
        // save jwt settings into db
        doPost("/api/admin/jwtSettings", settings).andExpect(status().isOk());

        loginTenantAdmin();

        installation();

        edgeImitator = new EdgeImitator("localhost", 7070, edge.getRoutingKey(), edge.getSecret());
        edgeImitator.expectMessageAmount(21);
        edgeImitator.connect();

        requestEdgeRuleChainMetadata();

        verifyEdgeConnectionAndInitialData();
    }

    /**
     * 功能：执行 `requestEdgeRuleChainMetadata` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    private void requestEdgeRuleChainMetadata() throws Exception {
        RuleChainId rootRuleChainId = getEdgeRootRuleChainId();
        RuleChainMetadataRequestMsg.Builder builder = RuleChainMetadataRequestMsg.newBuilder()
                .setRuleChainIdMSB(rootRuleChainId.getId().getMostSignificantBits())
                .setRuleChainIdLSB(rootRuleChainId.getId().getLeastSignificantBits());
        testAutoGeneratedCodeByProtobuf(builder);
        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder()
                .addRuleChainMetadataRequestMsg(builder.build());
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
    }

    /**
     * 功能：获取规则链。
     * 参数：无。
     * 返回：处理结果。
     */
    private RuleChainId getEdgeRootRuleChainId() throws Exception {
        List<RuleChain> edgeRuleChains = doGetTypedWithPageLink("/api/edge/" + edge.getUuidId() + "/ruleChains?",
                new TypeReference<PageData<RuleChain>>() {}, new PageLink(100)).getData();
        for (RuleChain edgeRuleChain : edgeRuleChains) {
            if (edgeRuleChain.isRoot()) {
                return edgeRuleChain.getId();
            }
        }
        throw new RuntimeException("Root rule chain not found");
    }

    /**
     * 功能：执行 `teardownEdgeTest` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @After
    public void teardownEdgeTest() {
        try {
            loginTenantAdmin();

            doDelete("/api/edge/" + edge.getId().toString())
                    .andExpect(status().isOk());
            edgeImitator.disconnect();
        } catch (Exception ignored) {}
    }

    /**
     * 功能：执行 `installation` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    private void installation() throws Exception {
        thermostatDeviceProfile = this.createDeviceProfile(THERMOSTAT_DEVICE_PROFILE_NAME,
                createMqttDeviceProfileTransportConfiguration(new JsonTransportPayloadConfiguration(), false));
        extendDeviceProfileData(thermostatDeviceProfile);
        thermostatDeviceProfile = doPost("/api/deviceProfile", thermostatDeviceProfile, DeviceProfile.class);

        Device savedDevice = saveDevice("Edge Device 1", THERMOSTAT_DEVICE_PROFILE_NAME);

        Asset savedAsset = saveAsset("Edge Asset 1");

        edge = doPost("/api/edge", constructEdge("Test Edge", "test"), Edge.class);

        doPost("/api/edge/" + edge.getUuidId()
                + "/device/" + savedDevice.getUuidId(), Device.class);
        doPost("/api/edge/" + edge.getUuidId()
                + "/asset/" + savedAsset.getUuidId(), Asset.class);

        // wait until assign device and asset events are fully processed by edge notification service
        TimeUnit.MILLISECONDS.sleep(500);
    }

    /**
     * 功能：执行 `extendDeviceProfileData` 对应的处理。
     * 参数：
     * - `deviceProfile`：设备信息或设备标识。
     * 返回：无。
     */
    protected void extendDeviceProfileData(DeviceProfile deviceProfile) {
        DeviceProfileData profileData = deviceProfile.getProfileData();
        List<DeviceProfileAlarm> alarms = new ArrayList<>();
        DeviceProfileAlarm deviceProfileAlarm = new DeviceProfileAlarm();
        deviceProfileAlarm.setAlarmType("High Temperature");
        AlarmRule alarmRule = new AlarmRule();
        alarmRule.setAlarmDetails("Alarm Details");
        AlarmCondition alarmCondition = new AlarmCondition();
        alarmCondition.setSpec(new SimpleAlarmConditionSpec());
        List<AlarmConditionFilter> condition = new ArrayList<>();
        AlarmConditionFilter alarmConditionFilter = new AlarmConditionFilter();
        alarmConditionFilter.setKey(new AlarmConditionFilterKey(AlarmConditionKeyType.ATTRIBUTE, "temperature"));
        NumericFilterPredicate predicate = new NumericFilterPredicate();
        predicate.setOperation(NumericFilterPredicate.NumericOperation.GREATER);
        predicate.setValue(new FilterPredicateValue<>(55.0));
        alarmConditionFilter.setPredicate(predicate);
        alarmConditionFilter.setValueType(EntityKeyValueType.NUMERIC);
        condition.add(alarmConditionFilter);
        alarmCondition.setCondition(condition);
        alarmRule.setCondition(alarmCondition);
        deviceProfileAlarm.setClearRule(alarmRule);
        TreeMap<AlarmSeverity, AlarmRule> createRules = new TreeMap<>();
        createRules.put(AlarmSeverity.CRITICAL, alarmRule);
        deviceProfileAlarm.setCreateRules(createRules);
        alarms.add(deviceProfileAlarm);
        profileData.setAlarms(alarms);
        profileData.setProvisionConfiguration(new AllowCreateNewDevicesDeviceProfileProvisionConfiguration("123"));
    }

    /**
     * 功能：校验边缘节点。
     * 参数：无。
     * 返回：无。
     */
    private void verifyEdgeConnectionAndInitialData() throws Exception {
        Assert.assertTrue(edgeImitator.waitForMessages());

        validateEdgeConfiguration();

        // 1 message from queue fetcher
        validateMsgsCnt(QueueUpdateMsg.class, 1);
        validateQueues();

        // 1 from rule chain fetcher
        validateMsgsCnt(RuleChainUpdateMsg.class, 1);
        UUID ruleChainUUID = validateRuleChains();

        // 1 from request message
        validateMsgsCnt(RuleChainMetadataUpdateMsg.class, 1);
        validateRuleChainMetadataUpdates(ruleChainUUID);

        // 4 messages ('general', 'mail', 'connectivity', 'jwt')
        validateMsgsCnt(AdminSettingsUpdateMsg.class, 4);
        validateAdminSettings(4);

        // 4 messages
        // - 1 from default profile fetcher
        // - 2 from device profile fetcher (default and thermostat)
        // - 1 from device fetcher
        validateMsgsCnt(DeviceProfileUpdateMsg.class, 4);
        validateDeviceProfiles(4);

        // 3 messages
        // - 1 from default profile fetcher
        // - 1 message from asset profile fetcher
        // - 1 message from asset fetcher
        validateMsgsCnt(AssetProfileUpdateMsg.class, 3);
        validateAssetProfiles(3);

        // 1 from device fetcher
        validateMsgsCnt(DeviceUpdateMsg.class, 1);
        validateDevices();

        // 1 from asset fetcher
        validateMsgsCnt(AssetUpdateMsg.class, 1);
        validateAssets();

        // 1 message from public customer fetcher
        validateMsgsCnt(CustomerUpdateMsg.class, 1);
        validatePublicCustomer();

        // 1 message from user fetcher
        validateMsgsCnt(UserUpdateMsg.class, 1);
        validateUsers();

        // 1 from tenant fetcher
        validateMsgsCnt(TenantUpdateMsg.class, 1);
        validateTenant();

        // 1 from tenant profile fetcher
        validateMsgsCnt(TenantProfileUpdateMsg.class, 1);
        validateTenantProfile();

        // 1 message sync completed
        validateMsgsCnt(SyncCompletedMsg.class, 1);
        validateSyncCompleted();
    }

    /**
     * 功能：校验`Msgs Cnt`。
     * 参数：
     * - `clazz`：`clazz` 参数。
     * - `expectedMsgCnt`：待处理消息。
     * 返回：无。
     */
    private <T extends AbstractMessage> void validateMsgsCnt(Class<T> clazz, int expectedMsgCnt) {
        List<T> downlinkMsgsByType = edgeImitator.findAllMessagesByType(clazz);
        if (downlinkMsgsByType.size() != expectedMsgCnt) {
            List<AbstractMessage> downlinkMsgs = edgeImitator.getDownlinkMsgs();
            for (AbstractMessage downlinkMsg : downlinkMsgs) {
                log.error("{}\n{}", downlinkMsg.getClass(), downlinkMsg);
            }
            Assert.fail("Unexpected message count for " + clazz + "! Expected: " + expectedMsgCnt + ", but found: " + downlinkMsgsByType.size());
        }
    }

    /**
     * 功能：校验边缘节点。
     * 参数：无。
     * 返回：无。
     */
    private void validateEdgeConfiguration() throws Exception {
        EdgeConfiguration configuration = edgeImitator.getConfiguration();
        Assert.assertNotNull(configuration);
        testAutoGeneratedCodeByProtobuf(configuration);
    }

    /**
     * 功能：校验租户。
     * 参数：无。
     * 返回：无。
     */
    private void validateTenant() throws Exception {
        Optional<TenantUpdateMsg> tenantUpdateMsgOpt = edgeImitator.findMessageByType(TenantUpdateMsg.class);
        Assert.assertTrue(tenantUpdateMsgOpt.isPresent());
        TenantUpdateMsg tenantUpdateMsg = tenantUpdateMsgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, tenantUpdateMsg.getMsgType());
        Tenant tenantMsg = JacksonUtil.fromString(tenantUpdateMsg.getEntity(), Tenant.class, true);
        Assert.assertNotNull(tenantMsg);
        Tenant tenant = doGet("/api/tenant/" + tenantMsg.getUuidId(), Tenant.class);
        Assert.assertNotNull(tenant);
        testAutoGeneratedCodeByProtobuf(tenantUpdateMsg);
    }

    /**
     * 功能：校验租户。
     * 参数：无。
     * 返回：无。
     */
    private void validateTenantProfile() throws Exception {
        Optional<TenantProfileUpdateMsg> tenantProfileUpdateMsgOpt = edgeImitator.findMessageByType(TenantProfileUpdateMsg.class);
        Assert.assertTrue(tenantProfileUpdateMsgOpt.isPresent());
        TenantProfileUpdateMsg tenantProfileUpdateMsg = tenantProfileUpdateMsgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, tenantProfileUpdateMsg.getMsgType());
        TenantProfile tenantProfile = JacksonUtil.fromString(tenantProfileUpdateMsg.getEntity(), TenantProfile.class, true);
        Assert.assertNotNull(tenantProfile);
        Tenant tenant = doGet("/api/tenant/" + tenantId.getId(), Tenant.class);
        Assert.assertNotNull(tenant);
        Assert.assertEquals(tenantProfile.getId(), tenant.getTenantProfileId());
        testAutoGeneratedCodeByProtobuf(tenantProfileUpdateMsg);
    }

    /**
     * 功能：校验设备。
     * 参数：
     * - `expectedMsgCnt`：待处理消息。
     * 返回：无。
     */
    private void validateDeviceProfiles(int expectedMsgCnt) throws Exception {
        List<DeviceProfileUpdateMsg> deviceProfileUpdateMsgList = edgeImitator.findAllMessagesByType(DeviceProfileUpdateMsg.class);
        // default msg default device profile from fetcher
        // default msg device profile from fetcher
        // thermostat msg from device profile fetcher
        // thermostat msg from device fetcher
        Assert.assertEquals(expectedMsgCnt, deviceProfileUpdateMsgList.size());
        Optional<DeviceProfileUpdateMsg> thermostatProfileUpdateMsgOpt =
                deviceProfileUpdateMsgList.stream().filter(dfum -> {
                    DeviceProfile deviceProfile = JacksonUtil.fromString(dfum.getEntity(), DeviceProfile.class, true);
                    Assert.assertNotNull(deviceProfile);
                    return THERMOSTAT_DEVICE_PROFILE_NAME.equals(deviceProfile.getName());
                }).findAny();
        Assert.assertTrue(thermostatProfileUpdateMsgOpt.isPresent());
        DeviceProfileUpdateMsg thermostatProfileUpdateMsg = thermostatProfileUpdateMsgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, thermostatProfileUpdateMsg.getMsgType());
        UUID deviceProfileUUID = new UUID(thermostatProfileUpdateMsg.getIdMSB(), thermostatProfileUpdateMsg.getIdLSB());
        DeviceProfile deviceProfile = doGet("/api/deviceProfile/" + deviceProfileUUID, DeviceProfile.class);
        Assert.assertNotNull(deviceProfile);
        Assert.assertNotNull(deviceProfile.getProfileData());
        Assert.assertNotNull(deviceProfile.getProfileData().getAlarms());
        Assert.assertNotNull(deviceProfile.getProfileData().getAlarms().get(0).getClearRule());

        testAutoGeneratedCodeByProtobuf(thermostatProfileUpdateMsg);
    }

    /**
     * 功能：校验`Devices`。
     * 参数：无。
     * 返回：无。
     */
    private void validateDevices() throws Exception {
        Optional<DeviceUpdateMsg> deviceUpdateMsgOpt = edgeImitator.findMessageByType(DeviceUpdateMsg.class);
        Assert.assertTrue(deviceUpdateMsgOpt.isPresent());
        validateDevice(deviceUpdateMsgOpt.get());
    }

    /**
     * 功能：校验设备。
     * 参数：
     * - `deviceUpdateMsg`：设备信息或设备标识。
     * 返回：无。
     */
    private void validateDevice(DeviceUpdateMsg deviceUpdateMsg) throws Exception {
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, deviceUpdateMsg.getMsgType());
        UUID deviceUUID = new UUID(deviceUpdateMsg.getIdMSB(), deviceUpdateMsg.getIdLSB());
        Device device = doGet("/api/device/" + deviceUUID, Device.class);
        Assert.assertNotNull(device);
        List<DeviceInfo> edgeDevices = doGetTypedWithPageLink("/api/edge/" + edge.getUuidId() + "/devices?",
                new TypeReference<PageData<DeviceInfo>>() {}, new PageLink(100)).getData();
        Assert.assertTrue(edgeDevices.stream().map(DeviceInfo::getId).anyMatch(id -> id.equals(device.getId())));

        testAutoGeneratedCodeByProtobuf(deviceUpdateMsg);
    }

    /**
     * 功能：校验`Assets`。
     * 参数：无。
     * 返回：无。
     */
    private void validateAssets() throws Exception {
        Optional<AssetUpdateMsg> assetUpdateMsgOpt = edgeImitator.findMessageByType(AssetUpdateMsg.class);
        Assert.assertTrue(assetUpdateMsgOpt.isPresent());
        validateAsset(assetUpdateMsgOpt.get());
    }

    /**
     * 功能：校验资产。
     * 参数：
     * - `assetUpdateMsg`：待处理消息。
     * 返回：无。
     */
    private void validateAsset(AssetUpdateMsg assetUpdateMsg) throws Exception {
        Assert.assertNotNull(assetUpdateMsg);
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, assetUpdateMsg.getMsgType());
        UUID assetUUID = new UUID(assetUpdateMsg.getIdMSB(), assetUpdateMsg.getIdLSB());
        Asset asset = doGet("/api/asset/" + assetUUID, Asset.class);
        Assert.assertNotNull(asset);
        List<Asset> edgeAssets = doGetTypedWithPageLink("/api/edge/" + edge.getUuidId() + "/assets?",
                new TypeReference<PageData<Asset>>() {}, new PageLink(100)).getData();
        Assert.assertTrue(edgeAssets.contains(asset));

        testAutoGeneratedCodeByProtobuf(assetUpdateMsg);
    }

    /**
     * 功能：校验`Rule Chains`。
     * 参数：无。
     * 返回：判断结果。
     */
    private UUID validateRuleChains() throws Exception {
        Optional<RuleChainUpdateMsg> ruleChainUpdateMsgOpt = edgeImitator.findMessageByType(RuleChainUpdateMsg.class);
        Assert.assertTrue(ruleChainUpdateMsgOpt.isPresent());
        RuleChainUpdateMsg ruleChainUpdateMsg = ruleChainUpdateMsgOpt.get();
        validateRuleChain(ruleChainUpdateMsg, UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE);
        return new UUID(ruleChainUpdateMsg.getIdMSB(), ruleChainUpdateMsg.getIdLSB());
    }

    /**
     * 功能：校验规则链。
     * 参数：
     * - `ruleChainUpdateMsg`：待处理消息。
     * - `expectedMsgType`：待处理消息。
     * 返回：无。
     */
    private void validateRuleChain(RuleChainUpdateMsg ruleChainUpdateMsg, UpdateMsgType expectedMsgType) throws Exception {
        Assert.assertEquals(expectedMsgType, ruleChainUpdateMsg.getMsgType());
        UUID ruleChainUUID = new UUID(ruleChainUpdateMsg.getIdMSB(), ruleChainUpdateMsg.getIdLSB());
        RuleChain ruleChain = doGet("/api/ruleChain/" + ruleChainUUID, RuleChain.class);
        Assert.assertNotNull(ruleChain);
        List<RuleChain> edgeRuleChains = doGetTypedWithPageLink("/api/edge/" + edge.getUuidId() + "/ruleChains?",
                new TypeReference<PageData<RuleChain>>() {}, new PageLink(100)).getData();
        Assert.assertTrue(edgeRuleChains.contains(ruleChain));
        testAutoGeneratedCodeByProtobuf(ruleChainUpdateMsg);
    }

    /**
     * 功能：校验规则链。
     * 参数：
     * - `expectedRuleChainUUID`：规则链ID。
     * 返回：无。
     */
    private void validateRuleChainMetadataUpdates(UUID expectedRuleChainUUID) {
        Optional<RuleChainMetadataUpdateMsg> ruleChainMetadataUpdateOpt = edgeImitator.findMessageByType(RuleChainMetadataUpdateMsg.class);
        Assert.assertTrue(ruleChainMetadataUpdateOpt.isPresent());
        RuleChainMetadataUpdateMsg ruleChainMetadataUpdateMsg = ruleChainMetadataUpdateOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, ruleChainMetadataUpdateMsg.getMsgType());
        RuleChainMetaData ruleChainMetaData = JacksonUtil.fromString(ruleChainMetadataUpdateMsg.getEntity(), RuleChainMetaData.class, true);
        Assert.assertNotNull(ruleChainMetaData);
        Assert.assertEquals(expectedRuleChainUUID, ruleChainMetaData.getRuleChainId().getId());
    }

    /**
     * 功能：校验配置。
     * 参数：
     * - `expectedMsgCnt`：待处理消息。
     * 返回：无。
     */
    private void validateAdminSettings(int expectedMsgCnt) {
        List<AdminSettingsUpdateMsg> adminSettingsUpdateMsgs = edgeImitator.findAllMessagesByType(AdminSettingsUpdateMsg.class);
        Assert.assertEquals(expectedMsgCnt, adminSettingsUpdateMsgs.size());

        for (AdminSettingsUpdateMsg adminSettingsUpdateMsg : adminSettingsUpdateMsgs) {
            AdminSettings adminSettings = JacksonUtil.fromString(adminSettingsUpdateMsg.getEntity(), AdminSettings.class, true);
            Assert.assertNotNull(adminSettings);
            if (adminSettings.getKey().equals("general")) {
                validateGeneralAdminSettings(adminSettings);
            }
            if (adminSettings.getKey().equals("mail")) {
                validateMailAdminSettings(adminSettings);
            }
            if (adminSettings.getKey().equals("connectivity")) {
                validateConnectivityAdminSettings(adminSettings);
            }
            if (adminSettings.getKey().equals("jwt")) {
                validateJwtAdminSettings(adminSettings);
            }
        }
    }

    /**
     * 功能：校验配置。
     * 参数：
     * - `adminSettings`：配置对象。
     * 返回：无。
     */
    private void validateGeneralAdminSettings(AdminSettings adminSettings) {
        Assert.assertNotNull(adminSettings.getJsonValue().get("baseUrl"));
    }

    /**
     * 功能：校验配置。
     * 参数：
     * - `adminSettings`：配置对象。
     * 返回：无。
     */
    private void validateMailAdminSettings(AdminSettings adminSettings) {
        JsonNode jsonNode = adminSettings.getJsonValue();
        Assert.assertNotNull(jsonNode.get("mailFrom"));
        Assert.assertNotNull(jsonNode.get("smtpProtocol"));
        Assert.assertNotNull(jsonNode.get("smtpHost"));
        Assert.assertNotNull(jsonNode.get("smtpPort"));
        Assert.assertNotNull(jsonNode.get("timeout"));
    }

    /**
     * 功能：校验配置。
     * 参数：
     * - `adminSettings`：配置对象。
     * 返回：无。
     */
    private void validateConnectivityAdminSettings(AdminSettings adminSettings) {
        JsonNode jsonNode = adminSettings.getJsonValue();
        Assert.assertNotNull(jsonNode.get("http"));
        Assert.assertNotNull(jsonNode.get("https"));
        Assert.assertNotNull(jsonNode.get("mqtt"));
        Assert.assertNotNull(jsonNode.get("mqtts"));
        Assert.assertNotNull(jsonNode.get("coap"));
        Assert.assertNotNull(jsonNode.get("coaps"));
    }

    /**
     * 功能：校验配置。
     * 参数：
     * - `adminSettings`：配置对象。
     * 返回：无。
     */
    private void validateJwtAdminSettings(AdminSettings adminSettings) {
        JsonNode jsonNode = adminSettings.getJsonValue();
        Assert.assertNotNull(jsonNode.get("tokenExpirationTime"));
        Assert.assertNotNull(jsonNode.get("refreshTokenExpTime"));
        Assert.assertNotNull(jsonNode.get("tokenIssuer"));
        Assert.assertNotNull(jsonNode.get("tokenSigningKey"));
    }

    /**
     * 功能：校验资产。
     * 参数：
     * - `expectedMsgCnt`：待处理消息。
     * 返回：无。
     */
    private void validateAssetProfiles(int expectedMsgCnt) throws Exception {
        List<AssetProfileUpdateMsg> assetProfileUpdateMsgs = edgeImitator.findAllMessagesByType(AssetProfileUpdateMsg.class);
        Assert.assertEquals(expectedMsgCnt, assetProfileUpdateMsgs.size());
        AssetProfileUpdateMsg assetProfileUpdateMsg = assetProfileUpdateMsgs.get(0);
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, assetProfileUpdateMsg.getMsgType());
        UUID assetProfileUUID = new UUID(assetProfileUpdateMsg.getIdMSB(), assetProfileUpdateMsg.getIdLSB());
        AssetProfile assetProfile = doGet("/api/assetProfile/" + assetProfileUUID, AssetProfile.class);
        Assert.assertNotNull(assetProfile);
        Assert.assertEquals("default", assetProfile.getName());
        Assert.assertTrue(assetProfile.isDefault());
        testAutoGeneratedCodeByProtobuf(assetProfileUpdateMsg);
    }

    /**
     * 功能：校验`Queues`。
     * 参数：无。
     * 返回：无。
     */
    private void validateQueues() throws Exception {
        Optional<QueueUpdateMsg> queueUpdateMsgOpt = edgeImitator.findMessageByType(QueueUpdateMsg.class);
        Assert.assertTrue(queueUpdateMsgOpt.isPresent());
        QueueUpdateMsg queueUpdateMsg = queueUpdateMsgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, queueUpdateMsg.getMsgType());
        UUID queueUUID = new UUID(queueUpdateMsg.getIdMSB(), queueUpdateMsg.getIdLSB());
        Queue queue = doGet("/api/queues/" + queueUUID, Queue.class);
        Assert.assertNotNull(queue);
        Assert.assertEquals(DataConstants.MAIN_QUEUE_NAME, queue.getName());
        Assert.assertEquals(DataConstants.MAIN_QUEUE_TOPIC, queue.getTopic());
        Assert.assertEquals(10, queue.getPartitions());
        Assert.assertEquals(25, queue.getPollInterval());
        testAutoGeneratedCodeByProtobuf(queueUpdateMsg);
    }

    /**
     * 功能：校验`Users`。
     * 参数：无。
     * 返回：无。
     */
    private void validateUsers() throws Exception {
        Optional<UserUpdateMsg> userUpdateMsgOpt = edgeImitator.findMessageByType(UserUpdateMsg.class);
        Assert.assertTrue(userUpdateMsgOpt.isPresent());
        UserUpdateMsg userUpdateMsg = userUpdateMsgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, userUpdateMsg.getMsgType());
        UUID userUUID = new UUID(userUpdateMsg.getIdMSB(), userUpdateMsg.getIdLSB());
        User user = doGet("/api/user/" + userUUID, User.class);
        Assert.assertNotNull(user);
        Assert.assertEquals("testtenant@thingsboard.org", user.getEmail());
        testAutoGeneratedCodeByProtobuf(userUpdateMsg);
    }

    /**
     * 功能：校验客户。
     * 参数：无。
     * 返回：无。
     */
    private void validatePublicCustomer() throws Exception {
        Optional<CustomerUpdateMsg> customerUpdateMsgOpt = edgeImitator.findMessageByType(CustomerUpdateMsg.class);
        Assert.assertTrue(customerUpdateMsgOpt.isPresent());
        CustomerUpdateMsg customerUpdateMsg = customerUpdateMsgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, customerUpdateMsg.getMsgType());
        UUID customerUUID = new UUID(customerUpdateMsg.getIdMSB(), customerUpdateMsg.getIdLSB());
        Customer customer = doGet("/api/customer/" + customerUUID, Customer.class);
        Assert.assertNotNull(customer);
        Assert.assertTrue(customer.isPublic());
    }

    /**
     * 功能：校验`Sync Completed`。
     * 参数：无。
     * 返回：无。
     */
    private void validateSyncCompleted() {
        Optional<SyncCompletedMsg> syncCompletedMsgOpt = edgeImitator.findMessageByType(SyncCompletedMsg.class);
        Assert.assertTrue(syncCompletedMsgOpt.isPresent());
    }

    /**
     * 功能：保存或创建设备。
     * 参数：无。
     * 返回：处理结果。
     */
    protected Device saveDeviceOnCloudAndVerifyDeliveryToEdge() throws Exception {
        // create device and assign to edge
        Device savedDevice = saveDevice(StringUtils.randomAlphanumeric(15), thermostatDeviceProfile.getName());
        edgeImitator.expectMessageAmount(2); // device and device profile messages
        doPost("/api/edge/" + edge.getUuidId()
                + "/device/" + savedDevice.getUuidId(), Device.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        Optional<DeviceUpdateMsg> deviceUpdateMsgOpt = edgeImitator.findMessageByType(DeviceUpdateMsg.class);
        Assert.assertTrue(deviceUpdateMsgOpt.isPresent());
        DeviceUpdateMsg deviceUpdateMsg = deviceUpdateMsgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, deviceUpdateMsg.getMsgType());
        Assert.assertEquals(savedDevice.getUuidId().getMostSignificantBits(), deviceUpdateMsg.getIdMSB());
        Assert.assertEquals(savedDevice.getUuidId().getLeastSignificantBits(), deviceUpdateMsg.getIdLSB());

        Optional<DeviceProfileUpdateMsg> deviceProfileUpdateMsgOpt = edgeImitator.findMessageByType(DeviceProfileUpdateMsg.class);
        Assert.assertTrue(deviceProfileUpdateMsgOpt.isPresent());
        DeviceProfileUpdateMsg deviceProfileUpdateMsg = deviceProfileUpdateMsgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, deviceProfileUpdateMsg.getMsgType());
        Assert.assertEquals(thermostatDeviceProfile.getUuidId().getMostSignificantBits(), deviceProfileUpdateMsg.getIdMSB());
        Assert.assertEquals(thermostatDeviceProfile.getUuidId().getLeastSignificantBits(), deviceProfileUpdateMsg.getIdLSB());
        return savedDevice;
    }

    /**
     * 功能：获取设备。
     * 参数：
     * - `deviceName`：设备信息或设备标识。
     * 返回：处理结果。
     */
    protected Device findDeviceByName(String deviceName) throws Exception {
        List<DeviceInfo> edgeDevices = doGetTypedWithPageLink("/api/edge/" + edge.getUuidId() + "/devices?",
                new TypeReference<PageData<DeviceInfo>>() {
                }, new PageLink(100)).getData();
        Optional<DeviceInfo> foundDevice = edgeDevices.stream().filter(d -> d.getName().equals(deviceName)).findAny();
        Assert.assertTrue(foundDevice.isPresent());
        Device device = foundDevice.get();
        Assert.assertEquals(deviceName, device.getName());
        return device;
    }

    /**
     * 功能：获取资产。
     * 参数：
     * - `assetName`：名称。
     * 返回：匹配的数据集合。
     */
    protected Asset findAssetByName(String assetName) throws Exception {
        List<Asset> edgeAssets = doGetTypedWithPageLink("/api/edge/" + edge.getUuidId() + "/assets?",
                new TypeReference<PageData<Asset>>() {
                }, new PageLink(100)).getData();

        Assert.assertEquals(1, edgeAssets.size());
        Asset asset = edgeAssets.get(0);
        Assert.assertEquals(assetName, asset.getName());
        return asset;
    }

    /**
     * 功能：保存或创建设备。
     * 参数：
     * - `deviceName`：设备信息或设备标识。
     * - `type`：类型。
     * 返回：处理结果。
     */
    protected Device saveDevice(String deviceName, String type) {
        Device device = new Device();
        device.setName(deviceName);
        device.setType(type);
        return doPost("/api/device", device, Device.class);
    }

    /**
     * 功能：保存或创建资产。
     * 参数：
     * - `assetName`：名称。
     * 返回：匹配的数据集合。
     */
    protected Asset saveAsset(String assetName) {
        Asset asset = new Asset();
        asset.setName(assetName);
        return doPost("/api/asset", asset, Asset.class);
    }

    /**
     * 功能：保存或创建信息对象。
     * 参数：
     * - `deviceProfileId`：设备配置ID。
     * - `type`：类型。
     * 返回：处理结果。
     */
    protected OtaPackageInfo saveOtaPackageInfo(DeviceProfileId deviceProfileId, OtaPackageType type) {
        SaveOtaPackageInfoRequest firmwareInfo = new SaveOtaPackageInfoRequest();
        firmwareInfo.setDeviceProfileId(deviceProfileId);
        firmwareInfo.setType(type);
        firmwareInfo.setTitle(type.name() + " Edge " + StringUtils.randomAlphanumeric(3));
        firmwareInfo.setVersion("v1.0");
        firmwareInfo.setTag("My " + type.name() + " #1 v1.0");
        firmwareInfo.setUsesUrl(true);
        firmwareInfo.setUrl("http://localhost:8080/v1/package");
        firmwareInfo.setAdditionalInfo(JacksonUtil.newObjectNode());
        firmwareInfo.setChecksumAlgorithm(ChecksumAlgorithm.SHA256);
        return doPost("/api/otaPackage", firmwareInfo, OtaPackageInfo.class);
    }

    /**
     * 功能：执行 `constructEdgeEvent` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * - `edgeEventAction`：`edgeEventAction` 参数。
     * - `entityId`：实体IDID。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    protected EdgeEvent constructEdgeEvent(TenantId tenantId, EdgeId edgeId, EdgeEventActionType edgeEventAction,
                                           UUID entityId, EdgeEventType edgeEventType, JsonNode entityBody) {
        EdgeEvent edgeEvent = new EdgeEvent();
        edgeEvent.setEdgeId(edgeId);
        edgeEvent.setTenantId(tenantId);
        edgeEvent.setAction(edgeEventAction);
        edgeEvent.setEntityId(entityId);
        edgeEvent.setType(edgeEventType);
        edgeEvent.setBody(entityBody);
        return edgeEvent;
    }

    /**
     * 功能：验证编码相关场景。
     * 参数：
     * - `builder`：`builder` 参数。
     * 返回：无。
     */
    protected void testAutoGeneratedCodeByProtobuf(MessageLite.Builder builder) throws InvalidProtocolBufferException {
        MessageLite source = builder.build();

        testAutoGeneratedCodeByProtobuf(source);

        MessageLite target = source.getParserForType().parseFrom(source.toByteArray());
        builder.clear().mergeFrom(target);
    }

    /**
     * 功能：验证编码相关场景。
     * 参数：
     * - `source`：`source` 参数。
     * 返回：无。
     */
    protected void testAutoGeneratedCodeByProtobuf(MessageLite source) throws InvalidProtocolBufferException {
        MessageLite target = source.getParserForType().parseFrom(source.toByteArray());
        Assert.assertEquals(source, target);
        Assert.assertEquals(source.hashCode(), target.hashCode());
    }

    /**
     * 功能：保存或创建规则链。
     * 参数：
     * - `ruleChainName`：名称。
     * 返回：处理结果。
     */
    protected RuleChainId createEdgeRuleChainAndAssignToEdge(String ruleChainName) throws Exception {
        edgeImitator.expectMessageAmount(1);
        RuleChain ruleChain = new RuleChain();
        ruleChain.setName(ruleChainName);
        ruleChain.setType(RuleChainType.EDGE);
        RuleChain savedRuleChain = doPost("/api/ruleChain", ruleChain, RuleChain.class);
        doPost("/api/edge/" + edge.getUuidId()
                + "/ruleChain/" + savedRuleChain.getUuidId(), RuleChain.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        return savedRuleChain.getId();
    }

    /**
     * 功能：执行 `unAssignFromEdgeAndDeleteRuleChain` 对应的处理。
     * 参数：
     * - `ruleChainId`：规则链ID。
     * 返回：无。
     */
    protected void unAssignFromEdgeAndDeleteRuleChain(RuleChainId ruleChainId) throws Exception {
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/edge/" + edge.getUuidId()
                + "/ruleChain/" + ruleChainId.getId(), RuleChain.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        // delete rule chain
        doDelete("/api/ruleChain/" + ruleChainId.getId())
                .andExpect(status().isOk());
    }

    /**
     * 功能：保存或创建仪表盘。
     * 参数：
     * - `dashboardName`：名称。
     * 返回：处理结果。
     */
    protected DashboardId createDashboardAndAssignToEdge(String dashboardName) throws Exception {
        edgeImitator.expectMessageAmount(1);
        Dashboard dashboard = new Dashboard();
        dashboard.setTitle(dashboardName);
        Dashboard savedDashboard = doPost("/api/dashboard", dashboard, Dashboard.class);
        doPost("/api/edge/" + edge.getUuidId()
                + "/dashboard/" + savedDashboard.getUuidId(), Dashboard.class);
        Assert.assertTrue(edgeImitator.waitForMessages());
        return savedDashboard.getId();
    }

    /**
     * 功能：执行 `unAssignFromEdgeAndDeleteDashboard` 对应的处理。
     * 参数：
     * - `dashboardId`：仪表盘IDID。
     * 返回：无。
     */
    protected void unAssignFromEdgeAndDeleteDashboard(DashboardId dashboardId) throws Exception {
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/edge/" + edge.getUuidId()
                + "/dashboard/" + dashboardId.getId(), RuleChain.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        // delete dashboard
        doDelete("/api/dashboard/" + dashboardId.getId())
                .andExpect(status().isOk());
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`AbstractEdgeTest` 在 ThingsBoard Application 测试模块 中承担测试支撑类型职责，核心目的是验证 Application 模块的控制器、服务、Actor 或集成流程。
 * 2. 核心流程：准备输入和依赖，调用被测对象并断言结果。
 * 3. 关键依赖：主要依赖或协作对象包括JUnit、Mockito、Spring Test、DAO、缓存、Transport 和测试夹具。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
