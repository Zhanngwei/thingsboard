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
 * 1. `Const` 是 ThingsBoard Microservices 中处理 `Const` 通用操作的工具类型。
 * 2. 它提供无状态或轻量的复用方法，减少多个调用点的重复实现。
 * 3. 方法通常完成格式化、校验、计算或简单对象构造。
 * 4. 它直接协作于方法参数和返回值所代表的数据类型。
 * 5. 集中工具方法可以统一边界行为，并降低细节变化对调用方的影响。
 * 6. 阅读时重点关注输入约束、边界值和方法是否修改传入对象。
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
