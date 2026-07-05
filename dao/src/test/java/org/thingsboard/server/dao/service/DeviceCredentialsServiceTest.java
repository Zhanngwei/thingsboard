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
package org.thingsboard.server.dao.service;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.DeviceCredentialsId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.exception.DataValidationException;

/**
 * 中文说明：
 * 1. 类目的：`DeviceCredentialsServiceTest` 是 ThingsBoard DAO 测试模块 中的DAO 服务测试或服务支撑类型，用于组织 DAO 层测试、共享服务夹具或持久化服务的公共执行流程。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括Spring Test、DAO Service、SQL/NoSQL DAO、缓存、事务管理器和测试容器。
 * 4. 生命周期：在测试套件或服务调用期间创建，负责准备上下文、执行 DAO 调用并清理状态。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Template Method / Service。
 */
@DaoSqlTest
public class DeviceCredentialsServiceTest extends AbstractServiceTest {

    /**
     * 设备凭据，提供当前类调用的业务操作。
     */
    @Autowired
    DeviceCredentialsService deviceCredentialsService;
    /**
     * 设备，提供当前类调用的业务操作。
     */
    @Autowired
    DeviceService deviceService;

    /**
     * 功能：验证设备凭据相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testCreateDeviceCredentials() {
        DeviceCredentials deviceCredentials = new DeviceCredentials();
        Assertions.assertThrows(DataValidationException.class, () -> {
            deviceCredentialsService.updateDeviceCredentials(tenantId, deviceCredentials);
        });
    }

    /**
     * 功能：验证设备凭据相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveDeviceCredentialsWithEmptyDevice() {
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        device.setTenantId(tenantId);
        device = deviceService.saveDevice(device);
        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, device.getId());
        deviceCredentials.setDeviceId(null);
        try {
            Assertions.assertThrows(DataValidationException.class, () -> {
                deviceCredentialsService.updateDeviceCredentials(tenantId, deviceCredentials);
            });
        } finally {
            deviceService.deleteDevice(tenantId, device.getId());
        }
    }

    /**
     * 功能：验证设备凭据相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveDeviceCredentialsWithEmptyCredentialsType() {
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        device.setTenantId(tenantId);
        device = deviceService.saveDevice(device);
        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, device.getId());
        deviceCredentials.setCredentialsType(null);
        try {
            Assertions.assertThrows(DataValidationException.class, () -> {
                deviceCredentialsService.updateDeviceCredentials(tenantId, deviceCredentials);
            });
        } finally {
            deviceService.deleteDevice(tenantId, device.getId());
        }
    }

    /**
     * 功能：验证设备凭据相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveDeviceCredentialsWithEmptyCredentialsId() {
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        device.setTenantId(tenantId);
        device = deviceService.saveDevice(device);
        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, device.getId());
        deviceCredentials.setCredentialsId(null);
        try {
            Assertions.assertThrows(DataValidationException.class, () -> {
                deviceCredentialsService.updateDeviceCredentials(tenantId, deviceCredentials);
            });
        } finally {
            deviceService.deleteDevice(tenantId, device.getId());
        }
    }

    /**
     * 功能：验证设备凭据相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveNonExistentDeviceCredentials() {
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        device.setTenantId(tenantId);
        device = deviceService.saveDevice(device);
        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, device.getId());
        DeviceCredentials newDeviceCredentials = new DeviceCredentials(new DeviceCredentialsId(Uuids.timeBased()));
        newDeviceCredentials.setCreatedTime(deviceCredentials.getCreatedTime());
        newDeviceCredentials.setDeviceId(deviceCredentials.getDeviceId());
        newDeviceCredentials.setCredentialsType(deviceCredentials.getCredentialsType());
        newDeviceCredentials.setCredentialsId(deviceCredentials.getCredentialsId());
        try {
            Assertions.assertThrows(DataValidationException.class, () -> {
                deviceCredentialsService.updateDeviceCredentials(tenantId, newDeviceCredentials);
            });
        } finally {
            deviceService.deleteDevice(tenantId, device.getId());
        }
    }

    /**
     * 功能：验证设备凭据相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveDeviceCredentialsWithNonExistentDevice() {
        Device device = new Device();
        device.setName("My device");
        device.setType("default");
        device.setTenantId(tenantId);
        device = deviceService.saveDevice(device);
        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, device.getId());
        deviceCredentials.setDeviceId(new DeviceId(Uuids.timeBased()));
        try {
            Assertions.assertThrows(DataValidationException.class, () -> {
                deviceCredentialsService.updateDeviceCredentials(tenantId, deviceCredentials);
            });
        } finally {
            deviceService.deleteDevice(tenantId, device.getId());
        }
    }

    /**
     * 功能：验证设备凭据相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindDeviceCredentialsByDeviceId() {
        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("My device");
        device.setType("default");
        Device savedDevice = deviceService.saveDevice(device);
        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, savedDevice.getId());
        Assert.assertEquals(savedDevice.getId(), deviceCredentials.getDeviceId());
        deviceService.deleteDevice(tenantId, savedDevice.getId());
        deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, savedDevice.getId());
        Assert.assertNull(deviceCredentials);
    }

    /**
     * 功能：验证设备凭据相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFindDeviceCredentialsByCredentialsId() {
        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("My device");
        device.setType("default");
        Device savedDevice = deviceService.saveDevice(device);
        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, savedDevice.getId());
        Assert.assertEquals(savedDevice.getId(), deviceCredentials.getDeviceId());
        DeviceCredentials foundDeviceCredentials = deviceCredentialsService.findDeviceCredentialsByCredentialsId(deviceCredentials.getCredentialsId());
        Assert.assertEquals(deviceCredentials, foundDeviceCredentials);
        deviceService.deleteDevice(tenantId, savedDevice.getId());
        foundDeviceCredentials = deviceCredentialsService.findDeviceCredentialsByCredentialsId(deviceCredentials.getCredentialsId());
        Assert.assertNull(foundDeviceCredentials);
    }

    /**
     * 功能：验证设备凭据相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testSaveDeviceCredentials() {
        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName("My device");
        device.setType("default");
        Device savedDevice = deviceService.saveDevice(device);
        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, savedDevice.getId());
        Assert.assertEquals(savedDevice.getId(), deviceCredentials.getDeviceId());
        deviceCredentials.setCredentialsType(DeviceCredentialsType.ACCESS_TOKEN);
        deviceCredentials.setCredentialsId("access_token");
        deviceCredentialsService.updateDeviceCredentials(tenantId, deviceCredentials);
        DeviceCredentials foundDeviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, savedDevice.getId());
        Assert.assertEquals(deviceCredentials, foundDeviceCredentials);
        deviceService.deleteDevice(tenantId, savedDevice.getId());
    }
}


/*
 * 本类总结：
 * 1. 核心职责：`DeviceCredentialsServiceTest` 在 ThingsBoard DAO 测试模块 中承担DAO 服务测试或服务支撑类型职责，核心目的是组织 DAO 层测试、共享服务夹具或持久化服务的公共执行流程。
 * 2. 核心流程：初始化测试或服务依赖，执行 DAO 契约调用，最后校验数据库、缓存或事件状态。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Test、DAO Service、SQL/NoSQL DAO、缓存、事务管理器和测试容器。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
