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
package org.thingsboard.server.service.security;

import com.google.common.base.Function;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.ApiUsageStateId;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.RpcId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.rpc.Rpc;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.controller.HttpValidationCallback;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.asset.AssetProfileService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.ota.OtaPackageService;
import org.thingsboard.server.dao.resource.ResourceService;
import org.thingsboard.server.dao.rpc.RpcService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.usagerecord.ApiUsageStateService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.exception.ToErrorResponseEntity;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.AccessControlService;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

/**
 * Created by ashvayka on 27.03.18.
 */
/**
 * 中文说明：
 * 1. 类目的：`AccessValidator` 是ThingsBoard Application 模块中的安全认证服务类型，用于处理认证、授权、JWT、OAuth2、2FA 或会话安全流程。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring Security、User DAO、缓存、邮件服务、OAuth2 客户端和审计服务。
 * 4. 生命周期：由 Spring 创建为服务 Bean，随登录、刷新令牌和权限校验请求调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Strategy。
 */
@Component
public class AccessValidator {

    /**
     * Actor 系统常量，用于统一引用固定值。
     */
    public static final String ONLY_SYSTEM_ADMINISTRATOR_IS_ALLOWED_TO_PERFORM_THIS_OPERATION = "Only system administrator is allowed to perform this operation!";
    public static final String CUSTOMER_USER_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION = "Customer user is not allowed to perform this operation!";
    /**
     * Actor 系统常量，用于统一引用固定值。
     */
    public static final String SYSTEM_ADMINISTRATOR_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION = "System administrator is not allowed to perform this operation!";
    public static final String DEVICE_WITH_REQUESTED_ID_NOT_FOUND = "Device with requested id wasn't found!";
    /**
     * 边缘节点常量，用于统一引用固定值。
     */
    public static final String EDGE_WITH_REQUESTED_ID_NOT_FOUND = "Edge with requested id wasn't found!";
    public static final String ENTITY_VIEW_WITH_REQUESTED_ID_NOT_FOUND = "Entity-view with requested id wasn't found!";

    /**
     * 租户，提供当前类调用的业务操作。
     */
    @Autowired
    protected TenantService tenantService;

    /**
     * 客户，提供当前类调用的业务操作。
     */
    @Autowired
    protected CustomerService customerService;

    /**
     * 用户，提供当前类调用的业务操作。
     */
    @Autowired
    protected UserService userService;

    /**
     * 设备，提供当前类调用的业务操作。
     */
    @Autowired
    protected DeviceService deviceService;

    /**
     * 设备配置，提供当前类调用的业务操作。
     */
    @Autowired
    protected DeviceProfileService deviceProfileService;

    /**
     * 资产配置集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired
    protected AssetProfileService assetProfileService;

    /**
     * 资产集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired
    protected AssetService assetService;

    /**
     * 告警，提供当前类调用的业务操作。
     */
    @Autowired
    protected AlarmService alarmService;

    /**
     * 规则链，提供当前类调用的业务操作。
     */
    @Autowired
    protected RuleChainService ruleChainService;

    /**
     * 实体视图，提供当前类调用的业务操作。
     */
    @Autowired
    protected EntityViewService entityViewService;

    /**
     * 边缘节点，提供当前类调用的业务操作。
     */
    @Autowired(required = false)
    protected EdgeService edgeService;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    protected AccessControlService accessControlService;

    /**
     * 状态，提供当前类调用的业务操作。
     */
    @Autowired
    protected ApiUsageStateService apiUsageStateService;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    protected ResourceService resourceService;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    protected OtaPackageService otaPackageService;

    /**
     * RPC，提供当前类调用的业务操作。
     */
    @Autowired
    protected RpcService rpcService;

    /**
     * 执行器，负责处理对应任务或消息。
     */
    private ExecutorService executor;

