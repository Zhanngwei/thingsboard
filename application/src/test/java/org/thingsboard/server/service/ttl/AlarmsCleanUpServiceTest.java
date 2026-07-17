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
package org.thingsboard.server.service.ttl;

import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.alarm.AlarmDao;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 中文说明：
 * 1. `AlarmsCleanUpServiceTest` 是 ThingsBoard Application 中验证 `AlarmsCleanUpService` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractControllerTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@DaoSqlTest
@TestPropertySource(properties = {
        "sql.ttl.alarms.removal_batch_size=5"
})
public class AlarmsCleanUpServiceTest extends AbstractControllerTest {

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    private AlarmsCleanUpService alarmsCleanUpService;
    /**
     * 告警，提供当前类调用的业务操作。
     */
    @SpyBean
    private AlarmService alarmService;
    /**
     * 告警，用于读取或保存对应领域对象。
     */
    @Autowired
    private AlarmDao alarmDao;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    private static Logger cleanUpServiceLogger;

    /**
     * 功能：执行 `before` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @BeforeClass
    public static void before() throws Exception {
        cleanUpServiceLogger = Mockito.spy(LoggerFactory.getLogger(AlarmsCleanUpService.class));
        setStaticFinalFieldValue(AlarmsCleanUpService.class, "log", cleanUpServiceLogger);
    }

    /**
     * 功能：验证`Alarms Clean Up`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testAlarmsCleanUp() throws Exception {
        int ttlDays = 1;
        updateDefaultTenantProfileConfig(profileConfiguration -> {
            profileConfiguration.setAlarmsTtlDays(ttlDays);
        });

        loginTenantAdmin();
        Device device = createDevice("device_0", "device_0");
        int count = 100;
        long ts = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(ttlDays) - TimeUnit.MINUTES.toMillis(10);
        List<AlarmId> outdatedAlarms = new ArrayList<>();
        List<AlarmId> freshAlarms = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Alarm alarm = Alarm.builder()
                    .tenantId(tenantId)
                    .originator(device.getId())
                    .cleared(false)
                    .acknowledged(false)
                    .severity(AlarmSeverity.CRITICAL)
                    .type("outdated_alarm_" + i)
                    .startTs(ts)
                    .endTs(ts)
                    .build();
            alarm.setId(new AlarmId(UUID.randomUUID()));
            alarm.setCreatedTime(ts);

            outdatedAlarms.add(alarmDao.save(tenantId, alarm).getId());

            alarm.setType("fresh_alarm_" + i);
            alarm.setStartTs(System.currentTimeMillis());
            alarm.setEndTs(alarm.getStartTs());
            alarm.setId(new AlarmId(UUID.randomUUID()));
            alarm.setCreatedTime(alarm.getStartTs());
            freshAlarms.add(alarmDao.save(tenantId, alarm).getId());
        }

        alarmsCleanUpService.cleanUp();

        for (AlarmId outdatedAlarm : outdatedAlarms) {
            verify(alarmService).delAlarm(eq(tenantId), eq(outdatedAlarm), eq(false));
        }
        for (AlarmId freshAlarm : freshAlarms) {
            verify(alarmService, never()).delAlarm(eq(tenantId), eq(freshAlarm), eq(false));
        }

        verify(cleanUpServiceLogger).info(startsWith("Removed {} outdated alarm"), eq((long) count), eq(tenantId), any());
    }

}
