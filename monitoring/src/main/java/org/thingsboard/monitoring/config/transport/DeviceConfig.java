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
package org.thingsboard.monitoring.config.transport;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.thingsboard.server.common.data.security.DeviceCredentials;

import java.util.UUID;

/**
 * 中文说明：
 * 1. `DeviceConfig` 是 ThingsBoard Monitoring 中描述设备行为的配置类型。
 * 2. 它集中保存该组件启动或运行时需要的可配置选项。
 * 3. 字段值决定功能开关、限制条件、地址或处理策略等具体行为。
 * 4. 它直接协作于配置加载组件和使用这些配置的运行类型。
 * 5. 独立配置对象可以避免大量零散参数在调用链中传递。
 * 6. 阅读时重点关注默认值、必填字段和配置项之间的约束关系。
 */
@Data
public class DeviceConfig {

    /**
     * `id`ID，用于定位对应业务对象。
     */
    private UUID id;
    private String name;
    /**
     * 凭据，用于认证或安全校验。
     */
    private DeviceCredentials credentials;

    /**
     * 功能：更新`Id`。
     * 参数：
     * - `id`：`id`ID。
     * 返回：无。
     */
    public void setId(String id) {
        this.id = StringUtils.isNotEmpty(id) ? UUID.fromString(id) : null;
    }

}
