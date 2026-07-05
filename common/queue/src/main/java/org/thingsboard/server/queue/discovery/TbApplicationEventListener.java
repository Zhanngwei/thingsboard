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
package org.thingsboard.server.queue.discovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.thingsboard.server.queue.discovery.event.TbApplicationEvent;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 中文说明：
 * 1. 类目的：`TbApplicationEventListener` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
public abstract class TbApplicationEventListener<T extends TbApplicationEvent> implements ApplicationListener<T> {

    /**
     * `lastProcessedSequenceNumber` 字段，保存当前对象的对应属性。
     */
    private int lastProcessedSequenceNumber = Integer.MIN_VALUE;
    private final Lock seqNumberLock = new ReentrantLock();

    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * 功能：处理事件。
     * 参数：
     * - `event`：`event` 参数。
     * 返回：无。
     */
    @Override
    public void onApplicationEvent(T event) {
        if (!filterTbApplicationEvent(event)) {
            log.trace("Skipping event due to filter: {}", event);
            return;
        }
        boolean validUpdate = false;
        seqNumberLock.lock();
        try {
            if (event.getSequenceNumber() > lastProcessedSequenceNumber) {
                validUpdate = true;
                lastProcessedSequenceNumber = event.getSequenceNumber();
            }
        } finally {
            seqNumberLock.unlock();
        }
        if (validUpdate) {
            try {
                onTbApplicationEvent(event);
            } catch (Exception e) {
                log.error("Failed to handle partition change event: {}", event, e);
            }
        } else {
            log.info("Application event ignored due to invalid sequence number ({} > {}). Event: {}", lastProcessedSequenceNumber, event.getSequenceNumber(), event);
        }
    }

    /**
     * 功能：处理事件。
     * 参数：
     * - `event`：`event` 参数。
     * 返回：无。
     */
    protected abstract void onTbApplicationEvent(T event);

    /**
     * 功能：执行 `filterTbApplicationEvent` 对应的处理。
     * 参数：
     * - `event`：`event` 参数。
     * 返回：判断结果。
     */
    protected boolean filterTbApplicationEvent(T event) {
        return true;
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`TbApplicationEventListener` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
