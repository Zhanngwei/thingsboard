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

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.server.common.data.ClaimRequest;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceInfo;
import org.thingsboard.server.common.data.DeviceInfoFilter;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.SaveDeviceWithCredentialsRequest;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.device.DeviceSearchQuery;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.sync.ie.importing.csv.BulkImportRequest;
import org.thingsboard.server.common.data.sync.ie.importing.csv.BulkImportResult;
import org.thingsboard.server.dao.device.claim.ClaimResponse;
import org.thingsboard.server.dao.device.claim.ClaimResult;
import org.thingsboard.server.dao.device.claim.ReclaimResult;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.device.DeviceBulkImportService;
import org.thingsboard.server.service.entitiy.device.TbDeviceService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import javax.annotation.Nullable;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.thingsboard.server.controller.ControllerConstants.CUSTOMER_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.CUSTOMER_ID;
import static org.thingsboard.server.controller.ControllerConstants.CUSTOMER_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.DEVICE_ACTIVE_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.DEVICE_ID;
import static org.thingsboard.server.controller.ControllerConstants.DEVICE_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.DEVICE_INFO_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.DEVICE_NAME_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.DEVICE_PROFILE_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.DEVICE_SORT_PROPERTY_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.DEVICE_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.DEVICE_TYPE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.DEVICE_UPDATE_CREDENTIALS_PARAM_ACCESS_TOKEN_DESCRIPTION_MARKDOWN;
import static org.thingsboard.server.controller.ControllerConstants.DEVICE_UPDATE_CREDENTIALS_PARAM_LVM2M_RPK_DESCRIPTION_MARKDOWN;
import static org.thingsboard.server.controller.ControllerConstants.DEVICE_UPDATE_CREDENTIALS_PARAM_MQTT_BASIC_DESCRIPTION_MARKDOWN;
import static org.thingsboard.server.controller.ControllerConstants.DEVICE_UPDATE_CREDENTIALS_PARAM_X509_CERTIFICATE_DESCRIPTION_MARKDOWN;
import static org.thingsboard.server.controller.ControllerConstants.DEVICE_WITH_DEVICE_CREDENTIALS_PARAM_ACCESS_TOKEN_DEFAULT_DESCRIPTION_MARKDOWN;
import static org.thingsboard.server.controller.ControllerConstants.DEVICE_WITH_DEVICE_CREDENTIALS_PARAM_ACCESS_TOKEN_DESCRIPTION_MARKDOWN;
import static org.thingsboard.server.controller.ControllerConstants.DEVICE_WITH_DEVICE_CREDENTIALS_PARAM_LVM2M_RPK_DESCRIPTION_MARKDOWN;
import static org.thingsboard.server.controller.ControllerConstants.DEVICE_WITH_DEVICE_CREDENTIALS_PARAM_MQTT_BASIC_DESCRIPTION_MARKDOWN;
import static org.thingsboard.server.controller.ControllerConstants.DEVICE_WITH_DEVICE_CREDENTIALS_PARAM_X509_CERTIFICATE_DESCRIPTION_MARKDOWN;
import static org.thingsboard.server.controller.ControllerConstants.EDGE_ASSIGN_ASYNC_FIRST_STEP_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.EDGE_ASSIGN_RECEIVE_STEP_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.EDGE_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.EDGE_UNASSIGN_ASYNC_FIRST_STEP_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.EDGE_UNASSIGN_RECEIVE_STEP_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_ID;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.UUID_WIKI_LINK;
import static org.thingsboard.server.controller.EdgeController.EDGE_ID;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
/**
 * 中文说明：
 * 1. 类目的：`DeviceController` 是ThingsBoard Application 模块中的REST/WebSocket 控制层类型，用于承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 生命周期：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 MVC Controller / Facade。
 */
public class DeviceController extends BaseController {

