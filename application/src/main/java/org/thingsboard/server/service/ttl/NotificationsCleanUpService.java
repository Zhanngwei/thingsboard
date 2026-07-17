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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.notification.NotificationRequestConfig;
import org.thingsboard.server.dao.notification.NotificationRequestDao;
import org.thingsboard.server.dao.sqlts.insert.sql.SqlPartitioningRepository;
import org.thingsboard.server.queue.discovery.PartitionService;

import java.util.concurrent.TimeUnit;

import static org.thingsboard.server.dao.model.ModelConstants.NOTIFICATION_TABLE_NAME;

/**
 * 中文说明：
 * 1. `NotificationsCleanUpService` 是 ThingsBoard Application 中负责 `Notifications Clean Up` 的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `AbstractCleanUpService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Service
@ConditionalOnExpression("${sql.ttl.notifications.enabled:true} && ${sql.ttl.notifications.ttl:0} > 0")
@Slf4j
public class NotificationsCleanUpService extends AbstractCleanUpService {

    /**
     * 存取组件，用于读取或保存对应领域对象。
     */
    private final SqlPartitioningRepository partitioningRepository;
    private final NotificationRequestDao notificationRequestDao;

    /**
     * `ttlInSec` 字段，保存当前对象的对应属性。
     */
    @Value("${sql.ttl.notifications.ttl:2592000}")
    private long ttlInSec;
    /**
     * 分区，用于定位消息或数据所属分区。
     */
    @Value("${sql.notifications.partition_size:168}")
    private int partitionSizeInHours;

    /**
     * 功能：创建 `NotificationsCleanUpService` 实例，并初始化必要字段。
     * 参数：
     * - `partitionService`：服务对象。
     * - `partitioningRepository`：分区标识或分区信息。
     * - `notificationRequestDao`：请求对象。
     * 返回：新创建的对象实例。
     */
    public NotificationsCleanUpService(PartitionService partitionService, SqlPartitioningRepository partitioningRepository,
                                       NotificationRequestDao notificationRequestDao) {
        super(partitionService);
        this.partitioningRepository = partitioningRepository;
        this.notificationRequestDao = notificationRequestDao;
    }

    /**
     * 功能：删除或清理`Up`。
     * 参数：无。
     * 返回：无。
     */
    @Scheduled(initialDelayString = "#{T(org.apache.commons.lang3.RandomUtils).nextLong(0, ${sql.ttl.notifications.checking_interval_ms:86400000})}",
            fixedDelayString = "${sql.ttl.notifications.checking_interval_ms:86400000}")
    public void cleanUp() {
        long expTime = System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(ttlInSec);
        long partitionDurationMs = TimeUnit.HOURS.toMillis(partitionSizeInHours);
        if (!isSystemTenantPartitionMine()) {
            partitioningRepository.cleanupPartitionsCache(NOTIFICATION_TABLE_NAME, expTime, partitionDurationMs);
            return;
        }

        long lastRemovedNotificationTs = partitioningRepository.dropPartitionsBefore(NOTIFICATION_TABLE_NAME, expTime, partitionDurationMs);
        if (lastRemovedNotificationTs > 0) {
            long gap = TimeUnit.MINUTES.toMillis(10);
            long requestExpTime = lastRemovedNotificationTs - TimeUnit.SECONDS.toMillis(NotificationRequestConfig.MAX_SENDING_DELAY) - gap;
            int removed = notificationRequestDao.removeAllByCreatedTimeBefore(requestExpTime);
            log.info("Removed {} outdated notification requests older than {}", removed, requestExpTime);
        }
    }

}
