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

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;

/**
 * 中文说明：
 * 1. `DefaultQueryLogComponentTest` 是 ThingsBoard DAO 中验证 `DefaultQueryLogComponent` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 它直接协作于被测类型、测试框架和必要的模拟依赖。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@RunWith(SpringRunner.class)
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = DefaultQueryLogComponent.class)
@EnableConfigurationProperties
@TestPropertySource(properties = {
        "sql.log_queries=true",
        "sql.log_queries_threshold:2999"
})
public class DefaultQueryLogComponentTest {

    /**
     * 租户ID，用于定位对应业务对象。
     */
    private TenantId tenantId;
    private QueryContext ctx;

    /**
     * 查询日志组件，表示当前对象的对应属性。
     */
    @SpyBean
    private DefaultQueryLogComponent queryLog;

    /**
     * 功能：初始化当前测试或组件需要的对象。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void setUp() {
        tenantId = new TenantId(UUID.fromString("97275c1c-9cf2-4d25-a68d-933031158f84"));
        ctx = new QueryContext(new QuerySecurityContext(tenantId, null, EntityType.ALARM));
    }

    /**
     * 功能：执行 `logQuery` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void logQuery() {

        BDDMockito.willReturn("").given(queryLog).substituteParametersInSqlString("", ctx);
        queryLog.logQuery(ctx, "", 3000);

        Mockito.verify(queryLog, times(1)).substituteParametersInSqlString("", ctx);

    }

    /**
     * 功能：执行 `substituteParametersInSqlString_StringType` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void substituteParametersInSqlString_StringType() {

        String sql = "Select * from Table Where name = :name AND id = :id";
        String sqlToUse = "Select * from Table Where name = 'Mery''s' AND id = 'ID_1'";

        ctx.addStringParameter("name", "Mery's");
        ctx.addStringParameter("id", "ID_1");

        String sqlToUseResult = queryLog.substituteParametersInSqlString(sql, ctx);
        assertEquals(sqlToUse, sqlToUseResult);
    }

    /**
     * 功能：执行 `substituteParametersInSqlString_DoubleLongType` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void substituteParametersInSqlString_DoubleLongType() {

        double sum = 0.00000021d;
        long price = 100000;
        String sql = "Select * from Table Where sum = :sum AND price = :price";
        String sqlToUse = "Select * from Table Where sum = 2.1E-7 AND price = 100000";

        ctx.addDoubleParameter("sum", sum);
        ctx.addLongParameter("price", price);

        String sqlToUseResult = queryLog.substituteParametersInSqlString(sql, ctx);
        assertEquals(sqlToUse, sqlToUseResult);
    }

    /**
     * 功能：执行 `substituteParametersInSqlString_BooleanType` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void substituteParametersInSqlString_BooleanType() {

        String sql = "Select * from Table Where check = :check AND mark = :mark";
        String sqlToUse = "Select * from Table Where check = true AND mark = false";

        ctx.addBooleanParameter("check", true);
        ctx.addBooleanParameter("mark", false);

        String sqlToUseResult = queryLog.substituteParametersInSqlString(sql, ctx);
        assertEquals(sqlToUse, sqlToUseResult);
    }

    /**
     * 功能：执行 `substituteParametersInSqlString_UuidType` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void substituteParametersInSqlString_UuidType() {

        UUID guid = UUID.randomUUID();
        String sql = "Select * from Table Where guid = :guid";
        String sqlToUse = "Select * from Table Where guid = '" + guid + "'";

        ctx.addUuidParameter("guid", guid);

        String sqlToUseResult = queryLog.substituteParametersInSqlString(sql, ctx);
        assertEquals(sqlToUse, sqlToUseResult);
    }

    /**
     * 功能：执行 `substituteParametersInSqlString_StringListType` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void substituteParametersInSqlString_StringListType() {

        List<String> ids = List.of("ID_1'", "ID_2", "ID_3", "ID_4");

        String sql = "Select * from Table Where id IN (:ids)";
        String sqlToUse = "Select * from Table Where id IN ('ID_1''', 'ID_2', 'ID_3', 'ID_4')";

        ctx.addStringListParameter("ids", ids);

        String sqlToUseResult = queryLog.substituteParametersInSqlString(sql, ctx);
        assertEquals(sqlToUse, sqlToUseResult);
    }

    /**
     * 功能：执行 `substituteParametersInSqlString_UuidListType` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void substituteParametersInSqlString_UuidListType() {

        List<UUID> guids = List.of(UUID.fromString("634a8d03-6871-4e01-94d0-876bf3e67dff"), UUID.fromString("3adbb5b8-4dc6-4faf-80dc-681a7b518b5e"), UUID.fromString("63a50f0c-2058-4d1d-8f15-812eb7f84412"));

        String sql = "Select * from Table Where guid IN (:guids)";
        String sqlToUse = "Select * from Table Where guid IN ('634a8d03-6871-4e01-94d0-876bf3e67dff', '3adbb5b8-4dc6-4faf-80dc-681a7b518b5e', '63a50f0c-2058-4d1d-8f15-812eb7f84412')";

        ctx.addUuidListParameter("guids", guids);

        String sqlToUseResult = queryLog.substituteParametersInSqlString(sql, ctx);
        assertEquals(sqlToUse, sqlToUseResult);
    }
}
