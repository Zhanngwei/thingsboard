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

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileInfo;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.resource.ImageService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.device.profile.TbDeviceProfileService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.List;
import java.util.UUID;

import static org.thingsboard.server.controller.ControllerConstants.DEVICE_PROFILE_DATA;
import static org.thingsboard.server.controller.ControllerConstants.DEVICE_PROFILE_ID;
import static org.thingsboard.server.controller.ControllerConstants.DEVICE_PROFILE_ID_PARAM_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.DEVICE_PROFILE_INFO_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.DEVICE_PROFILE_SORT_PROPERTY_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.DEVICE_PROFILE_TEXT_SEARCH_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.INLINE_IMAGES;
import static org.thingsboard.server.controller.ControllerConstants.INLINE_IMAGES_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.NEW_LINE;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_DATA_PARAMETERS;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_NUMBER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.PAGE_SIZE_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.SORT_ORDER_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.SORT_PROPERTY_DESCRIPTION;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;
import static org.thingsboard.server.controller.ControllerConstants.TRANSPORT_TYPE_ALLOWABLE_VALUES;
import static org.thingsboard.server.controller.ControllerConstants.UUID_WIKI_LINK;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
/**
 * 中文说明：
 * 1. 类目的：`DeviceProfileController` 是ThingsBoard Application 模块中的REST/WebSocket 控制层类型，用于承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 生命周期：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 MVC Controller / Facade。
 */
public class DeviceProfileController extends BaseController {

