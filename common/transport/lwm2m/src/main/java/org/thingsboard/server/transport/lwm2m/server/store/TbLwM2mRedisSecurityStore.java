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

import org.eclipse.leshan.core.SecurityMode;
import org.eclipse.leshan.server.security.NonUniqueSecurityInfoException;
import org.eclipse.leshan.server.security.SecurityInfo;
import org.nustaq.serialization.FSTConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.thingsboard.server.transport.lwm2m.secure.TbLwM2MSecurityInfo;

import java.util.concurrent.locks.Lock;

/**
 * 中文说明：
 * 1. `TbLwM2mRedisSecurityStore` 是 ThingsBoard Common Transport 中负责 LwM2M 接入或传输适配的类型。
 * 2. 它处理连接、会话、协议消息或平台传输消息之间的转换。
 * 3. 类中的状态和配置用于控制当前协议交互的具体行为。
 * 4. 直接依赖的类型边界包括 `TbEditableSecurityStore`。
 * 5. 单独的传输类型可以隔离协议细节，使平台内部继续使用统一消息模型。
 * 6. 阅读时重点关注入站消息入口、会话状态和消息提交位置。
 */
public class TbLwM2mRedisSecurityStore implements TbEditableSecurityStore {
    /**
     * `SEC_EP`常量，用于统一引用固定值。
     */
    private static final String SEC_EP = "SEC#EP#";
    private static final String LOCK_EP = "LOCK#EP#";
    /**
     * `PSKID_SEC`常量，用于统一引用固定值。
     */
    private static final String PSKID_SEC = "PSKID#SEC";

    /**
     * 工厂，用于按场景创建或提供目标对象。
     */
    private final RedisConnectionFactory connectionFactory;
    private final FSTConfiguration serializer;
    /**
     * 锁，用于保护并发读写的共享状态。
     */
    private final RedisLockRegistry redisLock;

    /**
     * 功能：创建 `TbLwM2mRedisSecurityStore` 实例，并初始化必要字段。
     * 参数：
     * - `connectionFactory`：`connectionFactory` 参数。
     * 返回：新创建的对象实例。
     */
    public TbLwM2mRedisSecurityStore(RedisConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
        redisLock = new RedisLockRegistry(connectionFactory, "Security");
        serializer = FSTConfiguration.createDefaultConfiguration();
    }

    /**
     * 功能：获取`By Endpoint`。
     * 参数：
     * - `endpoint`：`endpoint` 参数。
     * 返回：处理结果。
     */
    @Override
    public SecurityInfo getByEndpoint(String endpoint) {
        Lock lock = null;
        try (var connection = connectionFactory.getConnection()) {
            lock = redisLock.obtain(toLockKey(endpoint));
            lock.lock();
            byte[] data = connection.get((SEC_EP + endpoint).getBytes());
            if (data == null || data.length == 0) {
                return null;
            } else {
                if (SecurityMode.NO_SEC.equals(((TbLwM2MSecurityInfo) serializer.asObject(data)).getSecurityMode())) {
                    return SecurityInfo.newPreSharedKeyInfo(SecurityMode.NO_SEC.toString(), SecurityMode.NO_SEC.toString(),
                            SecurityMode.NO_SEC.toString().getBytes());
                }
                else {
                    return ((TbLwM2MSecurityInfo) serializer.asObject(data)).getSecurityInfo();
                }
            }
        } finally {
            if (lock != null) {
                lock.unlock();
            }
        }
    }

    /**
     * 功能：获取`By Identity`。
     * 参数：
     * - `identity`：实体对象。
     * 返回：处理结果。
     */
    @Override
    public SecurityInfo getByIdentity(String identity) {
        Lock lock = null;
        try (var connection = connectionFactory.getConnection()) {
            lock = redisLock.obtain(toLockKey(identity));
            lock.lock();
            byte[] ep = connection.hGet(PSKID_SEC.getBytes(), identity.getBytes());
            if (ep == null) {
                return null;
            } else {
                byte[] data = connection.get((SEC_EP + new String(ep)).getBytes());
                if (data == null || data.length == 0) {
                    return null;
                } else {
                    return ((TbLwM2MSecurityInfo) serializer.asObject(data)).getSecurityInfo();
                }
            }
        } finally {
            if (lock != null) {
                lock.unlock();
            }
        }
    }

