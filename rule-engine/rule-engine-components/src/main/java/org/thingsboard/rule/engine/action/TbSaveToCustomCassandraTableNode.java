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
package org.thingsboard.rule.engine.action;

import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.datastax.oss.driver.api.core.cql.AsyncResultSet;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.BoundStatementBuilder;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.Statement;
import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.dao.cassandra.CassandraCluster;
import org.thingsboard.server.dao.cassandra.guava.GuavaSession;
import org.thingsboard.server.dao.nosql.CassandraStatementTask;
import org.thingsboard.server.dao.nosql.TbResultSetFuture;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.thingsboard.common.util.DonAsynchron.withCallback;

@Slf4j
@RuleNode(type = ComponentType.ACTION,
        name = "save to custom table",
        configClazz = TbSaveToCustomCassandraTableNodeConfiguration.class,
        nodeDescription = "Node stores data from incoming Message payload to the Cassandra database into the predefined custom table" +
                " that should have <b>cs_tb_</b> prefix, to avoid the data insertion to the common TB tables.<br>" +
                "<b>Note:</b> rule node can be used only for Cassandra DB.",
        nodeDetails = "Administrator should set the custom table name without prefix: <b>cs_tb_</b>. <br>" +
                "Administrator can configure the mapping between the Message field names and Table columns name.<br>" +
                "<b>Note:</b>If the mapping key is <b>$entity_id</b>, that is identified by the Message Originator, then to the appropriate column name(mapping value) will be write the message originator id.<br><br>" +
                "If specified message field does not exist or is not a JSON Primitive, the outbound message will be routed via <b>failure</b> chain," +
                " otherwise, the message will be routed via <b>success</b> chain.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeCustomTableConfig",
        icon = "file_upload",
        ruleChainTypes = RuleChainType.CORE)
/**
 * 中文说明：`TbSaveToCustomCassandraTableNode` 是保存到自定义Cassandra表节点规则节点，用于执行告警、客户归属、关系、设备状态、日志或外部存储等动作。
 * 输入关系：作为规则链节点接收上游节点传入的 `TbMsg`，根据消息体、元数据、发起实体或上下文服务读取所需数据。
 * 输出关系：处理成功时通过 `Success`、`True`、`False` 或其它命名关系把原消息或转换后的消息交给后续节点，实际关系由节点逻辑和配置决定。
 * 失败关系：配置校验、脚本执行、服务调用、数据解析或异步回调异常时通过 `Failure` 关系交给规则链失败分支。
 * 配置对象：`TbSaveToCustomCassandraTableNodeConfiguration`，配置内容来自规则节点 JSON，并在 `init` 或父类初始化阶段转换为运行时对象。
 * 调用方和生命周期：Rule Engine 节点运行时创建本节点并调用 `init`，每条消息进入 `onMsg` 或等价处理方法，`destroy` 负责释放脚本引擎、缓存、监听器等资源。
 */
public class TbSaveToCustomCassandraTableNode implements TbNode {

    /**
     * 常量字段：定义 `TABLE_PREFIX`，用于与本类处理流程相关的运行时值，本身不触发外部系统调用。
     */
    private static final String TABLE_PREFIX = "cs_tb_";
    /**
     * 常量字段：定义 `parser`，用于与本类处理流程相关的运行时值，本身不触发外部系统调用。
     */
    private static final JsonParser parser = new JsonParser();
    /**
     * 常量字段：定义 `ENTITY_ID`，用于目标实体类型、名称或标识，本身不触发外部系统调用。
     */
    private static final String ENTITY_ID = "$entityId";

    /**
     * 字段说明：保存从规则节点 JSON 转换得到的配置对象，供消息处理和生命周期方法复用。
     */
    private TbSaveToCustomCassandraTableNodeConfiguration config;
    /**
     * 字段说明：保存 `session`，表示与本类处理流程相关的运行时值，供本类方法在规则节点处理流程中使用。
     */
    private GuavaSession session;
    /**
     * 字段说明：保存 `cassandraCluster`，表示与本类处理流程相关的运行时值，供本类方法在规则节点处理流程中使用。
     */
    private CassandraCluster cassandraCluster;
    /**
     * 字段说明：保存 `defaultWriteLevel`，表示与本类处理流程相关的运行时值，供本类方法在规则节点处理流程中使用。
     */
    private ConsistencyLevel defaultWriteLevel;
    /**
     * 字段说明：保存 `saveStmt`，表示与本类处理流程相关的运行时值，供本类方法在规则节点处理流程中使用。
     */
    private PreparedStatement saveStmt;
    /**
     * 字段说明：保存 `readResultsProcessingExecutor` 服务层引用；具体服务实现可能访问数据库或缓存，本字段本身不管理事务。
     */
    private ExecutorService readResultsProcessingExecutor;
    private Map<String, String> fieldsMap;

