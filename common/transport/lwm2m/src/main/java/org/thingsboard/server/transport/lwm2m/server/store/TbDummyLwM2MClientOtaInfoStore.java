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
package org.thingsboard.server.transport.lwm2m.server.store;

import org.thingsboard.server.transport.lwm2m.server.ota.firmware.LwM2MClientFwOtaInfo;
import org.thingsboard.server.transport.lwm2m.server.ota.software.LwM2MClientSwOtaInfo;

/**
 * 中文说明：
 * 1. `TbDummyLwM2MClientOtaInfoStore` 是 ThingsBoard Common Transport 中承载 LwM2M 信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `TbLwM2MClientOtaInfoStore`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
public class TbDummyLwM2MClientOtaInfoStore implements TbLwM2MClientOtaInfoStore {

    /**
     * 功能：获取`Fw`。
     * 参数：
     * - `endpoint`：`endpoint` 参数。
     * 返回：处理结果。
     */
    @Override
    public LwM2MClientFwOtaInfo getFw(String endpoint) {
        return null;
    }

    /**
     * 功能：获取`Sw`。
     * 参数：
     * - `endpoint`：`endpoint` 参数。
     * 返回：处理结果。
     */
    @Override
    public LwM2MClientSwOtaInfo getSw(String endpoint) {
        return null;
    }

    /**
     * 功能：执行 `putFw` 对应的处理。
     * 参数：
     * - `info`：`info` 参数。
     * 返回：无。
     */
    @Override
    public void putFw(LwM2MClientFwOtaInfo info) {

    }

    /**
     * 功能：执行 `putSw` 对应的处理。
     * 参数：
     * - `info`：`info` 参数。
     * 返回：无。
     */
    @Override
    public void putSw(LwM2MClientSwOtaInfo info) {

    }
}
