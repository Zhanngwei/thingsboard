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
package org.thingsboard.server.service.telemetry;

import com.google.common.util.concurrent.FutureCallback;
import org.thingsboard.rule.engine.api.RuleEngineTelemetryService;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;

import java.util.List;

/**
 * Created by ashvayka on 27.03.18.
 */
/**
 * 中文说明：
 * 1. `InternalTelemetryService` 是 ThingsBoard Application 中定义遥测数据能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 直接依赖的类型边界包括 `RuleEngineTelemetryService`。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface InternalTelemetryService extends RuleEngineTelemetryService {

    /**
     * 功能：保存或创建`And Notify Internal`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `ts`：时间戳。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void saveAndNotifyInternal(TenantId tenantId, EntityId entityId, List<TsKvEntry> ts, FutureCallback<Integer> callback);

    /**
     * 功能：保存或创建`And Notify Internal`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `ts`：时间戳。
     * - `ttl`：`ttl` 参数。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    void saveAndNotifyInternal(TenantId tenantId, EntityId entityId, List<TsKvEntry> ts, long ttl, FutureCallback<Integer> callback);

    /**
     * 功能：保存或创建`And Notify Internal`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * - `attributes`：数据列表。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    void saveAndNotifyInternal(TenantId tenantId, EntityId entityId, String scope, List<AttributeKvEntry> attributes, boolean notifyDevice, FutureCallback<Void> callback);

    /**
     * 功能：保存或创建`Latest And Notify Internal`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `ts`：时间戳。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void saveLatestAndNotifyInternal(TenantId tenantId, EntityId entityId, List<TsKvEntry> ts, FutureCallback<Void> callback);

    /**
     * 功能：删除或清理`And Notify Internal`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * - `keys`：键。
     * - 其余参数：补充处理条件。
     * 返回：无。
     */
    void deleteAndNotifyInternal(TenantId tenantId, EntityId entityId, String scope, List<String> keys, boolean notifyDevice, FutureCallback<Void> callback);

    /**
     * 功能：删除或清理`Latest Internal`。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `keys`：键。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    void deleteLatestInternal(TenantId tenantId, EntityId entityId, List<String> keys, FutureCallback<Void> callback);

}
