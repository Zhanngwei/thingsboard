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
package org.thingsboard.server.actors.device;

import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.TbActor;
import org.thingsboard.server.actors.TbActorId;
import org.thingsboard.server.actors.TbEntityActorId;
import org.thingsboard.server.actors.service.ContextBasedCreator;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;

/**
 * 中文说明：
 * 1. `DeviceActorCreator` 是 ThingsBoard Application 中创建或提供设备对象的构造组件。
 * 2. 它根据输入配置、类型或上下文选择合适的具体实现。
 * 3. 创建细节被集中在该类型中，调用方只依赖稳定的创建入口。
 * 4. 直接依赖的类型边界包括 `ContextBasedCreator`。
 * 5. 独立工厂可以避免调用方了解构造顺序和实现类选择规则。
 * 6. 阅读时重点关注实现选择条件、默认分支和对象初始化参数。
 */
public class DeviceActorCreator extends ContextBasedCreator {

    /**
     * 租户ID，用于定位对应业务对象。
     */
    private final TenantId tenantId;
    private final DeviceId deviceId;

    /**
     * 功能：创建 `DeviceActorCreator` 实例，并初始化必要字段。
     * 参数：
     * - `context`：处理上下文。
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * 返回：新创建的对象实例。
     */
    public DeviceActorCreator(ActorSystemContext context, TenantId tenantId, DeviceId deviceId) {
        super(context);
        this.tenantId = tenantId;
        this.deviceId = deviceId;
    }

    /**
     * 功能：保存或创建Actor 实例。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbActorId createActorId() {
        return new TbEntityActorId(deviceId);
    }

    /**
     * 功能：保存或创建Actor 实例。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbActor createActor() {
        return new DeviceActor(context, tenantId, deviceId);
    }

}
