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
package org.thingsboard.server.service.sync.ie.importing.csv;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import lombok.Data;
import lombok.SneakyThrows;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasAdditionalInfo;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.DataType;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.sync.ie.importing.csv.BulkImportColumnType;
import org.thingsboard.server.common.data.sync.ie.importing.csv.BulkImportRequest;
import org.thingsboard.server.common.data.sync.ie.importing.csv.BulkImportResult;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.adaptor.JsonConverter;
import org.thingsboard.server.controller.BaseController;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.service.action.EntityActionService;
import org.thingsboard.server.service.security.AccessValidator;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.AccessControlService;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;
import org.thingsboard.server.utils.CsvUtils;
import org.thingsboard.server.common.data.util.TypeCastUtil;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 中文说明：
 * 1. `AbstractBulkImportService` 是 ThingsBoard Application 中负责 `Bulk Import` 的业务服务。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 直接依赖的类型边界包括 `HasId`。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
public abstract class AbstractBulkImportService<E extends HasId<? extends EntityId> & HasTenantId> {
    /**
     * 订阅，提供当前类调用的业务操作。
     */
    @Autowired
    private TelemetrySubscriptionService tsSubscriptionService;
    /**
     * 租户对象，用于描述当前业务场景。
     */
    @Autowired
    private TbTenantProfileCache tenantProfileCache;
    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    private AccessControlService accessControlService;
    /**
     * 校验器，封装可复用的处理规则。
     */
    @Autowired
    private AccessValidator accessValidator;
    /**
     * 实体，提供当前类调用的业务操作。
     */
    @Autowired
    private EntityActionService entityActionService;

    /**
     * 执行器，负责处理对应任务或消息。
     */
    private ThreadPoolExecutor executor;

    /**
     * 功能：初始化或启动执行器。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    private void initExecutor() {
        if (executor == null) {
            executor = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().availableProcessors(),
                    60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(150_000),
                    ThingsBoardThreadFactory.forName("bulk-import"), new ThreadPoolExecutor.CallerRunsPolicy());
            executor.allowCoreThreadTimeOut(true);
        }
    }

    /**
     * 功能：处理`Bulk Import`。
     * 参数：
     * - `request`：请求对象。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    public final BulkImportResult<E> processBulkImport(BulkImportRequest request, SecurityUser user) throws Exception {
        List<EntityData> entitiesData = parseData(request);

        BulkImportResult<E> result = new BulkImportResult<>();
        CountDownLatch completionLatch = new CountDownLatch(entitiesData.size());

        SecurityContext securityContext = SecurityContextHolder.getContext();

        entitiesData.forEach(entityData -> DonAsynchron.submit(() -> {
                    SecurityContextHolder.setContext(securityContext);

                    ImportedEntityInfo<E> importedEntityInfo = saveEntity(entityData.getFields(), user);
                    E entity = importedEntityInfo.getEntity();

                    if (request.getMapping().getUpdate() || !importedEntityInfo.isUpdated()) {
                        saveKvs(user, entity, entityData.getKvs());
                    }
                    return importedEntityInfo;
                },
                importedEntityInfo -> {
                    if (importedEntityInfo.isUpdated()) {
                        result.getUpdated().incrementAndGet();
                    } else {
                        result.getCreated().incrementAndGet();
                    }
                    completionLatch.countDown();
                },
                throwable -> {
                    result.getErrors().incrementAndGet();
                    result.getErrorsList().add(String.format("Line %d: %s", entityData.getLineNumber(), ExceptionUtils.getRootCauseMessage(throwable)));
                    completionLatch.countDown();
                },
                executor));

        completionLatch.await();
        return result;
    }

    /**
     * 功能：保存或创建实体。
     * 参数：
     * - `fields`：键值映射。
     * - `user`：`user` 参数。
     * 返回：处理结果。
     */
    @SneakyThrows
    private ImportedEntityInfo<E> saveEntity(Map<BulkImportColumnType, String> fields, SecurityUser user) {
        ImportedEntityInfo<E> importedEntityInfo = new ImportedEntityInfo<>();

        E entity = findOrCreateEntity(user.getTenantId(), fields.get(BulkImportColumnType.NAME));
        if (entity.getId() != null) {
            importedEntityInfo.setOldEntity((E) entity.getClass().getConstructor(entity.getClass()).newInstance(entity));
            importedEntityInfo.setUpdated(true);
        } else {
            setOwners(entity, user);
        }

        setEntityFields(entity, fields);
        accessControlService.checkPermission(user, Resource.of(getEntityType()), Operation.WRITE, entity.getId(), entity);

        E savedEntity = saveEntity(user, entity, fields);

        importedEntityInfo.setEntity(savedEntity);
        return importedEntityInfo;
    }


    /**
     * 功能：获取实体。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `name`：名称。
     * 返回：处理结果。
     */
    protected abstract E findOrCreateEntity(TenantId tenantId, String name);

