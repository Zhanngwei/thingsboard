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
package org.thingsboard.server.dao.device;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceIdInfo;
import org.thingsboard.server.common.data.DeviceInfo;
import org.thingsboard.server.common.data.DeviceInfoFilter;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.device.DeviceSearchQuery;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.dao.device.provision.ProvisionRequest;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.List;
import java.util.UUID;

/**
 * 中文说明：
 * 1. 类目的：`DeviceService` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
public interface DeviceService extends EntityDaoService {

    /**
     * 功能：获取设备ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * 返回：处理结果。
     */
    DeviceInfo findDeviceInfoById(TenantId tenantId, DeviceId deviceId);

    /**
     * 功能：获取设备ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * 返回：处理结果。
     */
    Device findDeviceById(TenantId tenantId, DeviceId deviceId);

    /**
     * 功能：获取设备ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<Device> findDeviceByIdAsync(TenantId tenantId, DeviceId deviceId);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `name`：名称。
     * 返回：处理结果。
     */
    Device findDeviceByTenantIdAndName(TenantId tenantId, String name);

    /**
     * 功能：保存或创建设备。
     * 参数：
     * - `device`：设备信息或设备标识。
     * - `doValidate`：`doValidate` 参数。
     * 返回：处理结果。
     */
    Device saveDevice(Device device, boolean doValidate);

    /**
     * 功能：保存或创建设备。
     * 参数：
     * - `device`：设备信息或设备标识。
     * 返回：处理结果。
     */
    Device saveDevice(Device device);

    /**
     * 功能：保存或创建设备。
     * 参数：
     * - `device`：设备信息或设备标识。
     * - `accessToken`：`accessToken` 参数。
     * 返回：处理结果。
     */
    Device saveDeviceWithAccessToken(Device device, String accessToken);

    /**
     * 功能：保存或创建设备凭据。
     * 参数：
     * - `device`：设备信息或设备标识。
     * - `deviceCredentials`：设备信息或设备标识。
     * 返回：处理结果。
     */
    Device saveDeviceWithCredentials(Device device, DeviceCredentials deviceCredentials);

    /**
     * 功能：保存或创建设备。
     * 参数：
     * - `provisionRequest`：请求对象。
     * - `profile`：`profile` 参数。
     * 返回：处理结果。
     */
    Device saveDevice(ProvisionRequest provisionRequest, DeviceProfile profile);

    /**
     * 功能：执行 `assignDeviceToCustomer` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * - `customerId`：客户IDID。
     * 返回：处理结果。
     */
    Device assignDeviceToCustomer(TenantId tenantId, DeviceId deviceId, CustomerId customerId);

    /**
     * 功能：执行 `unassignDeviceFromCustomer` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * 返回：处理结果。
     */
    Device unassignDeviceFromCustomer(TenantId tenantId, DeviceId deviceId);

    /**
     * 功能：删除或清理设备。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * 返回：无。
     */
    void deleteDevice(TenantId tenantId, DeviceId deviceId);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<Device> findDevicesByTenantId(TenantId tenantId, PageLink pageLink);

    /**
     * 功能：获取设备。
     * 参数：
     * - `filter`：`filter` 参数。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<DeviceInfo> findDeviceInfosByFilter(DeviceInfoFilter filter, PageLink pageLink);

    /**
     * 功能：获取设备ID。
     * 参数：
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<DeviceIdInfo> findDeviceIdInfos(PageLink pageLink);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `type`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<Device> findDevicesByTenantIdAndType(TenantId tenantId, String type, PageLink pageLink);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceProfileId`：设备配置ID。
     * - `type`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<Device> findDevicesByTenantIdAndTypeAndEmptyOtaPackage(TenantId tenantId, DeviceProfileId deviceProfileId, OtaPackageType type, PageLink pageLink);

    /**
     * 功能：统计设备配置数量。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceProfileId`：设备配置ID。
     * - `otaPackageType`：类型。
     * 返回：数值结果。
     */
    Long countDevicesByTenantIdAndDeviceProfileIdAndEmptyOtaPackage(TenantId tenantId, DeviceProfileId deviceProfileId, OtaPackageType otaPackageType);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceIds`：设备信息或设备标识。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<List<Device>> findDevicesByTenantIdAndIdsAsync(TenantId tenantId, List<DeviceId> deviceIds);

    /**
     * 功能：获取`Devices By Ids`。
     * 参数：
     * - `deviceIds`：设备信息或设备标识。
     * 返回：匹配的数据集合。
     */
    List<Device> findDevicesByIds(List<DeviceId> deviceIds);

    /**
     * 功能：获取`Devices By Ids Async`。
     * 参数：
     * - `deviceIds`：设备信息或设备标识。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<List<Device>> findDevicesByIdsAsync(List<DeviceId> deviceIds);

    /**
     * 功能：删除或清理租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：无。
     */
    void deleteDevicesByTenantId(TenantId tenantId);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<Device> findDevicesByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `type`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<Device> findDevicesByTenantIdAndCustomerIdAndType(TenantId tenantId, CustomerId customerId, String type, PageLink pageLink);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * - `deviceIds`：设备信息或设备标识。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<List<Device>> findDevicesByTenantIdCustomerIdAndIdsAsync(TenantId tenantId, CustomerId customerId, List<DeviceId> deviceIds);

    /**
     * 功能：执行 `unassignCustomerDevices` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `customerId`：客户IDID。
     * 返回：无。
     */
    void unassignCustomerDevices(TenantId tenantId, CustomerId customerId);

    /**
     * 功能：获取查询条件。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `query`：`query` 参数。
     * 返回：匹配的数据集合。
     */
    ListenableFuture<List<Device>> findDevicesByQuery(TenantId tenantId, DeviceSearchQuery query);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * 返回：匹配的数据集合。
     */
    @Deprecated(since = "3.6.2", forRemoval = true)
    ListenableFuture<List<EntitySubtype>> findDeviceTypesByTenantId(TenantId tenantId);

    /**
     * 功能：执行 `assignDeviceToTenant` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `device`：设备信息或设备标识。
     * 返回：处理结果。
     */
    Device assignDeviceToTenant(TenantId tenantId, Device device);

    /**
     * 功能：获取设备配置。
     * 参数：
     * - `transportType`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<UUID> findDevicesIdsByDeviceProfileTransportType(DeviceTransportType transportType, PageLink pageLink);

    /**
     * 功能：执行 `assignDeviceToEdge` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * - `edgeId`：边缘节点ID。
     * 返回：处理结果。
     */
    Device assignDeviceToEdge(TenantId tenantId, DeviceId deviceId, EdgeId edgeId);

    /**
     * 功能：执行 `unassignDeviceFromEdge` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `deviceId`：设备IDID。
     * - `edgeId`：边缘节点ID。
     * 返回：处理结果。
     */
    Device unassignDeviceFromEdge(TenantId tenantId, DeviceId deviceId, EdgeId edgeId);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<Device> findDevicesByTenantIdAndEdgeId(TenantId tenantId, EdgeId edgeId, PageLink pageLink);

    /**
     * 功能：获取租户ID。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `edgeId`：边缘节点ID。
     * - `type`：类型。
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    PageData<Device> findDevicesByTenantIdAndEdgeIdAndType(TenantId tenantId, EdgeId edgeId, String type, PageLink pageLink);

}

/*
 * 本类总结：
 * 1. 核心职责：`DeviceService` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
