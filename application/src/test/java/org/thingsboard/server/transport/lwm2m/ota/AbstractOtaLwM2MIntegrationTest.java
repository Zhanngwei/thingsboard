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
package org.thingsboard.server.transport.lwm2m.ota;

import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.transport.lwm2m.AbstractLwM2MIntegrationTest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.common.data.ota.OtaPackageType.FIRMWARE;
import static org.thingsboard.server.common.data.ota.OtaPackageType.SOFTWARE;

/**
 * 中文说明：
 * 1. 类目的：`AbstractOtaLwM2MIntegrationTest` 是ThingsBoard Application 测试模块中的传输层测试或适配类型，用于验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 生命周期：由 JUnit 测试生命周期创建，随单个测试方法准备和清理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Integration Test / Fixture。
 */
@DaoSqlTest
public abstract class AbstractOtaLwM2MIntegrationTest extends AbstractLwM2MIntegrationTest {

    /**
     * `RESOURCES_OTA`常量，用于统一引用固定值。
     */
    private final  String[] RESOURCES_OTA = new String[]{"3.xml", "5.xml", "9.xml"};
    protected static final String CLIENT_ENDPOINT_WITHOUT_FW_INFO = "WithoutFirmwareInfoDevice";
    /**
     * 客户端常量，用于统一引用固定值。
     */
    protected static final String CLIENT_ENDPOINT_OTA5 = "Ota5_Device";
    protected static final String CLIENT_ENDPOINT_OTA9 = "Ota9_Device";

    /**
     * 功能：创建 `AbstractOtaLwM2MIntegrationTest` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public AbstractOtaLwM2MIntegrationTest() {
        setResources(this.RESOURCES_OTA);
    }

    /**
     * 功能：保存或创建`Firmware`。
     * 参数：无。
     * 返回：处理结果。
     */
    protected OtaPackageInfo createFirmware() throws Exception {
        String CHECKSUM = "4bf5122f344554c53bde2ebb8cd2b7e3d1600ad631c385a5d7cce23c7785459a";

        OtaPackageInfo firmwareInfo = new OtaPackageInfo();
        firmwareInfo.setDeviceProfileId(deviceProfile.getId());
        firmwareInfo.setType(FIRMWARE);
        firmwareInfo.setTitle("My firmware");
        firmwareInfo.setVersion("v1.0");

        OtaPackageInfo savedFirmwareInfo = doPost("/api/otaPackage", firmwareInfo, OtaPackageInfo.class);

        MockMultipartFile testData = new MockMultipartFile("file", "filename.txt", "text/plain", new byte[]{1});

        return savaData("/api/otaPackage/" + savedFirmwareInfo.getId().getId().toString() + "?checksum={checksum}&checksumAlgorithm={checksumAlgorithm}", testData, CHECKSUM, "SHA256");
    }

    /**
     * 功能：保存或创建`Software`。
     * 参数：无。
     * 返回：处理结果。
     */
    protected OtaPackageInfo createSoftware() throws Exception {
        String CHECKSUM = "4bf5122f344554c53bde2ebb8cd2b7e3d1600ad631c385a5d7cce23c7785459a";

        OtaPackageInfo swInfo = new OtaPackageInfo();
        swInfo.setDeviceProfileId(deviceProfile.getId());
        swInfo.setType(SOFTWARE);
        swInfo.setTitle("My sw");
        swInfo.setVersion("v1.0");

        OtaPackageInfo savedFirmwareInfo = doPost("/api/otaPackage", swInfo, OtaPackageInfo.class);

        MockMultipartFile testData = new MockMultipartFile("file", "filename.txt", "text/plain", new byte[]{1});

        return savaData("/api/otaPackage/" + savedFirmwareInfo.getId().getId().toString() + "?checksum={checksum}&checksumAlgorithm={checksumAlgorithm}", testData, CHECKSUM, "SHA256");
    }

    /**
     * 功能：执行 `savaData` 对应的处理。
     * 参数：
     * - `urlTemplate`：`urlTemplate` 参数。
     * - `content`：`content` 参数。
     * - `params`：`params` 参数。
     * 返回：处理结果。
     */
    protected OtaPackageInfo savaData(String urlTemplate, MockMultipartFile content, String... params) throws Exception {
        MockMultipartHttpServletRequestBuilder postRequest = MockMvcRequestBuilders.multipart(urlTemplate, params);
        postRequest.file(content);
        setJwtToken(postRequest);
        return readResponse(mockMvc.perform(postRequest).andExpect(status().isOk()), OtaPackageInfo.class);
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`AbstractOtaLwM2MIntegrationTest` 在 ThingsBoard Application 测试模块 中承担传输层测试或适配类型职责，核心目的是验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 核心流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
 * 3. 关键依赖：主要依赖或协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
