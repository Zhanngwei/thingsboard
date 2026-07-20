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
package org.thingsboard.server.dao.sqlts.insert;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.regex.Pattern;

/**
 * 中文说明：
 * 1. `AbstractInsertRepository` 是 ThingsBoard DAO 中负责 `Insert` 存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 它直接协作于持久化模型、查询实现和对应领域服务。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
 */
@Repository
public abstract class AbstractInsertRepository {

    private static final ThreadLocal<Pattern> PATTERN_THREAD_LOCAL = ThreadLocal.withInitial(() -> Pattern.compile(String.valueOf(Character.MIN_VALUE)));
    /**
     * `EMPTY_STR`常量，用于统一引用固定值。
     */
    private static final String EMPTY_STR = "";

    /**
     * 是否移除对应数据。
     */
    @Value("${sql.remove_null_chars:true}")
    private boolean removeNullChars;

    /**
     * JDBC 访问模板，表示当前对象的对应属性。
     */
    @Autowired
    protected JdbcTemplate jdbcTemplate;

    /**
     * 事务执行模板，表示当前对象的对应属性。
     */
    @Autowired
    protected TransactionTemplate transactionTemplate;

    /**
     * 功能：执行 `replaceNullChars` 对应的处理。
     * 参数：
     * - `strValue`：值。
     * 返回：文本结果。
     */
    protected String replaceNullChars(String strValue) {
        if (removeNullChars && strValue != null) {
            return PATTERN_THREAD_LOCAL.get().matcher(strValue).replaceAll(EMPTY_STR);
        }
        return strValue;
    }
}
