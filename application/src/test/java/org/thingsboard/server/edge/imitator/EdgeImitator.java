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
package org.thingsboard.server.edge.imitator;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.AbstractMessage;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.thingsboard.edge.rpc.EdgeGrpcClient;
import org.thingsboard.edge.rpc.EdgeRpcClient;
import org.thingsboard.server.controller.AbstractWebTest;
import org.thingsboard.server.gen.edge.v1.AdminSettingsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AlarmCommentUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AlarmUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AssetProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AssetUpdateMsg;
import org.thingsboard.server.gen.edge.v1.CustomerUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DashboardUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceCredentialsRequestMsg;
import org.thingsboard.server.gen.edge.v1.DeviceCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DeviceRpcCallMsg;
import org.thingsboard.server.gen.edge.v1.DeviceUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkResponseMsg;
import org.thingsboard.server.gen.edge.v1.EdgeConfiguration;
import org.thingsboard.server.gen.edge.v1.EntityDataProto;
import org.thingsboard.server.gen.edge.v1.EntityViewUpdateMsg;
import org.thingsboard.server.gen.edge.v1.OtaPackageUpdateMsg;
import org.thingsboard.server.gen.edge.v1.QueueUpdateMsg;
import org.thingsboard.server.gen.edge.v1.RelationUpdateMsg;
import org.thingsboard.server.gen.edge.v1.ResourceUpdateMsg;
import org.thingsboard.server.gen.edge.v1.RuleChainMetadataUpdateMsg;
import org.thingsboard.server.gen.edge.v1.RuleChainUpdateMsg;
import org.thingsboard.server.gen.edge.v1.TenantProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.TenantUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.gen.edge.v1.UplinkResponseMsg;
import org.thingsboard.server.gen.edge.v1.UserCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UserUpdateMsg;
import org.thingsboard.server.gen.edge.v1.WidgetTypeUpdateMsg;
import org.thingsboard.server.gen.edge.v1.WidgetsBundleUpdateMsg;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. 类目的：`EdgeImitator` 是ThingsBoard Application 测试模块中的测试支撑类型，用于验证 Application 模块的控制器、服务、Actor 或集成流程。
 * 2. 所属模块：位于 application 模块，支撑服务端启动、Web API、Actor、队列、传输层或业务服务流程。
 * 3. 协作对象：主要协作对象包括JUnit、Mockito、Spring Test、DAO、缓存、Transport 和测试夹具。
 * 4. 生命周期：由测试框架按测试类和测试方法管理。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 Test Fixture。
 */
@Slf4j
public class EdgeImitator {

    /**
     * 键，用于定位映射、配置或数据项。
     */
    private String routingKey;
    private String routingSecret;

    /**
     * 边缘节点，用于发起外部调用或协议交互。
     */
    private EdgeRpcClient edgeRpcClient;

    private final Lock lock = new ReentrantLock();

    /**
     * 等待器，用于在测试或异步流程中等待结果。
     */
    private CountDownLatch messagesLatch;
    private CountDownLatch responsesLatch;
    /**
     * `ignoredTypes`列表，用于保存一组待处理对象。
     */
    private List<Class<? extends AbstractMessage>> ignoredTypes;

    /**
     * 是否满足时序数据条件。
     */
    @Setter
    private boolean randomFailuresOnTimeseriesDownlink = false;
    /**
     * 失败信息，表示当前对象的对应属性。
     */
    @Setter
    private double failureProbability = 0.0;

    /**
     * `configuration`，保存当前对象的配置选项。
     */
    @Getter
    private EdgeConfiguration configuration;
    /**
     * `downlinkMsgs`列表，用于保存一组待处理对象。
     */
    @Getter
    private List<AbstractMessage> downlinkMsgs;

    /**
     * 当前响应对象，封装处理完成后的返回信息。
     */
    @Getter
    private UplinkResponseMsg latestResponseMsg;

