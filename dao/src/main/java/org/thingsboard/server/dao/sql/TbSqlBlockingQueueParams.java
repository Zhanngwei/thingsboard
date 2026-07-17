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
package org.thingsboard.server.dao.sql;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * 中文说明：
 * 1. `TbSqlBlockingQueueParams` 是 ThingsBoard DAO 中负责队列存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 它直接协作于持久化模型、查询实现和对应领域服务。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
 */
@Slf4j
@Data
@Builder
public class TbSqlBlockingQueueParams {

    /**
     * 名称，用于标识或展示当前对象。
     */
    private final String logName;
    private final int batchSize;
    /**
     * 最大延迟，用于控制时间范围或等待时长。
     */
    private final long maxDelay;
    private final long statsPrintIntervalMs;
    /**
     * 名称，用于展示或标识当前对象。
     */
    private final String statsNamePrefix;
    private final boolean batchSortEnabled;
}
