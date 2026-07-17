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
import org.thingsboard.monitoring.config.MonitoringTarget;

import java.util.UUID;

/**
 * 中文说明：
 * 1. `TransportMonitoringTarget` 是 ThingsBoard Monitoring 中负责传输层接入或传输适配的类型。
 * 2. 它处理连接、会话、协议消息或平台传输消息之间的转换。
 * 3. 类中的状态和配置用于控制当前协议交互的具体行为。
 * 4. 直接依赖的类型边界包括 `MonitoringTarget`。
 * 5. 单独的传输类型可以隔离协议细节，使平台内部继续使用统一消息模型。
 * 6. 阅读时重点关注入站消息入口、会话状态和消息提交位置。
 */
@Data
public class TransportMonitoringTarget implements MonitoringTarget {

    /**
     * 基础访问地址，用于定位外部资源或本地资源。
     */
    private String baseUrl;
    private DeviceConfig device; // set manually during initialization
    /**
     * 队列，用于标识消息投递或消费的队列。
     */
    private String queue;
    private boolean checkDomainIps;

    /**
     * 功能：获取设备ID。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public UUID getDeviceId() {
        return device.getId();
    }

    /**
     * 功能：获取队列。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getQueue() {
        return StringUtils.defaultIfEmpty(queue, "Main");
    }

}
