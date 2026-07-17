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
package org.thingsboard.rule.engine.api;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 中文说明：
 * 1. `TbEmail` 是 ThingsBoard Rule Engine API 中围绕 `Tb Email` 提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 它直接协作于构造参数、字段类型和公开方法涉及的对象。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
@Data
@Builder
public class TbEmail {

    /**
     * `from` 字段，保存当前对象的对应属性。
     */
    private final String from;
    /**
     * `to` 字段，保存当前对象的对应属性。
     */
    private final String to;
    /**
     * `cc` 字段，保存当前对象的对应属性。
     */
    private final String cc;
    /**
     * `bcc` 字段，保存当前对象的对应属性。
     */
    private final String bcc;
    /**
     * `subject` 字段，保存当前对象的对应属性。
     */
    private final String subject;
    /**
     * `body` 字段，保存当前对象的对应属性。
     */
    private final String body;
    /**
     * `images`映射关系，用于按键查找对应值。
     */
    private final Map<String, String> images;
    /**
     * 是否满足`html`条件。
     */
    private final boolean html;

}
