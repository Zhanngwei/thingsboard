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
package org.thingsboard.rule.engine.telemetry;

import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.adaptor.JsonConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.thingsboard.server.common.data.msg.TbMsgType.POST_TELEMETRY_REQUEST;

/**
 * 中文说明：`TbMsgTimeseriesNode` 是消息时间序列节点规则节点，用于保存、删除或通知属性与时间序列遥测数据。
 * 输入关系：作为规则链节点接收上游节点传入的 `TbMsg`，根据消息体、元数据、发起实体或上下文服务读取所需数据。
 * 输出关系：处理成功时通过 `Success`、`True`、`False` 或其它命名关系把原消息或转换后的消息交给后续节点，实际关系由节点逻辑和配置决定。
 * 失败关系：配置校验、脚本执行、服务调用、数据解析或异步回调异常时通过 `Failure` 关系交给规则链失败分支。
 * 配置对象：`TbMsgTimeseriesNodeConfiguration`，配置内容来自规则节点 JSON，并在 `init` 或父类初始化阶段转换为运行时对象。
 * 调用方和生命周期：Rule Engine 节点运行时创建本节点并调用 `init`，每条消息进入 `onMsg` 或等价处理方法，`destroy` 负责释放脚本引擎、缓存、监听器等资源。
 */
@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "save timeseries",
        configClazz = TbMsgTimeseriesNodeConfiguration.class,
        nodeDescription = "Saves timeseries data",
        nodeDetails = "Saves timeseries telemetry data based on configurable TTL parameter. Expects messages with 'POST_TELEMETRY_REQUEST' message type. " +
                "Timestamp in milliseconds will be taken from metadata.ts, otherwise 'now' message timestamp will be applied. " +
                "Allows stopping updating values for incoming keys in the latest ts_kv table if 'skipLatestPersistence' is set to true.\n " +
                "<br/>" +
                "Enable 'useServerTs' param to use the timestamp of the message processing instead of the timestamp from the message. " +
                "Useful for all sorts of sequential processing if you merge messages from multiple sources (devices, assets, etc).\n" +
                "<br/>" +
                "In the case of sequential processing, the platform guarantees that the messages are processed in the order of their submission to the queue. " +
                "However, the timestamp of the messages originated by multiple devices/servers may be unsynchronized long before they are pushed to the queue. " +
                "The DB layer has certain optimizations to ignore the updates of the \"attributes\" and \"latest values\" tables if the new record has a timestamp that is older than the previous record. " +
                "So, to make sure that all the messages will be processed correctly, one should enable this parameter for sequential message processing scenarios.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeTimeseriesConfig",
        icon = "file_upload"
)
public class TbMsgTimeseriesNode implements TbNode {

    /**
     * 配置，保存当前对象的配置选项。
     */
    private TbMsgTimeseriesNodeConfiguration config;
    /**
     * 上下文，汇总当前处理所需的上下文信息。
     */
    private TbContext ctx;
    /**
     * 租户对象，用于描述当前业务场景。
     */
    private long tenantProfileDefaultStorageTtl;

    /**
     * 功能：执行 `init` 对应的处理。
     * 参数：
     * - `ctx`：处理上下文。
     * - `configuration`：配置对象。
     * 返回：无。
     */
    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbMsgTimeseriesNodeConfiguration.class);
        this.ctx = ctx;
        ctx.addTenantProfileListener(this::onTenantProfileUpdate);
        onTenantProfileUpdate(ctx.getTenantProfile());
    }

    /**
     * 功能：处理租户。
     * 参数：
     * - `tenantProfile`：租户信息或租户标识。
     * 返回：无。
     */
    void onTenantProfileUpdate(TenantProfile tenantProfile) {
        DefaultTenantProfileConfiguration configuration = (DefaultTenantProfileConfiguration) tenantProfile.getProfileData().getConfiguration();
        tenantProfileDefaultStorageTtl = TimeUnit.DAYS.toSeconds(configuration.getDefaultStorageTtlDays());
    }

    /**
     * 功能：处理消息。
     * 参数：
     * - `ctx`：处理上下文。
     * - `msg`：待处理消息。
     * 返回：无。
     */
    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        if (!msg.isTypeOf(POST_TELEMETRY_REQUEST)) {
            ctx.tellFailure(msg, new IllegalArgumentException("Unsupported msg type: " + msg.getType()));
            return;
        }
        long ts = computeTs(msg, config.isUseServerTs());
        String src = msg.getData();
        Map<Long, List<KvEntry>> tsKvMap = JsonConverter.convertToTelemetry(JsonParser.parseString(src), ts);
        if (tsKvMap.isEmpty()) {
            ctx.tellFailure(msg, new IllegalArgumentException("Msg body is empty: " + src));
            return;
        }
        List<TsKvEntry> tsKvEntryList = new ArrayList<>();
        for (Map.Entry<Long, List<KvEntry>> tsKvEntry : tsKvMap.entrySet()) {
            for (KvEntry kvEntry : tsKvEntry.getValue()) {
                tsKvEntryList.add(new BasicTsKvEntry(tsKvEntry.getKey(), kvEntry));
            }
        }
        String ttlValue = msg.getMetaData().getValue("TTL");
        long ttl = !StringUtils.isEmpty(ttlValue) ? Long.parseLong(ttlValue) : config.getDefaultTTL();
        if (ttl == 0L) {
            ttl = tenantProfileDefaultStorageTtl;
        }
        if (config.isSkipLatestPersistence()) {
            ctx.getTelemetryService().saveWithoutLatestAndNotify(ctx.getTenantId(), msg.getCustomerId(), msg.getOriginator(), tsKvEntryList, ttl, new TelemetryNodeCallback(ctx, msg));
        } else {
            ctx.getTelemetryService().saveAndNotify(ctx.getTenantId(), msg.getCustomerId(), msg.getOriginator(), tsKvEntryList, ttl, new TelemetryNodeCallback(ctx, msg));
        }
    }

    /**
     * 功能：执行 `computeTs` 对应的处理。
     * 参数：
     * - `msg`：待处理消息。
     * - `ignoreMetadataTs`：时间戳。
     * 返回：数值结果。
     */
    public static long computeTs(TbMsg msg, boolean ignoreMetadataTs) {
        return ignoreMetadataTs ? System.currentTimeMillis() : msg.getMetaDataTs();
    }

    /**
     * 功能：执行 `destroy` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void destroy() {
        ctx.removeListeners();
    }

    /*
     * 本类总结：`TbMsgTimeseriesNode` 负责保存、删除或通知属性与时间序列遥测数据；作为节点时遵循 Rule Engine 的输入、输出、失败和生命周期约定，作为配置或 helper 时仅承载对应数据和辅助逻辑。
     * 数据库、缓存、MQTT、Actor 与事务边界以具体方法说明为准；本类或方法本身未直接涉及时，相关行为可能仅存在于具体实现或调用链中。
     */
}
