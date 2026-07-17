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
package org.thingsboard.server.common.data.msg;

/**
 * Created by ashvayka on 19.01.18.
 */
/**
 * 中文说明：
 * 1. `TbNodeConnectionType` 是 ThingsBoard Common Data 中承载规则节点信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
public final class TbNodeConnectionType {

    /**
     * `SUCCESS`常量，用于统一引用固定值。
     */
    public static final String SUCCESS = "Success";
    public static final String FAILURE = "Failure";

    /**
     * `TRUE`常量，用于统一引用固定值。
     */
    public static final String TRUE = "True";
    public static final String FALSE = "False";

    /**
     * `OTHER`常量，用于统一引用固定值。
     */
    public static final String OTHER = "Other";

}
