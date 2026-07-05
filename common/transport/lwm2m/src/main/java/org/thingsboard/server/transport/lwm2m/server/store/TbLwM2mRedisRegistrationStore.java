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

import org.eclipse.californium.core.coap.Token;
import org.eclipse.californium.core.observe.ObservationStoreException;
import org.eclipse.californium.elements.EndpointContext;
import org.eclipse.leshan.core.Destroyable;
import org.eclipse.leshan.core.Startable;
import org.eclipse.leshan.core.Stoppable;
import org.eclipse.leshan.core.californium.ObserveUtil;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.util.NamedThreadFactory;
import org.eclipse.leshan.core.util.Validate;
import org.eclipse.leshan.server.californium.registration.CaliforniumRegistrationStore;
import org.eclipse.leshan.server.redis.RedisRegistrationStore;
import org.eclipse.leshan.server.redis.serialization.ObservationSerDes;
import org.eclipse.leshan.server.redis.serialization.RegistrationSerDes;
import org.eclipse.leshan.server.registration.Deregistration;
import org.eclipse.leshan.server.registration.ExpirationListener;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.registration.RegistrationUpdate;
import org.eclipse.leshan.server.registration.UpdatedRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisClusterConnection;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.thingsboard.server.transport.lwm2m.server.store.util.LwM2MIdentitySerDes;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * 中文说明：
 * 1. 类目的：`TbLwM2mRedisRegistrationStore` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
public class TbLwM2mRedisRegistrationStore implements CaliforniumRegistrationStore, Startable, Stoppable, Destroyable {
    /** Default time in seconds between 2 cleaning tasks (used to remove expired registration). */
    /**
     * `DEFAULT_CLEAN_PERIOD`常量，用于统一引用固定值。
     */
    public static final long DEFAULT_CLEAN_PERIOD = 60;
    public static final int DEFAULT_CLEAN_LIMIT = 500;
    /** Defaut Extra time for registration lifetime in seconds */
    /**
     * `DEFAULT_GRACE_PERIOD`常量，用于统一引用固定值。
     */
    public static final long DEFAULT_GRACE_PERIOD = 0;

    private static final Logger LOG = LoggerFactory.getLogger(RedisRegistrationStore.class);

    // Redis key prefixes
    /**
     * `REG_EP`常量，用于统一引用固定值。
     */
    private static final String REG_EP = "REG:EP:"; // (Endpoint => Registration)
    private static final String REG_EP_REGID_IDX = "EP:REGID:"; // secondary index key (Registration ID => Endpoint)
    /**
     * `REG_EP_ADDR_IDX`常量，用于统一引用固定值。
     */
    private static final String REG_EP_ADDR_IDX = "EP:ADDR:"; // secondary index key (Socket Address => Endpoint)
    private static final String REG_EP_IDENTITY = "EP:IDENTITY:"; // secondary index key (Identity => Endpoint)
    /**
     * 锁常量，用于统一引用固定值。
     */
    private static final String LOCK_EP = "LOCK:EP:";
    private static final byte[] OBS_TKN = "OBS:TKN:".getBytes(UTF_8);
    /**
     * `OBS_TKNS_REGID_IDX`常量，用于统一引用固定值。
     */
    private static final String OBS_TKNS_REGID_IDX = "TKNS:REGID:"; // secondary index (token list by registration)
    private static final byte[] EXP_EP = "EXP:EP".getBytes(UTF_8); // a sorted set used for registration expiration
    // (expiration date, Endpoint)

    /**
     * 工厂，用于按场景创建或提供目标对象。
     */
    private final RedisConnectionFactory connectionFactory;

    // Listener use to notify when a registration expires
    /**
     * 监听器列表，用于保存一组待处理对象。
     */
    private ExpirationListener expirationListener;

    /**
     * 执行器，负责处理对应任务或消息。
     */
    private final ScheduledExecutorService schedExecutor;
    private ScheduledFuture<?> cleanerTask;
    /**
     * 当前处理是否已经启动。
     */
    private boolean started = false;

    /**
     * `cleanPeriod` 字段，保存当前对象的对应属性。
     */
    private final long cleanPeriod; // in seconds
    private final int cleanLimit; // maximum number to clean in a clean period
    /**
     * `gracePeriod` 字段，保存当前对象的对应属性。
     */
    private final long gracePeriod; // in seconds

    /**
     * 锁，用于保护并发读写的共享状态。
     */
    private final RedisLockRegistry redisLock;

    /**
     * 功能：创建 `TbLwM2mRedisRegistrationStore` 实例，并初始化必要字段。
     * 参数：
     * - `connectionFactory`：`connectionFactory` 参数。
     * 返回：新创建的对象实例。
     */
    public TbLwM2mRedisRegistrationStore(RedisConnectionFactory connectionFactory) {
        this(connectionFactory, DEFAULT_CLEAN_PERIOD, DEFAULT_GRACE_PERIOD, DEFAULT_CLEAN_LIMIT); // default clean period 60s
    }