    /**
     * 功能：执行 `put` 对应的处理。
     * 参数：
     * - `tbSecurityInfo`：`tbSecurityInfo` 参数。
     * 返回：无。
     */
    @Override
    public void put(TbLwM2MSecurityInfo tbSecurityInfo) throws NonUniqueSecurityInfoException {
        SecurityInfo info = tbSecurityInfo.getSecurityInfo();
        byte[] tbSecurityInfoSerialized = serializer.asByteArray(tbSecurityInfo);
        Lock lock = null;
        try (var connection = connectionFactory.getConnection()) {
            lock = redisLock.obtain(tbSecurityInfo.getEndpoint());
            lock.lock();
            if (info != null && info.getIdentity() != null) {
                byte[] oldEndpointBytes = connection.hGet(PSKID_SEC.getBytes(), info.getIdentity().getBytes());
                if (oldEndpointBytes != null) {
                    String oldEndpoint = new String(oldEndpointBytes);
                    if (!oldEndpoint.equals(info.getEndpoint())) {
                        throw new NonUniqueSecurityInfoException("PSK Identity " + info.getIdentity() + " is already used");
                    }
                    connection.hSet(PSKID_SEC.getBytes(), info.getIdentity().getBytes(), info.getEndpoint().getBytes());
                }
            }

            byte[] previousData = connection.getSet((SEC_EP + tbSecurityInfo.getEndpoint()).getBytes(), tbSecurityInfoSerialized);
            if (previousData != null && info != null) {
                String previousIdentity = ((TbLwM2MSecurityInfo) serializer.asObject(previousData)).getSecurityInfo().getIdentity();
                if (previousIdentity != null && !previousIdentity.equals(info.getIdentity())) {
                    connection.hDel(PSKID_SEC.getBytes(), previousIdentity.getBytes());
                }
            }
        } finally {
            if (lock != null) {
                lock.unlock();
            }
        }
    }

    /**
     * 功能：获取信息对象。
     * 参数：
     * - `endpoint`：`endpoint` 参数。
     * 返回：处理结果。
     */
    @Override
    public TbLwM2MSecurityInfo getTbLwM2MSecurityInfoByEndpoint(String endpoint) {
        Lock lock = null;
        try (var connection = connectionFactory.getConnection()) {
            lock = redisLock.obtain(endpoint);
            lock.lock();
            byte[] data = connection.get((SEC_EP + endpoint).getBytes());
            if (data != null && data.length > 0) {
                return (TbLwM2MSecurityInfo) serializer.asObject(data);
            } else {
                return null;
            }
        } finally {
            if (lock != null) {
                lock.unlock();
            }
        }
    }

    /**
     * 功能：执行 `remove` 对应的处理。
     * 参数：
     * - `endpoint`：`endpoint` 参数。
     * 返回：无。
     */
    @Override
    public void remove(String endpoint) {
        Lock lock = null;
        try (var connection = connectionFactory.getConnection()) {
            lock = redisLock.obtain(endpoint);
            lock.lock();
            byte[] data = connection.get((SEC_EP + endpoint).getBytes());
            if (data != null && data.length > 0) {
                SecurityInfo info = ((TbLwM2MSecurityInfo) serializer.asObject(data)).getSecurityInfo();
                if (info != null && info.getIdentity() != null) {
                    connection.hDel(PSKID_SEC.getBytes(), info.getIdentity().getBytes());
                }
                connection.del((SEC_EP + endpoint).getBytes());
            }
        } finally {
            if (lock != null) {
                lock.unlock();
            }
        }
    }

    /**
     * 功能：执行 `toLockKey` 对应的处理。
     * 参数：
     * - `endpoint`：`endpoint` 参数。
     * 返回：文本结果。
     */
    private String toLockKey(String endpoint) {
        return LOCK_EP + endpoint;
    }
}
