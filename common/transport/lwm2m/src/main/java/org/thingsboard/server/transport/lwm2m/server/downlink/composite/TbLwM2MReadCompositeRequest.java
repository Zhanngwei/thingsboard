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
package org.thingsboard.server.transport.lwm2m.server.downlink.composite;

import lombok.Builder;
import lombok.Getter;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.response.ReadCompositeResponse;
import org.thingsboard.server.transport.lwm2m.server.LwM2MOperationType;
import org.thingsboard.server.transport.lwm2m.server.downlink.HasContentFormat;

import java.util.Optional;

/**
 * 中文说明：
 * 1. `TbLwM2MReadCompositeRequest` 是 ThingsBoard Common Transport 中承载请求信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `AbstractTbLwM2MTargetedDownlinkCompositeRequest`、`HasContentFormat`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
public class TbLwM2MReadCompositeRequest extends AbstractTbLwM2MTargetedDownlinkCompositeRequest<ReadCompositeResponse> implements HasContentFormat {


    /**
     * 当前请求对象，封装本次处理需要的输入信息。
     */
    private final Optional<ContentFormat> requestContentFormatOpt;

    /**
     * 当前响应对象，封装处理完成后的返回信息。
     */
    @Getter
    private final ContentFormat responseContentFormat;

    /**
     * 功能：创建 `TbLwM2MReadCompositeRequest` 实例，并初始化必要字段。
     * 参数：
     * - `versionedIds`：`versionedIds` 参数。
     * - `timeout`：`timeout` 参数。
     * - `requestContentFormat`：请求对象。
     * - `responseContentFormat`：响应对象。
     * 返回：新创建的对象实例。
     */
    @Builder
    private TbLwM2MReadCompositeRequest(String [] versionedIds, long timeout, ContentFormat requestContentFormat, ContentFormat responseContentFormat) {
        super(versionedIds, timeout);
        this.requestContentFormatOpt = Optional.ofNullable(requestContentFormat);
        this.responseContentFormat = responseContentFormat;
    }

    /**
     * 功能：获取类型。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public LwM2MOperationType getType() {
        return LwM2MOperationType.READ_COMPOSITE;
    }

    /**
     * 功能：获取内容格式。
     * 参数：无。
     * 返回：可能存在的结果。
     */
    @Override
    public Optional<ContentFormat> getRequestContentFormat() {
        return this.requestContentFormatOpt;
    }
}