    /**
     * 功能：创建 `TbLwM2mRedisRegistrationStore` 实例，并初始化必要字段。
     * 参数：
     * - `connectionFactory`：`connectionFactory` 参数。
     * - `cleanPeriodInSec`：`cleanPeriodInSec` 参数。
     * - `lifetimeGracePeriodInSec`：`lifetimeGracePeriodInSec` 参数。
     * - `cleanLimit`：数量限制。
     * 返回：新创建的对象实例。
     */
    public TbLwM2mRedisRegistrationStore(RedisConnectionFactory connectionFactory, long cleanPeriodInSec, long lifetimeGracePeriodInSec, int cleanLimit) {
        this(connectionFactory, Executors.newScheduledThreadPool(1,
                new NamedThreadFactory(String.format("RedisRegistrationStore Cleaner (%ds)", cleanPeriodInSec))),
                cleanPeriodInSec, lifetimeGracePeriodInSec, cleanLimit);
    }

    /**
     * 功能：创建 `TbLwM2mRedisRegistrationStore` 实例，并初始化必要字段。
     * 参数：
     * - `connectionFactory`：`connectionFactory` 参数。
     * - `schedExecutor`：`schedExecutor` 参数。
     * - `cleanPeriodInSec`：`cleanPeriodInSec` 参数。
     * - `lifetimeGracePeriodInSec`：`lifetimeGracePeriodInSec` 参数。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public TbLwM2mRedisRegistrationStore(RedisConnectionFactory connectionFactory, ScheduledExecutorService schedExecutor, long cleanPeriodInSec,
                                         long lifetimeGracePeriodInSec, int cleanLimit) {
        this(connectionFactory, schedExecutor, cleanPeriodInSec, lifetimeGracePeriodInSec, cleanLimit,
                new RedisLockRegistry(connectionFactory, "Registration"));
    }

    /**
     * 功能：创建 `TbLwM2mRedisRegistrationStore` 实例，并初始化必要字段。
     * 参数：
     * - `connectionFactory`：`connectionFactory` 参数。
     * - `schedExecutor`：`schedExecutor` 参数。
     * - `cleanPeriodInSec`：`cleanPeriodInSec` 参数。
     * - `lifetimeGracePeriodInSec`：`lifetimeGracePeriodInSec` 参数。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public TbLwM2mRedisRegistrationStore(RedisConnectionFactory connectionFactory, ScheduledExecutorService schedExecutor, long cleanPeriodInSec,
                                         long lifetimeGracePeriodInSec, int cleanLimit, RedisLockRegistry lockRegistry) {
        this.connectionFactory = connectionFactory;
        this.schedExecutor = schedExecutor;
        this.cleanPeriod = cleanPeriodInSec;
        this.cleanLimit = cleanLimit;
        this.gracePeriod = lifetimeGracePeriodInSec;
        this.redisLock = lockRegistry;
    }

    /* *************** Redis Key utility function **************** */

    /**
     * 功能：执行 `toKey` 对应的处理。
     * 参数：
     * - `prefix`：`prefix` 参数。
     * - `key`：键。
     * 返回：处理结果。
     */
    private byte[] toKey(byte[] prefix, byte[] key) {
        byte[] result = new byte[prefix.length + key.length];
        System.arraycopy(prefix, 0, result, 0, prefix.length);
        System.arraycopy(key, 0, result, prefix.length, key.length);
        return result;
    }

    /**
     * 功能：执行 `toKey` 对应的处理。
     * 参数：
     * - `prefix`：`prefix` 参数。
     * - `registrationID`：`registrationID`ID。
     * 返回：处理结果。
     */
    private byte[] toKey(String prefix, String registrationID) {
        return (prefix + registrationID).getBytes();
    }

    /**
     * 功能：执行 `toLockKey` 对应的处理。
     * 参数：
     * - `endpoint`：`endpoint` 参数。
     * 返回：文本结果。
     */
    private String toLockKey(String endpoint) {
        return new String(toKey(LOCK_EP, endpoint));
    }

    /**
     * 功能：执行 `toLockKey` 对应的处理。
     * 参数：
     * - `endpoint`：`endpoint` 参数。
     * 返回：文本结果。
     */
    private String toLockKey(byte[] endpoint) {
        return new String(toKey(LOCK_EP.getBytes(UTF_8), endpoint));
    }

    /* *************** Leshan Registration API **************** */