    /**
     * 功能：更新`Owners`。
     * 参数：
     * - `entity`：实体对象。
     * - `user`：`user` 参数。
     * 返回：无。
     */
    protected abstract void setOwners(E entity, SecurityUser user);

    /**
     * 功能：更新实体。
     * 参数：
     * - `entity`：实体对象。
     * - `fields`：键值映射。
     * 返回：无。
     */
    protected abstract void setEntityFields(E entity, Map<BulkImportColumnType, String> fields);

    /**
     * 功能：保存或创建实体。
     * 参数：
     * - `user`：`user` 参数。
     * - `entity`：实体对象。
     * - `fields`：键值映射。
     * 返回：处理结果。
     */
    protected abstract E saveEntity(SecurityUser user, E entity, Map<BulkImportColumnType, String> fields);

    /**
     * 功能：获取实体。
     * 参数：无。
     * 返回：处理结果。
     */
    protected abstract EntityType getEntityType();

    /**
     * 功能：获取扩展信息。
     * 参数：
     * - `entity`：实体对象。
     * 返回：处理结果。
     */
    protected ObjectNode getOrCreateAdditionalInfoObj(HasAdditionalInfo entity) {
        return entity.getAdditionalInfo() == null || entity.getAdditionalInfo().isNull() ?
                JacksonUtil.newObjectNode() : (ObjectNode) entity.getAdditionalInfo();
    }

    /**
     * 功能：保存或创建`Kvs`。
     * 参数：
     * - `user`：`user` 参数。
     * - `entity`：实体对象。
     * - `data`：待处理数据。
     * 返回：无。
     */
    private void saveKvs(SecurityUser user, E entity, Map<BulkImportRequest.ColumnMapping, ParsedValue> data) {
        Arrays.stream(BulkImportColumnType.values())
                .filter(BulkImportColumnType::isKv)
                .map(kvType -> {
                    JsonObject kvs = new JsonObject();
                    data.entrySet().stream()
                            .filter(dataEntry -> dataEntry.getKey().getType() == kvType &&
                                    StringUtils.isNotEmpty(dataEntry.getKey().getKey()))
                            .forEach(dataEntry -> kvs.add(dataEntry.getKey().getKey(), dataEntry.getValue().toJsonPrimitive()));
                    return Map.entry(kvType, kvs);
                })
                .filter(kvsEntry -> kvsEntry.getValue().entrySet().size() > 0)
                .forEach(kvsEntry -> {
                    BulkImportColumnType kvType = kvsEntry.getKey();
                    if (kvType == BulkImportColumnType.SHARED_ATTRIBUTE || kvType == BulkImportColumnType.SERVER_ATTRIBUTE) {
                        saveAttributes(user, entity, kvsEntry, kvType);
                    } else {
                        saveTelemetry(user, entity, kvsEntry);
                    }
                });
    }

    /**
     * 功能：保存或创建遥测。
     * 参数：
     * - `user`：`user` 参数。
     * - `entity`：实体对象。
     * - `kvsEntry`：键值映射。
     * 返回：无。
     */
    @SneakyThrows
    private void saveTelemetry(SecurityUser user, E entity, Map.Entry<BulkImportColumnType, JsonObject> kvsEntry) {
        List<TsKvEntry> timeseries = JsonConverter.convertToTelemetry(kvsEntry.getValue(), System.currentTimeMillis())
                .entrySet().stream()
                .flatMap(entry -> entry.getValue().stream().map(kvEntry -> new BasicTsKvEntry(entry.getKey(), kvEntry)))
                .collect(Collectors.toList());

        accessValidator.validateEntityAndCallback(user, Operation.WRITE_TELEMETRY, entity.getId(), (result, tenantId, entityId) -> {
            TenantProfile tenantProfile = tenantProfileCache.get(tenantId);
            long tenantTtl = TimeUnit.DAYS.toSeconds(((DefaultTenantProfileConfiguration) tenantProfile.getProfileData().getConfiguration()).getDefaultStorageTtlDays());
            tsSubscriptionService.saveAndNotify(tenantId, user.getCustomerId(), entityId, timeseries, tenantTtl, new FutureCallback<Void>() {
                @Override
                public void onSuccess(@Nullable Void tmp) {
                    entityActionService.logEntityAction(user, (UUIDBased & EntityId) entityId, null, null,
                            ActionType.TIMESERIES_UPDATED, null, timeseries);
                }

                @Override
                public void onFailure(Throwable t) {
                    entityActionService.logEntityAction(user, (UUIDBased & EntityId) entityId, null, null,
                            ActionType.TIMESERIES_UPDATED, BaseController.toException(t), timeseries);
                    throw new RuntimeException(t);
                }
            });
        });
    }

