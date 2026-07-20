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
package org.thingsboard.rule.engine.transform;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.concurrent.ExecutionException;

/**
 * 中文说明：
 * 1. `TbJsonPathNode` 是 ThingsBoard Rule Engine Components 中处理 `Tb Json Path` 的规则节点。
 * 2. 它接收规则链消息，根据节点配置执行判断、转换或外部动作。
 * 3. 处理结果通过成功、失败或自定义关系继续传递给后续节点。
 * 4. 直接依赖的类型边界包括 `TbNode`。
 * 5. 独立节点类型让该能力可以在规则链中配置、复用和替换。
 * 6. 阅读时重点关注初始化配置、消息处理入口和关系类型的选择。
 */
@Slf4j
@RuleNode(
        type = ComponentType.TRANSFORMATION,
        name = "json path",
        configClazz = TbJsonPathNodeConfiguration.class,
        nodeDescription = "Transforms incoming message body using JSONPath expression.",
        nodeDetails = "JSONPath expression specifies a path to an element or a set of elements in a JSON structure.<br><br>" +
                "Output connections: <code>Success</code>, <code>Failure</code>.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        icon = "functions",
        configDirective = "tbTransformationNodeJsonPathConfig"
)
public class TbJsonPathNode implements TbNode {

    /**
     * 配置，保存当前对象的配置选项。
     */
    private TbJsonPathNodeConfiguration config;
    /**
     * 路径，保存当前对象的配置选项。
     */
    private Configuration configurationJsonPath;
    /**
     * 路径，用于定位本地文件或目录。
     */
    private JsonPath jsonPath;
    /**
     * 值，保存当前处理得到的具体内容。
     */
    private String jsonPathValue;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `configuration`：配置对象。
     * 返回：无。
     */
    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbJsonPathNodeConfiguration.class);
        this.jsonPathValue = config.getJsonPath();
        if (!TbJsonPathNodeConfiguration.DEFAULT_JSON_PATH.equals(this.jsonPathValue)) {
            this.configurationJsonPath = Configuration.builder()
                    .jsonProvider(new JacksonJsonNodeJsonProvider())
                    .build();
            this.jsonPath = JsonPath.compile(config.getJsonPath());
        }
    }

    /**
     * 功能：处理消息。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：无。
     */
    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        if (!TbJsonPathNodeConfiguration.DEFAULT_JSON_PATH.equals(this.jsonPathValue)) {
            try {
                Object jsonPathData = jsonPath.read(msg.getData(), this.configurationJsonPath);
                ctx.tellSuccess(TbMsg.transformMsgData(msg, JacksonUtil.toString(jsonPathData)));
            } catch (PathNotFoundException e) {
                ctx.tellFailure(msg, e);
            }
        } else {
            ctx.tellSuccess(msg);
        }
    }
}
