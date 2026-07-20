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

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.validation.Length;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * 中文说明：
 * 1. `StringLengthValidator` 是 ThingsBoard DAO 中负责 `String Length Validator` 存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 直接依赖的类型边界包括 `ConstraintValidator`。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
 */
@Slf4j
public class StringLengthValidator implements ConstraintValidator<Length, Object> {
    /**
     * `max` 字段，保存当前对象的对应属性。
     */
    private int max;

    /**
     * 功能：判断`Valid`。
     * 参数：
     * - `value`：值。
     * - `context`：处理上下文。
     * 返回：判断结果。
     */
    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        String stringValue;
        if (value instanceof CharSequence || value instanceof JsonNode) {
            stringValue = value.toString();
        } else {
            return true;
        }
        if (StringUtils.isEmpty(stringValue)) {
            return true;
        }
        return stringValue.length() <= max;
    }

    /**
     * 功能：执行 `initialize` 对应的处理。
     * 参数：
     * - `constraintAnnotation`：`constraintAnnotation` 参数。
     * 返回：无。
     */
    @Override
    public void initialize(Length constraintAnnotation) {
        this.max = constraintAnnotation.max();
    }
}
