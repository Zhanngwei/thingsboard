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

/**
 * Created by ashvayka on 17.05.18.
 */
/**
 * 中文说明：
 * 1. `TbUrlConstants` 是 ThingsBoard Application 中处理 `Tb Url Constants` 通用操作的工具类型。
 * 2. 它提供无状态或轻量的复用方法，减少多个调用点的重复实现。
 * 3. 方法通常完成格式化、校验、计算或简单对象构造。
 * 4. 它直接协作于方法参数和返回值所代表的数据类型。
 * 5. 集中工具方法可以统一边界行为，并降低细节变化对调用方的影响。
 * 6. 阅读时重点关注输入约束、边界值和方法是否修改传入对象。
 */
public class TbUrlConstants {
    /**
     * 遥测常量，用于统一引用固定值。
     */
    public static final String TELEMETRY_URL_PREFIX = "/api/plugins/telemetry";
    public static final String RPC_V1_URL_PREFIX = "/api/plugins/rpc";
    /**
     * RPC常量，用于统一引用固定值。
     */
    public static final String RPC_V2_URL_PREFIX = "/api/rpc";
}
