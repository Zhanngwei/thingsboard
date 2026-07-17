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
package org.thingsboard.server.common.adaptor;

import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

/**
 * 中文说明：
 * 1. `JsonConverterTest` 是 ThingsBoard Common 中验证 `JsonConverter` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 它直接协作于被测类型、测试框架和必要的模拟依赖。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
public class JsonConverterTest {

    private final JsonParser JSON_PARSER = new JsonParser();

    /**
     * 功能：执行 `before` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @BeforeEach
    public void before() {
        JsonConverter.setTypeCastEnabled(true);
    }

    /**
     * 功能：验证`Parse Big Decimal As Long`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testParseBigDecimalAsLong() {
        var result = JsonConverter.convertToTelemetry(JSON_PARSER.parse("{\"meterReadingDelta\": 1E+1}"), 0L);
        Assert.assertEquals(10L, result.get(0L).get(0).getLongValue().get().longValue());
    }

    /**
     * 功能：验证`Parse Big Decimal As Double`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testParseBigDecimalAsDouble() {
        var result = JsonConverter.convertToTelemetry(JSON_PARSER.parse("{\"meterReadingDelta\": 101E-1}"), 0L);
        Assert.assertEquals(10.1, result.get(0L).get(0).getDoubleValue().get(), 0.0);
    }

    /**
     * 功能：验证`Parse Attributes Big Decimal As Long`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testParseAttributesBigDecimalAsLong() {
        var result = new ArrayList<>(JsonConverter.convertToAttributes(JSON_PARSER.parse("{\"meterReadingDelta\": 1E1}")));
        Assert.assertEquals(10L, result.get(0).getLongValue().get().longValue());
    }

    /**
     * 功能：验证`Parse As Double With Zero`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testParseAsDoubleWithZero() {
        var result = JsonConverter.convertToTelemetry(JSON_PARSER.parse("{\"meterReadingDelta\": 42.0}"), 0L);
        Assert.assertEquals(42.0, result.get(0L).get(0).getDoubleValue().get(), 0.0);
    }

    /**
     * 功能：验证`Parse As Double`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testParseAsDouble() {
        var result = JsonConverter.convertToTelemetry(JSON_PARSER.parse("{\"meterReadingDelta\": 1.1}"), 0L);
        Assert.assertEquals(1.1, result.get(0L).get(0).getDoubleValue().get(), 0.0);
    }

    /**
     * 功能：验证`Parse As Long`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testParseAsLong() {
        var result = JsonConverter.convertToTelemetry(JSON_PARSER.parse("{\"meterReadingDelta\": 11}"), 0L);
        Assert.assertEquals(11L, result.get(0L).get(0).getLongValue().get().longValue());
    }

    /**
     * 功能：验证`Parse Big Decimal As String Out Of Long Range`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testParseBigDecimalAsStringOutOfLongRange() {
        var result = JsonConverter.convertToTelemetry(JSON_PARSER.parse("{\"meterReadingDelta\": 9.9701010061400066E19}"), 0L);
        Assert.assertEquals("99701010061400066000", result.get(0L).get(0).getStrValue().get());
    }

    /**
     * 功能：验证`Parse Big Decimal As String Out Of Long Range2`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testParseBigDecimalAsStringOutOfLongRange2() {
        var result = JsonConverter.convertToTelemetry(JSON_PARSER.parse("{\"meterReadingDelta\": 99701010061400066001}"), 0L);
        Assert.assertEquals("99701010061400066001", result.get(0L).get(0).getStrValue().get());
    }

    /**
     * 功能：验证`Parse Big Decimal As String Out Of Long Range3`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testParseBigDecimalAsStringOutOfLongRange3() {
        var result = JsonConverter.convertToTelemetry(JSON_PARSER.parse("{\"meterReadingDelta\": 1E19}"), 0L);
        Assert.assertEquals("10000000000000000000", result.get(0L).get(0).getStrValue().get());
    }

    /**
     * 功能：验证`Parse Big Decimal Out Of Long Range Without Parsing`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testParseBigDecimalOutOfLongRangeWithoutParsing() {
        JsonConverter.setTypeCastEnabled(false);
        Assertions.assertThrows(JsonSyntaxException.class, () -> {
            JsonConverter.convertToTelemetry(JSON_PARSER.parse("{\"meterReadingDelta\": 89701010051400054084}"), 0L);
        });
    }

    /**
     * 功能：验证`Parse Big Decimal Out Of Long Range Without Parsing2`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testParseBigDecimalOutOfLongRangeWithoutParsing2() {
        JsonConverter.setTypeCastEnabled(false);
        Assertions.assertThrows(JsonSyntaxException.class, () -> {
            JsonConverter.convertToTelemetry(JSON_PARSER.parse("{\"meterReadingDelta\": 9.9701010061400066E19}"), 0L);
        });
    }
}
