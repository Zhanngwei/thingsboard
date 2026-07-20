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
package org.thingsboard.server.transport.lwm2m.bootstrap.store;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.node.LwM2mObject;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.request.BootstrapDeleteRequest;
import org.eclipse.leshan.core.request.BootstrapDiscoverRequest;
import org.eclipse.leshan.core.request.BootstrapDownlinkRequest;
import org.eclipse.leshan.core.request.BootstrapReadRequest;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.response.BootstrapDiscoverResponse;
import org.eclipse.leshan.core.response.BootstrapReadResponse;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig;
import org.eclipse.leshan.server.bootstrap.BootstrapConfigStore;
import org.eclipse.leshan.server.bootstrap.BootstrapSession;
import org.eclipse.leshan.server.bootstrap.BootstrapUtil;
import org.eclipse.leshan.server.bootstrap.InvalidConfigurationException;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import static org.eclipse.leshan.core.model.ResourceModel.Type.OPAQUE;
import static org.eclipse.leshan.server.bootstrap.BootstrapUtil.toWriteRequest;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.BOOTSTRAP_DEFAULT_SHORT_ID;

/**
 * 中文说明：
 * 1. `LwM2MBootstrapConfigStoreTaskProvider` 是 ThingsBoard Common Transport 中创建或提供 LwM2M 对象的构造组件。
 * 2. 它根据输入配置、类型或上下文选择合适的具体实现。
 * 3. 创建细节被集中在该类型中，调用方只依赖稳定的创建入口。
 * 4. 直接依赖的类型边界包括 `LwM2MBootstrapTaskProvider`。
 * 5. 独立工厂可以避免调用方了解构造顺序和实现类选择规则。
 * 6. 阅读时重点关注实现选择条件、默认分支和对象初始化参数。
 */
@Slf4j
public class LwM2MBootstrapConfigStoreTaskProvider implements LwM2MBootstrapTaskProvider {

    /**
     * 读写锁，用于保护并发读写的共享状态。
     */
    protected final ReadWriteLock readWriteLock;
    protected final Lock writeLock;

    /**
     * 存储组件，表示当前对象的对应属性。
     */
    private BootstrapConfigStore store;

    /**
     * `supportedObjects`映射关系，用于按键查找对应值。
     */
    private Map<Integer, String> supportedObjects;

    /**
     * Map<sEndpoint, LwM2MBootstrapClientInstanceIds: securityInstances, serverInstances>
     */
    /**
     * 会话映射关系，用于按键查找对应值。
     */
    protected Map<String, LwM2MBootstrapClientInstanceIds> lwM2MBootstrapSessionClients;

    /**
     * 功能：创建 `LwM2MBootstrapConfigStoreTaskProvider` 实例，并初始化必要字段。
     * 参数：
     * - `store`：`store` 参数。
     * 返回：新创建的对象实例。
     */
    public LwM2MBootstrapConfigStoreTaskProvider(BootstrapConfigStore store) {
        this.store = store;
        this.lwM2MBootstrapSessionClients = new ConcurrentHashMap<>();
        readWriteLock = new ReentrantReadWriteLock();
        writeLock = readWriteLock.writeLock();
    }

