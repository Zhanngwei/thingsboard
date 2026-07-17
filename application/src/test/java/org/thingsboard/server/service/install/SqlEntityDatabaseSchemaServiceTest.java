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
package org.thingsboard.server.service.install;

import org.junit.Test;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 中文说明：
 * 1. `SqlEntityDatabaseSchemaServiceTest` 是 ThingsBoard Application 中验证 `SqlEntityDatabaseSchemaService` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 它直接协作于被测类型、测试框架和必要的模拟依赖。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
public class SqlEntityDatabaseSchemaServiceTest {

    /**
     * 功能：验证 `givenPsqlDbSchemaService_whenCreateDatabaseSchema_thenVerifyPsqlIndexSpecificCall` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenPsqlDbSchemaService_whenCreateDatabaseSchema_thenVerifyPsqlIndexSpecificCall() throws Exception {
        SqlEntityDatabaseSchemaService service = spy(new SqlEntityDatabaseSchemaService());
        willDoNothing().given(service).executeQueryFromFile(anyString());

        service.createDatabaseSchema();

        verify(service, times(1)).createDatabaseIndexes();
        verify(service, times(1)).executeQueryFromFile(SqlEntityDatabaseSchemaService.SCHEMA_ENTITIES_SQL);
        verify(service, times(1)).executeQueryFromFile(SqlEntityDatabaseSchemaService.SCHEMA_ENTITIES_IDX_SQL);
        verify(service, times(1)).executeQueryFromFile(SqlEntityDatabaseSchemaService.SCHEMA_ENTITIES_IDX_PSQL_ADDON_SQL);
        verify(service, times(3)).executeQueryFromFile(anyString());
    }

    /**
     * 功能：验证 `givenPsqlDbSchemaService_whenCreateDatabaseIndexes_thenVerifyPsqlIndexSpecificCall` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenPsqlDbSchemaService_whenCreateDatabaseIndexes_thenVerifyPsqlIndexSpecificCall() throws Exception {
        SqlEntityDatabaseSchemaService service = spy(new SqlEntityDatabaseSchemaService());
        willDoNothing().given(service).executeQueryFromFile(anyString());

        service.createDatabaseIndexes();

        verify(service, times(1)).executeQueryFromFile(SqlEntityDatabaseSchemaService.SCHEMA_ENTITIES_IDX_SQL);
        verify(service, times(1)).executeQueryFromFile(SqlEntityDatabaseSchemaService.SCHEMA_ENTITIES_IDX_PSQL_ADDON_SQL);
        verify(service, times(2)).executeQueryFromFile(anyString());
    }

}
