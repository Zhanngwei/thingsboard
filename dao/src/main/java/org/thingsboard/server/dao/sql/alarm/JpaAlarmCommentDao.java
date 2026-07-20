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

import com.google.common.util.concurrent.ListenableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.alarm.AlarmCommentInfo;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.alarm.AlarmCommentDao;
import org.thingsboard.server.dao.model.sql.AlarmCommentEntity;
import org.thingsboard.server.dao.sql.JpaPartitionedAbstractDao;
import org.thingsboard.server.dao.sqlts.insert.sql.SqlPartitioningRepository;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.thingsboard.server.dao.model.ModelConstants.ALARM_COMMENT_TABLE_NAME;

/**
 * 中文说明：
 * 1. `JpaAlarmCommentDao` 是 ThingsBoard DAO 中负责告警存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 直接依赖的类型边界包括 `JpaPartitionedAbstractDao`、`AlarmCommentDao`。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
 */
@Slf4j
@Component
@SqlDao
@RequiredArgsConstructor
public class JpaAlarmCommentDao extends JpaPartitionedAbstractDao<AlarmCommentEntity, AlarmComment> implements AlarmCommentDao {
    /**
     * 存取组件，用于读取或保存对应领域对象。
     */
    private final SqlPartitioningRepository partitioningRepository;
    /**
     * 分区，用于定位消息或数据所属分区。
     */
    @Value("${sql.alarm_comments.partition_size:168}")
    private int partitionSizeInHours;

    /**
     * 告警，用于读取或保存对应领域对象。
     */
    @Autowired
    private AlarmCommentRepository alarmCommentRepository;

    /**
     * 功能：获取告警。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `id`：`id`ID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<AlarmCommentInfo> findAlarmComments(TenantId tenantId, AlarmId id, PageLink pageLink) {
        log.trace("Try to find alarm comments by alarm id using [{}]", id);
        return DaoUtil.toPageData(
                alarmCommentRepository.findAllByAlarmId(id.getId(), DaoUtil.toPageable(pageLink)));
    }

    /**
     * 功能：获取告警ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `key`：键。
     * 返回：处理结果。
     */
    @Override
    public AlarmComment findAlarmCommentById(TenantId tenantId, UUID key) {
        log.trace("Try to find alarm comment by id using [{}]", key);
        return DaoUtil.getData(alarmCommentRepository.findById(key));
    }

    /**
     * 功能：获取告警ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `key`：键。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<AlarmComment> findAlarmCommentByIdAsync(TenantId tenantId, UUID key) {
        log.trace("Try to find alarm comment by id using [{}]", key);
        return findByIdAsync(tenantId, key);
    }

    /**
     * 功能：保存或创建分区。
     * 参数：
     * - `entity`：实体对象。
     * 返回：无。
     */
    @Override
    public void createPartition(AlarmCommentEntity entity) {
        partitioningRepository.createPartitionIfNotExists(ALARM_COMMENT_TABLE_NAME, entity.getCreatedTime(), TimeUnit.HOURS.toMillis(partitionSizeInHours));
    }

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected Class<AlarmCommentEntity> getEntityClass() {
        return AlarmCommentEntity.class;
    }

    /**
     * 功能：获取存取组件。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected JpaRepository<AlarmCommentEntity, UUID> getRepository() {
        return alarmCommentRepository;
    }
}