    /**
     * 功能：创建 `EdgeImitator` 实例，并初始化必要字段。
     * 参数：
     * - `host`：`host` 参数。
     * - `port`：`port` 参数。
     * - `routingKey`：键。
     * - `routingSecret`：`routingSecret` 参数。
     * 返回：新创建的对象实例。
     */
    public EdgeImitator(String host, int port, String routingKey, String routingSecret) throws NoSuchFieldException, IllegalAccessException {
        edgeRpcClient = new EdgeGrpcClient();
        messagesLatch = new CountDownLatch(0);
        responsesLatch = new CountDownLatch(0);
        downlinkMsgs = new ArrayList<>();
        ignoredTypes = new ArrayList<>();
        this.routingKey = routingKey;
        this.routingSecret = routingSecret;
        updateEdgeClientFields("rpcHost", host);
        updateEdgeClientFields("rpcPort", port);
        updateEdgeClientFields("timeoutSecs", 3);
        updateEdgeClientFields("keepAliveTimeSec", 300);
        updateEdgeClientFields("keepAliveTimeoutSec", 5);
        updateEdgeClientFields("maxInboundMessageSize", 4194304);
    }

    /**
     * 功能：更新边缘节点。
     * 参数：
     * - `fieldName`：名称。
     * - `value`：值。
     * 返回：无。
     */
    private void updateEdgeClientFields(String fieldName, Object value) throws NoSuchFieldException, IllegalAccessException {
        Field fieldToSet = edgeRpcClient.getClass().getDeclaredField(fieldName);
        fieldToSet.setAccessible(true);
        fieldToSet.set(edgeRpcClient, value);
        fieldToSet.setAccessible(false);
    }

    /**
     * 功能：执行 `connect` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void connect() {
        edgeRpcClient.connect(routingKey, routingSecret,
                this::onUplinkResponse,
                this::onEdgeUpdate,
                this::onDownlink,
                this::onClose);

        edgeRpcClient.sendSyncRequestMsg(true);
    }

    /**
     * 功能：执行 `disconnect` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void disconnect() throws InterruptedException {
        edgeRpcClient.disconnect(false);
    }

    /**
     * 功能：发送或提交消息。
     * 参数：
     * - `uplinkMsg`：待处理消息。
     * 返回：无。
     */
    public void sendUplinkMsg(UplinkMsg uplinkMsg) {
        edgeRpcClient.sendUplinkMsg(uplinkMsg);
    }

    /**
     * 功能：处理响应。
     * 参数：
     * - `msg`：待处理消息。
     * 返回：无。
     */
    private void onUplinkResponse(UplinkResponseMsg msg) {
        log.info("onUplinkResponse: {}", msg);
        latestResponseMsg = msg;
        responsesLatch.countDown();
    }

    /**
     * 功能：处理边缘节点。
     * 参数：
     * - `edgeConfiguration`：配置对象。
     * 返回：无。
     */
    private void onEdgeUpdate(EdgeConfiguration edgeConfiguration) {
        this.configuration = edgeConfiguration;
    }

