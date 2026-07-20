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
package org.thingsboard.client.tools.migrator;

import com.google.common.collect.Lists;
import org.apache.cassandra.io.sstable.CQLSSTableWriter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.math.NumberUtils;
import org.thingsboard.server.common.data.StringUtils;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 中文说明：
 * 1. `PgCaMigrator` 是 ThingsBoard Tools 中围绕 `Pg Ca Migrator` 提供具体能力的类型。
 * 2. 它封装当前声明对应的核心操作和必要状态。
 * 3. 类中的字段和方法共同完成该职责范围内的数据处理。
 * 4. 它直接协作于构造参数、字段类型和公开方法涉及的对象。
 * 5. 独立类型可以明确职责边界，避免相关逻辑分散到多个调用方。
 * 6. 阅读时重点关注父类契约、公开入口和状态发生变化的位置。
 */
public class PgCaMigrator {

    /**
     * `LOG_BATCH`常量，用于统一引用固定值。
     */
    private final long LOG_BATCH = 1000000;
    private final long rowPerFile = 1000000;

    /**
     * 时间戳，用于控制时间范围或等待时长。
     */
    private long linesTsMigrated = 0;
    private long linesLatestMigrated = 0;
    private long castErrors = 0;
    /**
     * `castedOk` 字段，保存当前对象的对应属性。
     */
    private long castedOk = 0;

    private long currentWriterCount = 1;

    /**
     * 源文件，用于定位本地文件或目录。
     */
    private final File sourceFile;
    private final boolean castStringIfPossible;

    /**
     * 实体，用于区分不同处理分支。
     */
    private final RelatedEntitiesParser entityIdsAndTypes;
    private final DictionaryParser keyParser;
    private CQLSSTableWriter currentTsWriter;
    /**
     * 当前分区写入器，表示当前对象的对应属性。
     */
    private CQLSSTableWriter currentPartitionsWriter;
    private CQLSSTableWriter currentTsLatestWriter;
    private final Set<String> partitions = new HashSet<>();

    /**
     * 时间戳，用于控制时间范围或等待时长。
     */
    private File outTsDir;
    private File outTsLatestDir;

    /**
     * 功能：创建 `PgCaMigrator` 实例，并初始化必要字段。
     * 参数：
     * - `sourceFile`：`sourceFile` 参数。
     * - `ourTsDir`：`ourTsDir` 参数。
     * - `outTsPartitionDir`：分区标识或分区信息。
     * - `outTsLatestDir`：`outTsLatestDir` 参数。
     * - 其余参数：补充处理条件。
     * 返回：新创建的对象实例。
     */
    public PgCaMigrator(File sourceFile,
                                File ourTsDir,
                                File outTsPartitionDir,
                                File outTsLatestDir,
                                RelatedEntitiesParser allEntityIdsAndTypes,
                                DictionaryParser dictionaryParser,
                                boolean castStringsIfPossible) {
        this.sourceFile = sourceFile;
        this.entityIdsAndTypes = allEntityIdsAndTypes;
        this.keyParser = dictionaryParser;
        this.castStringIfPossible = castStringsIfPossible;
        if(outTsLatestDir != null) {
            this.currentTsLatestWriter = WriterBuilder.getLatestWriter(outTsLatestDir);
            this.outTsLatestDir = outTsLatestDir;
        }
        if(ourTsDir != null) {
            this.currentTsWriter = WriterBuilder.getTsWriter(ourTsDir);
            this.currentPartitionsWriter = WriterBuilder.getPartitionWriter(outTsPartitionDir);
            this.outTsDir = ourTsDir;
        }
    }

