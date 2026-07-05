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
package org.thingsboard.rule.engine.debug;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.script.ScriptLanguage;

/**
 * `TbMsgGeneratorNodeConfiguration` 类，封装当前模块中的一组相关职责。
 */
@Data
public class TbMsgGeneratorNodeConfiguration implements NodeConfiguration<TbMsgGeneratorNodeConfiguration> {

    /**
     * 消息常量，用于统一引用固定值。
     */
    public static final int UNLIMITED_MSG_COUNT = 0;
    /**
     * `DEFAULT_SCRIPT`常量，用于统一引用固定值。
     */
    public static final String DEFAULT_SCRIPT = "var msg = { temp: 42, humidity: 77 };\n" +
            "var metadata = { data: 40 };\n" +
            "var msgType = \"POST_TELEMETRY_REQUEST\";\n\n" +
            "return { msg: msg, metadata: metadata, msgType: msgType };";

    /**
     * 消息，承载当前步骤需要处理的内容。
     */
    private int msgCount;
    /**
     * `periodInSeconds` 字段，保存当前对象的对应属性。
     */
    private int periodInSeconds;
    /**
     * `originatorId`ID，用于定位对应业务对象。
     */
    private String originatorId;
    /**
     * 类型，用于区分不同处理分支。
     */
    private EntityType originatorType;
    /**
     * `scriptLang` 字段，保存当前对象的对应属性。
     */
    private ScriptLanguage scriptLang;
    /**
     * `jsScript` 字段，保存当前对象的对应属性。
     */
    private String jsScript;
    /**
     * `tbelScript` 字段，保存当前对象的对应属性。
     */
    private String tbelScript;

    /**
     * 功能：执行 `defaultConfiguration` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbMsgGeneratorNodeConfiguration defaultConfiguration() {
        TbMsgGeneratorNodeConfiguration configuration = new TbMsgGeneratorNodeConfiguration();
        configuration.setMsgCount(UNLIMITED_MSG_COUNT);
        configuration.setPeriodInSeconds(1);
        configuration.setScriptLang(ScriptLanguage.TBEL);
        configuration.setJsScript(DEFAULT_SCRIPT);
        configuration.setTbelScript(DEFAULT_SCRIPT);
        return configuration;
    }
}

/*
 * 本类总结：
 * 本类集中描述 generator 节点的可配置项；运行时异步脚本回调、消息调度和线程安全边界由 TbMsgGeneratorNode 处理。
 */
