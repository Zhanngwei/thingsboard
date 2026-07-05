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
package org.thingsboard.rule.engine.rpc;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.server.common.data.StringUtils;

/**
 * `TbSendRpcReplyNodeConfiguration` 类，封装当前模块中的一组相关职责。
 */
@Data
public class TbSendRpcReplyNodeConfiguration implements NodeConfiguration<TbSendRpcReplyNodeConfiguration> {

    /**
     * 服务常量，用于统一引用固定值。
     */
    public static final String SERVICE_ID = "serviceId";
    /**
     * 会话常量，用于统一引用固定值。
     */
    public static final String SESSION_ID = "sessionId";
    /**
     * 请求常量，用于统一引用固定值。
     */
    public static final String REQUEST_ID = "requestId";

    /**
     * 属性，提供当前类调用的业务操作。
     */
    private String serviceIdMetaDataAttribute;
    /**
     * 会话，保存当前步骤读取或计算得到的内容。
     */
    private String sessionIdMetaDataAttribute;
    /**
     * 当前请求对象，封装本次处理需要的输入信息。
     */
    private String requestIdMetaDataAttribute;

    /**
     * 功能：执行 `defaultConfiguration` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public TbSendRpcReplyNodeConfiguration defaultConfiguration() {
        TbSendRpcReplyNodeConfiguration configuration = new TbSendRpcReplyNodeConfiguration();
        configuration.setServiceIdMetaDataAttribute(SERVICE_ID);
        configuration.setSessionIdMetaDataAttribute(SESSION_ID);
        configuration.setRequestIdMetaDataAttribute(REQUEST_ID);
        return configuration;
    }

    /**
     * 功能：获取属性。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getServiceIdMetaDataAttribute() {
        return !StringUtils.isEmpty(serviceIdMetaDataAttribute) ? serviceIdMetaDataAttribute : SERVICE_ID;
    }

    /**
     * 功能：获取会话。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getSessionIdMetaDataAttribute() {
        return !StringUtils.isEmpty(sessionIdMetaDataAttribute) ? sessionIdMetaDataAttribute : SESSION_ID;
    }

    /**
     * 功能：获取属性。
     * 参数：无。
     * 返回：文本结果。
     */
    public String getRequestIdMetaDataAttribute() {
        return !StringUtils.isEmpty(requestIdMetaDataAttribute) ? requestIdMetaDataAttribute : REQUEST_ID;
    }
}

/*
 * 本类总结：
 * 本类描述 RPC 回复节点从消息元数据读取 serviceId、sessionId 和 requestId 的配置。
 * 实际 RPC 回复发送、EdgeEvent 持久化和异步回调由 TbSendRPCReplyNode 完成；本类不直接涉及数据库或缓存。
 */

