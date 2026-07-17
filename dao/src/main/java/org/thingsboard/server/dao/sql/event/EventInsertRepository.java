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
package org.thingsboard.server.dao.sql.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.thingsboard.server.common.data.event.ErrorEvent;
import org.thingsboard.server.common.data.event.Event;
import org.thingsboard.server.common.data.event.EventType;
import org.thingsboard.server.common.data.event.LifecycleEvent;
import org.thingsboard.server.common.data.event.RuleChainDebugEvent;
import org.thingsboard.server.common.data.event.RuleNodeDebugEvent;
import org.thingsboard.server.common.data.event.StatisticsEvent;
import org.thingsboard.server.dao.util.SqlDao;

import javax.annotation.PostConstruct;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. `EventInsertRepository` 是 ThingsBoard DAO 中负责事件存取的访问组件。
 * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
 * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
 * 4. 它直接协作于持久化模型、查询实现和对应领域服务。
 * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
 * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
 */
@Repository
@Transactional
@SqlDao
public class EventInsertRepository {

    private static final ThreadLocal<Pattern> PATTERN_THREAD_LOCAL = ThreadLocal.withInitial(() -> Pattern.compile(String.valueOf(Character.MIN_VALUE)));

    /**
     * `EMPTY_STR`常量，用于统一引用固定值。
     */
    private static final String EMPTY_STR = "";

    private final Map<EventType, String> insertStmtMap = new ConcurrentHashMap<>();

    /**
     * JDBC 访问模板，表示当前对象的对应属性。
     */
    @Autowired
    protected JdbcTemplate jdbcTemplate;

    /**
     * 事务执行模板，表示当前对象的对应属性。
     */
    @Autowired
    private TransactionTemplate transactionTemplate;

    /**
     * 是否移除对应数据。
     */
    @Value("${sql.remove_null_chars:true}")
    private boolean removeNullChars;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @PostConstruct
    public void init() {
        insertStmtMap.put(EventType.ERROR, "INSERT INTO " + EventType.ERROR.getTable() +
                " (id, tenant_id, ts, entity_id, service_id, e_method, e_error) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING;");
        insertStmtMap.put(EventType.LC_EVENT, "INSERT INTO " + EventType.LC_EVENT.getTable() +
                " (id, tenant_id, ts, entity_id, service_id, e_type, e_success, e_error) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING;");
        insertStmtMap.put(EventType.STATS, "INSERT INTO " + EventType.STATS.getTable() +
                " (id, tenant_id, ts, entity_id, service_id, e_messages_processed, e_errors_occurred) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING;");
        insertStmtMap.put(EventType.DEBUG_RULE_NODE, "INSERT INTO " + EventType.DEBUG_RULE_NODE.getTable() +
                " (id, tenant_id, ts, entity_id, service_id, e_type, e_entity_id, e_entity_type, e_msg_id, e_msg_type, e_data_type, e_relation_type, e_data, e_metadata, e_error) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING;");
        insertStmtMap.put(EventType.DEBUG_RULE_CHAIN, "INSERT INTO " + EventType.DEBUG_RULE_CHAIN.getTable() +
                " (id, tenant_id, ts, entity_id, service_id, e_message, e_error) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING;");
    }