    /**
     * 功能：获取`Tasks`。
     * 参数：
     * - `session`：会话对象。
     * - `previousResponse`：响应对象。
     * 返回：处理结果。
     */
    @Override
    public Tasks getTasks(BootstrapSession session, List<LwM2mResponse> previousResponse) {
        BootstrapConfig config = store.get(session.getEndpoint(), session.getIdentity(), session);
        if (config == null) {
            return null;
        }
        if (previousResponse == null && shouldStartWithDiscover(config)) {
            Tasks tasks = new Tasks();
            tasks.requestsToSend = new ArrayList<>(1);
            tasks.requestsToSend.add(new BootstrapDiscoverRequest());
            tasks.last = false;
            return tasks;
        } else {
            Tasks tasks = new Tasks();
            if (this.supportedObjects == null) {
                initSupportedObjectsDefault();
            }
            // add supportedObjects
            tasks.supportedObjects = this.supportedObjects;
            // handle bootstrap discover response
            if (previousResponse != null) {
                if (previousResponse.get(0) instanceof BootstrapDiscoverResponse) {
                    BootstrapDiscoverResponse discoverResponse = (BootstrapDiscoverResponse) previousResponse.get(0);
                    if (discoverResponse.isSuccess()) {
                        this.initAfterBootstrapDiscover(discoverResponse);
                        findSecurityInstanceId(discoverResponse.getObjectLinks(), session.getEndpoint());
                    } else {
                        log.warn(
                                "Bootstrap Discover return error {} : to continue bootstrap session without autoIdForSecurityObject mode. {}",
                                discoverResponse, session);
                    }
                    if (this.lwM2MBootstrapSessionClients.get(session.getEndpoint()).getSecurityInstances().get(BOOTSTRAP_DEFAULT_SHORT_ID) == null) {
                        log.error(
                                "Unable to find bootstrap server instance in Security Object (0) in response {}: unable to continue bootstrap session with autoIdForSecurityObject mode. {}",
                                discoverResponse, session);
                        return null;
                    }
                    tasks.requestsToSend = new ArrayList<>(1);
                    tasks.requestsToSend.add(new BootstrapReadRequest("/1"));
                    tasks.last = false;
                    return tasks;
                }
                BootstrapReadResponse readResponse = (BootstrapReadResponse) previousResponse.get(0);
                Integer bootstrapServerIdOld = null;
                if (readResponse.isSuccess()) {
                    findServerInstanceId(readResponse, session.getEndpoint());
                    if (this.lwM2MBootstrapSessionClients.get(session.getEndpoint()).getSecurityInstances().size() > 0 && this.lwM2MBootstrapSessionClients.get(session.getEndpoint()).getServerInstances().size() > 0) {
                        bootstrapServerIdOld = this.findBootstrapServerId(session.getEndpoint());
                    }
                } else {
                    log.warn(
                            "Bootstrap ReadResponse return error {} : to continue bootstrap session without find Server Instance Id. {}",
                            readResponse, session);
                }
                // create requests from config
                tasks.requestsToSend = this.toRequests(config,
                        config.contentFormat != null ? config.contentFormat : session.getContentFormat(),
                        bootstrapServerIdOld, session.getEndpoint());
            } else {
                // create requests from config
                tasks.requestsToSend = BootstrapUtil.toRequests(config,
                        config.contentFormat != null ? config.contentFormat : session.getContentFormat());
            }
            return tasks;
        }
    }

    /**
     * 功能：执行 `shouldStartWithDiscover` 对应的处理。
     * 参数：
     * - `config`：配置对象。
     * 返回：判断结果。
     */
    protected boolean shouldStartWithDiscover(BootstrapConfig config) {
        return config.autoIdForSecurityObject;
    }

    /**
     * "Short Server ID": This Resource MUST be set when the Bootstrap-Server Resource has a value of 'false'.
     * The values ID:0 and ID:65535 values MUST NOT be used for identifying the LwM2M Server.
     * "Short Server ID":
     * - Link Instance (lwm2m Server) hase linkParams with key = "ssid" value = "shortId" (ver lvm2m = 1.1).
     * - Link Instance (bootstrap Server) hase not linkParams with key = "ssid" (ver lvm2m = 1.1).
     */
    /**
     * 功能：获取`Security Instance Id`。
     * 参数：
     * - `objectLinks`：`objectLinks` 参数。
     * - `endpoint`：`endpoint` 参数。
     * 返回：无。
     */
    protected void findSecurityInstanceId(Link[] objectLinks, String endpoint) {
        log.info("Object after discover: [{}]", objectLinks);
        for (Link link : objectLinks) {
            if (link.getUriReference().startsWith("/0/")) {
                try {
                    LwM2mPath path = new LwM2mPath(link.getUriReference());
                    if (path.isObjectInstance()) {
                        if (link.getLinkParams().containsKey("ssid")) {
                            int serverId = Integer.parseInt(link.getLinkParams().get("ssid").getUnquoted());
                            if (!lwM2MBootstrapSessionClients.get(endpoint).getSecurityInstances().containsKey(serverId)) {
                                lwM2MBootstrapSessionClients.get(endpoint).getSecurityInstances().put(serverId, path.getObjectInstanceId());
                            } else {
                                log.error("Invalid lwm2mSecurityInstance by [{}]", path.getObjectInstanceId());
                            }
                            lwM2MBootstrapSessionClients.get(endpoint).getSecurityInstances().put(Integer.valueOf(link.getLinkParams().get("ssid").getUnquoted()), path.getObjectInstanceId());
                        } else {
                            if (!this.lwM2MBootstrapSessionClients.get(endpoint).getSecurityInstances().containsKey(0)) {
                                this.lwM2MBootstrapSessionClients.get(endpoint).getSecurityInstances().put(BOOTSTRAP_DEFAULT_SHORT_ID, path.getObjectInstanceId());
                            } else {
                                log.error("Invalid bootstrapSecurityInstance by [{}]", path.getObjectInstanceId());
                            }
                        }
                    }
                } catch (Exception e) {
                    // ignore if this is not a LWM2M path
                    log.error("Invalid LwM2MPath starting by \"/0/\"");
                }
            }
        }
    }

