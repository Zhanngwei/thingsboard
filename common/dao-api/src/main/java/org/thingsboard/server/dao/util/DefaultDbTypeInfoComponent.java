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
package org.thingsboard.server.dao.util;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 中文说明：
 * 1. `DefaultDbTypeInfoComponent` 是 ThingsBoard Common 中负责 `Db Type Component` 的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `DbTypeInfoComponent`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Component
public class DefaultDbTypeInfoComponent implements DbTypeInfoComponent {

    /**
     * 时间戳，用于区分不同处理分支。
     */
    @Value("${database.ts_latest.type:sql}")
    @Getter
    private String latestTsDbType;

    /**
     * 功能：判断时间戳。
     * 参数：无。
     * 返回：判断结果。
     */
    @Override
    public boolean isLatestTsDaoStoredToSql() {
        return !latestTsDbType.equalsIgnoreCase("cassandra");
    }
}
