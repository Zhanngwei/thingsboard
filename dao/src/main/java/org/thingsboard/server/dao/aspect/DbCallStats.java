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
package org.thingsboard.server.dao.aspect;

import lombok.Data;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. `DbCallStats` 是 ThingsBoard DAO 中负责统计数据存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 它直接协作于持久化模型、查询实现和对应领域服务。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
 */
@Data
public class DbCallStats {

    /**
     * 租户ID，用于定位对应业务对象。
     */
    private final TenantId tenantId;
    private final ConcurrentMap<String, MethodCallStats> methodStats = new ConcurrentHashMap<>();
    private final AtomicInteger successCalls = new AtomicInteger();
    private final AtomicInteger failureCalls = new AtomicInteger();

    /**
     * 功能：处理`on Method Call`。
     * 参数：
     * - `methodName`：名称。
     * - `success`：`success` 参数。
     * - `executionTime`：`executionTime` 参数。
     * 返回：无。
     */
    public void onMethodCall(String methodName, boolean success, long executionTime) {
        var methodCallStats = methodStats.computeIfAbsent(methodName, m -> new MethodCallStats());
        methodCallStats.getExecutions().incrementAndGet();
        methodCallStats.getTiming().addAndGet(executionTime);
        if (success) {
            successCalls.incrementAndGet();
        } else {
            failureCalls.incrementAndGet();
            methodCallStats.getFailures().incrementAndGet();
        }
    }

    /**
     * 功能：执行 `snapshot` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public DbCallStatsSnapshot snapshot() {
        return DbCallStatsSnapshot.builder()
                .tenantId(tenantId)
                .totalSuccess(successCalls.get())
                .totalFailure(failureCalls.get())
                .methodStats(methodStats.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().snapshot())))
                .totalTiming(methodStats.values().stream().map(MethodCallStats::getTiming).map(AtomicLong::get).reduce(0L, Long::sum))
                .build();
    }

}
