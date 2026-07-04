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

@DaoSqlTest
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
public class OtaPackageEdgeTest extends AbstractEdgeTest {

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testOtaPackages_usesUrl` 对应的测试支撑类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由测试框架按测试类和测试方法管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：准备输入和依赖，调用被测对象并断言结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
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

    @Test
    /**
     * 方法说明：
     * 1. 职责：执行 `testOtaPackages_hasData` 对应的测试支撑类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由测试框架按测试类和测试方法管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：准备输入和依赖，调用被测对象并断言结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
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
     * 方法说明：
     * 1. 职责：执行 `saveData` 对应的测试支撑类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由测试框架按测试类和测试方法管理时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：准备输入和依赖，调用被测对象并断言结果。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
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
