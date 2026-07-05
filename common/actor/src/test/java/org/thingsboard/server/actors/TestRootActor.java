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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.TbActorStopReason;

/**
 * 中文说明：
 * 1. 类目的：`TestRootActor` 是ThingsBoard Common 测试模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、序列化框架、协议处理器、队列消费者或测试框架按需创建和使用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@Slf4j
public class TestRootActor extends AbstractTbActor {

    /**
     * Actor 实例ID，用于定位对应业务对象。
     */
    @Getter
    private final TbActorId actorId;
    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    @Getter
    private final ActorTestCtx testCtx;

    /**
     * 是否满足`initialized`条件。
     */
    private boolean initialized;
    private long sum;
    /**
     * 数量，用于控制数量、位置或分页范围。
     */
    private int count;

    /**
     * 功能：创建 `TestRootActor` 实例，并初始化必要字段。
     * 参数：
     * - `actorId`：Actor 实例ID。
     * - `testCtx`：处理上下文。
     * 返回：新创建的对象实例。
     */
    public TestRootActor(TbActorId actorId, ActorTestCtx testCtx) {
        this.actorId = actorId;
        this.testCtx = testCtx;
    }

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * 返回：无。
     */
    @Override
    public void init(TbActorCtx ctx) throws TbActorException {
        super.init(ctx);
        initialized = true;
    }

    /**
     * 功能：执行 `process` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：判断结果。
     */
    @Override
    public boolean process(TbActorMsg msg) {
        if (initialized) {
            int value = ((IntTbActorMsg) msg).getValue();
            sum += value;
            count += 1;
            if (count == testCtx.getExpectedInvocationCount()) {
                testCtx.getActual().set(sum);
                testCtx.getInvocationCount().addAndGet(count);
                sum = 0;
                count = 0;
                testCtx.getLatch().countDown();
            }
        }
        return true;
    }

    /**
     * 功能：执行 `destroy` 对应的处理。
     * 参数：
     * - `stopReason`：`stopReason` 参数。
     * - `cause`：`cause` 参数。
     * 返回：无。
     */
    @Override
    public void destroy(TbActorStopReason stopReason, Throwable cause) {

    }

    /**
     * 中文说明：
     * 1. 类目的：`TestRootActorCreator` 是ThingsBoard Common 测试模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
     * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
     * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
     * 4. 生命周期：由调用模块、序列化框架、协议处理器、队列消费者或测试框架按需创建和使用。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 DTO / Contract / Adapter。
     */
    public static class TestRootActorCreator implements TbActorCreator {

        /**
         * Actor 实例ID，用于定位对应业务对象。
         */
        private final TbActorId actorId;
        private final ActorTestCtx testCtx;

        /**
         * 功能：创建 `TestRootActor` 实例，并初始化必要字段。
         * 参数：
         * - `actorId`：Actor 实例ID。
         * - `testCtx`：处理上下文。
         * 返回：新创建的对象实例。
         */
        public TestRootActorCreator(TbActorId actorId, ActorTestCtx testCtx) {
            this.actorId = actorId;
            this.testCtx = testCtx;
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
            return new TestRootActor(actorId, testCtx);
        }
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`TestRootActor` 在 ThingsBoard Common 测试模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
