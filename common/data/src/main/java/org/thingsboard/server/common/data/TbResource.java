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
package org.thingsboard.server.common.data;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.TbResourceId;

import java.util.Base64;
import java.util.Optional;

/**
 * 中文说明：
 * 1. 类目的：`TbResource` 是ThingsBoard Common 模块中的公共数据模型类型，用于承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 所属模块：位于 common 聚合模块，支撑服务端启动、Web API、Actor、队列、传输层、公共数据契约或业务服务流程。
 * 3. 协作对象：主要协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 生命周期：通常由 REST 请求、DAO 查询、消息反序列化、配置加载或测试夹具创建，并随单次业务流程传递。
 * 5. 设计原因：单独建模该类型可以隔离职责边界，避免 Controller、Service、DAO、Actor 或测试夹具之间直接耦合。
 * 6. 技术关联：是否涉及事务、缓存、MQTT、Actor、数据库和 Rule Engine 取决于调用链；本注释用于标明该类型在链路中的直接或间接位置。
 * 7. 设计模式：主要体现 DTO / Value Object / Builder。
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class TbResource extends TbResourceInfo {

    /**
     * 版本号常量，用于统一引用固定值。
     */
    private static final long serialVersionUID = 7379609705527272306L;

    /**
     * 数据列表，用于保存一组待处理对象。
     */
    private byte[] data;

    /**
     * `preview`列表，用于保存一组待处理对象。
     */
    private byte[] preview;

    /**
     * 功能：创建 `TbResource` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public TbResource() {
        super();
    }

    /**
     * 功能：创建 `TbResource` 实例，并初始化必要字段。
     * 参数：
     * - `id`：`id`ID。
     * 返回：新创建的对象实例。
     */
    public TbResource(TbResourceId id) {
        super(id);
    }

    /**
     * 功能：创建 `TbResource` 实例，并初始化必要字段。
     * 参数：
     * - `resourceInfo`：`resourceInfo` 参数。
     * 返回：新创建的对象实例。
     */
    public TbResource(TbResourceInfo resourceInfo) {
        super(resourceInfo);
    }

    /**
     * 功能：创建 `TbResource` 实例，并初始化必要字段。
     * 参数：
     * - `resource`：`resource` 参数。
     * 返回：新创建的对象实例。
     */
    public TbResource(TbResource resource) {
        super(resource);
        this.data = resource.data;
        this.preview = resource.preview;
    }

    /**
     * 功能：获取数据。
     * 参数：无。
     * 返回：文本结果。
     */
    @ApiModelProperty(position = 12, value = "Resource data.", example = "77u/PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPCEtLQpGSUxFIElORk9STUFUSU9OCgpPTUEgUGVybWFuZW50IERvY3VtZW50CiAgIEZpbGU6IE9NQS1TVVAtTHdNMk1fQmluYXJ5QXBwRGF0YUNvbnRhaW5lci1WMV8wXzEtMjAxOTAyMjEtQQogICBUeXBlOiB4bWwKClB1YmxpYyBSZWFjaGFibGUgSW5mb3JtYXRpb24KICAgUGF0aDogaHR0cDovL3d3dy5vcGVubW9iaWxlYWxsaWFuY2Uub3JnL3RlY2gvcHJvZmlsZXMKICAgTmFtZTogTHdNMk1fQmluYXJ5QXBwRGF0YUNvbnRhaW5lci12MV8wXzEueG1sCgpOT1JNQVRJVkUgSU5GT1JNQVRJT04KCiAgSW5mb3JtYXRpb24gYWJvdXQgdGhpcyBmaWxlIGNhbiBiZSBmb3VuZCBpbiB0aGUgbGF0ZXN0IHJldmlzaW9uIG9mCgogIE9NQS1UUy1MV00yTV9CaW5hcnlBcHBEYXRhQ29udGFpbmVyLVYxXzBfMQoKICBUaGlzIGlzIGF2YWlsYWJsZSBhdCBodHRwOi8vd3d3Lm9wZW5tb2JpbGVhbGxpYW5jZS5vcmcvCgogIFNlbmQgY29tbWVudHMgdG8gaHR0cHM6Ly9naXRodWIuY29tL09wZW5Nb2JpbGVBbGxpYW5jZS9PTUFfTHdNMk1fZm9yX0RldmVsb3BlcnMvaXNzdWVzCgpDSEFOR0UgSElTVE9SWQoKMTUwNjIwMTggU3RhdHVzIGNoYW5nZWQgdG8gQXBwcm92ZWQgYnkgRE0sIERvYyBSZWYgIyBPTUEtRE0mU0UtMjAxOC0wMDYxLUlOUF9MV00yTV9BUFBEQVRBX1YxXzBfRVJQX2Zvcl9maW5hbF9BcHByb3ZhbAoyMTAyMjAxOSBTdGF0dXMgY2hhbmdlZCB0byBBcHByb3ZlZCBieSBJUFNPLCBEb2MgUmVmICMgT01BLUlQU08tMjAxOS0wMDI1LUlOUF9Md00yTV9PYmplY3RfQXBwX0RhdGFfQ29udGFpbmVyXzFfMF8xX2Zvcl9GaW5hbF9BcHByb3ZhbAoKTEVHQUwgRElTQ0xBSU1FUgoKQ29weXJpZ2h0IDIwMTkgT3BlbiBNb2JpbGUgQWxsaWFuY2UuCgpSZWRpc3RyaWJ1dGlvbiBhbmQgdXNlIGluIHNvdXJjZSBhbmQgYmluYXJ5IGZvcm1zLCB3aXRoIG9yIHdpdGhvdXQKbW9kaWZpY2F0aW9uLCBhcmUgcGVybWl0dGVkIHByb3ZpZGVkIHRoYXQgdGhlIGZvbGxvd2luZyBjb25kaXRpb25zCmFyZSBtZXQ6CgoxLiBSZWRpc3RyaWJ1dGlvbnMgb2Ygc291cmNlIGNvZGUgbXVzdCByZXRhaW4gdGhlIGFib3ZlIGNvcHlyaWdodApub3RpY2UsIHRoaXMgbGlzdCBvZiBjb25kaXRpb25zIGFuZCB0aGUgZm9sbG93aW5nIGRpc2NsYWltZXIuCjIuIFJlZGlzdHJpYnV0aW9ucyBpbiBiaW5hcnkgZm9ybSBtdXN0IHJlcHJvZHVjZSB0aGUgYWJvdmUgY29weXJpZ2h0Cm5vdGljZSwgdGhpcyBsaXN0IG9mIGNvbmRpdGlvbnMgYW5kIHRoZSBmb2xsb3dpbmcgZGlzY2xhaW1lciBpbiB0aGUKZG9jdW1lbnRhdGlvbiBhbmQvb3Igb3RoZXIgbWF0ZXJpYWxzIHByb3ZpZGVkIHdpdGggdGhlIGRpc3RyaWJ1dGlvbi4KMy4gTmVpdGhlciB0aGUgbmFtZSBvZiB0aGUgY29weXJpZ2h0IGhvbGRlciBub3IgdGhlIG5hbWVzIG9mIGl0cwpjb250cmlidXRvcnMgbWF5IGJlIHVzZWQgdG8gZW5kb3JzZSBvciBwcm9tb3RlIHByb2R1Y3RzIGRlcml2ZWQKZnJvbSB0aGlzIHNvZnR3YXJlIHdpdGhvdXQgc3BlY2lmaWMgcHJpb3Igd3JpdHRlbiBwZXJtaXNzaW9uLgoKVEhJUyBTT0ZUV0FSRSBJUyBQUk9WSURFRCBCWSBUSEUgQ09QWVJJR0hUIEhPTERFUlMgQU5EIENPTlRSSUJVVE9SUwoiQVMgSVMiIEFORCBBTlkgRVhQUkVTUyBPUiBJTVBMSUVEIFdBUlJBTlRJRVMsIElOQ0xVRElORywgQlVUIE5PVApMSU1JVEVEIFRPLCBUSEUgSU1QTElFRCBXQVJSQU5USUVTIE9GIE1FUkNIQU5UQUJJTElUWSBBTkQgRklUTkVTUwpGT1IgQSBQQVJUSUNVTEFSIFBVUlBPU0UgQVJFIERJU0NMQUlNRUQuIElOIE5PIEVWRU5UIFNIQUxMIFRIRQpDT1BZUklHSFQgSE9MREVSIE9SIENPTlRSSUJVVE9SUyBCRSBMSUFCTEUgRk9SIEFOWSBESVJFQ1QsIElORElSRUNULApJTkNJREVOVEFMLCBTUEVDSUFMLCBFWEVNUExBUlksIE9SIENPTlNFUVVFTlRJQUwgREFNQUdFUyAoSU5DTFVESU5HLApCVVQgTk9UIExJTUlURUQgVE8sIFBST0NVUkVNRU5UIE9GIFNVQlNUSVRVVEUgR09PRFMgT1IgU0VSVklDRVM7CkxPU1MgT0YgVVNFLCBEQVRBLCBPUiBQUk9GSVRTOyBPUiBCVVNJTkVTUyBJTlRFUlJVUFRJT04pIEhPV0VWRVIKQ0FVU0VEIEFORCBPTiBBTlkgVEhFT1JZIE9GIExJQUJJTElUWSwgV0hFVEhFUiBJTiBDT05UUkFDVCwgU1RSSUNUCkxJQUJJTElUWSwgT1IgVE9SVCAoSU5DTFVESU5HIE5FR0xJR0VOQ0UgT1IgT1RIRVJXSVNFKSBBUklTSU5HIElOCkFOWSBXQVkgT1VUIE9GIFRIRSBVU0UgT0YgVEhJUyBTT0ZUV0FSRSwgRVZFTiBJRiBBRFZJU0VEIE9GIFRIRQpQT1NTSUJJTElUWSBPRiBTVUNIIERBTUFHRS4KClRoZSBhYm92ZSBsaWNlbnNlIGlzIHVzZWQgYXMgYSBsaWNlbnNlIHVuZGVyIGNvcHlyaWdodCBvbmx5LiBQbGVhc2UKcmVmZXJlbmNlIHRoZSBPTUEgSVBSIFBvbGljeSBmb3IgcGF0ZW50IGxpY2Vuc2luZyB0ZXJtczoKaHR0cHM6Ly93d3cub21hc3BlY3dvcmtzLm9yZy9hYm91dC9pbnRlbGxlY3R1YWwtcHJvcGVydHktcmlnaHRzLwoKLS0+CjxMV00yTSB4bWxuczp4c2k9Imh0dHA6Ly93d3cudzMub3JnLzIwMDEvWE1MU2NoZW1hLWluc3RhbmNlIiB4c2k6bm9OYW1lc3BhY2VTY2hlbWFMb2NhdGlvbj0iaHR0cDovL29wZW5tb2JpbGVhbGxpYW5jZS5vcmcvdGVjaC9wcm9maWxlcy9MV00yTS54c2QiPgoJPE9iamVjdCBPYmplY3RUeXBlPSJNT0RlZmluaXRpb24iPgoJCTxOYW1lPkJpbmFyeUFwcERhdGFDb250YWluZXI8L05hbWU+CgkJPERlc2NyaXB0aW9uMT48IVtDREFUQVtUaGlzIEx3TTJNIE9iamVjdHMgcHJvdmlkZXMgdGhlIGFwcGxpY2F0aW9uIHNlcnZpY2UgZGF0YSByZWxhdGVkIHRvIGEgTHdNMk0gU2VydmVyLCBlZy4gV2F0ZXIgbWV0ZXIgZGF0YS4gClRoZXJlIGFyZSBzZXZlcmFsIG1ldGhvZHMgdG8gY3JlYXRlIGluc3RhbmNlIHRvIGluZGljYXRlIHRoZSBtZXNzYWdlIGRpcmVjdGlvbiBiYXNlZCBvbiB0aGUgbmVnb3RpYXRpb24gYmV0d2VlbiBBcHBsaWNhdGlvbiBhbmQgTHdNMk0uIFRoZSBDbGllbnQgYW5kIFNlcnZlciBzaG91bGQgbmVnb3RpYXRlIHRoZSBpbnN0YW5jZShzKSB1c2VkIHRvIGV4Y2hhbmdlIHRoZSBkYXRhLiBGb3IgZXhhbXBsZToKIC0gVXNpbmcgYSBzaW5nbGUgaW5zdGFuY2UgZm9yIGJvdGggZGlyZWN0aW9ucyBjb21tdW5pY2F0aW9uLCBmcm9tIENsaWVudCB0byBTZXJ2ZXIgYW5kIGZyb20gU2VydmVyIHRvIENsaWVudC4KIC0gVXNpbmcgYW4gaW5zdGFuY2UgZm9yIGNvbW11bmljYXRpb24gZnJvbSBDbGllbnQgdG8gU2VydmVyIGFuZCBhbm90aGVyIG9uZSBmb3IgY29tbXVuaWNhdGlvbiBmcm9tIFNlcnZlciB0byBDbGllbnQKIC0gVXNpbmcgc2V2ZXJhbCBpbnN0YW5jZXMKXV0+PC9EZXNjcmlwdGlvbjE+CgkJPE9iamVjdElEPjE5PC9PYmplY3RJRD4KCQk8T2JqZWN0VVJOPnVybjpvbWE6bHdtMm06b21hOjE5PC9PYmplY3RVUk4+CgkJPExXTTJNVmVyc2lvbj4xLjA8L0xXTTJNVmVyc2lvbj4KCQk8T2JqZWN0VmVyc2lvbj4xLjA8L09iamVjdFZlcnNpb24+CgkJPE11bHRpcGxlSW5zdGFuY2VzPk11bHRpcGxlPC9NdWx0aXBsZUluc3RhbmNlcz4KCQk8TWFuZGF0b3J5Pk9wdGlvbmFsPC9NYW5kYXRvcnk+CgkJPFJlc291cmNlcz4KCQkJPEl0ZW0gSUQ9IjAiPjxOYW1lPkRhdGE8L05hbWU+CgkJCQk8T3BlcmF0aW9ucz5SVzwvT3BlcmF0aW9ucz4KCQkJCTxNdWx0aXBsZUluc3RhbmNlcz5NdWx0aXBsZTwvTXVsdGlwbGVJbnN0YW5jZXM+CgkJCQk8TWFuZGF0b3J5Pk1hbmRhdG9yeTwvTWFuZGF0b3J5PgoJCQkJPFR5cGU+T3BhcXVlPC9UeXBlPgoJCQkJPFJhbmdlRW51bWVyYXRpb24gLz4KCQkJCTxVbml0cyAvPgoJCQkJPERlc2NyaXB0aW9uPjwhW0NEQVRBW0luZGljYXRlcyB0aGUgYXBwbGljYXRpb24gZGF0YSBjb250ZW50Ll1dPjwvRGVzY3JpcHRpb24+CgkJCTwvSXRlbT4KCQkJPEl0ZW0gSUQ9IjEiPjxOYW1lPkRhdGEgUHJpb3JpdHk8L05hbWU+CgkJCQk8T3BlcmF0aW9ucz5SVzwvT3BlcmF0aW9ucz4KCQkJCTxNdWx0aXBsZUluc3RhbmNlcz5TaW5nbGU8L011bHRpcGxlSW5zdGFuY2VzPgoJCQkJPE1hbmRhdG9yeT5PcHRpb25hbDwvTWFuZGF0b3J5PgoJCQkJPFR5cGU+SW50ZWdlcjwvVHlwZT4KCQkJCTxSYW5nZUVudW1lcmF0aW9uPjEgYnl0ZXM8L1JhbmdlRW51bWVyYXRpb24+CgkJCQk8VW5pdHMgLz4KCQkJCTxEZXNjcmlwdGlvbj48IVtDREFUQVtJbmRpY2F0ZXMgdGhlIEFwcGxpY2F0aW9uIGRhdGEgcHJpb3JpdHk6CjA6SW1tZWRpYXRlCjE6QmVzdEVmZm9ydAoyOkxhdGVzdAozLTEwMDogUmVzZXJ2ZWQgZm9yIGZ1dHVyZSB1c2UuCjEwMS0yNTQ6IFByb3ByaWV0YXJ5IG1vZGUuXV0+PC9EZXNjcmlwdGlvbj4KCQkJPC9JdGVtPgoJCQk8SXRlbSBJRD0iMiI+PE5hbWU+RGF0YSBDcmVhdGlvbiBUaW1lPC9OYW1lPgoJCQkJPE9wZXJhdGlvbnM+Ulc8L09wZXJhdGlvbnM+CgkJCQk8TXVsdGlwbGVJbnN0YW5jZXM+U2luZ2xlPC9NdWx0aXBsZUluc3RhbmNlcz4KCQkJCTxNYW5kYXRvcnk+T3B0aW9uYWw8L01hbmRhdG9yeT4KCQkJCTxUeXBlPlRpbWU8L1R5cGU+CgkJCQk8UmFuZ2VFbnVtZXJhdGlvbiAvPgoJCQkJPFVuaXRzIC8+CgkJCQk8RGVzY3JpcHRpb24+PCFbQ0RBVEFbSW5kaWNhdGVzIHRoZSBEYXRhIGluc3RhbmNlIGNyZWF0aW9uIHRpbWVzdGFtcC5dXT48L0Rlc2NyaXB0aW9uPgoJCQk8L0l0ZW0+CgkJCTxJdGVtIElEPSIzIj48TmFtZT5EYXRhIERlc2NyaXB0aW9uPC9OYW1lPgoJCQkJPE9wZXJhdGlvbnM+Ulc8L09wZXJhdGlvbnM+CgkJCQk8TXVsdGlwbGVJbnN0YW5jZXM+U2luZ2xlPC9NdWx0aXBsZUluc3RhbmNlcz4KCQkJCTxNYW5kYXRvcnk+T3B0aW9uYWw8L01hbmRhdG9yeT4KCQkJCTxUeXBlPlN0cmluZzwvVHlwZT4KCQkJCTxSYW5nZUVudW1lcmF0aW9uPjMyIGJ5dGVzPC9SYW5nZUVudW1lcmF0aW9uPgoJCQkJPFVuaXRzIC8+CgkJCQk8RGVzY3JpcHRpb24+PCFbQ0RBVEFbSW5kaWNhdGVzIHRoZSBkYXRhIGRlc2NyaXB0aW9uLgplLmcuICJtZXRlciByZWFkaW5nIi5dXT48L0Rlc2NyaXB0aW9uPgoJCQk8L0l0ZW0+CgkJCTxJdGVtIElEPSI0Ij48TmFtZT5EYXRhIEZvcm1hdDwvTmFtZT4KCQkJCTxPcGVyYXRpb25zPlJXPC9PcGVyYXRpb25zPgoJCQkJPE11bHRpcGxlSW5zdGFuY2VzPlNpbmdsZTwvTXVsdGlwbGVJbnN0YW5jZXM+CgkJCQk8TWFuZGF0b3J5Pk9wdGlvbmFsPC9NYW5kYXRvcnk+CgkJCQk8VHlwZT5TdHJpbmc8L1R5cGU+CgkJCQk8UmFuZ2VFbnVtZXJhdGlvbj4zMiBieXRlczwvUmFuZ2VFbnVtZXJhdGlvbj4KCQkJCTxVbml0cyAvPgoJCQkJPERlc2NyaXB0aW9uPjwhW0NEQVRBW0luZGljYXRlcyB0aGUgZm9ybWF0IG9mIHRoZSBBcHBsaWNhdGlvbiBEYXRhLgplLmcuIFlHLU1ldGVyLVdhdGVyLVJlYWRpbmcKVVRGOC1zdHJpbmcKXV0+PC9EZXNjcmlwdGlvbj4KCQkJPC9JdGVtPgoJCQk8SXRlbSBJRD0iNSI+PE5hbWU+QXBwIElEPC9OYW1lPgoJCQkJPE9wZXJhdGlvbnM+Ulc8L09wZXJhdGlvbnM+CgkJCQk8TXVsdGlwbGVJbnN0YW5jZXM+U2luZ2xlPC9NdWx0aXBsZUluc3RhbmNlcz4KCQkJCTxNYW5kYXRvcnk+T3B0aW9uYWw8L01hbmRhdG9yeT4KCQkJCTxUeXBlPkludGVnZXI8L1R5cGU+CgkJCQk8UmFuZ2VFbnVtZXJhdGlvbj4yIGJ5dGVzPC9SYW5nZUVudW1lcmF0aW9uPgoJCQkJPFVuaXRzIC8+CgkJCQk8RGVzY3JpcHRpb24+PCFbQ0RBVEFbSW5kaWNhdGVzIHRoZSBkZXN0aW5hdGlvbiBBcHBsaWNhdGlvbiBJRC5dXT48L0Rlc2NyaXB0aW9uPgoJCQk8L0l0ZW0+PC9SZXNvdXJjZXM+CgkJPERlc2NyaXB0aW9uMj48IVtDREFUQVtdXT48L0Rlc2NyaXB0aW9uMj4KCTwvT2JqZWN0Pgo8L0xXTTJNPgo=", accessMode = ApiModelProperty.AccessMode.READ_WRITE)
    @JsonGetter("data")
    public String getEncodedData() {
        return Optional.ofNullable(data)
                .map(Base64.getEncoder()::encodeToString)
                .orElse(null);
    }

    /**
     * 功能：更新数据。
     * 参数：
     * - `data`：待处理数据。
     * 返回：无。
     */
    @JsonSetter("data")
    public void setEncodedData(String data) {
        this.data = Optional.ofNullable(data)
                .map(Base64.getDecoder()::decode)
                .orElse(null);
    }

    /**
     * 功能：获取`Encoded Preview`。
     * 参数：无。
     * 返回：文本结果。
     */
    @JsonGetter("preview")
    public String getEncodedPreview() {
        return Optional.ofNullable(preview)
                .map(Base64.getEncoder()::encodeToString)
                .orElse(null);
    }

    /**
     * 功能：更新`Encoded Preview`。
     * 参数：
     * - `preview`：`preview` 参数。
     * 返回：无。
     */
    @JsonSetter("preview")
    public void setEncodedPreview(String preview) {
        this.preview = Optional.ofNullable(preview)
                .map(Base64.getDecoder()::decode)
                .orElse(null);
    }

    /**
     * 功能：生成当前对象的文本表示。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    public String toString() {
        return super.toString();
    }
}

/*
 * 本类总结：
 * 1. 核心职责：`TbResource` 在 ThingsBoard Common 模块 中承担公共数据模型类型职责，核心目的是承载 ThingsBoard 实体、配置、查询、告警、通知、安全或设备画像等跨层数据契约。
 * 2. 核心流程：接收外部或持久化数据后在各层之间传递，必要时参与校验、序列化或转换。
 * 3. 关键依赖：主要依赖或协作对象包括REST Controller、DAO、Rule Engine、Transport、Edge 同步、缓存和 JSON 序列化框架。
 * 4. 学习重点：阅读本文件时应关注其生命周期、线程安全边界以及事务、缓存、MQTT、Actor、数据库和 Rule Engine 的直接或间接关系。
 */
