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
package org.thingsboard.server.dao.sql.alarm;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.alarm.AlarmCommentType;
import org.thingsboard.server.common.data.id.AlarmCommentId;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.alarm.AlarmCommentDao;
import org.thingsboard.server.dao.alarm.AlarmDao;

import java.util.UUID;

import static org.junit.Assert.assertEquals;

/**
 * 中文说明：
 * 1. `JpaAlarmCommentDaoTest` 是 ThingsBoard DAO 中验证 `JpaAlarmCommentDao` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractJpaDaoTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@Slf4j
public class JpaAlarmCommentDaoTest extends AbstractJpaDaoTest {

    /**
     * 告警，用于读取或保存对应领域对象。
     */
    @Autowired
    private AlarmCommentDao alarmCommentDao;
    /**
     * 告警，用于读取或保存对应领域对象。
     */
    @Autowired
    private AlarmDao alarmDao;


    /**
     * 功能：验证告警ID相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindAlarmCommentsByAlarmId() {
        log.info("Current system time in millis = {}", System.currentTimeMillis());
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID alarmId1 = UUID.randomUUID();
        UUID alarmId2 = UUID.randomUUID();
        UUID commentId1 = UUID.randomUUID();
        UUID commentId2 = UUID.randomUUID();
        UUID commentId3 = UUID.randomUUID();
        saveAlarm(alarmId1, UUID.randomUUID(), UUID.randomUUID(), "TEST_ALARM");
        saveAlarm(alarmId2, UUID.randomUUID(), UUID.randomUUID(), "TEST_ALARM");

        saveAlarmComment(commentId1, alarmId1, userId, AlarmCommentType.OTHER);
        saveAlarmComment(commentId2, alarmId1, userId, AlarmCommentType.OTHER);
        saveAlarmComment(commentId3, alarmId2, userId, AlarmCommentType.OTHER);

        int count = alarmCommentDao.findAlarmComments(TenantId.fromUUID(tenantId), new AlarmId(alarmId1), new PageLink(10, 0)).getData().size();
        assertEquals(2, count);
    }

    /**
     * 功能：保存或创建告警。
     * 参数：
     * - `id`：`id`ID。
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * - `type`：类型。
     * 返回：无。
     */
    private void saveAlarm(UUID id, UUID tenantId, UUID deviceId, String type) {
        Alarm alarm = new Alarm();
        alarm.setId(new AlarmId(id));
        alarm.setTenantId(TenantId.fromUUID(tenantId));
        alarm.setOriginator(new DeviceId(deviceId));
        alarm.setType(type);
        alarm.setPropagate(true);
        alarm.setStartTs(System.currentTimeMillis());
        alarm.setEndTs(System.currentTimeMillis());
        alarmDao.save(TenantId.fromUUID(tenantId), alarm);
    }
    /**
     * 功能：保存或创建告警。
     * 参数：
     * - `id`：`id`ID。
     * - `alarmId`：告警IDID。
     * - `userId`：用户ID。
     * - `type`：类型。
     * 返回：无。
     */
    private void saveAlarmComment(UUID id, UUID alarmId, UUID userId, AlarmCommentType type) {
        AlarmComment alarmComment = new AlarmComment();
        alarmComment.setId(new AlarmCommentId(id));
        alarmComment.setAlarmId(TenantId.fromUUID(alarmId));
        alarmComment.setUserId(new UserId(userId));
        alarmComment.setType(type);
        alarmComment.setComment(JacksonUtil.newObjectNode().put("text", RandomStringUtils.randomAlphanumeric(10)));
        alarmCommentDao.save(TenantId.fromUUID(UUID.randomUUID()), alarmComment);
    }
}