    @Override
    /**
     * 方法说明：在节点生命周期初始化阶段加载规则节点 JSON 配置并准备脚本、缓存、监听器或本地状态。
     * 调用边界：由规则节点生命周期、配置升级流程或配置默认值创建流程调用；数据库/缓存：会通过 ThingsBoard 服务层或外部会话发起读写，具体数据库和缓存行为由服务实现负责；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        config = TbNodeUtils.convert(configuration, TbSaveToCustomCassandraTableNodeConfiguration.class);
        cassandraCluster = ctx.getCassandraCluster();
        if (cassandraCluster == null) {
            throw new RuntimeException("Unable to connect to Cassandra database");
        } else {
            startExecutor();
            saveStmt = getSaveStmt();
        }
    }

    @Override
    /**
     * 方法说明：作为规则链消息处理入口接收上游 TbMsg 并按节点配置输出到后续关系。
     * 输入输出：输入为上游规则链传入的 `TbMsg`；成功时交给成功、布尔或命名关系，异常时交给失败关系。
     * 数据库/缓存/Rule Engine/Actor/MQTT/事务：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：由规则节点运行时调用或通过 `ctx` 投递、确认、调度消息，通常处于 Actor 调度链路；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public void onMsg(TbContext ctx, TbMsg msg) {
        // 异步回调用于把服务或转换结果映射为规则链成功/失败关系。
        withCallback(save(msg, ctx), aVoid -> ctx.tellSuccess(msg), e -> ctx.tellFailure(msg, e), ctx.getDbCallbackExecutor());
    }

    @Override
    /**
     * 方法说明：在节点生命周期销毁阶段释放缓存、脚本引擎、监听器或本地状态。
     * 调用边界：由规则节点生命周期、配置升级流程或配置默认值创建流程调用；数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    public void destroy() {
        stopExecutor();
        saveStmt = null;
    }

    /**
     * 方法说明：执行 `startExecutor` 对应的辅助逻辑，供 `TbSaveToCustomCassandraTableNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：使用本地内存缓存、队列或并发结构，本方法本身不直接访问数据库，具体调用链可能涉及缓存；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private void startExecutor() {
        readResultsProcessingExecutor = Executors.newCachedThreadPool();
    }

    /**
     * 方法说明：执行 `stopExecutor` 对应的辅助逻辑，供 `TbSaveToCustomCassandraTableNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private void stopExecutor() {
        if (readResultsProcessingExecutor != null) {
            readResultsProcessingExecutor.shutdownNow();
        }
    }

    /**
     * 方法说明：执行 `prepare` 对应的辅助逻辑，供 `TbSaveToCustomCassandraTableNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private PreparedStatement prepare(String query) {
        return getSession().prepare(query);
    }

    /**
     * 方法说明：读取配置、消息字段、实体字段或服务返回值，供 `TbSaveToCustomCassandraTableNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：会通过 ThingsBoard 服务层或外部会话发起读写，具体数据库和缓存行为由服务实现负责；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private GuavaSession getSession() {
        if (session == null) {
            session = cassandraCluster.getSession();
            defaultWriteLevel = cassandraCluster.getDefaultWriteConsistencyLevel();
        }
        return session;
    }

    /**
     * 方法说明：通过服务层保存实体、属性、遥测或关系数据，供 `TbSaveToCustomCassandraTableNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private PreparedStatement getSaveStmt() {
        fieldsMap = config.getFieldsMapping();
        if (fieldsMap.isEmpty()) {
            throw new RuntimeException("Fields(key,value) map is empty!");
        } else {
            return prepareStatement(new ArrayList<>(fieldsMap.values()));
        }
    }

    /**
     * 方法说明：执行 `prepareStatement` 对应的辅助逻辑，供 `TbSaveToCustomCassandraTableNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private PreparedStatement prepareStatement(List<String> fieldsList) {
        return prepare(createQuery(fieldsList));
    }

    /**
     * 方法说明：创建实体、告警、关系或辅助对象，供 `TbSaveToCustomCassandraTableNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private String createQuery(List<String> fieldsList) {
        int size = fieldsList.size();
        StringBuilder query = new StringBuilder();
        query.append("INSERT INTO ")
                .append(TABLE_PREFIX)
                .append(config.getTableName())
                .append("(");
        for (String field : fieldsList) {
            query.append(field);
            if (fieldsList.get(size - 1).equals(field)) {
                query.append(")");
            } else {
                query.append(",");
            }
        }
        query.append(" VALUES(");
        for (int i = 0; i < size; i++) {
            if (i == size - 1) {
                query.append("?)");
            } else {
                query.append("?, ");
            }
        }
        return query.toString();
    }

    /**
     * 方法说明：通过服务层保存实体、属性、遥测或关系数据，供 `TbSaveToCustomCassandraTableNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private ListenableFuture<Void> save(TbMsg msg, TbContext ctx) {
        JsonElement data = parser.parse(msg.getData());
        if (!data.isJsonObject()) {
            throw new IllegalStateException("Invalid message structure, it is not a JSON Object:" + data);
        } else {
            JsonObject dataAsObject = data.getAsJsonObject();
            BoundStatementBuilder stmtBuilder = new BoundStatementBuilder(saveStmt.bind());
            AtomicInteger i = new AtomicInteger(0);
            fieldsMap.forEach((key, value) -> {
                if (key.equals(ENTITY_ID)) {
                    stmtBuilder.setUuid(i.get(), msg.getOriginator().getId());
                } else if (dataAsObject.has(key)) {
                    JsonElement dataKeyElement = dataAsObject.get(key);
                    if (dataKeyElement.isJsonPrimitive()) {
                        JsonPrimitive primitive = dataKeyElement.getAsJsonPrimitive();
                        if (primitive.isNumber()) {
                            if (primitive.getAsString().contains(".")) {
                                stmtBuilder.setDouble(i.get(), primitive.getAsDouble());
                            } else {
                                stmtBuilder.setLong(i.get(), primitive.getAsLong());
                            }
                        } else if (primitive.isBoolean()) {
                            stmtBuilder.setBoolean(i.get(), primitive.getAsBoolean());
                        } else if (primitive.isString()) {
                            stmtBuilder.setString(i.get(), primitive.getAsString());
                        } else {
                            stmtBuilder.setToNull(i.get());
                        }
                    } else if (dataKeyElement.isJsonObject()) {
                        stmtBuilder.setString(i.get(), dataKeyElement.getAsJsonObject().toString());
                    } else {
                        throw new IllegalStateException("Message data key: '" + key + "' with value: '" + value + "' is not a JSON Object or JSON Primitive!");
                    }
                } else {
                    throw new RuntimeException("Message data doesn't contain key: " + "'" + key + "'!");
                }
                i.getAndIncrement();
            });
            // 脚本执行交给规则节点脚本引擎，异常会通过回调进入失败关系。
            return getFuture(executeAsyncWrite(ctx, stmtBuilder.build()), rs -> null);
        }
    }

    /**
     * 方法说明：执行 `executeAsyncWrite` 对应的辅助逻辑，供 `TbSaveToCustomCassandraTableNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private TbResultSetFuture executeAsyncWrite(TbContext ctx, Statement statement) {
        // 脚本执行交给规则节点脚本引擎，异常会通过回调进入失败关系。
        return executeAsync(ctx, statement, defaultWriteLevel);
    }

    /**
     * 方法说明：执行 `executeAsync` 对应的辅助逻辑，供 `TbSaveToCustomCassandraTableNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：会通过 ThingsBoard 服务层或外部会话发起读写，具体数据库和缓存行为由服务实现负责；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private TbResultSetFuture executeAsync(TbContext ctx, Statement statement, ConsistencyLevel level) {
        if (log.isDebugEnabled()) {
            log.debug("Execute cassandra async statement {}", statementToString(statement));
        }
        if (statement.getConsistencyLevel() == null) {
            statement.setConsistencyLevel(level);
        }
        return ctx.submitCassandraWriteTask(new CassandraStatementTask(ctx.getTenantId(), getSession(), statement));
    }

    /**
     * 方法说明：执行 `statementToString` 对应的辅助逻辑，供 `TbSaveToCustomCassandraTableNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private static String statementToString(Statement statement) {
        if (statement instanceof BoundStatement) {
            return ((BoundStatement) statement).getPreparedStatement().getQuery();
        } else {
            return statement.toString();
        }
    }

    /**
     * 方法说明：读取配置、消息字段、实体字段或服务返回值，供 `TbSaveToCustomCassandraTableNode` 的规则节点处理或辅助流程调用。
     * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
     */
    private <T> ListenableFuture<T> getFuture(TbResultSetFuture future, java.util.function.Function<AsyncResultSet, T> transformer) {
        // 异步聚合或转换服务返回值，完成后再继续规则链处理。
        return Futures.transform(future, new Function<AsyncResultSet, T>() {
            @Nullable
            @Override
            /**
             * 方法说明：把函数应用到已解析的参数值，供 `TbSaveToCustomCassandraTableNode` 的规则节点处理或辅助流程调用。
             * 调用边界：数据库/缓存：本方法本身不直接访问数据库或缓存，具体实现/调用链可能涉及；Rule Engine/Actor：本方法本身不直接调度 Actor，若由节点入口调用则处于规则引擎调用链；MQTT：本方法本身不直接发布或订阅 MQTT 消息；事务：本方法本身不直接开启或提交事务。
             */
            public T apply(@Nullable AsyncResultSet input) {
                return transformer.apply(input);
            }
        }, readResultsProcessingExecutor);
    }

    /*
     * 本类总结：`TbSaveToCustomCassandraTableNode` 负责执行告警、客户归属、关系、设备状态、日志或外部存储等动作；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
