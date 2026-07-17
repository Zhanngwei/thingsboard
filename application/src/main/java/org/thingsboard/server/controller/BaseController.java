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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.Getter;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceInfo;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.EntityViewInfo;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantInfo;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetInfo;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeInfo;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.AlarmCommentId;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.QueueId;
import org.thingsboard.server.common.data.id.RpcId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.id.WidgetsBundleId;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.plugin.ComponentDescriptor;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.data.rpc.Rpc;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.rule.RuleNode;
import org.thingsboard.server.common.data.util.ThrowingBiFunction;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.alarm.AlarmCommentService;
import org.thingsboard.server.dao.asset.AssetProfileService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.audit.AuditLogService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.ClaimDevicesService;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.oauth2.OAuth2ConfigTemplateService;
import org.thingsboard.server.dao.oauth2.OAuth2Service;
import org.thingsboard.server.dao.ota.OtaPackageService;
import org.thingsboard.server.dao.queue.QueueService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.resource.ResourceService;
import org.thingsboard.server.dao.rpc.RpcService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.service.ConstraintValidator;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.dao.tenant.TenantProfileService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.widget.WidgetTypeService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;
import org.thingsboard.server.exception.ThingsboardErrorResponseHandler;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.action.EntityActionService;
import org.thingsboard.server.service.component.ComponentDiscoveryService;
import org.thingsboard.server.service.entitiy.TbNotificationEntityService;
import org.thingsboard.server.service.entitiy.user.TbUserSettingsService;
import org.thingsboard.server.service.ota.OtaPackageStateService;
import org.thingsboard.server.service.profile.TbAssetProfileCache;
import org.thingsboard.server.service.profile.TbDeviceProfileCache;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.AccessControlService;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;
import org.thingsboard.server.service.state.DeviceStateService;
import org.thingsboard.server.service.sync.ie.exporting.ExportableEntitiesService;
import org.thingsboard.server.service.sync.vc.EntitiesVersionControlService;
import org.thingsboard.server.service.telemetry.AlarmSubscriptionService;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolation;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.StringUtils.isNotEmpty;
import static org.thingsboard.server.common.data.query.EntityKeyType.ENTITY_FIELD;
import static org.thingsboard.server.controller.UserController.YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION;
import static org.thingsboard.server.dao.service.Validator.validateId;

/**
 * 中文说明：
 * 1. `BaseController` 是 ThingsBoard Application 中处理 `Base Controller` 请求的 API 控制器。
 * 2. 它负责校验请求参数、解析当前用户上下文并调用对应服务完成操作。
 * 3. 方法返回面向客户端的数据对象或统一的异步响应。
 * 4. 它直接协作于业务服务、权限校验组件和请求响应模型。
 * 5. 单独设置控制器可以把 HTTP 边界与业务实现分开，保持接口行为稳定。
 * 6. 阅读时重点关注路由、权限条件、参数校验以及服务调用结果的转换。
 */
@TbCoreComponent
public abstract class BaseController {

    private final Logger log = org.slf4j.LoggerFactory.getLogger(getClass());

    /*Swagger UI description*/

    /**
     * 响应，负责处理对应任务或消息。
     */
    @Autowired
    private ThingsboardErrorResponseHandler errorResponseHandler;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    protected AccessControlService accessControlService;

    /**
     * 租户，提供当前类调用的业务操作。
     */
    @Autowired
    protected TenantService tenantService;

    /**
     * 租户，提供当前类调用的业务操作。
     */
    @Autowired
    protected TenantProfileService tenantProfileService;

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
     * 用户集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired
    protected TbUserSettingsService userSettingsService;

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
     * 资产集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired
    protected AssetService assetService;

    /**
     * 资产配置集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired
    protected AssetProfileService assetProfileService;

    /**
     * 告警，提供当前类调用的业务操作。
     */
    @Autowired
    protected AlarmSubscriptionService alarmService;

    /**
     * 告警，提供当前类调用的业务操作。
     */
    @Autowired
    protected AlarmCommentService alarmCommentService;

    /**
     * 设备凭据，提供当前类调用的业务操作。
     */
    @Autowired
    protected DeviceCredentialsService deviceCredentialsService;

    /**
     * 部件包，提供当前类调用的业务操作。
     */
    @Autowired
    protected WidgetsBundleService widgetsBundleService;

    /**
     * 部件类型，提供当前类调用的业务操作。
     */
    @Autowired
    protected WidgetTypeService widgetTypeService;

    /**
     * 仪表盘，提供当前类调用的业务操作。
     */
    @Autowired
    protected DashboardService dashboardService;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    protected OAuth2Service oAuth2Service;

    /**
     * 配置，提供当前类调用的业务操作。
     */
    @Autowired
    protected OAuth2ConfigTemplateService oAuth2ConfigTemplateService;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    protected ComponentDiscoveryService componentDescriptorService;

    /**
     * 规则链，提供当前类调用的业务操作。
     */
    @Autowired
    protected RuleChainService ruleChainService;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    protected TbClusterService tbClusterService;

    /**
     * 关系，提供当前类调用的业务操作。
     */
    @Autowired
    protected RelationService relationService;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    protected AuditLogService auditLogService;