    /**
     * 功能：保存或创建`Registration`。
     * 参数：
     * - `registration`：`registration` 参数。
     * 返回：处理结果。
     */
    @Override
    public Deregistration addRegistration(Registration registration) {
        Lock lock = null;
        try (var connection = connectionFactory.getConnection()) {
            String lockKey = toLockKey(registration.getEndpoint());

            try {
                lock = redisLock.obtain(lockKey);
                lock.lock();
                // add registration
                byte[] k = toEndpointKey(registration.getEndpoint());
                byte[] old = connection.getSet(k, serializeReg(registration));

                // add registration: secondary indexes
                byte[] regid_idx = toRegIdKey(registration.getId());
                connection.set(regid_idx, registration.getEndpoint().getBytes(UTF_8));
                byte[] addr_idx = toRegAddrKey(registration.getSocketAddress());
                connection.set(addr_idx, registration.getEndpoint().getBytes(UTF_8));
                byte[] identity_idx = toRegIdentityKey(registration.getIdentity());
                connection.set(identity_idx, registration.getEndpoint().getBytes(UTF_8));

                // Add or update expiration
                addOrUpdateExpiration(connection, registration);

                if (old != null) {
                    Registration oldRegistration = deserializeReg(old);
                    // remove old secondary index
                    if (!registration.getId().equals(oldRegistration.getId()))
                        connection.del(toRegIdKey(oldRegistration.getId()));
                    if (!oldRegistration.getSocketAddress().equals(registration.getSocketAddress())) {
                        removeAddrIndex(connection, oldRegistration);
                    }
                    if (registrationsHaveDifferentIdentities(oldRegistration, registration)) {
                        removeIdentityIndex(connection, oldRegistration);
                    }
                    // remove old observation
                    Collection<Observation> obsRemoved = unsafeRemoveAllObservations(connection, oldRegistration.getId());

                    return new Deregistration(oldRegistration, obsRemoved);
                }

                return null;
            } finally {
                if (lock != null) {
                    lock.unlock();
                }
            }
        }
    }

    /**
     * 功能：更新`Registration`。
     * 参数：
     * - `update`：`update` 参数。
     * 返回：处理结果。
     */
    @Override
    public UpdatedRegistration updateRegistration(RegistrationUpdate update) {
        Lock lock = null;
        try (var connection = connectionFactory.getConnection()) {

            // Fetch the registration ep by registration ID index
            byte[] ep = connection.get(toRegIdKey(update.getRegistrationId()));
            if (ep == null) {
                return null;
            }

            String lockKey = toLockKey(ep);
            try {
                lock = redisLock.obtain(lockKey);
                lock.lock();

                // Fetch the registration
                byte[] data = connection.get(toEndpointKey(ep));
                if (data == null) {
                    return null;
                }

                Registration r = deserializeReg(data);

                Registration updatedRegistration = update.update(r);

                // Store the new registration
                connection.set(toEndpointKey(updatedRegistration.getEndpoint()), serializeReg(updatedRegistration));

                // Add or update expiration
                addOrUpdateExpiration(connection, updatedRegistration);

                /** Update secondary index :
                 * If registration is already associated to this address we don't care as we only want to keep the most
                 * recent binding. */
                byte[] addr_idx = toRegAddrKey(updatedRegistration.getSocketAddress());
                connection.set(addr_idx, updatedRegistration.getEndpoint().getBytes(UTF_8));
                if (!r.getSocketAddress().equals(updatedRegistration.getSocketAddress())) {
                    removeAddrIndex(connection, r);
                }
                if (registrationsHaveDifferentIdentities(r, updatedRegistration)) {
                    removeIdentityIndex(connection, r);
                }

                return new UpdatedRegistration(r, updatedRegistration);

            } finally {
                if (lock != null) {
                    lock.unlock();
                }
            }
        }
    }

    /**
     * 功能：获取`Registration`。
     * 参数：
     * - `registrationId`：`registrationId`ID。
     * 返回：处理结果。
     */
    @Override
    public Registration getRegistration(String registrationId) {
        try (var connection = connectionFactory.getConnection()) {
            return getRegistration(connection, registrationId);
        }
    }

    /**
     * 功能：获取`Registration By Endpoint`。
     * 参数：
     * - `endpoint`：`endpoint` 参数。
     * 返回：处理结果。
     */
    @Override
    public Registration getRegistrationByEndpoint(String endpoint) {
        Validate.notNull(endpoint);
        try (var connection = connectionFactory.getConnection()) {
            byte[] data = connection.get(toEndpointKey(endpoint));
            if (data == null) {
                return null;
            }
            return deserializeReg(data);
        }
    }

    /**
     * 功能：获取`Registration By Adress`。
     * 参数：
     * - `address`：`address` 参数。
     * 返回：处理结果。
     */
    @Override
    public Registration getRegistrationByAdress(InetSocketAddress address) {
        Validate.notNull(address);
        try (var connection = connectionFactory.getConnection()) {
            byte[] ep = connection.get(toRegAddrKey(address));
            if (ep == null) {
                return null;
            }
            byte[] data = connection.get(toEndpointKey(ep));
            if (data == null) {
                return null;
            }
            return deserializeReg(data);
        }
    }

