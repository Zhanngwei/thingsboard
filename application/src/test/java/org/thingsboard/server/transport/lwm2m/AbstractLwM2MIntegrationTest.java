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
package org.thingsboard.server.transport.lwm2m;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.leshan.client.californium.LeshanClient;
import org.eclipse.leshan.client.object.Security;
import org.eclipse.leshan.core.ResponseCode;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.SocketUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MBootstrapClientCredentials;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MClientCredential;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MDeviceCredentials;
import org.thingsboard.server.common.data.device.credentials.lwm2m.NoSecBootstrapClientCredential;
import org.thingsboard.server.common.data.device.credentials.lwm2m.NoSecClientCredential;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.device.profile.DisabledDeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.lwm2m.OtherConfiguration;
import org.thingsboard.server.common.data.device.profile.lwm2m.TelemetryMappingConfiguration;
import org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap.AbstractLwM2MBootstrapServerCredential;
import org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap.LwM2MBootstrapServerCredential;
import org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap.NoSecLwM2MBootstrapServerCredential;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.SingleEntityFilter;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.service.ws.telemetry.cmd.TelemetryCmdsWrapper;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.EntityDataCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.EntityDataUpdate;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.LatestValueCmd;
import org.thingsboard.server.transport.AbstractTransportIntegrationTest;
import org.thingsboard.server.transport.lwm2m.client.LwM2MTestClient;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClientContext;
import org.thingsboard.server.transport.lwm2m.server.uplink.LwM2mUplinkMsgHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.eclipse.californium.core.config.CoapConfig.COAP_PORT;
import static org.eclipse.californium.core.config.CoapConfig.COAP_SECURE_PORT;
import static org.eclipse.leshan.client.object.Security.noSec;
import static org.eclipse.leshan.client.object.Security.noSecBootstap;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_BOOTSTRAP_STARTED;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_BOOTSTRAP_SUCCESS;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_INIT;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_REGISTRATION_STARTED;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_REGISTRATION_SUCCESS;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MProfileBootstrapConfigType;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MProfileBootstrapConfigType.NONE;

