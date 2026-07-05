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
package org.thingsboard.server.service.component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.rule.engine.api.NodeDefinition;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.filter.TbMsgTypeSwitchNode;
import org.thingsboard.rule.engine.filter.TbOriginatorTypeSwitchNode;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.plugin.ComponentDescriptor;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.dao.component.ComponentDescriptorService;

import javax.annotation.PostConstruct;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. 类目的：`AnnotationComponentDiscoveryService` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
@Service
@Slf4j
public class AnnotationComponentDiscoveryService implements ComponentDiscoveryService {

    /**
     * `MAX_OPTIMISITC_RETRIES`常量，用于统一引用固定值。
     */
    public static final int MAX_OPTIMISITC_RETRIES = 3;

    /**
     * `scanPackages`列表，用于保存一组待处理对象。
     */
    @Value("${plugins.scan_packages}")
    private String[] scanPackages;

    /**
     * 环境配置，保存当前对象的配置选项。
     */
    @Autowired
    private Environment environment;

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    private ComponentDescriptorService componentDescriptorService;

    private final Map<String, RuleNodeClassInfo> ruleNodeClasses = new HashMap<>();

    private final Map<String, ComponentDescriptor> components = new HashMap<>();

    private final Map<ComponentType, List<ComponentDescriptor>> coreComponentsMap = new HashMap<>();

    private final Map<ComponentType, List<ComponentDescriptor>> edgeComponentsMap = new HashMap<>();

    /**
     * 功能：判断`Install`。
     * 参数：无。
     * 返回：判断结果。
     */
    private boolean isInstall() {
        return environment.acceptsProfiles(Profiles.of("install"));
    }

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    public void init() {
        for (var def : discoverBeansByAnnotationType(RuleNode.class)) {
            String clazzName = def.getBeanClassName();
            try {
                var clazz = Class.forName(clazzName);
                RuleNode annotation = clazz.getAnnotation(RuleNode.class);
                ruleNodeClasses.put(clazzName, new RuleNodeClassInfo(clazz, annotation));
            } catch (Exception e) {
                log.warn("Failed to create instance of rule node type: {} due to: ", clazzName, e);
            }
        }
        if (!isInstall()) {
            discoverComponents();
        }
    }

