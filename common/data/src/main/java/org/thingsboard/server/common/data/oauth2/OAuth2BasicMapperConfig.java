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
package org.thingsboard.server.common.data.oauth2;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.thingsboard.server.common.data.validation.Length;

/**
 * 中文说明：
 * 1. 类目的：`OAuth2BasicMapperConfig` 是ThingsBoard Common 模块中的公共数据模型类型，用于承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 生命周期：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Value Object / Builder。
 */
@Builder(toBuilder = true)
@EqualsAndHashCode
@Data
@ToString
@ApiModel
public class OAuth2BasicMapperConfig {
    /**
     * 属性，用于定位映射、配置或数据项。
     */
    @Length(fieldName = "emailAttributeKey", max = 31)
    @ApiModelProperty(value = "Email attribute key of OAuth2 principal attributes. " +
            "Must be specified for BASIC mapper type and cannot be specified for GITHUB type")
    private final String emailAttributeKey;
    /**
     * 属性，用于定位映射、配置或数据项。
     */
    @Length(fieldName = "firstNameAttributeKey", max = 31)
    @ApiModelProperty(value = "First name attribute key")
    private final String firstNameAttributeKey;
    /**
     * 属性，用于定位映射、配置或数据项。
     */
    @Length(fieldName = "lastNameAttributeKey", max = 31)
    @ApiModelProperty(value = "Last name attribute key")
    private final String lastNameAttributeKey;
    /**
     * 租户对象，用于描述当前业务场景。
     */
    @ApiModelProperty(value = "Tenant naming strategy. For DOMAIN type, domain for tenant name will be taken from the email (substring before '@')", required = true)
    private final TenantNameStrategyType tenantNameStrategy;
    /**
     * 租户对象，用于描述当前业务场景。
     */
    @Length(fieldName = "tenantNamePattern")
    @ApiModelProperty(value = "Tenant name pattern for CUSTOM naming strategy. " +
            "OAuth2 attributes in the pattern can be used by enclosing attribute key in '%{' and '}'", example = "%{email}")
    private final String tenantNamePattern;
    /**
     * 客户对象，用于描述当前业务场景。
     */
    @Length(fieldName = "customerNamePattern")
    @ApiModelProperty(value = "Customer name pattern. When creating a user on the first OAuth2 log in, if specified, " +
            "customer name will be used to create or find existing customer in the platform and assign customerId to the user")
    private final String customerNamePattern;
    /**
     * 仪表盘，用于标识或展示当前对象。
     */
    @Length(fieldName = "defaultDashboardName")
    @ApiModelProperty(value = "Name of the tenant's dashboard to set as default dashboard for newly created user")
    private final String defaultDashboardName;
    /**
     * 是否满足`alwaysFullScreen`条件。
     */
    @ApiModelProperty(value = "Whether default dashboard should be open in full screen")
    private final boolean alwaysFullScreen;
}

/*
 * 本类总结：
 * 1. 核心职责：`OAuth2BasicMapperConfig` 在 ThingsBoard Common 模块 中承担公共数据模型类型职责，核心目的是承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 核心流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
 * 3. 关键依赖：主要依赖或协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
