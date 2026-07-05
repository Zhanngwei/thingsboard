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
package org.thingsboard.server.transport.lwm2m;

/**
 * 中文说明：
 * 1. 类目的：`Lwm2mTestHelper` 是ThingsBoard Application 测试模块中的传输层测试或适配类型，用于验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 生命周期：由 JUnit 测试生命周期创建，随单个测试方法准备和清理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Integration Test / Fixture。
 */
public class Lwm2mTestHelper {

    // Models
    /**
     * `resources`常量，用于统一引用固定值。
     */
    public static final String[] resources = new String[]{"0.xml", "1.xml", "2.xml", "3.xml", "5.xml", "6.xml", "9.xml", "19.xml", "3303.xml"};
    public static final int BINARY_APP_DATA_CONTAINER = 19;
    /**
     * `TEMPERATURE_SENSOR`常量，用于统一引用固定值。
     */
    public static final int TEMPERATURE_SENSOR = 3303;

    // Ids in Client
    /**
     * `OBJECT_ID_0`常量，用于统一引用固定值。
     */
    public static final int OBJECT_ID_0 = 0;
    public static final int OBJECT_ID_1 = 1;
    /**
     * `OBJECT_INSTANCE_ID_0`常量，用于统一引用固定值。
     */
    public static final int OBJECT_INSTANCE_ID_0 = 0;
    public static final int OBJECT_INSTANCE_ID_1 = 1;
    /**
     * `OBJECT_INSTANCE_ID_2`常量，用于统一引用固定值。
     */
    public static final int OBJECT_INSTANCE_ID_2 = 2;
    public static final int OBJECT_INSTANCE_ID_12 = 12;
    /**
     * `RESOURCE_ID_0`常量，用于统一引用固定值。
     */
    public static final int RESOURCE_ID_0 = 0;
    public static final int RESOURCE_ID_1 = 1;
    /**
     * `RESOURCE_ID_2`常量，用于统一引用固定值。
     */
    public static final int RESOURCE_ID_2 = 2;
    public static final int RESOURCE_ID_3 = 3;
    /**
     * `RESOURCE_ID_4`常量，用于统一引用固定值。
     */
    public static final int RESOURCE_ID_4 = 4;
    public static final int RESOURCE_ID_7 = 7;
    /**
     * `RESOURCE_ID_8`常量，用于统一引用固定值。
     */
    public static final int RESOURCE_ID_8 = 8;
    public static final int RESOURCE_ID_9 = 9;
    /**
     * `RESOURCE_ID_11`常量，用于统一引用固定值。
     */
    public static final int RESOURCE_ID_11 = 11;
    public static final int RESOURCE_ID_14 = 14;
    /**
     * `RESOURCE_ID_15`常量，用于统一引用固定值。
     */
    public static final int RESOURCE_ID_15 = 15;
    public static final int RESOURCE_INSTANCE_ID_2 = 2;

    /**
     * 名称常量，用于统一引用固定值。
     */
    public static final String RESOURCE_ID_NAME_3_9 = "batteryLevel";
    public static final String RESOURCE_ID_NAME_3_14 = "UtfOffset";
    /**
     * 名称常量，用于统一引用固定值。
     */
    public static final String RESOURCE_ID_NAME_19_0_0 = "dataRead";
    public static final String RESOURCE_ID_NAME_19_1_0 = "dataWrite";
    /**
     * 名称常量，用于统一引用固定值。
     */
    public static final String RESOURCE_ID_NAME_19_0_3 = "dataDescription";

    /**
     * 中文说明：
     * 1. 类目的：`LwM2MClientState` 是ThingsBoard Application 测试模块中的传输层测试或适配类型，用于验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
     * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
     * 3. 协作对象：主要协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
     * 4. 生命周期：由 JUnit 测试生命周期创建，随单个测试方法准备和清理。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 Integration Test / Fixture。
     */
    public enum LwM2MClientState {

