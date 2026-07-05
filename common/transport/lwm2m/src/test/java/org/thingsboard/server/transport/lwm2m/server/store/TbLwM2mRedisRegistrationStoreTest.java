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
package org.thingsboard.server.transport.lwm2m.server.store;

import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.util.NamedThreadFactory;
import org.eclipse.leshan.server.redis.serialization.RegistrationSerDes;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationUpdate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.Lock;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.thingsboard.server.transport.lwm2m.server.store.TbLwM2mRedisRegistrationStore.DEFAULT_CLEAN_LIMIT;
import static org.thingsboard.server.transport.lwm2m.server.store.TbLwM2mRedisRegistrationStore.DEFAULT_CLEAN_PERIOD;
import static org.thingsboard.server.transport.lwm2m.server.store.TbLwM2mRedisRegistrationStore.DEFAULT_GRACE_PERIOD;


/**
 * 中文说明：
 * 1. 类目的：`TbLwM2mRedisRegistrationStoreTest` 是ThingsBoard Common 测试模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、序列化框架、协议处理器、队列消费者或测试框架按需创建和使用。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
@ExtendWith(MockitoExtension.class)
class TbLwM2mRedisRegistrationStoreTest {

    /**
     * 工厂，用于按场景创建或提供目标对象。
     */
    RedisConnectionFactory connectionFactory;
    RedisConnection connection;
    /**
     * 锁，用于保护并发读写的共享状态。
     */
    RedisLockRegistry lockRegistry;

    /**
     * 存储组件，表示当前对象的对应属性。
     */
    TbLwM2mRedisRegistrationStore registrationStore;

    /**
     * 功能：初始化当前测试或组件需要的对象。
     * 参数：无。
     * 返回：无。
     */
    @BeforeEach
    void setUp() {
        lockRegistry = mock(RedisLockRegistry.class);
        lenient().when(lockRegistry.obtain(any())).thenReturn(mock(Lock.class));
        connection = mock(RedisConnection.class);
        //when(connection.set(any(byte[].class), any(byte[].class))).
        connectionFactory = mock(RedisConnectionFactory.class);
        lenient().when(connectionFactory.getConnection()).thenReturn(connection);
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1,
                new NamedThreadFactory(String.format("RedisRegistrationStore Cleaner (%ds)", DEFAULT_CLEAN_PERIOD)));
        registrationStore = new TbLwM2mRedisRegistrationStore(connectionFactory, executorService,
                DEFAULT_CLEAN_PERIOD, DEFAULT_GRACE_PERIOD, DEFAULT_CLEAN_LIMIT, lockRegistry);
    }

    /**
     * 功能：验证`Add Registration With No Old Registration`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void testAddRegistrationWithNoOldRegistration() {
        setOldRegistration(null);
        Registration registration = buildRegistration();

        assertThat(registrationStore.addRegistration(registration)).isNull();

        byte[] endpoint = registration.getEndpoint().getBytes(UTF_8);
        verify(connection, times(1)).set(getRegIdKey(registration), endpoint);
        verify(connection, times(1)).set(getRegAddrKey(registration), endpoint);
        verify(connection, times(1)).set(getRegIdentityKey(registration), endpoint);
        verify(connection, times(3)).set(any(byte[].class), any(byte[].class));
        verify(connection, times(0)).del(any(byte[].class));
    }

    /**
     * 功能：验证`Add Registration With Old Registration Equal To Current`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void testAddRegistrationWithOldRegistrationEqualToCurrent(){
        var oldRegistration = buildRegistration();
        setOldRegistration(oldRegistration);
        Registration registration = buildRegistration();

        var deregistration = registrationStore.addRegistration(registration);

        assertThat(deregistration.getRegistration()).isEqualTo(oldRegistration);

        byte[] endpoint = registration.getEndpoint().getBytes(UTF_8);
        verify(connection, times(1)).set(getRegIdKey(registration), endpoint);
        verify(connection, times(1)).set(getRegAddrKey(registration), endpoint);
        verify(connection, times(1)).set(getRegIdentityKey(registration), endpoint);
        verify(connection, times(3)).set(any(byte[].class), any(byte[].class));
        verify(connection, times(1)).del(getTknsRegIdKey(oldRegistration));
        verify(connection, times(1)).del(any(byte[].class));
    }

    /**
     * 功能：验证`Add Registration Removes Indexes`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void testAddRegistrationRemovesIndexes(){
        var oldRegistration = buildRegistration(Identity.unsecure(getTestAddress(1234)));
        setOldRegistration(oldRegistration);
        var registration = buildRegistration(Identity.unsecure(getTestAddress(2345)));

        var deregistration = registrationStore.addRegistration(registration);

        assertThat(deregistration.getRegistration()).isEqualTo(oldRegistration);
        byte[] endpoint = registration.getEndpoint().getBytes(UTF_8);
        verify(connection, times(1)).set(getRegIdKey(registration), endpoint);
        verify(connection, times(1)).set(getRegAddrKey(registration), endpoint);
        verify(connection, times(1)).set(getRegIdentityKey(registration), endpoint);
        verify(connection, times(3)).set(any(byte[].class), any(byte[].class));
        verify(connection, times(1)).del(getRegAddrKey(oldRegistration));
        verify(connection, times(1)).del(getRegIdentityKey(oldRegistration));
        verify(connection, times(1)).del(getTknsRegIdKey(oldRegistration));
        verify(connection, times(3)).del(any(byte[].class));
    }

    /**
     * 功能：验证`Update Registration When No Registration Found`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void testUpdateRegistrationWhenNoRegistrationFound() {
        setOldRegistration(null);
        Registration registration = buildRegistration();
        RegistrationUpdate update = createUpdateFromRegistration(registration);

        assertThat(registrationStore.updateRegistration(update)).isNull();

        verify(connection, times(1)).get(getRegIdKey(registration));
        verify(connection, times(1)).get(any(byte[].class));
        verify(connection, times(0)).del(any(byte[].class));
    }

    /**
     * 功能：验证`Update Registration With Same Registration`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void testUpdateRegistrationWithSameRegistration() {
        Registration registration = buildRegistration();
        setOldRegistration(registration);
        RegistrationUpdate update = createUpdateFromRegistration(registration);

        assertThat(registrationStore.updateRegistration(update)).isNotNull();

        var endpoint = registration.getEndpoint().getBytes(UTF_8);
        // check registration and addressIndex here updated
        verify(connection, times(1)).set(eq(getEndpointKey(endpoint)), any(byte[].class));
        verify(connection, times(1)).set(getRegAddrKey(registration), endpoint);
        verify(connection, times(2)).set(any(byte[].class), any(byte[].class));
        verify(connection, times(0)).del(any(byte[].class));
    }

    /**
     * 功能：验证`Update Registration With Registration From Secure Identities With Different Address`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void testUpdateRegistrationWithRegistrationFromSecureIdentitiesWithDifferentAddress() {
        Registration oldRegistration = buildRegistration(Identity.psk(getTestAddress(1234), "my:psk"));
        setOldRegistration(oldRegistration);
        Registration newRegistration = buildRegistration(Identity.psk(getTestAddress(2345), "my:psk"));
        RegistrationUpdate update = createUpdateFromRegistration(newRegistration);
        assertThat(oldRegistration.getEndpoint()).isEqualTo(newRegistration.getEndpoint());

        assertThat(registrationStore.updateRegistration(update)).isNotNull();

        var endpoint = newRegistration.getEndpoint().getBytes(UTF_8);
        // check registration and addressIndex here updated
        verify(connection, times(1)).set(eq(getEndpointKey(endpoint)), any(byte[].class));
        verify(connection, times(1)).set(getRegAddrKey(newRegistration), endpoint);
        // check old AddrIndex has been removed
        verify(connection, times(1)).del(getRegAddrKey(oldRegistration));
        // check identityIndex has not been removed
        verify(connection, times(0)).del(getRegIdentityKey(oldRegistration));
        // check only one key (AddrIndex) in total was removed
        verify(connection, times(1)).del(any(byte[].class));
    }

    /**
     * 功能：验证`Get Registration By Identity Returns Registration For Secure Identity With Different Address`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void testGetRegistrationByIdentityReturnsRegistrationForSecureIdentityWithDifferentAddress() {
        Registration registration = buildRegistration(Identity.psk(getTestAddress(1234), "my:psk"));
        setOldRegistration(registration);
        Identity sameIdentityWithDifferentAddress = Identity.psk(getTestAddress(2345), "my:psk");

        Registration retrievedRegistration = registrationStore.getRegistrationByIdentity(sameIdentityWithDifferentAddress);

        assertThat(retrievedRegistration).isEqualTo(registration);
    }

    /**
     * 功能：更新`Old Registration`。
     * 参数：
     * - `oldRegistration`：`oldRegistration` 参数。
     * 返回：无。
     */
    private void setOldRegistration(Registration oldRegistration){
        byte[] serializedRegistration = null;
        if (oldRegistration != null){
            byte[] endpoint = oldRegistration.getEndpoint().getBytes(UTF_8);
            // set the AddrIndex
            byte[] regAddrKey = getRegAddrKey(oldRegistration);
            lenient().when(connection.get(eq(regAddrKey))).thenReturn(endpoint);
            // set the IdentityIndex
            byte[] regIdentityKey = getRegIdentityKey(oldRegistration);
            lenient().when(connection.get(eq(regIdentityKey))).thenReturn(endpoint);
            // set the IdIndex
            byte[] regIdKey = getRegIdKey(oldRegistration);
            lenient().when(connection.get(eq(regIdKey))).thenReturn(endpoint);
            // set the registration
            serializedRegistration = RegistrationSerDes.bSerialize(oldRegistration);
            lenient().when(connection.get(eq(getEndpointKey(endpoint)))).thenReturn(serializedRegistration);
        }
        lenient().when(connection.getSet(any(byte[].class), any(byte[].class))).thenReturn(serializedRegistration);
    }

    /**
     * 功能：获取键。
     * 参数：
     * - `registration`：`registration` 参数。
     * 返回：处理结果。
     */
    private byte[] getRegAddrKey(Registration registration){
        return ReflectionTestUtils.invokeMethod(registrationStore, "toRegAddrKey", registration.getSocketAddress());
    }

    /**
     * 功能：获取键。
     * 参数：
     * - `registration`：`registration` 参数。
     * 返回：处理结果。
     */
    private byte[] getRegIdentityKey(Registration registration){
        return ReflectionTestUtils.invokeMethod(registrationStore, "toRegIdentityKey", registration.getIdentity());
    }

    /**
     * 功能：获取键。
     * 参数：
     * - `registration`：`registration` 参数。
     * 返回：处理结果。
     */
    private byte[] getRegIdKey(Registration registration){
        return ReflectionTestUtils.invokeMethod(registrationStore, "toRegIdKey", registration.getId());
    }

    /**
     * 功能：获取键。
     * 参数：
     * - `endpoint`：`endpoint` 参数。
     * 返回：处理结果。
     */
    private byte[] getEndpointKey(byte[] endpoint){
        return ReflectionTestUtils.invokeMethod(registrationStore, "toEndpointKey", endpoint);
    }

    /**
     * 功能：获取键。
     * 参数：
     * - `registration`：`registration` 参数。
     * 返回：处理结果。
     */
    private byte[] getTknsRegIdKey(Registration registration){
        return ReflectionTestUtils.invokeMethod(registrationStore, "toKey", "TKNS:REGID:", registration.getId());
    }

    /**
     * 功能：构建`Registration`。
     * 参数：无。
     * 返回：处理结果。
     */
    private static Registration buildRegistration() {
        return buildRegistration(Identity.psk(getTestAddress(), "my:psk"));
    }

    /**
     * 功能：构建`Registration`。
     * 参数：
     * - `identity`：实体对象。
     * 返回：处理结果。
     */
    private static Registration buildRegistration(Identity identity){
        return new Registration.Builder("my_reg_id", "abcde", identity)
                .objectLinks(new Link[]{})
                .build();
    }

    /**
     * 功能：保存或创建`Update From Registration`。
     * 参数：
     * - `registration`：`registration` 参数。
     * 返回：处理结果。
     */
    private static RegistrationUpdate createUpdateFromRegistration(Registration registration){
        return new RegistrationUpdate(
                registration.getId(),
                registration.getIdentity(),
                registration.getLifeTimeInSec(),
                registration.getSmsNumber(),
                registration.getBindingMode(),
                registration.getObjectLinks(),
                registration.getAdditionalRegistrationAttributes()
        );
    }

    /**
     * 功能：获取`Test Address`。
     * 参数：无。
     * 返回：处理结果。
     */
    private static InetSocketAddress getTestAddress() {
        return getTestAddress(5684);
    }

    /**
     * 功能：获取`Test Address`。
     * 参数：
     * - `port`：`port` 参数。
     * 返回：处理结果。
     */
    private static InetSocketAddress getTestAddress(int port) {
        try {
            return new InetSocketAddress(InetAddress.getByName("1.2.3.4"), port);
        } catch (UnknownHostException e) {
            throw new AssertionError("Cannot create test address");
        }
    }

/*
 * 本类总结：
 * 1. 核心职责：`TbLwM2mRedisRegistrationStoreTest` 在 ThingsBoard Common 测试模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
}