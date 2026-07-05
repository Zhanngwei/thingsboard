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
package org.thingsboard.server.queue.memory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.queue.TbQueueMsg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 中文说明：
 * 1. 类目的：`DefaultInMemoryStorage` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Component
@Slf4j
public final class DefaultInMemoryStorage implements InMemoryStorage {
    private final ConcurrentHashMap<String, BlockingQueue<TbQueueMsg>> storage = new ConcurrentHashMap<>();

    /**
     * 功能：执行 `printStats` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void printStats() {
        if (log.isDebugEnabled()) {
            storage.forEach((topic, queue) -> {
                if (queue.size() > 0) {
                    log.debug("[{}] Queue Size [{}]", topic, queue.size());
                }
            });
        }
    }

    /**
     * 功能：获取`Lag Total`。
     * 参数：无。
     * 返回：数值结果。
     */
    @Override
    public int getLagTotal() {
        return storage.values().stream().map(BlockingQueue::size).reduce(0, Integer::sum);
    }

    /**
     * 功能：执行 `put` 对应的处理。
     * 参数：
     * - `topic`：主题名称或主题对象。
     * - `msg`：待处理消息。
     * 返回：判断结果。
     */
    @Override
    public boolean put(String topic, TbQueueMsg msg) {
        return storage.computeIfAbsent(topic, (t) -> new LinkedBlockingQueue<>()).add(msg);
    }

    /**
     * 功能：执行 `get` 对应的处理。
     * 参数：
     * - `topic`：主题名称或主题对象。
     * 返回：匹配的数据集合。
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T extends TbQueueMsg> List<T> get(String topic) throws InterruptedException {
        final BlockingQueue<TbQueueMsg> queue = storage.get(topic);
        if (queue != null) {
            final TbQueueMsg firstMsg = queue.poll();
            if (firstMsg != null) {
                final int queueSize = queue.size();
                if (queueSize > 0) {
                    final List<TbQueueMsg> entities = new ArrayList<>(Math.min(queueSize, 999) + 1);
                    entities.add(firstMsg);
                    queue.drainTo(entities, 999);
                    return (List<T>) entities;
                }
                return Collections.singletonList((T) firstMsg);
            }
        }
        return Collections.emptyList();
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`DefaultInMemoryStorage` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
