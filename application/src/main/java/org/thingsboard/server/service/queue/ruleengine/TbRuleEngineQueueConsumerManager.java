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
package org.thingsboard.server.service.queue.ruleengine;

import com.google.protobuf.ProtocolStringList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.gen.MsgProtos;
import org.thingsboard.server.common.msg.queue.QueueToRuleEngineMsg;
import org.thingsboard.server.common.msg.queue.RuleEngineException;
import org.thingsboard.server.common.msg.queue.RuleNodeInfo;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.QueueKey;
import org.thingsboard.server.service.queue.TbMsgPackCallback;
import org.thingsboard.server.service.queue.TbMsgPackProcessingContext;
import org.thingsboard.server.service.queue.TbRuleEngineConsumerStats;
import org.thingsboard.server.service.queue.processing.TbRuleEngineProcessingDecision;
import org.thingsboard.server.service.queue.processing.TbRuleEngineProcessingResult;
import org.thingsboard.server.service.queue.processing.TbRuleEngineProcessingStrategy;
import org.thingsboard.server.service.queue.processing.TbRuleEngineSubmitStrategy;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. `TbRuleEngineQueueConsumerManager` 是 ThingsBoard Application 中负责队列的协调管理组件。
 * 2. 它集中组织该领域的核心操作，并向上层提供稳定的调用入口。
 * 3. 类中的依赖和状态用于完成校验、编排、查询或更新等直接职责。
 * 4. 它直接协作于领域模型、存取接口和相关业务组件。
 * 5. 把这些操作集中在独立类型中，可以避免调用方重复拼装同一业务流程。
 * 6. 阅读时重点关注公开方法的职责边界、关键校验和依赖调用顺序。
 */
@Slf4j
public class TbRuleEngineQueueConsumerManager {

    /**
     * 状态常量，用于统一引用固定值。
     */
    public static final String SUCCESSFUL_STATUS = "successful";
    public static final String FAILED_STATUS = "failed";

    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    private final TbRuleEngineConsumerContext ctx;
    private final QueueKey queueKey;
    /**
     * `stats` 字段，保存当前对象的对应属性。
     */
    private final TbRuleEngineConsumerStats stats;
    private final ReentrantLock lock = new ReentrantLock(); //NonfairSync

    /**
     * 队列，用于标识消息投递或消费的队列。
     */
    @Getter
    private volatile Queue queue;
    /**
     * `partitions`集合，用于去重保存或快速判断对象是否存在。
     */
    @Getter
    private volatile Set<TopicPartitionInfo> partitions;
    private volatile ConsumerWrapper consumerWrapper;

    /**
     * 当前处理是否已经停止。
     */
    private volatile boolean stopped;

    private final java.util.Queue<TbQueueConsumerManagerTask> tasks = new ConcurrentLinkedQueue<>();

    /**
     * 功能：创建 `TbRuleEngineQueueConsumerManager` 实例，并初始化必要字段。
     * 参数：
     * - `ctx`：处理上下文。
     * - `queueKey`：队列名称或队列对象。
     * 返回：新创建的对象实例。
     */
    public TbRuleEngineQueueConsumerManager(TbRuleEngineConsumerContext ctx, QueueKey queueKey) {
        this.ctx = ctx;
        this.queueKey = queueKey;
        this.stats = new TbRuleEngineConsumerStats(queueKey, ctx.getStatsFactory());
    }

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：
     * - `queue`：队列名称或队列对象。
     * 返回：无。
     */
    public void init(Queue queue) {
        this.queue = queue;
        if (queue.isConsumerPerPartition()) {
            this.consumerWrapper = new ConsumerPerPartitionWrapper();
        } else {
            this.consumerWrapper = new SingleConsumerWrapper();
        }
        log.debug("[{}] Initialized consumer for queue: {}", queueKey, queue);
    }

    /**
     * 功能：执行 `update` 对应的处理。
     * 参数：
     * - `queue`：队列名称或队列对象。
     * 返回：无。
     */
    public void update(Queue queue) {
        addTask(TbQueueConsumerManagerTask.configUpdate(queue));
    }

    /**
     * 功能：执行 `update` 对应的处理。
     * 参数：
     * - `partitions`：分区标识或分区信息。
     * 返回：无。
     */
    public void update(Set<TopicPartitionInfo> partitions) {
        addTask(TbQueueConsumerManagerTask.partitionChange(partitions));
    }

