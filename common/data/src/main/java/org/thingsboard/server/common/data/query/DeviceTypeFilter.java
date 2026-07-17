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
package org.thingsboard.server.common.data.query;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;

/**
 * 中文说明：
 * 1. `DeviceTypeFilter` 是 ThingsBoard Common Data 中处理设备的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 直接依赖的类型边界包括 `EntityFilter`。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
@Setter
public class DeviceTypeFilter implements EntityFilter {

    /**
     * Replaced by {@link DeviceTypeFilter#getDeviceTypes()} instead.
     */
    /**
     * 设备，用于区分不同处理分支。
     */
    @Deprecated(since = "3.5", forRemoval = true)
    private String deviceType;

    /**
     * 设备列表，用于保存一组待处理对象。
     */
    private List<String> deviceTypes;

    /**
     * 功能：获取设备。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    public List<String> getDeviceTypes() {
        return !CollectionUtils.isEmpty(deviceTypes) ? deviceTypes : Collections.singletonList(deviceType);
    }

    /**
     * 设备对象，用于描述当前业务场景。
     */
    @Getter
    private String deviceNameFilter;

    /**
     * 功能：创建 `DeviceTypeFilter` 实例，并初始化必要字段。
     * 参数：
     * - `deviceTypes`：设备信息或设备标识。
     * - `deviceNameFilter`：设备信息或设备标识。
     * 返回：新创建的对象实例。
     */
    public DeviceTypeFilter(List<String> deviceTypes, String deviceNameFilter) {
        this.deviceTypes = deviceTypes;
        this.deviceNameFilter = deviceNameFilter;
    }

    /**
     * 功能：获取类型。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public EntityFilterType getType() {
        return EntityFilterType.DEVICE_TYPE;
    }
}
