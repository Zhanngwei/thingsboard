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
package org.thingsboard.server.common.data.id;

import org.junit.Assert;
import org.junit.Test;

/**
 * 中文说明：
 * 1. `EntityIdTest` 是 ThingsBoard Common Data 中验证 `EntityId` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 它直接协作于被测类型、测试框架和必要的模拟依赖。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
public class EntityIdTest {

    /**
     * 功能：验证 `givenConstantNullUuid_whenCompare_thenToStringEqualsPredefinedUuid` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenConstantNullUuid_whenCompare_thenToStringEqualsPredefinedUuid() {
        Assert.assertEquals("13814000-1dd2-11b2-8080-808080808080", EntityId.NULL_UUID.toString());
    }
}