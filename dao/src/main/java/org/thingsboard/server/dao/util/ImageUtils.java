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
package org.thingsboard.server.dao.util;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;
import lombok.extern.slf4j.Slf4j;
import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.DocumentLoader;
import org.apache.batik.bridge.GVTBuilder;
import org.apache.batik.bridge.UserAgent;
import org.apache.batik.bridge.UserAgentAdapter;
import org.apache.batik.gvt.GraphicsNode;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.batik.util.XMLResourceDescriptor;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.w3c.dom.Document;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Map;

/**
 * 中文说明：
 * 1. `ImageUtils` 是 ThingsBoard DAO 中处理图片资源通用操作的工具类型。
 * 2. 它提供无状态或轻量的复用方法，减少多个调用点的重复实现。
 * 3. 方法通常完成格式化、校验、计算或简单对象构造。
 * 4. 它直接协作于方法参数和返回值所代表的数据类型。
 * 5. 集中工具方法可以统一边界行为，并降低细节变化对调用方的影响。
 * 6. 阅读时重点关注输入约束、边界值和方法是否修改传入对象。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class ImageUtils {

    private static final Map<String, String> mediaTypeMappings = Map.of(
            "jpeg", "jpg",
            "svg+xml", "svg",
            "x-icon", "ico"
    );

    /**
     * 功能：执行 `mediaTypeToFileExtension` 对应的处理。
     * 参数：
     * - `mimeType`：类型。
     * 返回：文本结果。
     */
    public static String mediaTypeToFileExtension(String mimeType) {
        String subtype = MimeTypeUtils.parseMimeType(mimeType).getSubtype();
        return mediaTypeMappings.getOrDefault(subtype, subtype);
    }

    /**
     * 功能：执行 `fileExtensionToMediaType` 对应的处理。
     * 参数：
     * - `extension`：`extension` 参数。
     * 返回：文本结果。
     */
    public static String fileExtensionToMediaType(String extension) {
        String subtype = mediaTypeMappings.entrySet().stream()
                .filter(mapping -> mapping.getValue().equals(extension))
                .map(Map.Entry::getKey).findFirst().orElse(extension);
        return new MimeType("image", subtype).toString();
    }

    /**
     * 功能：处理图片资源。
     * 参数：
     * - `data`：待处理数据。
     * - `mediaType`：类型。
     * - `thumbnailMaxDimension`：`thumbnailMaxDimension` 参数。
     * 返回：处理结果。
     */
    public static ProcessedImage processImage(byte[] data, String mediaType, int thumbnailMaxDimension) throws Exception {
        if (mediaTypeToFileExtension(mediaType).equals("svg")) {
            try {
                return processSvgImage(data, mediaType, thumbnailMaxDimension);
            } catch (Exception e) {
                if (log.isDebugEnabled()) { // printing stacktrace
                    log.warn("Couldn't process SVG image, leaving preview as original image", e);
                } else {
                    log.warn("Couldn't process SVG image, leaving preview as original image: {}", ExceptionUtils.getMessage(e));
                }
                return previewAsOriginalImage(data, mediaType);
            }
        }
        BufferedImage bufferedImage = null;
        try {
            bufferedImage = ImageIO.read(new ByteArrayInputStream(data));
        } catch (Exception ignored) {
        }
        if (bufferedImage == null) { // means that media type is not supported by ImageIO; extracting width and height from metadata and leaving preview as original image
            Metadata metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(data));
            ProcessedImage image = previewAsOriginalImage(data, mediaType);
            String dirName = "Unknown";
            for (Directory dir : metadata.getDirectories()) {
                Tag widthTag = dir.getTags().stream()
                        .filter(tag -> tag.getTagName().toLowerCase().contains("width"))
                        .findFirst().orElse(null);
                Tag heightTag = dir.getTags().stream()
                        .filter(tag -> tag.getTagName().toLowerCase().contains("height"))
                        .findFirst().orElse(null);
                if (widthTag == null || heightTag == null) {
                    continue;
                }
                int width = Integer.parseInt(dir.getObject(widthTag.getTagType()).toString());
                int height = Integer.parseInt(dir.getObject(heightTag.getTagType()).toString());
                image.setWidth(width);
                image.setHeight(height);
                image.getPreview().setWidth(width);
                image.getPreview().setHeight(height);
                dirName = dir.getName();
                break;
            }
            log.warn("Couldn't process {} ({}) with ImageIO, leaving preview as original image", mediaType, dirName);
            return image;
        }

        ProcessedImage image = new ProcessedImage();
        image.setMediaType(mediaType);
        image.setData(data);
        image.setSize(data.length);
        image.setWidth(bufferedImage.getWidth());
        image.setHeight(bufferedImage.getHeight());

        ProcessedImage preview = new ProcessedImage();
        int[] thumbnailDimensions = getThumbnailDimensions(image.getWidth(), image.getHeight(), thumbnailMaxDimension, true);
        preview.setWidth(thumbnailDimensions[0]);
        preview.setHeight(thumbnailDimensions[1]);

        if (preview.getWidth() == image.getWidth() && preview.getHeight() == image.getHeight()) {
            if (mediaType.equals("image/png")) {
                preview.setMediaType(mediaType);
                preview.setData(null);
                preview.setSize(data.length);
                image.setPreview(preview);
                return image;
            }
        }

        BufferedImage thumbnail = new BufferedImage(preview.getWidth(), preview.getHeight(), BufferedImage.TYPE_INT_ARGB);
        thumbnail.getGraphics().drawImage(bufferedImage, 0, 0, preview.getWidth(), preview.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(thumbnail, "png", out);

        preview.setMediaType("image/png");
        preview.setData(out.toByteArray());
        preview.setSize(preview.getData().length);
        image.setPreview(preview);
        return image;
    }

    /**
     * 功能：处理图片资源。
     * 参数：
     * - `data`：待处理数据。
     * - `mediaType`：类型。
     * - `thumbnailMaxDimension`：`thumbnailMaxDimension` 参数。
     * 返回：处理结果。
     */
    public static ProcessedImage processSvgImage(byte[] data, String mediaType, int thumbnailMaxDimension) throws Exception {
        SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(XMLResourceDescriptor.getXMLParserClassName());
        Document document = factory.createDocument(null, new ByteArrayInputStream(data));
        Integer width = null;
        Integer height = null;
        String strWidth = document.getDocumentElement().getAttribute("width");
        String strHeight = document.getDocumentElement().getAttribute("height");
        if (StringUtils.isNotEmpty(strWidth) && StringUtils.isNotEmpty(strHeight)) {
            try {
                width = (int) Double.parseDouble(strWidth);
                height = (int) Double.parseDouble(strHeight);
            } catch (NumberFormatException ignored) {} // in case width and height are in %, mm, etc.
        }
        if (width == null || height == null) {
            String viewBox = document.getDocumentElement().getAttribute("viewBox");
            if (StringUtils.isNotEmpty(viewBox)) {
                String[] viewBoxValues = viewBox.split(" ");
                if (viewBoxValues.length > 3) {
                    width = (int) Double.parseDouble(viewBoxValues[2]);
                    height = (int) Double.parseDouble(viewBoxValues[3]);
                }
            }
        }
        if (width == null) {
            UserAgent agent = new UserAgentAdapter();
            DocumentLoader loader = new DocumentLoader(agent);
            BridgeContext context = new BridgeContext(agent, loader);
            context.setDynamic(true);
            GVTBuilder builder = new GVTBuilder();
            GraphicsNode root = builder.build(context, document);
            var bounds = root.getPrimitiveBounds();
            if (bounds != null) {
                width = (int) bounds.getWidth();
                height = (int) bounds.getHeight();
            }
        }
        ProcessedImage image = new ProcessedImage();
        image.setMediaType(mediaType);
        image.setWidth(width == null ? 0 : width);
        image.setHeight(height == null ? 0 : height);
        image.setData(data);
        image.setSize(data.length);

        PNGTranscoder transcoder = new PNGTranscoder();
        if (image.getSize() < 10240) { // if SVG is smaller than 10kB (average 250x250 PNG preview size)
            return withPreviewAsOriginalImage(image);
        } else if (image.getSize() > 102400 && image.getWidth() != 0) { // considering SVG image detailed after 100kB
            // increasing preview dimensions
            thumbnailMaxDimension = 512;
            int[] thumbnailDimensions = getThumbnailDimensions(image.getWidth(), image.getHeight(), thumbnailMaxDimension, false);
            transcoder.addTranscodingHint(PNGTranscoder.KEY_WIDTH, (float) thumbnailDimensions[0]);
            transcoder.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, (float) thumbnailDimensions[1]);
        } else {
            transcoder.addTranscodingHint(PNGTranscoder.KEY_MAX_WIDTH, (float) thumbnailMaxDimension);
            transcoder.addTranscodingHint(PNGTranscoder.KEY_MAX_HEIGHT, (float) thumbnailMaxDimension);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        transcoder.transcode(new TranscoderInput(new ByteArrayInputStream(data)), new TranscoderOutput(out));
        byte[] pngThumbnail = out.toByteArray();

        ProcessedImage preview = new ProcessedImage();
        preview.setWidth(thumbnailMaxDimension);
        preview.setHeight(thumbnailMaxDimension);
        preview.setMediaType("image/png");
        preview.setData(pngThumbnail);
        preview.setSize(pngThumbnail.length);
        image.setPreview(preview);
        return image;
    }

    /**
     * 功能：执行 `previewAsOriginalImage` 对应的处理。
     * 参数：
     * - `data`：待处理数据。
     * - `mediaType`：类型。
     * 返回：处理结果。
     */
    private static ProcessedImage previewAsOriginalImage(byte[] data, String mediaType) {
        ProcessedImage image = new ProcessedImage();
        image.setMediaType(mediaType);
        image.setData(data);
        image.setSize(data.length);
        image.setWidth(0);
        image.setHeight(0);
        return withPreviewAsOriginalImage(image);
    }

    /**
     * 功能：执行 `withPreviewAsOriginalImage` 对应的处理。
     * 参数：
     * - `originalImage`：`originalImage` 参数。
     * 返回：处理结果。
     */
    public static ProcessedImage withPreviewAsOriginalImage(ProcessedImage originalImage) {
        originalImage.setPreview(originalImage.withData(null));
        return originalImage;
    }

    /**
     * 功能：获取`Thumbnail Dimensions`。
     * 参数：
     * - `originalWidth`：`originalWidth` 参数。
     * - `originalHeight`：`originalHeight` 参数。
     * - `maxDimension`：`maxDimension` 参数。
     * - `originalIfSmaller`：`originalIfSmaller` 参数。
     * 返回：处理结果。
     */
    private static int[] getThumbnailDimensions(int originalWidth, int originalHeight, int maxDimension, boolean originalIfSmaller) {
        if (originalWidth <= maxDimension && originalHeight <= maxDimension && originalIfSmaller) {
            return new int[]{originalWidth, originalHeight};
        }
        int thumbnailWidth;
        int thumbnailHeight;
        double aspectRatio = (double) originalWidth / originalHeight;
        if (originalWidth > originalHeight) {
            thumbnailWidth = maxDimension;
            thumbnailHeight = (int) (maxDimension / aspectRatio);
        } else {
            thumbnailWidth = (int) (maxDimension * aspectRatio);
            thumbnailHeight = maxDimension;
        }
        return new int[]{thumbnailWidth, thumbnailHeight};
    }

    /**
     * 中文说明：
     * 1. `ProcessedImage` 是 ThingsBoard DAO 中负责图片资源存取的访问组件。
     * 2. 它定义或实现查询、保存、更新和删除相关数据的操作。
     * 3. 方法参数和返回值以领域对象、标识符或分页结果为主。
     * 4. 它直接协作于持久化模型、查询实现和对应领域服务。
     * 5. 独立存取边界可以隐藏具体存储实现，避免业务层依赖底层查询细节。
     * 6. 阅读时重点关注查询条件、实体转换和批量操作的边界。
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProcessedImage {
        /**
         * 类型，用于区分不同处理分支。
         */
        private String mediaType;
        private int width;
        /**
         * `height` 字段，保存当前对象的对应属性。
         */
        private int height;
        /**
         * 数据列表，用于保存一组待处理对象。
         */
        @With
        private byte[] data;
        private long size;
        /**
         * `preview` 字段，保存当前对象的对应属性。
         */
        private ProcessedImage preview;
    }

}
