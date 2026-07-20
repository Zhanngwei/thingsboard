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
package org.thingsboard.server.service.subscription;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.service.ws.WebSocketService;
import org.thingsboard.server.service.ws.WebSocketSessionRef;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.EntityCountUpdate;

/**
 * 中文说明：
 * 1. `TbEntityCountSubCtx` 是 ThingsBoard Application 中围绕实体提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 直接依赖的类型边界包括 `TbAbstractSubCtx`。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
@Slf4j
public class TbEntityCountSubCtx extends TbAbstractSubCtx<EntityCountQuery> {

    /**
     * `result` 字段，保存当前对象的对应属性。
     */
    private volatile int result;

    /**
     * 功能：创建 `TbEntityCountSubCtx` 实例，并初始化必要字段。
     * 参数：
     * - `serviceId`：服务ID。
     * - `wsService`：服务对象。
     * - `entityService`：服务对象。
     * - `localSubscriptionService`：服务对象。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public TbEntityCountSubCtx(String serviceId, WebSocketService wsService, EntityService entityService,
                               TbLocalSubscriptionService localSubscriptionService, AttributesService attributesService,
                               SubscriptionServiceStatistics stats, WebSocketSessionRef sessionRef, int cmdId) {
        super(serviceId, wsService, entityService, localSubscriptionService, attributesService, stats, sessionRef, cmdId);
    }

    /**
     * 功能：获取数据。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void fetchData() {
        result = (int) entityService.countEntitiesByQuery(getTenantId(), getCustomerId(), query);
        sendWsMsg(new EntityCountUpdate(cmdId, result));
    }

    /**
     * 功能：执行 `update` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    protected void update() {
        int newCount = (int) entityService.countEntitiesByQuery(getTenantId(), getCustomerId(), query);
        if (newCount != result) {
            result = newCount;
            sendWsMsg(new EntityCountUpdate(cmdId, result));
        }
    }

    /**
     * 功能：判断`Dynamic`。
     * 参数：无。
     * 返回：判断结果。
     */
    @Override
    public boolean isDynamic() {
        return true;
    }
}
