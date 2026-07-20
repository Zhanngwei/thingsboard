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
import lombok.Data;

/**
 * 中文说明：
 * 1. `LwM2mObject` 是 ThingsBoard Common Data 中承载 LwM2M 信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@ApiModel
@Data
public class LwM2mObject {
    /**
     * `id`ID，用于定位对应业务对象。
     */
    @ApiModelProperty(position = 1, value = "LwM2M Object id.", example = "19")
    int id;
    /**
     * 键ID，用于定位对应业务对象。
     */
    @ApiModelProperty(position = 2, value = "LwM2M Object key id.", example = "19_1.0")
    String keyId;
    /**
     * 名称，用于标识或展示当前对象。
     */
    @ApiModelProperty(position = 3, value = "LwM2M Object name.", example = "BinaryAppDataContainer")
    String name;
    /**
     * 是否满足`multiple`条件。
     */
    @ApiModelProperty(position = 4, value = "LwM2M Object multiple.", example = "true")
    boolean multiple;
    /**
     * 是否满足`mandatory`条件。
     */
    @ApiModelProperty(position = 5, value = "LwM2M Object mandatory.", example = "false")
    boolean mandatory;
    /**
     * `instances`列表，用于保存一组待处理对象。
     */
    @ApiModelProperty(position = 6, value = "LwM2M Object instances.")
    LwM2mInstance [] instances;
}
