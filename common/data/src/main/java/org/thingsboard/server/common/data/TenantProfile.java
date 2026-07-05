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
package org.thingsboard.server.common.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;
import org.thingsboard.server.common.data.validation.Length;
import org.thingsboard.server.common.data.validation.NoXss;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;

/**
 * 中文说明：
 * 1. 类目的：`TenantProfile` 是ThingsBoard Common 模块中的公共数据模型类型，用于承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 生命周期：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Value Object / Builder。
 */
@ApiModel
@Data
@ToString(exclude = {"profileDataBytes"})
@EqualsAndHashCode(callSuper = true)
@Slf4j
public class TenantProfile extends BaseData<TenantProfileId> implements HasName {

    /**
     * 版本号常量，用于统一引用固定值。
     */
    private static final long serialVersionUID = 3021989561267192281L;

    public static final ObjectMapper mapper = new ObjectMapper();

    /**
     * 名称，用于标识或展示当前对象。
     */
    @NoXss
    @Length(fieldName = "name")
    @ApiModelProperty(position = 3, value = "Name of the tenant profile", example = "High Priority Tenants")
    private String name;
    /**
     * 描述信息，用于展示或标识当前对象。
     */
    @NoXss
    @ApiModelProperty(position = 4, value = "Description of the tenant profile", example = "Any text")
    private String description;
    /**
     * 当前对象是否为默认项。
     */
    @ApiModelProperty(position = 5, value = "Default Tenant profile to be used.", example = "true")
    private boolean isDefault;
    /**
     * 是否满足规则引擎条件。
     */
    @ApiModelProperty(position = 6, value = "If enabled, will push all messages related to this tenant and processed by the rule engine into separate queue. " +
            "Useful for complex microservices deployments, to isolate processing of the data for specific tenants", example = "true")
    private boolean isolatedTbRuleEngine;
    /**
     * 配置，保存当前步骤读取或计算得到的内容。
     */
    @ApiModelProperty(position = 7, value = "Complex JSON object that contains profile settings: queue configs, max devices, max assets, rate limits, etc.")
    private transient TenantProfileData profileData;
    /**
     * 配置列表，用于保存一组待处理对象。
     */
    @JsonIgnore
    private byte[] profileDataBytes;

    /**
     * 功能：创建 `TenantProfile` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public TenantProfile() {
        super();
    }

    /**
     * 功能：创建 `TenantProfile` 实例，并初始化必要字段。
     * 参数：
     * - `tenantProfileId`：租户IDID。
     * 返回：新创建的对象实例。
     */
    public TenantProfile(TenantProfileId tenantProfileId) {
        super(tenantProfileId);
    }

    /**
     * 功能：创建 `TenantProfile` 实例，并初始化必要字段。
     * 参数：
     * - `tenantProfile`：租户信息或租户标识。
     * 返回：新创建的对象实例。
     */
    public TenantProfile(TenantProfile tenantProfile) {
        super(tenantProfile);
        this.name = tenantProfile.getName();
        this.description = tenantProfile.getDescription();
        this.isDefault = tenantProfile.isDefault();
        this.isolatedTbRuleEngine = tenantProfile.isIsolatedTbRuleEngine();
        this.setProfileData(tenantProfile.getProfileData());
    }

    /**
     * 功能：获取`Id`。
     * 参数：无。
     * 返回：处理结果。
     */
    @ApiModelProperty(position = 1, value = "JSON object with the tenant profile Id. " +
            "Specify this field to update the tenant profile. " +
            "Referencing non-existing tenant profile Id will cause error. " +
            "Omit this field to create new tenant profile.")
    @Override
    public TenantProfileId getId() {
        return super.getId();
    }

    /**
     * 功能：获取创建时间。
     * 参数：无。
     * 返回：数值结果。
     */
    @ApiModelProperty(position = 2, value = "Timestamp of the tenant profile creation, in milliseconds", example = "1609459200000", accessMode = ApiModelProperty.AccessMode.READ_ONLY)
    @Override
    public long getCreatedTime() {
        return super.getCreatedTime();
    }

    /**
     * 功能：获取名称。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * 功能：获取配置。
     * 参数：无。
     * 返回：处理结果。
     */
    public TenantProfileData getProfileData() {
        if (profileData != null) {
            return profileData;
        } else {
            if (profileDataBytes != null) {
                try {
                    profileData = mapper.readValue(new ByteArrayInputStream(profileDataBytes), TenantProfileData.class);
                } catch (IOException e) {
                    log.warn("Can't deserialize tenant profile data: ", e);
                    return createDefaultTenantProfileData();
                }
                return profileData;
            } else {
                return createDefaultTenantProfileData();
            }
        }
    }

    /**
     * 功能：获取配置。
     * 参数：无。
     * 返回：可能存在的结果。
     */
    @JsonIgnore
    public Optional<DefaultTenantProfileConfiguration> getProfileConfiguration() {
        return Optional.ofNullable(getProfileData().getConfiguration())
                .filter(profileConfiguration -> profileConfiguration instanceof DefaultTenantProfileConfiguration)
                .map(profileConfiguration -> (DefaultTenantProfileConfiguration) profileConfiguration);
    }

    /**
     * 功能：获取配置。
     * 参数：无。
     * 返回：处理结果。
     */
    @JsonIgnore
    public DefaultTenantProfileConfiguration getDefaultProfileConfiguration() {
        return getProfileConfiguration().orElse(null);
    }

    /**
     * 功能：保存或创建租户。
     * 参数：无。
     * 返回：处理结果。
     */
    public TenantProfileData createDefaultTenantProfileData() {
        TenantProfileData tpd = new TenantProfileData();
        tpd.setConfiguration(new DefaultTenantProfileConfiguration());
        this.profileData = tpd;
        return tpd;
    }

    /**
     * 功能：更新配置。
     * 参数：
     * - `data`：待处理数据。
     * 返回：无。
     */
    public void setProfileData(TenantProfileData data) {
        this.profileData = data;
        try {
            this.profileDataBytes = data != null ? mapper.writeValueAsBytes(data) : null;
        } catch (JsonProcessingException e) {
            log.warn("Can't serialize tenant profile data: ", e);
        }
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`TenantProfile` 在 ThingsBoard Common 模块 中承担公共数据模型类型职责，核心目的是承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 核心流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
 * 3. 关键依赖：主要依赖或协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
