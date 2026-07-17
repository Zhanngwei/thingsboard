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
package org.thingsboard.server.queue.common;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.TbStopWatch;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.stats.MessagesStats;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.TbQueueMsgMetadata;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.TbQueueRequestTemplate;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 中文说明：
 * 1. `DefaultTbQueueRequestTemplate` 是 ThingsBoard Common Queue 中承载请求信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `TbQueueMsg`、`TbQueueRequestTemplate`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Slf4j
public class DefaultTbQueueRequestTemplate<Request extends TbQueueMsg, Response extends TbQueueMsg> extends AbstractTbQueueTemplate
        implements TbQueueRequestTemplate<Request, Response> {

    /**
     * 队列，用于标识消息投递或消费的队列。
     */
    private final TbQueueAdmin queueAdmin;
    private final TbQueueProducer<Request> requestTemplate;
    /**
     * 当前响应对象，封装处理完成后的返回信息。
     */
    private final TbQueueConsumer<Response> responseTemplate;
    final ConcurrentHashMap<UUID, DefaultTbQueueRequestTemplate.ResponseMetaData<Response>> pendingRequests = new ConcurrentHashMap<>();
    /**
     * 是否满足执行器条件。
     */
    final boolean internalExecutor;
    final ExecutorService executor;
    /**
     * 当前请求对象，封装本次处理需要的输入信息。
     */
    final long maxRequestTimeoutNs;
    final long maxRequestTimeout;
    /**
     * 当前请求对象，封装本次处理需要的输入信息。
     */
    final long maxPendingRequests;
    final long pollInterval;
    /**
     * 当前处理是否已经停止。
     */
    volatile boolean stopped = false;
    long nextCleanupNs = 0L;
    private final Lock cleanerLock = new ReentrantLock();

    /**
     * `messagesStats` 字段，保存当前对象的对应属性。
     */
    private MessagesStats messagesStats;

    /**
     * 功能：创建 `DefaultTbQueueRequestTemplate` 实例，并初始化必要字段。
     * 参数：
     * - `queueAdmin`：队列名称或队列对象。
     * - `requestTemplate`：请求对象。
     * - `responseTemplate`：响应对象。
     * - `maxRequestTimeout`：请求对象。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    @Builder
    public DefaultTbQueueRequestTemplate(TbQueueAdmin queueAdmin,
                                         TbQueueProducer<Request> requestTemplate,
                                         TbQueueConsumer<Response> responseTemplate,
                                         long maxRequestTimeout,
                                         long maxPendingRequests,
                                         long pollInterval,
                                         @Nullable ExecutorService executor) {
        this.queueAdmin = queueAdmin;
        this.requestTemplate = requestTemplate;
        this.responseTemplate = responseTemplate;
        this.maxRequestTimeoutNs = TimeUnit.MILLISECONDS.toNanos(maxRequestTimeout);
        this.maxRequestTimeout = maxRequestTimeout;
        this.maxPendingRequests = maxPendingRequests;
        this.pollInterval = pollInterval;
        this.internalExecutor = (executor == null);
        this.executor = internalExecutor ? createExecutor() : executor;
    }

    /**
     * 功能：保存或创建执行器。
     * 参数：无。
     * 返回：处理结果。
     */
    ExecutorService createExecutor() {
        return Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("tb-queue-request-template-" + responseTemplate.getTopic()));
    }

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void init() {
        queueAdmin.createTopicIfNotExists(responseTemplate.getTopic());
        requestTemplate.init();
        responseTemplate.subscribe();
        executor.submit(this::mainLoop);
    }

    /**
     * 功能：执行 `mainLoop` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    void mainLoop() {
        while (!stopped) {
            TbStopWatch sw = TbStopWatch.create();
            try {
                fetchAndProcessResponses();
            } catch (Throwable e) {
                long sleepNanos = TimeUnit.MILLISECONDS.toNanos(this.pollInterval) - sw.stopAndGetTotalTimeNanos();
                log.warn("Failed to obtain and process responses from queue. Going to sleep " + sleepNanos + "ns", e);
                sleep(sleepNanos);
            }
        }
    }

    /**
     * 功能：获取`And Process Responses`。
     * 参数：无。
     * 返回：无。
     */
    void fetchAndProcessResponses() {
        final long pendingRequestsCount = pendingRequests.mappingCount();
        log.trace("Starting template pool topic {}, for pendingRequests {}", responseTemplate.getTopic(), pendingRequestsCount);
        List<Response> responses = doPoll(); //poll js responses
        log.trace("Completed template poll topic {}, for pendingRequests [{}], received [{}] responses", responseTemplate.getTopic(), pendingRequestsCount, responses.size());
        responses.forEach(this::processResponse); //this can take a long time
        responseTemplate.commit();
        tryCleanStaleRequests();
    }

    /**
     * 功能：执行 `tryCleanStaleRequests` 对应的处理。
     * 参数：无。
     * 返回：判断结果。
     */
    private boolean tryCleanStaleRequests() {
        if (!cleanerLock.tryLock()) {
            return false;
        }
        try {
            log.trace("tryCleanStaleRequest...");
            final long currentNs = getCurrentClockNs();
            if (nextCleanupNs < currentNs) {
                pendingRequests.forEach((key, value) -> {
                    if (value.expTime < currentNs) {
                        ResponseMetaData<Response> staleRequest = pendingRequests.remove(key);
                        if (staleRequest != null) {
                            setTimeoutException(key, staleRequest, currentNs);
                        }
                    }
                });
                setupNextCleanup();
            }
        } finally {
            cleanerLock.unlock();
        }
        return true;
    }

    /**
     * 功能：执行 `setupNextCleanup` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    void setupNextCleanup() {
        nextCleanupNs = getCurrentClockNs() + maxRequestTimeoutNs;
        log.trace("setupNextCleanup {}", nextCleanupNs);
    }

    /**
     * 功能：执行 `doPoll` 对应的处理。
     * 参数：无。
     * 返回：匹配的数据集合。
     */
    List<Response> doPoll() {
        return responseTemplate.poll(pollInterval);
    }

    /**
     * 功能：执行 `sleep` 对应的处理。
     * 参数：
     * - `nanos`：`nanos` 参数。
     * 返回：无。
     */
    void sleep(long nanos) {
        LockSupport.parkNanos(nanos);
    }

    /**
     * 功能：更新超时时间。
     * 参数：
     * - `key`：键。
     * - `staleRequest`：请求对象。
     * - `currentNs`：`currentNs` 参数。
     * 返回：无。
     */
    void setTimeoutException(UUID key, ResponseMetaData<Response> staleRequest, long currentNs) {
        if (currentNs >= staleRequest.getSubmitTime() + staleRequest.getTimeout()) {
            log.debug("Request timeout detected, currentNs [{}], {}, key [{}]", currentNs, staleRequest, key);
        } else {
            log.info("Request timeout detected, currentNs [{}], {}, key [{}]", currentNs, staleRequest, key);
        }
        staleRequest.future.setException(new TimeoutException());
    }

    /**
     * 功能：处理响应。
     * 参数：
     * - `response`：响应对象。
     * 返回：无。
     */
    void processResponse(Response response) {
        byte[] requestIdHeader = response.getHeaders().get(REQUEST_ID_HEADER);
        UUID requestId;
        if (requestIdHeader == null) {
            log.error("[{}] Missing requestId in header and body", response);
        } else {
            requestId = bytesToUuid(requestIdHeader);
            log.trace("[{}] Response received: {}", requestId, response);
            ResponseMetaData<Response> expectedResponse = pendingRequests.remove(requestId);
            if (expectedResponse == null) {
                log.debug("[{}] Invalid or stale request, response: {}", requestId, String.valueOf(response).replace("\n", " "));
            } else {
                expectedResponse.future.set(response);
            }
        }
    }

    /**
     * 功能：执行 `stop` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void stop() {
        stopped = true;

        if (responseTemplate != null) {
            responseTemplate.unsubscribe();
        }

        if (requestTemplate != null) {
            requestTemplate.stop();
        }

        if (internalExecutor) {
            executor.shutdownNow();
        }
    }

    /**
     * 功能：更新`Messages Stats`。
     * 参数：
     * - `messagesStats`：待处理消息。
     * 返回：无。
     */
    @Override
    public void setMessagesStats(MessagesStats messagesStats) {
        this.messagesStats = messagesStats;
    }

    /**
     * 功能：执行 `send` 对应的处理。
     * 参数：
     * - `request`：请求对象。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<Response> send(Request request) {
        return send(request, this.maxRequestTimeoutNs);
    }

    /**
     * 功能：执行 `send` 对应的处理。
     * 参数：
     * - `request`：请求对象。
     * - `requestTimeoutNs`：请求对象。
     * 返回：匹配的数据集合。
     */
    @Override
    public ListenableFuture<Response> send(Request request, long requestTimeoutNs) {
        if (pendingRequests.mappingCount() >= maxPendingRequests) {
            log.warn("Pending request map is full [{}]! Consider to increase maxPendingRequests or increase processing performance. Request is {}", maxPendingRequests, request);
            return Futures.immediateFailedFuture(new RuntimeException("Pending request map is full!"));
        }
        UUID requestId = UUID.randomUUID();
        request.getHeaders().put(REQUEST_ID_HEADER, uuidToBytes(requestId));
        request.getHeaders().put(RESPONSE_TOPIC_HEADER, stringToBytes(responseTemplate.getTopic()));
        request.getHeaders().put(EXPIRE_TS_HEADER, longToBytes(getCurrentTimeMs() + maxRequestTimeout));
        long currentClockNs = getCurrentClockNs();
        SettableFuture<Response> future = SettableFuture.create();
        ResponseMetaData<Response> responseMetaData = new ResponseMetaData<>(currentClockNs + requestTimeoutNs, future, currentClockNs, requestTimeoutNs);
        log.trace("pending {}", responseMetaData);
        if (pendingRequests.putIfAbsent(requestId, responseMetaData) != null) {
            log.warn("Pending request already exists [{}]!", maxPendingRequests);
            return Futures.immediateFailedFuture(new RuntimeException("Pending request already exists !" + requestId));
        }
        sendToRequestTemplate(request, requestId, future, responseMetaData);
        return future;
    }

    /**
     * MONOTONIC clock instead jumping wall clock.
     * Wrapped into the method for the test purposes to travel through the time
     * */
    /**
     * 功能：获取`Current Clock Ns`。
     * 参数：无。
     * 返回：数值结果。
     */
    long getCurrentClockNs() {
        return System.nanoTime();
    }

    /**
     * Wall clock to send timestamp to an external service
     * */
    /**
     * 功能：获取时间。
     * 参数：无。
     * 返回：数值结果。
     */
    long getCurrentTimeMs() {
        return System.currentTimeMillis();
    }

    /**
     * 功能：发送或提交请求。
     * 参数：
     * - `request`：请求对象。
     * - `requestId`：请求ID。
     * - `future`：`future` 参数。
     * - `responseMetaData`：响应对象。
     * 返回：无。
     */
    void sendToRequestTemplate(Request request, UUID requestId, SettableFuture<Response> future, ResponseMetaData<Response> responseMetaData) {
        log.trace("[{}] Sending request, key [{}], expTime [{}], request {}", requestId, request.getKey(), responseMetaData.expTime, request);
        if (messagesStats != null) {
            messagesStats.incrementTotal();
        }
        requestTemplate.send(TopicPartitionInfo.builder().topic(requestTemplate.getDefaultTopic()).build(), request, new TbQueueCallback() {
            @Override
            public void onSuccess(TbQueueMsgMetadata metadata) {
                if (messagesStats != null) {
                    messagesStats.incrementSuccessful();
                }
                log.trace("[{}] Request sent: {}, request {}", requestId, metadata, request);
            }

            @Override
            public void onFailure(Throwable t) {
                if (messagesStats != null) {
                    messagesStats.incrementFailed();
                }
                pendingRequests.remove(requestId);
                future.setException(t);
            }
        });
    }

    /**
     * 中文说明：
     * 1. `ResponseMetaData` 是 ThingsBoard Common Queue 中承载响应信息的数据类型。
     * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
     * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
     * 4. 它直接协作于创建该对象的生产方和读取字段的消费方。
     * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
     * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
     */
    @Getter
    static class ResponseMetaData<T> {
        /**
         * 时间，用于控制时间范围或等待时长。
         */
        private final long submitTime;
        private final long timeout;
        /**
         * 时间，用于控制时间范围或等待时长。
         */
        private final long expTime;
        private final SettableFuture<T> future;

        ResponseMetaData(long ts, SettableFuture<T> future, long submitTime, long timeout) {
            this.submitTime = submitTime;
            this.timeout = timeout;
            this.expTime = ts;
            this.future = future;
        }

        /**
         * 功能：生成当前对象的文本表示。
         * 参数：无。
         * 返回：文本结果。
         */
        @Override
        public String toString() {
            return "ResponseMetaData{" +
                    "submitTime=" + submitTime +
                    ", calculatedExpTime=" + (submitTime + timeout) +
                    ", deltaMs=" + (expTime - submitTime) +
                    ", expTime=" + expTime +
                    ", future=" + future +
                    '}';
        }
    }

}
