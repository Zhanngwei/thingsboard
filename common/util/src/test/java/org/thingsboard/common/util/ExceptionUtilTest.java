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
package org.thingsboard.common.util;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 中文说明：
 * 1. `ExceptionUtilTest` 是 ThingsBoard Common 中验证 `ExceptionUtil` 相关行为的测试类型。
 * 2. 它通过测试夹具构造输入，并执行被测类型的关键入口。
 * 3. 测试方法用准备数据、执行步骤和预期结果描述需要保持的行为。
 * 4. 它直接协作于被测类型、测试框架和必要的模拟依赖。
 * 5. 独立测试类型用于固定当前行为，防止后续修改造成回归。
 * 6. 阅读时重点关注测试方法名称中的场景、准备数据和最终断言。
 */
class ExceptionUtilTest {

    final Exception cause = new RuntimeException();

    /**
     * 功能：验证 `givenRootCause_whenLookupExceptionInCause_thenReturnRootCauseAndNoStackOverflow` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenRootCause_whenLookupExceptionInCause_thenReturnRootCauseAndNoStackOverflow() {
        Exception e = cause;
        for (int i = 0; i <= 16384; i++) {
            e = new Exception(e);
        }
        assertThat(ExceptionUtil.lookupExceptionInCause(e, RuntimeException.class)).isSameAs(cause);
    }

    /**
     * 功能：验证 `givenCause_whenLookupExceptionInCause_thenReturnCause` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenCause_whenLookupExceptionInCause_thenReturnCause() {
        assertThat(ExceptionUtil.lookupExceptionInCause(new Exception(cause), RuntimeException.class)).isSameAs(cause);
    }

    /**
     * 功能：验证 `givenNoCauseAndExceptionIsWantedCauseClass_whenLookupExceptionInCause_thenReturnSelf` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenNoCauseAndExceptionIsWantedCauseClass_whenLookupExceptionInCause_thenReturnSelf() {
        assertThat(ExceptionUtil.lookupExceptionInCause(cause, RuntimeException.class)).isSameAs(cause);
    }

    /**
     * 功能：验证 `givenNoCause_whenLookupExceptionInCause_thenReturnNull` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenNoCause_whenLookupExceptionInCause_thenReturnNull() {
        assertThat(ExceptionUtil.lookupExceptionInCause(new Exception(), RuntimeException.class)).isNull();
    }

    /**
     * 功能：验证 `givenNotWantedCause_whenLookupExceptionInCause_thenReturnNull` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenNotWantedCause_whenLookupExceptionInCause_thenReturnNull() {
        final Exception cause = new IOException();
        assertThat(ExceptionUtil.lookupExceptionInCause(new Exception(cause), RuntimeException.class)).isNull();
    }

    /**
     * 功能：验证 `givenCause_whenLookupExceptionInCauseByMany_thenReturnFirstCause` 描述的测试场景。
     * 参数：无。
     * 返回：无。
     */
    @Test
    void givenCause_whenLookupExceptionInCauseByMany_thenReturnFirstCause() {
        final Exception causeIAE = new IllegalAccessException();
        assertThat(ExceptionUtil.lookupExceptionInCause(new Exception(causeIAE))).isNull();
        assertThat(ExceptionUtil.lookupExceptionInCause(new Exception(causeIAE), IOException.class, NoSuchFieldException.class)).isNull();
        assertThat(ExceptionUtil.lookupExceptionInCause(new Exception(causeIAE), IllegalAccessException.class, IOException.class, NoSuchFieldException.class)).isSameAs(causeIAE);
        assertThat(ExceptionUtil.lookupExceptionInCause(new Exception(causeIAE), IOException.class, NoSuchFieldException.class, IllegalAccessException.class)).isSameAs(causeIAE);

        final Exception causeIOE = new IOException(causeIAE);
        assertThat(ExceptionUtil.lookupExceptionInCause(new Exception(causeIOE))).isNull();
        assertThat(ExceptionUtil.lookupExceptionInCause(new Exception(causeIAE), ClassNotFoundException.class, NoSuchFieldException.class)).isNull();
        assertThat(ExceptionUtil.lookupExceptionInCause(new Exception(causeIOE), IOException.class, NoSuchFieldException.class)).isSameAs(causeIOE);
        assertThat(ExceptionUtil.lookupExceptionInCause(new Exception(causeIOE), IllegalAccessException.class, IOException.class, NoSuchFieldException.class)).isSameAs(causeIOE);
        assertThat(ExceptionUtil.lookupExceptionInCause(new Exception(causeIOE), IOException.class, NoSuchFieldException.class, IllegalAccessException.class)).isSameAs(causeIOE);
    }

}
