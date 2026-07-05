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
package org.thingsboard.server.service.subscription;

import org.apache.commons.lang3.StringUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DataType;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.KeyValueProto;
import org.thingsboard.server.gen.transport.TransportProtos.KeyValueType;
import org.thingsboard.server.gen.transport.TransportProtos.SubscriptionMgrMsgProto;
import org.thingsboard.server.gen.transport.TransportProtos.TbAlarmDeleteProto;
import org.thingsboard.server.gen.transport.TransportProtos.TbAlarmUpdateProto;
import org.thingsboard.server.gen.transport.TransportProtos.TbAttributeDeleteProto;
import org.thingsboard.server.gen.transport.TransportProtos.TbAttributeUpdateProto;
import org.thingsboard.server.gen.transport.TransportProtos.TbEntitySubEventProto;
import org.thingsboard.server.gen.transport.TransportProtos.TbTimeSeriesDeleteProto;
import org.thingsboard.server.gen.transport.TransportProtos.TbTimeSeriesUpdateProto;
import org.thingsboard.server.gen.transport.TransportProtos.ToCoreMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToCoreNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TsKvProto;
import org.thingsboard.server.service.ws.notification.sub.NotificationRequestUpdate;
import org.thingsboard.server.service.ws.notification.sub.NotificationUpdate;
import org.thingsboard.server.service.ws.notification.sub.NotificationsSubscriptionUpdate;
import org.thingsboard.server.service.ws.telemetry.sub.AlarmSubscriptionUpdate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * 中文说明：
 * 1. 类目的：`TbSubscriptionUtils` 是ThingsBoard Application 模块中的业务服务类型，用于承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 生命周期：由 Spring 容器创建为单例服务，按请求、队列消息或调度任务调用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Service / Facade。
 */
public class TbSubscriptionUtils {

    /**
     * 功能：执行 `toSubEventProto` 对应的处理。
     * 参数：
     * - `serviceId`：服务ID。
     * - `event`：`event` 参数。
     * 返回：处理结果。
     */
    public static ToCoreMsg toSubEventProto(String serviceId, TbEntitySubEvent event) {
        SubscriptionMgrMsgProto.Builder msgBuilder = SubscriptionMgrMsgProto.newBuilder();
        var builder = TbEntitySubEventProto.newBuilder()
                .setServiceId(serviceId)
                .setSeqNumber(event.getSeqNumber())
                .setTenantIdMSB(event.getTenantId().getId().getMostSignificantBits())
                .setTenantIdLSB(event.getTenantId().getId().getLeastSignificantBits())
                .setEntityType(event.getEntityId().getEntityType().name())
                .setEntityIdMSB(event.getEntityId().getId().getMostSignificantBits())
                .setEntityIdLSB(event.getEntityId().getId().getLeastSignificantBits())
                .setType(event.getType().name());
        TbSubscriptionsInfo info = event.getInfo();
        if (info != null) {
            builder.setNotifications(info.notifications)
                    .setAlarms(info.alarms)
                    .setTsAllKeys(info.tsAllKeys)
                    .setAttrAllKeys(info.attrAllKeys);
            if (info.tsKeys != null) {
                builder.addAllTsKeys(info.tsKeys);
            }
            if (info.attrKeys != null) {
                builder.addAllAttrKeys(info.attrKeys);
            }
        }
        msgBuilder.setSubEvent(builder);
        return ToCoreMsg.newBuilder().setToSubscriptionMgrMsg(msgBuilder).build();
    }

    /**
     * 功能：执行 `toProto` 对应的处理。
     * 参数：
     * - `id`：`id`ID。
     * - `seqNumber`：`seqNumber` 参数。
     * - `update`：`update` 参数。
     * 返回：处理结果。
     */
    public static ToCoreNotificationMsg toProto(UUID id, int seqNumber, TbEntityUpdatesInfo update) {
        TransportProtos.TbEntitySubEventCallbackProto.Builder updateProto = TransportProtos.TbEntitySubEventCallbackProto.newBuilder()
                .setEntityIdMSB(id.getMostSignificantBits())
                .setEntityIdLSB(id.getLeastSignificantBits())
                .setSeqNumber(seqNumber)
                .setAttributesUpdateTs(update.attributesUpdateTs)
                .setTimeSeriesUpdateTs(update.timeSeriesUpdateTs);
        return ToCoreNotificationMsg.newBuilder()
                .setToLocalSubscriptionServiceMsg(
                        TransportProtos.LocalSubscriptionServiceMsgProto.newBuilder()
                                .setSubEventCallback(updateProto)
                                .build())
                .build();
    }


