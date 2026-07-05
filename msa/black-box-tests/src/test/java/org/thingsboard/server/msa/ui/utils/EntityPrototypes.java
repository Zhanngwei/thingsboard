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
package org.thingsboard.server.msa.ui.utils;

import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.device.profile.DisabledDeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.security.Authority;

/**
 * 中文说明：
 * 1. 类目的：`EntityPrototypes` 是 ThingsBoard MSA 测试模块 中的微服务测试和部署支撑类型，用于支撑微服务部署、黑盒测试、UI 自动化、协议连通性验证或版本控制执行器路由。
 * 2. 所属模块：位于 msa 聚合模块，服务于 ThingsBoard 的运维监控、微服务测试或 MQTT 客户端协议边界。
 * 3. 协作对象：主要协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
 * 4. 生命周期：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁。
 * 5. 设计原因：单独建模该类型可以隔离协议细节、测试编排、页面操作和运行时探测逻辑，避免业务模块直接耦合外部工具或网络状态机。
 * 6. 事务与缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理。
 * 7. MQTT/Actor/Rule Engine：是否直接涉及 MQTT 取决于模块；监控和 MSA 可能通过协议入口间接触发 Actor 与 Rule Engine，netty-mqtt 则直接管理 MQTT 会话。
 * 8. 设计模式：主要体现 Test Fixture / Page Object / Service。
 */
public class EntityPrototypes {

    /**
     * 功能：执行 `defaultCustomerPrototype` 对应的处理。
     * 参数：
     * - `entityName`：实体对象。
     * 返回：处理结果。
     */
    public static Customer defaultCustomerPrototype(String entityName) {
        Customer customer = new Customer();
        customer.setTitle(entityName);
        return customer;
    }

    /**
     * 功能：执行 `defaultCustomerPrototype` 对应的处理。
     * 参数：
     * - `entityName`：实体对象。
     * - `description`：`description` 参数。
     * 返回：处理结果。
     */
    public static Customer defaultCustomerPrototype(String entityName, String description) {
        Customer customer = new Customer();
        customer.setTitle(entityName);
        customer.setAdditionalInfo(JacksonUtil.newObjectNode().put("description", description));
        return customer;
    }

    /**
     * 功能：执行 `defaultCustomerPrototype` 对应的处理。
     * 参数：
     * - `entityName`：实体对象。
     * - `number`：`number` 参数。
     * 返回：处理结果。
     */
    public static Customer defaultCustomerPrototype(String entityName, int number) {
        Customer customer = new Customer();
        customer.setTitle(entityName);
        customer.setPhone("+1" + number);
        return customer;
    }

    /**
     * 功能：执行 `defaultRuleChainPrototype` 对应的处理。
     * 参数：
     * - `entityName`：实体对象。
     * 返回：处理结果。
     */
    public static RuleChain defaultRuleChainPrototype(String entityName) {
        RuleChain ruleChain = new RuleChain();
        ruleChain.setName(entityName);
        return ruleChain;
    }

    /**
     * 功能：执行 `defaultRuleChainPrototype` 对应的处理。
     * 参数：
     * - `entityName`：实体对象。
     * - `description`：`description` 参数。
     * 返回：处理结果。
     */
    public static RuleChain defaultRuleChainPrototype(String entityName, String description) {
        RuleChain ruleChain = new RuleChain();
        ruleChain.setName(entityName);
        ruleChain.setAdditionalInfo(JacksonUtil.newObjectNode().put("description", description));
        return ruleChain;
    }

    /**
     * 功能：执行 `defaultRuleChainPrototype` 对应的处理。
     * 参数：
     * - `entityName`：实体对象。
     * - `debugMode`：`debugMode` 参数。
     * 返回：处理结果。
     */
    public static RuleChain defaultRuleChainPrototype(String entityName, boolean debugMode) {
        RuleChain ruleChain = new RuleChain();
        ruleChain.setName(entityName);
        ruleChain.setDebugMode(debugMode);
        return ruleChain;
    }

