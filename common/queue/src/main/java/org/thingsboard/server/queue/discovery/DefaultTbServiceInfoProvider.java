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
package org.thingsboard.server.queue.discovery;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TbTransportService;
import org.thingsboard.server.common.data.util.CollectionsUtil;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.ServiceInfo;
import org.thingsboard.server.queue.util.AfterContextReady;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.thingsboard.common.util.SystemUtil.getCpuCount;
import static org.thingsboard.common.util.SystemUtil.getCpuUsage;
import static org.thingsboard.common.util.SystemUtil.getDiscSpaceUsage;
import static org.thingsboard.common.util.SystemUtil.getMemoryUsage;
import static org.thingsboard.common.util.SystemUtil.getTotalDiscSpace;
import static org.thingsboard.common.util.SystemUtil.getTotalMemory;


/**
 * 中文说明：
 * 1. 类目的：`DefaultTbServiceInfoProvider` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Component
@Slf4j
public class DefaultTbServiceInfoProvider implements TbServiceInfoProvider {

    /**
     * 服务ID，用于定位对应业务对象。
     */
    @Getter
    @Value("${service.id:#{null}}")
    private String serviceId;

    /**
     * 类型，提供当前类调用的业务操作。
     */
    @Getter
    @Value("${service.type:monolith}")
    private String serviceType;

    /**
     * 租户集合，用于去重保存或快速判断对象是否存在。
     */
    @Getter
    @Value("${service.rule_engine.assigned_tenant_profiles:}")
    private Set<UUID> assignedTenantProfiles;

    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    @Autowired
    private ApplicationContext applicationContext;

    /**
     * 服务列表，用于保存一组待处理对象。
     */
    private List<ServiceType> serviceTypes;
    private ServiceInfo serviceInfo;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    public void init() {
        if (StringUtils.isEmpty(serviceId)) {
            try {
                serviceId = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                serviceId = StringUtils.randomAlphabetic(10);
            }
        }
        log.info("Current Service ID: {}", serviceId);
        if (serviceType.equalsIgnoreCase("monolith")) {
            serviceTypes = List.of(ServiceType.values());
        } else {
            serviceTypes = Collections.singletonList(ServiceType.of(serviceType));
        }
        if (!serviceTypes.contains(ServiceType.TB_RULE_ENGINE) || assignedTenantProfiles == null) {
            assignedTenantProfiles = Collections.emptySet();
        }

       generateNewServiceInfoWithCurrentSystemInfo();
    }

    /**
     * 功能：更新`Transports`。
     * 参数：无。
     * 返回：无。
     */
    @AfterContextReady
    public void setTransports() {
        serviceInfo = ServiceInfo.newBuilder(serviceInfo)
                .addAllTransports(getTransportServices().stream()
                        .map(TbTransportService::getName)
                        .collect(Collectors.toSet()))
                .build();
    }

    /**
     * 功能：获取传输层。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    private Collection<TbTransportService> getTransportServices() {
        return applicationContext.getBeansOfType(TbTransportService.class).values();
    }

    /**
     * 功能：获取服务。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public ServiceInfo getServiceInfo() {
        return serviceInfo;
    }

    /**
     * 功能：判断服务。
     * 参数：
     * - `serviceType`：服务对象。
     * 返回：判断结果。
     */
    @Override
    public boolean isService(ServiceType serviceType) {
        return serviceTypes.contains(serviceType);
    }

    /**
     * 功能：执行 `generateNewServiceInfoWithCurrentSystemInfo` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public ServiceInfo generateNewServiceInfoWithCurrentSystemInfo() {
        ServiceInfo.Builder builder = ServiceInfo.newBuilder()
                .setServiceId(serviceId)
                .addAllServiceTypes(serviceTypes.stream().map(ServiceType::name).collect(Collectors.toList()))
                .setSystemInfo(getCurrentSystemInfoProto());
        if (CollectionsUtil.isNotEmpty(assignedTenantProfiles)) {
            builder.addAllAssignedTenantProfiles(assignedTenantProfiles.stream().map(UUID::toString).collect(Collectors.toList()));
        }
        return serviceInfo = builder.build();
    }

    /**
     * 功能：获取信息对象。
     * 参数：无。
     * 返回：处理结果。
     */
    private TransportProtos.SystemInfoProto getCurrentSystemInfoProto() {
        TransportProtos.SystemInfoProto.Builder builder = TransportProtos.SystemInfoProto.newBuilder();

        getCpuUsage().ifPresent(builder::setCpuUsage);
        getMemoryUsage().ifPresent(builder::setMemoryUsage);
        getDiscSpaceUsage().ifPresent(builder::setDiskUsage);

        getCpuCount().ifPresent(builder::setCpuCount);
        getTotalMemory().ifPresent(builder::setTotalMemory);
        getTotalDiscSpace().ifPresent(builder::setTotalDiscSpace);

        return builder.build();
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultTbServiceInfoProvider` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
