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
 * 1. `DefaultTbServiceInfoProvider` 是 ThingsBoard Common Queue 中创建或提供 `Tb Provider` 对象的构造组件。
 * 2. 它根据输入配置、类型或上下文选择合适的具体实现。
 * 3. 创建细节被集中在该类型中，调用方只依赖稳定的创建入口。
 * 4. 直接依赖的类型边界包括 `TbServiceInfoProvider`。
 * 5. 独立工厂可以避免调用方了解构造顺序和实现类选择规则。
 * 6. 阅读时重点关注实现选择条件、默认分支和对象初始化参数。
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
