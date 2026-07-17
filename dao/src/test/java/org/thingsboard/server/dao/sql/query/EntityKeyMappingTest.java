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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.thingsboard.server.common.data.StringUtils.splitByCommaWithoutQuotes;

/**
 * 中文说明：
 * 1. `EntityKeyMappingTest` 是 ThingsBoard DAO 中验证 `EntityKeyMapping` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 它直接协作于被测类型、测试框架和必要的模拟依赖。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@RunWith(SpringRunner.class )
@SpringBootTest(classes = EntityKeyMapping.class)
public class EntityKeyMappingTest {

    /**
     * 实体映射关系，用于按键查找对应值。
     */
    @Autowired
    private EntityKeyMapping entityKeyMapping;

    private static final List<String> result = List.of("device1", "device2", "device3");

    /**
     * 功能：验证`Split To List`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSplitToList() {
        String value = "device1, device2, device3";
        Assert.assertEquals(splitByCommaWithoutQuotes(value), result);
    }

    /**
     * 功能：验证`Replace Single Quote`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testReplaceSingleQuote() {
        String value = "'device1', 'device2', 'device3'";
        Assert.assertEquals(splitByCommaWithoutQuotes(value), result);
    }

    /**
     * 功能：验证`Replace Double Quote`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testReplaceDoubleQuote() {
        String value = "\"device1\", \"device2\", \"device3\"";
        Assert.assertEquals(splitByCommaWithoutQuotes(value), result);
    }

    /**
     * 功能：验证`Split Without Space`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSplitWithoutSpace() {
        String value = "\"device1\"    ,    \"device2\"    ,    \"device3\"";
        Assert.assertEquals(splitByCommaWithoutQuotes(value), result);
    }

    /**
     * 功能：验证`Save Spaces Between String`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveSpacesBetweenString() {
        String value = "device 1 , device 2  ,         device 3";
        List<String> result = List.of("device 1", "device 2", "device 3");
        Assert.assertEquals(splitByCommaWithoutQuotes(value), result);
    }

    /**
     * 功能：验证`Save Quote In String`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveQuoteInString() {
        String value = "device ''1 , device \"\"2  ,         device \"'3";
        List<String> result = List.of("device ''1", "device \"\"2", "device \"'3");
        Assert.assertEquals(splitByCommaWithoutQuotes(value), result);
    }

    /**
     * 功能：验证`Not Delete Quote When Different Style`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testNotDeleteQuoteWhenDifferentStyle() {

        String value = "\"device1\", 'device2', \"device3\"";
        List<String> result = List.of("\"device1\"", "'device2'", "\"device3\"");
        Assert.assertEquals(splitByCommaWithoutQuotes(value), result);

        value = "'device1', \"device2\", \"device3\"";
        result = List.of("'device1'", "\"device2\"", "\"device3\"");
        Assert.assertEquals(splitByCommaWithoutQuotes(value), result);

        value = "device1, 'device2', \"device3\"";
        result = List.of("device1", "'device2'", "\"device3\"");
        Assert.assertEquals(splitByCommaWithoutQuotes(value), result);


        value = "'device1', device2, \"device3\"";
        result = List.of("'device1'", "device2", "\"device3\"");
        Assert.assertEquals(splitByCommaWithoutQuotes(value), result);

        value = "device1, \"device2\", \"device3\"";
        result = List.of("device1", "\"device2\"", "\"device3\"");
        Assert.assertEquals(splitByCommaWithoutQuotes(value), result);


        value = "\"device1\", device2, \"device3\"";
        result = List.of("\"device1\"", "device2", "\"device3\"");
        Assert.assertEquals(splitByCommaWithoutQuotes(value), result);
    }
}