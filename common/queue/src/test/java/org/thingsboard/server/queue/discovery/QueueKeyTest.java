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
package org.thingsboard.server.queue.discovery;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.ServiceType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 中文说明：
 * 1. `QueueKeyTest` 是 ThingsBoard Common Queue 中验证 `QueueKey` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 它直接协作于被测类型、测试框架和必要的模拟依赖。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@Slf4j
class QueueKeyTest {

    /**
     * 功能：验证租户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void testToStringSystemTenant() {
        QueueKey queueKey = new QueueKey(ServiceType.TB_RULE_ENGINE, DataConstants.MAIN_QUEUE_NAME, TenantId.SYS_TENANT_ID);
        log.info("The queue key is {}",queueKey);
        assertThat(queueKey.toString()).isEqualTo("QK(Main,TB_RULE_ENGINE,system)");
    }

    /**
     * 功能：验证租户相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void testToStringCustomTenant() {
        TenantId tenantId = TenantId.fromUUID(UUID.fromString("3ebd39eb-43d4-4911-a818-cdbf8d508f88"));
        QueueKey queueKey = new QueueKey(ServiceType.TB_RULE_ENGINE, DataConstants.MAIN_QUEUE_NAME, tenantId);
        log.info("The queue key is {}",queueKey);
        assertThat(queueKey.toString()).isEqualTo("QK(Main,TB_RULE_ENGINE,3ebd39eb-43d4-4911-a818-cdbf8d508f88)");
    }
}
