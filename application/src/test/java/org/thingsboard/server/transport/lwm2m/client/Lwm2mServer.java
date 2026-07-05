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
package org.thingsboard.server.transport.lwm2m.client;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.client.resource.LwM2mInstanceEnabler;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.request.BindingMode;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

/**
 * A simple {@link LwM2mInstanceEnabler} for the Server (1) object.
 */
/**
 * 中文说明：
 * 1. 类目的：`Lwm2mServer` 是ThingsBoard Application 测试模块中的传输层测试或适配类型，用于验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 生命周期：由 JUnit 测试生命周期创建，随单个测试方法准备和清理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Integration Test / Fixture。
 */
@Slf4j
public class Lwm2mServer extends BaseInstanceEnabler {

    private static final Logger LOG = LoggerFactory.getLogger(Lwm2mServer.class);

    private final static List<Integer> supportedResources = Arrays.asList(0, 1, 2, 3, 6, 7, 8, 22);

    /**
     * 服务端ID，用于定位对应业务对象。
     */
    private int shortServerId;
    private long lifetime;
    /**
     * `defaultMinPeriod` 字段，保存当前对象的对应属性。
     */
    private Long defaultMinPeriod;
    private Long defaultMaxPeriod;
    /**
     * 绑定模式集合，用于去重保存或快速判断对象是否存在。
     */
    private EnumSet<BindingMode> binding;
    private BindingMode preferredTransport;
    /**
     * 是否满足`notifyWhenDisable`条件。
     */
    private boolean notifyWhenDisable;

    /**
     * 功能：创建 `Lwm2mServer` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public Lwm2mServer() {
        // should only be used at bootstrap time
    }

    /**
     * 功能：创建 `Lwm2mServer` 实例，并初始化必要字段。
     * 参数：
     * - `shortServerId`：服务端ID。
     * - `lifetime`：`lifetime` 参数。
     * - `binding`：`binding` 参数。
     * - `notifyWhenDisable`：`notifyWhenDisable` 参数。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public Lwm2mServer(int shortServerId, long lifetime, EnumSet<BindingMode> binding, boolean notifyWhenDisable,
                       BindingMode preferredTransport) {
        this.shortServerId = shortServerId;
        this.lifetime = lifetime;
        this.binding = binding;
        this.notifyWhenDisable = notifyWhenDisable;
        this.preferredTransport = preferredTransport;
    }

    /**
     * 功能：创建 `Lwm2mServer` 实例，并初始化必要字段。
     * 参数：
     * - `shortServerId`：服务端ID。
     * - `lifetime`：`lifetime` 参数。
     * 返回：新创建的对象实例。
     */
    public Lwm2mServer(int shortServerId, long lifetime) {
        this(shortServerId, lifetime, EnumSet.of(BindingMode.U), false, BindingMode.U);
    }

    /**
     * 功能：执行 `read` 对应的处理。
     * 参数：
     * - `identity`：实体对象。
     * - `resourceid`：`resourceid`ID。
     * 返回：处理结果。
     */
    @Override
    public ReadResponse read(ServerIdentity identity, int resourceid) {
        if (!identity.isSystem())
            LOG.debug("Read on Server resource /{}/{}/{}", getModel().id, getId(), resourceid);

        switch (resourceid) {
        case 0: // short server ID
            return ReadResponse.success(resourceid, shortServerId);

        case 1: // lifetime
            return ReadResponse.success(resourceid, lifetime);

        case 2: // default min period
            if (null == defaultMinPeriod)
                return ReadResponse.notFound();
            return ReadResponse.success(resourceid, defaultMinPeriod);

        case 3: // default max period
            if (null == defaultMaxPeriod)
                return ReadResponse.notFound();
            return ReadResponse.success(resourceid, defaultMaxPeriod);

        case 6: // notification storing when disable or offline
            return ReadResponse.success(resourceid, notifyWhenDisable);

        case 7: // binding
            return ReadResponse.success(resourceid, BindingMode.toString(binding));

        case 22: // preferred transport
            if (preferredTransport == null)
                return ReadResponse.notFound();
            return ReadResponse.success(resourceid, preferredTransport.toString());

        default:
            return super.read(identity, resourceid);
        }
    }

