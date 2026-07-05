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

import static org.thingsboard.server.msa.TestProperties.getBaseUrl;

/**
 * 中文说明：
 * 1. 类目的：`Const` 是 ThingsBoard MSA 测试模块 中的微服务测试和部署支撑类型，用于支撑微服务部署、黑盒测试、UI 自动化、协议连通性验证或版本控制执行器路由。
 * 2. 所属模块：位于 msa 聚合模块，服务于 ThingsBoard 的运维监控、微服务测试或 MQTT 客户端协议边界。
 * 3. 协作对象：主要协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
 * 4. 生命周期：由 MSA 测试套件、Docker 编排流程、Selenium 驱动或 Spring Boot VC executor 启动和销毁。
 * 5. 设计原因：单独建模该类型可以隔离协议细节、测试编排、页面操作和运行时探测逻辑，避免业务模块直接耦合外部工具或网络状态机。
 * 6. 事务与缓存：测试通过服务 API 或容器初始化间接影响数据库；VC executor 自身主要负责队列路由而非事务管理。
 * 7. MQTT/Actor/Rule Engine：是否直接涉及 MQTT 取决于模块；监控和 MSA 可能通过协议入口间接触发 Actor 与 Rule Engine，netty-mqtt 则直接管理 MQTT 会话。
 * 8. 设计模式：主要体现 Test Fixture / Page Object / Service。
 */
public class Const {

    public static final String URL = getBaseUrl();
    /**
     * 租户常量，用于统一引用固定值。
     */
    public static final String TENANT_EMAIL = "tenant@thingsboard.org";
    public static final String TENANT_PASSWORD = "tenant";
    /**
     * 实体常量，用于统一引用固定值。
     */
    public static final String ENTITY_NAME = "Az!@#$%^&*()_-+=~`";
    public static final String ROOT_RULE_CHAIN_NAME = "Root Rule Chain";
    /**
     * 规则链常量，用于统一引用固定值。
     */
    public static final String IMPORT_RULE_CHAIN_NAME = "Rule Chain For Import";
    public static final String IMPORT_DEVICE_PROFILE_NAME = "Device Profile For Import";
    /**
     * 资产配置常量，用于统一引用固定值。
     */
    public static final String IMPORT_ASSET_PROFILE_NAME = "Asset Profile For Import";
    public static final String IMPORT_RULE_CHAIN_FILE_NAME = "ruleChainForImport.json";
    /**
     * 设备配置常量，用于统一引用固定值。
     */
    public static final String IMPORT_DEVICE_PROFILE_FILE_NAME = "deviceProfileForImport.json";
    public static final String IMPORT_ASSET_PROFILE_FILE_NAME = "assetProfileForImport.json";
    /**
     * 名称常量，用于统一引用固定值。
     */
    public static final String IMPORT_TXT_FILE_NAME = "forImport.txt";
    public static final String EMPTY_IMPORT_MESSAGE = "No file selected";
    /**
     * 规则链常量，用于统一引用固定值。
     */
    public static final String EMPTY_RULE_CHAIN_MESSAGE = "Rule chain name should be specified!";
    public static final String EMPTY_CUSTOMER_MESSAGE = "Customer title should be specified!";
    /**
     * 设备配置常量，用于统一引用固定值。
     */
    public static final String EMPTY_DEVICE_PROFILE_MESSAGE = "Device profile name should be specified!";
    public static final String EMPTY_ASSET_PROFILE_MESSAGE = "Asset profile name should be specified!";
    /**
     * 设备常量，用于统一引用固定值。
     */
    public static final String EMPTY_DEVICE_MESSAGE = "Device name should be specified!";
    public static final String DELETE_RULE_CHAIN_WITH_PROFILE_MESSAGE = "The rule chain referenced by the device profiles cannot be deleted!";
    /**
     * 客户常量，用于统一引用固定值。
     */
    public static final String SAME_NAME_WARNING_CUSTOMER_MESSAGE = "Customer with such title already exists!";
    public static final String SAME_NAME_WARNING_DEVICE_PROFILE_MESSAGE = "Device profile with such name already exists!";
    /**
     * 资产配置常量，用于统一引用固定值。
     */
    public static final String SAME_NAME_WARNING_ASSET_PROFILE_MESSAGE = "Asset profile with such name already exists!";
    public static final String SAME_NAME_WARNING_DEVICE_MESSAGE = "Device with such name already exists!";
    /**
     * 消息常量，用于统一引用固定值。
     */
    public static final String PHONE_NUMBER_ERROR_MESSAGE = "Phone number is invalid or not possible";
    public static final String NAME_IS_REQUIRED_MESSAGE = "Name is required.";
    /**
     * 设备配置常量，用于统一引用固定值。
     */
    public static final String DEVICE_PROFILE_IS_REQUIRED_MESSAGE = "Device profile is required";
    public static final String DEVICE_ACTIVE_STATE = "Active";
    /**
     * 设备常量，用于统一引用固定值。
     */
    public static final String DEVICE_INACTIVE_STATE = "Inactive";
    public static final String PUBLIC_CUSTOMER_NAME = "Public";
}

/*
 * 本类总结：
 * 1. 核心职责：`Const` 在 ThingsBoard MSA 测试模块 中承担微服务测试和部署支撑类型职责，核心目的是支撑微服务部署、黑盒测试、UI 自动化、协议连通性验证或版本控制执行器路由。
 * 2. 核心流程：准备微服务环境和测试数据，执行 REST、协议或 UI 操作，等待异步结果并断言服务端状态。
 * 3. 关键依赖：主要依赖或协作对象包括Docker Compose、Testcontainers、Selenium、TestNG/JUnit、REST 客户端、MQTT/CoAP/HTTP 客户端、Web UI 和版本控制队列。
 * 4. 学习重点：阅读本文件时应关注连接生命周期、异步回调、协议状态、测试环境、线程安全边界，以及它与 MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
