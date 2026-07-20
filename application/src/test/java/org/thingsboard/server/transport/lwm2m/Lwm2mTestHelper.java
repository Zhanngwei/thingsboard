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
 * 1. `Lwm2mTestHelper` 是 ThingsBoard Application 中处理 LwM2M 通用操作的工具类型。
 * 2. 它提供无状态或轻量的复用方法，减少多个调用点的重复实现。
 * 3. 方法通常完成格式化、校验、计算或简单对象构造。
 * 4. 它直接协作于方法参数和返回值所代表的数据类型。
 * 5. 集中工具方法可以统一边界行为，并降低细节变化对调用方的影响。
 * 6. 阅读时重点关注输入约束、边界值和方法是否修改传入对象。
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
     * 1. `LwM2MClientState` 是 ThingsBoard Application 中定义 LwM2M 固定取值的枚举类型。
     * 2. 它列出当前流程允许使用的有限状态、模式或类别。
     * 3. 枚举值可携带与该选项关联的标识、名称或处理参数。
     * 4. 它直接协作于使用该枚举进行分支判断或序列化的类型。
     * 5. 使用枚举可以限制非法取值，并让分支语义在源码中保持明确。
     * 6. 阅读时重点关注各枚举值含义、附加字段和反向查找方法。
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
     * 1. `LwM2MProfileBootstrapConfigType` 是 ThingsBoard Application 中定义 LwM2M 固定取值的枚举类型。
     * 2. 它列出当前流程允许使用的有限状态、模式或类别。
     * 3. 枚举值可携带与该选项关联的标识、名称或处理参数。
     * 4. 它直接协作于使用该枚举进行分支判断或序列化的类型。
     * 5. 使用枚举可以限制非法取值，并让分支语义在源码中保持明确。
     * 6. 阅读时重点关注各枚举值含义、附加字段和反向查找方法。
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