    /**
     * 功能：执行 `delete` 对应的处理。
     * 参数：
     * - `drainQueue`：队列名称或队列对象。
     * 返回：无。
     */
    public void delete(boolean drainQueue) {
        addTask(TbQueueConsumerManagerTask.delete(drainQueue));
    }

    /**
     * 功能：保存或创建`Task`。
     * 参数：
     * - `todo`：`todo` 参数。
     * 返回：无。
     */
    private void addTask(TbQueueConsumerManagerTask todo) {
        if (stopped) {
            return;
        }
        tasks.add(todo);
        log.trace("[{}] Added task: {}", queueKey, todo);
        tryProcessTasks();
    }

    /**
     * 功能：执行 `tryProcessTasks` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    private void tryProcessTasks() {
        if (!ctx.isReady()) {
            log.debug("[{}] TbRuleEngineConsumerContext is not ready yet, will process tasks later", queueKey);
            ctx.getScheduler().schedule(this::tryProcessTasks, 1, TimeUnit.SECONDS);
            return;
        }
        ctx.getMgmtExecutor().submit(() -> {
            if (lock.tryLock()) {
                try {
                    Queue newConfiguration = null;
                    Set<TopicPartitionInfo> newPartitions = null;
                    while (!stopped) {
                        TbQueueConsumerManagerTask task = tasks.poll();
                        if (task == null) {
                            break;
                        }
                        log.trace("[{}] Processing task: {}", queueKey, task);

                        if (task.getEvent() == QueueEvent.PARTITION_CHANGE) {
                            newPartitions = task.getPartitions();
                        } else if (task.getEvent() == QueueEvent.CONFIG_UPDATE) {
                            newConfiguration = task.getQueue();
                        } else if (task.getEvent() == QueueEvent.DELETE) {
                            doDelete(task.isDrainQueue());
                            return;
                        }
                    }
                    if (stopped) {
                        return;
                    }
                    if (newConfiguration != null) {
                        doUpdate(newConfiguration);
                    }
                    if (newPartitions != null) {
                        doUpdate(newPartitions);
                    }
                } catch (Exception e) {
                    log.error("[{}] Failed to process tasks", queueKey, e);
                } finally {
                    lock.unlock();
                }
            } else {
                log.trace("[{}] Failed to acquire lock", queueKey);
                ctx.getScheduler().schedule(this::tryProcessTasks, 1, TimeUnit.SECONDS);
            }
        });
    }

    /**
     * 功能：执行 `doUpdate` 对应的处理。
     * 参数：
     * - `newQueue`：队列名称或队列对象。
     * 返回：无。
     */
    private void doUpdate(Queue newQueue) {
        log.info("[{}] Processing queue update: {}", queueKey, newQueue);
        var oldQueue = this.queue;
        this.queue = newQueue;
        if (log.isTraceEnabled()) {
            log.trace("[{}] Old queue configuration: {}", queueKey, oldQueue);
            log.trace("[{}] New queue configuration: {}", queueKey, newQueue);
        }

        if (oldQueue == null) {
            init(queue);
        } else if (newQueue.isConsumerPerPartition() != oldQueue.isConsumerPerPartition()) {
            consumerWrapper.getConsumers().forEach(TbQueueConsumerTask::initiateStop);
            consumerWrapper.getConsumers().forEach(TbQueueConsumerTask::awaitCompletion);

            init(queue);
            if (partitions != null) {
                doUpdate(partitions); // even if partitions number was changed, there can be no partition change event
            }
        } else {
            // do nothing, because partitions change (if they changed) will be handled on PartitionChangeEvent,
            // and changes to pollInterval/packProcessingTimeout/submitStrategy/processingStrategy will be picked up by consumer on the fly,
            // and queue topic and name are immutable
        }
    }

    /**
     * 功能：执行 `doUpdate` 对应的处理。
     * 参数：
     * - `partitions`：分区标识或分区信息。
     * 返回：无。
     */
    private void doUpdate(Set<TopicPartitionInfo> partitions) {
        this.partitions = partitions;
        consumerWrapper.updatePartitions(partitions);
    }

