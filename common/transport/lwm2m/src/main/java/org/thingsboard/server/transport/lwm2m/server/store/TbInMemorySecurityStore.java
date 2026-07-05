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
import org.thingsboard.server.transport.lwm2m.secure.TbLwM2MSecurityInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 中文说明：
 * 1. 类目的：`TbInMemorySecurityStore` 是ThingsBoard Common 模块中的公共基础设施类型，用于定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 生命周期：由调用模块、Spring Bean、协议处理器、队列流程或序列化框架管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Contract / Adapter。
 */
public class TbInMemorySecurityStore implements TbEditableSecurityStore {
    // lock for the two maps
    protected final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    protected final Lock readLock = readWriteLock.readLock();
    protected final Lock writeLock = readWriteLock.writeLock();

    // by client end-point
    protected Map<String, TbLwM2MSecurityInfo> securityByEp = new HashMap<>();

    // by PSK identity
    protected Map<String, TbLwM2MSecurityInfo> securityByIdentity = new HashMap<>();

    /**
     * 功能：创建 `TbInMemorySecurityStore` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public TbInMemorySecurityStore() {
    }

    /**
     * {@inheritDoc}
     */
    /**
     * 功能：获取`By Endpoint`。
     * 参数：
     * - `endpoint`：`endpoint` 参数。
     * 返回：处理结果。
     */
    @Override
    public SecurityInfo getByEndpoint(String endpoint) {
        readLock.lock();
        try {
            TbLwM2MSecurityInfo securityInfo = securityByEp.get(endpoint);
            if (securityInfo != null ) {
                if (SecurityMode.NO_SEC.equals(securityInfo.getSecurityMode())) {
                    return SecurityInfo.newPreSharedKeyInfo(SecurityMode.NO_SEC.toString(), SecurityMode.NO_SEC.toString(),
                            SecurityMode.NO_SEC.toString().getBytes());
                } else {
                    return securityInfo.getSecurityInfo();
                }
            }
            else {
                return null;
            }
        } finally {
            readLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    /**
     * 功能：获取`By Identity`。
     * 参数：
     * - `identity`：实体对象。
     * 返回：处理结果。
     */
    @Override
    public SecurityInfo getByIdentity(String identity) {
        readLock.lock();
        try {
            TbLwM2MSecurityInfo securityInfo = securityByIdentity.get(identity);
            if (securityInfo != null) {
                return securityInfo.getSecurityInfo();
            } else {
                return null;
            }
        } finally {
            readLock.unlock();
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
        writeLock.lock();
        try {
            String identity = null;
            if (tbSecurityInfo.getSecurityInfo() != null) {
                identity = tbSecurityInfo.getSecurityInfo().getIdentity();
                if (identity != null) {
                    TbLwM2MSecurityInfo infoByIdentity = securityByIdentity.get(identity);
                    if (infoByIdentity != null && !tbSecurityInfo.getSecurityInfo().getEndpoint().equals(infoByIdentity.getEndpoint())) {
                        throw new NonUniqueSecurityInfoException("PSK Identity " + identity + " is already used");
                    }
                    securityByIdentity.put(tbSecurityInfo.getSecurityInfo().getIdentity(), tbSecurityInfo);
                }
            }

            TbLwM2MSecurityInfo previous = securityByEp.put(tbSecurityInfo.getEndpoint(), tbSecurityInfo);
            if (previous != null && previous.getSecurityInfo() != null) {
                String previousIdentity = previous.getSecurityInfo().getIdentity();
                if (previousIdentity != null && !previousIdentity.equals(identity)) {
                    securityByIdentity.remove(previousIdentity);
                }
            }
        } finally {
            writeLock.unlock();
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
        writeLock.lock();
        try {
            TbLwM2MSecurityInfo securityInfo = securityByEp.remove(endpoint);
            if (securityInfo != null && securityInfo.getSecurityInfo() != null && securityInfo.getSecurityInfo().getIdentity() != null) {
                securityByIdentity.remove(securityInfo.getSecurityInfo().getIdentity());
            }
        } finally {
            writeLock.unlock();
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
        readLock.lock();
        try {
            return securityByEp.get(endpoint);
        } finally {
            readLock.unlock();
        }
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`TbInMemorySecurityStore` 在 ThingsBoard Common 模块 中承担公共基础设施类型职责，核心目的是定义跨服务端模块复用的数据结构、接口契约或协议适配逻辑。
 * 2. 核心流程：接收调用方输入后完成数据承载、协议转换、接口委派或测试断言。
 * 3. 关键依赖：主要依赖或协作对象包括DAO、Application、Rule Engine、Transport、Queue、Actor、Cache 和 Edge 同步模块。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
