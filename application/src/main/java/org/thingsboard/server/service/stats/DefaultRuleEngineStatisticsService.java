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
package org.thingsboard.server.service.stats;

import com.google.common.util.concurrent.FutureCallback;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.usagerecord.ApiLimitService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.util.TbRuleEngineComponent;
import org.thingsboard.server.service.queue.TbRuleEngineConsumerStats;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. `DefaultRuleEngineStatisticsService` 是 ThingsBoard Application 中负责 `Rule Engine Statistics` 的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `RuleEngineStatisticsService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@TbRuleEngineComponent
@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultRuleEngineStatisticsService implements RuleEngineStatisticsService {

    /**
     * 队列常量，用于统一引用固定值。
     */
    public static final String TB_SERVICE_QUEUE = "TbServiceQueue";
    public static final String RULE_ENGINE_EXCEPTION = "ruleEngineException";
    public static final FutureCallback<Integer> CALLBACK = new FutureCallback<Integer>() {
        @Override
        public void onSuccess(@Nullable Integer result) {

        }

        @Override
        public void onFailure(Throwable t) {
            log.warn("Failed to persist statistics", t);
        }
    };

    /**
     * 服务，提供当前类调用的业务操作。
     */
    private final TbServiceInfoProvider serviceInfoProvider;
    private final TelemetrySubscriptionService tsService;
    /**
     * 资产集合，用于去重保存或快速判断对象是否存在。
     */
    private final AssetService assetService;
    private final ApiLimitService apiLimitService;
    private final Lock lock = new ReentrantLock();
    private final ConcurrentMap<TenantQueueKey, AssetId> tenantQueueAssets = new ConcurrentHashMap<>();

    /**
     * 消息，承载当前步骤需要处理的内容。
     */
    @Value("${queue.rule-engine.stats.max-error-message-length:4096}")
    private int maxErrorMessageLength;

    /**
     * 功能：上报队列。
     * 参数：
     * - `ts`：时间戳。
     * - `ruleEngineStats`：`ruleEngineStats` 参数。
     * 返回：无。
     */
    @Override
    public void reportQueueStats(long ts, TbRuleEngineConsumerStats ruleEngineStats) {
        String queueName = ruleEngineStats.getQueueName();
        ruleEngineStats.getTenantStats().forEach((id, stats) -> {
            try {
                TenantId tenantId = TenantId.fromUUID(id);
                AssetId serviceAssetId = getServiceAssetId(tenantId, queueName);
                if (stats.getTotalMsgCounter().get() > 0) {
                    List<TsKvEntry> tsList = stats.getCounters().entrySet().stream()
                            .map(kv -> new BasicTsKvEntry(ts, new LongDataEntry(kv.getKey(), (long) kv.getValue().get())))
                            .collect(Collectors.toList());
                    if (!tsList.isEmpty()) {
                        long ttl = apiLimitService.getLimit(tenantId, DefaultTenantProfileConfiguration::getQueueStatsTtlDays);
                        ttl = TimeUnit.DAYS.toSeconds(ttl);
                        tsService.saveAndNotifyInternal(tenantId, serviceAssetId, tsList, ttl, CALLBACK);
                    }
                }
            } catch (Exception e) {
                if (!"Asset is referencing to non-existent tenant!".equalsIgnoreCase(e.getMessage())) {
                    log.debug("[{}] Failed to store the statistics", id, e);
                }
            }
        });
        ruleEngineStats.getTenantExceptions().forEach((tenantId, e) -> {
            try {
                TsKvEntry tsKv = new BasicTsKvEntry(e.getTs(), new JsonDataEntry(RULE_ENGINE_EXCEPTION, e.toJsonString(maxErrorMessageLength)));
                long ttl = apiLimitService.getLimit(tenantId, DefaultTenantProfileConfiguration::getRuleEngineExceptionsTtlDays);
                ttl = TimeUnit.DAYS.toSeconds(ttl);
                tsService.saveAndNotifyInternal(tenantId, getServiceAssetId(tenantId, queueName), Collections.singletonList(tsKv), ttl, CALLBACK);
            } catch (Exception e2) {
                if (!"Asset is referencing to non-existent tenant!".equalsIgnoreCase(e2.getMessage())) {
                    log.debug("[{}] Failed to store the statistics", tenantId, e2);
                }
            }
        });
    }

    /**
     * 功能：获取资产ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `queueName`：队列名称或队列对象。
     * 返回：匹配的数据集合。
     */
    private AssetId getServiceAssetId(TenantId tenantId, String queueName) {
        TenantQueueKey key = new TenantQueueKey(tenantId, queueName);
        AssetId assetId = tenantQueueAssets.get(key);
        if (assetId == null) {
            lock.lock();
            try {
                assetId = tenantQueueAssets.get(key);
                if (assetId == null) {
                    Asset asset = assetService.findAssetByTenantIdAndName(tenantId, queueName + "_" + serviceInfoProvider.getServiceId());
                    if (asset == null) {
                        asset = new Asset();
                        asset.setTenantId(tenantId);
                        asset.setName(queueName + "_" + serviceInfoProvider.getServiceId());
                        asset.setType(TB_SERVICE_QUEUE);
                        asset = assetService.saveAsset(asset);
                    }
                    assetId = asset.getId();
                    tenantQueueAssets.put(key, assetId);
                }
            } finally {
                lock.unlock();
            }
        }
        return assetId;
    }

    /**
     * 中文说明：
     * 1. `TenantQueueKey` 是 ThingsBoard Application 中围绕租户提供具体能力的类型。
     * 2. 它封装当前声明对应的核心操作和必要状态。
     * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
     * 4. 它直接协作于构造参数、字段类型和公开方法涉及的对象。
     * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
     * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
     */
    @Data
    private static class TenantQueueKey {
        /**
         * 租户ID，用于定位对应业务对象。
         */
        private final TenantId tenantId;
        private final String queueName;
    }
}
