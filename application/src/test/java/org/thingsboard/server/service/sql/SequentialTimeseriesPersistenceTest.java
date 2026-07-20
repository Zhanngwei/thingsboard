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
 * 1. `SequentialTimeseriesPersistenceTest` 是 ThingsBoard Application 中验证 `SequentialTimeseriesPersistence` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractControllerTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
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