    /**
     * 功能：获取`Registration By Identity`。
     * 参数：
     * - `identity`：实体对象。
     * 返回：处理结果。
     */
    @Override
    public Registration getRegistrationByIdentity(Identity identity) {
        Validate.notNull(identity);
        try (var connection = connectionFactory.getConnection()) {
            byte[] ep = connection.get(toRegIdentityKey(identity));
            if (ep == null) {
                return null;
            }
            byte[] data = connection.get(toEndpointKey(ep));
            if (data == null) {
                return null;
            }
            return deserializeReg(data);
        }
    }

    /**
     * 功能：获取`All Registrations`。
     * 参数：无。
     * 返回：处理结果。
     */
    @Override
    public Iterator<Registration> getAllRegistrations() {
        try (var connection = connectionFactory.getConnection()) {
            Collection<Registration> list = new LinkedList<>();
            ScanOptions scanOptions = ScanOptions.scanOptions().count(100).match(REG_EP + "*").build();
            List<Cursor<byte[]>> scans = new ArrayList<>();
            if (connection instanceof RedisClusterConnection) {
                ((RedisClusterConnection) connection).clusterGetNodes().forEach(node -> {
                    scans.add(((RedisClusterConnection) connection).scan(node, scanOptions));
                });
            } else {
                scans.add(connection.scan(scanOptions));
            }

            scans.forEach(scan -> {
                scan.forEachRemaining(key -> {
                    byte[] element = connection.get(key);
                    list.add(deserializeReg(element));
                });
            });
            return list.iterator();
        }
    }

    /**
     * 功能：删除或清理`Registration`。
     * 参数：
     * - `registrationId`：`registrationId`ID。
     * 返回：处理结果。
     */
    @Override
    public Deregistration removeRegistration(String registrationId) {
        try (var connection = connectionFactory.getConnection()) {
            return removeRegistration(connection, registrationId, false);
        }
    }

    /**
     * 功能：删除或清理`Registration`。
     * 参数：
     * - `connection`：`connection` 参数。
     * - `registrationId`：`registrationId`ID。
     * - `removeOnlyIfNotAlive`：`removeOnlyIfNotAlive` 参数。
     * 返回：处理结果。
     */
    private Deregistration removeRegistration(RedisConnection connection, String registrationId, boolean removeOnlyIfNotAlive) {
        // fetch the client ep by registration ID index
        byte[] ep = connection.get(toRegIdKey(registrationId));
        if (ep == null) {
            return null;
        }

        Lock lock = null;
        String lockKey = toLockKey(ep);
        try {
            lock = redisLock.obtain(lockKey);
            lock.lock();

            // fetch the client
            byte[] data = connection.get(toEndpointKey(ep));
            if (data == null) {
                return null;
            }
            Registration r = deserializeReg(data);

            if (!removeOnlyIfNotAlive || !r.isAlive(gracePeriod)) {
                long nbRemoved = connection.del(toRegIdKey(r.getId()));
                if (nbRemoved > 0) {
                    connection.del(toEndpointKey(r.getEndpoint()));
                    Collection<Observation> obsRemoved = unsafeRemoveAllObservations(connection, r.getId());
                    removeAddrIndex(connection, r);
                    removeIdentityIndex(connection, r);
                    removeExpiration(connection, r);
                    return new Deregistration(r, obsRemoved);
                }
            }
            return null;
        } finally {
            if (lock != null) {
                lock.unlock();
            }
        }
    }

    /**
     * 功能：删除或清理索引。
     * 参数：
     * - `connection`：`connection` 参数。
     * - `r`：`r` 参数。
     * 返回：无。
     */
    private void removeAddrIndex(RedisConnection connection, Registration r) {
        removeSecondaryIndex(connection, toRegAddrKey(r.getSocketAddress()), r.getEndpoint());
    }

    /**
     * 功能：删除或清理索引。
     * 参数：
     * - `connection`：`connection` 参数。
     * - `r`：`r` 参数。
     * 返回：无。
     */
    private void removeIdentityIndex(RedisConnection connection, Registration r) {
        removeSecondaryIndex(connection, toRegIdentityKey(r.getIdentity()), r.getEndpoint());
    }

    //TODO: JedisCluster didn't implement Transaction, maybe should use some advanced key creation strategies
    /**
     * 功能：删除或清理索引。
     * 参数：
     * - `connection`：`connection` 参数。
     * - `indexKey`：键。
     * - `endpointName`：名称。
     * 返回：无。
     */
    private void removeSecondaryIndex(RedisConnection connection, byte[] indexKey, String endpointName) {
        // Watch the key to remove.
//        connection.watch(indexKey);

        byte[] epFromAddr = connection.get(indexKey);
        // Delete the key if needed.
        if (Arrays.equals(epFromAddr, endpointName.getBytes(UTF_8))) {
            // Try to delete the key
//            connection.multi();
            connection.del(indexKey);
//            connection.exec();
            // if transaction failed this is not an issue as the index is probably reused and we don't need to
            // delete it anymore.
        } else {
            // the key must not be deleted.
//            connection.unwatch();
        }
    }

