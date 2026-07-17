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
package org.thingsboard.server.cache;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootContextLoader;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 中文说明：
 * 1. `CaffeineCacheDefaultConfigurationTest` 是 ThingsBoard Application 中验证 `CaffeineCacheDefaultConfiguration` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 它直接协作于被测类型、测试框架和必要的模拟依赖。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = CaffeineCacheDefaultConfigurationTest.class, loader = SpringBootContextLoader.class)
@ComponentScan({"org.thingsboard.server.cache"})
@EnableConfigurationProperties
@Slf4j
public class CaffeineCacheDefaultConfigurationTest {

    /**
     * `cacheSpecsMap`映射关系，用于按键查找对应值。
     */
    @Autowired
    CacheSpecsMap cacheSpecsMap;

    /**
     * 功能：校验管理器。
     * 参数：无。
     * 返回：无。
     */
    @Test
    public void verifyTransactionAwareCacheManagerProxy() {
        assertThat(cacheSpecsMap.getSpecs()).as("specs").isNotNull();
        cacheSpecsMap.getSpecs().forEach((name, cacheSpecs)->assertThat(cacheSpecs).as("cache %s specs", name).isNotNull());

        SoftAssertions softly = new SoftAssertions();
        cacheSpecsMap.getSpecs().forEach((name, cacheSpecs)->{
            softly.assertThat(name).as("cache name").isNotEmpty();
            softly.assertThat(cacheSpecs.getTimeToLiveInMinutes()).as("cache %s time to live", name).isGreaterThan(0);
            softly.assertThat(cacheSpecs.getMaxSize()).as("cache %s max size", name).isGreaterThan(0);
        });
        softly.assertAll();
    }

}
