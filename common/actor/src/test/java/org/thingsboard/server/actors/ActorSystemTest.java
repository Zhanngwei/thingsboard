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
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.id.DeviceId;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 中文说明：
 * 1. `ActorSystemTest` 是 ThingsBoard Actor 中验证 `ActorSystem` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 它直接协作于被测类型、测试框架和必要的模拟依赖。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class ActorSystemTest {

    /**
     * `ROOT_DISPATCHER`常量，用于统一引用固定值。
     */
    public static final String ROOT_DISPATCHER = "root-dispatcher";
    private static final int _100K = 100 * 1024;
    /**
     * 超时时间常量，用于统一引用固定值。
     */
    public static final int TIMEOUT_AWAIT_MAX_SEC = 100;

    /**
     * Actor 系统，表示当前对象的对应属性。
     */
    private volatile TbActorSystem actorSystem;
    private volatile ExecutorService submitPool;
    /**
     * 执行器，负责处理对应任务或消息。
     */
    private ExecutorService executor;
    private int parallelism;

    /**
     * 功能：初始化或启动Actor 系统。
     * 参数：无。
     * 返回：无。
     */
    @Before
    public void initActorSystem() {
        int cores = Runtime.getRuntime().availableProcessors();
        parallelism = Math.max(2, cores / 2);
        TbActorSystemSettings settings = new TbActorSystemSettings(5, parallelism, 42);
        actorSystem = new DefaultTbActorSystem(settings);
        submitPool = Executors.newFixedThreadPool(parallelism, ThingsBoardThreadFactory.forName(getClass().getSimpleName() + "-submit-test-scope")); //order guaranteed
    }

    /**
     * 功能：停止或关闭Actor 系统。
     * 参数：无。
     * 返回：无。
     */
    @After
    public void shutdownActorSystem() {
        actorSystem.stop();
        submitPool.shutdownNow();
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    /**
     * 功能：执行 `test1actorsAnd100KMessages` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void test1actorsAnd100KMessages() throws InterruptedException {
        executor = ThingsBoardExecutors.newWorkStealingPool(parallelism, getClass());
        actorSystem.createDispatcher(ROOT_DISPATCHER, executor);
        testActorsAndMessages(1, _100K, 1);
    }

    /**
     * 功能：执行 `test10actorsAnd100KMessages` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void test10actorsAnd100KMessages() throws InterruptedException {
        executor = ThingsBoardExecutors.newWorkStealingPool(parallelism, getClass());
        actorSystem.createDispatcher(ROOT_DISPATCHER, executor);
        testActorsAndMessages(10, _100K, 1);
    }

    /**
     * 功能：执行 `test100KActorsAnd1Messages5timesSingleThread` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void test100KActorsAnd1Messages5timesSingleThread() throws InterruptedException {
        executor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName(getClass().getSimpleName()));
        actorSystem.createDispatcher(ROOT_DISPATCHER, executor);
        testActorsAndMessages(_100K, 1, 5);
    }

    /**
     * 功能：执行 `test100KActorsAnd1Messages5times` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void test100KActorsAnd1Messages5times() throws InterruptedException {
        executor = ThingsBoardExecutors.newWorkStealingPool(parallelism, getClass());
        actorSystem.createDispatcher(ROOT_DISPATCHER, executor);
        testActorsAndMessages(_100K, 1, 5);
    }

    /**
     * 功能：执行 `test100KActorsAnd10Messages` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void test100KActorsAnd10Messages() throws InterruptedException {
        executor = ThingsBoardExecutors.newWorkStealingPool(parallelism, getClass());
        actorSystem.createDispatcher(ROOT_DISPATCHER, executor);
        testActorsAndMessages(_100K, 10, 1);
    }

    /**
     * 功能：执行 `test1KActorsAnd1KMessages` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void test1KActorsAnd1KMessages() throws InterruptedException {
        executor = ThingsBoardExecutors.newWorkStealingPool(parallelism, getClass());
        actorSystem.createDispatcher(ROOT_DISPATCHER, executor);
        testActorsAndMessages(1000, 1000, 10);
    }

    /**
     * 功能：验证`No Messages After Destroy`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testNoMessagesAfterDestroy() throws InterruptedException {
        executor = ThingsBoardExecutors.newWorkStealingPool(parallelism, getClass());
        actorSystem.createDispatcher(ROOT_DISPATCHER, executor);
        ActorTestCtx testCtx1 = getActorTestCtx(1);
        ActorTestCtx testCtx2 = getActorTestCtx(1);

        TbActorRef actorId1 = actorSystem.createRootActor(ROOT_DISPATCHER, new SlowInitActor.SlowInitActorCreator(
                new TbEntityActorId(new DeviceId(UUID.randomUUID())), testCtx1));
        TbActorRef actorId2 = actorSystem.createRootActor(ROOT_DISPATCHER, new SlowInitActor.SlowInitActorCreator(
                new TbEntityActorId(new DeviceId(UUID.randomUUID())), testCtx2));

        actorId1.tell(new IntTbActorMsg(42));
        actorId2.tell(new IntTbActorMsg(42));
        actorSystem.stop(actorId1);

        Assert.assertTrue(testCtx2.getLatch().await(1, TimeUnit.SECONDS));
        Assert.assertFalse(testCtx1.getLatch().await(1, TimeUnit.SECONDS));
    }

    /**
     * 功能：验证Actor 实例相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testOneActorCreated() throws InterruptedException {
        executor = ThingsBoardExecutors.newWorkStealingPool(parallelism, getClass());
        actorSystem.createDispatcher(ROOT_DISPATCHER, executor);
        ActorTestCtx testCtx1 = getActorTestCtx(1);
        ActorTestCtx testCtx2 = getActorTestCtx(1);
        TbActorId actorId = new TbEntityActorId(new DeviceId(UUID.randomUUID()));
        final CountDownLatch initLatch = new CountDownLatch(1);
        final CountDownLatch actorsReadyLatch = new CountDownLatch(2);
        submitPool.submit(() -> {
            actorSystem.createRootActor(ROOT_DISPATCHER, new SlowCreateActor.SlowCreateActorCreator(actorId, testCtx1, initLatch));
            actorsReadyLatch.countDown();
        });
        submitPool.submit(() -> {
            actorSystem.createRootActor(ROOT_DISPATCHER, new SlowCreateActor.SlowCreateActorCreator(actorId, testCtx2, initLatch));
            actorsReadyLatch.countDown();
        });

        initLatch.countDown(); //replacement for Thread.wait(500) in the SlowCreateActorCreator
        Assert.assertTrue(actorsReadyLatch.await(TIMEOUT_AWAIT_MAX_SEC, TimeUnit.SECONDS));

        actorSystem.tell(actorId, new IntTbActorMsg(42));

        Assert.assertTrue(testCtx1.getLatch().await(TIMEOUT_AWAIT_MAX_SEC, TimeUnit.SECONDS));
        Assert.assertFalse(testCtx2.getLatch().await(1, TimeUnit.SECONDS));
    }

    /**
     * 功能：验证Actor 实例相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testActorCreatorCalledOnce() throws InterruptedException {
        executor = ThingsBoardExecutors.newWorkStealingPool(parallelism, getClass());
        actorSystem.createDispatcher(ROOT_DISPATCHER, executor);
        ActorTestCtx testCtx = getActorTestCtx(1);
        TbActorId actorId = new TbEntityActorId(new DeviceId(UUID.randomUUID()));
        final int actorsCount = 1000;
        final CountDownLatch initLatch = new CountDownLatch(1);
        final CountDownLatch actorsReadyLatch = new CountDownLatch(actorsCount);
        for (int i = 0; i < actorsCount; i++) {
            submitPool.submit(() -> {
                actorSystem.createRootActor(ROOT_DISPATCHER, new SlowCreateActor.SlowCreateActorCreator(actorId, testCtx, initLatch));
                actorsReadyLatch.countDown();
            });
        }
        initLatch.countDown();
        Assert.assertTrue(actorsReadyLatch.await(TIMEOUT_AWAIT_MAX_SEC, TimeUnit.SECONDS));

        actorSystem.tell(actorId, new IntTbActorMsg(42));

        Assert.assertTrue(testCtx.getLatch().await(TIMEOUT_AWAIT_MAX_SEC, TimeUnit.SECONDS));
        //One for creation and one for message
        Assert.assertEquals(2, testCtx.getInvocationCount().get());
    }

    /**
     * 功能：验证`Failed Init`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void testFailedInit() throws InterruptedException {
        executor = ThingsBoardExecutors.newWorkStealingPool(parallelism, getClass());
        actorSystem.createDispatcher(ROOT_DISPATCHER, executor);
        ActorTestCtx testCtx1 = getActorTestCtx(1);
        ActorTestCtx testCtx2 = getActorTestCtx(1);

        TbActorRef actorId1 = actorSystem.createRootActor(ROOT_DISPATCHER, new FailedToInitActor.FailedToInitActorCreator(
                new TbEntityActorId(new DeviceId(UUID.randomUUID())), testCtx1, 1, 3000));
        TbActorRef actorId2 = actorSystem.createRootActor(ROOT_DISPATCHER, new FailedToInitActor.FailedToInitActorCreator(
                new TbEntityActorId(new DeviceId(UUID.randomUUID())), testCtx2, 2, 1));

        actorId1.tell(new IntTbActorMsg(42));
        actorId2.tell(new IntTbActorMsg(42));

        Assert.assertFalse(testCtx1.getLatch().await(2, TimeUnit.SECONDS));
        Assert.assertTrue(testCtx2.getLatch().await(1, TimeUnit.SECONDS));
        Assert.assertTrue(testCtx1.getLatch().await(3, TimeUnit.SECONDS));
    }


    /**
     * 功能：验证`Actors And Messages`相关场景。
     * 参数：
     * - `actorsCount`：`actorsCount` 参数。
     * - `msgNumber`：待处理消息。
     * - `times`：`times` 参数。
     * 返回：无。
     */
    public void testActorsAndMessages(int actorsCount, int msgNumber, int times) throws InterruptedException {
        Random random = new Random();
        int[] randomIntegers = new int[msgNumber];
        long sumTmp = 0;
        for (int i = 0; i < msgNumber; i++) {
            int tmp = random.nextInt();
            randomIntegers[i] = tmp;
            sumTmp += tmp;
        }
        long expected = sumTmp;

        List<ActorTestCtx> testCtxes = new ArrayList<>();

        List<TbActorRef> actorRefs = new ArrayList<>();
        for (int actorIdx = 0; actorIdx < actorsCount; actorIdx++) {
            ActorTestCtx testCtx = getActorTestCtx(msgNumber);
            actorRefs.add(actorSystem.createRootActor(ROOT_DISPATCHER, new TestRootActor.TestRootActorCreator(
                    new TbEntityActorId(new DeviceId(UUID.randomUUID())), testCtx)));
            testCtxes.add(testCtx);
        }

        for (int t = 0; t < times; t++) {
            long start = System.nanoTime();
            for (int i = 0; i < msgNumber; i++) {
                int tmp = randomIntegers[i];
                submitPool.execute(() -> actorRefs.forEach(actorId -> actorId.tell(new IntTbActorMsg(tmp))));
            }
            log.info("Submitted all messages");
            testCtxes.forEach(ctx -> {
                try {
                    boolean success = ctx.getLatch().await(1, TimeUnit.MINUTES);
                    if (!success) {
                        log.warn("Failed: {}, {}", ctx.getActual().get(), ctx.getInvocationCount().get());
                    }
                    Assert.assertTrue(success);
                    Assert.assertEquals(expected, ctx.getActual().get());
                    Assert.assertEquals(msgNumber, ctx.getInvocationCount().get());
                    ctx.clear();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
            long duration = System.nanoTime() - start;
            log.info("Time spend: {}ns ({} ms)", duration, TimeUnit.NANOSECONDS.toMillis(duration));
        }
    }

    /**
     * 功能：获取上下文。
     * 参数：
     * - `i`：`i` 参数。
     * 返回：处理结果。
     */
    private ActorTestCtx getActorTestCtx(int i) {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        AtomicLong actual = new AtomicLong();
        AtomicInteger invocations = new AtomicInteger();
        return new ActorTestCtx(countDownLatch, invocations, i, actual);
    }
}
