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
package org.thingsboard.server.service.entitiy.user;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.HasTitle;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.settings.AbstractUserDashboardInfo;
import org.thingsboard.server.common.data.settings.LastVisitedDashboardInfo;
import org.thingsboard.server.common.data.settings.StarredDashboardInfo;
import org.thingsboard.server.common.data.settings.UserDashboardAction;
import org.thingsboard.server.common.data.settings.UserDashboardsInfo;
import org.thingsboard.server.common.data.settings.UserSettings;
import org.thingsboard.server.common.data.settings.UserSettingsType;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.user.UserSettingsService;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@TbCoreComponent
@AllArgsConstructor
@Slf4j
/**
 * 中文说明：
 * 1. 类目的：`DefaultTbUserSettingsService` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
public class DefaultTbUserSettingsService implements TbUserSettingsService {

    /**
     * 字段说明：
     * 1. 保存 `MAX_DASHBOARD_INFO_LIST_SIZE` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private static final int MAX_DASHBOARD_INFO_LIST_SIZE = 10;
    private static final Predicate<HasTitle> EMPTY_TITLE = i -> StringUtils.isEmpty(i.getTitle());

    /**
     * 字段说明：
     * 1. 保存 `settingsService` 对应的配置、依赖、上下文或运行期状态。
     * 2. 数据来源通常是 Spring 注入、构造参数、配置文件、DAO 查询、队列消息或测试夹具。
     * 3. 生命周期与持有该字段的对象一致，单例 Bean 字段随应用生命周期存在，消息/测试字段随单次流程存在。
     * 4. 单独保存该字段可以减少重复查询或参数透传，使 Controller、Service、Actor 和测试代码的职责更清晰。
     * 5. 并发与缓存语义取决于字段具体类型；可变集合、缓存或异步状态需要由调用方保证线程安全。
     */
    private final UserSettingsService settingsService;
    private final DashboardService dashboardService;

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `saveUserSettings` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public UserSettings saveUserSettings(TenantId tenantId, UserSettings userSettings) {
        return settingsService.saveUserSettings(tenantId, userSettings);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `updateUserSettings` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void updateUserSettings(TenantId tenantId, UserId userId, UserSettingsType type, JsonNode settings) {
        settingsService.updateUserSettings(tenantId, userId, type, settings);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `findUserSettings` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public UserSettings findUserSettings(TenantId tenantId, UserId userId, UserSettingsType type) {
        return settingsService.findUserSettings(tenantId, userId, type);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `deleteUserSettings` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public void deleteUserSettings(TenantId tenantId, UserId userId, UserSettingsType type, List<String> jsonPaths) {
        settingsService.deleteUserSettings(tenantId, userId, type, jsonPaths);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `findUserDashboardsInfo` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public UserDashboardsInfo findUserDashboardsInfo(TenantId tenantId, UserId id) {
        UserSettings us = findUserSettings(tenantId, id, UserSettingsType.VISITED_DASHBOARDS);
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (us == null) {
            return UserDashboardsInfo.EMPTY;
        }
        UserDashboardsInfo stored = JacksonUtil.convertValue(us.getSettings(), UserDashboardsInfo.class);
        return refreshDashboardTitles(tenantId, stored);
    }

    @Override
    /**
     * 方法说明：
     * 1. 职责：执行 `reportUserDashboardAction` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    public UserDashboardsInfo reportUserDashboardAction(TenantId tenantId, UserId id, DashboardId dashboardId, UserDashboardAction action) {
        UserSettings us = findUserSettings(tenantId, id, UserSettingsType.VISITED_DASHBOARDS);
        UserDashboardsInfo stored = null;
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (us != null) {
            stored = JacksonUtil.convertValue(us.getSettings(), UserDashboardsInfo.class);
        }
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (stored == null) {
            stored = new UserDashboardsInfo();
        }

        // 根据枚举、状态或协议版本分支，保持不同业务路径的处理语义独立。
        switch (action) {
            case STAR:
                addToStarred(stored, dashboardId);
                break;
            case UNSTAR:
                removeFromStarred(stored, dashboardId);
                break;
            case VISIT:
                addToVisited(stored, dashboardId);
                break;
        }

        stored = refreshDashboardTitles(tenantId, stored);

        us = new UserSettings();
        us.setUserId(id);
        us.setType(UserSettingsType.VISITED_DASHBOARDS);
        us.setSettings(JacksonUtil.valueToTree(stored));
        saveUserSettings(tenantId, us);
        return stored;
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `addToVisited` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void addToVisited(UserDashboardsInfo stored, DashboardId dashboardId) {
        UUID id = dashboardId.getId();
        long ts = System.currentTimeMillis();
        var opt = stored.getLast().stream().filter(filterById(id)).findFirst();
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (opt.isPresent()) {
            opt.get().setLastVisited(ts);
        } else {
            var newInfo = new LastVisitedDashboardInfo();
            newInfo.setId(id);
            newInfo.setStarred(stored.getStarred().stream().anyMatch(filterById(id)));
            newInfo.setLastVisited(System.currentTimeMillis());
            stored.getLast().add(newInfo);
        }
        stored.getLast().sort(Comparator.comparing(LastVisitedDashboardInfo::getLastVisited).reversed());
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (stored.getLast().size() > MAX_DASHBOARD_INFO_LIST_SIZE) {
            stored.setLast(stored.getLast().stream().limit(MAX_DASHBOARD_INFO_LIST_SIZE).collect(Collectors.toList()));
        }
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `removeFromStarred` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void removeFromStarred(UserDashboardsInfo stored, DashboardId dashboardId) {
        UUID id = dashboardId.getId();
        stored.getStarred().removeIf(filterById(id));
        stored.getLast().stream().filter(d -> id.equals(d.getId())).findFirst().ifPresent(d -> d.setStarred(false));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `addToStarred` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private void addToStarred(UserDashboardsInfo stored, DashboardId dashboardId) {
        UUID id = dashboardId.getId();
        long ts = System.currentTimeMillis();
        var opt = stored.getStarred().stream().filter(filterById(id)).findFirst();
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (opt.isPresent()) {
            opt.get().setStarredAt(ts);
        } else {
            var newInfo = new StarredDashboardInfo();
            newInfo.setId(id);
            newInfo.setStarredAt(System.currentTimeMillis());
            stored.getStarred().add(newInfo);
        }
        stored.getStarred().sort(Comparator.comparing(StarredDashboardInfo::getStarredAt).reversed());
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (stored.getStarred().size() > MAX_DASHBOARD_INFO_LIST_SIZE) {
            stored.setStarred(stored.getStarred().stream().limit(MAX_DASHBOARD_INFO_LIST_SIZE).collect(Collectors.toList()));
        }
        Set<UUID> starredMap =
                stored.getStarred().stream().map(AbstractUserDashboardInfo::getId).collect(Collectors.toSet());
        stored.getLast().forEach(d -> d.setStarred(starredMap.contains(d.getId())));
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `filterById` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private Predicate<AbstractUserDashboardInfo> filterById(UUID id) {
        return d -> id.equals(d.getId());
    }

    /**
     * 方法说明：
     * 1. 职责：执行 `refreshDashboardTitles` 对应的业务服务类型流程，完成参数校验、状态读取、消息路由或结果转换。
     * 2. 参数：输入参数由调用方提供，通常代表请求 DTO、实体标识、租户/用户上下文、队列消息、Actor 消息或测试数据。
     * 3. 返回值：返回处理结果、响应 DTO、异步句柄或状态对象；`void` 方法通常通过副作用、回调或异常表达结果。
     * 4. 调用时机：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用时由 Controller、Service、Actor、队列消费者、Transport 处理器或测试框架调用。
     * 5. 使用流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
     * 6. 线程安全：方法本身不隐式保证线程安全；单例 Bean、Actor 消息和异步回调需要依赖外层并发模型。
     * 7. 事务/缓存/MQTT/Actor/数据库/Rule Engine：是否直接涉及取决于实现体中的 DAO、缓存、队列、Transport、Actor 或规则引擎调用。
     */
    private UserDashboardsInfo refreshDashboardTitles(TenantId tenantId, UserDashboardsInfo stored) {
        // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
        if (stored == null) {
            return UserDashboardsInfo.EMPTY;
        }
        stored.getLast().forEach(i -> i.setTitle(null));
        stored.getStarred().forEach(i -> i.setTitle(null));

        Set<UUID> uniqueIds = new HashSet<>();
        stored.getLast().stream().map(AbstractUserDashboardInfo::getId).forEach(uniqueIds::add);
        stored.getStarred().stream().map(AbstractUserDashboardInfo::getId).forEach(uniqueIds::add);

        Map<UUID, String> dashboardTitles = new HashMap<>();
        uniqueIds.forEach(id -> {
                    var title = dashboardService.findDashboardTitleById(tenantId, new DashboardId(id));
                    // 条件分支用于保护权限、状态或参数边界，避免无效请求进入后续链路。
                    if (StringUtils.isNotEmpty(title)) {
                        dashboardTitles.put(id, title);
                    }
                }
        );

        stored.getLast().forEach(i -> i.setTitle(dashboardTitles.get(i.getId())));
        stored.getLast().removeIf(EMPTY_TITLE);
        stored.getStarred().forEach(i -> i.setTitle(dashboardTitles.get(i.getId())));
        stored.getStarred().removeIf(EMPTY_TITLE);
        return stored;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultTbUserSettingsService` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