    /**
     * 功能：执行 `defaultDeviceProfile` 对应的处理。
     * 参数：
     * - `entityName`：实体对象。
     * 返回：处理结果。
     */
    public static DeviceProfile defaultDeviceProfile(String entityName) {
        DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setName(entityName);
        deviceProfile.setType(DeviceProfileType.DEFAULT);
        deviceProfile.setTransportType(DeviceTransportType.DEFAULT);
        deviceProfile.setProvisionType(DeviceProfileProvisionType.DISABLED);
        DeviceProfileData deviceProfileData = new DeviceProfileData();
        deviceProfileData.setConfiguration(new DefaultDeviceProfileConfiguration());
        deviceProfileData.setProvisionConfiguration(new DisabledDeviceProfileProvisionConfiguration(null));
        deviceProfileData.setTransportConfiguration(new DefaultDeviceProfileTransportConfiguration());
        deviceProfile.setProfileData(deviceProfileData);
        return deviceProfile;
    }

    /**
     * 功能：执行 `defaultDeviceProfile` 对应的处理。
     * 参数：
     * - `entityName`：实体对象。
     * - `description`：`description` 参数。
     * 返回：处理结果。
     */
    public static DeviceProfile defaultDeviceProfile(String entityName, String description) {
        DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setName(entityName);
        deviceProfile.setDescription(description);
        deviceProfile.setType(DeviceProfileType.DEFAULT);
        deviceProfile.setTransportType(DeviceTransportType.DEFAULT);
        deviceProfile.setProvisionType(DeviceProfileProvisionType.DISABLED);
        DeviceProfileData deviceProfileData = new DeviceProfileData();
        deviceProfileData.setConfiguration(new DefaultDeviceProfileConfiguration());
        deviceProfileData.setProvisionConfiguration(new DisabledDeviceProfileProvisionConfiguration(null));
        deviceProfileData.setTransportConfiguration(new DefaultDeviceProfileTransportConfiguration());
        deviceProfile.setProfileData(deviceProfileData);
        return deviceProfile;
    }

    /**
     * 功能：执行 `defaultAssetProfile` 对应的处理。
     * 参数：
     * - `entityName`：实体对象。
     * 返回：匹配的数据集合。
     */
    public static AssetProfile defaultAssetProfile(String entityName) {
        AssetProfile assetProfile = new AssetProfile();
        assetProfile.setName(entityName);
        return assetProfile;
    }

    /**
     * 功能：执行 `defaultAssetProfile` 对应的处理。
     * 参数：
     * - `entityName`：实体对象。
     * - `description`：`description` 参数。
     * 返回：匹配的数据集合。
     */
    public static AssetProfile defaultAssetProfile(String entityName, String description) {
        AssetProfile assetProfile = new AssetProfile();
        assetProfile.setName(entityName);
        assetProfile.setDescription(description);
        return assetProfile;
    }

    /**
     * 功能：执行 `defaultAlarm` 对应的处理。
     * 参数：
     * - `id`：`id`ID。
     * - `type`：类型。
     * 返回：处理结果。
     */
    public static Alarm defaultAlarm(EntityId id, String type) {
        Alarm alarm = new Alarm();
        alarm.setType(type);
        alarm.setOriginator(id);
        alarm.setSeverity(AlarmSeverity.CRITICAL);
        return alarm;
    }

    /**
     * 功能：执行 `defaultAlarm` 对应的处理。
     * 参数：
     * - `id`：`id`ID。
     * - `type`：类型。
     * - `propagate`：`propagate` 参数。
     * 返回：处理结果。
     */
    public static Alarm defaultAlarm(EntityId id, String type, boolean propagate) {
        Alarm alarm = new Alarm();
        alarm.setType(type);
        alarm.setOriginator(id);
        alarm.setSeverity(AlarmSeverity.CRITICAL);
        alarm.setPropagate(propagate);
        return alarm;
    }

