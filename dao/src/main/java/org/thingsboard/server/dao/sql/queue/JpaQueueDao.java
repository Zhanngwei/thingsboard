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
package org.thingsboard.server.dao.sql.queue;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.QueueEntity;
import org.thingsboard.server.dao.queue.QueueDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 中文说明：
 * 1. `JpaQueueDao` 是 ThingsBoard DAO 中负责队列存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 直接依赖的类型边界包括 `JpaAbstractDao`、`QueueDao`。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
 */
@Slf4j
@Component
@SqlDao
public class JpaQueueDao extends JpaAbstractDao<QueueEntity, Queue> implements QueueDao {

    /**
     * 队列，用于读取或保存对应领域对象。
     */
    @Autowired
    private QueueRepository queueRepository;

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected Class<QueueEntity> getEntityClass() {
        return QueueEntity.class;
    }

    /**
     * 功能：获取存取组件。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    protected JpaRepository<QueueEntity, UUID> getRepository() {
        return queueRepository;
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `topic`：主题名称或主题对象。
     * 返回：处理结果。
     */
    @Override
    public Queue findQueueByTenantIdAndTopic(TenantId tenantId, String topic) {
        return DaoUtil.getData(queueRepository.findByTenantIdAndTopic(tenantId.getId(), topic));
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `name`：名称。
     * 返回：处理结果。
     */
    @Override
    public Queue findQueueByTenantIdAndName(TenantId tenantId, String name) {
        return DaoUtil.getData(queueRepository.findByTenantIdAndName(tenantId.getId(), name));
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<Queue> findAllByTenantId(TenantId tenantId) {
        List<QueueEntity> entities = queueRepository.findByTenantId(tenantId.getId());
        return DaoUtil.convertDataList(entities);
    }

    /**
     * 功能：获取`All Main Queues`。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<Queue> findAllMainQueues() {
        List<QueueEntity> entities = Lists.newArrayList(queueRepository.findAllByName(DataConstants.MAIN_QUEUE_NAME));
        return DaoUtil.convertDataList(entities);
    }

    /**
     * 功能：获取`All Queues`。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<Queue> findAllQueues() {
        List<QueueEntity> entities = Lists.newArrayList(queueRepository.findAll());
        return DaoUtil.convertDataList(entities);
    }

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public PageData<Queue> findQueuesByTenantId(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(queueRepository
                .findByTenantId(tenantId.getId(), pageLink.getTextSearch(), DaoUtil.toPageable(pageLink)));
    }
}