        ON_INIT(1, "onInit"),
        ON_BOOTSTRAP_STARTED(1, "onBootstrapStarted"),
        ON_BOOTSTRAP_SUCCESS(2, "onBootstrapSuccess"),
        ON_BOOTSTRAP_FAILURE(3, "onBootstrapFailure"),
        ON_BOOTSTRAP_TIMEOUT(4, "onBootstrapTimeout"),
        ON_REGISTRATION_STARTED(5, "onRegistrationStarted"),
        ON_REGISTRATION_SUCCESS(6, "onRegistrationSuccess"),
        ON_REGISTRATION_FAILURE(7, "onRegistrationFailure"),
        ON_REGISTRATION_TIMEOUT(7, "onRegistrationTimeout"),
        ON_UPDATE_STARTED(8, "onUpdateStarted"),
        ON_UPDATE_SUCCESS(9, "onUpdateSuccess"),
        ON_UPDATE_FAILURE(10, "onUpdateFailure"),
        ON_UPDATE_TIMEOUT(11, "onUpdateTimeout"),
        ON_DEREGISTRATION_STARTED(12, "onDeregistrationStarted"),
        ON_DEREGISTRATION_SUCCESS(13, "onDeregistrationSuccess"),
        ON_DEREGISTRATION_FAILURE(14, "onDeregistrationFailure"),
        ON_DEREGISTRATION_TIMEOUT(15, "onDeregistrationTimeout"),
        ON_EXPECTED_ERROR(16, "onUnexpectedError");

        /**
         * 编码，表示当前对象的对应属性。
         */
        public int code;
        public String type;

        LwM2MClientState(int code, String type) {
            this.code = code;
            this.type = type;
        }

        /**
         * 功能：执行 `fromLwM2MClientStateByType` 对应的处理。
         * 参数：
         * - `type`：类型。
         * 返回：处理结果。
         */
        public static LwM2MClientState fromLwM2MClientStateByType(String type) {
            for (LwM2MClientState to : LwM2MClientState.values()) {
                if (to.type.equals(type)) {
                    return to;
                }
            }
            throw new IllegalArgumentException(String.format("Unsupported Client State type  : %s", type));
        }

        /**
         * 功能：执行 `fromLwM2MClientStateByCode` 对应的处理。
         * 参数：
         * - `code`：`code` 参数。
         * 返回：处理结果。
         */
        public static LwM2MClientState fromLwM2MClientStateByCode(int code) {
            for (LwM2MClientState to : LwM2MClientState.values()) {
                if (to.code == code) {
                    return to;
                }
            }
            throw new IllegalArgumentException(String.format("Unsupported Client State code : %s", code));
        }
    }

    /**
     * 中文说明：
     * 1. 类目的：`LwM2MProfileBootstrapConfigType` 是ThingsBoard Application 测试模块中的传输层测试或适配类型，用于验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
     * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
     * 3. 协作对象：主要协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
     * 4. 生命周期：由 JUnit 测试生命周期创建，随单个测试方法准备和清理。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 Integration Test / Fixture。
     */
    public enum LwM2MProfileBootstrapConfigType {

        LWM2M_ONLY(1, "only Lwm2m Server"),
        BOOTSTRAP_ONLY(2, "only Bootstrap Server"),
        BOTH(3, "Lwm2m Server and Bootstrap Server"),
        NONE(4, "Without Lwm2m Server and Bootstrap Server");

        /**
         * 编码，表示当前对象的对应属性。
         */
        public int code;
        public String type;

        LwM2MProfileBootstrapConfigType(int code, String type) {
            this.code = code;
            this.type = type;
        }

        /**
         * 功能：执行 `fromLwM2MBootstrapConfigByType` 对应的处理。
         * 参数：
         * - `type`：类型。
         * 返回：处理结果。
         */
        public static LwM2MProfileBootstrapConfigType fromLwM2MBootstrapConfigByType(String type) {
            for (LwM2MProfileBootstrapConfigType to : LwM2MProfileBootstrapConfigType.values()) {
                if (to.type.equals(type)) {
                    return to;
                }
            }
            throw new IllegalArgumentException(String.format("Unsupported Profile Bootstrap Config type  : %s", type));
        }

        /**
         * 功能：执行 `fromLwM2MBootstrapConfigByCode` 对应的处理。
         * 参数：
         * - `code`：`code` 参数。
         * 返回：处理结果。
         */
        public static LwM2MProfileBootstrapConfigType fromLwM2MBootstrapConfigByCode(int code) {
            for (LwM2MProfileBootstrapConfigType to : LwM2MProfileBootstrapConfigType.values()) {
                if (to.code == code) {
                    return to;
                }
            }
            throw new IllegalArgumentException(String.format("Unsupported Profile Bootstrap Config code : %s", code));
        }
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`Lwm2mTestHelper` 在 ThingsBoard Application 测试模块 中承担传输层测试或适配类型职责，核心目的是验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 核心流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
 * 3. 关键依赖：主要依赖或协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
