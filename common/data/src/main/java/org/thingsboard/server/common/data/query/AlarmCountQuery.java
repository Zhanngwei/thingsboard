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
package org.thingsboard.server.common.data.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.id.UserId;

import java.util.List;

/**
 * 中文说明：
 * 1. `AlarmCountQuery` 是 ThingsBoard Common Data 中承载告警信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `EntityCountQuery`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@ToString
public class AlarmCountQuery extends EntityCountQuery {
    /**
     * 时间戳，用于标识当前数据或事件发生的时间。
     */
    private long startTs;
    private long endTs;
    /**
     * 时间窗口，用于控制时间范围或等待时长。
     */
    private long timeWindow;
    private List<String> typeList;
    /**
     * 状态列表，用于保存一组待处理对象。
     */
    private List<AlarmSearchStatus> statusList;
    private List<AlarmSeverity> severityList;
    /**
     * 是否满足`searchPropagatedAlarms`条件。
     */
    private boolean searchPropagatedAlarms;
    private UserId assigneeId;
}
