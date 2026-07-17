/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Content of this file was modified to addresses the issue https://issues.apache.org/jira/browse/KAFKA-4090
 *
 */
package org.apache.kafka.common.network;

import org.apache.kafka.common.memory.MemoryPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thingsboard.server.common.data.exception.ThingsboardKafkaClientError;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ScatteringByteChannel;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * A size delimited Receive that consists of a 4 byte network-ordered size N followed by N bytes of content
 */
/**
 * 中文说明：
 * 1. `NetworkReceive` 是 ThingsBoard Application 中承载 `Network Receive` 信息的数据类型。
 * 2. 它把一次调用、消息传递或序列化所需的数据组织为明确结构。
 * 3. 字段分别表示该业务对象的标识、状态、内容或处理参数。
 * 4. 直接依赖的类型边界包括 `Receive`。
 * 5. 独立数据类型可以固定跨层契约，避免使用无结构的参数集合。
 * 6. 阅读时重点关注字段语义、构造方式以及对象在调用链中的使用位置。
 */
public class NetworkReceive implements Receive {

    /**
     * `UNKNOWN_SOURCE`常量，用于统一引用固定值。
     */
    public final static String UNKNOWN_SOURCE = "";
    public final static int UNLIMITED = -1;
    /**
     * `TB_MAX_REQUESTED_BUFFER_SIZE`常量，用于统一引用固定值。
     */
    public final static int TB_MAX_REQUESTED_BUFFER_SIZE = 100 * 1024 * 1024;
    public final static int TB_LOG_REQUESTED_BUFFER_SIZE = 10 * 1024 * 1024;
    private static final Logger log = LoggerFactory.getLogger(NetworkReceive.class);
    private static final ByteBuffer EMPTY_BUFFER = ByteBuffer.allocate(0);

    /**
     * `source` 字段，保存当前对象的对应属性。
     */
    private final String source;
    private final ByteBuffer size;
    /**
     * `maxSize` 字段，保存当前对象的对应属性。
     */
    private final int maxSize;
    private final MemoryPool memoryPool;
    /**
     * 当前请求对象，封装本次处理需要的输入信息。
     */
    private int requestedBufferSize = -1;
    private ByteBuffer buffer;


    /**
     * 功能：创建 `NetworkReceive` 实例，并初始化必要字段。
     * 参数：
     * - `source`：`source` 参数。
     * - `buffer`：`buffer` 参数。
     * 返回：新创建的对象实例。
     */
    public NetworkReceive(String source, ByteBuffer buffer) {
        this.source = source;
        this.buffer = buffer;
        this.size = null;
        this.maxSize = TB_MAX_REQUESTED_BUFFER_SIZE;
        this.memoryPool = MemoryPool.NONE;
    }

    /**
     * 功能：创建 `NetworkReceive` 实例，并初始化必要字段。
     * 参数：
     * - `source`：`source` 参数。
     * 返回：新创建的对象实例。
     */
    public NetworkReceive(String source) {
        this.source = source;
        this.size = ByteBuffer.allocate(4);
        this.buffer = null;
        this.maxSize = TB_MAX_REQUESTED_BUFFER_SIZE;
        this.memoryPool = MemoryPool.NONE;
    }

    /**
     * 功能：创建 `NetworkReceive` 实例，并初始化必要字段。
     * 参数：
     * - `maxSize`：`maxSize` 参数。
     * - `source`：`source` 参数。
     * 返回：新创建的对象实例。
     */
    public NetworkReceive(int maxSize, String source) {
        this.source = source;
        this.size = ByteBuffer.allocate(4);
        this.buffer = null;
        this.maxSize = getMaxSize(maxSize);
        this.memoryPool = MemoryPool.NONE;
    }

    /**
     * 功能：创建 `NetworkReceive` 实例，并初始化必要字段。
     * 参数：
     * - `maxSize`：`maxSize` 参数。
     * - `source`：`source` 参数。
     * - `memoryPool`：`memoryPool` 参数。
     * 返回：新创建的对象实例。
     */
    public NetworkReceive(int maxSize, String source, MemoryPool memoryPool) {
        this.source = source;
        this.size = ByteBuffer.allocate(4);
        this.buffer = null;
        this.maxSize = getMaxSize(maxSize);
        this.memoryPool = memoryPool;
    }

