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
package org.thingsboard.server.utils;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.Charset;


/**
 * @author Andrew Shvayka
 */
/**
 * 中文说明：
 * 1. `MiscUtils` 是 ThingsBoard Application 中处理 `Misc Utils` 通用操作的工具类型。
 * 2. 它提供无状态或轻量的复用方法，减少多个调用点的重复实现。
 * 3. 方法通常完成格式化、校验、计算或简单对象构造。
 * 4. 它直接协作于方法参数和返回值所代表的数据类型。
 * 5. 集中工具方法可以统一边界行为，并降低细节变化对调用方的影响。
 * 6. 阅读时重点关注输入约束、边界值和方法是否修改传入对象。
 */
public class MiscUtils {

    public static final Charset UTF8 = Charset.forName("UTF-8");

    /**
     * 功能：执行 `missingProperty` 对应的处理。
     * 参数：
     * - `propertyName`：名称。
     * 返回：文本结果。
     */
    public static String missingProperty(String propertyName) {
        return "The " + propertyName + " property need to be set!";
    }

    /**
     * 功能：执行 `forName` 对应的处理。
     * 参数：
     * - `name`：名称。
     * 返回：处理结果。
     */
    @SuppressWarnings("deprecation")
    public static HashFunction forName(String name) {
        switch (name) {
            case "murmur3_32":
                return Hashing.murmur3_32();
            case "murmur3_128":
                return Hashing.murmur3_128();
            case "crc32":
                return Hashing.crc32();
            case "md5":
                return Hashing.md5();
            default:
                throw new IllegalArgumentException("Can't find hash function with name " + name);
        }
    }

    /**
     * 功能：执行 `constructBaseUrl` 对应的处理。
     * 参数：
     * - `request`：请求对象。
     * 返回：文本结果。
     */
    public static String constructBaseUrl(HttpServletRequest request) {
        return String.format("%s://%s:%d",
                getScheme(request),
                getDomainName(request),
                getPort(request));
    }

    /**
     * 功能：获取`Scheme`。
     * 参数：
     * - `request`：请求对象。
     * 返回：文本结果。
     */
    public static String getScheme(HttpServletRequest request){
        String scheme = request.getScheme();
        String forwardedProto = request.getHeader("x-forwarded-proto");
        if (forwardedProto != null) {
            scheme = forwardedProto;
        }
        return scheme;
    }

    /**
     * 功能：获取名称。
     * 参数：
     * - `request`：请求对象。
     * 返回：文本结果。
     */
    public static String getDomainName(HttpServletRequest request){
        return request.getServerName();
    }

    /**
     * 功能：获取端口号。
     * 参数：
     * - `request`：请求对象。
     * 返回：文本结果。
     */
    public static String getDomainNameAndPort(HttpServletRequest request){
        String domainName = getDomainName(request);
        String scheme = getScheme(request);
        int port = MiscUtils.getPort(request);
        if (needsPort(scheme, port)) {
            domainName += ":" + port;
        }
        return domainName;
    }

    /**
     * 功能：执行 `needsPort` 对应的处理。
     * 参数：
     * - `scheme`：`scheme` 参数。
     * - `port`：`port` 参数。
     * 返回：判断结果。
     */
    private static boolean needsPort(String scheme, int port) {
        boolean isHttpDefault = "http".equals(scheme.toLowerCase()) && port == 80;
        boolean isHttpsDefault = "https".equals(scheme.toLowerCase()) && port == 443;
        return !isHttpDefault && !isHttpsDefault;
    }

    /**
     * 功能：获取端口号。
     * 参数：
     * - `request`：请求对象。
     * 返回：数值结果。
     */
    public static int getPort(HttpServletRequest request){
        String forwardedProto = request.getHeader("x-forwarded-proto");

        int serverPort = request.getServerPort();
        if (request.getHeader("x-forwarded-port") != null) {
            try {
                serverPort = request.getIntHeader("x-forwarded-port");
            } catch (NumberFormatException e) {
            }
        } else if (forwardedProto != null) {
            switch (forwardedProto) {
                case "http":
                    serverPort = 80;
                    break;
                case "https":
                    serverPort = 443;
                    break;
            }
        }
        return serverPort;
    }
}
