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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.SaveDeviceWithCredentialsRequest;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.SingleEntityFilter;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.common.data.query.EntityKeyType.TIME_SERIES;

/**
 * 中文说明：
 * 1. 类目的：`TelemetryControllerTest` 是ThingsBoard Application 测试模块中的REST/WebSocket 控制层类型，用于承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 生命周期：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 MVC Controller / Facade。
 */
@DaoSqlTest
@TestPropertySource(properties = {
        "sql.attributes.value_no_xss_validation=true",
        "sql.ts.value_no_xss_validation=true"
})
public class TelemetryControllerTest extends AbstractControllerTest {

    /**
     * 功能：验证校验器相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testConstraintValidator() throws Exception {
        loginTenantAdmin();
        Device device = createDevice();
        String correctRequestBody = "{\"data\": \"value\"}";
        doPostAsync("/api/plugins/telemetry/" + device.getId() + "/SHARED_SCOPE", correctRequestBody, String.class, status().isOk());
        doPostAsync("/api/plugins/telemetry/DEVICE/" + device.getId() + "/timeseries/smth", correctRequestBody, String.class, status().isOk());
        String invalidRequestBody = "{\"<object data=\\\"data:text/html,<script>alert(document)</script>\\\"></object>\": \"data\"}";
        doPostAsync("/api/plugins/telemetry/" + device.getId() + "/SHARED_SCOPE", invalidRequestBody, String.class, status().isBadRequest());
        doPostAsync("/api/plugins/telemetry/DEVICE/" + device.getId() + "/timeseries/smth", invalidRequestBody, String.class, status().isBadRequest());
    }

    /**
     * 功能：验证遥测相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testTelemetryRequests() throws Exception {
        loginTenantAdmin();
        Device device = createDevice();

        var startTs = 1704899727000L; // Wednesday, January 10 15:15:27 GMT
        var endOfWeek1Ts = 1705269600000L;  // Monday, January 15, 2024 0:00:00 GMT+02:00
        var endOfWeek2Ts = 1705874400000L;  // Monday, January 22, 2024 0:00:00 GMT+02:00
        var endTs = endOfWeek2Ts + TimeUnit.DAYS.toMillis(1) + TimeUnit.HOURS.toMillis(1); // Monday, January 23, 2024 1:00:00 GMT+02:00

        var firstIntervalTs = startTs + (endOfWeek1Ts - startTs) / 2;
        var secondIntervalTs = endOfWeek1Ts + (endOfWeek2Ts - endOfWeek1Ts) / 2;
        var thirdIntervalTs = endOfWeek2Ts + (endTs - endOfWeek2Ts) / 2;

        var middleOfTheInterval = startTs + (endTs - startTs) / 2;

        tsService.save(tenantId, device.getId(), new BasicTsKvEntry(1704899728000L, new LongDataEntry("t", 1L))); // Wednesday, January 10 15:15:28 GMT
        tsService.save(tenantId, device.getId(), new BasicTsKvEntry(1704899729000L, new LongDataEntry("t", 3L))); // Wednesday, January 10 15:15:29 GMT
        tsService.save(tenantId, device.getId(), new BasicTsKvEntry(endOfWeek1Ts, new LongDataEntry("t", 2L))); // Monday, January 15, 2024 0:00:00 GMT+02:00
        tsService.save(tenantId, device.getId(), new BasicTsKvEntry(endOfWeek1Ts + 1000, new LongDataEntry("t", 5L))); // Monday, January 15, 2024 0:00:01 GMT+02:00
        tsService.save(tenantId, device.getId(), new BasicTsKvEntry(endOfWeek2Ts, new LongDataEntry("t", 9L))); // Monday, January 22, 2024 0:00:00 GMT+02:00
        tsService.save(tenantId, device.getId(), new BasicTsKvEntry(endOfWeek2Ts + 1000, new LongDataEntry("t", 2L))); // Monday, January 22, 2024 0:00:01 GMT+02:00

        ObjectNode result = doGetAsync("/api/plugins/telemetry/DEVICE/" + device.getId() +
                        "/values/timeseries?keys=t&startTs={startTs}&endTs={endTs}&agg={agg}&intervalType={intervalType}&timeZone={timeZone}",
                ObjectNode.class, startTs, endTs, "SUM", "WEEK_ISO", "Europe/Kyiv");
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.get("t"));
        Assert.assertEquals(3, result.get("t").size());

        var firstIntervalResult = result.get("t").get(0);
        Assert.assertEquals(4L, firstIntervalResult.get("value").asLong());
        Assert.assertEquals(firstIntervalTs, firstIntervalResult.get("ts").asLong());

        var secondIntervalResult = result.get("t").get(1);
        Assert.assertEquals(7L, secondIntervalResult.get("value").asLong());
        Assert.assertEquals(secondIntervalTs, secondIntervalResult.get("ts").asLong());

        var thirdIntervalResult = result.get("t").get(2);
        Assert.assertEquals(11L, thirdIntervalResult.get("value").asLong());
        Assert.assertEquals(thirdIntervalTs, thirdIntervalResult.get("ts").asLong());

        result = doGetAsync("/api/plugins/telemetry/DEVICE/" + device.getId() +
                        "/values/timeseries?keys=t&startTs={startTs}&endTs={endTs}&agg={agg}&intervalType={intervalType}&timeZone={timeZone}",
                ObjectNode.class, startTs, endTs, "SUM", "MONTH", "Europe/Kyiv");

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.get("t"));
        Assert.assertEquals(1, result.get("t").size());

        var monthResult = result.get("t").get(0);
        Assert.assertEquals(22L, monthResult.get("value").asLong());
        Assert.assertEquals(middleOfTheInterval, monthResult.get("ts").asLong());
    }

    /**
     * 功能：验证遥测相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testDeleteAllTelemetryWithLatest() throws Exception {
        loginTenantAdmin();
        Device device = createDevice();

        SingleEntityFilter filter = new SingleEntityFilter();
        filter.setSingleEntity(device.getId());

        getWsClient().subscribeLatestUpdate(List.of(new EntityKey(TIME_SERIES, "data")), filter);

        getWsClient().registerWaitForUpdate(1);

        long startTs = System.currentTimeMillis();

        String testBody = "{\"data\": \"value\"}";
        doPostAsync("/api/plugins/telemetry/DEVICE/" + device.getId() + "/timeseries/smth", testBody, String.class, status().isOk());

        long endTs = System.currentTimeMillis();

        ObjectNode latest = doGetAsync("/api/plugins/telemetry/DEVICE/" + device.getId() + "/values/timeseries?keys=data", ObjectNode.class);

        Assert.assertNotNull(latest);
        var data = latest.get("data");
        Assert.assertNotNull(data);

        Assert.assertEquals("value", data.get(0).get("value").asText());

        ObjectNode timeseries = doGetAsync("/api/plugins/telemetry/DEVICE/" + device.getId() + "/values/timeseries?keys=data&startTs={startTs}&endTs={endTs}", ObjectNode.class, startTs, endTs);

        Assert.assertNotNull(timeseries);

        Assert.assertEquals("value", timeseries.get("data").get(0).get("value").asText());

        doDeleteAsync("/api/plugins/telemetry/DEVICE/" + device.getId() + "/timeseries/delete?keys=data&deleteAllDataForKeys=true", String.class);

        latest = doGetAsync("/api/plugins/telemetry/DEVICE/" + device.getId() + "/values/timeseries?keys=data", ObjectNode.class);

        Assert.assertTrue(latest.get("data").get(0).get("value").isNull());

        timeseries = doGetAsync("/api/plugins/telemetry/DEVICE/" + device.getId() + "/values/timeseries?keys=data&startTs={startTs}&endTs={endTs}", ObjectNode.class, startTs, endTs);

        Assert.assertTrue(timeseries.isEmpty());
    }

    /**
     * 功能：验证遥测相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testDeleteAllTelemetryWithoutLatest() throws Exception {
        loginTenantAdmin();
        Device device = createDevice();

        SingleEntityFilter filter = new SingleEntityFilter();
        filter.setSingleEntity(device.getId());

        getWsClient().subscribeLatestUpdate(List.of(new EntityKey(TIME_SERIES, "data")), filter);

        getWsClient().registerWaitForUpdate(1);

        long startTs = System.currentTimeMillis();

        String testBody = "{\"data\": \"value\"}";
        doPostAsync("/api/plugins/telemetry/DEVICE/" + device.getId() + "/timeseries/smth", testBody, String.class, status().isOk());

        long endTs = System.currentTimeMillis();

        ObjectNode latest = doGetAsync("/api/plugins/telemetry/DEVICE/" + device.getId() + "/values/timeseries?keys=data", ObjectNode.class);

        Assert.assertNotNull(latest);

        Assert.assertEquals("value", latest.get("data").get(0).get("value").asText());

        ObjectNode timeseries = doGetAsync("/api/plugins/telemetry/DEVICE/" + device.getId() + "/values/timeseries?keys=data&startTs={startTs}&endTs={endTs}", ObjectNode.class, startTs, endTs);

        Assert.assertNotNull(timeseries);

        Assert.assertEquals("value", timeseries.get("data").get(0).get("value").asText());

        doDeleteAsync("/api/plugins/telemetry/DEVICE/" + device.getId() + "/timeseries/delete?keys=data&deleteAllDataForKeys=true&deleteLatest=false", String.class);

        latest = doGetAsync("/api/plugins/telemetry/DEVICE/" + device.getId() + "/values/timeseries?keys=data", ObjectNode.class);

        Assert.assertEquals("value", latest.get("data").get(0).get("value").asText());

        timeseries = doGetAsync("/api/plugins/telemetry/DEVICE/" + device.getId() + "/values/timeseries?keys=data&startTs={startTs}&endTs={endTs}", ObjectNode.class, startTs, endTs);

        Assert.assertTrue(timeseries.isEmpty());
    }

    /**
     * 功能：验证值相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testValueConstraintValidator() throws Exception {
        loginTenantAdmin();
        Device device = createDevice();
        String correctRequestBody = "{\"data\": \"value\"}";
        doPostAsync("/api/plugins/telemetry/" + device.getId() + "/SHARED_SCOPE", correctRequestBody, String.class, status().isOk());
        doPostAsync("/api/plugins/telemetry/DEVICE/" + device.getId() + "/timeseries/smth", correctRequestBody, String.class, status().isOk());
        String invalidRequestBody = "{\"data\": \"<object data=\\\"data:text/html,<script>alert(document)</script>\\\"></object>\"}";
        doPostAsync("/api/plugins/telemetry/" + device.getId() + "/SHARED_SCOPE", invalidRequestBody, String.class, status().isBadRequest());
        doPostAsync("/api/plugins/telemetry/DEVICE/" + device.getId() + "/timeseries/smth", invalidRequestBody, String.class, status().isBadRequest());
    }

    /**
     * 功能：验证键相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testEmptyKeyIsProhibited() throws Exception {
        loginTenantAdmin();
        Device device = createDevice();
        String invalidRequestBody = "{\"\": \"value\"}";
        doPostAsync("/api/plugins/telemetry/" + device.getId() + "/SHARED_SCOPE", invalidRequestBody, String.class, status().isBadRequest());
        doPostAsync("/api/plugins/telemetry/DEVICE/" + device.getId() + "/timeseries/smth", invalidRequestBody, String.class, status().isBadRequest());

        String invalidRequestBody2 = "{\" \": \"value\"}";
        doPostAsync("/api/plugins/telemetry/" + device.getId() + "/SHARED_SCOPE", invalidRequestBody2, String.class, status().isBadRequest());
        doPostAsync("/api/plugins/telemetry/DEVICE/" + device.getId() + "/timeseries/smth", invalidRequestBody2, String.class, status().isBadRequest());
    }

    /**
     * 功能：保存或创建设备。
     * 参数：无。
     * 返回：处理结果。
     */
    private Device createDevice() throws Exception {
        String testToken = "TEST_TOKEN";

        Device device = new Device();
        device.setName("My device");
        device.setType("default");

        DeviceCredentials deviceCredentials = new DeviceCredentials();
        deviceCredentials.setCredentialsType(DeviceCredentialsType.ACCESS_TOKEN);
        deviceCredentials.setCredentialsId(testToken);

        SaveDeviceWithCredentialsRequest saveRequest = new SaveDeviceWithCredentialsRequest(device, deviceCredentials);

        return readResponse(doPost("/api/device-with-credentials", saveRequest).andExpect(status().isOk()), Device.class);
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`TelemetryControllerTest` 在 ThingsBoard Application 测试模块 中承担REST/WebSocket 控制层类型职责，核心目的是承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 核心流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
 * 3. 关键依赖：主要依赖或协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