    /**
     * 功能：初始化或启动执行器。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    public void initExecutor() {
        executor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("access-validator"));
    }

    /**
     * 功能：停止或关闭执行器。
     * 参数：无。
     * 返回：无。
     */
    @PreDestroy
    public void shutdownExecutor() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    /**
     * 功能：校验实体。
     * 参数：
     * - `currentUser`：`currentUser` 参数。
     * - `operation`：`operation` 参数。
     * - `entityType`：实体对象。
     * - `entityIdStr`：实体对象。
     * - 其余参数：补充处理条件。
     * 返回：判断结果。
     */
    public DeferredResult<ResponseEntity> validateEntityAndCallback(SecurityUser currentUser, Operation operation, String entityType, String entityIdStr,
                                                                    ThreeConsumer<DeferredResult<ResponseEntity>, TenantId, EntityId> onSuccess) throws ThingsboardException {
        return validateEntityAndCallback(currentUser, operation, entityType, entityIdStr, onSuccess, (result, t) -> handleError(t, result, HttpStatus.INTERNAL_SERVER_ERROR));
    }

    /**
     * 功能：校验实体。
     * 参数：
     * - `currentUser`：`currentUser` 参数。
     * - `operation`：`operation` 参数。
     * - `entityType`：实体对象。
     * - `entityIdStr`：实体对象。
     * - 其余参数：补充处理条件。
     * 返回：判断结果。
     */
    public DeferredResult<ResponseEntity> validateEntityAndCallback(SecurityUser currentUser, Operation operation, String entityType, String entityIdStr,
                                                                    ThreeConsumer<DeferredResult<ResponseEntity>, TenantId, EntityId> onSuccess,
                                                                    BiConsumer<DeferredResult<ResponseEntity>, Throwable> onFailure) throws ThingsboardException {
        return validateEntityAndCallback(currentUser, operation, EntityIdFactory.getByTypeAndId(entityType, entityIdStr),
                onSuccess, onFailure);
    }

    /**
     * 功能：校验实体。
     * 参数：
     * - `currentUser`：`currentUser` 参数。
     * - `operation`：`operation` 参数。
     * - `entityId`：实体IDID。
     * - `onSuccess`：`onSuccess` 参数。
     * 返回：判断结果。
     */
    public DeferredResult<ResponseEntity> validateEntityAndCallback(SecurityUser currentUser, Operation operation, EntityId entityId,
                                                                    ThreeConsumer<DeferredResult<ResponseEntity>, TenantId, EntityId> onSuccess) throws ThingsboardException {
        return validateEntityAndCallback(currentUser, operation, entityId, onSuccess, (result, t) -> handleError(t, result, HttpStatus.INTERNAL_SERVER_ERROR));
    }

    /**
     * 功能：校验实体。
     * 参数：
     * - `currentUser`：`currentUser` 参数。
     * - `operation`：`operation` 参数。
     * - `entityId`：实体IDID。
     * - `onSuccess`：`onSuccess` 参数。
     * - 其余参数：补充处理条件。
     * 返回：判断结果。
     */
    public DeferredResult<ResponseEntity> validateEntityAndCallback(SecurityUser currentUser, Operation operation, EntityId entityId,
                                                                    ThreeConsumer<DeferredResult<ResponseEntity>, TenantId, EntityId> onSuccess,
                                                                    BiConsumer<DeferredResult<ResponseEntity>, Throwable> onFailure) throws ThingsboardException {

        final DeferredResult<ResponseEntity> response = new DeferredResult<>();

        validate(currentUser, operation, entityId, new HttpValidationCallback(response,
                new FutureCallback<DeferredResult<ResponseEntity>>() {
                    @Override
                    public void onSuccess(@Nullable DeferredResult<ResponseEntity> result) {
                        try {
                            onSuccess.accept(response, currentUser.getTenantId(), entityId);
                        } catch (Exception e) {
                            onFailure(e);
                        }
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        onFailure.accept(response, t);
                    }
                }));

        return response;
    }

