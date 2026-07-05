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
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.script.ScriptLanguage;
import org.thingsboard.server.common.data.validation.NoXss;

import java.util.Collections;
import java.util.List;

/**
 * 中文说明：`TbCreateAlarmNodeConfiguration` 是创建告警节点配置对象，用于承载规则节点 JSON 中的配置项和默认值。
 * 配置来源：实例字段通常由前端规则节点配置 JSON 反序列化得到，`defaultConfiguration` 提供缺省配置。
 * 调用边界：本类本身不直接涉及数据库、缓存、MQTT、Actor 或事务；具体实现和调用链可能在使用这些配置的节点中涉及。
 */
@Data
public class TbCreateAlarmNodeConfiguration extends TbAbstractAlarmNodeConfiguration implements NodeConfiguration<TbCreateAlarmNodeConfiguration> {

    /**
     * `severity` 字段，保存当前对象的对应属性。
     */
    @NoXss
    private String severity;
    /**
     * 是否向关联对象传播当前状态。
     */
    private boolean propagate;
    /**
     * 是否向关联对象传播当前状态。
     */
    private boolean propagateToOwner;
    /**
     * 是否向关联对象传播当前状态。
     */
    private boolean propagateToTenant;
    /**
     * 是否使用告警。
     */
    private boolean useMessageAlarmData;
    /**
     * 是否满足告警条件。
     */
    private boolean overwriteAlarmDetails = true;
    /**
     * 是否满足`dynamicSeverity`条件。
     */
    private boolean dynamicSeverity;

    /**
     * 关系列表，用于保存一组待处理对象。
     */
    private List<String> relationTypes;

    /**
     * 功能：执行 `defaultConfiguration` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbCreateAlarmNodeConfiguration defaultConfiguration() {
        TbCreateAlarmNodeConfiguration configuration = new TbCreateAlarmNodeConfiguration();
        configuration.setScriptLang(ScriptLanguage.TBEL);
        configuration.setAlarmDetailsBuildJs(ALARM_DETAILS_BUILD_JS_TEMPLATE);
        configuration.setAlarmDetailsBuildTbel(ALARM_DETAILS_BUILD_TBEL_TEMPLATE);
        configuration.setAlarmType("General Alarm");
        configuration.setSeverity(AlarmSeverity.CRITICAL.name());
        configuration.setPropagate(false);
        configuration.setPropagateToOwner(false);
        configuration.setPropagateToTenant(false);
        configuration.setUseMessageAlarmData(false);
        configuration.setOverwriteAlarmDetails(false);
        configuration.setRelationTypes(Collections.emptyList());
        configuration.setDynamicSeverity(false);
        return configuration;
    }

    /*
     * 本类总结：`TbCreateAlarmNodeConfiguration` 负责执行告警、客户归属、关系、设备状态、日志或外部存储等动作；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