    /**
     * 功能：创建 `NetworkReceive` 实例，并初始化必要字段。
     * 参数：无。
     * 返回：新创建的对象实例。
     */
    public NetworkReceive() {
        this(UNKNOWN_SOURCE);
    }

    /**
     * 功能：执行 `source` 对应的处理。
     * 参数：无。
     * 返回：文本结果。
     */
    @Override
    public String source() {
        return source;
    }

    /**
     * 功能：执行 `complete` 对应的处理。
     * 参数：无。
     * 返回：判断结果。
     */
    @Override
    public boolean complete() {
        return !size.hasRemaining() && buffer != null && !buffer.hasRemaining();
    }

    /**
     * 功能：执行 `readFrom` 对应的处理。
     * 参数：
     * - `channel`：网络通道。
     * 返回：数值结果。
     */
    public long readFrom(ScatteringByteChannel channel) throws IOException {
        int read = 0;
        if (size.hasRemaining()) {
            int bytesRead = channel.read(size);
            if (bytesRead < 0)
                throw new EOFException();
            read += bytesRead;
            if (!size.hasRemaining()) {
                size.rewind();
                int receiveSize = size.getInt();
                if (receiveSize < 0)
                    throw new InvalidReceiveException("Invalid receive (size = " + receiveSize + ")");
                if (maxSize != UNLIMITED && receiveSize > maxSize) {
                    throw new ThingsboardKafkaClientError("Invalid receive (size = " + receiveSize + " larger than " + maxSize + ")");
                }
                requestedBufferSize = receiveSize; //may be 0 for some payloads (SASL)
                if (receiveSize == 0) {
                    buffer = EMPTY_BUFFER;
                }
            }
        }
        if (buffer == null && requestedBufferSize != -1) { //we know the size we want but havent been able to allocate it yet
            if (requestedBufferSize > TB_LOG_REQUESTED_BUFFER_SIZE) {
                String stackTrace = Arrays.stream(Thread.currentThread().getStackTrace()).map(StackTraceElement::toString).collect(Collectors.joining("|"));
                log.error("Allocating buffer of size {} for source {}", requestedBufferSize, source);
                log.error("Stack Trace: {}", stackTrace);
            }
            buffer = memoryPool.tryAllocate(requestedBufferSize);
            if (buffer == null)
                log.trace("Broker low on memory - could not allocate buffer of size {} for source {}", requestedBufferSize, source);
        }
        if (buffer != null) {
            int bytesRead = channel.read(buffer);
            if (bytesRead < 0)
                throw new EOFException();
            read += bytesRead;
        }

        return read;
    }

    /**
     * 功能：执行 `requiredMemoryAmountKnown` 对应的处理。
     * 参数：无。
     * 返回：判断结果。
     */
    @Override
    public boolean requiredMemoryAmountKnown() {
        return requestedBufferSize != -1;
    }

    /**
     * 功能：执行 `memoryAllocated` 对应的处理。
     * 参数：无。
     * 返回：判断结果。
     */
    @Override
    public boolean memoryAllocated() {
        return buffer != null;
    }


    /**
     * 功能：执行 `close` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    @Override
    public void close() throws IOException {
        if (buffer != null && buffer != EMPTY_BUFFER) {
            memoryPool.release(buffer);
            buffer = null;
        }
    }

    /**
     * 功能：执行 `payload` 对应的处理。
     * 参数：无。
     * 返回：处理结果。
     */
    public ByteBuffer payload() {
        return this.buffer;
    }

    /**
     * 功能：执行 `bytesRead` 对应的处理。
     * 参数：无。
     * 返回：数值结果。
     */
    public int bytesRead() {
        if (buffer == null)
            return size.position();
        return buffer.position() + size.position();
    }

    /**
     * Returns the total size of the receive including payload and size buffer
     * for use in metrics. This is consistent with {@link NetworkSend#size()}
     */
    /**
     * 功能：执行 `size` 对应的处理。
     * 参数：无。
     * 返回：数值结果。
     */
    public int size() {
        return payload().limit() + size.limit();
    }

    /**
     * 功能：获取`Max Size`。
     * 参数：
     * - `maxSize`：`maxSize` 参数。
     * 返回：数值结果。
     */
    private int getMaxSize(int maxSize) {
        return maxSize == UNLIMITED ? TB_MAX_REQUESTED_BUFFER_SIZE : Math.min(maxSize, TB_MAX_REQUESTED_BUFFER_SIZE);
    }

}