    /**
     * 功能：保存或创建`Attributes`。
     * 参数：
     * - `user`：`user` 参数。
     * - `entity`：实体对象。
     * - `kvsEntry`：键值映射。
     * - `kvType`：类型。
     * 返回：无。
     */
    @SneakyThrows
    private void saveAttributes(SecurityUser user, E entity, Map.Entry<BulkImportColumnType, JsonObject> kvsEntry, BulkImportColumnType kvType) {
        String scope = kvType.getKey();
        List<AttributeKvEntry> attributes = new ArrayList<>(JsonConverter.convertToAttributes(kvsEntry.getValue()));

        accessValidator.validateEntityAndCallback(user, Operation.WRITE_ATTRIBUTES, entity.getId(), (result, tenantId, entityId) -> {
            tsSubscriptionService.saveAndNotify(tenantId, entityId, scope, attributes, new FutureCallback<>() {

                @Override
                public void onSuccess(Void unused) {
                    entityActionService.logEntityAction(user, (UUIDBased & EntityId) entityId, null,
                            null, ActionType.ATTRIBUTES_UPDATED, null, scope, attributes);
                }

                @Override
                public void onFailure(Throwable throwable) {
                    entityActionService.logEntityAction(user, (UUIDBased & EntityId) entityId, null,
                            null, ActionType.ATTRIBUTES_UPDATED, BaseController.toException(throwable),
                            scope, attributes);
                    throw new RuntimeException(throwable);
                }

            });
        });
    }

    /**
     * 功能：解析数据。
     * 参数：
     * - `request`：请求对象。
     * 返回：匹配的数据集合。
     */
    private List<EntityData> parseData(BulkImportRequest request) throws Exception {
        List<List<String>> records = CsvUtils.parseCsv(request.getFile(), request.getMapping().getDelimiter());
        AtomicInteger linesCounter = new AtomicInteger(0);

        if (request.getMapping().getHeader()) {
            records.remove(0);
            linesCounter.incrementAndGet();
        }

        List<BulkImportRequest.ColumnMapping> columnsMappings = request.getMapping().getColumns();
        return records.stream()
                .map(record -> {
                    EntityData entityData = new EntityData();
                    Stream.iterate(0, i -> i < record.size(), i -> i + 1)
                            .map(i -> Map.entry(columnsMappings.get(i), record.get(i)))
                            .filter(entry -> StringUtils.isNotEmpty(entry.getValue()))
                            .forEach(entry -> {
                                if (!entry.getKey().getType().isKv()) {
                                    entityData.getFields().put(entry.getKey().getType(), entry.getValue());
                                } else {
                                    Pair<DataType, Object> castResult = TypeCastUtil.castValue(entry.getValue());
                                    entityData.getKvs().put(entry.getKey(), new ParsedValue(castResult.getValue(), castResult.getKey()));
                                }
                            });
                    entityData.setLineNumber(linesCounter.incrementAndGet());
                    return entityData;
                })
                .collect(Collectors.toList());
    }

    /**
     * 功能：停止或关闭执行器。
     * 参数：无。
     * 返回：无。
     */
    @PreDestroy
    private void shutdownExecutor() {
        if (!executor.isTerminating()) {
            executor.shutdown();
        }
    }

    /**
     * 中文说明：
     * 1. `EntityData` 是 ThingsBoard Application 中承载实体信息的数据类型。
     * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
     * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
     * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
     * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
     * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
     */
    @Data
    protected static class EntityData {
        private final Map<BulkImportColumnType, String> fields = new LinkedHashMap<>();
        private final Map<BulkImportRequest.ColumnMapping, ParsedValue> kvs = new LinkedHashMap<>();
        /**
         * `lineNumber` 字段，保存当前对象的对应属性。
         */
        private int lineNumber;
    }

    /**
     * 中文说明：
     * 1. `ParsedValue` 是 ThingsBoard Application 中围绕 `Parsed Value` 提供具体能力的类型。
     * 2. 它封装当前声明对应的核心操作和必要状态。
     * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
     * 4. 它直接协作于构造参数、字段类型和公开方法涉及的对象。
     * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
     * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
     */
    @Data
    protected static class ParsedValue {
        /**
         * 值，保存当前处理得到的具体内容。
         */
        private final Object value;
        private final DataType dataType;

        /**
         * 功能：执行 `toJsonPrimitive` 对应的处理。
         * 参数：无。
         * 返回：处理结果。
         */
        public JsonPrimitive toJsonPrimitive() {
            switch (dataType) {
                case STRING:
                    return new JsonPrimitive((String) value);
                case LONG:
                    return new JsonPrimitive((Long) value);
                case DOUBLE:
                    return new JsonPrimitive((Double) value);
                case BOOLEAN:
                    return new JsonPrimitive((Boolean) value);
                default:
                    return null;
            }
        }

        /**
         * 功能：执行 `stringValue` 对应的处理。
         * 参数：无。
         * 返回：文本结果。
         */
        public String stringValue() {
            return value.toString();
        }

    }

}
