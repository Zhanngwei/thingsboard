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
 * 消息生成器节点的配置对象，定义生成次数、周期、来源实体和脚本内容。
 * 本类仅保存配置，不直接执行脚本、不访问数据库或缓存，也不直接处理 Rule Engine 消息流。
 */
@Data
public class TbMsgGeneratorNodeConfiguration implements NodeConfiguration<TbMsgGeneratorNodeConfiguration> {

    /**
     * 表示不限制生成消息数量的配置值。
     */
    public static final int UNLIMITED_MSG_COUNT = 0;
    /**
     * 默认脚本，返回一条示例遥测消息、元数据和消息类型。
     */
    public static final String DEFAULT_SCRIPT = "var msg = { temp: 42, humidity: 77 };\n" +
            "var metadata = { data: 40 };\n" +
            "var msgType = \"POST_TELEMETRY_REQUEST\";\n\n" +
            "return { msg: msg, metadata: metadata, msgType: msgType };";

    /**
     * 允许生成的消息总数，0 表示无限生成。
     */
    private int msgCount;
    /**
     * 两次生成之间的周期，单位为秒。
     */
    private int periodInSeconds;
    /**
     * 可选的来源实体 UUID 字符串；为空时使用节点自身作为来源实体。
     */
    private String originatorId;
    /**
     * 来源实体类型，只有配置了来源实体 UUID 时才会被使用。
     */
    private EntityType originatorType;
    /**
     * 脚本语言，决定运行 TBEL 脚本还是 JavaScript 脚本。
     */
    private ScriptLanguage scriptLang;
    /**
     * JavaScript 版本的生成脚本。
     */
    private String jsScript;
    /**
     * TBEL 版本的生成脚本。
     */
    private String tbelScript;

    /**
     * 创建消息生成器节点的默认配置。
     * 本方法只构造内存对象，不触发脚本执行、数据库读取、缓存读取或 Rule Engine 消息投递。
     *
     * @return 带默认生成周期、无限次数和默认脚本的配置实例
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
