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
package org.thingsboard.server.transport.lwm2m.server;

import lombok.Getter;

/**
 * Define the behavior of a write request.
 */
/**
 * 中文说明：
 * 1. 类目的：`LwM2MOperationType` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
public enum LwM2MOperationType {

    READ(0, "Read", true),
    READ_COMPOSITE(1, "ReadComposite", false, true),
    DISCOVER(2, "Discover", true),
    DISCOVER_ALL(3, "DiscoverAll", false),
    OBSERVE_READ_ALL(4, "ObserveReadAll", false),

    OBSERVE(5, "Observe", true),
    OBSERVE_COMPOSITE(6, "ObserveComposite", false, true),
    OBSERVE_CANCEL(7, "ObserveCancel", true),
    OBSERVE_COMPOSITE_CANCEL(8, "ObserveCompositeCancel", false, true),
    OBSERVE_CANCEL_ALL(9, "ObserveCancelAll", false),
    EXECUTE(10, "Execute", true),
    /**
     * Replaces the Object Instance or the Resource(s) with the new value provided in the “Write” operation. (see
     * section 5.3.3 of the LW M2M spec).
     * if all resources are to be replaced
     */
    WRITE_REPLACE(11, "WriteReplace", true),

    /**
     * Adds or updates Resources provided in the new value and leaves other existing Resources unchanged. (see section
     * 5.3.3 of the LW M2M spec).
     * if this is a partial update request
     */
    WRITE_UPDATE(12, "WriteUpdate", true),
    WRITE_COMPOSITE(14, "WriteComposite", false, true),
    WRITE_ATTRIBUTES(15, "WriteAttributes", true),
    DELETE(16, "Delete", true),
    CREATE(17, "Create", true);
    // only for RPC - future
//    FW_UPDATE(18, "FirmwareUpdate", false),
//    FW_READ_INFO(19, "FirmwareReadInfo", false),
//    SW_UPDATE(20, "SoftwareUpdate", false),
//    SW_READ_INFO(21, "SoftwareReadInfo", false),
//    SW_UNINSTALL(22, "SoftwareUninstall", false);


    /**
     * 编码，表示当前对象的对应属性。
     */
    @Getter
    private final int code;
    /**
     * 类型，用于区分不同处理分支。
     */
    @Getter
    private final String type;
    /**
     * `hasObjectId`ID，用于定位对应业务对象。
     */
    @Getter
    private final boolean hasObjectId;

    /**
     * 是否满足`composite`条件。
     */
    @Getter
    private final boolean composite;

    LwM2MOperationType(int code, String type, boolean hasObjectId) {
        this(code, type, hasObjectId, false);
    }

    LwM2MOperationType(int code, String type, boolean hasObjectId, boolean composite) {
        this.code = code;
        this.type = type;
        this.hasObjectId = hasObjectId;
        this.composite = composite;
        if (hasObjectId && composite) {
            throw new IllegalArgumentException("Can't set both Composite and hasObjectId for the same operation!");
        }
    }

    /**
     * 功能：执行 `fromType` 对应的处理。
     * 参数：
     * - `type`：类型。
     * 返回：处理结果。
     */
    public static LwM2MOperationType fromType(String type) {
        for (LwM2MOperationType to : LwM2MOperationType.values()) {
            if (to.type.equals(type)) {
                return to;
            }
        }
        return null;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`LwM2MOperationType` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