    /**
     * 功能：保存或创建`Or Update Expiration`。
     * 参数：
     * - `connection`：`connection` 参数。
     * - `registration`：`registration` 参数。
     * 返回：无。
     */
    private void addOrUpdateExpiration(RedisConnection connection, Registration registration) {
        connection.zAdd(EXP_EP, registration.getExpirationTimeStamp(gracePeriod), registration.getEndpoint().getBytes(UTF_8));
    }

    /**
     * 功能：删除或清理`Expiration`。
     * 参数：
     * - `connection`：`connection` 参数。
     * - `registration`：`registration` 参数。
     * 返回：无。
     */
    private void removeExpiration(RedisConnection connection, Registration registration) {
        connection.zRem(EXP_EP, registration.getEndpoint().getBytes(UTF_8));
    }

    /**
     * 功能：执行 `registrationsHaveDifferentIdentities` 对应的处理。
     * 参数：
     * - `first`：`first` 参数。
     * - `second`：`second` 参数。
     * 返回：判断结果。
     */
    private boolean registrationsHaveDifferentIdentities(Registration first, Registration second){
        var first_identity_string = LwM2MIdentitySerDes.serialize(first.getIdentity()).toString();
        var second_identity_string = LwM2MIdentitySerDes.serialize(second.getIdentity()).toString();
        return !first_identity_string.equals(second_identity_string);
    }

    /**
     * 功能：执行 `toRegIdKey` 对应的处理。
     * 参数：
     * - `registrationId`：`registrationId`ID。
     * 返回：处理结果。
     */
    private byte[] toRegIdKey(String registrationId) {
        return toKey(REG_EP_REGID_IDX, registrationId);
    }

    /**
     * 功能：执行 `toRegAddrKey` 对应的处理。
     * 参数：
     * - `addr`：`addr` 参数。
     * 返回：处理结果。
     */
    private byte[] toRegAddrKey(InetSocketAddress addr) {
        return toKey(REG_EP_ADDR_IDX, addr.getAddress().toString() + ":" + addr.getPort());
    }

    /**
     * 功能：执行 `toRegIdentityKey` 对应的处理。
     * 参数：
     * - `identity`：实体对象。
     * 返回：处理结果。
     */
    private byte[] toRegIdentityKey(Identity identity) {
        return toKey(REG_EP_IDENTITY, LwM2MIdentitySerDes.serialize(identity).toString());
    }

    /**
     * 功能：执行 `toEndpointKey` 对应的处理。
     * 参数：
     * - `endpoint`：`endpoint` 参数。
     * 返回：处理结果。
     */
    private byte[] toEndpointKey(String endpoint) {
        return toKey(REG_EP, endpoint);
    }

    /**
     * 功能：执行 `toEndpointKey` 对应的处理。
     * 参数：
     * - `endpoint`：`endpoint` 参数。
     * 返回：处理结果。
     */
    private byte[] toEndpointKey(byte[] endpoint) {
        return toKey(REG_EP.getBytes(UTF_8), endpoint);
    }

    /**
     * 功能：执行 `serializeReg` 对应的处理。
     * 参数：
     * - `registration`：`registration` 参数。
     * 返回：处理结果。
     */
    private byte[] serializeReg(Registration registration) {
        return RegistrationSerDes.bSerialize(registration);
    }

    /**
     * 功能：执行 `deserializeReg` 对应的处理。
     * 参数：
     * - `data`：待处理数据。
     * 返回：处理结果。
     */
    private Registration deserializeReg(byte[] data) {
        return RegistrationSerDes.deserialize(data);
    }

    /* *************** Leshan Observation API **************** */

    /*
     * The observation is not persisted here, it is done by the Californium layer (in the implementation of the
     * org.eclipse.californium.core.observe.ObservationStore#add method)
     */
    /**
     * 功能：保存或创建`Observation`。
     * 参数：
     * - `registrationId`：`registrationId`ID。
     * - `observation`：`observation` 参数。
     * 返回：匹配的数据集合。
     */
    @Override
    public Collection<Observation> addObservation(String registrationId, Observation observation) {
        List<Observation> removed = new ArrayList<>();
        try (var connection = connectionFactory.getConnection()) {

            // fetch the client ep by registration ID index
            byte[] ep = connection.get(toRegIdKey(registrationId));
            if (ep == null) {
                return null;
            }

            Lock lock = null;
            String lockKey = toLockKey(ep);

            try {
                lock = redisLock.obtain(lockKey);
                lock.lock();

                // cancel existing observations for the same path and registration id.
                for (Observation obs : getObservations(connection, registrationId)) {
                    //TODO: should be able to use CompositeObservation
                    if (((SingleObservation)observation).getPath().equals(((SingleObservation)obs).getPath())
                            && !Arrays.equals(observation.getId(), obs.getId())) {
                        removed.add(obs);
                        unsafeRemoveObservation(connection, registrationId, obs.getId());
                    }
                }

            } finally {
                if (lock != null) {
                    lock.unlock();
                }
            }
        }
        return removed;
    }

