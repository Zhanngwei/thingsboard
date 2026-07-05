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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.SaveOtaPackageInfoRequest;
import org.thingsboard.server.common.data.ota.ChecksumAlgorithm;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.edge.v1.OtaPackageUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;

import java.nio.ByteBuffer;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.common.data.ota.OtaPackageType.FIRMWARE;

/**
 * 中文说明：
 * 1. 类目的：`OtaPackageEdgeTest` 是ThingsBoard Application 测试模块中的测试支撑类型，用于验证 Application 模块的控制器、服务、Actor 或集成流程。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括JUnit、Mockito、Spring Test、DAO、缓存、Transport 和测试夹具。
 * 4. 生命周期：由测试框架按测试类和测试方法管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Test Fixture。
 */
@DaoSqlTest
public class OtaPackageEdgeTest extends AbstractEdgeTest {

    /**
     * 功能：验证URL 地址相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testOtaPackages_usesUrl() throws Exception {
        // create ota package
        SaveOtaPackageInfoRequest firmwareInfo = new SaveOtaPackageInfoRequest();
        firmwareInfo.setDeviceProfileId(thermostatDeviceProfile.getId());
        firmwareInfo.setType(FIRMWARE);
        firmwareInfo.setTitle("My firmware #1");
        firmwareInfo.setVersion("v1.0");
        firmwareInfo.setTag("My firmware #1 v1.0");
        firmwareInfo.setUsesUrl(true);
        firmwareInfo.setUrl("http://localhost:8080/v1/package");
        firmwareInfo.setAdditionalInfo(JacksonUtil.newObjectNode());

        edgeImitator.expectMessageAmount(1);
        OtaPackageInfo savedFirmwareInfo = doPost("/api/otaPackage", firmwareInfo, OtaPackageInfo.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof OtaPackageUpdateMsg);
        OtaPackageUpdateMsg otaPackageUpdateMsg = (OtaPackageUpdateMsg) latestMessage;
        OtaPackage otaPackage = JacksonUtil.fromString(otaPackageUpdateMsg.getEntity(), OtaPackage.class, true);
        Assert.assertNotNull(otaPackage);
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, otaPackageUpdateMsg.getMsgType());
        Assert.assertEquals(savedFirmwareInfo.getId(), otaPackage.getId());
        Assert.assertEquals(thermostatDeviceProfile.getId(), otaPackage.getDeviceProfileId());
        Assert.assertEquals(FIRMWARE, otaPackage.getType());
        Assert.assertEquals("My firmware #1", otaPackage.getTitle());
        Assert.assertEquals("v1.0", otaPackage.getVersion());
        Assert.assertEquals("My firmware #1 v1.0", otaPackage.getTag());
        Assert.assertEquals("http://localhost:8080/v1/package", otaPackage.getUrl());
        Assert.assertNull(otaPackage.getData());
        Assert.assertNull(otaPackage.getFileName());
        Assert.assertNull(otaPackage.getContentType());
        Assert.assertNull(otaPackage.getChecksumAlgorithm());
        Assert.assertNull(otaPackage.getChecksum());
        Assert.assertNull(otaPackage.getDataSize());

        // delete ota package
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/otaPackage/" + savedFirmwareInfo.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof OtaPackageUpdateMsg);
        otaPackageUpdateMsg = (OtaPackageUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, otaPackageUpdateMsg.getMsgType());
        Assert.assertEquals(savedFirmwareInfo.getUuidId().getMostSignificantBits(), otaPackageUpdateMsg.getIdMSB());
        Assert.assertEquals(savedFirmwareInfo.getUuidId().getLeastSignificantBits(), otaPackageUpdateMsg.getIdLSB());
    }

    /**
     * 功能：验证数据相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testOtaPackages_hasData() throws Exception {
        // create ota package
        SaveOtaPackageInfoRequest firmwareInfo = new SaveOtaPackageInfoRequest();
        firmwareInfo.setDeviceProfileId(thermostatDeviceProfile.getId());
        firmwareInfo.setType(FIRMWARE);
        firmwareInfo.setTitle("My firmware #2");
        firmwareInfo.setVersion("v2.0");
        firmwareInfo.setTag("My firmware #2 v2.0");
        firmwareInfo.setUsesUrl(false);
        firmwareInfo.setHasData(false);
        firmwareInfo.setAdditionalInfo(JacksonUtil.newObjectNode());

        edgeImitator.expectMessageAmount(1);

        OtaPackageInfo savedFirmwareInfo = doPost("/api/otaPackage", firmwareInfo, OtaPackageInfo.class);
        MockMultipartFile testData = new MockMultipartFile("file", "firmware.bin", "image/png", ByteBuffer.wrap(new byte[]{1, 3, 5}).array());
        savedFirmwareInfo = saveData("/api/otaPackage/" + savedFirmwareInfo.getId().getId().toString() + "?checksumAlgorithm={checksumAlgorithm}", testData, ChecksumAlgorithm.SHA256.name());

        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof OtaPackageUpdateMsg);
        OtaPackageUpdateMsg otaPackageUpdateMsg = (OtaPackageUpdateMsg) latestMessage;
        OtaPackage otaPackage = JacksonUtil.fromString(otaPackageUpdateMsg.getEntity(), OtaPackage.class, true);
        Assert.assertNotNull(otaPackage);
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, otaPackageUpdateMsg.getMsgType());
        Assert.assertEquals(savedFirmwareInfo.getId(), otaPackage.getId());
        Assert.assertEquals(thermostatDeviceProfile.getId(), otaPackage.getDeviceProfileId());
        Assert.assertEquals(FIRMWARE, otaPackage.getType());
        Assert.assertEquals("My firmware #2", otaPackage.getTitle());
        Assert.assertEquals("v2.0", otaPackage.getVersion());
        Assert.assertEquals("My firmware #2 v2.0", otaPackage.getTag());
        Assert.assertFalse(otaPackage.hasUrl());
        Assert.assertEquals("firmware.bin", otaPackage.getFileName());
        Assert.assertEquals("image/png", otaPackage.getContentType());
        Assert.assertEquals(ChecksumAlgorithm.SHA256, otaPackage.getChecksumAlgorithm());
        Assert.assertEquals("62467691cf583d4fa78b18fafaf9801f505e0ef03baf0603fd4b0cd004cd1e75", otaPackage.getChecksum());
        Assert.assertEquals(3L, otaPackage.getDataSize().longValue());
        Assert.assertEquals(ByteBuffer.wrap(new byte[]{1, 3, 5}), otaPackage.getData());

        // delete ota package
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/otaPackage/" + savedFirmwareInfo.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());
        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof OtaPackageUpdateMsg);
        otaPackageUpdateMsg = (OtaPackageUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, otaPackageUpdateMsg.getMsgType());
        Assert.assertEquals(savedFirmwareInfo.getUuidId().getMostSignificantBits(), otaPackageUpdateMsg.getIdMSB());
        Assert.assertEquals(savedFirmwareInfo.getUuidId().getLeastSignificantBits(), otaPackageUpdateMsg.getIdLSB());
    }

    /**
     * 功能：保存或创建数据。
     * 参数：
     * - `urlTemplate`：`urlTemplate` 参数。
     * - `content`：`content` 参数。
     * - `params`：`params` 参数。
     * 返回：处理结果。
     */
    private OtaPackageInfo saveData(String urlTemplate, MockMultipartFile content, String... params) throws Exception {
        MockMultipartHttpServletRequestBuilder postRequest = MockMvcRequestBuilders.multipart(urlTemplate, params);
        postRequest.file(content);
        setJwtToken(postRequest);
        return readResponse(mockMvc.perform(postRequest).andExpect(status().isOk()), OtaPackageInfo.class);
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`OtaPackageEdgeTest` 在 ThingsBoard Application 测试模块 中承担测试支撑类型职责，核心目的是验证 Application 模块的控制器、服务、Actor 或集成流程。
 * 2. 核心流程：准备输入和依赖，调用被测对象并断言结果。
 * 3. 关键依赖：主要依赖或协作对象包括JUnit、Mockito、Spring Test、DAO、缓存、Transport 和测试夹具。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
