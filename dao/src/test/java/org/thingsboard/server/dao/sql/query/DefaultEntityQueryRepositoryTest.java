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
package org.thingsboard.server.dao.sql.query;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.support.TransactionTemplate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * 中文说明：
 * 1. `DefaultEntityQueryRepositoryTest` 是 ThingsBoard DAO 中验证 `DefaultEntityQueryRepository` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 它直接协作于被测类型、测试框架和必要的模拟依赖。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = DefaultEntityQueryRepository.class)
public class DefaultEntityQueryRepositoryTest {

    /**
     * JDBC 访问模板，表示当前对象的对应属性。
     */
    @MockBean
    NamedParameterJdbcTemplate jdbcTemplate;
    /**
     * 事务执行模板，表示当前对象的对应属性。
     */
    @MockBean
    TransactionTemplate transactionTemplate;
    /**
     * 查询日志组件，表示当前对象的对应属性。
     */
    @MockBean
    DefaultQueryLogComponent queryLog;

    /**
     * 存储组件，用于读取或保存对应领域对象。
     */
    @Autowired
    DefaultEntityQueryRepository repo;

    /*
     * This value has to be reasonable small to prevent infinite recursion as early as possible
     * */
    /**
     * 功能：验证 `givenDefaultMaxLevel_whenStaticConstant_thenEqualsTo` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenDefaultMaxLevel_whenStaticConstant_thenEqualsTo() {
        assertThat(repo.getMaxLevelAllowed(), equalTo(50));
    }

    /**
     * 功能：验证 `givenMaxLevelZeroOrNegative_whenGetMaxLevel_thenReturnDefaultMaxLevel` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenMaxLevelZeroOrNegative_whenGetMaxLevel_thenReturnDefaultMaxLevel() {
        assertThat(repo.getMaxLevel(0), equalTo(repo.getMaxLevelAllowed()));
        assertThat(repo.getMaxLevel(-1), equalTo(repo.getMaxLevelAllowed()));
        assertThat(repo.getMaxLevel(-2), equalTo(repo.getMaxLevelAllowed()));
        assertThat(repo.getMaxLevel(Integer.MIN_VALUE), equalTo(repo.getMaxLevelAllowed()));
    }

    /**
     * 功能：验证 `givenMaxLevelPositive_whenGetMaxLevel_thenValueTheSame` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenMaxLevelPositive_whenGetMaxLevel_thenValueTheSame() {
        assertThat(repo.getMaxLevel(1), equalTo(1));
        assertThat(repo.getMaxLevel(2), equalTo(2));
        assertThat(repo.getMaxLevel(repo.getMaxLevelAllowed()), equalTo(repo.getMaxLevelAllowed()));
        assertThat(repo.getMaxLevel(repo.getMaxLevelAllowed() + 1), equalTo(repo.getMaxLevelAllowed()));
        assertThat(repo.getMaxLevel(Integer.MAX_VALUE), equalTo(repo.getMaxLevelAllowed()));
    }

}
