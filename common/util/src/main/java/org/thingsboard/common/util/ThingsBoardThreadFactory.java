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
 * 1. `ThingsBoardThreadFactory` 是 ThingsBoard Common 中创建或提供 `Things Board Thread` 对象的构造组件。
 * 2. 它根据输入配置、类型或上下文选择合适的具体实现。
 * 3. 创建细节被集中在该类型中，调用方只依赖稳定的创建入口。
 * 4. 直接依赖的类型边界包括 `ThreadFactory`。
 * 5. 独立工厂可以避免调用方了解构造顺序和实现类选择规则。
 * 6. 阅读时重点关注实现选择条件、默认分支和对象初始化参数。
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
