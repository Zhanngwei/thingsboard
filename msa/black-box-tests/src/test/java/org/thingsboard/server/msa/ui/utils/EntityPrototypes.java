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
 * 1. `EntityPrototypes` 是 ThingsBoard Microservices 中围绕实体提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 它直接协作于构造参数、字段类型和公开方法涉及的对象。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
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
