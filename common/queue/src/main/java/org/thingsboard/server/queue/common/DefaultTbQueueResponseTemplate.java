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

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.stats.MessagesStats;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueHandler;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.TbQueueResponseTemplate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 中文说明：
 * 1. `DefaultTbQueueResponseTemplate` 是 ThingsBoard Common Queue 中承载响应信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `TbQueueMsg`、`TbQueueResponseTemplate`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Slf4j
public class DefaultTbQueueResponseTemplate<Request extends TbQueueMsg, Response extends TbQueueMsg> extends AbstractTbQueueTemplate
        implements TbQueueResponseTemplate<Request, Response> {

    /**
     * 当前请求对象，封装本次处理需要的输入信息。
     */
    private final TbQueueConsumer<Request> requestTemplate;
    private final TbQueueProducer<Response> responseTemplate;
    /**
     * `pendingRequests`映射关系，用于按键查找对应值。
     */
    private final ConcurrentMap<UUID, String> pendingRequests;
    private final ExecutorService loopExecutor;
    /**
     * 执行器，负责处理对应任务或消息。
     */
    private final ScheduledExecutorService timeoutExecutor;
    private final ExecutorService callbackExecutor;
    /**
     * `stats` 字段，保存当前对象的对应属性。
     */
    private final MessagesStats stats;
    private final int maxPendingRequests;
    /**
     * 当前请求对象，封装本次处理需要的输入信息。
     */
    private final long requestTimeout;

    /**
     * 时间间隔，用于控制时间范围或等待时长。
     */
    private final long pollInterval;
    private volatile boolean stopped = false;
    private final AtomicInteger pendingRequestCount = new AtomicInteger();

    /**
     * 功能：创建 `DefaultTbQueueResponseTemplate` 实例，并初始化必要字段。
     * 参数：
     * - `requestTemplate`：请求对象。
     * - `responseTemplate`：响应对象。
     * - `handler`：处理器对象。
     * - `pollInterval`：`pollInterval` 参数。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    @Builder
    public DefaultTbQueueResponseTemplate(TbQueueConsumer<Request> requestTemplate,
                                          TbQueueProducer<Response> responseTemplate,
                                          TbQueueHandler<Request, Response> handler,
                                          long pollInterval,
                                          long requestTimeout,
                                          int maxPendingRequests,
                                          ExecutorService executor,
                                          MessagesStats stats) {
        this.requestTemplate = requestTemplate;
        this.responseTemplate = responseTemplate;
        this.pendingRequests = new ConcurrentHashMap<>();
        this.maxPendingRequests = maxPendingRequests;
        this.pollInterval = pollInterval;
        this.requestTimeout = requestTimeout;
        this.callbackExecutor = executor;
        this.stats = stats;
        this.timeoutExecutor = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("tb-queue-response-template-timeout-" + requestTemplate.getTopic()));
        this.loopExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("tb-queue-response-template-loop-" + requestTemplate.getTopic()));
    }

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：
     * - `handler`：处理器对象。
     * 返回：无。
     */
    @Override
    public void init(TbQueueHandler<Request, Response> handler) {
        this.responseTemplate.init();
        requestTemplate.subscribe();
        loopExecutor.submit(() -> {
            while (!stopped) {
                try {
                    while (pendingRequestCount.get() >= maxPendingRequests) {
                        try {
                            Thread.sleep(pollInterval);
                        } catch (InterruptedException e) {
                            log.trace("Failed to wait until the server has capacity to handle new requests", e);
                        }
                    }
                    List<Request> requests = requestTemplate.poll(pollInterval);

                    if (requests.isEmpty()) {
                        continue;
                    }

                    requests.forEach(request -> {
                        long currentTime = System.currentTimeMillis();
                        long expireTs = bytesToLong(request.getHeaders().get(EXPIRE_TS_HEADER));
                        if (expireTs >= currentTime) {
                            byte[] requestIdHeader = request.getHeaders().get(REQUEST_ID_HEADER);
                            if (requestIdHeader == null) {
                                log.error("[{}] Missing requestId in header", request);
                                return;
                            }
                            byte[] responseTopicHeader = request.getHeaders().get(RESPONSE_TOPIC_HEADER);
                            if (responseTopicHeader == null) {
                                log.error("[{}] Missing response topic in header", request);
                                return;
                            }
                            UUID requestId = bytesToUuid(requestIdHeader);
                            String responseTopic = bytesToString(responseTopicHeader);
                            try {
                                pendingRequestCount.getAndIncrement();
                                stats.incrementTotal();
                                AsyncCallbackTemplate.withCallbackAndTimeout(handler.handle(request),
                                        response -> {
                                            pendingRequestCount.decrementAndGet();
                                            response.getHeaders().put(REQUEST_ID_HEADER, uuidToBytes(requestId));
                                            responseTemplate.send(TopicPartitionInfo.builder().topic(responseTopic).build(), response, null);
                                            stats.incrementSuccessful();
                                        },
                                        e -> {
                                            pendingRequestCount.decrementAndGet();
                                            if (e.getCause() != null && e.getCause() instanceof TimeoutException) {
                                                log.warn("[{}] Timeout to process the request: {}", requestId, request, e);
                                            } else {
                                                log.trace("[{}] Failed to process the request: {}", requestId, request, e);
                                            }
                                            stats.incrementFailed();
                                        },
                                        requestTimeout,
                                        timeoutExecutor,
                                        callbackExecutor);
                            } catch (Throwable e) {
                                pendingRequestCount.decrementAndGet();
                                log.warn("[{}] Failed to process the request: {}", requestId, request, e);
                                stats.incrementFailed();
                            }
                        }
                    });
                    requestTemplate.commit();
                } catch (Throwable e) {
                    log.warn("Failed to obtain messages from queue.", e);
                    try {
                        Thread.sleep(pollInterval);
                    } catch (InterruptedException e2) {
                        log.trace("Failed to wait until the server has capacity to handle new requests", e2);
                    }
                }
            }
        });
    }

    /**
     * 功能：执行 `stop` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void stop() {
        stopped = true;
        if (requestTemplate != null) {
            requestTemplate.unsubscribe();
        }
        if (responseTemplate != null) {
            responseTemplate.stop();
        }
        if (timeoutExecutor != null) {
            timeoutExecutor.shutdownNow();
        }
        if (loopExecutor != null) {
            loopExecutor.shutdownNow();
        }
    }

}
