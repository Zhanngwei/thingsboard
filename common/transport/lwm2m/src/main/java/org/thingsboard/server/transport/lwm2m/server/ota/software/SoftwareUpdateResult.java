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
package org.thingsboard.server.transport.lwm2m.server.ota.software;

import lombok.Getter;

/**
 * SW Update Result
 * Contains the result of downloading or installing/uninstalling the software
 * 0: Initial value.
 * - Prior to download any new package in the Device, Update Result MUST be reset to this initial value.
 * - One side effect of executing the Uninstall resource is to reset Update Result to this initial value "0".
 * 1: Downloading.
 * - The package downloading process is on-going.
 * 2: Software successfully installed.
 * 3: Successfully Downloaded and package integrity verified
 * (( 4-49, for expansion, of other scenarios))
 * ** Failed
 * 50: Not enough storage for the new software package.
 * 51: Out of memory during downloading process.
 * 52: Connection lost during downloading process.
 * 53: Package integrity check failure.
 * 54: Unsupported package type.
 * 56: Invalid URI
 * 57: Device defined update error
 * 58: Software installation failure
 * 59: Uninstallation Failure during forUpdate(arg=0)
 * 60-200 : (for expansion, selection to be in blocks depending on new introduction of features)
 * This Resource MAY be reported by sending Observe operation.
 */
/**
 * 中文说明：
 * 1. 类目的：`SoftwareUpdateResult` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
public enum SoftwareUpdateResult {
    INITIAL(0, "Initial value", false),
    DOWNLOADING(1, "Downloading", false),
    SUCCESSFULLY_INSTALLED(2, "Software successfully installed", false),
    SUCCESSFULLY_DOWNLOADED_VERIFIED(3, "Successfully Downloaded and package integrity verified", false),
    NOT_ENOUGH_STORAGE(50, "Not enough storage for the new software package", true),
    OUT_OFF_MEMORY(51, "Out of memory during downloading process", true),
    CONNECTION_LOST(52, "Connection lost during downloading process", false),
    PACKAGE_CHECK_FAILURE(53, "Package integrity check failure.", false),
    UNSUPPORTED_PACKAGE_TYPE(54, "Unsupported package type", false),
    INVALID_URI(56, "Invalid URI", true),
    UPDATE_ERROR(57, "Device defined update error", true),
    INSTALL_FAILURE(58, "Software installation failure", true),
    UN_INSTALL_FAILURE(59, "Uninstallation Failure during forUpdate(arg=0)", true);

    /**
     * 编码，表示当前对象的对应属性。
     */
    @Getter
    private int code;
    /**
     * 类型，用于区分不同处理分支。
     */
    @Getter
    private String type;
    /**
     * 是否为`again`。
     */
    @Getter
    private boolean isAgain;

    SoftwareUpdateResult(int code, String type, boolean isAgain) {
        this.code = code;
        this.type = type;
        this.isAgain = isAgain;
    }

    /**
     * 功能：执行 `fromUpdateResultSwByType` 对应的处理。
     * 参数：
     * - `type`：类型。
     * 返回：处理结果。
     */
    public static SoftwareUpdateResult fromUpdateResultSwByType(String type) {
        for (SoftwareUpdateResult to : SoftwareUpdateResult.values()) {
            if (to.type.equals(type)) {
                return to;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported SW Update Result type  : %s", type));
    }

    /**
     * 功能：执行 `fromUpdateResultSwByCode` 对应的处理。
     * 参数：
     * - `code`：`code` 参数。
     * 返回：处理结果。
     */
    public static SoftwareUpdateResult fromUpdateResultSwByCode(int code) {
        for (SoftwareUpdateResult to : SoftwareUpdateResult.values()) {
            if (to.code == code) {
                return to;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported SW Update Result code  : %s", code));
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`SoftwareUpdateResult` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
