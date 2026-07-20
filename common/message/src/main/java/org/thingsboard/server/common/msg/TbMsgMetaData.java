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
package org.thingsboard.server.common.msg;

import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ashvayka on 13.01.18.
 */
/**
 * 中文说明：
 * 1. `TbMsgMetaData` 是 ThingsBoard Common Message 中承载消息信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `Serializable`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Data
public final class TbMsgMetaData implements Serializable {

    public static final TbMsgMetaData EMPTY = new TbMsgMetaData(0);

    /**
     * 数据映射关系，用于按键查找对应值。
     */
    private final Map<String, String> data;

    /**
     * 功能：创建 `TbMsgMetaData` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public TbMsgMetaData() {
        this.data = new ConcurrentHashMap<>();
    }

    /**
     * 功能：创建 `TbMsgMetaData` 实例，并初始化必要字段。
     * 参数：
     * - `data`：待处理数据。
     * 返回：新创建的对象实例。
     */
    public TbMsgMetaData(Map<String, String> data) {
        this.data = new ConcurrentHashMap<>();
        data.forEach(this::putValue);
    }

    /**
     * Internal constructor to create immutable TbMsgMetaData.EMPTY
     * */
    /**
     * 功能：创建 `TbMsgMetaData` 实例，并初始化必要字段。
     * 参数：
     * - `ignored`：`ignored` 参数。
     * 返回：新创建的对象实例。
     */
    private TbMsgMetaData(int ignored) {
        this.data = Collections.emptyMap();
    }

    /**
     * 功能：获取值。
     * 参数：
     * - `key`：键。
     * 返回：文本结果。
     */
    public String getValue(String key) {
        return this.data.get(key);
    }

    /**
     * 功能：执行 `putValue` 对应的处理。
     * 参数：
     * - `key`：键。
     * - `value`：值。
     * 返回：无。
     */
    public void putValue(String key, String value) {
        if (key != null && value != null) {
            this.data.put(key, value);
        }
    }

    /**
     * 功能：执行 `values` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public Map<String, String> values() {
        return new HashMap<>(this.data);
    }

    /**
     * 功能：执行 `copy` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public TbMsgMetaData copy() {
        return new TbMsgMetaData(this.data);
    }
}
