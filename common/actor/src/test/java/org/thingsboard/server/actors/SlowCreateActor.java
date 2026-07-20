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
package org.thingsboard.server.actors;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 中文说明：
 * 1. `SlowCreateActor` 是 ThingsBoard Actor 中处理 `Slow Create Actor` 消息的 Actor 类型。
 * 2. 它按消息顺序执行状态变更、路由或组件协调逻辑。
 * 3. 类内状态用于保存当前 Actor 处理消息所需的上下文和运行数据。
 * 4. 直接依赖的类型边界包括 `TestRootActor`。
 * 5. 使用独立 Actor 可以串行化同一业务对象的异步操作，并隔离并发状态。
 * 6. 阅读时重点关注消息分派入口、状态更新位置和向其它 Actor 发送消息的分支。
 */
@Slf4j
public class SlowCreateActor extends TestRootActor {

    /**
     * 超时时间常量，用于统一引用固定值。
     */
    public static final int TIMEOUT_AWAIT_MAX_MS = 5000;

    /**
     * 功能：创建 `SlowCreateActor` 实例，并初始化必要字段。
     * 参数：
     * - `actorId`：Actor 实例ID。
     * - `testCtx`：处理上下文。
     * - `initLatch`：`initLatch` 参数。
     * 返回：新创建的对象实例。
     */
    public SlowCreateActor(TbActorId actorId, ActorTestCtx testCtx, CountDownLatch initLatch) {
        super(actorId, testCtx);
        try {
            initLatch.await(TIMEOUT_AWAIT_MAX_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        testCtx.getInvocationCount().incrementAndGet();
    }

    /**
     * 中文说明：
     * 1. `SlowCreateActorCreator` 是 ThingsBoard Actor 中创建或提供 `Slow Create Actor` 对象的构造组件。
     * 2. 它根据输入配置、类型或上下文选择合适的具体实现。
     * 3. 创建细节被集中在该类型中，调用方只依赖稳定的创建入口。
     * 4. 直接依赖的类型边界包括 `TbActorCreator`。
     * 5. 独立工厂可以避免调用方了解构造顺序和实现类选择规则。
     * 6. 阅读时重点关注实现选择条件、默认分支和对象初始化参数。
     */
    public static class SlowCreateActorCreator implements TbActorCreator {

        /**
         * Actor 实例ID，用于定位对应业务对象。
         */
        private final TbActorId actorId;
        private final ActorTestCtx testCtx;
        /**
         * 初始化等待器，用于在测试或异步流程中等待结果。
         */
        private final CountDownLatch initLatch;

        /**
         * 功能：创建 `SlowCreateActor` 实例，并初始化必要字段。
         * 参数：
         * - `actorId`：Actor 实例ID。
         * - `testCtx`：处理上下文。
         * - `initLatch`：`initLatch` 参数。
         * 返回：新创建的对象实例。
         */
        public SlowCreateActorCreator(TbActorId actorId, ActorTestCtx testCtx, CountDownLatch initLatch) {
            this.actorId = actorId;
            this.testCtx = testCtx;
            this.initLatch = initLatch;
        }

        /**
         * 功能：保存或创建Actor 实例。
         * 参数：无。
         * 返回：处理结果。
         */
        @Override
        public TbActorId createActorId() {
            return actorId;
        }

        /**
         * 功能：保存或创建Actor 实例。
         * 参数：无。
         * 返回：处理结果。
         */
        @Override
        public TbActor createActor() {
            return new SlowCreateActor(actorId, testCtx, initLatch);
        }
    }
}