/**
 * 中文说明：
 * 1. `AbstractLwM2MIntegrationTest` 是 ThingsBoard Application 中验证 `AbstractLwM2MIntegration` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractTransportIntegrationTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@TestPropertySource(properties = {
        "transport.lwm2m.enabled=true",
})
@Slf4j
@DaoSqlTest
public abstract class AbstractLwM2MIntegrationTest extends AbstractTransportIntegrationTest {

    /**
     * 消息，负责处理对应任务或消息。
     */
    @SpyBean
    LwM2mUplinkMsgHandler defaultLwM2mUplinkMsgHandlerTest;

    /**
     * 上下文，用于发起外部调用或协议交互。
     */
    @Autowired
    private LwM2mClientContext clientContextTest;

    //  Lwm2m Server
    /**
     * 端口号常量，用于统一引用固定值。
     */
    public static final int port = 5685;
    public static final int securityPort = 5686;
    /**
     * 端口号常量，用于统一引用固定值。
     */
    public static final int portBs = 5687;
    public static final int securityPortBs = 5688;
    /**
     * 端口号常量，用于统一引用固定值。
     */
    public static final int[] SERVERS_PORT_NUMBERS = {port, securityPort, portBs, securityPortBs};

    /**
     * 主机地址常量，用于统一引用固定值。
     */
    public static final String host = "localhost";
    public static final String hostBs = "localhost";
    /**
     * 服务端常量，用于统一引用固定值。
     */
    public static final int shortServerId = 123;
    public static final int shortServerIdBs = 111;
    /**
     * `COAP`常量，用于统一引用固定值。
     */
    public static final String COAP = "coap://";
    public static final String COAPS = "coaps://";
    /**
     * URI 地址常量，用于统一引用固定值。
     */
    public static final String URI = COAP + host + ":" + port;
    public static final String SECURE_URI = COAPS + host + ":" + securityPort;
    /**
     * URI 地址常量，用于统一引用固定值。
     */
    public static final String URI_BS = COAP + hostBs + ":" + portBs;
    public static final String SECURE_URI_BS = COAPS + hostBs + ":" + securityPortBs;
    public static final Configuration COAP_CONFIG = new Configuration().set(COAP_PORT, port).set(COAP_SECURE_PORT, securityPort);
    public static Configuration COAP_CONFIG_BS = new Configuration().set(COAP_PORT, portBs).set(COAP_SECURE_PORT, securityPortBs);
    public static final Security SECURITY_NO_SEC = noSec(URI, shortServerId);
    public static final Security SECURITY_NO_SEC_BS = noSecBootstap(URI_BS);

    protected final String OBSERVE_ATTRIBUTES_WITHOUT_PARAMS =
            "    {\n" +
                    "    \"keyName\": {},\n" +
                    "    \"observe\": [],\n" +
                    "    \"attribute\": [],\n" +
                    "    \"telemetry\": [],\n" +
                    "    \"attributeLwm2m\": {}\n" +
                    "  }";

    public static final String OBSERVE_ATTRIBUTES_WITH_PARAMS =

            "    {\n" +
                    "    \"keyName\": {\n" +
                    "      \"/3_1.0/0/9\": \"batteryLevel\"\n" +
                    "    },\n" +
                    "    \"observe\": [],\n" +
                    "    \"attribute\": [\n" +
                    "    ],\n" +
                    "    \"telemetry\": [\n" +
                    "      \"/3_1.0/0/9\"\n" +
                    "    ],\n" +
                    "    \"attributeLwm2m\": {}\n" +
                    "  }";

    public static final String CLIENT_LWM2M_SETTINGS =
            "     {\n" +
                    "    \"edrxCycle\": null,\n" +
                    "    \"powerMode\": \"DRX\",\n" +
                    "    \"fwUpdateResource\": null,\n" +
                    "    \"fwUpdateStrategy\": 1,\n" +
                    "    \"psmActivityTimer\": null,\n" +
                    "    \"swUpdateResource\": null,\n" +
                    "    \"swUpdateStrategy\": 1,\n" +
                    "    \"pagingTransmissionWindow\": null,\n" +
                    "    \"clientOnlyObserveAfterConnect\": 1\n" +
                    "  }";

    protected final Set<Lwm2mTestHelper.LwM2MClientState> expectedStatusesBsSuccess = new HashSet<>(Arrays.asList(ON_INIT, ON_BOOTSTRAP_STARTED, ON_BOOTSTRAP_SUCCESS));
    protected final Set<Lwm2mTestHelper.LwM2MClientState> expectedStatusesRegistrationLwm2mSuccess = new HashSet<>(Arrays.asList(ON_INIT, ON_REGISTRATION_STARTED, ON_REGISTRATION_SUCCESS));
    protected final Set<Lwm2mTestHelper.LwM2MClientState> expectedStatusesRegistrationBsSuccess = new HashSet<>(Arrays.asList(ON_BOOTSTRAP_STARTED, ON_BOOTSTRAP_SUCCESS, ON_REGISTRATION_STARTED, ON_REGISTRATION_SUCCESS));
    /**
     * 设备配置，保存当前对象的配置选项。
     */
    protected DeviceProfile deviceProfile;
    protected ScheduledExecutorService executor;
    /**
     * 客户端，用于发起外部调用或协议交互。
     */
    protected LwM2MTestClient lwM2MTestClient;
    private String[] resources;

    /**
     * 功能：初始化或启动`Init`。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void startInit() throws Exception {
        init();
    }

    /**
     * 功能：执行 `after` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @After
    public void after() {
        clientDestroy();
        executor.shutdownNow();
    }

    /**
     * 功能：执行 `afterClass` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @AfterClass
    public static void afterClass() {
        awaitServersDestroy();
    }

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    private void init() throws Exception {
        executor = Executors.newScheduledThreadPool(10, ThingsBoardThreadFactory.forName("test-lwm2m-scheduled"));
        loginTenantAdmin();
        for (String resourceName : this.resources) {
            TbResource lwModel = new TbResource();
            lwModel.setResourceType(ResourceType.LWM2M_MODEL);
            lwModel.setTitle(resourceName);
            lwModel.setFileName(resourceName);
            lwModel.setTenantId(tenantId);
            byte[] bytes = IOUtils.toByteArray(AbstractLwM2MIntegrationTest.class.getClassLoader().getResourceAsStream("lwm2m/" + resourceName));
            lwModel.setData(bytes);
            lwModel = doPostWithTypedResponse("/api/resource", lwModel, new TypeReference<>() {
            });
            Assert.assertNotNull(lwModel);
        }
    }

    /**
     * 功能：执行 `basicTestConnectionObserveTelemetry` 对应的处理。
     * 参数：
     * - `security`：`security` 参数。
     * - `deviceCredentials`：设备信息或设备标识。
     * - `coapConfig`：配置对象。
     * - `endpoint`：`endpoint` 参数。
     * 返回：无。
     */
    public void basicTestConnectionObserveTelemetry(Security security,
                                                    LwM2MDeviceCredentials deviceCredentials,
                                                    Configuration coapConfig,
                                                    String endpoint) throws Exception {
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = getTransportConfiguration(OBSERVE_ATTRIBUTES_WITH_PARAMS, getBootstrapServerCredentialsNoSec(NONE));
        createDeviceProfile(transportConfiguration);
        Device device = createDevice(deviceCredentials, endpoint);

        SingleEntityFilter sef = new SingleEntityFilter();
        sef.setSingleEntity(device.getId());
        LatestValueCmd latestCmd = new LatestValueCmd();
        latestCmd.setKeys(Collections.singletonList(new EntityKey(EntityKeyType.TIME_SERIES, "batteryLevel")));
        EntityDataQuery edq = new EntityDataQuery(sef, new EntityDataPageLink(1, 0, null, null),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

        EntityDataCmd cmd = new EntityDataCmd(1, edq, null, latestCmd, null);
        getWsClient().send(cmd);
        getWsClient().waitForReply();

        getWsClient().registerWaitForUpdate();
        createNewClient(security, coapConfig, false, endpoint, false, null);
        awaitObserveReadAll(0, false, device.getId().getId().toString());
        String msg = getWsClient().waitForUpdate();

        EntityDataUpdate update = JacksonUtil.fromString(msg, EntityDataUpdate.class);
        Assert.assertEquals(1, update.getCmdId());
        List<EntityData> eData = update.getUpdate();
        Assert.assertNotNull(eData);
        Assert.assertEquals(1, eData.size());
        Assert.assertEquals(device.getId(), eData.get(0).getEntityId());
        Assert.assertNotNull(eData.get(0).getLatest().get(EntityKeyType.TIME_SERIES));
        var tsValue = eData.get(0).getLatest().get(EntityKeyType.TIME_SERIES).get("batteryLevel");
        Assert.assertThat(Long.parseLong(tsValue.getValue()), instanceOf(Long.class));
        int expectedMax = 50;
        int expectedMin = 5;
        Assert.assertTrue(expectedMax >= Long.parseLong(tsValue.getValue()));
        Assert.assertTrue(expectedMin <= Long.parseLong(tsValue.getValue()));
    }

    /**
     * 功能：保存或创建设备配置。
     * 参数：
     * - `transportConfiguration`：配置对象。
     * 返回：无。
     */
    protected void createDeviceProfile(Lwm2mDeviceProfileTransportConfiguration transportConfiguration) throws Exception {
        deviceProfile = new DeviceProfile();
        deviceProfile.setName("LwM2M");
        deviceProfile.setType(DeviceProfileType.DEFAULT);
        deviceProfile.setTenantId(tenantId);
        deviceProfile.setTransportType(DeviceTransportType.LWM2M);
        deviceProfile.setProvisionType(DeviceProfileProvisionType.DISABLED);
        deviceProfile.setDescription(deviceProfile.getName());

        DeviceProfileData deviceProfileData = new DeviceProfileData();
        deviceProfileData.setConfiguration(new DefaultDeviceProfileConfiguration());
        deviceProfileData.setProvisionConfiguration(new DisabledDeviceProfileProvisionConfiguration(null));
        deviceProfileData.setTransportConfiguration(transportConfiguration);
        deviceProfile.setProfileData(deviceProfileData);

        deviceProfile = doPost("/api/deviceProfile", deviceProfile, DeviceProfile.class);
        Assert.assertNotNull(deviceProfile);
    }

    /**
     * 功能：保存或创建设备。
     * 参数：
     * - `credentials`：`credentials` 参数。
     * - `endpoint`：`endpoint` 参数。
     * 返回：处理结果。
     */
    protected Device createDevice(LwM2MDeviceCredentials credentials, String endpoint) throws Exception {
        Device device = new Device();
        device.setName(endpoint);
        device.setDeviceProfileId(deviceProfile.getId());
        device.setTenantId(tenantId);
        device = doPost("/api/device", device, Device.class);
        Assert.assertNotNull(device);

        DeviceCredentials deviceCredentials =
                doGet("/api/device/" + device.getId().getId().toString() + "/credentials", DeviceCredentials.class);
        Assert.assertEquals(device.getId(), deviceCredentials.getDeviceId());
        deviceCredentials.setCredentialsType(DeviceCredentialsType.LWM2M_CREDENTIALS);
        deviceCredentials.setCredentialsValue(JacksonUtil.toString(credentials));
        doPost("/api/device/credentials", deviceCredentials).andExpect(status().isOk());
        return device;
    }

    /**
     * 功能：保存或创建凭据。
     * 参数：
     * - `endpoint`：`endpoint` 参数。
     * 返回：处理结果。
     */
    public NoSecClientCredential createNoSecClientCredentials(String endpoint) {
        NoSecClientCredential clientCredentials = new NoSecClientCredential();
        clientCredentials.setEndpoint(endpoint);
        return clientCredentials;
    }

    /**
     * 功能：更新`Resources`。
     * 参数：
     * - `resources`：`resources` 参数。
     * 返回：无。
     */
    public void setResources(String[] resources) {
        this.resources = resources;
    }

    /**
     * 功能：保存或创建客户端。
     * 参数：
     * - `security`：`security` 参数。
     * - `coapConfig`：配置对象。
     * - `isRpc`：`isRpc` 参数。
     * - `endpoint`：`endpoint` 参数。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    public void createNewClient(Security security, Configuration coapConfig, boolean isRpc, String endpoint, boolean isBootstrap, Security securityBs) throws Exception {
        this.clientDestroy();
        lwM2MTestClient = new LwM2MTestClient(this.executor, endpoint);
        int clientPort = SocketUtils.findAvailableUdpPort();
        lwM2MTestClient.init(security, coapConfig, clientPort, isRpc, isBootstrap, this.shortServerId, this.shortServerIdBs,
                securityBs, this.defaultLwM2mUplinkMsgHandlerTest, this.clientContextTest);
    }

    /**
     * 功能：执行 `clientDestroy` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    private void clientDestroy() {
        try {
            if (lwM2MTestClient != null) {
                lwM2MTestClient.destroy();
                awaitClientDestroy(lwM2MTestClient.getLeshanClient());
            }
        } catch (Exception e) {
            log.error("Failed client Destroy", e);
        }
    }

    /**
     * 功能：获取传输层。
     * 参数：
     * - `observeAttr`：`observeAttr` 参数。
     * - `bootstrapServerCredentials`：数据列表。
     * 返回：处理结果。
     */
    protected Lwm2mDeviceProfileTransportConfiguration getTransportConfiguration(String observeAttr, List<LwM2MBootstrapServerCredential> bootstrapServerCredentials) {
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = new Lwm2mDeviceProfileTransportConfiguration();
        TelemetryMappingConfiguration observeAttrConfiguration = JacksonUtil.fromString(observeAttr, TelemetryMappingConfiguration.class);
        OtherConfiguration clientLwM2mSettings = JacksonUtil.fromString(CLIENT_LWM2M_SETTINGS, OtherConfiguration.class);
        transportConfiguration.setBootstrapServerUpdateEnable(true);
        transportConfiguration.setObserveAttr(observeAttrConfiguration);
        transportConfiguration.setClientLwM2mSettings(clientLwM2mSettings);
        transportConfiguration.setBootstrap(bootstrapServerCredentials);
        return transportConfiguration;
    }

    /**
     * 功能：获取凭据。
     * 参数：
     * - `bootstrapConfigType`：配置对象。
     * 返回：匹配的数据集合。
     */
    protected List<LwM2MBootstrapServerCredential> getBootstrapServerCredentialsNoSec(LwM2MProfileBootstrapConfigType bootstrapConfigType) {
        List<LwM2MBootstrapServerCredential> bootstrap = new ArrayList<>();
        switch (bootstrapConfigType) {
            case BOTH:
                bootstrap.add(getBootstrapServerCredentialNoSec(false));
                bootstrap.add(getBootstrapServerCredentialNoSec(true));
                break;
            case BOOTSTRAP_ONLY:
                bootstrap.add(getBootstrapServerCredentialNoSec(true));
                break;
            case LWM2M_ONLY:
                bootstrap.add(getBootstrapServerCredentialNoSec(false));
                break;
            case NONE:
        }
        return bootstrap;
    }

    /**
     * 功能：获取服务端。
     * 参数：
     * - `isBootstrap`：`isBootstrap` 参数。
     * 返回：处理结果。
     */
    private AbstractLwM2MBootstrapServerCredential getBootstrapServerCredentialNoSec(boolean isBootstrap) {
        AbstractLwM2MBootstrapServerCredential bootstrapServerCredential = new NoSecLwM2MBootstrapServerCredential();
        bootstrapServerCredential.setServerPublicKey("");
        bootstrapServerCredential.setShortServerId(isBootstrap ? shortServerIdBs : shortServerId);
        bootstrapServerCredential.setBootstrapServerIs(isBootstrap);
        bootstrapServerCredential.setHost(isBootstrap ? hostBs : host);
        bootstrapServerCredential.setPort(isBootstrap ? portBs : port);
        return bootstrapServerCredential;
    }

    /**
     * 功能：获取设备凭据。
     * 参数：
     * - `clientCredentials`：客户端对象。
     * 返回：处理结果。
     */
    protected LwM2MDeviceCredentials getDeviceCredentialsNoSec(LwM2MClientCredential clientCredentials) {
        LwM2MDeviceCredentials credentials = new LwM2MDeviceCredentials();
        credentials.setClient(clientCredentials);
        LwM2MBootstrapClientCredentials bootstrapCredentials = new LwM2MBootstrapClientCredentials();
        NoSecBootstrapClientCredential serverCredentials = new NoSecBootstrapClientCredential();
        bootstrapCredentials.setBootstrapServer(serverCredentials);
        bootstrapCredentials.setLwm2mServer(serverCredentials);
        credentials.setBootstrap(bootstrapCredentials);
        return credentials;
    }

    /**
     * 功能：执行 `awaitServersDestroy` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    private static void awaitServersDestroy() {
        await("One of servers ports number is not free")
                .atMost(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .until(() -> isServerPortsAvailable() == null);
    }

    /**
     * 功能：判断服务端。
     * 参数：无。
     * 返回：判断结果。
     */
    private static String isServerPortsAvailable() {
        for (int port : SERVERS_PORT_NUMBERS) {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                serverSocket.close();
                Assert.assertEquals(true, serverSocket.isClosed());
            } catch (IOException e) {
                log.warn(String.format("Port %n still in use", port));
                return (String.format("Port %n still in use", port));
            }
        }
        return null;
    }

    /**
     * 功能：执行 `awaitClientDestroy` 对应的处理。
     * 参数：
     * - `leshanClient`：客户端对象。
     * 返回：无。
     */
    private static void awaitClientDestroy(LeshanClient leshanClient) {
        await("Destroy LeshanClient: delete All is registered Servers.")
                .atMost(DEFAULT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .until(() -> leshanClient.getRegisteredServers().size() == 0);
    }

    /**
     * 功能：执行 `awaitObserveReadAll` 对应的处理。
     * 参数：
     * - `cntObserve`：`cntObserve` 参数。
     * - `isBootstrap`：`isBootstrap` 参数。
     * - `deviceIdStr`：设备信息或设备标识。
     * 返回：无。
     */
    protected  void awaitObserveReadAll(int cntObserve, boolean isBootstrap, String deviceIdStr) throws Exception {
        if (!isBootstrap) {
            await("ObserveReadAll after start client: countObserve " + cntObserve)
                    .atMost(40, TimeUnit.SECONDS)
                    .until(() -> {
                        String actualResultReadAll = sendObserve("ObserveReadAll", null, deviceIdStr);
                        ObjectNode rpcActualResultReadAll = JacksonUtil.fromString(actualResultReadAll, ObjectNode.class);
                        Assert.assertEquals(ResponseCode.CONTENT.getName(), rpcActualResultReadAll.get("result").asText());
                        String actualValuesReadAll = rpcActualResultReadAll.get("value").asText();
                        log.warn("ObserveReadAll:  [{}]", actualValuesReadAll);
                        int actualCntObserve = "[]".equals(actualValuesReadAll) ? 0 : actualValuesReadAll.split(",").length;
                        return cntObserve == actualCntObserve;
                    });
        }
    }

    /**
     * 功能：发送或提交`Observe`。
     * 参数：
     * - `method`：`method` 参数。
     * - `params`：`params` 参数。
     * - `deviceIdStr`：设备信息或设备标识。
     * 返回：文本结果。
     */
    protected String sendObserve(String method, String params, String deviceIdStr) throws Exception {
        String sendRpcRequest;
        if (params == null) {
            sendRpcRequest = "{\"method\": \"" + method + "\"}";
        }
        else {
            sendRpcRequest = "{\"method\": \"" + method + "\", \"params\": {\"id\": \"" + params + "\"}}";
        }
        return doPostAsync("/api/plugins/rpc/twoway/" + deviceIdStr, sendRpcRequest, String.class, status().isOk());
    }

}
