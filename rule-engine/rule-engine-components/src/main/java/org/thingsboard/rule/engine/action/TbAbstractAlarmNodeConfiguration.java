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
package org.thingsboard.rule.engine.action;

import lombok.Data;
import org.thingsboard.server.common.data.script.ScriptLanguage;
import org.thingsboard.server.common.data.validation.NoXss;

/**
 * 中文说明：`TbAbstractAlarmNodeConfiguration` 是抽象告警节点配置对象，用于承载规则节点 JSON 中的配置项和默认值。
 * 配置来源：实例字段通常由前端规则节点配置 JSON 反序列化得到，`defaultConfiguration` 提供缺省配置。
 * 调用边界：本类本身不直接涉及数据库、缓存、MQTT、Actor 或事务；具体实现和调用链可能在使用这些配置的节点中涉及。
 */
@Data
public abstract class TbAbstractAlarmNodeConfiguration {

    /**
     * 告警常量，用于统一引用固定值。
     */
    static final String ALARM_DETAILS_BUILD_JS_TEMPLATE = "" +
            "var details = {};\n" +
            "if (metadata.prevAlarmDetails) {\n" +
            "    details = JSON.parse(metadata.prevAlarmDetails);\n" +
            "    //remove prevAlarmDetails from metadata\n" +
            "    delete metadata.prevAlarmDetails;\n" +
            "    //now metadata is the same as it comes IN this rule node\n" +
            "}\n" +
            "\n" +
            "\n" +
            "return details;";

    /**
     * 告警常量，用于统一引用固定值。
     */
    static final String ALARM_DETAILS_BUILD_TBEL_TEMPLATE = "" +
            "var details = {};\n" +
            "if (metadata.prevAlarmDetails != null) {\n" +
            "    details = JSON.parse(metadata.prevAlarmDetails);\n" +
            "    //remove prevAlarmDetails from metadata\n" +
            "    metadata.remove('prevAlarmDetails');\n" +
            "    //now metadata is the same as it comes IN this rule node\n" +
            "}\n" +
            "\n" +
            "\n" +
            "return details;";


    /**
     * 告警，用于区分不同处理分支。
     */
    @NoXss
    private String alarmType;
    /**
     * `scriptLang` 字段，保存当前对象的对应属性。
     */
    private ScriptLanguage scriptLang;
    /**
     * 告警对象，用于描述当前业务场景。
     */
    private String alarmDetailsBuildJs;
    /**
     * 告警对象，用于描述当前业务场景。
     */
    private String alarmDetailsBuildTbel;

    /*
     * 本类总结：`TbAbstractAlarmNodeConfiguration` 负责执行告警、客户归属、关系、设备状态、日志或外部存储等动作；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
