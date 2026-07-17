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
package org.thingsboard.server.service.edge.rpc.constructor.asset;

import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.gen.edge.v1.AssetProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.AssetUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.service.edge.rpc.constructor.MsgConstructor;

/**
 * 中文说明：
 * 1. `AssetMsgConstructor` 是 ThingsBoard Application 中定义资产能力边界的接口。
 * 2. 它声明实现方必须提供的核心操作和输入输出约定。
 * 3. 接口方法共同定义该能力的输入、输出和行为边界。
 * 4. 直接依赖的类型边界包括 `MsgConstructor`。
 * 5. 接口使调用方依赖稳定契约，并允许不同实现按场景替换。
 * 6. 阅读时重点关注方法契约、参数语义和实现类需要保证的行为。
 */
public interface AssetMsgConstructor extends MsgConstructor {

    /**
     * 功能：执行 `constructAssetUpdatedMsg` 对应的处理。
     * 参数：
     * - `msgType`：待处理消息。
     * - `asset`：`asset` 参数。
     * 返回：匹配的数据集合。
     */
    AssetUpdateMsg constructAssetUpdatedMsg(UpdateMsgType msgType, Asset asset);

    /**
     * 功能：执行 `constructAssetDeleteMsg` 对应的处理。
     * 参数：
     * - `assetId`：资产IDID。
     * 返回：匹配的数据集合。
     */
    AssetUpdateMsg constructAssetDeleteMsg(AssetId assetId);

    /**
     * 功能：执行 `constructAssetProfileUpdatedMsg` 对应的处理。
     * 参数：
     * - `msgType`：待处理消息。
     * - `assetProfile`：`assetProfile` 参数。
     * 返回：匹配的数据集合。
     */
    AssetProfileUpdateMsg constructAssetProfileUpdatedMsg(UpdateMsgType msgType, AssetProfile assetProfile);

    /**
     * 功能：执行 `constructAssetProfileDeleteMsg` 对应的处理。
     * 参数：
     * - `assetProfileId`：资产配置ID。
     * 返回：匹配的数据集合。
     */
    AssetProfileUpdateMsg constructAssetProfileDeleteMsg(AssetProfileId assetProfileId);
}