    /**
     * 功能：获取服务端。
     * 参数：
     * - `readResponse`：响应对象。
     * - `endpoint`：`endpoint` 参数。
     * 返回：无。
     */
    protected void findServerInstanceId(BootstrapReadResponse readResponse, String endpoint) {
        try {
            ((LwM2mObject) readResponse.getContent()).getInstances().values().forEach(instance -> {
                var shId = OPAQUE.equals(instance.getResource(0).getType()) ? new BigInteger((byte[]) instance.getResource(0).getValue()).intValue() : instance.getResource(0).getValue();
                int shortId;
                if (shId instanceof Long) {
                    shortId = ((Long) shId).intValue();
                } else {
                    shortId = (int) shId;
                }
                this.lwM2MBootstrapSessionClients.get(endpoint).getServerInstances().put(shortId, instance.getId());
            });
        } catch (Exception e) {
            log.error("Failed find Server Instance Id. ", e);
        }
    }

    /**
     * 功能：获取服务端。
     * 参数：
     * - `endpoint`：`endpoint` 参数。
     * 返回：数值结果。
     */
    protected Integer findBootstrapServerId(String endpoint) {
        Integer bootstrapServerIdOld = null;
        Map<Integer, Integer> filteredMap = this.lwM2MBootstrapSessionClients.get(endpoint).getServerInstances().entrySet()
                .stream().filter(x -> !this.lwM2MBootstrapSessionClients.get(endpoint).getSecurityInstances().containsKey(x.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (filteredMap.size() > 0) {
            bootstrapServerIdOld = filteredMap.keySet().stream().findFirst().get();
        }
        return bootstrapServerIdOld;
    }

    /**
     * 功能：获取存储组件。
     * 参数：无。
     * 返回：处理结果。
     */
    public BootstrapConfigStore getStore() {
        return this.store;
    }

    /**
     * 功能：初始化或启动`After Bootstrap Discover`。
     * 参数：
     * - `response`：响应对象。
     * 返回：无。
     */
    private void initAfterBootstrapDiscover(BootstrapDiscoverResponse response) {
        Link[] links = response.getObjectLinks();
        Arrays.stream(links).forEach(link -> {
            LwM2mPath path = new LwM2mPath(link.getUriReference());
            if (!path.isRoot() && path.getObjectId() < 3) {
                if (path.isObject()) {
                    String ver = link.getLinkParams().get("ver") != null ? link.getLinkParams().get("ver").getUnquoted() : "1.0";
                    this.supportedObjects.put(path.getObjectId(), ver);
                }
            }
        });
    }


    /**
     * 功能：执行 `toRequests` 对应的处理。
     * 参数：
     * - `bootstrapConfig`：配置对象。
     * - `contentFormat`：`contentFormat` 参数。
     * - `bootstrapServerIdOld`：`bootstrapServerIdOld` 参数。
     * - `endpoint`：`endpoint` 参数。
     * 返回：处理结果。
     */
    public List<BootstrapDownlinkRequest<? extends LwM2mResponse>> toRequests(BootstrapConfig bootstrapConfig,
                                                                              ContentFormat contentFormat,
                                                                              Integer bootstrapServerIdOld,
                                                                              String endpoint) {
        List<BootstrapDownlinkRequest<? extends LwM2mResponse>> requests = new ArrayList<>();
        Set<String> pathsDelete = new HashSet<>();
        List<BootstrapDownlinkRequest<? extends LwM2mResponse>> requestsWrite = new ArrayList<>();
        boolean isBsServer = false;
        boolean isLwServer = false;
        /** Map<serverId ("Short Server ID"), InstanceId> */
        Map<Integer, Integer> instances = new HashMap<>();
        Integer bootstrapServerIdNew = null;
        // handle security
        int lwm2mSecurityInstanceId = 0;
        int bootstrapSecurityInstanceId = this.lwM2MBootstrapSessionClients.get(endpoint).getSecurityInstances().get(BOOTSTRAP_DEFAULT_SHORT_ID);
        for (BootstrapConfig.ServerSecurity security : new TreeMap<>(bootstrapConfig.security).values()) {
            if (security.bootstrapServer) {
                requestsWrite.add(toWriteRequest(bootstrapSecurityInstanceId, security, contentFormat));
                isBsServer = true;
                bootstrapServerIdNew = security.serverId;
                instances.put(security.serverId, bootstrapSecurityInstanceId);
            } else {
                if (lwm2mSecurityInstanceId == bootstrapSecurityInstanceId) {
                    lwm2mSecurityInstanceId++;
                }
                requestsWrite.add(toWriteRequest(lwm2mSecurityInstanceId, security, contentFormat));
                instances.put(security.serverId, lwm2mSecurityInstanceId);
                isLwServer = true;
                if (!isBsServer && this.lwM2MBootstrapSessionClients.get(endpoint).getSecurityInstances().containsKey(security.serverId) &&
                        lwm2mSecurityInstanceId != this.lwM2MBootstrapSessionClients.get(endpoint).getSecurityInstances().get(security.serverId)) {
                    pathsDelete.add("/0/" + this.lwM2MBootstrapSessionClients.get(endpoint).getSecurityInstances().get(security.serverId));
                }
                /**
                 * If there is an instance in the serverInstances with serverId which we replace in the securityInstances
                 */
                // find serverId in securityInstances by id (instance)
                Integer serverIdOld = null;
                for (Map.Entry<Integer, Integer> entry : this.lwM2MBootstrapSessionClients.get(endpoint).getSecurityInstances().entrySet()) {
                    if (entry.getValue().equals(lwm2mSecurityInstanceId)) {
                        serverIdOld = entry.getKey();
                    }
                }
                if (!isBsServer && serverIdOld != null && this.lwM2MBootstrapSessionClients.get(endpoint).getServerInstances().containsKey(serverIdOld)) {
                    pathsDelete.add("/1/" + this.lwM2MBootstrapSessionClients.get(endpoint).getServerInstances().get(serverIdOld));
                }
                lwm2mSecurityInstanceId++;
            }
        }
        // handle server
        for (Map.Entry<Integer, BootstrapConfig.ServerConfig> server : bootstrapConfig.servers.entrySet()) {
            int securityInstanceId = instances.get(server.getValue().shortId);
            requestsWrite.add(toWriteRequest(securityInstanceId, server.getValue(), contentFormat));
            if (!isBsServer) {
                /** Delete instance if bootstrapServerIdNew not equals bootstrapServerIdOld or securityInstanceBsIdNew not equals serverInstanceBsIdOld */
                if (bootstrapServerIdNew != null && server.getValue().shortId == bootstrapServerIdNew &&
                        (bootstrapServerIdNew != bootstrapServerIdOld || securityInstanceId != this.lwM2MBootstrapSessionClients.get(endpoint).getServerInstances().get(bootstrapServerIdOld))) {
                    pathsDelete.add("/1/" + this.lwM2MBootstrapSessionClients.get(endpoint).getServerInstances().get(bootstrapServerIdOld));
                    /** Delete instance if serverIdNew is present in serverInstances and  securityInstanceIdOld by serverIdNew not equals serverInstanceIdOld */
                } else if (this.lwM2MBootstrapSessionClients.get(endpoint).getServerInstances().containsKey(server.getValue().shortId) &&
                        securityInstanceId != this.lwM2MBootstrapSessionClients.get(endpoint).getServerInstances().get(server.getValue().shortId)) {
                    pathsDelete.add("/1/" + this.lwM2MBootstrapSessionClients.get(endpoint).getServerInstances().get(server.getValue().shortId));
                }
            }
        }
        // handle acl
        for (Map.Entry<Integer, BootstrapConfig.ACLConfig> acl : bootstrapConfig.acls.entrySet()) {
            requestsWrite.add(toWriteRequest(acl.getKey(), acl.getValue(), contentFormat));
        }
        // handle delete
        if (isBsServer && isLwServer) {
            requests.add(new BootstrapDeleteRequest("/0"));
            requests.add(new BootstrapDeleteRequest("/1"));
        } else {
            pathsDelete.forEach(pathDelete -> requests.add(new BootstrapDeleteRequest(pathDelete)));
        }
        // handle write
        if (requestsWrite.size() > 0) {
            requests.addAll(requestsWrite);
        }
        return (requests);
    }


    /**
     * 功能：初始化或启动`Supported Objects Default`。
     * 参数：无。
     * 返回：无。
     */
    private void initSupportedObjectsDefault() {
        this.supportedObjects = new HashMap<>();
        this.supportedObjects.put(0, "1.1");
        this.supportedObjects.put(1, "1.1");
        this.supportedObjects.put(2, "1.0");
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
            this.lwM2MBootstrapSessionClients.remove(endpoint);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * 功能：执行 `put` 对应的处理。
     * 参数：
     * - `endpoint`：`endpoint` 参数。
     * 返回：无。
     */
    @Override
    public void put(String endpoint) throws InvalidConfigurationException {
        writeLock.lock();
        try {
            this.lwM2MBootstrapSessionClients.put(endpoint, new LwM2MBootstrapClientInstanceIds());
        } finally {
            writeLock.unlock();
        }
    }
}
