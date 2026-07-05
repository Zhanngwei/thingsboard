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
package org.thingsboard.server.transport.lwm2m.rpc;

import org.junit.Before;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MDeviceCredentials;
import org.thingsboard.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.transport.lwm2m.AbstractLwM2MIntegrationTest;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import static org.eclipse.leshan.core.LwM2mId.ACCESS_CONTROL;
import static org.eclipse.leshan.core.LwM2mId.DEVICE;
import static org.eclipse.leshan.core.LwM2mId.FIRMWARE;
import static org.eclipse.leshan.core.LwM2mId.SERVER;
import static org.eclipse.leshan.core.LwM2mId.SOFTWARE_MANAGEMENT;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.BINARY_APP_DATA_CONTAINER;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MProfileBootstrapConfigType.NONE;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.OBJECT_ID_0;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.OBJECT_INSTANCE_ID_0;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.OBJECT_INSTANCE_ID_1;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_0;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_14;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_9;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_NAME_19_0_0;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_NAME_19_1_0;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_NAME_3_14;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.RESOURCE_ID_NAME_3_9;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.TEMPERATURE_SENSOR;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.resources;

/**
 * 中文说明：
 * 1. 类目的：`AbstractRpcLwM2MIntegrationTest` 是ThingsBoard Application 测试模块中的传输层测试或适配类型，用于验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 生命周期：由 JUnit 测试生命周期创建，随单个测试方法准备和清理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Integration Test / Fixture。
 */
@DaoSqlTest
public abstract class AbstractRpcLwM2MIntegrationTest extends AbstractLwM2MIntegrationTest {

    /**
     * RPC常量，用于统一引用固定值。
     */
    protected String OBSERVE_ATTRIBUTES_WITH_PARAMS_RPC;
    protected String deviceId;
    /**
     * `expectedObjects`集合，用于去重保存或快速判断对象是否存在。
     */
    public Set expectedObjects;
    public Set expectedObjectIdVers;
    /**
     * `expectedInstances`集合，用于去重保存或快速判断对象是否存在。
     */
    public Set expectedInstances;
    public Set expectedObjectIdVerInstances;

    /**
     * `objectInstanceIdVer_1` 字段，保存当前对象的对应属性。
     */
    protected String objectInstanceIdVer_1;
    protected String objectIdVer_0;
    /**
     * `objectIdVer_2` 字段，保存当前对象的对应属性。
     */
    protected String objectIdVer_2;
    private static final Predicate PREDICATE_3 = path -> (!((String) path).startsWith("/" + TEMPERATURE_SENSOR) && ((String) path).startsWith("/" + DEVICE));
    /**
     * `objectIdVer_3` 字段，保存当前对象的对应属性。
     */
    protected String objectIdVer_3;
    protected String objectInstanceIdVer_3;
    /**
     * `objectInstanceIdVer_5` 字段，保存当前对象的对应属性。
     */
    protected String objectInstanceIdVer_5;
    protected String objectInstanceIdVer_9;
    /**
     * `objectIdVer_19` 字段，保存当前对象的对应属性。
     */
    protected String objectIdVer_19;
    protected final String OBJECT_ID_VER_50 = "/50";
    /**
     * `objectIdVer_3303` 字段，保存当前对象的对应属性。
     */
    protected String objectIdVer_3303;
    protected static AtomicInteger endpointSequence = new AtomicInteger();
    /**
     * 设备常量，用于统一引用固定值。
     */
    protected static String DEVICE_ENDPOINT_RPC_PREF = "deviceEndpointRpc";
    protected String idVer_3_0_9;
    /**
     * `idVer_19_0_0` 字段，保存当前对象的对应属性。
     */
    protected String idVer_19_0_0;