    /**
     * 功能：执行 `save` 对应的处理。
     * 参数：
     * - `entities`：数据列表。
     * 返回：无。
     */
    public void save(List<Event> entities) {
        Map<EventType, List<Event>> eventsByType = entities.stream().collect(Collectors.groupingBy(Event::getType, Collectors.toList()));
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                for (var entry : eventsByType.entrySet()) {
                    jdbcTemplate.batchUpdate(insertStmtMap.get(entry.getKey()), getStatementSetter(entry.getKey(), entry.getValue()));
                }
            }
        });
    }

    /**
     * 功能：获取语句。
     * 参数：
     * - `eventType`：类型。
     * - `events`：数据列表。
     * 返回：匹配的数据集合。
     */
    private BatchPreparedStatementSetter getStatementSetter(EventType eventType, List<Event> events) {
        switch (eventType) {
            case ERROR:
                return getErrorEventSetter(events);
            case LC_EVENT:
                return getLcEventSetter(events);
            case STATS:
                return getStatsEventSetter(events);
            case DEBUG_RULE_NODE:
                return getRuleNodeEventSetter(events);
            case DEBUG_RULE_CHAIN:
                return getRuleChainEventSetter(events);
            default:
                throw new RuntimeException(eventType + " support is not implemented!");
        }
    }

    /**
     * 功能：获取事件。
     * 参数：
     * - `events`：数据列表。
     * 返回：匹配的数据集合。
     */
    private BatchPreparedStatementSetter getErrorEventSetter(List<Event> events) {
        return new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ErrorEvent event = (ErrorEvent) events.get(i);
                setCommonEventFields(ps, event);
                safePutString(ps, 6, event.getMethod());
                safePutString(ps, 7, event.getError());
            }

            @Override
            public int getBatchSize() {
                return events.size();
            }
        };
    }

    /**
     * 功能：获取事件。
     * 参数：
     * - `events`：数据列表。
     * 返回：匹配的数据集合。
     */
    private BatchPreparedStatementSetter getLcEventSetter(List<Event> events) {
        return new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                LifecycleEvent event = (LifecycleEvent) events.get(i);
                setCommonEventFields(ps, event);
                safePutString(ps, 6, event.getLcEventType());
                ps.setBoolean(7, event.isSuccess());
                safePutString(ps, 8, event.getError());
            }

            @Override
            public int getBatchSize() {
                return events.size();
            }
        };
    }

    /**
     * 功能：获取事件。
     * 参数：
     * - `events`：数据列表。
     * 返回：匹配的数据集合。
     */
    private BatchPreparedStatementSetter getStatsEventSetter(List<Event> events) {
        return new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                StatisticsEvent event = (StatisticsEvent) events.get(i);
                setCommonEventFields(ps, event);
                ps.setLong(6, event.getMessagesProcessed());
                ps.setLong(7, event.getErrorsOccurred());
            }

            @Override
            public int getBatchSize() {
                return events.size();
            }
        };
    }

    /**
     * 功能：获取规则节点。
     * 参数：
     * - `events`：数据列表。
     * 返回：匹配的数据集合。
     */
    private BatchPreparedStatementSetter getRuleNodeEventSetter(List<Event> events) {
        return new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                RuleNodeDebugEvent event = (RuleNodeDebugEvent) events.get(i);
                setCommonEventFields(ps, event);
                safePutString(ps, 6, event.getEventType());
                safePutUUID(ps, 7, event.getEventEntity() != null ? event.getEventEntity().getId() : null);
                safePutString(ps, 8, event.getEventEntity() != null ? event.getEventEntity().getEntityType().name() : null);
                safePutUUID(ps, 9, event.getMsgId());
                safePutString(ps, 10, event.getMsgType());
                safePutString(ps, 11, event.getDataType());
                safePutString(ps, 12, event.getRelationType());
                safePutString(ps, 13, event.getData());
                safePutString(ps, 14, event.getMetadata());
                safePutString(ps, 15, event.getError());
            }

            @Override
            public int getBatchSize() {
                return events.size();
            }
        };
    }

    /**
     * 功能：获取规则链。
     * 参数：
     * - `events`：数据列表。
     * 返回：匹配的数据集合。
     */
    private BatchPreparedStatementSetter getRuleChainEventSetter(List<Event> events) {
        return new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                RuleChainDebugEvent event = (RuleChainDebugEvent) events.get(i);
                setCommonEventFields(ps, event);
                safePutString(ps, 6, event.getMessage());
                safePutString(ps, 7, event.getError());
            }

            @Override
            public int getBatchSize() {
                return events.size();
            }
        };
    }

    /**
     * 功能：执行 `safePutString` 对应的处理。
     * 参数：
     * - `ps`：`ps` 参数。
     * - `parameterIdx`：`parameterIdx` 参数。
     * - `value`：值。
     * 返回：无。
     */
    void safePutString(PreparedStatement ps, int parameterIdx, String value) throws SQLException {
        if (value != null) {
            ps.setString(parameterIdx, replaceNullChars(value));
        } else {
            ps.setNull(parameterIdx, Types.VARCHAR);
        }
    }

    /**
     * 功能：执行 `safePutUUID` 对应的处理。
     * 参数：
     * - `ps`：`ps` 参数。
     * - `parameterIdx`：`parameterIdx` 参数。
     * - `value`：值。
     * 返回：无。
     */
    void safePutUUID(PreparedStatement ps, int parameterIdx, UUID value) throws SQLException {
        if (value != null) {
            ps.setObject(parameterIdx, value);
        } else {
            ps.setNull(parameterIdx, Types.OTHER);
        }
    }

    /**
     * 功能：更新事件。
     * 参数：
     * - `ps`：`ps` 参数。
     * - `event`：`event` 参数。
     * 返回：无。
     */
    private void setCommonEventFields(PreparedStatement ps, Event event) throws SQLException {
        ps.setObject(1, event.getId().getId());
        ps.setObject(2, event.getTenantId().getId());
        ps.setLong(3, event.getCreatedTime());
        ps.setObject(4, event.getEntityId());
        ps.setString(5, event.getServiceId());
    }

    /**
     * 功能：执行 `replaceNullChars` 对应的处理。
     * 参数：
     * - `strValue`：值。
     * 返回：文本结果。
     */
    private String replaceNullChars(String strValue) {
        if (removeNullChars && strValue != null) {
            return PATTERN_THREAD_LOCAL.get().matcher(strValue).replaceAll(EMPTY_STR);
        }
        return strValue;
    }
}
