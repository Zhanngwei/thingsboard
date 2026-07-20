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
package org.thingsboard.server.common.data.objects;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Victor Basanets on 9/05/2017.
 */
/**
 * 中文说明：
 * 1. `AttributesEntityView` 是 ThingsBoard Common Data 中承载实体视图信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `Serializable`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Data
@ApiModel
@NoArgsConstructor
public class AttributesEntityView implements Serializable {

    @ApiModelProperty(position = 1, required = true, value = "List of client-side attribute keys to expose", example = "currentConfiguration")
    private List<String> cs = new ArrayList<>();
    @ApiModelProperty(position = 3, required = true, value = "List of server-side attribute keys to expose", example = "model")
    private List<String> ss = new ArrayList<>();
    @ApiModelProperty(position = 2, required = true, value = "List of shared attribute keys to expose", example = "targetConfiguration")
    private List<String> sh = new ArrayList<>();

    /**
     * 功能：创建 `AttributesEntityView` 实例，并初始化必要字段。
     * 参数：
     * - `cs`：数据列表。
     * - `ss`：数据列表。
     * - `sh`：数据列表。
     * 返回：新创建的对象实例。
     */
    public AttributesEntityView(List<String> cs,
                                List<String> ss,
                                List<String> sh) {

        this.cs = new ArrayList<>(cs);
        this.ss = new ArrayList<>(ss);
        this.sh = new ArrayList<>(sh);
    }

    /**
     * 功能：创建 `AttributesEntityView` 实例，并初始化必要字段。
     * 参数：
     * - `obj`：`obj` 参数。
     * 返回：新创建的对象实例。
     */
    public AttributesEntityView(AttributesEntityView obj) {
        this(obj.getCs(), obj.getSs(), obj.getSh());
    }
}
