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
package org.thingsboard.server.transport.lwm2m.config;

import lombok.Getter;
import org.eclipse.leshan.core.LwM2m.LwM2mVersion;
import org.eclipse.leshan.core.request.ContentFormat;

/**
 * 中文说明：
 * 1. 类目的：`TbLwM2mVersion` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
public enum TbLwM2mVersion {
    VERSION_1_0(0, LwM2mVersion.V1_0, ContentFormat.TLV, false),
    VERSION_1_1(1, LwM2mVersion.V1_1, ContentFormat.TEXT, true);

    /**
     * 编码，表示当前对象的对应属性。
     */
    @Getter
    private final int code;
    /**
     * 版本号，表示当前对象的对应属性。
     */
    @Getter
    private final LwM2mVersion version;
    /**
     * 内容格式，表示当前对象的对应属性。
     */
    @Getter
    private final ContentFormat contentFormat;
    /**
     * 是否满足`composite`条件。
     */
    @Getter
    private final boolean composite;

    TbLwM2mVersion(int code, LwM2mVersion version, ContentFormat contentFormat, boolean composite) {
        this.code = code;
        this.version = version;
        this.contentFormat = contentFormat;
        this.composite = composite;
    }

    /**
     * 功能：执行 `fromVersion` 对应的处理。
     * 参数：
     * - `version`：`version` 参数。
     * 返回：处理结果。
     */
    public static TbLwM2mVersion fromVersion(LwM2mVersion version) {
        for (TbLwM2mVersion to : TbLwM2mVersion.values()) {
            if (to.version.equals(version)) {
                return to;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported typeLwM2mVersion type : %s", version));
    }

    /**
     * 功能：执行 `fromVersionStr` 对应的处理。
     * 参数：
     * - `versionStr`：`versionStr` 参数。
     * 返回：处理结果。
     */
    public static TbLwM2mVersion fromVersionStr(String versionStr) {
        for (TbLwM2mVersion to : TbLwM2mVersion.values()) {
            if (to.version.toString().equals(versionStr)) {
                return to;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported contentFormatLwM2mVersion version : %s", versionStr));
    }

    /**
     * 功能：执行 `fromCode` 对应的处理。
     * 参数：
     * - `code`：`code` 参数。
     * 返回：处理结果。
     */
    public static TbLwM2mVersion fromCode(int code) {
        for (TbLwM2mVersion to : TbLwM2mVersion.values()) {
            if (to.code == code) {
                return to;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupported codeLwM2mVersion code : %d", code));
    }
}


/*
 * 本类总结：
 * 1. 核心职责：`TbLwM2mVersion` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
