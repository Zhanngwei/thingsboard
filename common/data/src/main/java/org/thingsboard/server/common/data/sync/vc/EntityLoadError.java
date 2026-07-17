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
package org.thingsboard.server.common.data.sync.vc;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.ClassUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.EntityId;

import java.io.Serializable;

/**
 * 中文说明：
 * 1. `EntityLoadError` 是 ThingsBoard Common Data 中表示实体失败语义的异常类型。
 * 2. 它用于把特定错误原因传递给上层处理流程。
 * 3. 异常中保存的消息、错误码或上下文帮助调用方判断失败类型。
 * 4. 直接依赖的类型边界包括 `Serializable`。
 * 5. 独立异常类型让调用方能够精确捕获该类错误，而不是依赖文本判断。
 * 6. 阅读时重点关注创建位置、携带信息和上层捕获后的处理结果。
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EntityLoadError implements Serializable {

    /**
     * 版本号常量，用于统一引用固定值。
     */
    private static final long serialVersionUID = 7538450180582109391L;

    /**
     * 类型，用于区分不同处理分支。
     */
    private String type;
    private EntityId source;
    /**
     * 目标对象，表示当前对象的对应属性。
     */
    private EntityId target;
    private String message;

    /**
     * 功能：执行 `credentialsError` 对应的处理。
     * 参数：
     * - `sourceId`：`sourceId`ID。
     * 返回：处理结果。
     */
    public static EntityLoadError credentialsError(EntityId sourceId) {
        return EntityLoadError.builder().type("DEVICE_CREDENTIALS_CONFLICT").source(sourceId).build();
    }

    /**
     * 功能：执行 `referenceEntityError` 对应的处理。
     * 参数：
     * - `sourceId`：`sourceId`ID。
     * - `targetId`：目标对象ID。
     * 返回：处理结果。
     */
    public static EntityLoadError referenceEntityError(EntityId sourceId, EntityId targetId) {
        return EntityLoadError.builder().type("MISSING_REFERENCED_ENTITY").source(sourceId).target(targetId).build();
    }

    /**
     * 功能：执行 `runtimeError` 对应的处理。
     * 参数：
     * - `e`：`e` 参数。
     * 返回：处理结果。
     */
    public static EntityLoadError runtimeError(Throwable e) {
        String message = e.getMessage();
        if (StringUtils.isEmpty(message)) {
            message = "unexpected error (" + ClassUtils.getShortClassName(e.getClass()) + ")";
        }
        return EntityLoadError.builder().type("RUNTIME").message(message).build();
    }

}