    /**
     * 功能：执行 `stop` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void stop() {
        log.debug("[{}] Stopping consumers", queueKey);
        consumerWrapper.getConsumers().forEach(TbQueueConsumerTask::initiateStop);
        stopped = true;
    }

    /**
     * 功能：执行 `awaitStop` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void awaitStop() {
        consumerWrapper.getConsumers().forEach(TbQueueConsumerTask::awaitCompletion);
        log.debug("[{}] Unsubscribed and stopped consumers", queueKey);
    }

    /**
     * 功能：执行 `doDelete` 对应的处理。
     * 参数：
     * - `drainQueue`：队列名称或队列对象。
     * 返回：无。
     */
    private void doDelete(boolean drainQueue) {
        stopped = true;
        log.info("[{}] Handling queue deletion", queueKey);
        consumerWrapper.getConsumers().forEach(TbQueueConsumerTask::awaitCompletion);

        List<TbQueueConsumer<TbProtoQueueMsg<ToRuleEngineMsg>>> queueConsumers = consumerWrapper.getConsumers().stream()
                .map(TbQueueConsumerTask::getConsumer).collect(Collectors.toList());
        ctx.getConsumersExecutor().submit(() -> {
            if (drainQueue) {
                drainQueue(queueConsumers);
            }

            queueConsumers.forEach(consumer -> {
                for (String topic : consumer.getFullTopicNames()) {
                    try {
                        ctx.getQueueAdmin().deleteTopic(topic);
                        log.info("Deleted topic {}", topic);
                    } catch (Exception e) {
                        log.error("Failed to delete topic {}", topic, e);
                    }
                }
                try {
                    consumer.unsubscribe();
                } catch (Exception e) {
                    log.error("[{}] Failed to unsubscribe consumer", queueKey, e);
                }
            });
        });
    }

    /**
     * 功能：执行 `launchConsumer` 对应的处理。
     * 参数：
     * - `consumerTask`：`consumerTask` 参数。
     * 返回：无。
     */
    private void launchConsumer(TbQueueConsumerTask consumerTask) {
        log.info("[{}] Launching consumer", consumerTask.getKey());
        Future<?> consumerLoop = ctx.getConsumersExecutor().submit(() -> {
            ThingsBoardThreadFactory.updateCurrentThreadName(consumerTask.getKey().toString());
            try {
                consumerLoop(consumerTask.getConsumer());
            } catch (Throwable e) {
                log.error("Failure in consumer loop", e);
            }
        });
        consumerTask.setTask(consumerLoop);
    }

    /**
     * 功能：执行 `consumerLoop` 对应的处理。
     * 参数：
     * - `consumer`：`consumer` 参数。
     * 返回：无。
     */
    private void consumerLoop(TbQueueConsumer<TbProtoQueueMsg<ToRuleEngineMsg>> consumer) {
        while (!stopped && !consumer.isStopped()) {
            try {
                List<TbProtoQueueMsg<ToRuleEngineMsg>> msgs = consumer.poll(queue.getPollInterval());
                if (msgs.isEmpty()) {
                    continue;
                }
                processMsgs(msgs, consumer, queue);
            } catch (Exception e) {
                if (!consumer.isStopped()) {
                    log.warn("Failed to process messages from queue", e);
                    try {
                        Thread.sleep(ctx.getPollDuration());
                    } catch (InterruptedException e2) {
                        log.trace("Failed to wait until the server has capacity to handle new requests", e2);
                    }
                }
            }
        }
        if (consumer.isStopped()) {
            consumer.unsubscribe();
        }
        log.info("Rule Engine consumer stopped");
    }

    /**
     * 功能：处理`Msgs`。
     * 参数：
     * - `msgs`：待处理消息。
     * - `consumer`：`consumer` 参数。
     * - `queue`：队列名称或队列对象。
     * 返回：无。
     */
    private void processMsgs(List<TbProtoQueueMsg<ToRuleEngineMsg>> msgs,
                             TbQueueConsumer<TbProtoQueueMsg<ToRuleEngineMsg>> consumer,
                             Queue queue) throws InterruptedException {
        TbRuleEngineSubmitStrategy submitStrategy = getSubmitStrategy(queue);
        TbRuleEngineProcessingStrategy ackStrategy = getProcessingStrategy(queue);
        submitStrategy.init(msgs);
        while (!stopped && !consumer.isStopped()) {
            TbMsgPackProcessingContext packCtx = new TbMsgPackProcessingContext(queue.getName(), submitStrategy, ackStrategy.isSkipTimeoutMsgs());
            submitStrategy.submitAttempt((id, msg) -> submitMessage(packCtx, id, msg));

            final boolean timeout = !packCtx.await(queue.getPackProcessingTimeout(), TimeUnit.MILLISECONDS);

            TbRuleEngineProcessingResult result = new TbRuleEngineProcessingResult(queue.getName(), timeout, packCtx);
            if (timeout) {
                printFirstOrAll(packCtx, packCtx.getPendingMap(), "Timeout");
            }
            if (!packCtx.getFailedMap().isEmpty()) {
                printFirstOrAll(packCtx, packCtx.getFailedMap(), "Failed");
            }
            packCtx.printProfilerStats();

            TbRuleEngineProcessingDecision decision = ackStrategy.analyze(result);
            if (ctx.isStatsEnabled()) {
                stats.log(result, decision.isCommit());
            }

            packCtx.cleanup();

            if (decision.isCommit()) {
                submitStrategy.stop();
                consumer.commit();
                break;
            } else {
                submitStrategy.update(decision.getReprocessMap());
            }
        }
    }

