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

/**
 * 中文说明：
 * 1. `FailedToInitActor` 是 ThingsBoard Actor 中承载 `Failed To Init` 信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `TestRootActor`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
@Slf4j
public class FailedToInitActor extends TestRootActor {

    /**
     * 重试次数，表示当前对象的对应属性。
     */
    int retryAttempts;
    int retryDelay;
    /**
     * 尝试次数，表示当前对象的对应属性。
     */
    int attempts = 0;

    /**
     * 功能：创建 `FailedToInitActor` 实例，并初始化必要字段。
     * 参数：
     * - `actorId`：Actor 实例ID。
     * - `testCtx`：处理上下文。
     * - `retryAttempts`：`retryAttempts` 参数。
     * - `retryDelay`：`retryDelay` 参数。
     * 返回：新创建的对象实例。
     */
    public FailedToInitActor(TbActorId actorId, ActorTestCtx testCtx, int retryAttempts, int retryDelay) {
        super(actorId, testCtx);
        this.retryAttempts = retryAttempts;
        this.retryDelay = retryDelay;
    }

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * 返回：无。
     */
    @Override
    public void init(TbActorCtx ctx) throws TbActorException {
        if (attempts < retryAttempts) {
            attempts++;
            throw new TbActorException("Test attempt", new RuntimeException());
        } else {
            super.init(ctx);
        }
    }

    /**
     * 功能：处理失败信息。
     * 参数：
     * - `attempt`：`attempt` 参数。
     * - `t`：`t` 参数。
     * 返回：处理结果。
     */
    @Override
    public InitFailureStrategy onInitFailure(int attempt, Throwable t) {
        return InitFailureStrategy.retryWithDelay(retryDelay);
    }

    /**
     * 中文说明：
     * 1. `FailedToInitActorCreator` 是 ThingsBoard Actor 中创建或提供 `Failed To Init` 对象的构造组件。
     * 2. 它根据输入配置、类型或上下文选择合适的具体实现。
     * 3. 创建细节被集中在该类型中，调用方只依赖稳定的创建入口。
     * 4. 直接依赖的类型边界包括 `TbActorCreator`。
     * 5. 独立工厂可以避免调用方了解构造顺序和实现类选择规则。
     * 6. 阅读时重点关注实现选择条件、默认分支和对象初始化参数。
     */
    public static class FailedToInitActorCreator implements TbActorCreator {

        /**
         * Actor 实例ID，用于定位对应业务对象。
         */
        private final TbActorId actorId;
        private final ActorTestCtx testCtx;
        /**
         * 重试次数，表示当前对象的对应属性。
         */
        private final int retryAttempts;
        private final int retryDelay;

        /**
         * 功能：创建 `FailedToInitActor` 实例，并初始化必要字段。
         * 参数：
         * - `actorId`：Actor 实例ID。
         * - `testCtx`：处理上下文。
         * - `retryAttempts`：`retryAttempts` 参数。
         * - `retryDelay`：`retryDelay` 参数。
         * 返回：新创建的对象实例。
         */
        public FailedToInitActorCreator(TbActorId actorId, ActorTestCtx testCtx, int retryAttempts, int retryDelay) {
            this.actorId = actorId;
            this.testCtx = testCtx;
            this.retryAttempts = retryAttempts;
            this.retryDelay = retryDelay;
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
            return new FailedToInitActor(actorId, testCtx, retryAttempts, retryDelay);
        }
    }
}
