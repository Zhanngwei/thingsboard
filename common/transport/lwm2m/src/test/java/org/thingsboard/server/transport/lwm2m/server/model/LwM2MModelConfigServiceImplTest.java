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
package org.thingsboard.server.transport.lwm2m.server.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.transport.lwm2m.server.store.TbLwM2MModelConfigStore;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;

/**
 * 中文说明：
 * 1. `LwM2MModelConfigServiceImplTest` 是 ThingsBoard Common Transport 中验证 `LwM2MModelConfigServiceImpl` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 它直接协作于被测类型、测试框架和必要的模拟依赖。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
class LwM2MModelConfigServiceImplTest {

    /**
     * 服务，提供当前类调用的业务操作。
     */
    LwM2MModelConfigServiceImpl service;
    TbLwM2MModelConfigStore modelStore;

    /**
     * 功能：初始化当前测试或组件需要的对象。
     * 参数：无。
     * 返回：无。
     */
    @BeforeEach
    void setUp() {
        service = new LwM2MModelConfigServiceImpl();
        modelStore = mock(TbLwM2MModelConfigStore.class);
        service.modelStore = modelStore;
    }

    /**
     * 功能：验证`Init With Duplicated Models`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void testInitWithDuplicatedModels() {
        LwM2MModelConfig config = new LwM2MModelConfig("urn:imei:951358811362976");
        List<LwM2MModelConfig> models = List.of(config, config);
        willReturn(models).given(modelStore).getAll();
        service.init();
        assertThat(service.currentModelConfigs).containsExactlyEntriesOf(Map.of(config.getEndpoint(), config));
    }

    /**
     * 功能：验证`Init With Non Unique Endpoints`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void testInitWithNonUniqueEndpoints() {
        LwM2MModelConfig configAlfa = new LwM2MModelConfig("urn:imei:951358811362976");
        LwM2MModelConfig configBravo = new LwM2MModelConfig("urn:imei:151358811362976");
        LwM2MModelConfig configDelta = new LwM2MModelConfig("urn:imei:151358811362976");
        assertThat(configBravo.getEndpoint()).as("non-unique endpoints provided").isEqualTo(configDelta.getEndpoint());
        List<LwM2MModelConfig> models = List.of(configAlfa, configBravo, configDelta);
        willReturn(models).given(modelStore).getAll();
        service.init();
        assertThat(service.currentModelConfigs).containsExactlyInAnyOrderEntriesOf(Map.of(
                configAlfa.getEndpoint(), configAlfa,
                configBravo.getEndpoint(), configBravo
        ));
    }

    /**
     * 功能：验证`Init With Empty Models`相关场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void testInitWithEmptyModels() {
        willReturn(Collections.emptyList()).given(modelStore).getAll();
        service.init();
        assertThat(service.currentModelConfigs).isEmpty();
    }

}