    /**
     * 功能：获取策略对象。
     * 参数：
     * - `queue`：队列名称或队列对象。
     * 返回：处理结果。
     */
    private TbRuleEngineSubmitStrategy getSubmitStrategy(Queue queue) {
        return ctx.getSubmitStrategyFactory().newInstance(queue.getName(), queue.getSubmitStrategy());
    }

    /**
     * 功能：获取策略对象。
     * 参数：
     * - `queue`：队列名称或队列对象。
     * 返回：处理结果。
     */
    private TbRuleEngineProcessingStrategy getProcessingStrategy(Queue queue) {
        return ctx.getProcessingStrategyFactory().newInstance(queue.getName(), queue.getProcessingStrategy());
    }

    /**
     * 功能：发送或提交消息。
     * 参数：
     * - `packCtx`：处理上下文。
     * - `id`：`id`ID。
     * - `msg`：待处理消息。
     * 返回：无。
     */
    private void submitMessage(TbMsgPackProcessingContext packCtx, UUID id, TbProtoQueueMsg<ToRuleEngineMsg> msg) {
        log.trace("[{}] Creating callback for topic {} message: {}", id, queue.getName(), msg.getValue());
        ToRuleEngineMsg toRuleEngineMsg = msg.getValue();
        TenantId tenantId = TenantId.fromUUID(new UUID(toRuleEngineMsg.getTenantIdMSB(), toRuleEngineMsg.getTenantIdLSB()));
        TbMsgCallback callback = ctx.isPrometheusStatsEnabled() ?
                new TbMsgPackCallback(id, tenantId, packCtx, stats.getTimer(tenantId, SUCCESSFUL_STATUS), stats.getTimer(tenantId, FAILED_STATUS)) :
                new TbMsgPackCallback(id, tenantId, packCtx);
        try {
            if (!toRuleEngineMsg.getTbMsg().isEmpty()) {
                forwardToRuleEngineActor(queue.getName(), tenantId, toRuleEngineMsg, callback);
            } else {
                callback.onSuccess();
            }
        } catch (Exception e) {
            callback.onFailure(new RuleEngineException(e.getMessage(), e));
        }
    }

    /**
     * 功能：执行 `forwardToRuleEngineActor` 对应的处理。
     * 参数：
     * - `queueName`：队列名称或队列对象。
     * - `tenantId`：租户IDID。
     * - `toRuleEngineMsg`：待处理消息。
     * - `callback`：处理完成后的回调。
     * 返回：无。
     */
    private void forwardToRuleEngineActor(String queueName, TenantId tenantId, ToRuleEngineMsg toRuleEngineMsg, TbMsgCallback callback) {
        TbMsg tbMsg = TbMsg.fromBytes(queueName, toRuleEngineMsg.getTbMsg().toByteArray(), callback);
        QueueToRuleEngineMsg msg;
        ProtocolStringList relationTypesList = toRuleEngineMsg.getRelationTypesList();
        Set<String> relationTypes;
        if (relationTypesList.size() == 1) {
            relationTypes = Collections.singleton(relationTypesList.get(0));
        } else {
            relationTypes = new HashSet<>(relationTypesList);
        }
        msg = new QueueToRuleEngineMsg(tenantId, tbMsg, relationTypes, toRuleEngineMsg.getFailureMessage());
        ctx.getActorContext().tell(msg);
    }