    /**
     * 设备，提供当前类调用的业务操作。
     */
    @Autowired
    protected DeviceStateService deviceStateService;

    /**
     * 实体视图，提供当前类调用的业务操作。
     */
    @Autowired
    protected EntityViewService entityViewService;

    /**
     * 时间戳，提供当前类调用的业务操作。
     */
    @Autowired
    protected TelemetrySubscriptionService tsSubService;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    protected AttributesService attributesService;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    protected ClaimDevicesService claimDevicesService;

    /**
     * 分区，提供当前类调用的业务操作。
     */
    @Autowired
    protected PartitionService partitionService;

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
     * 状态，提供当前类调用的业务操作。
     */
    @Autowired
    protected OtaPackageStateService otaPackageStateService;

    /**
     * RPC，提供当前类调用的业务操作。
     */
    @Autowired
    protected RpcService rpcService;

    /**
     * 提供者，用于按场景创建或提供目标对象。
     */
    @Autowired
    protected TbQueueProducerProvider producerProvider;

    /**
     * 租户对象，用于描述当前业务场景。
     */
    @Autowired
    protected TbTenantProfileCache tenantProfileCache;

    /**
     * 设备配置，保存当前对象的配置选项。
     */
    @Autowired
    protected TbDeviceProfileCache deviceProfileCache;

    /**
     * 资产配置集合，用于去重保存或快速判断对象是否存在。
     */
    @Autowired
    protected TbAssetProfileCache assetProfileCache;

    /**
     * 边缘节点，提供当前类调用的业务操作。
     */
    @Autowired(required = false)
    protected EdgeService edgeService;

    /**
     * 通知服务，提供当前类调用的业务操作。
     */
    @Autowired
    protected TbNotificationEntityService notificationEntityService;

    /**
     * 实体，提供当前类调用的业务操作。
     */
    @Autowired
    protected EntityActionService entityActionService;

    /**
     * 队列，提供当前类调用的业务操作。
     */
    @Autowired
    protected QueueService queueService;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    protected EntitiesVersionControlService vcService;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    protected ExportableEntitiesService entitiesService;

    /**
     * 是否满足错误信息条件。
     */
    @Value("${server.log_controller_error_stack_trace}")
    @Getter
    private boolean logControllerErrorStackTrace;

    /**
     * 是否启用`edges`。
     */
    @Value("${edges.enabled}")
    @Getter
    protected boolean edgesEnabled;

    /**
     * 功能：处理`Controller Exception`。
     * 参数：
     * - `e`：`e` 参数。
     * - `response`：响应对象。
     * 返回：无。
     */
    @ExceptionHandler(Exception.class)
    public void handleControllerException(Exception e, HttpServletResponse response) {
        ThingsboardException thingsboardException = handleException(e);
        if (thingsboardException.getErrorCode() == ThingsboardErrorCode.GENERAL && thingsboardException.getCause() instanceof Exception
                && StringUtils.equals(thingsboardException.getCause().getMessage(), thingsboardException.getMessage())) {
            e = (Exception) thingsboardException.getCause();
        } else {
            e = thingsboardException;
        }
        errorResponseHandler.handle(e, response);
    }

    /**
     * 功能：处理`Thingsboard Exception`。
     * 参数：
     * - `ex`：`ex` 参数。
     * - `response`：响应对象。
     * 返回：无。
     */
    @ExceptionHandler(ThingsboardException.class)
    public void handleThingsboardException(ThingsboardException ex, HttpServletResponse response) {
        errorResponseHandler.handle(ex, response);
    }

    /**
     * @deprecated Exceptions that are not of {@link ThingsboardException} type
     * are now caught and mapped to {@link ThingsboardException} by
     * {@link ExceptionHandler} {@link BaseController#handleControllerException(Exception, HttpServletResponse)}
     * which basically acts like the following boilerplate:
     * {@code
     *  try {
     *      someExceptionThrowingMethod();
     *  } catch (Exception e) {
     *      throw handleException(e);
     *  }
     * }
     * */
    /**
     * 功能：处理`Exception`。
     * 参数：
     * - `exception`：`exception` 参数。
     * 返回：处理结果。
     */
    @Deprecated
    ThingsboardException handleException(Exception exception) {
        return handleException(exception, true);
    }