    /**
     * 功能：执行 `discoverBeansByAnnotationType` 对应的处理。
     * 参数：
     * - `annotationType`：类型。
     * 返回：匹配的数据集合。
     */
    private Set<BeanDefinition> discoverBeansByAnnotationType(Class<? extends Annotation> annotationType) {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(annotationType));
        Set<BeanDefinition> defs = new HashSet<>();
        for (String scanPackage : scanPackages) {
            defs.addAll(scanner.findCandidateComponents(scanPackage));
        }
        return defs;
    }

    /**
     * 功能：获取规则节点。
     * 参数：
     * - `clazz`：`clazz` 参数。
     * 返回：可能存在的结果。
     */
    @Override
    public Optional<RuleNodeClassInfo> getRuleNodeInfo(String clazz) {
        return Optional.ofNullable(ruleNodeClasses.get(clazz));
    }

    /**
     * 功能：获取`Versioned Nodes`。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<RuleNodeClassInfo> getVersionedNodes() {
        return ruleNodeClasses.values().stream().filter(RuleNodeClassInfo::isVersioned).collect(Collectors.toList());
    }

    /**
     * 功能：保存或创建规则节点。
     * 参数：无。
     * 返回：无。
     */
    private void registerRuleNodeComponents() {
        for (RuleNodeClassInfo def : ruleNodeClasses.values()) {
            int retryCount = 0;
            Exception cause = null;
            while (retryCount < MAX_OPTIMISITC_RETRIES) {
                try {
                    ComponentType type = def.getAnnotation().type();
                    ComponentDescriptor component = scanAndPersistComponent(def, type);
                    components.put(component.getClazz(), component);
                    putComponentIntoMaps(type, def.getAnnotation(), component);
                    break;
                } catch (Exception e) {
                    log.trace("Can't initialize component {}, due to {}", def.getClassName(), e.getMessage(), e);
                    cause = e;
                    retryCount++;
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                        throw new RuntimeException(e1);
                    }
                }
            }
            if (cause != null && retryCount == MAX_OPTIMISITC_RETRIES) {
                log.error("Can't initialize component {}, due to {}", def.getClassName(), cause.getMessage(), cause);
                throw new RuntimeException(cause);
            }
        }
    }

    /**
     * 功能：执行 `putComponentIntoMaps` 对应的处理。
     * 参数：
     * - `type`：类型。
     * - `ruleNodeAnnotation`：`ruleNodeAnnotation` 参数。
     * - `component`：`component` 参数。
     * 返回：无。
     */
    private void putComponentIntoMaps(ComponentType type, RuleNode ruleNodeAnnotation, ComponentDescriptor component) {
        boolean ruleChainTypesMethodAvailable;
        try {
            ruleNodeAnnotation.getClass().getMethod("ruleChainTypes");
            ruleChainTypesMethodAvailable = true;
        } catch (NoSuchMethodException exception) {
            log.warn("[{}] does not have ruleChainTypes. Probably extension class compiled before 3.3 release. " +
                    "Please update your extensions and compile using latest 3.3 release dependency", ruleNodeAnnotation.name());
            ruleChainTypesMethodAvailable = false;
        }
        if (ruleChainTypesMethodAvailable) {
            if (ruleChainTypeContainsArray(RuleChainType.CORE, ruleNodeAnnotation.ruleChainTypes())) {
                coreComponentsMap.computeIfAbsent(type, k -> new ArrayList<>()).add(component);
            }
            if (ruleChainTypeContainsArray(RuleChainType.EDGE, ruleNodeAnnotation.ruleChainTypes())) {
                edgeComponentsMap.computeIfAbsent(type, k -> new ArrayList<>()).add(component);
            }
        } else {
            coreComponentsMap.computeIfAbsent(type, k -> new ArrayList<>()).add(component);
        }
    }

    /**
     * 功能：执行 `ruleChainTypeContainsArray` 对应的处理。
     * 参数：
     * - `ruleChainType`：类型。
     * - `array`：`array` 参数。
     * 返回：判断结果。
     */
    private boolean ruleChainTypeContainsArray(RuleChainType ruleChainType, RuleChainType[] array) {
        for (RuleChainType tmp : array) {
            if (ruleChainType.equals(tmp)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 功能：执行 `scanAndPersistComponent` 对应的处理。
     * 参数：
     * - `def`：`def` 参数。
     * - `type`：类型。
     * 返回：处理结果。
     */
    private ComponentDescriptor scanAndPersistComponent(RuleNodeClassInfo def, ComponentType type) {
        ComponentDescriptor scannedComponent = new ComponentDescriptor();
        String clazzName = def.getClassName();
        try {
            scannedComponent.setType(type);
            Class<?> clazz = def.getClazz();
            RuleNode ruleNodeAnnotation = clazz.getAnnotation(RuleNode.class);
            scannedComponent.setConfigurationVersion(def.getCurrentVersion());
            scannedComponent.setName(ruleNodeAnnotation.name());
            scannedComponent.setScope(ruleNodeAnnotation.scope());
            scannedComponent.setClusteringMode(ruleNodeAnnotation.clusteringMode());
            scannedComponent.setHasQueueName(ruleNodeAnnotation.hasQueueName());
            NodeDefinition nodeDefinition = prepareNodeDefinition(clazz, ruleNodeAnnotation);
            ObjectNode configurationDescriptor = JacksonUtil.newObjectNode();
            JsonNode node = JacksonUtil.valueToTree(nodeDefinition);
            configurationDescriptor.set("nodeDefinition", node);
            scannedComponent.setConfigurationDescriptor(configurationDescriptor);
            scannedComponent.setClazz(clazzName);
            log.debug("Processing scanned component: {}", scannedComponent);
        } catch (Exception e) {
            log.error("Can't initialize component {}, due to {}", clazzName, e.getMessage(), e);
            throw new RuntimeException(e);
        }
        ComponentDescriptor persistedComponent = componentDescriptorService.findByClazz(TenantId.SYS_TENANT_ID, clazzName);
        if (persistedComponent == null) {
            log.debug("Persisting new component: {}", scannedComponent);
            scannedComponent = componentDescriptorService.saveComponent(TenantId.SYS_TENANT_ID, scannedComponent);
        } else if (scannedComponent.equals(persistedComponent)) {
            log.debug("Component is already persisted: {}", persistedComponent);
            scannedComponent = persistedComponent;
        } else {
            log.debug("Component {} will be updated to {}", persistedComponent, scannedComponent);
            componentDescriptorService.deleteByClazz(TenantId.SYS_TENANT_ID, persistedComponent.getClazz());
            scannedComponent.setId(persistedComponent.getId());
            scannedComponent = componentDescriptorService.saveComponent(TenantId.SYS_TENANT_ID, scannedComponent);
        }
        return scannedComponent;
    }

    /**
     * 功能：执行 `prepareNodeDefinition` 对应的处理。
     * 参数：
     * - `clazz`：`clazz` 参数。
     * - `nodeAnnotation`：`nodeAnnotation` 参数。
     * 返回：处理结果。
     */
    private NodeDefinition prepareNodeDefinition(Class<?> clazz, RuleNode nodeAnnotation) throws Exception {
        NodeDefinition nodeDefinition = new NodeDefinition();
        nodeDefinition.setDetails(nodeAnnotation.nodeDetails());
        nodeDefinition.setDescription(nodeAnnotation.nodeDescription());
        nodeDefinition.setInEnabled(nodeAnnotation.inEnabled());
        nodeDefinition.setOutEnabled(nodeAnnotation.outEnabled());
        nodeDefinition.setRelationTypes(getRelationTypesWithFailureRelation(clazz, nodeAnnotation));
        nodeDefinition.setCustomRelations(nodeAnnotation.customRelations());
        nodeDefinition.setRuleChainNode(nodeAnnotation.ruleChainNode());
        Class<? extends NodeConfiguration> configClazz = nodeAnnotation.configClazz();
        NodeConfiguration config = configClazz.getDeclaredConstructor().newInstance();
        NodeConfiguration defaultConfiguration = config.defaultConfiguration();
        nodeDefinition.setDefaultConfiguration(JacksonUtil.valueToTree(defaultConfiguration));
        nodeDefinition.setUiResources(nodeAnnotation.uiResources());
        nodeDefinition.setConfigDirective(nodeAnnotation.configDirective());
        nodeDefinition.setIcon(nodeAnnotation.icon());
        nodeDefinition.setIconUrl(nodeAnnotation.iconUrl());
        nodeDefinition.setDocUrl(nodeAnnotation.docUrl());
        return nodeDefinition;
    }

    /**
     * 功能：获取关系。
     * 参数：
     * - `clazz`：`clazz` 参数。
     * - `nodeAnnotation`：`nodeAnnotation` 参数。
     * 返回：处理结果。
     */
    private String[] getRelationTypesWithFailureRelation(Class<?> clazz, RuleNode nodeAnnotation) {
        List<String> relationTypes = new ArrayList<>(Arrays.asList(nodeAnnotation.relationTypes()));
        if (TbOriginatorTypeSwitchNode.class.equals(clazz)) {
            relationTypes.addAll(EntityType.NORMAL_NAMES);
        }
        if (TbMsgTypeSwitchNode.class.equals(clazz)) {
            relationTypes.addAll(TbMsgType.NODE_CONNECTIONS);
            relationTypes.add(TbNodeConnectionType.OTHER);
        }
        if (!relationTypes.contains(TbNodeConnectionType.FAILURE)) {
            relationTypes.add(TbNodeConnectionType.FAILURE);
        }
        return relationTypes.toArray(new String[relationTypes.size()]);
    }

    /**
     * 功能：执行 `discoverComponents` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void discoverComponents() {
        registerRuleNodeComponents();
        log.debug("Found following definitions: {}", components.values());
    }

    /**
     * 功能：获取`Components`。
     * 参数：
     * - `type`：类型。
     * - `ruleChainType`：类型。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<ComponentDescriptor> getComponents(ComponentType type, RuleChainType ruleChainType) {
        if (RuleChainType.CORE.equals(ruleChainType)) {
            if (coreComponentsMap.containsKey(type)) {
                return Collections.unmodifiableList(coreComponentsMap.get(type));
            } else {
                return Collections.emptyList();
            }
        } else if (RuleChainType.EDGE.equals(ruleChainType)) {
            if (edgeComponentsMap.containsKey(type)) {
                return Collections.unmodifiableList(edgeComponentsMap.get(type));
            } else {
                return Collections.emptyList();
            }
        } else {
            log.error("Unsupported rule chain type {}", ruleChainType);
            throw new RuntimeException("Unsupported rule chain type " + ruleChainType);
        }
    }

    /**
     * 功能：获取`Components`。
     * 参数：
     * - `types`：类型。
     * - `ruleChainType`：类型。
     * 返回：匹配的数据集合。
     */
    @Override
    public List<ComponentDescriptor> getComponents(Set<ComponentType> types, RuleChainType ruleChainType) {
        if (RuleChainType.CORE.equals(ruleChainType)) {
            return getComponents(types, coreComponentsMap);
        } else if (RuleChainType.EDGE.equals(ruleChainType)) {
            return getComponents(types, edgeComponentsMap);
        } else {
            log.error("Unsupported rule chain type {}", ruleChainType);
            throw new RuntimeException("Unsupported rule chain type " + ruleChainType);
        }
    }

    /**
     * 功能：获取`Component`。
     * 参数：
     * - `clazz`：`clazz` 参数。
     * 返回：可能存在的结果。
     */
    @Override
    public Optional<ComponentDescriptor> getComponent(String clazz) {
        return Optional.ofNullable(components.get(clazz));
    }

    /**
     * 功能：获取`Components`。
     * 参数：
     * - `types`：类型。
     * - `componentsMap`：数据列表。
     * 返回：匹配的数据集合。
     */
    private List<ComponentDescriptor> getComponents(Set<ComponentType> types, Map<ComponentType, List<ComponentDescriptor>> componentsMap) {
        List<ComponentDescriptor> result = new ArrayList<>();
        types.stream().filter(componentsMap::containsKey).forEach(type -> {
            result.addAll(componentsMap.get(type));
        });
        return Collections.unmodifiableList(result);
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`AnnotationComponentDiscoveryService` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