    /**
     * 功能：处理`on Downlink`。
     * 参数：
     * - `downlinkMsg`：待处理消息。
     * 返回：无。
     */
    private void onDownlink(DownlinkMsg downlinkMsg) {
        ListenableFuture<List<Void>> future = processDownlinkMsg(downlinkMsg);
        Futures.addCallback(future, new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable List<Void> result) {
                DownlinkResponseMsg downlinkResponseMsg = DownlinkResponseMsg.newBuilder()
                        .setDownlinkMsgId(downlinkMsg.getDownlinkMsgId())
                        .setSuccess(true).build();
                edgeRpcClient.sendDownlinkResponseMsg(downlinkResponseMsg);
            }

            @Override
            public void onFailure(Throwable t) {
                DownlinkResponseMsg downlinkResponseMsg = DownlinkResponseMsg.newBuilder()
                        .setDownlinkMsgId(downlinkMsg.getDownlinkMsgId())
                        .setSuccess(false).setErrorMsg(t.getMessage()).build();
                edgeRpcClient.sendDownlinkResponseMsg(downlinkResponseMsg);
            }
        }, MoreExecutors.directExecutor());
    }

    /**
     * 功能：处理`on Close`。
     * 参数：
     * - `e`：`e` 参数。
     * 返回：无。
     */
    private void onClose(Exception e) {
        log.info("onClose: {}", e.getMessage());
    }

    /**
     * 功能：处理消息。
     * 参数：
     * - `downlinkMsg`：待处理消息。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<List<Void>> processDownlinkMsg(DownlinkMsg downlinkMsg) {
        log.trace("processDownlinkMsg: {}", downlinkMsg);
        List<ListenableFuture<Void>> result = new ArrayList<>();
        if (downlinkMsg.getAdminSettingsUpdateMsgCount() > 0) {
            for (AdminSettingsUpdateMsg adminSettingsUpdateMsg : downlinkMsg.getAdminSettingsUpdateMsgList()) {
                result.add(saveDownlinkMsg(adminSettingsUpdateMsg));
            }
        }
        if (downlinkMsg.getDeviceProfileUpdateMsgCount() > 0) {
            for (DeviceProfileUpdateMsg deviceProfileUpdateMsg : downlinkMsg.getDeviceProfileUpdateMsgList()) {
                result.add(saveDownlinkMsg(deviceProfileUpdateMsg));
            }
        }
        if (downlinkMsg.getDeviceUpdateMsgCount() > 0) {
            for (DeviceUpdateMsg deviceUpdateMsg : downlinkMsg.getDeviceUpdateMsgList()) {
                result.add(saveDownlinkMsg(deviceUpdateMsg));
            }
        }
        if (downlinkMsg.getDeviceCredentialsUpdateMsgCount() > 0) {
            for (DeviceCredentialsUpdateMsg deviceCredentialsUpdateMsg : downlinkMsg.getDeviceCredentialsUpdateMsgList()) {
                result.add(saveDownlinkMsg(deviceCredentialsUpdateMsg));
            }
        }
        if (downlinkMsg.getAssetProfileUpdateMsgCount() > 0) {
            for (AssetProfileUpdateMsg assetProfileUpdateMsg : downlinkMsg.getAssetProfileUpdateMsgList()) {
                result.add(saveDownlinkMsg(assetProfileUpdateMsg));
            }
        }
        if (downlinkMsg.getAssetUpdateMsgCount() > 0) {
            for (AssetUpdateMsg assetUpdateMsg : downlinkMsg.getAssetUpdateMsgList()) {
                result.add(saveDownlinkMsg(assetUpdateMsg));
            }
        }
        if (downlinkMsg.getRuleChainUpdateMsgCount() > 0) {
            for (RuleChainUpdateMsg ruleChainUpdateMsg : downlinkMsg.getRuleChainUpdateMsgList()) {
                result.add(saveDownlinkMsg(ruleChainUpdateMsg));
            }
        }
        if (downlinkMsg.getRuleChainMetadataUpdateMsgCount() > 0) {
            for (RuleChainMetadataUpdateMsg ruleChainMetadataUpdateMsg : downlinkMsg.getRuleChainMetadataUpdateMsgList()) {
                result.add(saveDownlinkMsg(ruleChainMetadataUpdateMsg));
            }
        }
        if (downlinkMsg.getDashboardUpdateMsgCount() > 0) {
            for (DashboardUpdateMsg dashboardUpdateMsg : downlinkMsg.getDashboardUpdateMsgList()) {
                result.add(saveDownlinkMsg(dashboardUpdateMsg));
            }
        }
        if (downlinkMsg.getRelationUpdateMsgCount() > 0) {
            for (RelationUpdateMsg relationUpdateMsg : downlinkMsg.getRelationUpdateMsgList()) {
                result.add(saveDownlinkMsg(relationUpdateMsg));
            }
        }
        if (downlinkMsg.getAlarmUpdateMsgCount() > 0) {
            for (AlarmUpdateMsg alarmUpdateMsg : downlinkMsg.getAlarmUpdateMsgList()) {
                result.add(saveDownlinkMsg(alarmUpdateMsg));
            }
        }
        if (downlinkMsg.getAlarmCommentUpdateMsgCount() > 0) {
            for (AlarmCommentUpdateMsg alarmCommentUpdateMsg : downlinkMsg.getAlarmCommentUpdateMsgList()) {
                result.add(saveDownlinkMsg(alarmCommentUpdateMsg));
            }
        }
        if (downlinkMsg.getEntityDataCount() > 0) {
            for (EntityDataProto entityData : downlinkMsg.getEntityDataList()) {
                if (randomFailuresOnTimeseriesDownlink) {
                    if (getRandomBoolean()) {
                        result.add(Futures.immediateFailedFuture(new RuntimeException("Random failure. This is expected error for edge test")));
                    } else {
                        result.add(saveDownlinkMsg(entityData));
                    }
                } else {
                    result.add(saveDownlinkMsg(entityData));
                }
            }
        }
        if (downlinkMsg.getEntityViewUpdateMsgCount() > 0) {
            for (EntityViewUpdateMsg entityViewUpdateMsg : downlinkMsg.getEntityViewUpdateMsgList()) {
                result.add(saveDownlinkMsg(entityViewUpdateMsg));
            }
        }
        if (downlinkMsg.getCustomerUpdateMsgCount() > 0) {
            for (CustomerUpdateMsg customerUpdateMsg : downlinkMsg.getCustomerUpdateMsgList()) {
                result.add(saveDownlinkMsg(customerUpdateMsg));
            }
        }
        if (downlinkMsg.getWidgetsBundleUpdateMsgCount() > 0) {
            for (WidgetsBundleUpdateMsg widgetsBundleUpdateMsg : downlinkMsg.getWidgetsBundleUpdateMsgList()) {
                result.add(saveDownlinkMsg(widgetsBundleUpdateMsg));
            }
        }
        if (downlinkMsg.getWidgetTypeUpdateMsgCount() > 0) {
            for (WidgetTypeUpdateMsg widgetTypeUpdateMsg : downlinkMsg.getWidgetTypeUpdateMsgList()) {
                result.add(saveDownlinkMsg(widgetTypeUpdateMsg));
            }
        }
        if (downlinkMsg.getUserUpdateMsgCount() > 0) {
            for (UserUpdateMsg userUpdateMsg : downlinkMsg.getUserUpdateMsgList()) {
                result.add(saveDownlinkMsg(userUpdateMsg));
            }
        }
        if (downlinkMsg.getUserCredentialsUpdateMsgCount() > 0) {
            for (UserCredentialsUpdateMsg userCredentialsUpdateMsg : downlinkMsg.getUserCredentialsUpdateMsgList()) {
                result.add(saveDownlinkMsg(userCredentialsUpdateMsg));
            }
        }
        if (downlinkMsg.getDeviceRpcCallMsgCount() > 0) {
            for (DeviceRpcCallMsg deviceRpcCallMsg : downlinkMsg.getDeviceRpcCallMsgList()) {
                result.add(saveDownlinkMsg(deviceRpcCallMsg));
            }
        }
        if (downlinkMsg.getDeviceCredentialsRequestMsgCount() > 0) {
            for (DeviceCredentialsRequestMsg deviceCredentialsRequestMsg : downlinkMsg.getDeviceCredentialsRequestMsgList()) {
                result.add(saveDownlinkMsg(deviceCredentialsRequestMsg));
            }
        }
        if (downlinkMsg.getOtaPackageUpdateMsgCount() > 0) {
            for (OtaPackageUpdateMsg otaPackageUpdateMsg : downlinkMsg.getOtaPackageUpdateMsgList()) {
                result.add(saveDownlinkMsg(otaPackageUpdateMsg));
            }
        }
        if (downlinkMsg.getQueueUpdateMsgCount() > 0) {
            for (QueueUpdateMsg queueUpdateMsg : downlinkMsg.getQueueUpdateMsgList()) {
                result.add(saveDownlinkMsg(queueUpdateMsg));
            }
        }
        if (downlinkMsg.getTenantUpdateMsgCount() > 0) {
            for (TenantUpdateMsg tenantUpdateMsg : downlinkMsg.getTenantUpdateMsgList()) {
                result.add(saveDownlinkMsg(tenantUpdateMsg));
            }
        }
        if (downlinkMsg.getTenantProfileUpdateMsgCount() > 0) {
            for (TenantProfileUpdateMsg tenantProfileUpdateMsg : downlinkMsg.getTenantProfileUpdateMsgList()) {
                result.add(saveDownlinkMsg(tenantProfileUpdateMsg));
            }
        }
        if (downlinkMsg.getResourceUpdateMsgCount() > 0) {
            for (ResourceUpdateMsg resourceUpdateMsg : downlinkMsg.getResourceUpdateMsgList()) {
                result.add(saveDownlinkMsg(resourceUpdateMsg));
            }
        }
        if (downlinkMsg.hasEdgeConfiguration()) {
            result.add(saveDownlinkMsg(downlinkMsg.getEdgeConfiguration()));
        }
        if (downlinkMsg.hasSyncCompletedMsg()) {
            result.add(saveDownlinkMsg(downlinkMsg.getSyncCompletedMsg()));
        }

        return Futures.allAsList(result);
    }

    /**
     * 功能：获取`Random Boolean`。
     * 参数：无。
     * 返回：判断结果。
     */
    private boolean getRandomBoolean() {
        double randomValue = ThreadLocalRandom.current().nextDouble() * 100;
        return randomValue <= this.failureProbability;
    }

    /**
     * 功能：保存或创建消息。
     * 参数：
     * - `message`：待处理消息。
     * 返回：匹配的数据集合。
     */
    private ListenableFuture<Void> saveDownlinkMsg(AbstractMessage message) {
        if (!ignoredTypes.contains(message.getClass())) {
            lock.lock();
            try {
                downlinkMsgs.add(message);
            } finally {
                lock.unlock();
            }
            messagesLatch.countDown();
        }
        return Futures.immediateFuture(null);
    }

    /**
     * 功能：执行 `waitForMessages` 对应的处理。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean waitForMessages() throws InterruptedException {
        return waitForMessages(AbstractWebTest.TIMEOUT);
    }

    /**
     * 功能：执行 `waitForMessages` 对应的处理。
     * 参数：
     * - `timeoutInSeconds`：`timeoutInSeconds` 参数。
     * 返回：判断结果。
     */
    public boolean waitForMessages(int timeoutInSeconds) throws InterruptedException {
        return messagesLatch.await(timeoutInSeconds, TimeUnit.SECONDS);
    }

    /**
     * 功能：执行 `expectMessageAmount` 对应的处理。
     * 参数：
     * - `messageAmount`：待处理消息。
     * 返回：无。
     */
    public void expectMessageAmount(int messageAmount) {
        // clear downlinks
        downlinkMsgs.clear();

        messagesLatch = new CountDownLatch(messageAmount);
    }

    /**
     * 功能：执行 `waitForResponses` 对应的处理。
     * 参数：无。
     * 返回：判断结果。
     */
    public boolean waitForResponses() throws InterruptedException {
        return responsesLatch.await(AbstractWebTest.TIMEOUT, TimeUnit.SECONDS);
    }

    /**
     * 功能：执行 `expectResponsesAmount` 对应的处理。
     * 参数：
     * - `messageAmount`：待处理消息。
     * 返回：无。
     */
    public void expectResponsesAmount(int messageAmount) {
        responsesLatch = new CountDownLatch(messageAmount);
    }

    /**
     * 功能：获取消息。
     * 参数：
     * - `tClass`：`tClass` 参数。
     * 返回：可能存在的结果。
     */
    @SuppressWarnings("unchecked")
    public <T extends AbstractMessage> Optional<T> findMessageByType(Class<T> tClass) {
        Optional<T> result;
        lock.lock();
        try {
            result = (Optional<T>) downlinkMsgs.stream().filter(downlinkMsg -> downlinkMsg.getClass().isAssignableFrom(tClass)).findAny();
        } finally {
            lock.unlock();
        }
        return result;
    }

    /**
     * 功能：获取类型。
     * 参数：
     * - `tClass`：`tClass` 参数。
     * 返回：匹配的数据集合。
     */
    @SuppressWarnings("unchecked")
    public <T extends AbstractMessage> List<T> findAllMessagesByType(Class<T> tClass) {
        List<T> result;
        lock.lock();
        try {
            result = (List<T>) downlinkMsgs.stream().filter(downlinkMsg -> downlinkMsg.getClass().isAssignableFrom(tClass)).collect(Collectors.toList());
        } finally {
            lock.unlock();
        }
        return result;
    }

    /**
     * 功能：获取消息。
     * 参数：无。
     * 返回：处理结果。
     */
    public AbstractMessage getLatestMessage() {
        return downlinkMsgs.get(downlinkMsgs.size() - 1);
    }

    /**
     * 功能：执行 `ignoreType` 对应的处理。
     * 参数：
     * - `type`：类型。
     * 返回：无。
     */
    public void ignoreType(Class<? extends AbstractMessage> type) {
        ignoredTypes.add(type);
    }

    /**
     * 功能：执行 `allowIgnoredTypes` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void allowIgnoredTypes() {
        ignoredTypes.clear();
    }

}

/*
 * 本类总结：
 * 1. 核心职责：`EdgeImitator` 在 ThingsBoard Application 测试模块 中承担测试支撑类型职责，核心目的是验证 Application 模块的控制器、服务、Actor 或集成流程。
 * 2. 核心流程：准备输入和依赖，调用被测对象并断言结果。
 * 3. 关键依赖：主要依赖或协作对象包括JUnit、Mockito、Spring Test、DAO、缓存、Transport 和测试夹具。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