    /**
     * 功能：删除或清理`Observation`。
     * 参数：
     * - `registrationId`：`registrationId`ID。
     * - `observationId`：`observationId`ID。
     * 返回：处理结果。
     */
    @Override
    public Observation removeObservation(String registrationId, byte[] observationId) {
        try (var connection = connectionFactory.getConnection()) {

            // fetch the client ep by registration ID index
            byte[] ep = connection.get(toRegIdKey(registrationId));
            if (ep == null) {
                return null;
            }

            // remove observation
            Lock lock = null;
            String lockKey = toLockKey(ep);
            try {
                lock = redisLock.obtain(lockKey);
                lock.lock();

                Observation observation = build(get(new Token(observationId)));
                if (observation != null && registrationId.equals(observation.getRegistrationId())) {
                    unsafeRemoveObservation(connection, registrationId, observationId);
                    return observation;
                }
                return null;

            } finally {
                if (lock != null) {
                    lock.unlock();
                }
            }
        }
    }

    /**
     * 功能：获取`Observation`。
     * 参数：
     * - `registrationId`：`registrationId`ID。
     * - `observationId`：`observationId`ID。
     * 返回：处理结果。
     */
    @Override
    public Observation getObservation(String registrationId, byte[] observationId) {
        return build(get(new Token(observationId)));
    }

    /**
     * 功能：获取`Observations`。
     * 参数：
     * - `registrationId`：`registrationId`ID。
     * 返回：匹配的数据集合。
     */
    @Override
    public Collection<Observation> getObservations(String registrationId) {
        try (var connection = connectionFactory.getConnection()) {
            return getObservations(connection, registrationId);
        }
    }

    /**
     * 功能：获取`Observations`。
     * 参数：
     * - `connection`：`connection` 参数。
     * - `registrationId`：`registrationId`ID。
     * 返回：匹配的数据集合。
     */
    private Collection<Observation> getObservations(RedisConnection connection, String registrationId) {
        Collection<Observation> result = new ArrayList<>();
        for (byte[] token : connection.lRange(toKey(OBS_TKNS_REGID_IDX, registrationId), 0, -1)) {
            byte[] obs = connection.get(toKey(OBS_TKN, token));
            if (obs != null) {
                result.add(build(deserializeObs(obs)));
            }
        }
        return result;
    }

    /**
     * 功能：删除或清理`Observations`。
     * 参数：
     * - `registrationId`：`registrationId`ID。
     * 返回：匹配的数据集合。
     */
    @Override
    public Collection<Observation> removeObservations(String registrationId) {
        try (var connection = connectionFactory.getConnection()) {
            // check registration exists
            Registration registration = getRegistration(connection, registrationId);
            if (registration == null)
                return Collections.emptyList();

            // get endpoint and create lock
            String endpoint = registration.getEndpoint();
            Lock lock = null;
            String lockKey = toLockKey(endpoint);
            try {
                lock = redisLock.obtain(lockKey);
                lock.lock();
                return unsafeRemoveAllObservations(connection, registrationId);
            } finally {
                if (lock != null) {
                    lock.unlock();
                }
            }
        }
    }

    /* *************** Californium ObservationStore API **************** */

    /**
     * 功能：执行 `putIfAbsent` 对应的处理。
     * 参数：
     * - `token`：`token` 参数。
     * - `obs`：`obs` 参数。
     * 返回：处理结果。
     */
    @Override
    public org.eclipse.californium.core.observe.Observation putIfAbsent(Token token,
                                                                        org.eclipse.californium.core.observe.Observation obs) throws ObservationStoreException {
        return add(obs, true);
    }

    /**
     * 功能：执行 `put` 对应的处理。
     * 参数：
     * - `token`：`token` 参数。
     * - `obs`：`obs` 参数。
     * 返回：处理结果。
     */
    @Override
    public org.eclipse.californium.core.observe.Observation put(Token token,
                                                                org.eclipse.californium.core.observe.Observation obs) throws ObservationStoreException {
        return add(obs, false);
    }