    /**
     * 功能：执行 `migrate` 对应的处理。
     * 参数：无。
     * 返回：无。
     */
    public void migrate() throws IOException {
        boolean isTsDone = false;
        boolean isLatestDone = false;
        String line;
        LineIterator iterator = FileUtils.lineIterator(this.sourceFile);

        try {
            while(iterator.hasNext()) {
                line = iterator.nextLine();
                if(!isLatestDone && isBlockLatestStarted(line)) {
                    System.out.println("START TO MIGRATE LATEST");
                    long start = System.currentTimeMillis();
                    processBlock(iterator, currentTsLatestWriter, outTsLatestDir, this::toValuesLatest);
                    System.out.println("TOTAL LINES MIGRATED: " + linesLatestMigrated + ", FORMING OF SSL FOR LATEST TS FINISHED WITH TIME: " + (System.currentTimeMillis() - start) + " ms.");
                    isLatestDone = true;
                }

                if(!isTsDone && isBlockTsStarted(line)) {
                    System.out.println("START TO MIGRATE TS");
                    long start = System.currentTimeMillis();
                    processBlock(iterator, currentTsWriter, outTsDir, this::toValuesTs);
                    System.out.println("TOTAL LINES MIGRATED: " + linesTsMigrated + ", FORMING OF SSL FOR TS FINISHED WITH TIME: " + (System.currentTimeMillis() - start) + " ms.");
                    isTsDone = true;
                }
            }

            System.out.println("Partitions collected " + partitions.size());
            long startTs = System.currentTimeMillis();
            for (String partition : partitions) {
                String[] split = partition.split("\\|");
                List<Object> values = Lists.newArrayList();
                values.add(split[0]);
                values.add(UUID.fromString(split[1]));
                values.add(split[2]);
                values.add(Long.parseLong(split[3]));
                currentPartitionsWriter.addRow(values);
            }

            System.out.println(new Date() + " Migrated partitions " + partitions.size() + " in " + (System.currentTimeMillis() - startTs));

            System.out.println();
            System.out.println("Finished migrate Telemetry");

        } finally {
            iterator.close();
            currentTsLatestWriter.close();
            currentTsWriter.close();
            currentPartitionsWriter.close();
        }
    }

    /**
     * 功能：执行 `logLinesProcessed` 对应的处理。
     * 参数：
     * - `lines`：`lines` 参数。
     * 返回：无。
     */
    private void logLinesProcessed(long lines) {
        if (lines % LOG_BATCH == 0) {
            System.out.println(new Date() + " lines processed = " + lines + " in, castOk " + castedOk + "  castErr " + castErrors);
        }
    }

    /**
     * 功能：执行 `logLinesMigrated` 对应的处理。
     * 参数：
     * - `lines`：`lines` 参数。
     * 返回：无。
     */
    private void logLinesMigrated(long lines) {
        if(lines % LOG_BATCH == 0) {
            System.out.println(new Date() + " lines migrated = " + lines + " in, castOk " + castedOk + "  castErr " + castErrors);
        }
    }

    /**
     * 功能：保存或创建键。
     * 参数：
     * - `result`：数据列表。
     * - `raw`：数据列表。
     * 返回：无。
     */
    private void addTypeIdKey(List<Object> result, List<String> raw) {
        result.add(entityIdsAndTypes.getEntityType(raw.get(0)));
        result.add(UUID.fromString(raw.get(0)));
        result.add(keyParser.getKeyByKeyId(raw.get(1)));
    }

    /**
     * 功能：保存或创建`Partitions`。
     * 参数：
     * - `result`：数据列表。
     * - `raw`：数据列表。
     * 返回：无。
     */
    private void addPartitions(List<Object> result, List<String> raw) {
        long ts = Long.parseLong(raw.get(2));
        long partition = toPartitionTs(ts);
        result.add(partition);
        result.add(ts);
    }

    /**
     * 功能：保存或创建时序数据。
     * 参数：
     * - `result`：数据列表。
     * - `raw`：数据列表。
     * 返回：无。
     */
    private void addTimeseries(List<Object> result, List<String> raw) {
        result.add(Long.parseLong(raw.get(2)));
    }

    /**
     * 功能：保存或创建`Values`。
     * 参数：
     * - `result`：数据列表。
     * - `raw`：数据列表。
     * 返回：无。
     */
    private void addValues(List<Object> result, List<String> raw) {
        result.add(raw.get(3).equals("\\N") ? null : raw.get(3).equals("t") ? Boolean.TRUE : Boolean.FALSE);
        result.add(raw.get(4).equals("\\N") ? null : raw.get(4));
        result.add(raw.get(5).equals("\\N") ? null : Long.parseLong(raw.get(5)));
        result.add(raw.get(6).equals("\\N") ? null : Double.parseDouble(raw.get(6)));
        result.add(raw.get(7).equals("\\N") ? null : raw.get(7));
    }

