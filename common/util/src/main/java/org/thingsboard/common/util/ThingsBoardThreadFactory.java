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
package org.thingsboard.common.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Copy of Executors.DefaultThreadFactory but with ability to set name of the pool
 */
/**
 * 中文说明：
 * 1. 类目的：`ThingsBoardThreadFactory` 是ThingsBoard Common 模块中的公共工具类型，用于提供跨模块复用的纯函数、解析、转换或辅助逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括Application、DAO、Transport、Rule Engine、测试工具和第三方库。
 * 4. 生命周期：通常作为静态工具或轻量对象按需调用，不持有长生命周期业务状态。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Utility / Helper。
 */
public class ThingsBoardThreadFactory implements ThreadFactory {
    /**
     * 主题常量，用于统一引用固定值。
     */
    public static final String THREAD_TOPIC_SEPARATOR = " | ";
    private static final AtomicInteger poolNumber = new AtomicInteger(1);
    /**
     * `group` 字段，保存当前对象的对应属性。
     */
    private final ThreadGroup group;
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    /**
     * 名称，用于展示或标识当前对象。
     */
    private final String namePrefix;

    /**
     * 功能：执行 `forName` 对应的处理。
     * 参数：
     * - `name`：名称。
     * 返回：处理结果。
     */
    public static ThingsBoardThreadFactory forName(String name) {
        return new ThingsBoardThreadFactory(name);
    }

    /**
     * 功能：创建 `ThingsBoardThreadFactory` 实例，并初始化必要字段。
     * 参数：
     * - `name`：名称。
     * 返回：新创建的对象实例。
     */
    private ThingsBoardThreadFactory(String name) {
        SecurityManager s = System.getSecurityManager();
        group = (s != null) ? s.getThreadGroup() :
                Thread.currentThread().getThreadGroup();
        namePrefix = name + "-" +
                poolNumber.getAndIncrement() +
                "-thread-";
    }

    /**
     * 功能：更新名称。
     * 参数：
     * - `threadSuffix`：`threadSuffix` 参数。
     * 返回：无。
     */
    public static void updateCurrentThreadName(String threadSuffix) {
        String name = Thread.currentThread().getName();
        int spliteratorIndex = name.indexOf(THREAD_TOPIC_SEPARATOR);
        if (spliteratorIndex > 0) {
            name = name.substring(0, spliteratorIndex);
        }
        name = name + THREAD_TOPIC_SEPARATOR + threadSuffix;
        Thread.currentThread().setName(name);
    }


    /**
     * 功能：执行 `newThread` 对应的处理。
     * 参数：
     * - `r`：`r` 参数。
     * 返回：处理结果。
     */
    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(group, r,
                namePrefix + threadNumber.getAndIncrement(),
                0);
        if (t.isDaemon())
            t.setDaemon(false);
        if (t.getPriority() != Thread.NORM_PRIORITY)
            t.setPriority(Thread.NORM_PRIORITY);
        return t;
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`ThingsBoardThreadFactory` 在 ThingsBoard Common 模块 中承担公共工具类型职责，核心目的是提供跨模块复用的纯函数、解析、转换或辅助逻辑。
 * 2. 核心流程：接收输入参数后执行本地转换、校验或解析并返回结果。
 * 3. 关键依赖：主要依赖或协作对象包括Application、DAO、Transport、Rule Engine、测试工具和第三方库。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