    /**
     * 功能：执行 `printFirstOrAll` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `map`：键值映射。
     * - `prefix`：`prefix` 参数。
     * 返回：无。
     */
    private void printFirstOrAll(TbMsgPackProcessingContext ctx, Map<UUID, TbProtoQueueMsg<ToRuleEngineMsg>> map, String prefix) {
        boolean printAll = log.isTraceEnabled();
        log.info("[{}] {} to process [{}] messages", queueKey, prefix, map.size());
        for (Map.Entry<UUID, TbProtoQueueMsg<ToRuleEngineMsg>> pending : map.entrySet()) {
            ToRuleEngineMsg tmp = pending.getValue().getValue();
            TbMsg tmpMsg = TbMsg.fromBytes(queue.getName(), tmp.getTbMsg().toByteArray(), TbMsgCallback.EMPTY);
            RuleNodeInfo ruleNodeInfo = ctx.getLastVisitedRuleNode(pending.getKey());
            if (printAll) {
                log.trace("[{}][{}] {} to process message: {}, Last Rule Node: {}", queueKey, TenantId.fromUUID(new UUID(tmp.getTenantIdMSB(), tmp.getTenantIdLSB())), prefix, tmpMsg, ruleNodeInfo);
            } else {
                log.info("[{}] {} to process message: {}, Last Rule Node: {}", TenantId.fromUUID(new UUID(tmp.getTenantIdMSB(), tmp.getTenantIdLSB())), prefix, tmpMsg, ruleNodeInfo);
                break;
            }
        }
    }

    /**
     * 功能：执行 `printStats` 对应的处理。
     * 参数：
     * - `ts`：时间戳。
     * 返回：无。
     */
    public void printStats(long ts) {
        stats.printStats();
        ctx.getStatisticsService().reportQueueStats(ts, stats);
        stats.reset();
    }

    /**
     * 功能：执行 `drainQueue` 对应的处理。
     * 参数：
     * - `consumers`：数据列表。
     * 返回：无。
     */
    private void drainQueue(List<TbQueueConsumer<TbProtoQueueMsg<ToRuleEngineMsg>>> consumers) {
        long finishTs = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(ctx.getTopicDeletionDelayInSec());
        try {
            int n = 0;
            while (System.currentTimeMillis() <= finishTs) {
                for (TbQueueConsumer<TbProtoQueueMsg<ToRuleEngineMsg>> consumer : consumers) {
                    List<TbProtoQueueMsg<ToRuleEngineMsg>> msgs = consumer.poll(queue.getPollInterval());
                    if (msgs.isEmpty()) {
                        continue;
                    }
                    for (TbProtoQueueMsg<ToRuleEngineMsg> msg : msgs) {
                        try {
                            MsgProtos.TbMsgProto tbMsgProto = MsgProtos.TbMsgProto.parseFrom(msg.getValue().getTbMsg().toByteArray());
                            EntityId originator = EntityIdFactory.getByTypeAndUuid(tbMsgProto.getEntityType(), new UUID(tbMsgProto.getEntityIdMSB(), tbMsgProto.getEntityIdLSB()));

                            TopicPartitionInfo tpi = ctx.getPartitionService().resolve(ServiceType.TB_RULE_ENGINE, queue.getName(), TenantId.SYS_TENANT_ID, originator);
                            ctx.getProducerProvider().getRuleEngineMsgProducer().send(tpi, msg, null);
                            n++;
                        } catch (Throwable e) {
                            log.warn("Failed to move message to system {}: {}", consumer.getTopic(), msg, e);
                        }
                    }
                    consumer.commit();
                }
            }
            if (n > 0) {
                log.info("Moved {} messages from {} to system {}", n, queueKey, queue.getName());
            }
        } catch (Exception e) {
            log.error("[{}] Failed to drain queue", queueKey, e);
        }
    }

    /**
     * 功能：执行 `partitionsToString` 对应的处理。
     * 参数：
     * - `partitions`：分区标识或分区信息。
     * 返回：文本结果。
     */
    private static String partitionsToString(Collection<TopicPartitionInfo> partitions) {
        return partitions.stream().map(TopicPartitionInfo::getFullTopicName).collect(Collectors.joining(", ", "[", "]"));
    }

    /**
     * 中文说明：
     * 1. `ConsumerWrapper` 是 ThingsBoard Application 中定义 `Consumer Wrapper` 能力边界的接口。
     * 2. 它声明实现方必须提供的核心操作和输入输出约定。
     * 3. 接口方法共同定义该能力的输入、输出和行为边界。
     * 4. 它直接协作于实现类以及使用该接口的调用组件。
     * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
     * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
     */
    interface ConsumerWrapper {