    /**
     * 功能：创建 `AbstractRpcLwM2MIntegrationTest` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public AbstractRpcLwM2MIntegrationTest() {
        setResources(resources);
    }

    /**
     * 功能：初始化或启动RPC。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void startInitRPC() throws Exception {
        initRpc();
    }

    /**
     * 功能：初始化或启动RPC。
     * 参数：无。
     * 返回：无。
     */
    private void initRpc () throws Exception {
        String endpoint = DEVICE_ENDPOINT_RPC_PREF + endpointSequence.incrementAndGet();
        createNewClient(SECURITY_NO_SEC, COAP_CONFIG, true, endpoint, false, null);
        expectedObjects = ConcurrentHashMap.newKeySet();
        expectedObjectIdVers = ConcurrentHashMap.newKeySet();
        expectedInstances = ConcurrentHashMap.newKeySet();
        expectedObjectIdVerInstances = ConcurrentHashMap.newKeySet();
        lwM2MTestClient.getLeshanClient().getObjectTree().getObjectEnablers().forEach((key, val) -> {
            if (key > 0) {
                String objectVerId = "/" + key;
                objectVerId += ("_" + val.getObjectModel().version);
                expectedObjects.add("/" + key);
                expectedObjectIdVers.add(objectVerId);
                String finalObjectVerId = objectVerId;
                val.getAvailableInstanceIds().forEach(inststanceId -> {
                    expectedInstances.add("/" + key + "/" + inststanceId);
                    expectedObjectIdVerInstances.add(finalObjectVerId + "/" + inststanceId);
                });
            }
        });
        String ver_Id_0 = lwM2MTestClient.getLeshanClient().getObjectTree().getModel().getObjectModel(OBJECT_ID_0).version;
        objectIdVer_0 = "/" + OBJECT_ID_0 + "_" + ver_Id_0;
        objectIdVer_2 = (String) expectedObjectIdVers.stream().filter(path -> ((String) path).startsWith("/" + ACCESS_CONTROL)).findFirst().get();
        objectIdVer_3 = (String) expectedObjectIdVers.stream().filter(PREDICATE_3).findFirst().get();
        objectIdVer_19 = (String) expectedObjectIdVers.stream().filter(path -> ((String) path).startsWith("/" + BINARY_APP_DATA_CONTAINER)).findFirst().get();
        objectIdVer_3303 = (String) expectedObjectIdVers.stream().filter(path -> ((String) path).startsWith("/" + TEMPERATURE_SENSOR)).findFirst().get();
        objectInstanceIdVer_1 = (String) expectedObjectIdVerInstances.stream().filter(path -> (!((String) path).startsWith("/" + BINARY_APP_DATA_CONTAINER) && ((String) path).startsWith("/" + SERVER))).findFirst().get();
        objectInstanceIdVer_3 = (String) expectedObjectIdVerInstances.stream().filter(PREDICATE_3).findFirst().get();
        objectInstanceIdVer_5 = (String) expectedObjectIdVerInstances.stream().filter(path -> ((String) path).startsWith("/" + FIRMWARE)).findFirst().get();
        objectInstanceIdVer_9 = (String) expectedObjectIdVerInstances.stream().filter(path -> ((String) path).startsWith("/" + SOFTWARE_MANAGEMENT)).findFirst().get();

        idVer_3_0_9 = objectIdVer_3 + "/" + OBJECT_INSTANCE_ID_0 + "/" + RESOURCE_ID_9;
        idVer_19_0_0 = objectIdVer_19 + "/" + OBJECT_INSTANCE_ID_0 + "/" + RESOURCE_ID_0;

        OBSERVE_ATTRIBUTES_WITH_PARAMS_RPC =
                "    {\n" +
                        "    \"keyName\": {\n" +
                        "      \"" + idVer_3_0_9 + "\": \"" + RESOURCE_ID_NAME_3_9 + "\",\n" +
                        "      \"" + objectIdVer_3 + "/" + OBJECT_INSTANCE_ID_0 + "/" + RESOURCE_ID_14 + "\": \"" + RESOURCE_ID_NAME_3_14 + "\",\n" +
                        "      \"" + idVer_19_0_0 + "\": \"" + RESOURCE_ID_NAME_19_0_0 + "\",\n" +
                        "      \"" + objectIdVer_19 + "/" + OBJECT_INSTANCE_ID_1 + "/" + RESOURCE_ID_0 + "\": \"" + RESOURCE_ID_NAME_19_1_0 + "\"\n" +
                        "    },\n" +
                        "    \"observe\": [\n" +
                        "      \"" + idVer_3_0_9 + "\",\n" +
                        "      \"" + idVer_19_0_0 + "\"\n" +
                        "    ],\n" +
                        "    \"attribute\": [\n" +
                        "      \"" + objectIdVer_3 + "/" + OBJECT_INSTANCE_ID_0 + "/" + RESOURCE_ID_14 + "\"\n" +
                        "    ],\n" +
                        "    \"telemetry\": [\n" +
                        "      \"" + idVer_3_0_9 + "\",\n" +
                        "      \"" + idVer_19_0_0 + "\",\n" +
                        "      \"" + objectIdVer_19 + "/" + OBJECT_INSTANCE_ID_1 + "/" + RESOURCE_ID_0 + "\"\n" +
                        "    ],\n" +
                        "    \"attributeLwm2m\": {}\n" +
                        "  }";

        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = getTransportConfiguration(OBSERVE_ATTRIBUTES_WITH_PARAMS_RPC, getBootstrapServerCredentialsNoSec(NONE));
        createDeviceProfile(transportConfiguration);

        LwM2MDeviceCredentials deviceCredentials = getDeviceCredentialsNoSec(createNoSecClientCredentials(endpoint));
        final Device device = createDevice(deviceCredentials, endpoint);
        deviceId = device.getId().getId().toString();

        lwM2MTestClient.start(true);
        awaitObserveReadAll(2, false, device.getId().getId().toString());
    }

    /**
     * 功能：执行 `pathIdVerToObjectId` 对应的处理。
     * 参数：
     * - `pathIdVer`：文件或资源路径。
     * 返回：文本结果。
     */
    protected String pathIdVerToObjectId(String pathIdVer) {
        if (pathIdVer.contains("_")) {
            String[] objVer = pathIdVer.split("/");
            objVer[1] = objVer[1].split("_")[0];
            return String.join("/", objVer);
        }
        return pathIdVer;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`AbstractRpcLwM2MIntegrationTest` 在 ThingsBoard Application 测试模块 中承担传输层测试或适配类型职责，核心目的是验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 核心流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
 * 3. 关键依赖：主要依赖或协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
