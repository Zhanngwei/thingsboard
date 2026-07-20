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
package org.thingsboard.server.service.ws.telemetry.cmd.v2;

import lombok.Data;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.IntervalType;

import java.util.List;

/**
 * 中文说明：
 * 1. `EntityHistoryCmd` 是 ThingsBoard Application 中承载实体信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `GetTsCmd`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Data
public class EntityHistoryCmd implements GetTsCmd {

    /**
     * `keys`列表，用于保存一组待处理对象。
     */
    private List<String> keys;
    private long startTs;
    /**
     * 时间戳，用于标识当前数据或事件发生的时间。
     */
    private long endTs;
    private IntervalType intervalType;
    /**
     * 时间间隔，用于控制时间范围或等待时长。
     */
    private long interval;
    private String timeZoneId;
    /**
     * 数量限制，用于控制数量、位置或分页范围。
     */
    private int limit;
    private Aggregation agg;
    /**
     * 是否满足`fetchLatestPreviousPoint`条件。
     */
    private boolean fetchLatestPreviousPoint;

}