        /**
         * 功能：更新`Partitions`。
         * 参数：
         * - `partitions`：分区标识或分区信息。
         * 返回：无。
         */
        void updatePartitions(Set<TopicPartitionInfo> partitions);

        /**
         * 功能：获取`Consumers`。
         * 参数：无。
         * 返回：匹配的数据集合。
         */
        Collection<TbQueueConsumerTask> getConsumers();

    }

    /**
     * 中文说明：
     * 1. `ConsumerPerPartitionWrapper` 是 ThingsBoard Application 中围绕分区提供具体能力的类型。
     * 2. 它封装当前声明对应的核心操作和必要状态。
     * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
     * 4. 直接依赖的类型边界包括 `ConsumerWrapper`。
     * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
     * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
     */
    class ConsumerPerPartitionWrapper implements ConsumerWrapper {
        private final Map<TopicPartitionInfo, TbQueueConsumerTask> consumers = new HashMap<>();

        /**
         * 功能：更新`Partitions`。
         * 参数：
         * - `partitions`：分区标识或分区信息。
         * 返回：无。
         */
        @Override
        public void updatePartitions(Set<TopicPartitionInfo> partitions) {
            Set<TopicPartitionInfo> addedPartitions = new HashSet<>(partitions);
            addedPartitions.removeAll(consumers.keySet());

            Set<TopicPartitionInfo> removedPartitions = new HashSet<>(consumers.keySet());
            removedPartitions.removeAll(partitions);
            log.info("[{}] Added partitions: {}, removed partitions: {}", queueKey, partitionsToString(addedPartitions), partitionsToString(removedPartitions));

            removedPartitions.forEach((tpi) -> {
                consumers.get(tpi).initiateStop();
            });
            removedPartitions.forEach((tpi) -> {
                consumers.remove(tpi).awaitCompletion();
            });

            addedPartitions.forEach((tpi) -> {
                String key = queueKey + "-" + tpi.getPartition().orElse(-999999);
                TbQueueConsumerTask consumer = new TbQueueConsumerTask(key, ctx.getQueueFactory().createToRuleEngineMsgConsumer(queue));
                consumers.put(tpi, consumer);
                consumer.subscribe(Set.of(tpi));
                launchConsumer(consumer);
            });
        }

        /**
         * 功能：获取`Consumers`。
         * 参数：无。
         * 返回：匹配的数据集合。
         */
        @Override
        public Collection<TbQueueConsumerTask> getConsumers() {
            return consumers.values();
        }
    }

    /**
     * 中文说明：
     * 1. `SingleConsumerWrapper` 是 ThingsBoard Application 中围绕 `Single Consumer Wrapper` 提供具体能力的类型。
     * 2. 它封装当前声明对应的核心操作和必要状态。
     * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
     * 4. 直接依赖的类型边界包括 `ConsumerWrapper`。
     * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
     * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
     */
    class SingleConsumerWrapper implements ConsumerWrapper {
        /**
         * 消息消费者，承载当前流程需要传递的内容。
         */
        private TbQueueConsumerTask consumer;

        /**
         * 功能：更新`Partitions`。
         * 参数：
         * - `partitions`：分区标识或分区信息。
         * 返回：无。
         */
        @Override
        public void updatePartitions(Set<TopicPartitionInfo> partitions) {
            log.info("[{}] New partitions: {}", queueKey, partitionsToString(partitions));
            if (partitions.isEmpty()) {
                if (consumer != null && consumer.isRunning()) {
                    consumer.initiateStop();
                    consumer.awaitCompletion();
                }
                consumer = null;
                return;
            }

            if (consumer == null) {
                consumer = new TbQueueConsumerTask(queueKey, ctx.getQueueFactory().createToRuleEngineMsgConsumer(queue));
            }
            consumer.subscribe(partitions);
            if (!consumer.isRunning()) {
                launchConsumer(consumer);
            }
        }

        /**
         * 功能：获取`Consumers`。
         * 参数：无。
         * 返回：匹配的数据集合。
         */
        @Override
        public Collection<TbQueueConsumerTask> getConsumers() {
            if (consumer == null) {
                return Collections.emptyList();
            }
            return List.of(consumer);
        }
    }

}
