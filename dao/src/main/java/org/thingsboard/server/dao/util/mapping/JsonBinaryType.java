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
package org.thingsboard.server.dao.util.mapping;

import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.usertype.DynamicParameterizedType;

import java.util.Properties;

/**
 * 中文说明：
 * 1. `JsonBinaryType` 是 ThingsBoard DAO 中负责 `Json Binary Type` 存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 直接依赖的类型边界包括 `AbstractSingleColumnStandardBasicType`、`DynamicParameterizedType`。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
 */
public class JsonBinaryType extends AbstractSingleColumnStandardBasicType<Object> implements DynamicParameterizedType {

    /**
     * 功能：创建 `JsonBinaryType` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public JsonBinaryType() {
        super(
                JsonBinarySqlTypeDescriptor.INSTANCE,
                new JsonTypeDescriptor()
        );
    }

    /**
     * 功能：获取名称。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getName() {
        return "jsonb";
    }

    /**
     * 功能：保存或创建类型。
     * 参数：无。
     * 返回：判断结果。
     */
    @Override
    protected boolean registerUnderJavaType() {
        return true;
    }

    /**
     * 功能：更新`Parameter Values`。
     * 参数：
     * - `parameters`：`parameters` 参数。
     * 返回：无。
     */
    @Override
    public void setParameterValues(Properties parameters) {
        ((JsonTypeDescriptor) getJavaTypeDescriptor())
                .setParameterValues(parameters);
    }
}