    /**
     * 字段说明：
     * 1. 保存 `tbDeviceProfileService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final TbDeviceProfileService tbDeviceProfileService;
    private final ImageService imageService;

    @Autowired
    /**
     * 字段说明：
     * 1. 保存 `timeseriesService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private TimeseriesService timeseriesService;

    @ApiOperation(value = "Get Device Profile (getDeviceProfileById)",
            notes = "Fetch the Device Profile object based on the provided Device Profile Id. " +
                    "The server checks that the device profile is owned by the same tenant. " + TENANT_AUTHORITY_PARAGRAPH,
            produces = "application/json")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/deviceProfile/{deviceProfileId}", method = RequestMethod.GET)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getDeviceProfileById` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public DeviceProfile getDeviceProfileById(
            @ApiParam(value = DEVICE_PROFILE_ID_PARAM_DESCRIPTION)
            @PathVariable(DEVICE_PROFILE_ID) String strDeviceProfileId,
            @ApiParam(value = INLINE_IMAGES_DESCRIPTION)
            @RequestParam(value = INLINE_IMAGES, required = false) boolean inlineImages) throws ThingsboardException {
        checkParameter(DEVICE_PROFILE_ID, strDeviceProfileId);
        DeviceProfileId deviceProfileId = new DeviceProfileId(toUUID(strDeviceProfileId));
        var result = checkDeviceProfileId(deviceProfileId, Operation.READ);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (inlineImages) {
            imageService.inlineImage(result);
        }
        return result;
    }

    @ApiOperation(value = "Get Device Profile Info (getDeviceProfileInfoById)",
            notes = "Fetch the Device Profile Info object based on the provided Device Profile Id. "
                    + DEVICE_PROFILE_INFO_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = "application/json")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/deviceProfileInfo/{deviceProfileId}", method = RequestMethod.GET)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getDeviceProfileInfoById` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public DeviceProfileInfo getDeviceProfileInfoById(
            @ApiParam(value = DEVICE_PROFILE_ID_PARAM_DESCRIPTION)
            @PathVariable(DEVICE_PROFILE_ID) String strDeviceProfileId) throws ThingsboardException {
        checkParameter(DEVICE_PROFILE_ID, strDeviceProfileId);
        DeviceProfileId deviceProfileId = new DeviceProfileId(toUUID(strDeviceProfileId));
        return new DeviceProfileInfo(checkDeviceProfileId(deviceProfileId, Operation.READ));
    }

    @ApiOperation(value = "Get Default Device Profile (getDefaultDeviceProfileInfo)",
            notes = "Fetch the Default Device Profile Info object. " +
                    DEVICE_PROFILE_INFO_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = "application/json")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/deviceProfileInfo/default", method = RequestMethod.GET)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getDefaultDeviceProfileInfo` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public DeviceProfileInfo getDefaultDeviceProfileInfo() throws ThingsboardException {
        return checkNotNull(deviceProfileService.findDefaultDeviceProfileInfo(getTenantId()));
    }

    @ApiOperation(value = "Get time-series keys (getTimeseriesKeys)",
            notes = "Get a set of unique time-series keys used by devices that belong to specified profile. " +
                    "If profile is not set returns a list of unique keys among all profiles. " +
                    "The call is used for auto-complete in the UI forms. " +
                    "The implementation limits the number of devices that participate in search to 100 as a trade of between accurate results and time-consuming queries. " +
                    TENANT_AUTHORITY_PARAGRAPH,
            produces = "application/json")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/deviceProfile/devices/keys/timeseries", method = RequestMethod.GET)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getTimeseriesKeys` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public List<String> getTimeseriesKeys(
            @ApiParam(value = DEVICE_PROFILE_ID_PARAM_DESCRIPTION)
            @RequestParam(name = DEVICE_PROFILE_ID, required = false) String deviceProfileIdStr) throws ThingsboardException {
        DeviceProfileId deviceProfileId;
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (StringUtils.isNotEmpty(deviceProfileIdStr)) {
            deviceProfileId = new DeviceProfileId(UUID.fromString(deviceProfileIdStr));
            checkDeviceProfileId(deviceProfileId, Operation.READ);
        } else {
            deviceProfileId = null;
        }

        return timeseriesService.findAllKeysByDeviceProfileId(getTenantId(), deviceProfileId);
    }

    @ApiOperation(value = "Get attribute keys (getAttributesKeys)",
            notes = "Get a set of unique attribute keys used by devices that belong to specified profile. " +
                    "If profile is not set returns a list of unique keys among all profiles. " +
                    "The call is used for auto-complete in the UI forms. " +
                    "The implementation limits the number of devices that participate in search to 100 as a trade of between accurate results and time-consuming queries. " +
                    TENANT_AUTHORITY_PARAGRAPH,
            produces = "application/json")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/deviceProfile/devices/keys/attributes", method = RequestMethod.GET)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getAttributesKeys` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public List<String> getAttributesKeys(
            @ApiParam(value = DEVICE_PROFILE_ID_PARAM_DESCRIPTION)
            @RequestParam(name = DEVICE_PROFILE_ID, required = false) String deviceProfileIdStr) throws ThingsboardException {
        DeviceProfileId deviceProfileId;
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (StringUtils.isNotEmpty(deviceProfileIdStr)) {
            deviceProfileId = new DeviceProfileId(UUID.fromString(deviceProfileIdStr));
            checkDeviceProfileId(deviceProfileId, Operation.READ);
        } else {
            deviceProfileId = null;
        }

        return attributesService.findAllKeysByDeviceProfileId(getTenantId(), deviceProfileId);
    }

    @ApiOperation(value = "Create Or Update Device Profile (saveDeviceProfile)",
            notes = "Create or update the Device Profile. When creating device profile, platform generates device profile id as " + UUID_WIKI_LINK +
                    "The newly created device profile id will be present in the response. " +
                    "Specify existing device profile id to update the device profile. " +
                    "Referencing non-existing device profile Id will cause 'Not Found' error. " + NEW_LINE +
                    "Device profile name is unique in the scope of tenant. Only one 'default' device profile may exist in scope of tenant." + DEVICE_PROFILE_DATA +
                    "Remove 'id', 'tenantId' from the request body example (below) to create new Device Profile entity. " +
                    TENANT_AUTHORITY_PARAGRAPH,
            produces = "application/json",
            consumes = "application/json")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/deviceProfile", method = RequestMethod.POST)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `saveDeviceProfile` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public DeviceProfile saveDeviceProfile(
            @ApiParam(value = "A JSON value representing the device profile.")
            @RequestBody DeviceProfile deviceProfile) throws Exception {
        deviceProfile.setTenantId(getTenantId());
        checkEntity(deviceProfile.getId(), deviceProfile, Resource.DEVICE_PROFILE);
        return tbDeviceProfileService.save(deviceProfile, getCurrentUser());
    }

    @ApiOperation(value = "Delete device profile (deleteDeviceProfile)",
            notes = "Deletes the device profile. Referencing non-existing device profile Id will cause an error. " +
                    "Can't delete the device profile if it is referenced by existing devices." + TENANT_AUTHORITY_PARAGRAPH,
            produces = "application/json")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/deviceProfile/{deviceProfileId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    /**
     * 方法说明：
     * 1. 职责：执行 `deleteDeviceProfile` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void deleteDeviceProfile(
            @ApiParam(value = DEVICE_PROFILE_ID_PARAM_DESCRIPTION)
            @PathVariable(DEVICE_PROFILE_ID) String strDeviceProfileId) throws ThingsboardException {
        checkParameter(DEVICE_PROFILE_ID, strDeviceProfileId);
        DeviceProfileId deviceProfileId = new DeviceProfileId(toUUID(strDeviceProfileId));
        DeviceProfile deviceProfile = checkDeviceProfileId(deviceProfileId, Operation.DELETE);
        tbDeviceProfileService.delete(deviceProfile, getCurrentUser());
    }

    @ApiOperation(value = "Make Device Profile Default (setDefaultDeviceProfile)",
            notes = "Marks device profile as default within a tenant scope." + TENANT_AUTHORITY_PARAGRAPH,
            produces = "application/json")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/deviceProfile/{deviceProfileId}/default", method = RequestMethod.POST)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `setDefaultDeviceProfile` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public DeviceProfile setDefaultDeviceProfile(
            @ApiParam(value = DEVICE_PROFILE_ID_PARAM_DESCRIPTION)
            @PathVariable(DEVICE_PROFILE_ID) String strDeviceProfileId) throws ThingsboardException {
        checkParameter(DEVICE_PROFILE_ID, strDeviceProfileId);
        DeviceProfileId deviceProfileId = new DeviceProfileId(toUUID(strDeviceProfileId));
        DeviceProfile deviceProfile = checkDeviceProfileId(deviceProfileId, Operation.WRITE);
        DeviceProfile previousDefaultDeviceProfile = deviceProfileService.findDefaultDeviceProfile(getTenantId());
        return tbDeviceProfileService.setDefaultDeviceProfile(deviceProfile, previousDefaultDeviceProfile, getCurrentUser());
    }

    @ApiOperation(value = "Get Device Profiles (getDeviceProfiles)",
            notes = "Returns a page of devices profile objects owned by tenant. " +
                    PAGE_DATA_PARAMETERS + TENANT_AUTHORITY_PARAGRAPH,
            produces = "application/json")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/deviceProfiles", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getDeviceProfiles` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public PageData<DeviceProfile> getDeviceProfiles(
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = DEVICE_PROFILE_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = DEVICE_PROFILE_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        return checkNotNull(deviceProfileService.findDeviceProfiles(getTenantId(), pageLink));
    }

    @ApiOperation(value = "Get Device Profiles for transport type (getDeviceProfileInfos)",
            notes = "Returns a page of devices profile info objects owned by tenant. " +
                    PAGE_DATA_PARAMETERS + DEVICE_PROFILE_INFO_DESCRIPTION + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH,
            produces = "application/json")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/deviceProfileInfos", params = {"pageSize", "page"}, method = RequestMethod.GET)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getDeviceProfileInfos` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public PageData<DeviceProfileInfo> getDeviceProfileInfos(
            @ApiParam(value = PAGE_SIZE_DESCRIPTION, required = true)
            @RequestParam int pageSize,
            @ApiParam(value = PAGE_NUMBER_DESCRIPTION, required = true)
            @RequestParam int page,
            @ApiParam(value = DEVICE_PROFILE_TEXT_SEARCH_DESCRIPTION)
            @RequestParam(required = false) String textSearch,
            @ApiParam(value = SORT_PROPERTY_DESCRIPTION, allowableValues = DEVICE_PROFILE_SORT_PROPERTY_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortProperty,
            @ApiParam(value = SORT_ORDER_DESCRIPTION, allowableValues = SORT_ORDER_ALLOWABLE_VALUES)
            @RequestParam(required = false) String sortOrder,
            @ApiParam(value = "Type of the transport", allowableValues = TRANSPORT_TYPE_ALLOWABLE_VALUES)
            @RequestParam(required = false) String transportType) throws ThingsboardException {
        PageLink pageLink = createPageLink(pageSize, page, textSearch, sortProperty, sortOrder);
        // 传输层调用会影响设备会话或协议响应，需要与消息确认语义保持一致。
        return checkNotNull(deviceProfileService.findDeviceProfileInfos(getTenantId(), pageLink, transportType));
    }

    @ApiOperation(value = "Get Device Profile names (getDeviceProfileNames)",
            notes = "Returns a set of unique device profile names owned by the tenant."
                    + TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/deviceProfile/names", method = RequestMethod.GET)
    @ResponseBody
    /**
     * 方法说明：
     * 1. 职责：执行 `getDeviceProfileNames` 对应的REST/WebSocket 控制层类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public List<EntityInfo> getDeviceProfileNames(
            @ApiParam(value = "Flag indicating whether to retrieve exclusively the names of device profiles that are referenced by tenant's devices.")
            @RequestParam(value = "activeOnly", required = false, defaultValue = "false") boolean activeOnly) throws ThingsboardException {
        SecurityUser user = getCurrentUser();
        TenantId tenantId = user.getTenantId();
        return checkNotNull(deviceProfileService.findDeviceProfileNamesByTenantId(tenantId, activeOnly));
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DeviceProfileController` 在 ThingsBoard Application 模块 中承担REST/WebSocket 控制层类型职责，核心目的是承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 核心流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
 * 3. 关键依赖：主要依赖或协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
