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
package org.thingsboard.server.service.device;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleEngineTelemetryService;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.device.ClaimDataInfo;
import org.thingsboard.server.dao.device.ClaimDevicesService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.device.claim.ClaimData;
import org.thingsboard.server.dao.device.claim.ClaimResponse;
import org.thingsboard.server.dao.device.claim.ClaimResult;
import org.thingsboard.server.dao.device.claim.ReclaimResult;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.queue.util.TbCoreComponent;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.thingsboard.server.common.data.CacheConstants.CLAIM_DEVICES_CACHE;

/**
 * 中文说明：
 * 1. `ClaimDevicesServiceImpl` 是 ThingsBoard Application 中负责 `Claim Devices` 的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `ClaimDevicesService`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Service
@Slf4j
@TbCoreComponent
public class ClaimDevicesServiceImpl implements ClaimDevicesService {

    /**
     * 属性常量，用于统一引用固定值。
     */
    private static final String CLAIM_ATTRIBUTE_NAME = "claimingAllowed";
    private static final String CLAIM_DATA_ATTRIBUTE_NAME = "claimingData";

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    private TbClusterService clusterService;
    /**
     * 设备，提供当前类调用的业务操作。
     */
    @Autowired
    private DeviceService deviceService;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    private AttributesService attributesService;
    /**
     * 遥测，提供当前类调用的业务操作。
     */
    @Autowired
    private RuleEngineTelemetryService telemetryService;
    /**
     * 客户，提供当前类调用的业务操作。
     */
    @Autowired
    private CustomerService customerService;
    /**
     * 管理器，负责处理对应任务或消息。
     */
    @Autowired
    private CacheManager cacheManager;

    /**
     * 当前对象是否为默认项。
     */
    @Value("${security.claim.allowClaimingByDefault}")
    private boolean isAllowedClaimingByDefault;

    /**
     * 持续时间，用于控制时间范围或等待时长。
     */
    @Value("${security.claim.duration}")
    private long systemDurationMs;

    /**
     * 功能：保存或创建信息对象。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * - `secretKey`：键。
     * - `durationMs`：`durationMs` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<Void> registerClaimingInfo(TenantId tenantId, DeviceId deviceId, String secretKey, long durationMs) {
        Device device = deviceService.findDeviceById(tenantId, deviceId);
        Cache cache = cacheManager.getCache(CLAIM_DEVICES_CACHE);
        List<Object> key = constructCacheKey(device.getId());
        if (isAllowedClaimingByDefault) {
            if (device.getCustomerId().getId().equals(ModelConstants.NULL_UUID)) {
                persistInCache(secretKey, durationMs, cache, key);
                return Futures.immediateFuture(null);
            }
            log.warn("The device [{}] has been already claimed!", device.getName());
            return Futures.immediateFailedFuture(new IllegalArgumentException());
        } else {
            ListenableFuture<List<AttributeKvEntry>> claimingAllowedFuture = attributesService.find(tenantId, device.getId(),
                    DataConstants.SERVER_SCOPE, Collections.singletonList(CLAIM_ATTRIBUTE_NAME));
            return Futures.transform(claimingAllowedFuture, list -> {
                if (list != null && !list.isEmpty()) {
                    Optional<Boolean> claimingAllowedOptional = list.get(0).getBooleanValue();
                    if (claimingAllowedOptional.isPresent() && claimingAllowedOptional.get()
                            && device.getCustomerId().getId().equals(ModelConstants.NULL_UUID)) {
                        persistInCache(secretKey, durationMs, cache, key);
                        return null;
                    }
                }
                log.warn("Failed to find claimingAllowed attribute for device or it is already claimed![{}]", device.getName());
                throw new IllegalArgumentException();
            }, MoreExecutors.directExecutor());
        }
    }

    /**
     * 功能：获取数据。
     * 参数：
     * - `cache`：`cache` 参数。
     * - `device`：设备信息或设备标识。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<ClaimDataInfo> getClaimData(Cache cache, Device device) {
        List<Object> key = constructCacheKey(device.getId());
        ClaimData claimDataFromCache = cache.get(key, ClaimData.class);
        if (claimDataFromCache != null) {
            return Futures.immediateFuture(new ClaimDataInfo(true, key, claimDataFromCache));
        } else {
            ListenableFuture<Optional<AttributeKvEntry>> claimDataAttrFuture = attributesService.find(device.getTenantId(), device.getId(),
                    DataConstants.SERVER_SCOPE, CLAIM_DATA_ATTRIBUTE_NAME);

            return Futures.transform(claimDataAttrFuture, claimDataAttr -> {
                if (claimDataAttr.isPresent()) {
                    ClaimData claimDataFromAttribute = JacksonUtil.fromString(claimDataAttr.get().getValueAsString(), ClaimData.class);
                    return new ClaimDataInfo(false, key, claimDataFromAttribute);
                }
                return null;
            }, MoreExecutors.directExecutor());
        }
    }

    /**
     * 功能：执行 `claimDevice` 对应的处理。
     * 参数：
     * - `device`：设备信息或设备标识。
     * - `customerId`：客户IDID。
     * - `secretKey`：键。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<ClaimResult> claimDevice(Device device, CustomerId customerId, String secretKey) {
        Cache cache = cacheManager.getCache(CLAIM_DEVICES_CACHE);
        ListenableFuture<ClaimDataInfo> claimDataFuture = getClaimData(cache, device);

        return Futures.transformAsync(claimDataFuture, claimData -> {
            if (claimData != null) {
                long currTs = System.currentTimeMillis();
                if (currTs > claimData.getData().getExpirationTime() || !secretKeyIsEmptyOrEqual(secretKey, claimData.getData().getSecretKey())) {
                    log.warn("The claiming timeout occurred or wrong 'secretKey' provided for the device [{}]", device.getName());
                    if (claimData.isFromCache()) {
                        cache.evict(claimData.getKey());
                    }
                    return Futures.immediateFuture(new ClaimResult(null, ClaimResponse.FAILURE));
                } else {
                    if (device.getCustomerId().getId().equals(ModelConstants.NULL_UUID)) {
                        device.setCustomerId(customerId);
                        Device savedDevice = deviceService.saveDevice(device);
                        clusterService.onDeviceUpdated(savedDevice, device);
                        return Futures.transform(removeClaimingSavedData(cache, claimData, device), result -> new ClaimResult(savedDevice, ClaimResponse.SUCCESS), MoreExecutors.directExecutor());
                    }
                    return Futures.transform(removeClaimingSavedData(cache, claimData, device), result -> new ClaimResult(null, ClaimResponse.CLAIMED), MoreExecutors.directExecutor());
                }
            } else {
                log.warn("Failed to find the device's claiming message![{}]", device.getName());
                if (device.getCustomerId().getId().equals(ModelConstants.NULL_UUID)) {
                    return Futures.immediateFuture(new ClaimResult(null, ClaimResponse.FAILURE));
                } else {
                    return Futures.immediateFuture(new ClaimResult(null, ClaimResponse.CLAIMED));
                }
            }
        }, MoreExecutors.directExecutor());
    }

    /**
     * 功能：执行 `secretKeyIsEmptyOrEqual` 对应的处理。
     * 参数：
     * - `secretKeyA`：键。
     * - `secretKeyB`：键。
     * 返回：判断结果。
     */
    private boolean secretKeyIsEmptyOrEqual(String secretKeyA, String secretKeyB) {
        return (StringUtils.isEmpty(secretKeyA) && StringUtils.isEmpty(secretKeyB)) || secretKeyA.equals(secretKeyB);
    }

    /**
     * 功能：执行 `reClaimDevice` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `device`：设备信息或设备标识。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<ReclaimResult> reClaimDevice(TenantId tenantId, Device device) {
        if (!device.getCustomerId().getId().equals(ModelConstants.NULL_UUID)) {
            cacheEviction(device.getId());
            Customer unassignedCustomer = customerService.findCustomerById(tenantId, device.getCustomerId());
            device.setCustomerId(null);
            Device savedDevice = deviceService.saveDevice(device);
            clusterService.onDeviceUpdated(savedDevice, device);
            if (isAllowedClaimingByDefault) {
                return Futures.immediateFuture(new ReclaimResult(unassignedCustomer));
            }
            SettableFuture<ReclaimResult> result = SettableFuture.create();
            telemetryService.saveAndNotify(
                    tenantId, savedDevice.getId(), DataConstants.SERVER_SCOPE, Collections.singletonList(
                            new BaseAttributeKvEntry(new BooleanDataEntry(CLAIM_ATTRIBUTE_NAME, true), System.currentTimeMillis())
                    ),
                    new FutureCallback<>() {
                        @Override
                        public void onSuccess(@Nullable Void tmp) {
                            result.set(new ReclaimResult(unassignedCustomer));
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            result.setException(t);
                        }
                    });
            return result;
        }
        cacheEviction(device.getId());
        return Futures.immediateFuture(new ReclaimResult(null));
    }

    /**
     * 功能：执行 `constructCacheKey` 对应的处理。
     * 参数：
     * - `deviceId`：设备IDID。
     * 返回：匹配的数据集合。
     */
    private List<Object> constructCacheKey(DeviceId deviceId) {
        return Collections.singletonList(deviceId);
    }

    /**
     * 功能：执行 `persistInCache` 对应的处理。
     * 参数：
     * - `secretKey`：键。
     * - `durationMs`：`durationMs` 参数。
     * - `cache`：`cache` 参数。
     * - `key`：键。
     * 返回：无。
     */
    private void persistInCache(String secretKey, long durationMs, Cache cache, List<Object> key) {
        ClaimData claimData = new ClaimData(secretKey,
                System.currentTimeMillis() + validateDurationMs(durationMs));
        cache.putIfAbsent(key, claimData);
    }

    /**
     * 功能：校验持续时间。
     * 参数：
     * - `durationMs`：`durationMs` 参数。
     * 返回：判断结果。
     */
    private long validateDurationMs(long durationMs) {
        if (durationMs > 0L) {
            return durationMs;
        }
        return systemDurationMs;
    }

    /**
     * 功能：删除或清理数据。
     * 参数：
     * - `cache`：`cache` 参数。
     * - `data`：待处理数据。
     * - `device`：设备信息或设备标识。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<Void> removeClaimingSavedData(Cache cache, ClaimDataInfo data, Device device) {
        if (data.isFromCache()) {
            cache.evict(data.getKey());
        }
        SettableFuture<Void> result = SettableFuture.create();
        telemetryService.deleteAndNotify(device.getTenantId(),
                device.getId(), DataConstants.SERVER_SCOPE, Arrays.asList(CLAIM_ATTRIBUTE_NAME, CLAIM_DATA_ATTRIBUTE_NAME), new FutureCallback<>() {
                    @Override
                    public void onSuccess(@Nullable Void tmp) {
                        result.set(tmp);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        result.setException(t);
                    }
                });
        return result;
    }

    /**
     * 功能：执行 `cacheEviction` 对应的处理。
     * 参数：
     * - `deviceId`：设备IDID。
     * 返回：无。
     */
    private void cacheEviction(DeviceId deviceId) {
        Cache cache = cacheManager.getCache(CLAIM_DEVICES_CACHE);
        cache.evict(constructCacheKey(deviceId));
    }

}
