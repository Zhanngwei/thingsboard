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
package org.thingsboard.server.service.script;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.thingsboard.script.api.ScriptType;
import org.thingsboard.script.api.js.JsInvokeService;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.UUID;

/**
 * 中文说明：
 * 1. `MockJsInvokeService` 是 ThingsBoard Application 中负责 `Mock Js Invoke` 的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `JsInvokeService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "js", value = "evaluator", havingValue = "mock")
public class MockJsInvokeService implements JsInvokeService {

    /**
     * 功能：执行 `eval` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `scriptType`：类型。
     * - `scriptBody`：`scriptBody` 参数。
     * - `argNames`：名称。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<UUID> eval(TenantId tenantId, ScriptType scriptType, String scriptBody, String... argNames) {
        log.warn("eval {} {} {} {}", tenantId, scriptType, scriptBody, argNames);
        return Futures.immediateFuture(UUID.randomUUID());
    }

    /**
     * 功能：执行 `invokeScript` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `scriptId`：`scriptId`ID。
     * - `args`：传入程序的参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<Object> invokeScript(TenantId tenantId, CustomerId customerId, UUID scriptId, Object... args) {
        log.warn("invokeFunction {} {} {} {}", tenantId, customerId, scriptId, args);
        return Futures.immediateFuture("{}");
    }

    /**
     * 功能：执行 `release` 对应的处理。
     * 参数：
     * - `scriptId`：`scriptId`ID。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<Void> release(UUID scriptId) {
        log.warn("release {}", scriptId);
        return Futures.immediateFuture(null);
    }
}