    /**
     * 功能：执行 `defaultAlarm` 对应的处理。
     * 参数：
     * - `id`：`id`ID。
     * - `type`：类型。
     * - `userId`：用户ID。
     * 返回：处理结果。
     */
    public static Alarm defaultAlarm(EntityId id, String type, UserId userId) {
        Alarm alarm = new Alarm();
        alarm.setType(type);
        alarm.setOriginator(id);
        alarm.setSeverity(AlarmSeverity.CRITICAL);
        alarm.setAssigneeId(userId);
        return alarm;
    }

    /**
     * 功能：执行 `defaultAlarm` 对应的处理。
     * 参数：
     * - `id`：`id`ID。
     * - `type`：类型。
     * - `userId`：用户ID。
     * - `propagate`：`propagate` 参数。
     * 返回：处理结果。
     */
    public static Alarm defaultAlarm(EntityId id, String type, UserId userId, boolean propagate) {
        Alarm alarm = new Alarm();
        alarm.setType(type);
        alarm.setOriginator(id);
        alarm.setSeverity(AlarmSeverity.CRITICAL);
        alarm.setAssigneeId(userId);
        alarm.setPropagate(propagate);
        return alarm;
    }

    /**
     * 功能：执行 `defaultUser` 对应的处理。
     * 参数：
     * - `email`：`email` 参数。
     * - `customerId`：客户IDID。
     * 返回：处理结果。
     */
    public static User defaultUser(String email, CustomerId customerId) {
        User user = new User();
        user.setEmail(email);
        user.setCustomerId(customerId);
        user.setAuthority(Authority.CUSTOMER_USER);
        return user;
    }

    /**
     * 功能：执行 `defaultUser` 对应的处理。
     * 参数：
     * - `email`：`email` 参数。
     * - `customerId`：客户IDID。
     * - `name`：名称。
     * 返回：处理结果。
     */
    public static User defaultUser(String email, CustomerId customerId, String name) {
        User user = new User();
        user.setEmail(email);
        user.setFirstName(name);
        user.setCustomerId(customerId);
        user.setAuthority(Authority.CUSTOMER_USER);
        return user;
    }

    /**
     * 功能：执行 `defaultDevicePrototype` 对应的处理。
     * 参数：
     * - `name`：名称。
     * 返回：处理结果。
     */
    public static Device defaultDevicePrototype(String name) {
        Device device = new Device();
        device.setName(name + RandomStringUtils.randomAlphanumeric(7));
        device.setType("DEFAULT");
        return device;
    }

    /**
     * 功能：执行 `defaultDevicePrototype` 对应的处理。
     * 参数：
     * - `name`：名称。
     * - `id`：`id`ID。
     * 返回：处理结果。
     */
    public static Device defaultDevicePrototype(String name, CustomerId id) {
        Device device = new Device();
        device.setName(name + RandomStringUtils.randomAlphanumeric(7));
        device.setCustomerId(id);
        device.setType("DEFAULT");
        return device;
    }

    /**
     * 功能：执行 `defaultDevicePrototype` 对应的处理。
     * 参数：
     * - `name`：名称。
     * - `description`：`description` 参数。
     * 返回：处理结果。
     */
    public static Device defaultDevicePrototype(String name, String description) {
        Device device = new Device();
        device.setName(name + RandomStringUtils.randomAlphanumeric(7));
        device.setType("DEFAULT");
        device.setAdditionalInfo(JacksonUtil.newObjectNode().put("description", description));
        return device;
    }

    /**
     * 功能：执行 `defaultDevicePrototype` 对应的处理。
     * 参数：
     * - `name`：名称。
     * - `description`：`description` 参数。
     * - `label`：`label` 参数。
     * 返回：处理结果。
     */
    public static Device defaultDevicePrototype(String name, String description, String label) {
        Device device = new Device();
        device.setName(name + RandomStringUtils.randomAlphanumeric(7));
        device.setType("DEFAULT");
        device.setAdditionalInfo(JacksonUtil.newObjectNode().put("description", description));
        device.setLabel(label);
        return device;
    }

