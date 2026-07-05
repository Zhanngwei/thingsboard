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
package org.thingsboard.server.dao;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.util.CollectionUtils;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.dao.model.ToData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. 类目的：`DaoUtil` 是 ThingsBoard DAO 模块 中的DAO 工具和配置类型，用于提供数据库类型判断、SQL 初始化、分页转换、异常包装和通用 DAO 辅助逻辑。
 * 2. 所属模块：位于 dao 模块，处在 ThingsBoard 服务端的数据访问和持久化实现层。
 * 3. 协作对象：主要协作对象包括DAO 实现、Spring 配置、数据库初始化脚本、Repository 和测试套件。
 * 4. 生命周期：通常作为静态工具、配置 Bean 或轻量对象按需调用，不持有长生命周期业务状态。
 * 5. 设计原因：单独建模该类型可以隔离 DAO API、业务服务、缓存和具体数据库实现，避免上层模块直接依赖 SQL、Cassandra 或测试容器细节。
 * 6. 事务与缓存：直接或间接涉及数据库访问，事务边界通常由 Spring 服务层或测试事务管理器控制；是否触发缓存取决于实现体中的 cache、evict 或 Redis/Caffeine 调用。
 * 7. MQTT/Actor/Rule Engine：DAO 层通常不直接处理 MQTT 或 Actor 消息，但设备、遥测、规则链等数据变更会被 Transport、Actor 或 Rule Engine 间接消费。
 * 8. 设计模式：主要体现 Utility / Factory / Template。
 */
public abstract class DaoUtil {

    /**
     * 功能：创建 `DaoUtil` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    private DaoUtil() {
    }

    /**
     * 功能：执行 `toPageData` 对应的处理。
     * 参数：
     * - `page`：`page` 参数。
     * 返回：匹配的数据集合。
     */
    public static <T> PageData<T> toPageData(Page<? extends ToData<T>> page) {
        List<T> data = convertDataList(page.getContent());
        return new PageData<>(data, page.getTotalPages(), page.getTotalElements(), page.hasNext());
    }

    /**
     * 功能：执行 `pageToPageData` 对应的处理。
     * 参数：
     * - `page`：`page` 参数。
     * 返回：匹配的数据集合。
     */
    public static <T> PageData<T> pageToPageData(Page<T> page) {
        return new PageData<>(page.getContent(), page.getTotalPages(), page.getTotalElements(), page.hasNext());
    }

    /**
     * 功能：执行 `toPageable` 对应的处理。
     * 参数：
     * - `pageLink`：`pageLink` 参数。
     * 返回：匹配的数据集合。
     */
    public static Pageable toPageable(PageLink pageLink) {
        return toPageable(pageLink, Collections.emptyMap());
    }

    /**
     * 功能：执行 `toPageable` 对应的处理。
     * 参数：
     * - `pageLink`：`pageLink` 参数。
     * - `columnMap`：键值映射。
     * 返回：匹配的数据集合。
     */
    public static Pageable toPageable(PageLink pageLink, Map<String, String> columnMap) {
        return PageRequest.of(pageLink.getPage(), pageLink.getPageSize(), pageLink.toSort(pageLink.getSortOrder(), columnMap));
    }

    /**
     * 功能：执行 `toPageable` 对应的处理。
     * 参数：
     * - `pageLink`：`pageLink` 参数。
     * - `sortOrders`：数据列表。
     * 返回：匹配的数据集合。
     */
    public static Pageable toPageable(PageLink pageLink, List<SortOrder> sortOrders) {
        return toPageable(pageLink, Collections.emptyMap(), sortOrders);
    }

    /**
     * 功能：执行 `toPageable` 对应的处理。
     * 参数：
     * - `pageLink`：`pageLink` 参数。
     * - `columnMap`：键值映射。
     * - `sortOrders`：数据列表。
     * 返回：匹配的数据集合。
     */
    public static Pageable toPageable(PageLink pageLink, Map<String, String> columnMap, List<SortOrder> sortOrders) {
        return PageRequest.of(pageLink.getPage(), pageLink.getPageSize(), pageLink.toSort(sortOrders, columnMap));
    }

    /**
     * 功能：转换数据。
     * 参数：
     * - `toDataList`：待处理数据。
     * 返回：匹配的数据集合。
     */
    public static <T> List<T> convertDataList(Collection<? extends ToData<T>> toDataList) {
        List<T> list = Collections.emptyList();
        if (toDataList != null && !toDataList.isEmpty()) {
            list = new ArrayList<>();
            for (ToData<T> object : toDataList) {
                if (object != null) {
                    list.add(object.toData());
                }
            }
        }
        return list;
    }

    /**
     * 功能：获取数据。
     * 参数：
     * - `data`：待处理数据。
     * 返回：处理结果。
     */
    public static <T> T getData(ToData<T> data) {
        T object = null;
        if (data != null) {
            object = data.toData();
        }
        return object;
    }

    /**
     * 功能：获取数据。
     * 参数：
     * - `data`：待处理数据。
     * 返回：处理结果。
     */
    public static <T> T getData(Optional<? extends ToData<T>> data) {
        T object = null;
        if (data.isPresent()) {
            object = data.get().toData();
        }
        return object;
    }

