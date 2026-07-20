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
package org.thingsboard.server.transport.lwm2m.server.log;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.queue.util.TbLwM2mTransportComponent;
import org.thingsboard.server.transport.lwm2m.server.LwM2mTransportServerHelper;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;

import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.LOG_LWM2M_TELEMETRY;

/**
 * 中文说明：
 * 1. `DefaultLwM2MTelemetryLogService` 是 ThingsBoard Common Transport 中负责遥测数据的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `LwM2MTelemetryLogService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Slf4j
@Service
@TbLwM2mTransportComponent
@RequiredArgsConstructor
public class DefaultLwM2MTelemetryLogService implements LwM2MTelemetryLogService {

    /**
     * `helper` 字段，保存当前对象的对应属性。
     */
    private final LwM2mTransportServerHelper helper;

    /**
     * 功能：执行 `log` 对应的处理。
     * 参数：
     * - `client`：客户端对象。
     * - `logMsg`：待处理消息。
     * 返回：无。
     */
    @Override
    public void log(LwM2mClient client, String logMsg) {
        if (logMsg != null && client != null && client.getSession() != null) {
            if (logMsg.length() > 1024) {
                logMsg = logMsg.substring(0, 1024);
            }
            this.helper.sendParametersOnThingsboardTelemetry(this.helper.getKvStringtoThingsboard(LOG_LWM2M_TELEMETRY, logMsg), client.getSession(), client.getKeyTsLatestMap());
        }
    }

}
