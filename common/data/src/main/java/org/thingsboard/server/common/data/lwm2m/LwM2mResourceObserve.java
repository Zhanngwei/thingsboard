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
package org.thingsboard.server.common.data.lwm2m;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.stream.Stream;

/**
 * 中文说明：
 * 1. `LwM2mResourceObserve` 是 ThingsBoard Common Data 中承载 LwM2M 信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@ApiModel
@Data
@AllArgsConstructor
public class LwM2mResourceObserve {
    /**
     * `id`ID，用于定位对应业务对象。
     */
    @ApiModelProperty(position = 1, value = "LwM2M Resource Observe id.", example = "0")
    int id;
    /**
     * 名称，用于标识或展示当前对象。
     */
    @ApiModelProperty(position = 2, value = "LwM2M Resource Observe name.", example = "Data")
    String name;
    /**
     * 是否满足`observe`条件。
     */
    @ApiModelProperty(position = 3, value = "LwM2M Resource Observe observe.", example = "false")
    boolean observe;
    /**
     * 是否满足属性条件。
     */
    @ApiModelProperty(position = 4, value = "LwM2M Resource Observe attribute.", example = "false")
    boolean attribute;
    /**
     * 是否满足遥测条件。
     */
    @ApiModelProperty(position = 5, value = "LwM2M Resource Observe telemetry.", example = "false")
    boolean telemetry;
    /**
     * 键，用于标识或展示当前对象。
     */
    @ApiModelProperty(position = 6, value = "LwM2M Resource Observe key name.", example = "data")
    String keyName;

    /**
     * 功能：创建 `LwM2mResourceObserve` 实例，并初始化必要字段。
     * 参数：
     * - `id`：`id`ID。
     * - `name`：名称。
     * - `observe`：`observe` 参数。
     * - `attribute`：`attribute` 参数。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public LwM2mResourceObserve(int id, String name, boolean observe, boolean attribute, boolean telemetry) {
        this.id = id;
        this.name = name;
        this.observe = observe;
        this.attribute = attribute;
        this.telemetry = telemetry;
        this.keyName = getCamelCase (this.name);
    }

    /**
     * 功能：获取`Camel Case`。
     * 参数：
     * - `name`：名称。
     * 返回：文本结果。
     */
    private String getCamelCase (String name) {
        name = name.replaceAll("-", " ");
        name = name.replaceAll("_", " ");
        String [] nameCamel1 = name.split(" ");
        String [] nameCamel2 = new String[nameCamel1.length];
        int[] idx = { 0 };
        Stream.of(nameCamel1).forEach((s -> {
            nameCamel2[idx[0]] = toProperCase(idx[0]++,  s);
        }));
        return String.join("", nameCamel2);
    }

    /**
     * 功能：执行 `toProperCase` 对应的处理。
     * 参数：
     * - `idx`：`idx` 参数。
     * - `s`：`s` 参数。
     * 返回：文本结果。
     */
    private String toProperCase(int idx, String s) {
        if (!s.isEmpty() && s.length()> 0) {
            String s1 = (idx == 0) ? s.substring(0, 1).toLowerCase() : s.substring(0, 1).toUpperCase();
            String s2 = "";
            if (s.length()> 1) s2 = s.substring(1).toLowerCase();
            s = s1 + s2;
        }
        return s;
    }
}