    /**
     * 功能：获取`Id`。
     * 参数：
     * - `idBased`：`idBased` 参数。
     * 返回：处理结果。
     */
    public static UUID getId(UUIDBased idBased) {
        UUID id = null;
        if (idBased != null) {
            id = idBased.getId();
        }
        return id;
    }

    /**
     * 功能：执行 `toUUIDs` 对应的处理。
     * 参数：
     * - `idBasedIds`：数据列表。
     * 返回：匹配的数据集合。
     */
    public static List<UUID> toUUIDs(List<? extends UUIDBased> idBasedIds) {
        List<UUID> ids = new ArrayList<>();
        for (UUIDBased idBased : idBasedIds) {
            ids.add(getId(idBased));
        }
        return ids;
    }

    /**
     * 功能：执行 `fromUUIDs` 对应的处理。
     * 参数：
     * - `uuids`：数据列表。
     * - `mapper`：`mapper` 参数。
     * 返回：匹配的数据集合。
     */
    public static <I> List<I> fromUUIDs(List<UUID> uuids, Function<UUID, I> mapper) {
        return uuids.stream().map(mapper).collect(Collectors.toList());
    }

    /**
     * 功能：执行 `toEntityId` 对应的处理。
     * 参数：
     * - `uuid`：`uuid`ID。
     * - `creator`：`creator` 参数。
     * 返回：处理结果。
     */
    public static <I> I toEntityId(UUID uuid, Function<UUID, I> creator) {
        if (uuid != null) {
            return creator.apply(uuid);
        } else {
            return null;
        }
    }

    /**
     * 功能：处理`In Batches`。
     * 参数：
     * - `finder`：`finder` 参数。
     * - `batchSize`：`batchSize` 参数。
     * - `processor`：处理器对象。
     * 返回：无。
     */
    public static <T> void processInBatches(Function<PageLink, PageData<T>> finder, int batchSize, Consumer<T> processor) {
        processBatches(finder, batchSize, batch -> batch.getData().forEach(processor));
    }

    /**
     * 功能：处理`Batches`。
     * 参数：
     * - `finder`：`finder` 参数。
     * - `batchSize`：`batchSize` 参数。
     * - `processor`：处理器对象。
     * 返回：无。
     */
    public static <T> void processBatches(Function<PageLink, PageData<T>> finder, int batchSize, Consumer<PageData<T>> processor) {
        PageLink pageLink = new PageLink(batchSize);
        PageData<T> batch;

        boolean hasNextBatch;
        do {
            batch = finder.apply(pageLink);
            processor.accept(batch);

            hasNextBatch = batch.hasNext();
            pageLink = pageLink.nextPageLink();
        } while (hasNextBatch);
    }

    /**
     * 功能：获取`String Id`。
     * 参数：
     * - `id`：`id`ID。
     * 返回：文本结果。
     */
    public static String getStringId(UUIDBased id) {
        if (id != null) {
            return id.toString();
        } else {
            return null;
        }
    }

    /**
     * 功能：转换租户。
     * 参数：
     * - `tenantUUID`：租户ID。
     * - `entityType`：实体对象。
     * - `types`：类型。
     * 返回：匹配的数据集合。
     */
    public static List<EntitySubtype> convertTenantEntityTypesToDto(UUID tenantUUID, EntityType entityType, List<String> types) {
        if (CollectionUtils.isEmpty(types)) {
            return Collections.emptyList();
        }
        TenantId tenantId = TenantId.fromUUID(tenantUUID);
        return types.stream()
                .map(type -> new EntitySubtype(tenantId, entityType, type))
                .collect(Collectors.toList());
    }

    /**
     * 功能：转换租户。
     * 参数：
     * - `tenantUUID`：租户ID。
     * - `entityType`：实体对象。
     * - `entityInfos`：实体对象。
     * 返回：匹配的数据集合。
     */
    @Deprecated // used only in deprecated DAO api
    public static List<EntitySubtype> convertTenantEntityInfosToDto(UUID tenantUUID, EntityType entityType, List<EntityInfo> entityInfos) {
        if (CollectionUtils.isEmpty(entityInfos)) {
            return Collections.emptyList();
        }
        var tenantId = TenantId.fromUUID(tenantUUID);
        return entityInfos.stream()
                .map(info -> new EntitySubtype(tenantId, entityType, info.getName()))
                .sorted(Comparator.comparing(EntitySubtype::getType))
                .collect(Collectors.toList());
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DaoUtil` 在 ThingsBoard DAO 模块 中承担DAO 工具和配置类型职责，核心目的是提供数据库类型判断、SQL 初始化、分页转换、异常包装和通用 DAO 辅助逻辑。
 * 2. 核心流程：接收 DAO 层输入后完成转换、初始化或辅助判断，并把结果交回具体持久化流程。
 * 3. 关键依赖：主要依赖或协作对象包括DAO 实现、Spring 配置、数据库初始化脚本、Repository 和测试套件。
 * 4. 学习重点：阅读本文件时应关注租户/实体作用域、事务边界、缓存失效、SQL/NoSQL 差异、数据库异常转换，以及数据变更对 Rule Engine、Transport、Actor 和审计链路的间接影响。
 */
