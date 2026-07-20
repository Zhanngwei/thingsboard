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
package org.thingsboard.server.common.data;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 中文说明：
 * 1. `SystemInfoData` 是 ThingsBoard Common Data 中承载 `System` 信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Data
public class SystemInfoData {
    /**
     * 服务ID，用于定位对应业务对象。
     */
    @ApiModelProperty(position = 1, value = "Service Id.")
    private String serviceId;
    /**
     * 类型，提供当前类调用的业务操作。
     */
    @ApiModelProperty(position = 2, value = "Service type.")
    private String serviceType;
    /**
     * `cpuUsage` 字段，保存当前对象的对应属性。
     */
    @ApiModelProperty(position = 3, value = "CPU usage, in percent.")
    private Long cpuUsage;
    /**
     * 数量，用于控制数量、位置或分页范围。
     */
    @ApiModelProperty(position = 4, value = "Total CPU usage.")
    private Long cpuCount;
    /**
     * `memoryUsage` 字段，保存当前对象的对应属性。
     */
    @ApiModelProperty(position = 5, value = "Memory usage, in percent.")
    private Long memoryUsage;
    /**
     * `totalMemory` 字段，保存当前对象的对应属性。
     */
    @ApiModelProperty(position = 6, value = "Total memory in bytes.")
    private Long totalMemory;
    /**
     * `discUsage` 字段，保存当前对象的对应属性。
     */
    @ApiModelProperty(position = 7, value = "Disk usage, in percent.")
    private Long discUsage;
    /**
     * `totalDiscSpace` 字段，保存当前对象的对应属性。
     */
    @ApiModelProperty(position = 8, value = "Total disc space in bytes.")
    private Long totalDiscSpace;

}
