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
package org.thingsboard.server.dao.service;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.cache.TbTransactionalCache;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 中文说明：
 * 1. `TbCacheSerializationTest` 是 ThingsBoard DAO 中验证 `TbCacheSerialization` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 直接依赖的类型边界包括 `AbstractServiceTest`。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@DaoSqlTest
public class TbCacheSerializationTest extends AbstractServiceTest {

    /**
     * 告警，用于区分不同处理分支。
     */
    @Autowired
    TbTransactionalCache<TenantId, PageData<EntitySubtype>> alarmTypesCache;

    /**
     * 功能：执行 `AlarmTypesSerializationTest` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void AlarmTypesSerializationTest() {
        var typesCount = 13;
        TenantId tenantId = new TenantId(UUID.randomUUID());
        List<EntitySubtype> types = new ArrayList<>(typesCount);
        for (int i = 0; i < typesCount; i++) {
            types.add(new EntitySubtype(tenantId, EntityType.ALARM, "alarm_type_" + i));
        }
        PageData<EntitySubtype> alarmTypesPage = new PageData<>(types, 1, typesCount, false);
        alarmTypesCache.put(tenantId, alarmTypesPage);
        PageData<EntitySubtype> foundAlarmTypes = alarmTypesCache.get(tenantId).get();
        Assert.assertEquals(alarmTypesPage, foundAlarmTypes);
    }
}