    /**
     * 功能：执行 `validate` 对应的处理。
     * 参数：
     * - `currentUser`：`currentUser` 参数。
     * - `operation`：`operation` 参数。
     * - `entityId`：实体IDID。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    public void validate(SecurityUser currentUser, Operation operation, EntityId entityId, FutureCallback<ValidationResult> callback) {
        switch (entityId.getEntityType()) {
            case DEVICE:
                validateDevice(currentUser, operation, entityId, callback);
                return;
            case DEVICE_PROFILE:
                validateDeviceProfile(currentUser, operation, entityId, callback);
                return;
            case ASSET:
                validateAsset(currentUser, operation, entityId, callback);
                return;
            case ASSET_PROFILE:
                validateAssetProfile(currentUser, operation, entityId, callback);
                return;
            case RULE_CHAIN:
                validateRuleChain(currentUser, operation, entityId, callback);
                return;
            case CUSTOMER:
                validateCustomer(currentUser, operation, entityId, callback);
                return;
            case TENANT:
                validateTenant(currentUser, operation, entityId, callback);
                return;
            case TENANT_PROFILE:
                validateTenantProfile(currentUser, operation, entityId, callback);
                return;
            case USER:
                validateUser(currentUser, operation, entityId, callback);
                return;
            case ENTITY_VIEW:
                validateEntityView(currentUser, operation, entityId, callback);
                return;
            case EDGE:
                validateEdge(currentUser, operation, entityId, callback);
                return;
            case API_USAGE_STATE:
                validateApiUsageState(currentUser, operation, entityId, callback);
                return;
            case TB_RESOURCE:
                validateResource(currentUser, operation, entityId, callback);
                return;
            case OTA_PACKAGE:
                validateOtaPackage(currentUser, operation, entityId, callback);
                return;
            case RPC:
                validateRpc(currentUser, operation, entityId, callback);
                return;
            default:
                //TODO: add support of other entities
                throw new IllegalStateException("Not Implemented!");
        }
    }

    /**
     * 功能：校验设备。
     * 参数：
     * - `currentUser`：`currentUser` 参数。
     * - `operation`：`operation` 参数。
     * - `entityId`：实体IDID。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    private void validateDevice(final SecurityUser currentUser, Operation operation, EntityId entityId, FutureCallback<ValidationResult> callback) {
        if (currentUser.isSystemAdmin()) {
            callback.onSuccess(ValidationResult.accessDenied(SYSTEM_ADMINISTRATOR_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
        } else {
            Futures.addCallback(Futures.immediateFuture(deviceService.findDeviceById(currentUser.getTenantId(), new DeviceId(entityId.getId()))), getCallback(callback, device -> {
                if (device == null) {
                    return ValidationResult.entityNotFound(DEVICE_WITH_REQUESTED_ID_NOT_FOUND);
                } else {
                    try {
                        accessControlService.checkPermission(currentUser, Resource.DEVICE, operation, entityId, device);
                    } catch (ThingsboardException e) {
                        return ValidationResult.accessDenied(e.getMessage());
                    }
                    return ValidationResult.ok(device);
                }
            }), executor);
        }
    }

    /**
     * 功能：校验RPC。
     * 参数：
     * - `currentUser`：`currentUser` 参数。
     * - `operation`：`operation` 参数。
     * - `entityId`：实体IDID。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    private void validateRpc(final SecurityUser currentUser, Operation operation, EntityId entityId, FutureCallback<ValidationResult> callback) {
        ListenableFuture<Rpc> rpcFurure = rpcService.findRpcByIdAsync(currentUser.getTenantId(), new RpcId(entityId.getId()));
        Futures.addCallback(rpcFurure, getCallback(callback, rpc -> {
            if (rpc == null) {
                return ValidationResult.entityNotFound("Rpc with requested id wasn't found!");
            } else {
                try {
                    accessControlService.checkPermission(currentUser, Resource.RPC, operation, entityId, rpc);
                } catch (ThingsboardException e) {
                    return ValidationResult.accessDenied(e.getMessage());
                }
                return ValidationResult.ok(rpc);
            }
        }), executor);
    }

    /**
     * 功能：校验设备配置。
     * 参数：
     * - `currentUser`：`currentUser` 参数。
     * - `operation`：`operation` 参数。
     * - `entityId`：实体IDID。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    private void validateDeviceProfile(final SecurityUser currentUser, Operation operation, EntityId entityId, FutureCallback<ValidationResult> callback) {
        if (currentUser.isSystemAdmin()) {
            callback.onSuccess(ValidationResult.accessDenied(SYSTEM_ADMINISTRATOR_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
        } else {
            DeviceProfile deviceProfile = deviceProfileService.findDeviceProfileById(currentUser.getTenantId(), new DeviceProfileId(entityId.getId()));
            if (deviceProfile == null) {
                callback.onSuccess(ValidationResult.entityNotFound("Device profile with requested id wasn't found!"));
            } else {
                try {
                    accessControlService.checkPermission(currentUser, Resource.DEVICE_PROFILE, operation, entityId, deviceProfile);
                } catch (ThingsboardException e) {
                    callback.onSuccess(ValidationResult.accessDenied(e.getMessage()));
                }
                callback.onSuccess(ValidationResult.ok(deviceProfile));
            }
        }
    }

    /**
     * 功能：校验资产配置。
     * 参数：
     * - `currentUser`：`currentUser` 参数。
     * - `operation`：`operation` 参数。
     * - `entityId`：实体IDID。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    private void validateAssetProfile(final SecurityUser currentUser, Operation operation, EntityId entityId, FutureCallback<ValidationResult> callback) {
        if (currentUser.isSystemAdmin()) {
            callback.onSuccess(ValidationResult.accessDenied(SYSTEM_ADMINISTRATOR_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
        } else {
            AssetProfile assetProfile = assetProfileService.findAssetProfileById(currentUser.getTenantId(), new AssetProfileId(entityId.getId()));
            if (assetProfile == null) {
                callback.onSuccess(ValidationResult.entityNotFound("Asset profile with requested id wasn't found!"));
            } else {
                try {
                    accessControlService.checkPermission(currentUser, Resource.ASSET_PROFILE, operation, entityId, assetProfile);
                } catch (ThingsboardException e) {
                    callback.onSuccess(ValidationResult.accessDenied(e.getMessage()));
                }
                callback.onSuccess(ValidationResult.ok(assetProfile));
            }
        }
    }

    /**
     * 功能：校验状态。
     * 参数：
     * - `currentUser`：`currentUser` 参数。
     * - `operation`：`operation` 参数。
     * - `entityId`：实体IDID。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    private void validateApiUsageState(final SecurityUser currentUser, Operation operation, EntityId entityId, FutureCallback<ValidationResult> callback) {
        if (currentUser.isSystemAdmin()) {
            callback.onSuccess(ValidationResult.accessDenied(SYSTEM_ADMINISTRATOR_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
        } else {
            if (!operation.equals(Operation.READ_TELEMETRY)) {
                callback.onSuccess(ValidationResult.accessDenied("Allowed only READ_TELEMETRY operation!"));
            }
            ApiUsageState apiUsageState = apiUsageStateService.findApiUsageStateById(currentUser.getTenantId(), new ApiUsageStateId(entityId.getId()));
            if (apiUsageState == null) {
                callback.onSuccess(ValidationResult.entityNotFound("Api Usage State with requested id wasn't found!"));
            } else {
                try {
                    accessControlService.checkPermission(currentUser, Resource.API_USAGE_STATE, operation, entityId, apiUsageState);
                } catch (ThingsboardException e) {
                    callback.onSuccess(ValidationResult.accessDenied(e.getMessage()));
                }
                callback.onSuccess(ValidationResult.ok(apiUsageState));
            }
        }
    }

    /**
     * 功能：校验`Ota Package`。
     * 参数：
     * - `currentUser`：`currentUser` 参数。
     * - `operation`：`operation` 参数。
     * - `entityId`：实体IDID。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    private void validateOtaPackage(final SecurityUser currentUser, Operation operation, EntityId entityId, FutureCallback<ValidationResult> callback) {
        if (currentUser.isSystemAdmin()) {
            callback.onSuccess(ValidationResult.accessDenied(SYSTEM_ADMINISTRATOR_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
        } else {
            OtaPackageInfo otaPackage = otaPackageService.findOtaPackageInfoById(currentUser.getTenantId(), new OtaPackageId(entityId.getId()));
            if (otaPackage == null) {
                callback.onSuccess(ValidationResult.entityNotFound("OtaPackage with requested id wasn't found!"));
            } else {
                try {
                    accessControlService.checkPermission(currentUser, Resource.OTA_PACKAGE, operation, entityId, otaPackage);
                } catch (ThingsboardException e) {
                    callback.onSuccess(ValidationResult.accessDenied(e.getMessage()));
                }
                callback.onSuccess(ValidationResult.ok(otaPackage));
            }
        }
    }

    /**
     * 功能：校验`Resource`。
     * 参数：
     * - `currentUser`：`currentUser` 参数。
     * - `operation`：`operation` 参数。
     * - `entityId`：实体IDID。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    private void validateResource(SecurityUser currentUser, Operation operation, EntityId entityId, FutureCallback<ValidationResult> callback) {
        ListenableFuture<TbResourceInfo> resourceFuture = resourceService.findResourceInfoByIdAsync(currentUser.getTenantId(), new TbResourceId(entityId.getId()));
        Futures.addCallback(resourceFuture, getCallback(callback, resource -> {
            if (resource == null) {
                return ValidationResult.entityNotFound("Resource with requested id wasn't found!");
            } else {
                try {
                    accessControlService.checkPermission(currentUser, Resource.TB_RESOURCE, operation, entityId, resource);
                } catch (ThingsboardException e) {
                    return ValidationResult.accessDenied(e.getMessage());
                }
                return ValidationResult.ok(resource);
            }
        }), executor);
    }

    /**
     * 功能：校验资产。
     * 参数：
     * - `currentUser`：`currentUser` 参数。
     * - `operation`：`operation` 参数。
     * - `entityId`：实体IDID。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    private void validateAsset(final SecurityUser currentUser, Operation operation, EntityId entityId, FutureCallback<ValidationResult> callback) {
        if (currentUser.isSystemAdmin()) {
            callback.onSuccess(ValidationResult.accessDenied(SYSTEM_ADMINISTRATOR_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
        } else {
            ListenableFuture<Asset> assetFuture = assetService.findAssetByIdAsync(currentUser.getTenantId(), new AssetId(entityId.getId()));
            Futures.addCallback(assetFuture, getCallback(callback, asset -> {
                if (asset == null) {
                    return ValidationResult.entityNotFound("Asset with requested id wasn't found!");
                } else {
                    try {
                        accessControlService.checkPermission(currentUser, Resource.ASSET, operation, entityId, asset);
                    } catch (ThingsboardException e) {
                        return ValidationResult.accessDenied(e.getMessage());
                    }
                    return ValidationResult.ok(asset);
                }
            }), executor);
        }
    }

    /**
     * 功能：校验规则链。
     * 参数：
     * - `currentUser`：`currentUser` 参数。
     * - `operation`：`operation` 参数。
     * - `entityId`：实体IDID。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    private void validateRuleChain(final SecurityUser currentUser, Operation operation, EntityId entityId, FutureCallback<ValidationResult> callback) {
        if (currentUser.isCustomerUser()) {
            callback.onSuccess(ValidationResult.accessDenied(CUSTOMER_USER_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
        } else {
            ListenableFuture<RuleChain> ruleChainFuture = ruleChainService.findRuleChainByIdAsync(currentUser.getTenantId(), new RuleChainId(entityId.getId()));
            Futures.addCallback(ruleChainFuture, getCallback(callback, ruleChain -> {
                if (ruleChain == null) {
                    return ValidationResult.entityNotFound("Rule chain with requested id wasn't found!");
                } else {
                    try {
                        accessControlService.checkPermission(currentUser, Resource.RULE_CHAIN, operation, entityId, ruleChain);
                    } catch (ThingsboardException e) {
                        return ValidationResult.accessDenied(e.getMessage());
                    }
                    return ValidationResult.ok(ruleChain);
                }
            }), executor);
        }
    }

    /**
     * 功能：校验`Rule`。
     * 参数：
     * - `currentUser`：`currentUser` 参数。
     * - `operation`：`operation` 参数。
     * - `entityId`：实体IDID。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    private void validateRule(final SecurityUser currentUser, Operation operation, EntityId entityId, FutureCallback<ValidationResult> callback) {
        if (currentUser.isCustomerUser()) {
            callback.onSuccess(ValidationResult.accessDenied(CUSTOMER_USER_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
        } else {
            ListenableFuture<RuleNode> ruleNodeFuture = ruleChainService.findRuleNodeByIdAsync(currentUser.getTenantId(), new RuleNodeId(entityId.getId()));
            Futures.addCallback(ruleNodeFuture, getCallback(callback, ruleNodeTmp -> {
                RuleNode ruleNode = ruleNodeTmp;
                if (ruleNode == null) {
                    return ValidationResult.entityNotFound("Rule node with requested id wasn't found!");
                } else if (ruleNode.getRuleChainId() == null) {
                    return ValidationResult.entityNotFound("Rule chain with requested node id wasn't found!");
                } else {
                    //TODO: make async
                    RuleChain ruleChain = ruleChainService.findRuleChainById(currentUser.getTenantId(), ruleNode.getRuleChainId());
                    try {
                        accessControlService.checkPermission(currentUser, Resource.RULE_CHAIN, operation, ruleNode.getRuleChainId(), ruleChain);
                    } catch (ThingsboardException e) {
                        return ValidationResult.accessDenied(e.getMessage());
                    }
                    return ValidationResult.ok(ruleNode);
                }
            }), executor);
        }
    }

    /**
     * 功能：校验客户。
     * 参数：
     * - `currentUser`：`currentUser` 参数。
     * - `operation`：`operation` 参数。
     * - `entityId`：实体IDID。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    private void validateCustomer(final SecurityUser currentUser, Operation operation, EntityId entityId, FutureCallback<ValidationResult> callback) {
        if (currentUser.isSystemAdmin()) {
            callback.onSuccess(ValidationResult.accessDenied(SYSTEM_ADMINISTRATOR_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
        } else {
            ListenableFuture<Customer> customerFuture = customerService.findCustomerByIdAsync(currentUser.getTenantId(), new CustomerId(entityId.getId()));
            Futures.addCallback(customerFuture, getCallback(callback, customer -> {
                if (customer == null) {
                    return ValidationResult.entityNotFound("Customer with requested id wasn't found!");
                } else {
                    try {
                        accessControlService.checkPermission(currentUser, Resource.CUSTOMER, operation, entityId, customer);
                    } catch (ThingsboardException e) {
                        return ValidationResult.accessDenied(e.getMessage());
                    }
                    return ValidationResult.ok(customer);
                }
            }), executor);
        }
    }

    /**
     * 功能：校验租户。
     * 参数：
     * - `currentUser`：`currentUser` 参数。
     * - `operation`：`operation` 参数。
     * - `entityId`：实体IDID。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    private void validateTenant(final SecurityUser currentUser, Operation operation, EntityId entityId, FutureCallback<ValidationResult> callback) {
        if (currentUser.isCustomerUser()) {
            callback.onSuccess(ValidationResult.accessDenied(CUSTOMER_USER_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
        } else if (currentUser.isSystemAdmin()) {
            callback.onSuccess(ValidationResult.ok(null));
        } else {
            ListenableFuture<Tenant> tenantFuture = tenantService.findTenantByIdAsync(currentUser.getTenantId(), TenantId.fromUUID(entityId.getId()));
            Futures.addCallback(tenantFuture, getCallback(callback, tenant -> {
                if (tenant == null) {
                    return ValidationResult.entityNotFound("Tenant with requested id wasn't found!");
                }
                try {
                    accessControlService.checkPermission(currentUser, Resource.TENANT, operation, entityId, tenant);
                } catch (ThingsboardException e) {
                    return ValidationResult.accessDenied(e.getMessage());
                }
                return ValidationResult.ok(tenant);

            }), executor);
        }
    }

    /**
     * 功能：校验租户。
     * 参数：
     * - `currentUser`：`currentUser` 参数。
     * - `operation`：`operation` 参数。
     * - `entityId`：实体IDID。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    private void validateTenantProfile(final SecurityUser currentUser, Operation operation, EntityId entityId, FutureCallback<ValidationResult> callback) {
        if (currentUser.isSystemAdmin()) {
            callback.onSuccess(ValidationResult.ok(null));
        } else {
            callback.onSuccess(ValidationResult.accessDenied(ONLY_SYSTEM_ADMINISTRATOR_IS_ALLOWED_TO_PERFORM_THIS_OPERATION));
        }
    }

    /**
     * 功能：校验用户。
     * 参数：
     * - `currentUser`：`currentUser` 参数。
     * - `operation`：`operation` 参数。
     * - `entityId`：实体IDID。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    private void validateUser(final SecurityUser currentUser, Operation operation, EntityId entityId, FutureCallback<ValidationResult> callback) {
        ListenableFuture<User> userFuture = userService.findUserByIdAsync(currentUser.getTenantId(), new UserId(entityId.getId()));
        Futures.addCallback(userFuture, getCallback(callback, user -> {
            if (user == null) {
                return ValidationResult.entityNotFound("User with requested id wasn't found!");
            }
            try {
                accessControlService.checkPermission(currentUser, Resource.USER, operation, entityId, user);
            } catch (ThingsboardException e) {
                return ValidationResult.accessDenied(e.getMessage());
            }
            return ValidationResult.ok(user);

        }), executor);
    }

    /**
     * 功能：校验实体视图。
     * 参数：
     * - `currentUser`：`currentUser` 参数。
     * - `operation`：`operation` 参数。
     * - `entityId`：实体IDID。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    private void validateEntityView(final SecurityUser currentUser, Operation operation, EntityId entityId, FutureCallback<ValidationResult> callback) {
        if (currentUser.isSystemAdmin()) {
            callback.onSuccess(ValidationResult.accessDenied(SYSTEM_ADMINISTRATOR_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
        } else {
            ListenableFuture<EntityView> entityViewFuture = entityViewService.findEntityViewByIdAsync(currentUser.getTenantId(), new EntityViewId(entityId.getId()));
            Futures.addCallback(entityViewFuture, getCallback(callback, entityView -> {
                if (entityView == null) {
                    return ValidationResult.entityNotFound(ENTITY_VIEW_WITH_REQUESTED_ID_NOT_FOUND);
                } else {
                    try {
                        accessControlService.checkPermission(currentUser, Resource.ENTITY_VIEW, operation, entityId, entityView);
                    } catch (ThingsboardException e) {
                        return ValidationResult.accessDenied(e.getMessage());
                    }
                    return ValidationResult.ok(entityView);
                }
            }), executor);
        }
    }

    /**
     * 功能：校验边缘节点。
     * 参数：
     * - `currentUser`：`currentUser` 参数。
     * - `operation`：`operation` 参数。
     * - `entityId`：实体IDID。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    private void validateEdge(final SecurityUser currentUser, Operation operation, EntityId entityId, FutureCallback<ValidationResult> callback) {
        if (currentUser.isSystemAdmin()) {
            callback.onSuccess(ValidationResult.accessDenied(SYSTEM_ADMINISTRATOR_IS_NOT_ALLOWED_TO_PERFORM_THIS_OPERATION));
        } else {
            ListenableFuture<Edge> edgeFuture = edgeService.findEdgeByIdAsync(currentUser.getTenantId(), new EdgeId(entityId.getId()));
            Futures.addCallback(edgeFuture, getCallback(callback, edge -> {
                if (edge == null) {
                    return ValidationResult.entityNotFound(EDGE_WITH_REQUESTED_ID_NOT_FOUND);
                } else {
                    try {
                        accessControlService.checkPermission(currentUser, Resource.EDGE, operation, entityId, edge);
                    } catch (ThingsboardException e) {
                        return ValidationResult.accessDenied(e.getMessage());
                    }
                    return ValidationResult.ok(edge);
                }
            }), executor);
        }
    }

    /**
     * 功能：获取回调。
     * 参数：
     * - `callback`：处理完成后的回调。
     * - `transformer`：`transformer` 参数。
     * 返回：异步处理结果。
     */
    private <T, V> FutureCallback<T> getCallback(FutureCallback<ValidationResult> callback, Function<T, ValidationResult<V>> transformer) {
        return new FutureCallback<T>() {
            @Override
            public void onSuccess(@Nullable T result) {
                callback.onSuccess(transformer.apply(result));
            }

            @Override
            public void onFailure(Throwable t) {
                callback.onFailure(t);
            }
        };
    }

