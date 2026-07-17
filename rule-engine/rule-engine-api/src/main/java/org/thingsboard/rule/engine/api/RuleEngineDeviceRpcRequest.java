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
package org.thingsboard.rule.engine.api;

import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.UUID;

/**
 * Created by ashvayka on 02.04.18.
 */
/**
 * 中文说明：
 * 1. `RuleEngineDeviceRpcRequest` 是 ThingsBoard Rule Engine API 中承载请求信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Data
@Builder
public final class RuleEngineDeviceRpcRequest {

    /**
     * 租户ID，用于定位对应业务对象。
     */
    private final TenantId tenantId;
    /**
     * 设备ID，用于定位对应业务对象。
     */
    private final DeviceId deviceId;
    /**
     * 请求ID，用于定位对应业务对象。
     */
    private final int requestId;
    /**
     * 请求ID，用于定位对应业务对象。
     */
    private final UUID requestUUID;
    /**
     * 服务ID，用于定位对应业务对象。
     */
    private final String originServiceId;
    /**
     * 是否满足`oneway`条件。
     */
    private final boolean oneway;
    /**
     * 是否满足`persisted`条件。
     */
    private final boolean persisted;
    /**
     * `method` 字段，保存当前对象的对应属性。
     */
    private final String method;
    /**
     * `body` 字段，保存当前对象的对应属性。
     */
    private final String body;
    /**
     * 过期时间，用于判断当前对象是否仍然有效。
     */
    private final long expirationTime;
    /**
     * 是否满足`restApiCall`条件。
     */
    private final boolean restApiCall;
    /**
     * 扩展信息，表示当前对象的对应属性。
     */
    private final String additionalInfo;
    /**
     * `retries` 字段，保存当前对象的对应属性。
     */
    private final Integer retries;
}