    /**
     * 功能：执行 `toValuesTs` 对应的处理。
     * 参数：
     * - `raw`：数据列表。
     * 返回：匹配的数据集合。
     */
    private List<Object> toValuesTs(List<String> raw) {

        logLinesMigrated(linesTsMigrated++);

        List<Object> result = new ArrayList<>();

        addTypeIdKey(result, raw);
        addPartitions(result, raw);
        addValues(result, raw);

        processPartitions(result);

        return result;
    }

    /**
     * 功能：执行 `toValuesLatest` 对应的处理。
     * 参数：
     * - `raw`：数据列表。
     * 返回：匹配的数据集合。
     */
    private List<Object> toValuesLatest(List<String> raw) {
        logLinesMigrated(linesLatestMigrated++);
        List<Object> result = new ArrayList<>();

        addTypeIdKey(result, raw);
        addTimeseries(result, raw);
        addValues(result, raw);

        return result;
    }

    /**
     * 功能：执行 `toPartitionTs` 对应的处理。
     * 参数：
     * - `ts`：时间戳。
     * 返回：数值结果。
     */
    private long toPartitionTs(long ts) {
        LocalDateTime time = LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneOffset.UTC);
        return time.truncatedTo(ChronoUnit.DAYS).withDayOfMonth(1).toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    /**
     * 功能：处理`Partitions`。
     * 参数：
     * - `values`：值。
     * 返回：无。
     */
    private void processPartitions(List<Object> values) {
        String key = values.get(0) + "|" + values.get(1) + "|" + values.get(2) + "|" + values.get(3);
        partitions.add(key);
    }

    /**
     * 功能：处理`Block`。
     * 参数：
     * - `iterator`：`iterator` 参数。
     * - `writer`：`writer` 参数。
     * - `outDir`：`outDir` 参数。
     * - `function`：数据列表。
     * 返回：无。
     */
    private void processBlock(LineIterator iterator, CQLSSTableWriter writer, File outDir, Function<List<String>, List<Object>> function) {
        String currentLine;
        long linesProcessed = 0;
        while(iterator.hasNext()) {
            logLinesProcessed(linesProcessed++);
            currentLine = iterator.nextLine();
            if(isBlockFinished(currentLine)) {
                return;
            }

            try {
                List<String> raw = Arrays.stream(currentLine.trim().split("\t"))
                        .map(String::trim)
                        .collect(Collectors.toList());
                List<Object> values = function.apply(raw);

                if (this.currentWriterCount == 0) {
                    System.out.println(new Date() + " close writer " + new Date());
                    writer.close();
                    writer = WriterBuilder.getLatestWriter(outDir);
                }

                if (this.castStringIfPossible) {
                    writer.addRow(castToNumericIfPossible(values));
                } else {
                    writer.addRow(values);
                }

                currentWriterCount++;
                if (currentWriterCount >= rowPerFile) {
                    currentWriterCount = 0;
                }
            } catch (Exception ex) {
                System.out.println(ex.getMessage() + " -> " + currentLine);
            }
        }
    }

    /**
     * 功能：执行 `castToNumericIfPossible` 对应的处理。
     * 参数：
     * - `values`：值。
     * 返回：匹配的数据集合。
     */
    private List<Object> castToNumericIfPossible(List<Object> values) {
        try {
            if (values.get(6) != null && NumberUtils.isNumber(values.get(6).toString())) {
                Double casted = NumberUtils.createDouble(values.get(6).toString());
                List<Object> numeric = Lists.newArrayList();
                numeric.addAll(values);
                numeric.set(6, null);
                numeric.set(8, casted);
                castedOk++;
                return numeric;
            }
        } catch (Throwable th) {
            castErrors++;
        }

        processPartitions(values);

        return values;
    }

    /**
     * 功能：判断`Block Finished`。
     * 参数：
     * - `line`：`line` 参数。
     * 返回：判断结果。
     */
    private boolean isBlockFinished(String line) {
        return StringUtils.isBlank(line) || line.equals("\\.");
    }

    /**
     * 功能：判断时间戳。
     * 参数：
     * - `line`：`line` 参数。
     * 返回：判断结果。
     */
    private boolean isBlockTsStarted(String line) {
        return line.startsWith("COPY public.ts_kv (");
    }

    /**
     * 功能：判断`Block Latest Started`。
     * 参数：
     * - `line`：`line` 参数。
     * 返回：判断结果。
     */
    private boolean isBlockLatestStarted(String line) {
        return line.startsWith("COPY public.ts_kv_latest (");
    }
}
