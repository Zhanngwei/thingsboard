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
package org.thingsboard.rule.engine;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.common.util.ListeningExecutor;

import java.util.concurrent.Callable;

/**
 * 测试目标：验证 {@code TestDbCallbackExecutor} 覆盖的 规则引擎组件 行为，重点说明配置、消息和断言路径。
 * 所属生产节点/组件：{@code TestDbCallbackExecutor}，用于守护对应 Rule Engine 组件的兼容性和边界条件。
 * Mock 依赖来源：内存 fixture、参数化数据源，以及测试体按需创建的 Mockito mock/spy；测试不启动真实外部服务。
 * 被验证流程：准备 fixture，初始化节点或工具对象，触发被测调用，再断言输出、异常或 Mock 交互。
 * 存在原因：防止规则引擎组件在升级、消息处理、异步回调或数据映射场景中发生回归。
 */
public class TestDbCallbackExecutor implements ListeningExecutor {

    /** 实现方法：{@code executeAsync} 为测试替身或抽象基类提供最小行为，输入来自调用方，生命周期随 enclosing fixture。 */
    @Override
    public <T> ListenableFuture<T> executeAsync(Callable<T> task) {
        try {
            return Futures.immediateFuture(task.call());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** 实现方法：{@code execute} 为测试替身或抽象基类提供最小行为，输入来自调用方，生命周期随 enclosing fixture。 */
    @Override
    public void execute(Runnable command) {
        command.run();
    }

}
/*
 * 本类总结：{@code TestDbCallbackExecutor} 为 {@code TestDbCallbackExecutor} 的 规则引擎组件 测试提供中文注释，说明测试目标、fixture 生命周期、Mock 来源和断言流程。
 * 本文件中的数据库、缓存、MQTT、Actor 或完整 Rule Engine 运行时均不由测试本身直接启动；相关行为通过 Mock、内存 fixture 或被测生产逻辑间接覆盖。
 */