    /**
     * 功能：处理错误信息。
     * 参数：
     * - `e`：`e` 参数。
     * - `response`：响应对象。
     * - `defaultErrorStatus`：错误信息。
     * 返回：无。
     */
    public static void handleError(Throwable e, final DeferredResult<ResponseEntity> response, HttpStatus defaultErrorStatus) {
        ResponseEntity responseEntity;
        if (e instanceof ToErrorResponseEntity) {
            responseEntity = ((ToErrorResponseEntity) e).toErrorResponseEntity();
        } else if (e instanceof IllegalArgumentException || e instanceof IncorrectParameterException || e instanceof DataValidationException) {
            responseEntity = new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } else {
            responseEntity = new ResponseEntity<>(defaultErrorStatus);
        }
        response.setResult(responseEntity);
    }

    /**
     * 中文说明：
     * 1. 类目的：`ThreeConsumer` 是ThingsBoard Application 模块中的安全认证服务类型，用于处理认证、授权、JWT、OAuth2、2FA 或会话安全流程。
     * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
     * 3. 协作对象：主要协作对象包括Spring Security、User DAO、缓存、邮件服务、OAuth2 客户端和审计服务。
     * 4. 生命周期：由 Spring 创建为服务 Bean，随登录、刷新令牌和权限校验请求调用。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 Service / Strategy。
     */
    public interface ThreeConsumer<A, B, C> {
        /**
         * 功能：执行 `accept` 对应的处理。
         * 参数：
         * - `a`：`a` 参数。
         * - `b`：`b` 参数。
         * - `c`：`c` 参数。
         * 返回：无。
         */
        void accept(A a, B b, C c);
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`AccessValidator` 在 ThingsBoard Application 模块 中承担安全认证服务类型职责，核心目的是处理认证、授权、JWT、OAuth2、2FA 或会话安全流程。
 * 2. 核心流程：读取安全上下文和凭据，校验权限后返回认证结果或安全响应。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Security、User DAO、缓存、邮件服务、OAuth2 客户端和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