    /**
     * 功能：处理`Exception`。
     * 参数：
     * - `exception`：`exception` 参数。
     * - `logException`：`logException` 参数。
     * 返回：处理结果。
     */
    private ThingsboardException handleException(Exception exception, boolean logException) {
        if (logException && logControllerErrorStackTrace) {
            log.error("Error [{}]", exception.getMessage(), exception);
        }

        String cause = "";
        if (exception.getCause() != null) {
            cause = exception.getCause().getClass().getCanonicalName();
        }

        if (exception instanceof ThingsboardException) {
            return (ThingsboardException) exception;
        } else if (exception instanceof IllegalArgumentException || exception instanceof IncorrectParameterException
                || exception instanceof DataValidationException || cause.contains("IncorrectParameterException")) {
            return new ThingsboardException(exception.getMessage(), ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        } else if (exception instanceof MessagingException) {
            return new ThingsboardException("Unable to send mail: " + exception.getMessage(), ThingsboardErrorCode.GENERAL);
        } else if (exception instanceof AsyncRequestTimeoutException) {
            return new ThingsboardException("Request timeout", ThingsboardErrorCode.GENERAL);
        } else if (exception instanceof DataAccessException) {
            String errorType = exception.getClass().getSimpleName();
            if (!logControllerErrorStackTrace) { // not to log the error twice
                log.warn("Database error: {} - {}", errorType, ExceptionUtils.getRootCauseMessage(exception));
            }
            return new ThingsboardException("Database error", ThingsboardErrorCode.GENERAL);
        }
        return new ThingsboardException(exception.getMessage(), exception, ThingsboardErrorCode.GENERAL);
    }

    /**
     * Handles validation error for controller method arguments annotated with @{@link javax.validation.Valid}
     * */
    /**
     * 功能：处理错误信息。
     * 参数：
     * - `validationError`：错误信息。
     * - `response`：响应对象。
     * 返回：无。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public void handleValidationError(MethodArgumentNotValidException validationError, HttpServletResponse response) {
        List<ConstraintViolation<Object>> constraintsViolations = validationError.getFieldErrors().stream()
                .map(fieldError -> {
                    try {
                        return (ConstraintViolation<Object>) fieldError.unwrap(ConstraintViolation.class);
                    } catch (Exception e) {
                        log.warn("FieldError source is not of type ConstraintViolation");
                        return null; // should not happen
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        String errorMessage = "Validation error: " + ConstraintValidator.getErrorMessage(constraintsViolations);
        ThingsboardException thingsboardException = new ThingsboardException(errorMessage, ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        handleControllerException(thingsboardException, response);
    }

    /**
     * 功能：校验`Not Null`。
     * 参数：
     * - `reference`：`reference` 参数。
     * 返回：判断结果。
     */
    <T> T checkNotNull(T reference) throws ThingsboardException {
        return checkNotNull(reference, "Requested item wasn't found!");
    }

    /**
     * 功能：校验`Not Null`。
     * 参数：
     * - `reference`：`reference` 参数。
     * - `notFoundMessage`：待处理消息。
     * 返回：判断结果。
     */
    <T> T checkNotNull(T reference, String notFoundMessage) throws ThingsboardException {
        if (reference == null) {
            throw new ThingsboardException(notFoundMessage, ThingsboardErrorCode.ITEM_NOT_FOUND);
        }
        return reference;
    }

    /**
     * 功能：校验`Not Null`。
     * 参数：
     * - `reference`：`reference` 参数。
     * 返回：判断结果。
     */
    <T> T checkNotNull(Optional<T> reference) throws ThingsboardException {
        return checkNotNull(reference, "Requested item wasn't found!");
    }

    /**
     * 功能：校验`Not Null`。
     * 参数：
     * - `reference`：`reference` 参数。
     * - `notFoundMessage`：待处理消息。
     * 返回：判断结果。
     */
    <T> T checkNotNull(Optional<T> reference, String notFoundMessage) throws ThingsboardException {
        if (reference.isPresent()) {
            return reference.get();
        } else {
            throw new ThingsboardException(notFoundMessage, ThingsboardErrorCode.ITEM_NOT_FOUND);
        }
    }

    /**
     * 功能：校验`Parameter`。
     * 参数：
     * - `name`：名称。
     * - `param`：`param` 参数。
     * 返回：无。
     */
    void checkParameter(String name, String param) throws ThingsboardException {
        if (StringUtils.isEmpty(param)) {
            throw new ThingsboardException("Parameter '" + name + "' can't be empty!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
    }

    /**
     * 功能：校验`Array Parameter`。
     * 参数：
     * - `name`：名称。
     * - `params`：`params` 参数。
     * 返回：无。
     */
    void checkArrayParameter(String name, String[] params) throws ThingsboardException {
        if (params == null || params.length == 0) {
            throw new ThingsboardException("Parameter '" + name + "' can't be empty!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        } else {
            for (String param : params) {
                checkParameter(name, param);
            }
        }
    }

    /**
     * 功能：校验`Enum Parameter`。
     * 参数：
     * - `name`：名称。
     * - `param`：`param` 参数。
     * - `valueOf`：值。
     * 返回：判断结果。
     */
    protected <T> T checkEnumParameter(String name, String param, Function<String, T> valueOf) throws ThingsboardException {
        try {
            return valueOf.apply(param.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ThingsboardException(name + " \"" + param + "\" is not supported!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
    }

    /**
     * 功能：执行 `toUUID` 对应的处理。
     * 参数：
     * - `id`：`id`ID。
     * 返回：处理结果。
     */
    UUID toUUID(String id) throws ThingsboardException {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw handleException(e, false);
        }
    }

    /**
     * 功能：保存或创建分页查询条件。
     * 参数：
     * - `pageSize`：`pageSize` 参数。
     * - `page`：`page` 参数。
     * - `textSearch`：`textSearch` 参数。
     * - `sortProperty`：`sortProperty` 参数。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    PageLink createPageLink(int pageSize, int page, String textSearch, String sortProperty, String sortOrder) throws ThingsboardException {
        if (StringUtils.isNotEmpty(sortProperty)) {
            if (!Validator.isValidProperty(sortProperty)) {
                throw new IllegalArgumentException("Invalid sort property");
            }
            SortOrder.Direction direction = SortOrder.Direction.ASC;
            if (StringUtils.isNotEmpty(sortOrder)) {
                try {
                    direction = SortOrder.Direction.valueOf(sortOrder.toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new ThingsboardException("Unsupported sort order '" + sortOrder + "'! Only 'ASC' or 'DESC' types are allowed.", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
                }
            }
            SortOrder sort = new SortOrder(sortProperty, direction);
            return new PageLink(pageSize, page, textSearch, sort);
        } else {
            return new PageLink(pageSize, page, textSearch);
        }
    }

    /**
     * 功能：保存或创建分页查询条件。
     * 参数：
     * - `pageSize`：`pageSize` 参数。
     * - `page`：`page` 参数。
     * - `textSearch`：`textSearch` 参数。
     * - `sortProperty`：`sortProperty` 参数。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    TimePageLink createTimePageLink(int pageSize, int page, String textSearch,
                                    String sortProperty, String sortOrder, Long startTime, Long endTime) throws ThingsboardException {
        PageLink pageLink = this.createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return new TimePageLink(pageLink, startTime, endTime);
    }

    /**
     * 功能：获取用户。
     * 参数：无。
     * 返回：处理结果。
     */
    protected SecurityUser getCurrentUser() throws ThingsboardException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof SecurityUser) {
            return (SecurityUser) authentication.getPrincipal();
        } else {
            throw new ThingsboardException("You aren't authorized to perform this operation!", ThingsboardErrorCode.AUTHENTICATION);
        }
    }

    /**
     * 功能：校验租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `operation`：`operation` 参数。
     * 返回：判断结果。
     */
    Tenant checkTenantId(TenantId tenantId, Operation operation) throws ThingsboardException {
        return checkEntityId(tenantId, (t, i) -> tenantService.findTenantById(tenantId), operation);
    }

    /**
     * 功能：校验租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `operation`：`operation` 参数。
     * 返回：判断结果。
     */
    TenantInfo checkTenantInfoId(TenantId tenantId, Operation operation) throws ThingsboardException {
        return checkEntityId(tenantId, (t, i) -> tenantService.findTenantInfoById(tenantId), operation);
    }

    /**
     * 功能：校验租户ID。
     * 参数：
     * - `tenantProfileId`：租户IDID。
     * - `operation`：`operation` 参数。
     * 返回：判断结果。
     */
    TenantProfile checkTenantProfileId(TenantProfileId tenantProfileId, Operation operation) throws ThingsboardException {
        try {
            validateId(tenantProfileId, "Incorrect tenantProfileId " + tenantProfileId);
            TenantProfile tenantProfile = tenantProfileService.findTenantProfileById(getTenantId(), tenantProfileId);
            checkNotNull(tenantProfile, "Tenant profile with id [" + tenantProfileId + "] is not found");
            accessControlService.checkPermission(getCurrentUser(), Resource.TENANT_PROFILE, operation);
            return tenantProfile;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    /**
     * 功能：获取租户ID。
     * 参数：无。
     * 返回：处理结果。
     */
    protected TenantId getTenantId() throws ThingsboardException {
        return getCurrentUser().getTenantId();
    }

    /**
     * 功能：校验客户ID。
     * 参数：
     * - `customerId`：客户IDID。
     * - `operation`：`operation` 参数。
     * 返回：判断结果。
     */
    Customer checkCustomerId(CustomerId customerId, Operation operation) throws ThingsboardException {
        return checkEntityId(customerId, customerService::findCustomerById, operation);
    }

    /**
     * 功能：校验用户。
     * 参数：
     * - `userId`：用户ID。
     * - `operation`：`operation` 参数。
     * 返回：判断结果。
     */
    User checkUserId(UserId userId, Operation operation) throws ThingsboardException {
        return checkEntityId(userId, userService::findUserById, operation);
    }

    /**
     * 功能：校验实体。
     * 参数：
     * - `entityId`：实体IDID。
     * - `entity`：实体对象。
     * - `resource`：`resource` 参数。
     * 返回：无。
     */
    protected <I extends EntityId, T extends HasTenantId> void checkEntity(I entityId, T entity, Resource resource) throws ThingsboardException {
        if (entityId == null) {
            accessControlService.checkPermission(getCurrentUser(), resource, Operation.CREATE, null, entity);
        } else {
            checkEntityId(entityId, Operation.WRITE);
        }
    }

    /**
     * 功能：校验实体ID。
     * 参数：
     * - `entityId`：实体IDID。
     * - `operation`：`operation` 参数。
     * 返回：无。
     */
    protected void checkEntityId(EntityId entityId, Operation operation) throws ThingsboardException {
        try {
            if (entityId == null) {
                throw new ThingsboardException("Parameter entityId can't be empty!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            }
            validateId(entityId.getId(), "Incorrect entityId " + entityId);
            switch (entityId.getEntityType()) {
                case ALARM:
                    checkAlarmId(new AlarmId(entityId.getId()), operation);
                    return;
                case DEVICE:
                    checkDeviceId(new DeviceId(entityId.getId()), operation);
                    return;
                case DEVICE_PROFILE:
                    checkDeviceProfileId(new DeviceProfileId(entityId.getId()), operation);
                    return;
                case CUSTOMER:
                    checkCustomerId(new CustomerId(entityId.getId()), operation);
                    return;
                case TENANT:
                    checkTenantId(TenantId.fromUUID(entityId.getId()), operation);
                    return;
                case TENANT_PROFILE:
                    checkTenantProfileId(new TenantProfileId(entityId.getId()), operation);
                    return;
                case RULE_CHAIN:
                    checkRuleChain(new RuleChainId(entityId.getId()), operation);
                    return;
                case RULE_NODE:
                    checkRuleNode(new RuleNodeId(entityId.getId()), operation);
                    return;
                case ASSET:
                    checkAssetId(new AssetId(entityId.getId()), operation);
                    return;
                case ASSET_PROFILE:
                    checkAssetProfileId(new AssetProfileId(entityId.getId()), operation);
                    return;
                case DASHBOARD:
                    checkDashboardId(new DashboardId(entityId.getId()), operation);
                    return;
                case USER:
                    checkUserId(new UserId(entityId.getId()), operation);
                    return;
                case ENTITY_VIEW:
                    checkEntityViewId(new EntityViewId(entityId.getId()), operation);
                    return;
                case EDGE:
                    checkEdgeId(new EdgeId(entityId.getId()), operation);
                    return;
                case WIDGETS_BUNDLE:
                    checkWidgetsBundleId(new WidgetsBundleId(entityId.getId()), operation);
                    return;
                case WIDGET_TYPE:
                    checkWidgetTypeId(new WidgetTypeId(entityId.getId()), operation);
                    return;
                case TB_RESOURCE:
                    checkResourceInfoId(new TbResourceId(entityId.getId()), operation);
                    return;
                case OTA_PACKAGE:
                    checkOtaPackageId(new OtaPackageId(entityId.getId()), operation);
                    return;
                case QUEUE:
                    checkQueueId(new QueueId(entityId.getId()), operation);
                    return;
                default:
                    checkEntityId(entityId, entitiesService::findEntityByTenantIdAndId, operation);
            }
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    protected <E extends HasId<I> & HasTenantId, I extends EntityId> E checkEntityId(I entityId, ThrowingBiFunction<TenantId, I, E> findingFunction, Operation operation) throws ThingsboardException {
        try {
            validateId((UUIDBased) entityId, "Invalid entity id");
            SecurityUser user = getCurrentUser();
            E entity = findingFunction.apply(user.getTenantId(), entityId);
            checkNotNull(entity, entityId.getEntityType().getNormalName() + " with id [" + entityId + "] is not found");
            return checkEntity(user, entity, operation);
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    protected <E extends HasId<I> & HasTenantId, I extends EntityId> E checkEntity(SecurityUser user, E entity, Operation operation) throws ThingsboardException {
        checkNotNull(entity, "Entity not found");
        accessControlService.checkPermission(user, Resource.of(entity.getId().getEntityType()), operation, entity.getId(), entity);
        return entity;
    }

    /**
     * 功能：校验设备ID。
     * 参数：
     * - `deviceId`：设备IDID。
     * - `operation`：`operation` 参数。
     * 返回：判断结果。
     */
    Device checkDeviceId(DeviceId deviceId, Operation operation) throws ThingsboardException {
        return checkEntityId(deviceId, deviceService::findDeviceById, operation);
    }

    /**
     * 功能：校验设备ID。
     * 参数：
     * - `deviceId`：设备IDID。
     * - `operation`：`operation` 参数。
     * 返回：判断结果。
     */
    DeviceInfo checkDeviceInfoId(DeviceId deviceId, Operation operation) throws ThingsboardException {
        return checkEntityId(deviceId, deviceService::findDeviceInfoById, operation);
    }

    /**
     * 功能：校验设备配置。
     * 参数：
     * - `deviceProfileId`：设备配置ID。
     * - `operation`：`operation` 参数。
     * 返回：判断结果。
     */
    DeviceProfile checkDeviceProfileId(DeviceProfileId deviceProfileId, Operation operation) throws ThingsboardException {
        return checkEntityId(deviceProfileId, deviceProfileService::findDeviceProfileById, operation);
    }

    /**
     * 功能：校验实体视图。
     * 参数：
     * - `entityViewId`：实体视图ID。
     * - `operation`：`operation` 参数。
     * 返回：判断结果。
     */
    protected EntityView checkEntityViewId(EntityViewId entityViewId, Operation operation) throws ThingsboardException {
        return checkEntityId(entityViewId, entityViewService::findEntityViewById, operation);
    }

    /**
     * 功能：校验实体视图。
     * 参数：
     * - `entityViewId`：实体视图ID。
     * - `operation`：`operation` 参数。
     * 返回：判断结果。
     */
    EntityViewInfo checkEntityViewInfoId(EntityViewId entityViewId, Operation operation) throws ThingsboardException {
        return checkEntityId(entityViewId, entityViewService::findEntityViewInfoById, operation);
    }

    /**
     * 功能：校验资产ID。
     * 参数：
     * - `assetId`：资产IDID。
     * - `operation`：`operation` 参数。
     * 返回：判断结果。
     */
    Asset checkAssetId(AssetId assetId, Operation operation) throws ThingsboardException {
        return checkEntityId(assetId, assetService::findAssetById, operation);
    }

    /**
     * 功能：校验资产ID。
     * 参数：
     * - `assetId`：资产IDID。
     * - `operation`：`operation` 参数。
     * 返回：判断结果。
     */
    AssetInfo checkAssetInfoId(AssetId assetId, Operation operation) throws ThingsboardException {
        return checkEntityId(assetId, assetService::findAssetInfoById, operation);
    }

    /**
     * 功能：校验资产配置。
     * 参数：
     * - `assetProfileId`：资产配置ID。
     * - `operation`：`operation` 参数。
     * 返回：判断结果。
     */
    AssetProfile checkAssetProfileId(AssetProfileId assetProfileId, Operation operation) throws ThingsboardException {
        return checkEntityId(assetProfileId, assetProfileService::findAssetProfileById, operation);
    }

    /**
     * 功能：校验告警ID。
     * 参数：
     * - `alarmId`：告警IDID。
     * - `operation`：`operation` 参数。
     * 返回：判断结果。
     */
    Alarm checkAlarmId(AlarmId alarmId, Operation operation) throws ThingsboardException {
        return checkEntityId(alarmId, alarmService::findAlarmById, operation);
    }

    /**
     * 功能：校验告警ID。
     * 参数：
     * - `alarmId`：告警IDID。
     * - `operation`：`operation` 参数。
     * 返回：判断结果。
     */
    AlarmInfo checkAlarmInfoId(AlarmId alarmId, Operation operation) throws ThingsboardException {
        return checkEntityId(alarmId, alarmService::findAlarmInfoById, operation);
    }

    /**
     * 功能：校验告警ID。
     * 参数：
     * - `alarmCommentId`：告警IDID。
     * - `alarmId`：告警IDID。
     * 返回：判断结果。
     */
    AlarmComment checkAlarmCommentId(AlarmCommentId alarmCommentId, AlarmId alarmId) throws ThingsboardException {
        try {
            validateId(alarmCommentId, "Incorrect alarmCommentId " + alarmCommentId);
            AlarmComment alarmComment = alarmCommentService.findAlarmCommentByIdAsync(getCurrentUser().getTenantId(), alarmCommentId).get();
            checkNotNull(alarmComment, "Alarm comment with id [" + alarmCommentId + "] is not found");
            if (!alarmId.equals(alarmComment.getAlarmId())) {
                throw new ThingsboardException("Alarm id does not match with comment alarm id", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            }
            return alarmComment;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    /**
     * 功能：校验部件包。
     * 参数：
     * - `widgetsBundleId`：部件包ID。
     * - `operation`：`operation` 参数。
     * 返回：判断结果。
     */
    WidgetsBundle checkWidgetsBundleId(WidgetsBundleId widgetsBundleId, Operation operation) throws ThingsboardException {
        return checkEntityId(widgetsBundleId, widgetsBundleService::findWidgetsBundleById, operation);
    }

    /**
     * 功能：校验部件类型。
     * 参数：
     * - `widgetTypeId`：部件类型ID。
     * - `operation`：`operation` 参数。
     * 返回：判断结果。
     */
    WidgetTypeDetails checkWidgetTypeId(WidgetTypeId widgetTypeId, Operation operation) throws ThingsboardException {
        return checkEntityId(widgetTypeId, widgetTypeService::findWidgetTypeDetailsById, operation);
    }

    /**
     * 功能：校验仪表盘ID。
     * 参数：
     * - `dashboardId`：仪表盘IDID。
     * - `operation`：`operation` 参数。
     * 返回：判断结果。
     */
    Dashboard checkDashboardId(DashboardId dashboardId, Operation operation) throws ThingsboardException {
        return checkEntityId(dashboardId, dashboardService::findDashboardById, operation);
    }

    /**
     * 功能：校验边缘节点。
     * 参数：
     * - `edgeId`：边缘节点ID。
     * - `operation`：`operation` 参数。
     * 返回：判断结果。
     */
    Edge checkEdgeId(EdgeId edgeId, Operation operation) throws ThingsboardException {
        return checkEntityId(edgeId, edgeService::findEdgeById, operation);
    }

    /**
     * 功能：校验边缘节点。
     * 参数：
     * - `edgeId`：边缘节点ID。
     * - `operation`：`operation` 参数。
     * 返回：判断结果。
     */
    EdgeInfo checkEdgeInfoId(EdgeId edgeId, Operation operation) throws ThingsboardException {
        return checkEntityId(edgeId, edgeService::findEdgeInfoById, operation);
    }

    /**
     * 功能：校验仪表盘ID。
     * 参数：
     * - `dashboardId`：仪表盘IDID。
     * - `operation`：`operation` 参数。
     * 返回：判断结果。
     */
    DashboardInfo checkDashboardInfoId(DashboardId dashboardId, Operation operation) throws ThingsboardException {
        return checkEntityId(dashboardId, dashboardService::findDashboardInfoById, operation);
    }

    /**
     * 功能：校验`Component Descriptor By Clazz`。
     * 参数：
     * - `clazz`：`clazz` 参数。
     * 返回：判断结果。
     */
    ComponentDescriptor checkComponentDescriptorByClazz(String clazz) throws ThingsboardException {
        try {
            log.debug("[{}] Lookup component descriptor", clazz);
            return checkNotNull(componentDescriptorService.getComponent(clazz));
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    /**
     * 功能：校验类型。
     * 参数：
     * - `type`：类型。
     * - `ruleChainType`：类型。
     * 返回：判断结果。
     */
    List<ComponentDescriptor> checkComponentDescriptorsByType(ComponentType type, RuleChainType ruleChainType) throws ThingsboardException {
        try {
            log.debug("[{}] Lookup component descriptors", type);
            return componentDescriptorService.getComponents(type, ruleChainType);
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    /**
     * 功能：校验`Component Descriptors By Types`。
     * 参数：
     * - `types`：类型。
     * - `ruleChainType`：类型。
     * 返回：判断结果。
     */
    List<ComponentDescriptor> checkComponentDescriptorsByTypes(Set<ComponentType> types, RuleChainType ruleChainType) throws ThingsboardException {
        try {
            log.debug("[{}] Lookup component descriptors", types);
            return componentDescriptorService.getComponents(types, ruleChainType);
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    /**
     * 功能：校验规则链。
     * 参数：
     * - `ruleChainId`：规则链ID。
     * - `operation`：`operation` 参数。
     * 返回：判断结果。
     */
    protected RuleChain checkRuleChain(RuleChainId ruleChainId, Operation operation) throws ThingsboardException {
        return checkEntityId(ruleChainId, ruleChainService::findRuleChainById, operation);
    }

    /**
     * 功能：校验规则节点。
     * 参数：
     * - `ruleNodeId`：规则节点ID。
     * - `operation`：`operation` 参数。
     * 返回：判断结果。
     */
    protected RuleNode checkRuleNode(RuleNodeId ruleNodeId, Operation operation) throws ThingsboardException {
        validateId(ruleNodeId, "Incorrect ruleNodeId " + ruleNodeId);
        RuleNode ruleNode = ruleChainService.findRuleNodeById(getTenantId(), ruleNodeId);
        checkNotNull(ruleNode, "Rule node with id [" + ruleNodeId + "] is not found");
        checkRuleChain(ruleNode.getRuleChainId(), operation);
        return ruleNode;
    }

    /**
     * 功能：校验`Resource Id`。
     * 参数：
     * - `resourceId`：`resourceId`ID。
     * - `operation`：`operation` 参数。
     * 返回：判断结果。
     */
    TbResource checkResourceId(TbResourceId resourceId, Operation operation) throws ThingsboardException {
        return checkEntityId(resourceId, resourceService::findResourceById, operation);
    }

    /**
     * 功能：校验信息对象。
     * 参数：
     * - `resourceId`：`resourceId`ID。
     * - `operation`：`operation` 参数。
     * 返回：判断结果。
     */
    TbResourceInfo checkResourceInfoId(TbResourceId resourceId, Operation operation) throws ThingsboardException {
        return checkEntityId(resourceId, resourceService::findResourceInfoById, operation);
    }

    /**
     * 功能：校验`Ota Package Id`。
     * 参数：
     * - `otaPackageId`：`otaPackageId`ID。
     * - `operation`：`operation` 参数。
     * 返回：判断结果。
     */
    OtaPackage checkOtaPackageId(OtaPackageId otaPackageId, Operation operation) throws ThingsboardException {
        return checkEntityId(otaPackageId, otaPackageService::findOtaPackageById, operation);
    }

    /**
     * 功能：校验信息对象。
     * 参数：
     * - `otaPackageId`：`otaPackageId`ID。
     * - `operation`：`operation` 参数。
     * 返回：判断结果。
     */
    OtaPackageInfo checkOtaPackageInfoId(OtaPackageId otaPackageId, Operation operation) throws ThingsboardException {
        return checkEntityId(otaPackageId, otaPackageService::findOtaPackageInfoById, operation);
    }

    /**
     * 功能：校验RPC。
     * 参数：
     * - `rpcId`：RPCID。
     * - `operation`：`operation` 参数。
     * 返回：判断结果。
     */
    Rpc checkRpcId(RpcId rpcId, Operation operation) throws ThingsboardException {
        return checkEntityId(rpcId, rpcService::findById, operation);
    }

    /**
     * 功能：校验队列。
     * 参数：
     * - `queueId`：队列ID。
     * - `operation`：`operation` 参数。
     * 返回：判断结果。
     */
    protected Queue checkQueueId(QueueId queueId, Operation operation) throws ThingsboardException {
        Queue queue = checkEntityId(queueId, queueService::findQueueById, operation);
        TenantId tenantId = getTenantId();
        if (queue.getTenantId().isNullUid() && !tenantId.isNullUid()) {
            TenantProfile tenantProfile = tenantProfileCache.get(tenantId);
            if (tenantProfile.isIsolatedTbRuleEngine()) {
                throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION,
                        ThingsboardErrorCode.PERMISSION_DENIED);
            }
        }
        return queue;
    }

    /**
     * 功能：执行 `emptyId` 对应的处理。
     * 参数：
     * - `entityType`：实体对象。
     * 返回：处理结果。
     */
    protected <I extends EntityId> I emptyId(EntityType entityType) {
        return (I) EntityIdFactory.getByTypeAndUuid(entityType, ModelConstants.NULL_UUID);
    }

    /**
     * 功能：执行 `toException` 对应的处理。
     * 参数：
     * - `error`：错误信息。
     * 返回：处理结果。
     */
    public static Exception toException(Throwable error) {
        return error != null ? (Exception.class.isInstance(error) ? (Exception) error : new Exception(error)) : null;
    }

    protected <E extends HasName & HasId<? extends EntityId>> void logEntityAction(SecurityUser user, EntityType entityType, E savedEntity, ActionType actionType) {
        logEntityAction(user, entityType, null, savedEntity, actionType, null);
    }

    protected <E extends HasName & HasId<? extends EntityId>> void logEntityAction(SecurityUser user, EntityType entityType, E entity, E savedEntity, ActionType actionType, Exception e) {
        EntityId entityId = savedEntity != null ? savedEntity.getId() : emptyId(entityType);
        if (!user.isSystemAdmin()) {
            entityActionService.logEntityAction(user, entityId, savedEntity != null ? savedEntity : entity,
                    user.getCustomerId(), actionType, e);
        }
    }

    protected <E extends HasName & HasId<? extends EntityId>> E doSaveAndLog(EntityType entityType, E entity, BiFunction<TenantId, E, E> savingFunction) throws Exception {
        ActionType actionType = entity.getId() == null ? ActionType.ADDED : ActionType.UPDATED;
        SecurityUser user = getCurrentUser();
        try {
            E savedEntity = savingFunction.apply(user.getTenantId(), entity);
            logEntityAction(user, entityType, savedEntity, actionType);
            return savedEntity;
        } catch (Exception e) {
            logEntityAction(user, entityType, entity, null, actionType, e);
            throw e;
        }
    }

    protected <E extends HasName & HasId<I>, I extends EntityId> void doDeleteAndLog(EntityType entityType, E entity, BiConsumer<TenantId, I> deleteFunction) throws Exception {
        SecurityUser user = getCurrentUser();
        try {
            deleteFunction.accept(user.getTenantId(), entity.getId());
            logEntityAction(user, entityType, entity, ActionType.DELETED);
        } catch (Exception e) {
            logEntityAction(user, entityType, entity, entity, ActionType.DELETED, e);
            throw e;
        }
    }

    /**
     * 功能：处理扩展信息。
     * 参数：
     * - `additionalInfo`：`additionalInfo` 参数。
     * - `requiredFields`：`requiredFields` 参数。
     * 返回：无。
     */
    protected void processDashboardIdFromAdditionalInfo(ObjectNode additionalInfo, String requiredFields) throws ThingsboardException {
        String dashboardId = additionalInfo.has(requiredFields) ? additionalInfo.get(requiredFields).asText() : null;
        if (dashboardId != null && !dashboardId.equals("null")) {
            if (dashboardService.findDashboardById(getTenantId(), new DashboardId(UUID.fromString(dashboardId))) == null) {
                additionalInfo.remove(requiredFields);
            }
        }
    }

    /**
     * 功能：解析类型。
     * 参数：
     * - `contentType`：类型。
     * 返回：处理结果。
     */
    protected MediaType parseMediaType(String contentType) {
        try {
            return MediaType.parseMediaType(contentType);
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    /**
     * 功能：执行 `wrapFuture` 对应的处理。
     * 参数：
     * - `future`：数据列表。
     * 返回：处理结果。
     */
    protected <T> DeferredResult<T> wrapFuture(ListenableFuture<T> future) {
        DeferredResult<T> deferredResult = new DeferredResult<>(); // Timeout of spring.mvc.async.request-timeout is used
        DonAsynchron.withCallback(future, deferredResult::setResult, deferredResult::setErrorResult);
        return deferredResult;
    }

    /**
     * 功能：执行 `wrapFuture` 对应的处理。
     * 参数：
     * - `future`：数据列表。
     * - `timeoutMs`：`timeoutMs` 参数。
     * 返回：处理结果。
     */
    protected <T> DeferredResult<T> wrapFuture(ListenableFuture<T> future, long timeoutMs) {
        DeferredResult<T> deferredResult = new DeferredResult<>(timeoutMs);
        DonAsynchron.withCallback(future, deferredResult::setResult, deferredResult::setErrorResult);
        return deferredResult;
    }

    /**
     * 功能：保存或创建排序规则。
     * 参数：
     * - `sortProperty`：`sortProperty` 参数。
     * - `sortOrder`：`sortOrder` 参数。
     * 返回：处理结果。
     */
    protected EntityDataSortOrder createEntityDataSortOrder(String sortProperty, String sortOrder) {
        if (isNotEmpty(sortProperty)) {
            EntityDataSortOrder entityDataSortOrder = new EntityDataSortOrder();
            entityDataSortOrder.setKey(new EntityKey(ENTITY_FIELD, sortProperty));
            if (isNotEmpty(sortOrder)) {
                entityDataSortOrder.setDirection(EntityDataSortOrder.Direction.valueOf(sortOrder));
            }
            return entityDataSortOrder;
        } else {
            return null;
        }
    }

}