    /**
     * 功能：执行 `write` 对应的处理。
     * 参数：
     * - `identity`：实体对象。
     * - `replace`：`replace` 参数。
     * - `resourceid`：`resourceid`ID。
     * - `value`：值。
     * 返回：处理结果。
     */
    @Override
    public WriteResponse write(ServerIdentity identity, boolean replace, int resourceid, LwM2mResource value) {
        if (!identity.isSystem())
            log.debug("Write on Server resource /{}/{}/{}", getModel().id, getId(), resourceid);

        switch (resourceid) {
        case 0:
            if (value.getType() != Type.INTEGER) {
                return WriteResponse.badRequest("invalid type");
            }
            int previousShortServerId = shortServerId;
            shortServerId = ((Long) value.getValue()).intValue();
            if (previousShortServerId != shortServerId)
                fireResourceChange(resourceid);
            return WriteResponse.success();

        case 1:
            if (value.getType() != Type.INTEGER) {
                return WriteResponse.badRequest("invalid type");
            }
            long previousLifetime = lifetime;
            lifetime = (Long) value.getValue();
            if (previousLifetime != lifetime)
                fireResourceChange(resourceid);
            return WriteResponse.success();

        case 2:
            if (value.getType() != Type.INTEGER) {
                return WriteResponse.badRequest("invalid type");
            }
            Long previousDefaultMinPeriod = defaultMinPeriod;
            defaultMinPeriod = (Long) value.getValue();
            if (!Objects.equals(previousDefaultMinPeriod, defaultMinPeriod))
                fireResourceChange(resourceid);
            return WriteResponse.success();

        case 3:
            if (value.getType() != Type.INTEGER) {
                return WriteResponse.badRequest("invalid type");
            }
            Long previousDefaultMaxPeriod = defaultMaxPeriod;
            defaultMaxPeriod = (Long) value.getValue();
            if (!Objects.equals(previousDefaultMaxPeriod, defaultMaxPeriod))
                fireResourceChange(resourceid);
            return WriteResponse.success();

        case 6: // notification storing when disable or offline
            if (value.getType() != Type.BOOLEAN) {
                return WriteResponse.badRequest("invalid type");
            }
            boolean previousNotifyWhenDisable = notifyWhenDisable;
            notifyWhenDisable = (boolean) value.getValue();
            if (previousNotifyWhenDisable != notifyWhenDisable)
                fireResourceChange(resourceid);
            return WriteResponse.success();

        case 7: // binding
            if (value.getType() != Type.STRING) {
                return WriteResponse.badRequest("invalid type");
            }
            try {
                EnumSet<BindingMode> previousBinding = binding;
                binding = BindingMode.parse((String) value.getValue());
                if (!Objects.equals(previousBinding, binding))
                    fireResourceChange(resourceid);
                return WriteResponse.success();
            } catch (IllegalArgumentException e) {
                return WriteResponse.badRequest("invalid value");
            }
        case 22: // preferredTransport
            if (value.getType() != Type.STRING) {
                return WriteResponse.badRequest("invalid type");
            }
            try {
                BindingMode previousPreferedTransport = preferredTransport;
                preferredTransport = BindingMode.valueOf((String) value.getValue());
                if (!Objects.equals(previousPreferedTransport, preferredTransport))
                    fireResourceChange(resourceid);
                return WriteResponse.success();
            } catch (IllegalArgumentException e) {
                return WriteResponse.badRequest("invalid value");
            }

        default:
            return super.write(identity, replace, resourceid, value);
        }
    }

    /**
     * 功能：执行 `execute` 对应的处理。
     * 参数：
     * - `identity`：实体对象。
     * - `resourceid`：`resourceid`ID。
     * - `params`：`params` 参数。
     * 返回：处理结果。
     */
    @Override
    public ExecuteResponse execute(ServerIdentity identity, int resourceid, String params) {
        log.info("Execute on Server resource /{}/{}/{}", getModel().id, getId(), resourceid);
        if (resourceid == 8) {
            getLwM2mClient().triggerRegistrationUpdate(identity);
            return ExecuteResponse.success();
        } else if (resourceid == 9) {
            boolean success = getLwM2mClient().triggerClientInitiatedBootstrap(true);
            if (success) {
                return ExecuteResponse.success();
            }
            else {
                return ExecuteResponse.badRequest("probably no bootstrap server configured");
            }
        } else {
            return super.execute(identity, resourceid, params);
        }
    }

    /**
     * 功能：执行 `reset` 对应的处理。
     * 参数：
     * - `resourceid`：`resourceid`ID。
     * 返回：无。
     */
    @Override
    public void reset(int resourceid) {
        switch (resourceid) {
        case 2:
            defaultMinPeriod = null;
            break;
        case 3:
            defaultMaxPeriod = null;
            break;
        default:
            super.reset(resourceid);
        }
    }

    /**
     * 功能：获取`Available Resource Ids`。
     * 参数：
     * - `model`：`model` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<Integer> getAvailableResourceIds(ObjectModel model) {
        return supportedResources;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`Lwm2mServer` 在 ThingsBoard Application 测试模块 中承担传输层测试或适配类型职责，核心目的是验证 MQTT、CoAP、LwM2M 或传输协议与服务端应用的集成行为。
 * 2. 核心流程：构造协议客户端并发送消息，等待服务端处理后断言响应或持久化结果。
 * 3. 关键依赖：主要依赖或协作对象包括Transport API、会话、遥测服务、Actor、队列和测试容器。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
