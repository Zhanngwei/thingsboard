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
package org.thingsboard.server.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.plugin.ComponentDescriptor;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.thingsboard.server.controller.ControllerConstants.SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH;

/**
 * 中文说明：
 * 1. 类目的：`ComponentDescriptorController` 是ThingsBoard Application 模块中的REST/WebSocket 控制层类型，用于承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 生命周期：由 Spring MVC 容器创建，按单次 Web 请求或 WebSocket 会话调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 MVC Controller / Facade。
 */
@RestController
@TbCoreComponent
@RequestMapping("/api")
public class ComponentDescriptorController extends BaseController {

    private static final String COMPONENT_DESCRIPTOR_DEFINITION = "Each Component Descriptor represents configuration of specific rule node (e.g. 'Save Timeseries' or 'Send Email'.). " +
            "The Component Descriptors are used by the rule chain Web UI to build the configuration forms for the rule nodes. " +
            "The Component Descriptors are discovered at runtime by scanning the class path and searching for @RuleNode annotation. " +
            "Once discovered, the up to date list of descriptors is persisted to the database.";

    /**
     * `name` 类，封装当前模块中的一组相关职责。
     */
    @ApiOperation(value = "Get Component Descriptor (getComponentDescriptorByClazz)",
            notes = "Gets the Component Descriptor object using class name from the path parameters. " +
                    COMPONENT_DESCRIPTOR_DEFINITION + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','TENANT_ADMIN')")
    @RequestMapping(value = "/component/{componentDescriptorClazz:.+}", method = RequestMethod.GET)
    @ResponseBody
    public ComponentDescriptor getComponentDescriptorByClazz(
            @ApiParam(value = "Component Descriptor class name", required = true)
            @PathVariable("componentDescriptorClazz") String strComponentDescriptorClazz) throws ThingsboardException {
        checkParameter("strComponentDescriptorClazz", strComponentDescriptorClazz);
        return checkComponentDescriptorByClazz(strComponentDescriptorClazz);
    }

    /**
     * 功能：获取类型。
     * 参数：
     * - `Node`：`Node` 参数。
     * - `ENRICHMENT`：`ENRICHMENT` 参数。
     * - `FILTER`：`FILTER` 参数。
     * - `TRANSFORMATION`：`TRANSFORMATION` 参数。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @ApiOperation(value = "Get Component Descriptors (getComponentDescriptorsByType)",
            notes = "Gets the Component Descriptors using rule node type and optional rule chain type request parameters. " +
                    COMPONENT_DESCRIPTOR_DEFINITION + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','TENANT_ADMIN')")
    @RequestMapping(value = "/components/{componentType}", method = RequestMethod.GET)
    @ResponseBody
    public List<ComponentDescriptor> getComponentDescriptorsByType(
            @ApiParam(value = "Type of the Rule Node", allowableValues = "ENRICHMENT,FILTER,TRANSFORMATION,ACTION,EXTERNAL", required = true)
            @PathVariable("componentType") String strComponentType,
            @ApiParam(value = "Type of the Rule Chain", allowableValues = "CORE,EDGE")
            @RequestParam(value = "ruleChainType", required = false) String strRuleChainType) throws ThingsboardException {
        checkParameter("componentType", strComponentType);
        return checkComponentDescriptorsByType(ComponentType.valueOf(strComponentType), getRuleChainType(strRuleChainType));
    }

    /**
     * 功能：获取`Component Descriptors By Types`。
     * 参数：
     * - `Nodes`：数据列表。
     * - `ENRICHMENT`：`ENRICHMENT` 参数。
     * - `FILTER`：`FILTER` 参数。
     * - `TRANSFORMATION`：`TRANSFORMATION` 参数。
     * - 其余参数：补充处理条件。
     * 返回：匹配的数据集合。
     */
    @ApiOperation(value = "Get Component Descriptors (getComponentDescriptorsByTypes)",
            notes = "Gets the Component Descriptors using coma separated list of rule node types and optional rule chain type request parameters. " +
                    COMPONENT_DESCRIPTOR_DEFINITION + SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH)
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','TENANT_ADMIN')")
    @RequestMapping(value = "/components", params = {"componentTypes"}, method = RequestMethod.GET)
    @ResponseBody
    public List<ComponentDescriptor> getComponentDescriptorsByTypes(
            @ApiParam(value = "List of types of the Rule Nodes, (ENRICHMENT, FILTER, TRANSFORMATION, ACTION or EXTERNAL)", required = true)
            @RequestParam("componentTypes") String[] strComponentTypes,
            @ApiParam(value = "Type of the Rule Chain", allowableValues = "CORE,EDGE")
            @RequestParam(value = "ruleChainType", required = false) String strRuleChainType) throws ThingsboardException {
        checkArrayParameter("componentTypes", strComponentTypes);
        Set<ComponentType> componentTypes = new HashSet<>();
        for (String strComponentType : strComponentTypes) {
            componentTypes.add(ComponentType.valueOf(strComponentType));
        }
        return checkComponentDescriptorsByTypes(componentTypes, getRuleChainType(strRuleChainType));
    }

    /**
     * 功能：获取规则链。
     * 参数：
     * - `strRuleChainType`：类型。
     * 返回：处理结果。
     */
    private RuleChainType getRuleChainType(String strRuleChainType) {
        RuleChainType ruleChainType;
        if (StringUtils.isEmpty(strRuleChainType)) {
            ruleChainType = RuleChainType.CORE;
        } else {
            ruleChainType = RuleChainType.valueOf(strRuleChainType);
        }
        return ruleChainType;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`ComponentDescriptorController` 在 ThingsBoard Application 模块 中承担REST/WebSocket 控制层类型职责，核心目的是承接 HTTP 或 WebSocket 入口并把请求委派给服务层。
 * 2. 核心流程：校验权限和参数后调用服务层，最终返回 DTO、响应体或异步回调。
 * 3. 关键依赖：主要依赖或协作对象包括Spring MVC、安全上下文、Service、DAO、缓存和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
