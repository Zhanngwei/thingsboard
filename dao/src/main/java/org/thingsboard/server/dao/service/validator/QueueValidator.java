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
package org.thingsboard.server.dao.service.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.queue.ProcessingStrategy;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.data.queue.SubmitStrategy;
import org.thingsboard.server.common.data.queue.SubmitStrategyType;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.queue.QueueDao;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;

/**
 * 中文说明：
 * 1. `QueueValidator` 是 ThingsBoard DAO 中负责队列存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 直接依赖的类型边界包括 `DataValidator`。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
 */
@Component
public class QueueValidator extends DataValidator<Queue> {

    /**
     * 队列，用于读取或保存对应领域对象。
     */
    @Autowired
    private QueueDao queueDao;

    /**
     * 租户对象，用于描述当前业务场景。
     */
    @Autowired
    private TbTenantProfileCache tenantProfileCache;

    /**
     * 功能：校验`Create`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `queue`：队列名称或队列对象。
     * 返回：无。
     */
    @Override
    protected void validateCreate(TenantId tenantId, Queue queue) {
        if (queueDao.findQueueByTenantIdAndName(tenantId, queue.getName()) != null) {
            throw new DataValidationException(String.format("Queue with name: %s already exists!", queue.getName()));
        }
        if (queueDao.findQueueByTenantIdAndTopic(tenantId, queue.getTopic()) != null) {
            throw new DataValidationException(String.format("Queue with topic: %s already exists!", queue.getTopic()));
        }
    }

    /**
     * 功能：校验`Update`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `queue`：队列名称或队列对象。
     * 返回：判断结果。
     */
    @Override
    protected Queue validateUpdate(TenantId tenantId, Queue queue) {
        Queue foundQueue = queueDao.findById(tenantId, queue.getUuidId());
        if (queueDao.findById(tenantId, queue.getUuidId()) == null) {
            throw new DataValidationException(String.format("Queue with id: %s does not exists!", queue.getId()));
        }
        if (!foundQueue.getName().equals(queue.getName())) {
            throw new DataValidationException("Queue name can't be changed!");
        }
        if (!foundQueue.getTopic().equals(queue.getTopic())) {
            throw new DataValidationException("Queue topic can't be changed!");
        }
        return foundQueue;
    }

    /**
     * 功能：校验数据。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `queue`：队列名称或队列对象。
     * 返回：无。
     */
    @Override
    protected void validateDataImpl(TenantId tenantId, Queue queue) {
        if (!tenantId.equals(TenantId.SYS_TENANT_ID)) {
            TenantProfile tenantProfile = tenantProfileCache.get(tenantId);

            if (!tenantProfile.isIsolatedTbRuleEngine()) {
                throw new DataValidationException("Tenant should be isolated!");
            }
        }

        validateQueueName(queue.getName());
        validateQueueTopic(queue.getTopic());

        if (queue.getPollInterval() < 1) {
            throw new DataValidationException("Queue poll interval should be more then 0!");
        }
        if (queue.getPartitions() < 1) {
            throw new DataValidationException("Queue partitions should be more then 0!");
        }
        if (queue.getPackProcessingTimeout() < 1) {
            throw new DataValidationException("Queue pack processing timeout should be more then 0!");
        }

        SubmitStrategy submitStrategy = queue.getSubmitStrategy();
        if (submitStrategy == null) {
            throw new DataValidationException("Queue submit strategy can't be null!");
        }
        if (submitStrategy.getType() == null) {
            throw new DataValidationException("Queue submit strategy type can't be null!");
        }
        if (submitStrategy.getType() == SubmitStrategyType.BATCH && submitStrategy.getBatchSize() < 1) {
            throw new DataValidationException("Queue submit strategy batch size should be more then 0!");
        }
        ProcessingStrategy processingStrategy = queue.getProcessingStrategy();
        if (processingStrategy == null) {
            throw new DataValidationException("Queue processing strategy can't be null!");
        }
        if (processingStrategy.getType() == null) {
            throw new DataValidationException("Queue processing strategy type can't be null!");
        }
        if (processingStrategy.getRetries() < 0) {
            throw new DataValidationException("Queue processing strategy retries can't be less then 0!");
        }
        if (processingStrategy.getFailurePercentage() < 0 || processingStrategy.getFailurePercentage() > 100) {
            throw new DataValidationException("Queue processing strategy failure percentage should be in a range from 0 to 100!");
        }
        if (processingStrategy.getPauseBetweenRetries() < 0) {
            throw new DataValidationException("Queue processing strategy pause between retries can't be less then 0!");
        }
        if (processingStrategy.getMaxPauseBetweenRetries() < processingStrategy.getPauseBetweenRetries()) {
            throw new DataValidationException("Queue processing strategy MAX pause between retries can't be less then pause between retries!");
        }
    }
}