    /**
     * 功能：执行 `defaultDevicePrototype` 对应的处理。
     * 参数：
     * - `name`：名称。
     * - `gateway`：`gateway` 参数。
     * 返回：处理结果。
     */
    public static Device defaultDevicePrototype(String name, boolean gateway) {
        Device device = new Device();
        device.setName(name + RandomStringUtils.randomAlphanumeric(7));
        device.setType("DEFAULT");
        device.setAdditionalInfo(JacksonUtil.newObjectNode().put("gateway", gateway));
        return device;
    }

    /**
     * 功能：执行 `defaultDevicePrototype` 对应的处理。
     * 参数：
     * - `name`：名称。
     * - `gateway`：`gateway` 参数。
     * - `overwriteActivityTime`：`overwriteActivityTime` 参数。
     * 返回：处理结果。
     */
    public static Device defaultDevicePrototype(String name, boolean gateway, boolean overwriteActivityTime) {
        Device device = new Device();
        device.setName(name + RandomStringUtils.randomAlphanumeric(7));
        device.setType("DEFAULT");
        device.setAdditionalInfo(JacksonUtil.newObjectNode()
                .put("gateway", gateway)
                .put("overwriteActivityTime", overwriteActivityTime));
        return device;
    }

    /**
     * 功能：执行 `defaultDevicePrototype` 对应的处理。
     * 参数：
     * - `name`：名称。
     * - `deviceProfileId`：设备配置ID。
     * 返回：处理结果。
     */
    public static Device defaultDevicePrototype(String name, DeviceProfileId deviceProfileId) {
        Device device = new Device();
        device.setName(name + RandomStringUtils.randomAlphanumeric(7));
        device.setType("DEFAULT");
        device.setDeviceProfileId(deviceProfileId);
        return device;
    }

    /**
     * 功能：执行 `defaultAssetPrototype` 对应的处理。
     * 参数：
     * - `name`：名称。
     * - `id`：`id`ID。
     * 返回：匹配的数据集合。
     */
    public static Asset defaultAssetPrototype(String name, CustomerId id) {
        Asset asset = new Asset();
        asset.setName(name + RandomStringUtils.randomAlphanumeric(7));
        asset.setCustomerId(id);
        asset.setType("DEFAULT");
        return asset;
    }

    /**
     * 功能：执行 `defaultEntityViewPrototype` 对应的处理。
     * 参数：
     * - `name`：名称。
     * - `type`：类型。
     * - `entityType`：实体对象。
     * 返回：处理结果。
     */
    public static EntityView defaultEntityViewPrototype(String name, String type, String entityType) {
        EntityView entityView = new EntityView();
        entityView.setName(name + RandomStringUtils.randomAlphanumeric(7));
        entityView.setType(type + RandomStringUtils.randomAlphanumeric(7));
        entityView.setAdditionalInfo(JacksonUtil.newObjectNode().put("entityType", entityType));
        return entityView;
    }

    /**
     * 功能：执行 `defaultDashboardPrototype` 对应的处理。
     * 参数：
     * - `title`：`title` 参数。
     * 返回：处理结果。
     */
    public static Dashboard defaultDashboardPrototype(String title) {
        Dashboard dashboard = new Dashboard();
        dashboard.setTitle(title + RandomStringUtils.randomAlphanumeric(7));
        return dashboard;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`EntityPrototypes` 在 ThingsBoard MSA 测试模块 中承担微服务测试和部署支撑类型职责，核心目的是支撑微服务部署、黑盒测试、UI 自动化、协议连通性验证或版本控制执行器路由。
 * 2. 核心流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
 * 3. 关键依赖：主要依赖或协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
 * 4. 学习重点：阅读本文件时应关注连接生命周期、异步回调、协议状态、测试环境、线程安全边界，以及它与 MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