    /**
     * 字段说明：
     * 1. 保存 `DEVICE_NAME` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    protected static final String DEVICE_NAME = "deviceName";

    /**
     * 字段说明：
     * 1. 保存 `deviceBulkImportService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final DeviceBulkImportService deviceBulkImportService;

    /**
     * 字段说明：
     * 1. 保存 `tbDeviceService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final TbDeviceService tbDeviceService;

    @ApiOperation(value = "Get Device (getDeviceById)",
            notes = "Fetch the Device object based on the provided Device Id. " +
                    "If the user has the authority of 'TENANT_ADMIN', the server checks that the device is owned by the same tenant. " +
                    "If the user has the authority of 'CUSTOMER_USER', the server checks that the device is assigned to the same customer." +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/device/{deviceId}", method = RequestMethod.GET)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getDeviceById` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Device getDeviceById(@ApiParam(value = DEVICE_ID_PARAM_DESCRIPTION)
                                @PathVariable(DEVICE_ID) String strDeviceId) throws ThingsboardException {
        checkParameter(DEVICE_ID, strDeviceId);
        DeviceId deviceId = new DeviceId(toUUID(strDeviceId));
        return checkDeviceId(deviceId, Operation.READ);
    }

    @ApiOperation(value = "Get Device Info (getDeviceInfoById)",
            notes = "Fetch the Device Info object based on the provided Device Id. " +
                    "If the user has the authority of 'Tenant Administrator', the server checks that the device is owned by the same tenant. " +
                    "If the user has the authority of 'Customer User', the server checks that the device is assigned to the same customer. " +
                    DEVICE_INFO_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/device/info/{deviceId}", method = RequestMethod.GET)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getDeviceInfoById` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public DeviceInfo getDeviceInfoById(@ApiParam(value = DEVICE_ID_PARAM_DESCRIPTION)
                                        @PathVariable(DEVICE_ID) String strDeviceId) throws ThingsboardException {
        checkParameter(DEVICE_ID, strDeviceId);
        DeviceId deviceId = new DeviceId(toUUID(strDeviceId));
        return checkDeviceInfoId(deviceId, Operation.READ);
    }

    @ApiOperation(value = "Create Or Update Device (saveDevice)",
            notes = "Create or update the Device. When creating device, platform generates Device Id as " + UUID_WIKI_LINK +
                    "Device credentials are also generated if not provided in the 'accessToken' request parameter. " +
                    "The newly created device id will be present in the response. " +
                    "Specify existing Device id to update the device. " +
                    "Referencing non-existing device Id will cause 'Not Found' error." +
                    "\n\nDevice name is unique in the scope of tenant. Use unique identifiers like MAC or IMEI for the device names and non-unique 'label' field for user-friendly visualization purposes." +
                    "Remove 'id', 'tenantId' and optionally 'customerId' from the request body example (below) to create new Device entity. " +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/device", method = RequestMethod.POST)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `saveDevice` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Device saveDevice(@ApiParam(value = "A JSON value representing the device.") @RequestBody Device device,
                             @ApiParam(value = "Optional value of the device credentials to be used during device creation. " +
                                     "If omitted, access token will be auto-generated.") @RequestParam(name = "accessToken", required = false) String accessToken) throws Exception {
        device.setTenantId(getCurrentUser().getTenantId());
        Device oldDevice = null;
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (device.getId() != null) {
            oldDevice = checkDeviceId(device.getId(), Operation.WRITE);
        } else {
            checkEntity(null, device, Resource.DEVICE);
        }
        return tbDeviceService.save(device, oldDevice, accessToken, getCurrentUser());
    }

    @ApiOperation(value = "Create Device (saveDevice) with credentials ",
            notes = "Create or update the Device. When creating device, platform generates Device Id as " + UUID_WIKI_LINK +
                    "Requires to provide the Device Credentials object as well as an existing device profile ID or use \"default\".\n" +
                    "You may find the example of device with different type of credentials below: \n\n" +
                    "- Credentials type: <b>\"Access token\"</b> with <b>device profile ID</b> below: \n\n" +
                    DEVICE_WITH_DEVICE_CREDENTIALS_PARAM_ACCESS_TOKEN_DESCRIPTION_MARKDOWN + "\n\n" +
                    "- Credentials type: <b>\"Access token\"</b> with  <b>device profile default</b> below: \n\n" +
                    DEVICE_WITH_DEVICE_CREDENTIALS_PARAM_ACCESS_TOKEN_DEFAULT_DESCRIPTION_MARKDOWN + "\n\n" +
                    "- Credentials type: <b>\"X509\"</b> with <b>device profile ID</b> below: \n\n" +
                    "Note: <b>credentialsId</b> -  format <b>Sha3Hash</b>, <b>certificateValue</b> - format <b>PEM</b> (with \"--BEGIN CERTIFICATE----\" and  -\"----END CERTIFICATE-\").\n\n" +
                    DEVICE_WITH_DEVICE_CREDENTIALS_PARAM_X509_CERTIFICATE_DESCRIPTION_MARKDOWN + "\n\n" +
                    "- Credentials type: <b>\"MQTT_BASIC\"</b> with <b>device profile ID</b> below: \n\n" +
                    DEVICE_WITH_DEVICE_CREDENTIALS_PARAM_MQTT_BASIC_DESCRIPTION_MARKDOWN + "\n\n" +
                    "- You may find the example of <b>LwM2M</b> device and <b>RPK</b> credentials below: \n\n" +
                    "Note: LwM2M device - only existing device profile ID (Transport configuration -> Transport type: \"LWM2M\".\n\n" +
                    DEVICE_WITH_DEVICE_CREDENTIALS_PARAM_LVM2M_RPK_DESCRIPTION_MARKDOWN + "\n\n" +
                    "Remove 'id', 'tenantId' and optionally 'customerId' from the request body example (below) to create new Device entity. " +
                    TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/device-with-credentials", method = RequestMethod.POST)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `saveDeviceWithCredentials` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Device saveDeviceWithCredentials(@ApiParam(value = "The JSON object with device and credentials. See method description above for example.")
                                            @Valid @RequestBody SaveDeviceWithCredentialsRequest deviceAndCredentials) throws ThingsboardException {
        Device device = deviceAndCredentials.getDevice();
        DeviceCredentials credentials = deviceAndCredentials.getCredentials();
        device.setTenantId(getCurrentUser().getTenantId());
        checkEntity(device.getId(), device, Resource.DEVICE);
        return tbDeviceService.saveDeviceWithCredentials(device, credentials, getCurrentUser());
    }

    @ApiOperation(value = "Delete device (deleteDevice)",
            notes = "Deletes the device, it's credentials and all the relations (from and to the device). Referencing non-existing device Id will cause an error." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/device/{deviceId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    /**
     * 方法说明：
     * 1. 职责：执行 `deleteDevice` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void deleteDevice(@ApiParam(value = DEVICE_ID_PARAM_DESCRIPTION)
                             @PathVariable(DEVICE_ID) String strDeviceId) throws Exception {
        checkParameter(DEVICE_ID, strDeviceId);
        DeviceId deviceId = new DeviceId(toUUID(strDeviceId));
        Device device = checkDeviceId(deviceId, Operation.DELETE);
        tbDeviceService.delete(device, getCurrentUser());
    }

    @ApiOperation(value = "Assign device to customer (assignDeviceToCustomer)",
            notes = "Creates assignment of the device to customer. Customer will be able to query device afterwards." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/customer/{customerId}/device/{deviceId}", method = RequestMethod.POST)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `assignDeviceToCustomer` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Device assignDeviceToCustomer(@ApiParam(value = CUSTOMER_ID_PARAM_DESCRIPTION)
                                         @PathVariable("customerId") String strCustomerId,
                                         @ApiParam(value = DEVICE_ID_PARAM_DESCRIPTION)
                                         @PathVariable(DEVICE_ID) String strDeviceId) throws ThingsboardException {
        checkParameter("customerId", strCustomerId);
        checkParameter(DEVICE_ID, strDeviceId);
        CustomerId customerId = new CustomerId(toUUID(strCustomerId));
        Customer customer = checkCustomerId(customerId, Operation.READ);
        DeviceId deviceId = new DeviceId(toUUID(strDeviceId));
        checkDeviceId(deviceId, Operation.ASSIGN_TO_CUSTOMER);
        return tbDeviceService.assignDeviceToCustomer(getTenantId(), deviceId, customer, getCurrentUser());
    }

    @ApiOperation(value = "Unassign device from customer (unassignDeviceFromCustomer)",
            notes = "Clears assignment of the device to customer. Customer will not be able to query device afterwards." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/customer/device/{deviceId}", method = RequestMethod.DELETE)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `unassignDeviceFromCustomer` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Device unassignDeviceFromCustomer(@ApiParam(value = DEVICE_ID_PARAM_DESCRIPTION)
                                             @PathVariable(DEVICE_ID) String strDeviceId) throws ThingsboardException {
        checkParameter(DEVICE_ID, strDeviceId);
        DeviceId deviceId = new DeviceId(toUUID(strDeviceId));
        Device device = checkDeviceId(deviceId, Operation.UNASSIGN_FROM_CUSTOMER);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (device.getCustomerId() == null || device.getCustomerId().getId().equals(ModelConstants.NULL_UUID)) {
            throw new IncorrectParameterException("Device isn't assigned to any customer!");
        }

        Customer customer = checkCustomerId(device.getCustomerId(), Operation.READ);

        return tbDeviceService.unassignDeviceFromCustomer(device, customer, getCurrentUser());
    }

    @ApiOperation(value = "Make device publicly available (assignDeviceToPublicCustomer)",
            notes = "Device will be available for non-authorized (not logged-in) users. " +
                    "This is useful to create dashboards that you plan to share/embed on a publicly available website. " +
                    "However, users that are logged-in and belong to different tenant will not be able to access the device." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/customer/public/device/{deviceId}", method = RequestMethod.POST)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `assignDeviceToPublicCustomer` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Device assignDeviceToPublicCustomer(@ApiParam(value = DEVICE_ID_PARAM_DESCRIPTION)
                                               @PathVariable(DEVICE_ID) String strDeviceId) throws ThingsboardException {
        checkParameter(DEVICE_ID, strDeviceId);
        DeviceId deviceId = new DeviceId(toUUID(strDeviceId));
        checkDeviceId(deviceId, Operation.ASSIGN_TO_CUSTOMER);
        return tbDeviceService.assignDeviceToPublicCustomer(getTenantId(), deviceId, getCurrentUser());
    }

    @ApiOperation(value = "Get Device Credentials (getDeviceCredentialsByDeviceId)",
            notes = "If during device creation there wasn't specified any credentials, platform generates random 'ACCESS_TOKEN' credentials." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/device/{deviceId}/credentials", method = RequestMethod.GET)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getDeviceCredentialsByDeviceId` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public DeviceCredentials getDeviceCredentialsByDeviceId(@ApiParam(value = DEVICE_ID_PARAM_DESCRIPTION)
                                                            @PathVariable(DEVICE_ID) String strDeviceId) throws ThingsboardException {
        checkParameter(DEVICE_ID, strDeviceId);
        DeviceId deviceId = new DeviceId(toUUID(strDeviceId));
        Device device = checkDeviceId(deviceId, Operation.READ_CREDENTIALS);
        return tbDeviceService.getDeviceCredentialsByDeviceId(device, getCurrentUser());
    }

    @ApiOperation(value = "Update device credentials (updateDeviceCredentials)",
            notes = "During device creation, platform generates random 'ACCESS_TOKEN' credentials. \" +\n" +
                    "Use this method to update the device credentials. First use 'getDeviceCredentialsByDeviceId' to get the credentials id and value.\n" +
                    "Then use current method to update the credentials type and value. It is not possible to create multiple device credentials for the same device.\n" +
                    "The structure of device credentials id and value is simple for the 'ACCESS_TOKEN' but is much more complex for the 'MQTT_BASIC' or 'LWM2M_CREDENTIALS'.\n" +
                    "You may find the example of device with different type of credentials below: \n\n" +
                    "- Credentials type: <b>\"Access token\"</b> with <b>device ID</b> and with <b>device ID</b> below: \n\n" +
                    DEVICE_UPDATE_CREDENTIALS_PARAM_ACCESS_TOKEN_DESCRIPTION_MARKDOWN + "\n\n" +
                    "- Credentials type: <b>\"X509\"</b> with <b>device profile ID</b> below: \n\n" +
                    "Note: <b>credentialsId</b> -  format <b>Sha3Hash</b>, <b>certificateValue</b> - format <b>PEM</b> (with \"--BEGIN CERTIFICATE----\" and  -\"----END CERTIFICATE-\").\n\n" +
                    DEVICE_UPDATE_CREDENTIALS_PARAM_X509_CERTIFICATE_DESCRIPTION_MARKDOWN + "\n\n" +
                    "- Credentials type: <b>\"MQTT_BASIC\"</b> with <b>device profile ID</b> below: \n\n" +
                    DEVICE_UPDATE_CREDENTIALS_PARAM_MQTT_BASIC_DESCRIPTION_MARKDOWN + "\n\n" +
                    "- You may find the example of <b>LwM2M</b> device and <b>RPK</b> credentials below: \n\n" +
                    "Note: LwM2M device - only existing device profile ID (Transport configuration -> Transport type: \"LWM2M\".\n\n" +
                    DEVICE_UPDATE_CREDENTIALS_PARAM_LVM2M_RPK_DESCRIPTION_MARKDOWN + "\n\n" +
                    "Update to real value:\n" +
                    " - 'id' (this is id of Device Credentials ->  \"Get Device Credentials (getDeviceCredentialsByDeviceId)\",\n" +
                    " - 'deviceId.id' (this is id of Device).\n" +
                    "Remove 'tenantId' and optionally 'customerId' from the request body example (below) to create new Device entity." +
                    TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/device/credentials", method = RequestMethod.POST)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `updateDeviceCredentials` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public DeviceCredentials updateDeviceCredentials(
            @ApiParam(value = "A JSON value representing the device credentials.")
            @RequestBody DeviceCredentials deviceCredentials) throws ThingsboardException {
        checkNotNull(deviceCredentials);
        Device device = checkDeviceId(deviceCredentials.getDeviceId(), Operation.WRITE_CREDENTIALS);
        return tbDeviceService.updateDeviceCredentials(device, deviceCredentials, getCurrentUser());
    }

    @ApiOperation(value = "Get Tenant Devices (getTenantDevices)",
            notes = "Returns a page of devices owned by tenant. " +
                    PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/devices", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getTenantDevices` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public PageData<Device> getTenantDevices(
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = DEVICE_TYPE_DESCRIPTION)
            @RequestParam(required = false) String type,
            @ApiParam(value = DEVICE_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = DEVICE_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        TenantId tenantId = getCurrentUser().getTenantId();
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (type != null && type.trim().length() > 0) {
            return checkNotNull(deviceService.findDevicesByTenantIdAndType(tenantId, type, pageLink));
        } else {
            return checkNotNull(deviceService.findDevicesByTenantId(tenantId, pageLink));
        }
    }

    @ApiOperation(value = "Get Tenant Device Infos (getTenantDeviceInfos)",
            notes = "Returns a page of devices info objects owned by tenant. " +
                    PAGE_DATA_PARAMETERS + DEVICE_INFO_DESCRIPTION + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/deviceInfos", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getTenantDeviceInfos` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public PageData<DeviceInfo> getTenantDeviceInfos(
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = DEVICE_TYPE_DESCRIPTION)
            @RequestParam(required = false) String type,
            @ApiParam(value = DEVICE_PROFILE_ID_PARAM_DESCRIPTION)
            @RequestParam(required = false) String deviceProfileId,
            @ApiParam(value = DEVICE_ACTIVE_PARAM_DESCRIPTION)
            @RequestParam(required = false) Boolean active,
            @ApiParam(value = DEVICE_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = DEVICE_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder
    ) throws ThingsboardException {
        TenantId tenantId = getCurrentUser().getTenantId();
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        DeviceInfoFilter.DeviceInfoFilterBuilder filter = DeviceInfoFilter.builder();
        filter.tenantId(tenantId);
        filter.active(active);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (type != null && type.trim().length() > 0) {
            filter.type(type);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        } else if (deviceProfileId != null && deviceProfileId.length() > 0) {
            filter.deviceProfileId(new DeviceProfileId(toUUID(deviceProfileId)));
        }
        return checkNotNull(deviceService.findDeviceInfosByFilter(filter.build(), pageLink));
    }

    @ApiOperation(value = "Get Tenant Device (getTenantDevice)",
            notes = "Requested device must be owned by tenant that the user belongs to. " +
                    "Device name is an unique property of device. So it can be used to identify the device." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/devices", params = {"deviceName"}, method = RequestMethod.GET)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getTenantDevice` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Device getTenantDevice(
            @ApiParam(value = DEVICE_NAME_DESCRIPTION)
            @RequestParam String deviceName) throws ThingsboardException {
        TenantId tenantId = getCurrentUser().getTenantId();
        return checkNotNull(deviceService.findDeviceByTenantIdAndName(tenantId, deviceName));
    }

    @ApiOperation(value = "Get Customer Devices (getCustomerDevices)",
            notes = "Returns a page of devices objects assigned to customer. " +
                    PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customer/{customerId}/devices", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getCustomerDevices` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public PageData<Device> getCustomerDevices(
            @ApiParam(value = CUSTOMER_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(CUSTOMER_ID) String strCustomerId,
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = DEVICE_TYPE_DESCRIPTION)
            @RequestParam(required = false) String type,
            @ApiParam(value = DEVICE_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = DEVICE_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter("customerId", strCustomerId);
        TenantId tenantId = getCurrentUser().getTenantId();
        CustomerId customerId = new CustomerId(toUUID(strCustomerId));
        checkCustomerId(customerId, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (type != null && type.trim().length() > 0) {
            return checkNotNull(deviceService.findDevicesByTenantIdAndCustomerIdAndType(tenantId, customerId, type, pageLink));
        } else {
            return checkNotNull(deviceService.findDevicesByTenantIdAndCustomerId(tenantId, customerId, pageLink));
        }
    }

    @ApiOperation(value = "Get Customer Device Infos (getCustomerDeviceInfos)",
            notes = "Returns a page of devices info objects assigned to customer. " +
                    PAGE_DATA_PARAMETERS + DEVICE_INFO_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customer/{customerId}/deviceInfos", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getCustomerDeviceInfos` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public PageData<DeviceInfo> getCustomerDeviceInfos(
            @ApiParam(value = CUSTOMER_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable("customerId") String strCustomerId,
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = DEVICE_TYPE_DESCRIPTION)
            @RequestParam(required = false) String type,
            @ApiParam(value = DEVICE_PROFILE_ID_PARAM_DESCRIPTION)
            @RequestParam(required = false) String deviceProfileId,
            @ApiParam(value = DEVICE_ACTIVE_PARAM_DESCRIPTION)
            @RequestParam(required = false) Boolean active,
            @ApiParam(value = DEVICE_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = DEVICE_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        checkParameter("customerId", strCustomerId);
        TenantId tenantId = getCurrentUser().getTenantId();
        CustomerId customerId = new CustomerId(toUUID(strCustomerId));
        checkCustomerId(customerId, Operation.READ);
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        DeviceInfoFilter.DeviceInfoFilterBuilder filter = DeviceInfoFilter.builder();
        filter.tenantId(tenantId);
        filter.customerId(customerId);
        filter.active(active);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (type != null && type.trim().length() > 0) {
            filter.type(type);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        } else if (deviceProfileId != null && deviceProfileId.length() > 0) {
            filter.deviceProfileId(new DeviceProfileId(toUUID(deviceProfileId)));
        }
        return checkNotNull(deviceService.findDeviceInfosByFilter(filter.build(), pageLink));
    }

    @ApiOperation(value = "Get Devices By Ids (getDevicesByIds)",
            notes = "Requested devices must be owned by tenant or assigned to customer which user is performing the request. " + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/devices", params = {"deviceIds"}, method = RequestMethod.GET)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getDevicesByIds` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public List<Device> getDevicesByIds(
            @ApiParam(value = "A list of devices ids, separated by comma ','")
            @RequestParam("deviceIds") String[] strDeviceIds) throws ThingsboardException, ExecutionException, InterruptedException {
        checkArrayParameter("deviceIds", strDeviceIds);
        SecurityUser user = getCurrentUser();
        TenantId tenantId = user.getTenantId();
        CustomerId customerId = user.getCustomerId();
        List<DeviceId> deviceIds = new ArrayList<>();
        // 循环处理批量实体或消息集合，需关注单项失败对整体流程的影响。
        for (String strDeviceId : strDeviceIds) {
            deviceIds.add(new DeviceId(toUUID(strDeviceId)));
        }
        // 异步结果通过回调继续处理，调用线程不会在这里同步等待完整业务链路。
        ListenableFuture<List<Device>> devices;
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (customerId == null || customerId.isNullUid()) {
            devices = deviceService.findDevicesByTenantIdAndIdsAsync(tenantId, deviceIds);
        } else {
            devices = deviceService.findDevicesByTenantIdCustomerIdAndIdsAsync(tenantId, customerId, deviceIds);
        }
        return checkNotNull(devices.get());
    }

    @ApiOperation(value = "Find related devices (findByQuery)",
            notes = "Returns all devices that are related to the specific entity. " +
                    "The entity id, relation type, device types, depth of the search, and other query parameters defined using complex 'DeviceSearchQuery' object. " +
                    "See 'Model' tab of the Parameters for more info." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/devices", method = RequestMethod.POST)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `findByQuery` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public List<Device> findByQuery(
            @ApiParam(value = "The device search query JSON")
            @RequestBody DeviceSearchQuery query) throws ThingsboardException, ExecutionException, InterruptedException {
        checkNotNull(query);
        checkNotNull(query.getParameters());
        checkNotNull(query.getDeviceTypes());
        checkEntityId(query.getParameters().getEntityId(), Operation.READ);
        List<Device> devices = checkNotNull(deviceService.findDevicesByQuery(getCurrentUser().getTenantId(), query).get());
        devices = devices.stream().filter(device -> {
            try {
                accessControlService.checkPermission(getCurrentUser(), Resource.DEVICE, Operation.READ, device.getId(), device);
                return true;
            // 异常在这里被转换为统一失败路径，避免底层异常直接泄露到调用方。
            } catch (ThingsboardException e) {
                return false;
            }
        }).collect(Collectors.toList());
        return devices;
    }

    @ApiOperation(value = "Get Device Types (getDeviceTypes)",
            notes = "Deprecated. See 'getDeviceProfileNames' API from Device Profile Controller instead." + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/device/types", method = RequestMethod.GET)
    @ResponseBody
    @Deprecated(since = "3.6.2")
    /**
     * 方法说明：
     * 1. 职责：执行 `getDeviceTypes` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public List<EntitySubtype> getDeviceTypes() throws ThingsboardException, ExecutionException, InterruptedException {
        SecurityUser user = getCurrentUser();
        TenantId tenantId = user.getTenantId();
        ListenableFuture<List<EntitySubtype>> deviceTypes = deviceService.findDeviceTypesByTenantId(tenantId);
        return checkNotNull(deviceTypes.get());
    }

    @ApiOperation(value = "Claim device (claimDevice)",
            notes = "Claiming makes it possible to assign a device to the specific customer using device/server side claiming data (in the form of secret key)." +
                    "To make this happen you have to provide unique device name and optional claiming data (it is needed only for device-side claiming)." +
                    "Once device is claimed, the customer becomes its owner and customer users may access device data as well as control the device. \n" +
                    "In order to enable claiming devices feature a system parameter security.claim.allowClaimingByDefault should be set to true, " +
                    "otherwise a server-side claimingAllowed attribute with the value true is obligatory for provisioned devices. \n" +
                    "See official documentation for more details regarding claiming." + CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('CUSTOMER_USER')")
    @RequestMapping(value = "/customer/device/{deviceName}/claim", method = RequestMethod.POST)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `claimDevice` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public DeferredResult<ResponseEntity> claimDevice(@ApiParam(value = "Unique name of the device which is going to be claimed")
                                                      @PathVariable(DEVICE_NAME) String deviceName,
                                                      @ApiParam(value = "Claiming request which can optionally contain secret key")
                                                      @RequestBody(required = false) ClaimRequest claimRequest) throws ThingsboardException {
        checkParameter(DEVICE_NAME, deviceName);
        final DeferredResult<ResponseEntity> deferredResult = new DeferredResult<>();

        SecurityUser user = getCurrentUser();
        TenantId tenantId = user.getTenantId();
        CustomerId customerId = user.getCustomerId();

        Device device = checkNotNull(deviceService.findDeviceByTenantIdAndName(tenantId, deviceName));
        accessControlService.checkPermission(user, Resource.DEVICE, Operation.CLAIM_DEVICES,
                device.getId(), device);
        String secretKey = getSecretKey(claimRequest);

        ListenableFuture<ClaimResult> future = tbDeviceService.claimDevice(tenantId, device, customerId, secretKey, user);

        Futures.addCallback(future, new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable ClaimResult result) {
                HttpStatus status;
                if (result != null) {
                    if (result.getResponse().equals(ClaimResponse.SUCCESS)) {
                        status = HttpStatus.OK;
                        deferredResult.setResult(new ResponseEntity<>(result, status));
                    } else {
                        status = HttpStatus.BAD_REQUEST;
                        deferredResult.setResult(new ResponseEntity<>(result.getResponse(), status));
                    }
                } else {
                    deferredResult.setResult(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
                }
            }

            @Override
            public void onFailure(Throwable t) {
                deferredResult.setErrorResult(t);
            }
        }, MoreExecutors.directExecutor());
        return deferredResult;
    }

    @ApiOperation(value = "Reclaim device (reClaimDevice)",
            notes = "Reclaiming means the device will be unassigned from the customer and the device will be available for claiming again."
                    + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customer/device/{deviceName}/claim", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    /**
     * 方法说明：
     * 1. 职责：执行 `reClaimDevice` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public DeferredResult<ResponseEntity> reClaimDevice(@ApiParam(value = "Unique name of the device which is going to be reclaimed")
                                                        @PathVariable(DEVICE_NAME) String deviceName) throws ThingsboardException {
        checkParameter(DEVICE_NAME, deviceName);
        final DeferredResult<ResponseEntity> deferredResult = new DeferredResult<>();

        SecurityUser user = getCurrentUser();
        TenantId tenantId = user.getTenantId();

        Device device = checkNotNull(deviceService.findDeviceByTenantIdAndName(tenantId, deviceName));
        accessControlService.checkPermission(user, Resource.DEVICE, Operation.CLAIM_DEVICES,
                device.getId(), device);

        ListenableFuture<ReclaimResult> result = tbDeviceService.reclaimDevice(tenantId, device, user);
        Futures.addCallback(result, new FutureCallback<>() {
            @Override
            public void onSuccess(ReclaimResult reclaimResult) {
                deferredResult.setResult(new ResponseEntity(HttpStatus.OK));
            }

            @Override
            public void onFailure(Throwable t) {
                deferredResult.setErrorResult(t);
            }
        }, MoreExecutors.directExecutor());
        return deferredResult;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `getSecretKey` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private String getSecretKey(ClaimRequest claimRequest) {
        String secretKey = claimRequest.getSecretKey();
        if (secretKey != null) {
            return secretKey;
        }
        return DataConstants.DEFAULT_SECRET_KEY;
    }

    @ApiOperation(value = "Assign device to tenant (assignDeviceToTenant)",
            notes = "Creates assignment of the device to tenant. Thereafter tenant will be able to reassign the device to a customer." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/tenant/{tenantId}/device/{deviceId}", method = RequestMethod.POST)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `assignDeviceToTenant` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Device assignDeviceToTenant(@ApiParam(value = TENANT_ID_PARAM_DESCRIPTION)
                                       @PathVariable(TENANT_ID) String strTenantId,
                                       @ApiParam(value = DEVICE_ID_PARAM_DESCRIPTION)
                                       @PathVariable(DEVICE_ID) String strDeviceId) throws ThingsboardException {
        checkParameter(TENANT_ID, strTenantId);
        checkParameter(DEVICE_ID, strDeviceId);
        DeviceId deviceId = new DeviceId(toUUID(strDeviceId));
        Device device = checkDeviceId(deviceId, Operation.ASSIGN_TO_TENANT);

        TenantId newTenantId = TenantId.fromUUID(toUUID(strTenantId));
        Tenant newTenant = tenantService.findTenantById(newTenantId);
        if (newTenant == null) {
            throw new ThingsboardException("Could not find the specified Tenant!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        return tbDeviceService.assignDeviceToTenant(device, newTenant, getCurrentUser());
    }

    @ApiOperation(value = "Assign device to edge (assignDeviceToEdge)",
            notes = "Creates assignment of an existing device to an instance of The Edge. " +
                    EDGE_ASSIGN_ASYNC_FIRST_STEP_DESCRIPTION +
                    "Second, remote edge service will receive a copy of assignment device " +
                    EDGE_ASSIGN_RECEIVE_STEP_DESCRIPTION +
                    "Third, once device will be delivered to edge service, it's going to be available for usage on remote edge instance." + TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/edge/{edgeId}/device/{deviceId}", method = RequestMethod.POST)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `assignDeviceToEdge` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Device assignDeviceToEdge(@ApiParam(value = EDGE_ID_PARAM_DESCRIPTION)
                                     @PathVariable(EDGE_ID) String strEdgeId,
                                     @ApiParam(value = DEVICE_ID_PARAM_DESCRIPTION)
                                     @PathVariable(DEVICE_ID) String strDeviceId) throws ThingsboardException {
        checkParameter(EDGE_ID, strEdgeId);
        checkParameter(DEVICE_ID, strDeviceId);
        EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
        Edge edge = checkEdgeId(edgeId, Operation.READ);

        DeviceId deviceId = new DeviceId(toUUID(strDeviceId));
        checkDeviceId(deviceId, Operation.READ);

        return tbDeviceService.assignDeviceToEdge(getTenantId(), deviceId, edge, getCurrentUser());
    }

    @ApiOperation(value = "Unassign device from edge (unassignDeviceFromEdge)",
            notes = "Clears assignment of the device to the edge. " +
                    EDGE_UNASSIGN_ASYNC_FIRST_STEP_DESCRIPTION +
                    "Second, remote edge service will receive an 'unassign' command to remove device " +
                    EDGE_UNASSIGN_RECEIVE_STEP_DESCRIPTION +
                    "Third, once 'unassign' command will be delivered to edge service, it's going to remove device locally." + TENANT_AUTHORITY_PARAGRAPH,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/edge/{edgeId}/device/{deviceId}", method = RequestMethod.DELETE)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `unassignDeviceFromEdge` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public Device unassignDeviceFromEdge(@ApiParam(value = EDGE_ID_PARAM_DESCRIPTION)
                                         @PathVariable(EDGE_ID) String strEdgeId,
                                         @ApiParam(value = DEVICE_ID_PARAM_DESCRIPTION)
                                         @PathVariable(DEVICE_ID) String strDeviceId) throws ThingsboardException {
        checkParameter(EDGE_ID, strEdgeId);
        checkParameter(DEVICE_ID, strDeviceId);
        EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
        Edge edge = checkEdgeId(edgeId, Operation.READ);

        DeviceId deviceId = new DeviceId(toUUID(strDeviceId));
        Device device = checkDeviceId(deviceId, Operation.READ);
        return tbDeviceService.unassignDeviceFromEdge(device, edge, getCurrentUser());
    }

    @ApiOperation(value = "Get devices assigned to edge (getEdgeDevices)",
            notes = "Returns a page of devices assigned to edge. " +
                    PAGE_DATA_PARAMETERS + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/edge/{edgeId}/devices", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getEdgeDevices` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public PageData<DeviceInfo> getEdgeDevices(
            @ApiParam(value = EDGE_ID_PARAM_DESCRIPTION, required = true)
            @PathVariable(EDGE_ID) String strEdgeId,
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = DEVICE_TYPE_DESCRIPTION)
            @RequestParam(required = false) String type,
            @ApiParam(value = DEVICE_PROFILE_ID_PARAM_DESCRIPTION)
            @RequestParam(required = false) String deviceProfileId,
            @ApiParam(value = DEVICE_ACTIVE_PARAM_DESCRIPTION)
            @RequestParam(required = false) Boolean active,
            @ApiParam(value = DEVICE_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = DEVICE_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder,
            @ApiParam(value = "Timestamp. Devices with creation time before it won't be queried")
            @RequestParam(required = false) Long startTime,
            @ApiParam(value = "Timestamp. Devices with creation time after it won't be queried")
            @RequestParam(required = false) Long endTime) throws ThingsboardException {
        checkParameter(EDGE_ID, strEdgeId);
        TenantId tenantId = getCurrentUser().getTenantId();
        EdgeId edgeId = new EdgeId(toUUID(strEdgeId));
        checkEdgeId(edgeId, Operation.READ);
        TimePageLink pageLink = createTimePageLink(pageSize, page, textSearch, sortProperty, sortOrder, startTime, endTime);
        DeviceInfoFilter.DeviceInfoFilterBuilder filter = DeviceInfoFilter.builder();
        filter.tenantId(tenantId);
        filter.edgeId(edgeId);
        filter.active(active);
        if (type != null && type.trim().length() > 0) {
            filter.type(type);
        } else if (deviceProfileId != null && deviceProfileId.length() > 0) {
            filter.deviceProfileId(new DeviceProfileId(toUUID(deviceProfileId)));
        }
        return checkNotNull(deviceService.findDeviceInfosByFilter(filter.build(), pageLink));
    }

    @ApiOperation(value = "Count devices by device profile  (countByDeviceProfileAndEmptyOtaPackage)",
            notes = "The platform gives an ability to load OTA (over-the-air) packages to devices. " +
                    "It can be done in two different ways: device scope or device profile scope." +
                    "In the response you will find the number of devices with specified device profile, but without previously defined device scope OTA package. " +
                    "It can be useful when you want to define number of devices that will be affected with future OTA package" + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/devices/count/{otaPackageType}/{deviceProfileId}", method = RequestMethod.GET)
    @ResponseBody
    public Long countByDeviceProfileAndEmptyOtaPackage
            (@ApiParam(value = "OTA package type", allowableValues = "FIRMWARE, SOFTWARE")
             @PathVariable("otaPackageType") String otaPackageType,
             @ApiParam(value = "Device Profile Id. I.g. '784f394c-42b6-435a-983c-b7beff2784f9'")
             @PathVariable("deviceProfileId") String deviceProfileId) throws ThingsboardException {
        checkParameter("OtaPackageType", otaPackageType);
        checkParameter("DeviceProfileId", deviceProfileId);
        return deviceService.countDevicesByTenantIdAndDeviceProfileIdAndEmptyOtaPackage(
                getTenantId(),
                new DeviceProfileId(UUID.fromString(deviceProfileId)),
                OtaPackageType.valueOf(otaPackageType));
    }

    @ApiOperation(value = "Import the bulk of devices (processDevicesBulkImport)",
            notes = "There's an ability to import the bulk of devices using the only .csv file." + TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @PostMapping("/device/bulk_import")
    /**
     * 方法说明：
     * 1. 职责：执行 `processDevicesBulkImport` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public BulkImportResult<Device> processDevicesBulkImport(@RequestBody BulkImportRequest request) throws
            Exception {
        SecurityUser user = getCurrentUser();
        return deviceBulkImportService.processBulkImport(request, user);
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DeviceController` 在 ThingsBoard Application 模块 中承担REST/WebSocket 控制层类型职责，核心目的是承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 核心流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
 * 3. 关键依赖：主要依赖或协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
