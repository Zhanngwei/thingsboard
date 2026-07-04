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

@Data
/**
 * RPC 请求节点配置，保存默认超时时间。
 * 配置类本身不直接发送 RPC、不访问数据库/缓存，也不涉及异步回调。
 */
public class TbSendRpcRequestNodeConfiguration implements NodeConfiguration<TbSendRpcRequestNodeConfiguration> {

    /**
     * 未从消息元数据提供 expirationTime 时使用的超时时间，单位秒。
     */
    private int timeoutInSeconds;

    /**
     * 构造 RPC 请求节点默认配置。
     * 本方法只设置默认超时，不直接调用 RpcService 或处理消息确认。
     */
    @Override
    public TbSendRpcRequestNodeConfiguration defaultConfiguration() {
        TbSendRpcRequestNodeConfiguration configuration = new TbSendRpcRequestNodeConfiguration();
        configuration.setTimeoutInSeconds(60);
        return configuration;
    }
}

/*
 * 本类总结：
 * 本类只提供 RPC 请求节点的超时配置，实际 RPC 发送、响应回调和路由由 TbSendRPCRequestNode 完成。
 * 它不直接涉及外部调用、数据库、缓存或 Actor 调度。
 */
