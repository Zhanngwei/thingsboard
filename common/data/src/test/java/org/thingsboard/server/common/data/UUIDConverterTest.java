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
package org.thingsboard.server.common.data;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Random;
import java.util.UUID;

/**
 * Created by ashvayka on 14.07.17.
 */
/**
 * 中文说明：
 * 1. `UUIDConverterTest` 是 ThingsBoard Common Data 中验证 `UUIDConverter` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 它直接协作于被测类型、测试框架和必要的模拟依赖。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@RunWith(MockitoJUnitRunner.class)
public class UUIDConverterTest {

    /**
     * 功能：执行 `basicUuidToStringTest` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void basicUuidToStringTest() {
        UUID original = UUID.fromString("58e0a7d7-eebc-11d8-9669-0800200c9a66");
        String result = UUIDConverter.fromTimeUUID(original);
        Assert.assertEquals("1d8eebc58e0a7d796690800200c9a66", result);
    }


    /**
     * 功能：执行 `basicUuid` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void basicUuid() {
        System.out.println(UUIDConverter.fromString("1e746126eaaefa6a91992ebcb67fe33"));
    }

    /**
     * 功能：执行 `basicUuidConversion` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void basicUuidConversion() {
        UUID original = UUID.fromString("3dd11790-abf2-11ea-b151-83a091b9d4cc");
        Assert.assertEquals(Uuids.unixTimestamp(original), 1591886749577L);
    }

    /**
     * 功能：执行 `basicStringToUUIDTest` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void basicStringToUUIDTest() {
        UUID result = UUIDConverter.fromString("1d8eebc58e0a7d796690800200c9a66");
        Assert.assertEquals(UUID.fromString("58e0a7d7-eebc-11d8-9669-0800200c9a66"), result);
    }

    /**
     * 功能：执行 `nonV1UuidToStringTest` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void nonV1UuidToStringTest() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            UUIDConverter.fromTimeUUID(UUID.fromString("58e0a7d7-eebc-01d8-9669-0800200c9a66"));
        });
    }

    /**
     * 功能：执行 `basicUuidComperisonTest` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void basicUuidComperisonTest() {
        Random r = new Random(System.currentTimeMillis());
        for (int i = 0; i < 100000; i++) {
            long ts = System.currentTimeMillis() + 1000 * 60 * 60 * 24 * 365 * 10;
            long before = (long) (Math.random() * ts);
            long after = (long) (Math.random() * ts);
            if (before > after) {
                long tmp = after;
                after = before;
                before = tmp;
            }

            String beforeStr = UUIDConverter.fromTimeUUID(Uuids.startOf(before));
            String afterStr = UUIDConverter.fromTimeUUID(Uuids.startOf(after));

            if (afterStr.compareTo(beforeStr) < 0) {
                System.out.println("Before: " + before + " | " + beforeStr);
                System.out.println("After: " + after + " | " + afterStr);
            }
            Assert.assertTrue(afterStr.compareTo(beforeStr) >= 0);
        }
    }


}
