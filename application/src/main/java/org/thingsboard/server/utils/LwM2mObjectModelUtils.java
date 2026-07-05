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
package org.thingsboard.server.utils;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.model.InvalidDDFFileException;
import org.eclipse.leshan.core.model.ObjectModel;
import org.thingsboard.server.common.data.ResourceType;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.lwm2m.LwM2mInstance;
import org.thingsboard.server.common.data.lwm2m.LwM2mObject;
import org.thingsboard.server.common.data.lwm2m.LwM2mResourceObserve;
import org.thingsboard.server.common.data.util.TbDDFFileParser;
import org.thingsboard.server.dao.exception.DataValidationException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.thingsboard.server.common.data.lwm2m.LwM2mConstants.LWM2M_SEPARATOR_KEY;
import static org.thingsboard.server.common.data.lwm2m.LwM2mConstants.LWM2M_SEPARATOR_SEARCH_TEXT;

/**
 * 中文说明：
 * 1. 类目的：`LwM2mObjectModelUtils` 是ThingsBoard Application 模块中的应用服务支撑类型，用于承载服务端运行期的数据、依赖或流程控制。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Spring Bean、DAO、缓存、队列、Actor、Transport、MQTT 和 Rule Engine 调用链。
 * 4. 生命周期：由 Spring 容器、Actor System、Web 请求或队列消费流程管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO/Helper。
 */
@Slf4j
public class LwM2mObjectModelUtils {

    private static final TbDDFFileParser ddfFileParser = new TbDDFFileParser();

    /**
     * 功能：执行 `toLwm2mResource` 对应的处理。
     * 参数：
     * - `resource`：`resource` 参数。
     * 返回：无。
     */
    public static void toLwm2mResource(TbResource resource) throws ThingsboardException {
        try {
            List<ObjectModel> objectModels =
                    ddfFileParser.parse(new ByteArrayInputStream(resource.getData()), resource.getSearchText());
            if (!objectModels.isEmpty()) {
                ObjectModel objectModel = objectModels.get(0);

                String resourceKey = objectModel.id + LWM2M_SEPARATOR_KEY + objectModel.version;
                String name = objectModel.name;
                resource.setResourceKey(resourceKey);
                if (resource.getId() == null) {
                    resource.setTitle(name + " id=" + objectModel.id + " v" + objectModel.version);
                }
                resource.setSearchText(resourceKey + LWM2M_SEPARATOR_SEARCH_TEXT + name);
            } else {
                throw new DataValidationException(String.format("Could not parse the XML of objectModel with name %s", resource.getSearchText()));
            }
        } catch (InvalidDDFFileException e) {
            log.error("Failed to parse file {}", resource.getFileName(), e);
            throw new DataValidationException("Failed to parse file " + resource.getFileName());
        } catch (IOException e) {
            throw new ThingsboardException(e, ThingsboardErrorCode.GENERAL);
        }
        if (resource.getResourceType().equals(ResourceType.LWM2M_MODEL) && toLwM2mObject(resource, true) == null) {
            throw new DataValidationException(String.format("Could not parse the XML of objectModel with name %s", resource.getSearchText()));
        }
    }

    /**
     * 功能：执行 `toLwM2mObject` 对应的处理。
     * 参数：
     * - `resource`：`resource` 参数。
     * - `isSave`：`isSave` 参数。
     * 返回：处理结果。
     */
    public static LwM2mObject toLwM2mObject(TbResource resource, boolean isSave) {
        try {
            List<ObjectModel> objectModels =
                    ddfFileParser.parse(new ByteArrayInputStream(resource.getData()), resource.getSearchText());
            if (objectModels.size() == 0) {
                return null;
            } else {
                ObjectModel obj = objectModels.get(0);
                LwM2mObject lwM2mObject = new LwM2mObject();
                lwM2mObject.setId(obj.id);
                lwM2mObject.setKeyId(resource.getResourceKey());
                lwM2mObject.setName(obj.name);
                lwM2mObject.setMultiple(obj.multiple);
                lwM2mObject.setMandatory(obj.mandatory);
                LwM2mInstance instance = new LwM2mInstance();
                instance.setId(0);
                List<LwM2mResourceObserve> resources = new ArrayList<>();
                obj.resources.forEach((k, v) -> {
                    if (isSave) {
                        LwM2mResourceObserve lwM2MResourceObserve = new LwM2mResourceObserve(k, v.name, false, false, false);
                        resources.add(lwM2MResourceObserve);
                    } else if (v.operations.isReadable()) {
                        LwM2mResourceObserve lwM2MResourceObserve = new LwM2mResourceObserve(k, v.name, false, false, false);
                        resources.add(lwM2MResourceObserve);
                    }
                });
                if (isSave || resources.size() > 0) {
                    instance.setResources(resources.toArray(LwM2mResourceObserve[]::new));
                    lwM2mObject.setInstances(new LwM2mInstance[]{instance});
                    return lwM2mObject;
                } else {
                    return null;
                }
            }
        } catch (IOException | InvalidDDFFileException e) {
            log.error("Could not parse the XML of objectModel with name [{}]", resource.getSearchText(), e);
            return null;
        }
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`LwM2mObjectModelUtils` 在 ThingsBoard Application 模块 中承担应用服务支撑类型职责，核心目的是承载服务端运行期的数据、依赖或流程控制。
 * 2. 核心流程：初始化依赖后处理请求、消息或测试断言，并把结果交还调用方。
 * 3. 关键依赖：主要依赖或协作对象包括Spring Bean、DAO、缓存、队列、Actor、Transport、MQTT 和 Rule Engine 调用链。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
