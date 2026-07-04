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

@Data
/**
 * RPC 回复节点配置，定义从消息元数据中读取会话标识的字段名。
 * 配置类本身不直接发送 RPC、不访问数据库/缓存，也不涉及异步回调。
 */
public class TbSendRpcReplyNodeConfiguration implements NodeConfiguration<TbSendRpcReplyNodeConfiguration> {

    /**
     * 默认 serviceId 元数据字段名。
     */
    public static final String SERVICE_ID = "serviceId";
    /**
     * 默认 sessionId 元数据字段名。
     */
    public static final String SESSION_ID = "sessionId";
    /**
     * 默认 requestId 元数据字段名。
     */
    public static final String REQUEST_ID = "requestId";

    /**
     * 配置的 serviceId 元数据字段名。
     */
    private String serviceIdMetaDataAttribute;
    /**
     * 配置的 sessionId 元数据字段名。
     */
    private String sessionIdMetaDataAttribute;
    /**
     * 配置的 requestId 元数据字段名。
     */
    private String requestIdMetaDataAttribute;

    /**
     * 构造 RPC 回复节点默认配置。
     * 本方法只设置默认元数据字段名，不直接调用 RpcService。
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
     * 返回 serviceId 元数据字段名，空配置时回退到默认值。
     * 本方法只读取本地配置，不直接涉及外部调用。
     */
    public String getServiceIdMetaDataAttribute() {
        return !StringUtils.isEmpty(serviceIdMetaDataAttribute) ? serviceIdMetaDataAttribute : SERVICE_ID;
    }

    /**
     * 返回 sessionId 元数据字段名，空配置时回退到默认值。
     * 本方法只读取本地配置，不直接涉及外部调用。
     */
    public String getSessionIdMetaDataAttribute() {
        return !StringUtils.isEmpty(sessionIdMetaDataAttribute) ? sessionIdMetaDataAttribute : SESSION_ID;
    }

    /**
     * 返回 requestId 元数据字段名，空配置时回退到默认值。
     * 本方法只读取本地配置，不直接涉及外部调用。
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