    /**
     * 功能：执行 `fromProto` 对应的处理。
     * 参数：
     * - `proto`：`proto` 参数。
     * 返回：处理结果。
     */
    public static TbEntitySubEvent fromProto(TbEntitySubEventProto proto) {
        ComponentLifecycleEvent event = ComponentLifecycleEvent.valueOf(proto.getType());
        var builder = TbEntitySubEvent.builder()
                .tenantId(TenantId.fromUUID(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB())))
                .seqNumber(proto.getSeqNumber())
                .entityId(EntityIdFactory.getByTypeAndUuid(proto.getEntityType(), new UUID(proto.getEntityIdMSB(), proto.getEntityIdLSB())))
                .type(event);
        if (!ComponentLifecycleEvent.DELETED.equals(event)) {
            builder.info(new TbSubscriptionsInfo(proto.getNotifications(), proto.getAlarms(),
                    proto.getTsAllKeys(), proto.getTsKeysCount() > 0 ? new HashSet<>(proto.getTsKeysList()) : null,
                    proto.getAttrAllKeys(), proto.getAttrKeysCount() > 0 ? new HashSet<>(proto.getAttrKeysList()) : null,
                    proto.getSeqNumber()));
        }
        return builder.build();
    }

    /**
     * 功能：执行 `fromProto` 对应的处理。
     * 参数：
     * - `proto`：`proto` 参数。
     * 返回：处理结果。
     */
    public static AlarmSubscriptionUpdate fromProto(TransportProtos.TbAlarmSubUpdateProto proto) {
        if (proto.getErrorCode() > 0) {
            return new AlarmSubscriptionUpdate(SubscriptionErrorCode.forCode(proto.getErrorCode()), proto.getErrorMsg());
        } else {
            AlarmInfo alarm = JacksonUtil.fromString(proto.getAlarm(), AlarmInfo.class);
            return new AlarmSubscriptionUpdate(alarm, proto.getDeleted());
        }
    }

    /**
     * 功能：执行 `fromProto` 对应的处理。
     * 参数：
     * - `proto`：`proto` 参数。
     * 返回：处理结果。
     */
    public static NotificationsSubscriptionUpdate fromProto(TransportProtos.NotificationsSubUpdateProto proto) {
        NotificationsSubscriptionUpdate update;
        if (StringUtils.isNotEmpty(proto.getNotificationUpdate())) {
            NotificationUpdate notificationUpdate = JacksonUtil.fromString(proto.getNotificationUpdate(), NotificationUpdate.class);
            update = new NotificationsSubscriptionUpdate(notificationUpdate);
        } else {
            NotificationRequestUpdate notificationRequestUpdate = JacksonUtil.fromString(proto.getNotificationRequestUpdate(), NotificationRequestUpdate.class);
            update = new NotificationsSubscriptionUpdate(notificationRequestUpdate);
        }
        return update;
    }

    /**
     * 功能：执行 `toAlarmSubUpdateToProto` 对应的处理。
     * 参数：
     * - `entityId`：实体IDID。
     * - `alarmInfo`：`alarmInfo` 参数。
     * - `deleted`：`deleted` 参数。
     * 返回：处理结果。
     */
    public static ToCoreNotificationMsg toAlarmSubUpdateToProto(EntityId entityId, AlarmInfo alarmInfo, boolean deleted) {
        TransportProtos.TbAlarmSubUpdateProto.Builder updateProto = TransportProtos.TbAlarmSubUpdateProto.newBuilder()
                .setEntityIdMSB(entityId.getId().getMostSignificantBits())
                .setEntityIdLSB(entityId.getId().getLeastSignificantBits())
                .setAlarm(JacksonUtil.toString(alarmInfo))
                .setDeleted(deleted);
        return ToCoreNotificationMsg.newBuilder()
                .setToLocalSubscriptionServiceMsg(
                        TransportProtos.LocalSubscriptionServiceMsgProto.newBuilder()
                                .setAlarmUpdate(updateProto)
                                .build())
                .build();
    }

    /**
     * 功能：执行 `notificationsSubUpdateToProto` 对应的处理。
     * 参数：
     * - `entityId`：实体IDID。
     * - `update`：`update` 参数。
     * 返回：处理结果。
     */
    public static ToCoreNotificationMsg notificationsSubUpdateToProto(EntityId entityId, NotificationsSubscriptionUpdate update) {
        TransportProtos.NotificationsSubUpdateProto.Builder updateProto = TransportProtos.NotificationsSubUpdateProto.newBuilder()
                .setEntityIdMSB(entityId.getId().getMostSignificantBits())
                .setEntityIdLSB(entityId.getId().getLeastSignificantBits());
        if (update.getNotificationUpdate() != null) {
            updateProto.setNotificationUpdate(JacksonUtil.toString(update.getNotificationUpdate()));
        }
        if (update.getNotificationRequestUpdate() != null) {
            updateProto.setNotificationRequestUpdate(JacksonUtil.toString(update.getNotificationRequestUpdate()));
        }
        return ToCoreNotificationMsg.newBuilder()
                .setToLocalSubscriptionServiceMsg(TransportProtos.LocalSubscriptionServiceMsgProto.newBuilder()
                        .setNotificationsUpdate(updateProto)
                        .build())
                .build();
    }

    /**
     * 功能：执行 `toTimeseriesUpdateProto` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `ts`：时间戳。
     * 返回：处理结果。
     */
    public static ToCoreMsg toTimeseriesUpdateProto(TenantId tenantId, EntityId entityId, List<TsKvEntry> ts) {
        TbTimeSeriesUpdateProto.Builder builder = TbTimeSeriesUpdateProto.newBuilder();
        builder.setEntityType(entityId.getEntityType().name());
        builder.setEntityIdMSB(entityId.getId().getMostSignificantBits());
        builder.setEntityIdLSB(entityId.getId().getLeastSignificantBits());
        builder.setTenantIdMSB(tenantId.getId().getMostSignificantBits());
        builder.setTenantIdLSB(tenantId.getId().getLeastSignificantBits());
        ts.forEach(v -> builder.addData(toKeyValueProto(v.getTs(), v).build()));
        SubscriptionMgrMsgProto.Builder msgBuilder = SubscriptionMgrMsgProto.newBuilder();
        msgBuilder.setTsUpdate(builder);
        return ToCoreMsg.newBuilder().setToSubscriptionMgrMsg(msgBuilder.build()).build();
    }

    /**
     * 功能：执行 `toTimeseriesDeleteProto` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `keys`：键。
     * 返回：处理结果。
     */
    public static ToCoreMsg toTimeseriesDeleteProto(TenantId tenantId, EntityId entityId, List<String> keys) {
        TbTimeSeriesDeleteProto.Builder builder = TbTimeSeriesDeleteProto.newBuilder();
        builder.setEntityType(entityId.getEntityType().name());
        builder.setEntityIdMSB(entityId.getId().getMostSignificantBits());
        builder.setEntityIdLSB(entityId.getId().getLeastSignificantBits());
        builder.setTenantIdMSB(tenantId.getId().getMostSignificantBits());
        builder.setTenantIdLSB(tenantId.getId().getLeastSignificantBits());
        builder.addAllKeys(keys);
        SubscriptionMgrMsgProto.Builder msgBuilder = SubscriptionMgrMsgProto.newBuilder();
        msgBuilder.setTsDelete(builder);
        return ToCoreMsg.newBuilder().setToSubscriptionMgrMsg(msgBuilder.build()).build();
    }

    /**
     * 功能：执行 `toAttributesUpdateProto` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * - `attributes`：数据列表。
     * 返回：处理结果。
     */
    public static ToCoreMsg toAttributesUpdateProto(TenantId tenantId, EntityId entityId, String scope, List<AttributeKvEntry> attributes) {
        TbAttributeUpdateProto.Builder builder = TbAttributeUpdateProto.newBuilder();
        builder.setEntityType(entityId.getEntityType().name());
        builder.setEntityIdMSB(entityId.getId().getMostSignificantBits());
        builder.setEntityIdLSB(entityId.getId().getLeastSignificantBits());
        builder.setTenantIdMSB(tenantId.getId().getMostSignificantBits());
        builder.setTenantIdLSB(tenantId.getId().getLeastSignificantBits());
        builder.setScope(scope);
        attributes.forEach(v -> builder.addData(toKeyValueProto(v.getLastUpdateTs(), v).build()));

        SubscriptionMgrMsgProto.Builder msgBuilder = SubscriptionMgrMsgProto.newBuilder();
        msgBuilder.setAttrUpdate(builder);
        return ToCoreMsg.newBuilder().setToSubscriptionMgrMsg(msgBuilder.build()).build();
    }

    /**
     * 功能：执行 `toAttributesDeleteProto` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `scope`：`scope` 参数。
     * - `keys`：键。
     * - 其余参数：补充处理条件。
     * 返回：处理结果。
     */
    public static ToCoreMsg toAttributesDeleteProto(TenantId tenantId, EntityId entityId, String scope, List<String> keys, boolean notifyDevice) {
        TbAttributeDeleteProto.Builder builder = TbAttributeDeleteProto.newBuilder();
        builder.setEntityType(entityId.getEntityType().name());
        builder.setEntityIdMSB(entityId.getId().getMostSignificantBits());
        builder.setEntityIdLSB(entityId.getId().getLeastSignificantBits());
        builder.setTenantIdMSB(tenantId.getId().getMostSignificantBits());
        builder.setTenantIdLSB(tenantId.getId().getLeastSignificantBits());
        builder.setScope(scope);
        builder.addAllKeys(keys);
        builder.setNotifyDevice(notifyDevice);

        SubscriptionMgrMsgProto.Builder msgBuilder = SubscriptionMgrMsgProto.newBuilder();
        msgBuilder.setAttrDelete(builder);
        return ToCoreMsg.newBuilder().setToSubscriptionMgrMsg(msgBuilder.build()).build();
    }


    /**
     * 功能：执行 `toKeyValueProto` 对应的处理。
     * 参数：
     * - `ts`：时间戳。
     * - `attr`：`attr` 参数。
     * 返回：处理结果。
     */
    private static TsKvProto.Builder toKeyValueProto(long ts, KvEntry attr) {
        KeyValueProto.Builder dataBuilder = KeyValueProto.newBuilder();
        dataBuilder.setKey(attr.getKey());
        dataBuilder.setType(KeyValueType.forNumber(attr.getDataType().ordinal()));
        switch (attr.getDataType()) {
            case BOOLEAN:
                attr.getBooleanValue().ifPresent(dataBuilder::setBoolV);
                break;
            case LONG:
                attr.getLongValue().ifPresent(dataBuilder::setLongV);
                break;
            case DOUBLE:
                attr.getDoubleValue().ifPresent(dataBuilder::setDoubleV);
                break;
            case JSON:
                attr.getJsonValue().ifPresent(dataBuilder::setJsonV);
                break;
            case STRING:
                attr.getStrValue().ifPresent(dataBuilder::setStringV);
                break;
        }
        return TsKvProto.newBuilder().setTs(ts).setKv(dataBuilder);
    }

    /**
     * 功能：执行 `toTsValueProto` 对应的处理。
     * 参数：
     * - `ts`：时间戳。
     * - `attr`：`attr` 参数。
     * 返回：处理结果。
     */
    private static TransportProtos.TsValueProto toTsValueProto(long ts, KvEntry attr) {
        TransportProtos.TsValueProto.Builder dataBuilder = TransportProtos.TsValueProto.newBuilder();
        dataBuilder.setTs(ts);
        dataBuilder.setType(KeyValueType.forNumber(attr.getDataType().ordinal()));
        switch (attr.getDataType()) {
            case BOOLEAN:
                attr.getBooleanValue().ifPresent(dataBuilder::setBoolV);
                break;
            case LONG:
                attr.getLongValue().ifPresent(dataBuilder::setLongV);
                break;
            case DOUBLE:
                attr.getDoubleValue().ifPresent(dataBuilder::setDoubleV);
                break;
            case JSON:
                attr.getJsonValue().ifPresent(dataBuilder::setJsonV);
                break;
            case STRING:
                attr.getStrValue().ifPresent(dataBuilder::setStringV);
                break;
        }
        return dataBuilder.build();
    }


    /**
     * 功能：执行 `toEntityId` 对应的处理。
     * 参数：
     * - `entityType`：实体对象。
     * - `entityIdMSB`：实体对象。
     * - `entityIdLSB`：实体对象。
     * 返回：处理结果。
     */
    public static EntityId toEntityId(String entityType, long entityIdMSB, long entityIdLSB) {
        return EntityIdFactory.getByTypeAndUuid(entityType, new UUID(entityIdMSB, entityIdLSB));
    }

    /**
     * 功能：执行 `toTsKvEntityList` 对应的处理。
     * 参数：
     * - `dataList`：待处理数据。
     * 返回：匹配的数据集合。
     */
    public static List<TsKvEntry> toTsKvEntityList(List<TsKvProto> dataList) {
        List<TsKvEntry> result = new ArrayList<>(dataList.size());
        dataList.forEach(proto -> result.add(new BasicTsKvEntry(proto.getTs(), getKvEntry(proto.getKv()))));
        return result;
    }

    /**
     * 功能：执行 `toAttributeKvList` 对应的处理。
     * 参数：
     * - `dataList`：待处理数据。
     * 返回：匹配的数据集合。
     */
    public static List<AttributeKvEntry> toAttributeKvList(List<TsKvProto> dataList) {
        List<AttributeKvEntry> result = new ArrayList<>(dataList.size());
        dataList.forEach(proto -> result.add(new BaseAttributeKvEntry(getKvEntry(proto.getKv()), proto.getTs())));
        return result;
    }

    /**
     * 功能：获取`Kv Entry`。
     * 参数：
     * - `proto`：`proto` 参数。
     * 返回：处理结果。
     */
    private static KvEntry getKvEntry(KeyValueProto proto) {
        KvEntry entry = null;
        DataType type = DataType.values()[proto.getType().getNumber()];
        switch (type) {
            case BOOLEAN:
                entry = new BooleanDataEntry(proto.getKey(), proto.getBoolV());
                break;
            case LONG:
                entry = new LongDataEntry(proto.getKey(), proto.getLongV());
                break;
            case DOUBLE:
                entry = new DoubleDataEntry(proto.getKey(), proto.getDoubleV());
                break;
            case STRING:
                entry = new StringDataEntry(proto.getKey(), proto.getStringV());
                break;
            case JSON:
                entry = new JsonDataEntry(proto.getKey(), proto.getJsonV());
                break;
        }
        return entry;
    }

    /**
     * 功能：执行 `toTsKvEntityList` 对应的处理。
     * 参数：
     * - `key`：键。
     * - `dataList`：待处理数据。
     * 返回：匹配的数据集合。
     */
    public static List<TsKvEntry> toTsKvEntityList(String key, List<TransportProtos.TsValueProto> dataList) {
        List<TsKvEntry> result = new ArrayList<>(dataList.size());
        dataList.forEach(proto -> result.add(new BasicTsKvEntry(proto.getTs(), getKvEntry(key, proto))));
        return result;
    }

    /**
     * 功能：获取`Kv Entry`。
     * 参数：
     * - `key`：键。
     * - `proto`：`proto` 参数。
     * 返回：处理结果。
     */
    private static KvEntry getKvEntry(String key, TransportProtos.TsValueProto proto) {
        KvEntry entry = null;
        DataType type = DataType.values()[proto.getType().getNumber()];
        switch (type) {
            case BOOLEAN:
                entry = new BooleanDataEntry(key, proto.getBoolV());
                break;
            case LONG:
                entry = new LongDataEntry(key, proto.getLongV());
                break;
            case DOUBLE:
                entry = new DoubleDataEntry(key, proto.getDoubleV());
                break;
            case STRING:
                entry = new StringDataEntry(key, proto.getStringV());
                break;
            case JSON:
                entry = new JsonDataEntry(key, proto.getJsonV());
                break;
        }
        return entry;
    }

    /**
     * 功能：执行 `toAlarmUpdateProto` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `alarm`：`alarm` 参数。
     * 返回：处理结果。
     */
    public static ToCoreMsg toAlarmUpdateProto(TenantId tenantId, EntityId entityId, AlarmInfo alarm) {
        TbAlarmUpdateProto.Builder builder = TbAlarmUpdateProto.newBuilder();
        builder.setEntityType(entityId.getEntityType().name());
        builder.setEntityIdMSB(entityId.getId().getMostSignificantBits());
        builder.setEntityIdLSB(entityId.getId().getLeastSignificantBits());
        builder.setTenantIdMSB(tenantId.getId().getMostSignificantBits());
        builder.setTenantIdLSB(tenantId.getId().getLeastSignificantBits());
        builder.setAlarm(JacksonUtil.toString(alarm));
        SubscriptionMgrMsgProto.Builder msgBuilder = SubscriptionMgrMsgProto.newBuilder();
        msgBuilder.setAlarmUpdate(builder);
        return ToCoreMsg.newBuilder().setToSubscriptionMgrMsg(msgBuilder.build()).build();
    }

    /**
     * 功能：执行 `toAlarmDeletedProto` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `entityId`：实体IDID。
     * - `alarm`：`alarm` 参数。
     * 返回：处理结果。
     */
    public static ToCoreMsg toAlarmDeletedProto(TenantId tenantId, EntityId entityId, AlarmInfo alarm) {
        TbAlarmDeleteProto.Builder builder = TbAlarmDeleteProto.newBuilder();
        builder.setEntityType(entityId.getEntityType().name());
        builder.setEntityIdMSB(entityId.getId().getMostSignificantBits());
        builder.setEntityIdLSB(entityId.getId().getLeastSignificantBits());
        builder.setTenantIdMSB(tenantId.getId().getMostSignificantBits());
        builder.setTenantIdLSB(tenantId.getId().getLeastSignificantBits());
        builder.setAlarm(JacksonUtil.toString(alarm));
        SubscriptionMgrMsgProto.Builder msgBuilder = SubscriptionMgrMsgProto.newBuilder();
        msgBuilder.setAlarmDelete(builder);
        return ToCoreMsg.newBuilder().setToSubscriptionMgrMsg(msgBuilder.build()).build();
    }

    /**
     * 功能：执行 `notificationUpdateToProto` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `recipientId`：`recipientId`ID。
     * - `notificationUpdate`：`notificationUpdate` 参数。
     * 返回：处理结果。
     */
    public static ToCoreMsg notificationUpdateToProto(TenantId tenantId, UserId recipientId, NotificationUpdate notificationUpdate) {
        TransportProtos.NotificationUpdateProto updateProto = TransportProtos.NotificationUpdateProto.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setRecipientIdMSB(recipientId.getId().getMostSignificantBits())
                .setRecipientIdLSB(recipientId.getId().getLeastSignificantBits())
                .setUpdate(JacksonUtil.toString(notificationUpdate))
                .build();
        return ToCoreMsg.newBuilder()
                .setToSubscriptionMgrMsg(SubscriptionMgrMsgProto.newBuilder()
                        .setNotificationUpdate(updateProto)
                        .build())
                .build();
    }

    /**
     * 功能：执行 `notificationRequestUpdateToProto` 对应的处理。
     * 参数：
     * - `tenantId`：租户IDID。
     * - `notificationRequestUpdate`：请求对象。
     * 返回：处理结果。
     */
    public static ToCoreNotificationMsg notificationRequestUpdateToProto(TenantId tenantId, NotificationRequestUpdate notificationRequestUpdate) {
        TransportProtos.NotificationRequestUpdateProto updateProto = TransportProtos.NotificationRequestUpdateProto.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setUpdate(JacksonUtil.toString(notificationRequestUpdate))
                .build();
        return ToCoreNotificationMsg.newBuilder()
                .setToSubscriptionMgrMsg(SubscriptionMgrMsgProto.newBuilder()
                        .setNotificationRequestUpdate(updateProto)
                        .build())
                .build();
    }

    /**
     * 功能：执行 `fromProto` 对应的处理。
     * 参数：
     * - `proto`：`proto` 参数。
     * 返回：匹配的数据集合。
     */
    public static List<TsKvEntry> fromProto(TransportProtos.TbSubUpdateProto proto) {
        List<TsKvEntry> result = new ArrayList<>();
        for (var p : proto.getDataList()) {
            result.addAll(toTsKvEntityList(p.getKey(), p.getTsValueList()));
        }
        return result;
    }

    /**
     * 功能：执行 `toProto` 对应的处理。
     * 参数：
     * - `entityId`：实体IDID。
     * - `updates`：数据列表。
     * 返回：处理结果。
     */
    static ToCoreNotificationMsg toProto(EntityId entityId, List<TsKvEntry> updates) {
        return toProto(true, null, entityId, updates);
    }

    /**
     * 功能：执行 `toProto` 对应的处理。
     * 参数：
     * - `scope`：`scope` 参数。
     * - `entityId`：实体IDID。
     * - `updates`：数据列表。
     * 返回：处理结果。
     */
    static ToCoreNotificationMsg toProto(String scope, EntityId entityId, List<TsKvEntry> updates) {
        return toProto(false, scope, entityId, updates);
    }

    /**
     * 功能：执行 `toProto` 对应的处理。
     * 参数：
     * - `timeSeries`：`timeSeries` 参数。
     * - `scope`：`scope` 参数。
     * - `entityId`：实体IDID。
     * - `updates`：数据列表。
     * 返回：处理结果。
     */
    static ToCoreNotificationMsg toProto(boolean timeSeries, String scope, EntityId entityId, List<TsKvEntry> updates) {
        TransportProtos.TbSubUpdateProto.Builder builder = TransportProtos.TbSubUpdateProto.newBuilder();

        builder.setEntityIdMSB(entityId.getId().getMostSignificantBits());
        builder.setEntityIdLSB(entityId.getId().getLeastSignificantBits());

        Map<String, List<TransportProtos.TsValueProto>> data = new TreeMap<>();

        for (TsKvEntry tsEntry : updates) {
            data.computeIfAbsent(tsEntry.getKey(), k -> new ArrayList<>()).add(toTsValueProto(tsEntry.getTs(), tsEntry));
        }

        data.forEach((key, value) -> {
            TransportProtos.TsValueListProto.Builder dataBuilder = TransportProtos.TsValueListProto.newBuilder();
            dataBuilder.setKey(key);
            dataBuilder.addAllTsValue(value);
            builder.addData(dataBuilder.build());
        });

        var result = TransportProtos.LocalSubscriptionServiceMsgProto.newBuilder();
        if (timeSeries) {
            result.setTsUpdate(builder);
        } else {
            builder.setScope(scope);
            result.setAttrUpdate(builder);
        }
        return ToCoreNotificationMsg.newBuilder().setToLocalSubscriptionServiceMsg(result).build();
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`TbSubscriptionUtils` 在 ThingsBoard Application 模块 中承担业务服务类型职责，核心目的是承载 ThingsBoard 服务端应用的业务编排、实体访问和异步处理。
 * 2. 核心流程：校验输入后调用 DAO 或外部服务，更新状态并发布事件或队列消息。
 * 3. 关键依赖：主要依赖或协作对象包括Controller、DAO、缓存、队列、Actor、Transport、Rule Engine 和审计服务。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
