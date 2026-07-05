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
package org.thingsboard.server.service.sql;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.rule.engine.telemetry.TbMsgTimeseriesNode;
import org.thingsboard.rule.engine.telemetry.TbMsgTimeseriesNodeConfiguration;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.timeseries.TimeseriesService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 中文说明：
 * 1. 类目的：`SequentialTimeseriesPersistenceTest` 是ThingsBoard Application 测试模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
@DaoSqlTest
public class SequentialTimeseriesPersistenceTest extends AbstractControllerTest {

    /**
     * `TOTALIZER`常量，用于统一引用固定值。
     */
    final String TOTALIZER = "Totalizer";
    final int TTL = 99999;
    /**
     * `GENERIC_CUMULATIVE_OBJ`常量，用于统一引用固定值。
     */
    final String GENERIC_CUMULATIVE_OBJ = "genericCumulativeObj";
    final List<Long> ts = List.of(10L, 20L, 30L, 40L, 60L, 70L, 50L, 80L);
    final List<Long> msgValue = List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L);

    /**
     * 时序数据，提供当前类调用的业务操作。
     */
    @Autowired
    TimeseriesService timeseriesService;

    /**
     * `configuration`，保存当前对象的配置选项。
     */
    TbMsgTimeseriesNodeConfiguration configuration;
    Tenant savedTenant;
    /**
     * 租户对象，用于描述当前业务场景。
     */
    User tenantAdmin;

    /**
     * 功能：执行 `beforeTest` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void beforeTest() throws Exception {
        configuration = new TbMsgTimeseriesNodeConfiguration();
        configuration.setUseServerTs(true);

        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        Assert.assertNotNull(savedTenant);

        tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getId());
        tenantAdmin.setEmail("tenant2@thingsboard.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");

        tenantAdmin = createUserAndLogin(tenantAdmin, "testPassword1");
    }

    /**
     * 功能：执行 `afterTest` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @After
    public void afterTest() throws Exception {
        loginSysAdmin();
        doDelete("/api/tenant/" + savedTenant.getId().getId().toString()).andExpect(status().isOk());
    }

    /**
     * 功能：验证时序数据相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSequentialTimeseriesPersistence() throws Exception {
        Asset asset = saveAsset("Asset");

        Device deviceA = saveDevice("Device A");
        Device deviceB = saveDevice("Device B");
        Device deviceC = saveDevice("Device C");
        Device deviceD = saveDevice("Device D");
        List<Device> devices = List.of(deviceA, deviceB, deviceC, deviceD);

        for (int i = 0; i < 2; i++) {
            int idx = i * devices.size();
            saveLatestTsForAssetAndDevice(devices, asset, idx);
            checkDiffBetweenLatestTsForDevicesAndAsset(devices, asset);
        }
    }

    /**
     * 功能：保存或创建设备。
     * 参数：
     * - `name`：名称。
     * 返回：处理结果。
     */
    Device saveDevice(String name) throws Exception {
        Device device = new Device();
        device.setName(name);
        device.setType("default");
        Device savedDevice = doPost("/api/device", device, Device.class);
        Assert.assertNotNull(savedDevice);
        return savedDevice;
    }

    /**
     * 功能：保存或创建资产。
     * 参数：
     * - `name`：名称。
     * 返回：匹配的数据集合。
     */
    Asset saveAsset(String name) throws Exception {
        Asset asset = new Asset();
        asset.setName(name);
        asset.setType("default");
        Asset savedAsset = doPost("/api/asset", asset, Asset.class);
        Assert.assertNotNull(savedAsset);
        return savedAsset;
    }

    /**
     * 功能：保存或创建设备。
     * 参数：
     * - `devices`：设备信息或设备标识。
     * - `asset`：`asset` 参数。
     * - `idx`：`idx` 参数。
     * 返回：无。
     */
    void saveLatestTsForAssetAndDevice(List<Device> devices, Asset asset, int idx) throws ExecutionException, InterruptedException, TimeoutException {
        for (Device device : devices) {
            TbMsg tbMsg = TbMsg.newMsg(TbMsgType.POST_TELEMETRY_REQUEST,
                    device.getId(),
                    getTbMsgMetadata(device.getName(), ts.get(idx)),
                    TbMsgDataType.JSON,
                    getTbMsgData(msgValue.get(idx)));
            saveDeviceTsEntry(device.getId(), tbMsg, msgValue.get(idx));
            saveAssetTsEntry(asset, device.getName(), msgValue.get(idx), TbMsgTimeseriesNode.computeTs(tbMsg, configuration.isUseServerTs()));
            idx++;
        }
    }

    /**
     * 功能：校验资产。
     * 参数：
     * - `devices`：设备信息或设备标识。
     * - `asset`：`asset` 参数。
     * 返回：无。
     */
    void checkDiffBetweenLatestTsForDevicesAndAsset(List<Device> devices, Asset asset) throws ExecutionException, InterruptedException, TimeoutException {
        TsKvEntry assetTsKvEntry = getTsKvLatest(asset.getId(), GENERIC_CUMULATIVE_OBJ);
        Assert.assertTrue(assetTsKvEntry.getJsonValue().isPresent());
        JsonObject assetJsonObject = new JsonParser().parse(assetTsKvEntry.getJsonValue().get()).getAsJsonObject();
        for (Device device : devices) {
            Long assetValue = assetJsonObject.get(device.getName()).getAsLong();
            TsKvEntry deviceLatest = getTsKvLatest(device.getId(), TOTALIZER);
            Assert.assertTrue(deviceLatest.getLongValue().isPresent());
            Long deviceValue = deviceLatest.getLongValue().get();
            Assert.assertEquals(assetValue, deviceValue);
        }
    }

    /**
     * 功能：获取消息。
     * 参数：
     * - `value`：值。
     * 返回：文本结果。
     */
    String getTbMsgData(long value) {
        return "{\"Totalizer\": " + value + "}";
    }

    /**
     * 功能：获取消息。
     * 参数：
     * - `name`：名称。
     * - `ts`：时间戳。
     * 返回：处理结果。
     */
    TbMsgMetaData getTbMsgMetadata(String name, long ts) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("deviceName", name);
        metadata.put("ts", String.valueOf(ts));
        return new TbMsgMetaData(metadata);
    }

    /**
     * 功能：保存或创建设备。
     * 参数：
     * - `entityId`：实体IDID。
     * - `tbMsg`：待处理消息。
     * - `value`：值。
     * 返回：无。
     */
    void saveDeviceTsEntry(EntityId entityId, TbMsg tbMsg, long value) throws ExecutionException, InterruptedException, TimeoutException {
        TsKvEntry tsKvEntry = new BasicTsKvEntry(TbMsgTimeseriesNode.computeTs(tbMsg, configuration.isUseServerTs()), new LongDataEntry(TOTALIZER, value));
        saveTimeseries(entityId, tsKvEntry);
    }

    /**
     * 功能：保存或创建资产。
     * 参数：
     * - `asset`：`asset` 参数。
     * - `key`：键。
     * - `value`：值。
     * - `ts`：时间戳。
     * 返回：无。
     */
    void saveAssetTsEntry(Asset asset, String key, long value, long ts) throws ExecutionException, InterruptedException, TimeoutException {
        Optional<String> tsKvEntryOpt = getTsKvLatest(asset.getId(), GENERIC_CUMULATIVE_OBJ).getJsonValue();
        TsKvEntry saveTsKvEntry = new BasicTsKvEntry(ts, new JsonDataEntry(GENERIC_CUMULATIVE_OBJ, getJsonObject(key, value, tsKvEntryOpt).toString()));
        saveTimeseries(asset.getId(), saveTsKvEntry);
    }

    /**
     * 功能：获取JSON。
     * 参数：
     * - `key`：键。
     * - `value`：值。
     * - `tsKvEntryOpt`：`tsKvEntryOpt` 参数。
     * 返回：处理结果。
     */
    JsonObject getJsonObject(String key, long value, Optional<String> tsKvEntryOpt) {
        JsonObject jsonObject = new JsonObject();
        if (tsKvEntryOpt.isPresent()) {
            jsonObject = new JsonParser().parse(tsKvEntryOpt.get()).getAsJsonObject();
        }
        jsonObject.addProperty(key, value);
        return jsonObject;
    }

    /**
     * 功能：保存或创建时序数据。
     * 参数：
     * - `entityId`：实体IDID。
     * - `saveTsKvEntry`：`saveTsKvEntry` 参数。
     * 返回：无。
     */
    void saveTimeseries(EntityId entityId, TsKvEntry saveTsKvEntry) throws InterruptedException, ExecutionException, TimeoutException {
        timeseriesService.save(savedTenant.getId(), entityId, List.of(saveTsKvEntry), TTL).get(TIMEOUT, TimeUnit.SECONDS);
    }

    /**
     * 功能：获取时间戳。
     * 参数：
     * - `entityId`：实体IDID。
     * - `key`：键。
     * 返回：处理结果。
     */
    TsKvEntry getTsKvLatest(EntityId entityId, String key) throws InterruptedException, ExecutionException, TimeoutException {
        List<TsKvEntry> tsKvEntries = timeseriesService.findLatest(
                savedTenant.getTenantId(),
                entityId,
                List.of(key)).get(TIMEOUT, TimeUnit.SECONDS);
        Assert.assertEquals(1, tsKvEntries.size());
        return tsKvEntries.get(0);
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`SequentialTimeseriesPersistenceTest` 在 ThingsBoard Application 测试模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
