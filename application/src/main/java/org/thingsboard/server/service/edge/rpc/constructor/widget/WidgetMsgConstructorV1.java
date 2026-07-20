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
package org.thingsboard.server.service.edge.rpc.constructor.widget;

import com.google.protobuf.ByteString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.resource.ImageService;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.WidgetTypeUpdateMsg;
import org.thingsboard.server.gen.edge.v1.WidgetsBundleUpdateMsg;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.rpc.utils.EdgeVersionUtils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * 中文说明：
 * 1. `WidgetMsgConstructorV1` 是 ThingsBoard Application 中创建或提供部件对象的构造组件。
 * 2. 它根据输入配置、类型或上下文选择合适的具体实现。
 * 3. 创建细节被集中在该类型中，调用方只依赖稳定的创建入口。
 * 4. 直接依赖的类型边界包括 `BaseWidgetMsgConstructor`。
 * 5. 独立工厂可以避免调用方了解构造顺序和实现类选择规则。
 * 6. 阅读时重点关注实现选择条件、默认分支和对象初始化参数。
 */
@Component
@TbCoreComponent
public class WidgetMsgConstructorV1 extends BaseWidgetMsgConstructor {

    /**
     * 服务，提供当前类调用的业务操作。
     */
    @Autowired
    private ImageService imageService;

    /**
     * 功能：执行 `constructWidgetsBundleUpdateMsg` 对应的处理。
     * 参数：
     * - `msgType`：待处理消息。
     * - `widgetsBundle`：`widgetsBundle` 参数。
     * - `widgets`：数据列表。
     * 返回：处理结果。
     */
    @Override
    public WidgetsBundleUpdateMsg constructWidgetsBundleUpdateMsg(UpdateMsgType msgType, WidgetsBundle widgetsBundle, List<String> widgets) {
        widgetsBundle = JacksonUtil.clone(widgetsBundle);
        imageService.inlineImageForEdge(widgetsBundle);
        WidgetsBundleUpdateMsg.Builder builder = WidgetsBundleUpdateMsg.newBuilder()
                .setMsgType(msgType)
                .setIdMSB(widgetsBundle.getId().getId().getMostSignificantBits())
                .setIdLSB(widgetsBundle.getId().getId().getLeastSignificantBits())
                .setTitle(widgetsBundle.getTitle())
                .setAlias(widgetsBundle.getAlias());
        if (widgetsBundle.getImage() != null) {
            builder.setImage(ByteString.copyFrom(widgetsBundle.getImage().getBytes(StandardCharsets.UTF_8)));
        }
        if (widgetsBundle.getDescription() != null) {
            builder.setDescription(widgetsBundle.getDescription());
        }
        if (widgetsBundle.getOrder() != null) {
            builder.setOrder(widgetsBundle.getOrder());
        }
        if (widgetsBundle.getTenantId().equals(TenantId.SYS_TENANT_ID)) {
            builder.setIsSystem(true);
        }
        builder.setWidgets(JacksonUtil.toString(widgets));
        return builder.build();
    }

    /**
     * 功能：执行 `constructWidgetTypeUpdateMsg` 对应的处理。
     * 参数：
     * - `msgType`：待处理消息。
     * - `widgetTypeDetails`：类型。
     * - `edgeVersion`：`edgeVersion` 参数。
     * 返回：处理结果。
     */
    @Override
    public WidgetTypeUpdateMsg constructWidgetTypeUpdateMsg(UpdateMsgType msgType, WidgetTypeDetails widgetTypeDetails, EdgeVersion edgeVersion) {
        widgetTypeDetails = JacksonUtil.clone(widgetTypeDetails);
        imageService.inlineImagesForEdge(widgetTypeDetails);
        WidgetTypeUpdateMsg.Builder builder = WidgetTypeUpdateMsg.newBuilder()
                .setMsgType(msgType)
                .setIdMSB(widgetTypeDetails.getId().getId().getMostSignificantBits())
                .setIdLSB(widgetTypeDetails.getId().getId().getLeastSignificantBits());
        if (widgetTypeDetails.getFqn() != null) {
            builder.setFqn(widgetTypeDetails.getFqn());
            if (widgetTypeDetails.getFqn().contains(".")) {
                String[] aliases = widgetTypeDetails.getFqn().split("\\.", 2);
                if (aliases.length == 2) {
                    builder.setBundleAlias(aliases[0]);
                    builder.setAlias(aliases[1]);
                }
            }
        }
        if (widgetTypeDetails.getName() != null) {
            builder.setName(widgetTypeDetails.getName());
        }
        if (widgetTypeDetails.getDescriptor() != null) {
            builder.setDescriptorJson(JacksonUtil.toString(widgetTypeDetails.getDescriptor()));
        }
        if (widgetTypeDetails.getTenantId().equals(TenantId.SYS_TENANT_ID)) {
            builder.setIsSystem(true);
        }
        if (widgetTypeDetails.getImage() != null) {
            builder.setImage(widgetTypeDetails.getImage());
        }
        if (widgetTypeDetails.getDescription() != null) {
            if (EdgeVersionUtils.isEdgeVersionOlderThan(edgeVersion, EdgeVersion.V_3_6_0) &&
                    widgetTypeDetails.getDescription().length() > 255) {
                builder.setDescription(widgetTypeDetails.getDescription().substring(0, 254));
            } else {
                builder.setDescription(widgetTypeDetails.getDescription());
            }
        }
        builder.setDeprecated(widgetTypeDetails.isDeprecated());
        if (widgetTypeDetails.getTags() != null) {
            builder.addAllTags(Arrays.asList(widgetTypeDetails.getTags()));
        }
        return builder.build();
    }

}