    /**
     * 功能：执行 `add` 对应的处理。
     * 参数：
     * - `obs`：`obs` 参数。
     * - `ifAbsent`：`ifAbsent` 参数。
     * 返回：处理结果。
     */
    private org.eclipse.californium.core.observe.Observation add(org.eclipse.californium.core.observe.Observation obs, boolean ifAbsent) throws ObservationStoreException {
        String endpoint = ObserveUtil.validateCoapObservation(obs);
        org.eclipse.californium.core.observe.Observation previousObservation = null;

        try (var connection = connectionFactory.getConnection()) {
            Lock lock = null;
            String lockKey = toLockKey(endpoint);
            try {
                lock = redisLock.obtain(lockKey);
                lock.lock();

                String registrationId = ObserveUtil.extractRegistrationId(obs);
                if (!connection.exists(toRegIdKey(registrationId)))
                    throw new ObservationStoreException("no registration for this Id");
                byte[] key = toKey(OBS_TKN, obs.getRequest().getToken().getBytes());
                byte[] serializeObs = serializeObs(obs);
                byte[] previousValue;
                if (ifAbsent) {
                    previousValue = connection.get(key);
                    if (previousValue == null || previousValue.length == 0) {
                        connection.set(key, serializeObs);
                    } else {
                        return deserializeObs(previousValue);
                    }
                } else {
                    previousValue = connection.getSet(key, serializeObs);
                }

                // secondary index to get the list by registrationId
                connection.lPush(toKey(OBS_TKNS_REGID_IDX, registrationId), obs.getRequest().getToken().getBytes());

                // log any collisions
                if (previousValue != null && previousValue.length != 0) {
                    previousObservation = deserializeObs(previousValue);
                    LOG.warn(
                            "Token collision ? observation from request [{}] will be replaced by observation from request [{}] ",
                            previousObservation.getRequest(), obs.getRequest());
                }
            } finally {
                if (lock != null) {
                    lock.unlock();
                }
            }
        }
        return previousObservation;
    }

    /**
     * 功能：执行 `remove` 对应的处理。
     * 参数：
     * - `token`：`token` 参数。
     * 返回：无。
     */
    @Override
    public void remove(Token token) {
        try (var connection = connectionFactory.getConnection()) {
            byte[] tokenKey = toKey(OBS_TKN, token.getBytes());

            // fetch the observation by token
            byte[] serializedObs = connection.get(tokenKey);
            if (serializedObs == null)
                return;

            org.eclipse.californium.core.observe.Observation obs = deserializeObs(serializedObs);
            String registrationId = ObserveUtil.extractRegistrationId(obs);
            Registration registration = getRegistration(connection, registrationId);
            if (registration == null) {
                LOG.warn("Unable to remove observation {}, registration {} does not exist anymore", obs.getRequest(),
                        registrationId);
                return;
            }

            String endpoint = registration.getEndpoint();
            Lock lock = null;
            String lockKey = toLockKey(endpoint);
            try {
                lock = redisLock.obtain(lockKey);
                lock.lock();

                unsafeRemoveObservation(connection, registrationId, token.getBytes());
            } finally {
                if (lock != null) {
                    lock.unlock();
                }
            }
        }

    }

    /**
     * 功能：执行 `get` 对应的处理。
     * 参数：
     * - `token`：`token` 参数。
     * 返回：处理结果。
     */
    @Override
    public org.eclipse.californium.core.observe.Observation get(Token token) {
        try (var connection = connectionFactory.getConnection()) {
            byte[] obs = connection.get(toKey(OBS_TKN, token.getBytes()));
            if (obs == null) {
                return null;
            } else {
                return deserializeObs(obs);
            }
        }
    }

    /* *************** Observation utility functions **************** */

    /**
     * 功能：获取`Registration`。
     * 参数：
     * - `connection`：`connection` 参数。
     * - `registrationId`：`registrationId`ID。
     * 返回：处理结果。
     */
    private Registration getRegistration(RedisConnection connection, String registrationId) {
        byte[] ep = connection.get(toRegIdKey(registrationId));
        if (ep == null) {
            return null;
        }
        byte[] data = connection.get(toEndpointKey(ep));
        if (data == null) {
            return null;
        }

        return deserializeReg(data);
    }

    /**
     * 功能：执行 `unsafeRemoveObservation` 对应的处理。
     * 参数：
     * - `connection`：`connection` 参数。
     * - `registrationId`：`registrationId`ID。
     * - `observationId`：`observationId`ID。
     * 返回：无。
     */
    private void unsafeRemoveObservation(RedisConnection connection, String registrationId, byte[] observationId) {
        if (connection.del(toKey(OBS_TKN, observationId)) > 0L) {
            connection.lRem(toKey(OBS_TKNS_REGID_IDX, registrationId), 0, observationId);
        }
    }

