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
package org.thingsboard.server.common.data.alarm;

import java.util.Collection;
import java.util.Optional;

/**
 * 中文说明：
 * 1. `AlarmStatusFilter` 是 ThingsBoard Common Data 中处理告警的处理器。
 * 2. 它把单一处理步骤封装为可调用、可替换的组件。
 * 3. 输入通常来自上游事件、网络消息或异步回调，输出交给下一处理步骤。
 * 4. 它直接协作于事件源、上下文对象和后续处理组件。
 * 5. 独立处理器可以缩小单个流程的职责范围，并便于组合处理链。
 * 6. 阅读时重点关注入口方法、条件分支和处理完成后的转发行为。
 */
public class AlarmStatusFilter {

    private static final AlarmStatusFilter EMPTY = new AlarmStatusFilter(Optional.empty(), Optional.empty());

    /**
     * 是否满足`clearFilter`条件。
     */
    private final Optional<Boolean> clearFilter;
    private final Optional<Boolean> ackFilter;

    /**
     * 功能：创建 `AlarmStatusFilter` 实例，并初始化必要字段。
     * 参数：
     * - `clearFilter`：`clearFilter` 参数。
     * - `ackFilter`：`ackFilter` 参数。
     * 返回：新创建的对象实例。
     */
    private AlarmStatusFilter(Optional<Boolean> clearFilter, Optional<Boolean> ackFilter) {
        this.clearFilter = clearFilter;
        this.ackFilter = ackFilter;
    }

    /**
     * 功能：执行 `from` 对应的处理。
     * 参数：
     * - `query`：`query` 参数。
     * 返回：处理结果。
     */
    public static AlarmStatusFilter from(AlarmQuery query) {
        if (query.getSearchStatus() != null) {
            return AlarmStatusFilter.from(query.getSearchStatus());
        } else if (query.getStatus() != null) {
            return AlarmStatusFilter.from(query.getStatus());
        }
        return AlarmStatusFilter.empty();
    }

    /**
     * 功能：执行 `from` 对应的处理。
     * 参数：
     * - `alarmSearchStatus`：`alarmSearchStatus` 参数。
     * 返回：处理结果。
     */
    public static AlarmStatusFilter from(AlarmSearchStatus alarmSearchStatus) {
        switch (alarmSearchStatus) {
            case ACK:
                return new AlarmStatusFilter(Optional.empty(), Optional.of(true));
            case UNACK:
                return new AlarmStatusFilter(Optional.empty(), Optional.of(false));
            case ACTIVE:
                return new AlarmStatusFilter(Optional.of(false), Optional.empty());
            case CLEARED:
                return new AlarmStatusFilter(Optional.of(true), Optional.empty());
            default:
                return EMPTY;
        }
    }

    /**
     * 功能：执行 `from` 对应的处理。
     * 参数：
     * - `alarmStatus`：`alarmStatus` 参数。
     * 返回：处理结果。
     */
    public static AlarmStatusFilter from(AlarmStatus alarmStatus) {
        switch (alarmStatus) {
            case ACTIVE_UNACK:
                return new AlarmStatusFilter(Optional.of(false), Optional.of(false));
            case ACTIVE_ACK:
                return new AlarmStatusFilter(Optional.of(false), Optional.of(true));
            case CLEARED_UNACK:
                return new AlarmStatusFilter(Optional.of(true), Optional.of(false));
            case CLEARED_ACK:
                return new AlarmStatusFilter(Optional.of(true), Optional.of(true));
            default:
                return EMPTY;
        }
    }

    /**
     * 功能：执行 `empty` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public static AlarmStatusFilter empty() {
        return EMPTY;
    }

    /**
     * 功能：判断`Any Filter`。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean hasAnyFilter() {
        return clearFilter.isPresent() || ackFilter.isPresent();
    }

    /**
     * 功能：判断`Clear Filter`。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean hasClearFilter() {
        return clearFilter.isPresent();
    }

    /**
     * 功能：判断`Ack Filter`。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean hasAckFilter() {
        return ackFilter.isPresent();
    }

    /**
     * 功能：获取`Clear Filter`。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean getClearFilter() {
        return clearFilter.orElseThrow(() -> new RuntimeException("Clear filter is not set! Use `hasClearFilter` to check."));
    }

    /**
     * 功能：获取`Ack Filter`。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean getAckFilter() {
        return ackFilter.orElseThrow(() -> new RuntimeException("Ack filter is not set! Use `hasAckFilter` to check."));
    }


    /**
     * 功能：执行 `from` 对应的处理。
     * 参数：
     * - `statuses`：数据列表。
     * 返回：处理结果。
     */
    public static AlarmStatusFilter from(Collection<AlarmSearchStatus> statuses) {
        if (statuses == null || statuses.isEmpty() || statuses.contains(AlarmSearchStatus.ANY)) {
            return EMPTY;
        }
        boolean clearFilter = statuses.contains(AlarmSearchStatus.CLEARED);
        boolean activeFilter = statuses.contains(AlarmSearchStatus.ACTIVE);
        Optional<Boolean> clear = Optional.empty();
        if (clearFilter && !activeFilter || !clearFilter && activeFilter) {
            clear = Optional.of(clearFilter);
        }

        boolean ackFilter = statuses.contains(AlarmSearchStatus.ACK);
        boolean unackFilter = statuses.contains(AlarmSearchStatus.UNACK);
        Optional<Boolean> ack = Optional.empty();
        if (ackFilter && !unackFilter || !ackFilter && unackFilter) {
            ack = Optional.of(ackFilter);
        }
        return new AlarmStatusFilter(clear, ack);
    }

    /**
     * 功能：执行 `matches` 对应的处理。
     * 参数：
     * - `alarm`：`alarm` 参数。
     * 返回：判断结果。
     */
    public boolean matches(Alarm alarm) {
        return ackFilter.map(ackFilter -> ackFilter.equals(alarm.isAcknowledged())).orElse(true) &&
                clearFilter.map(clearedFilter -> clearedFilter.equals(alarm.isCleared())).orElse(true);
    }

}