    /**
     * 功能：执行 `unsafeRemoveAllObservations` 对应的处理。
     * 参数：
     * - `connection`：`connection` 参数。
     * - `registrationId`：`registrationId`ID。
     * 返回：匹配的数据集合。
     */
    private Collection<Observation> unsafeRemoveAllObservations(RedisConnection connection, String registrationId) {
        Collection<Observation> removed = new ArrayList<>();
        byte[] regIdKey = toKey(OBS_TKNS_REGID_IDX, registrationId);

        // fetch all observations by token
        for (byte[] token : connection.lRange(regIdKey, 0, -1)) {
            byte[] obs = connection.get(toKey(OBS_TKN, token));
            if (obs != null) {
                removed.add(build(deserializeObs(obs)));
            }
            connection.del(toKey(OBS_TKN, token));
        }
        connection.del(regIdKey);

        return removed;
    }

    /**
     * 功能：更新上下文。
     * 参数：
     * - `token`：`token` 参数。
     * - `correlationContext`：处理上下文。
     * 返回：无。
     */
    @Override
    public void setContext(Token token, EndpointContext correlationContext) {
        // In Leshan we always set context when we send the request, so this should not be needed to implement this.
    }

    /**
     * 功能：执行 `serializeObs` 对应的处理。
     * 参数：
     * - `obs`：`obs` 参数。
     * 返回：处理结果。
     */
    private byte[] serializeObs(org.eclipse.californium.core.observe.Observation obs) {
        return ObservationSerDes.serialize(obs);
    }

    /**
     * 功能：执行 `deserializeObs` 对应的处理。
     * 参数：
     * - `data`：待处理数据。
     * 返回：处理结果。
     */
    private org.eclipse.californium.core.observe.Observation deserializeObs(byte[] data) {
        return ObservationSerDes.deserialize(data);
    }

    /**
     * 功能：执行 `build` 对应的处理。
     * 参数：
     * - `cfObs`：`cfObs` 参数。
     * 返回：处理结果。
     */
    private Observation build(org.eclipse.californium.core.observe.Observation cfObs) {
        if (cfObs == null)
            return null;

        return ObserveUtil.createLwM2mObservation(cfObs.getRequest());
    }

    /* *************** Expiration handling **************** */

    /**
     * Start regular cleanup of dead registrations.
     */
    /**
     * 功能：执行 `start` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public synchronized void start() {
        if (!started) {
            started = true;
            cleanerTask = schedExecutor.scheduleAtFixedRate(new Cleaner(), cleanPeriod, cleanPeriod, TimeUnit.SECONDS);
        }
    }

    /**
     * Stop the underlying cleanup of the registrations.
     */
    /**
     * 功能：执行 `stop` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public synchronized void stop() {
        if (started) {
            started = false;
            if (cleanerTask != null) {
                cleanerTask.cancel(false);
                cleanerTask = null;
            }
        }
    }

    /**
     * Destroy "cleanup" scheduler.
     */
    /**
     * 功能：执行 `destroy` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public synchronized void destroy() {
        started = false;
        schedExecutor.shutdownNow();
        try {
            schedExecutor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOG.warn("Destroying RedisRegistrationStore was interrupted.", e);
        }
    }

    /**
     * 中文说明：
     * 1. 类目的：`Cleaner` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
     * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
     * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
     * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
     * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
     * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
     * 7. 设计模式：主要体现 DTO / Contract / Adapter。
     */
    private class Cleaner implements Runnable {

        /**
         * 功能：执行 `run` 对应的处理。
         * 参数：无。
         * 返回：无。
         */
        @Override
        public void run() {
            try (var connection = connectionFactory.getConnection()) {
                Set<byte[]> endpointsExpired = connection.zRangeByScore(EXP_EP, Double.NEGATIVE_INFINITY,
                        System.currentTimeMillis(), 0, cleanLimit);

                for (byte[] endpoint : endpointsExpired) {
                    Registration r = deserializeReg(connection.get(toEndpointKey(endpoint)));
                    if (!r.isAlive(gracePeriod)) {
                        Deregistration dereg = removeRegistration(connection, r.getId(), true);
                        if (dereg != null)
                            expirationListener.registrationExpired(dereg.getRegistration(), dereg.getObservations());
                    }
                }
            } catch (Exception e) {
                LOG.warn("Unexpected Exception while registration cleaning", e);
            }
        }
    }

    /**
     * 功能：更新监听器。
     * 参数：
     * - `listener`：数据列表。
     * 返回：无。
     */
    @Override
    public void setExpirationListener(ExpirationListener listener) {
        expirationListener = listener;
    }

    /**
     * 功能：更新执行器。
     * 参数：
     * - `executor`：`executor` 参数。
     * 返回：无。
     */
    @Override
    public void setExecutor(ScheduledExecutorService executor) {
        // TODO should we reuse californium executor ?
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`TbLwM2mRedisRegistrationStore` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
