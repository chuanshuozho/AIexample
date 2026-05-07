package com.example.ai.service.impl;

import com.example.ai.entity.DocumentChunk;
import com.example.ai.entity.KnowledgeDocument;
import com.example.ai.entity.VectorEmbedding;
import com.example.ai.repository.DocumentChunkRepository;
import com.example.ai.repository.KnowledgeDocumentRepository;
import com.example.ai.repository.VectorEmbeddingRepository;
import com.example.ai.service.DocumentService;
import com.example.ai.service.EmbeddingService;
import com.example.ai.vectorstore.MySQLVectorStore;
import net.sourceforge.tess4j.Tesseract;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class DocumentServiceImpl implements DocumentService {

    private final KnowledgeDocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final VectorEmbeddingRepository vectorEmbeddingRepository;
    private final EmbeddingService embeddingService;
    private final MySQLVectorStore vectorStore;

    @Value("${app.knowledge.max-file-size:5242880}")  // 默认 5MB
    private long maxFileSize;

    @Value("${app.knowledge.chunk-size:500}")
    private int chunkSize;

    @Value("${app.knowledge.chunk-overlap:50}")
    private int chunkOverlap;

    @Value("${app.knowledge.storage-path:./uploads/knowledge}")
    private String storagePath;
    
    @Value("${app.knowledge.batch-size:10}")  // 分批处理大小
    private int batchSize;
    
    @Value("${app.knowledge.ocr.enabled:true}")  // 启用 OCR
    private boolean ocrEnabled;
    
    @Value("${app.knowledge.ocr.datapath:}")  // Tesseract 数据路径
    private String tesseractDataPath;
    
    @Value("${app.knowledge.ocr.language:chi_sim+eng}")  // OCR 语言（中文+英文）
    private String ocrLanguage;

    @Autowired
    public DocumentServiceImpl(KnowledgeDocumentRepository documentRepository,
                               DocumentChunkRepository chunkRepository,
                               VectorEmbeddingRepository vectorEmbeddingRepository,
                               EmbeddingService embeddingService,
                               MySQLVectorStore vectorStore) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.vectorEmbeddingRepository = vectorEmbeddingRepository;
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
    }

    @Override
    @Transactional
    public KnowledgeDocument uploadDocument(MultipartFile file) {
        // 限制文件大小（默认 5MB）
        if (file.getSize() > maxFileSize) {
            throw new RuntimeException("文件大小超过限制，最大允许 " + (maxFileSize / 1024 / 1024) + "MB");
        }

        String originalFilename = file.getOriginalFilename();
        String fileType = getFileExtension(originalFilename);
        if (!isValidFileType(fileType)) {
            throw new RuntimeException("不支持的文件类型: " + fileType + "。支持的类型: PDF, TXT, MD, DOCX");
        }

        try {
            Path uploadPath = Paths.get(storagePath).toAbsolutePath().normalize();
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String fileName = System.currentTimeMillis() + "_" + originalFilename;
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath);

            KnowledgeDocument document = new KnowledgeDocument(
                    originalFilename,
                    filePath.toString(),
                    file.getSize(),
                    fileType.toUpperCase()
            );

            return documentRepository.save(document);
        } catch (IOException e) {
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<KnowledgeDocument> getAllDocuments() {
        return documentRepository.findAllByOrderByUploadTimeDesc();
    }

    @Override
    public Optional<KnowledgeDocument> getDocument(Long documentId) {
        return documentRepository.findById(documentId);
    }

    @Override
    public void updateDocument(KnowledgeDocument document) {
        documentRepository.save(document);
    }

    @Override
    @Transactional
    public void deleteDocument(Long documentId) {
        Optional<KnowledgeDocument> docOpt = documentRepository.findById(documentId);
        if (docOpt.isEmpty()) {
            return;
        }

        KnowledgeDocument doc = docOpt.get();
        vectorStore.deleteByDocumentId(documentId);
        chunkRepository.deleteByDocumentId(documentId);

        try {
            Path filePath = Paths.get(doc.getFilePath());
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // Log but continue
        }

        documentRepository.deleteById(documentId);
    }

    @Override
    public List<DocumentChunk> getDocumentChunks(Long documentId) {
        return chunkRepository.findByDocumentIdOrderByChunkIndexAsc(documentId);
    }

    @Override
    @Transactional
    public void processDocument(Long documentId) {
        Optional<KnowledgeDocument> docOpt = documentRepository.findById(documentId);
        if (docOpt.isEmpty()) {
            throw new RuntimeException("Document not found: " + documentId);
        }

        KnowledgeDocument document = docOpt.get();
        System.out.println("=== 开始处理文档 ===");
        System.out.println("文档ID: " + documentId);
        System.out.println("文件: " + document.getFileName());
        System.out.println("大小: " + document.getFileSize() + " bytes");
        System.out.println("当前状态: " + document.getStatus());

        // 检查文档是否已经处理过
        if ("COMPLETED".equals(document.getStatus()) || "PROCESSING".equals(document.getStatus())) {
            System.out.println("文档已经处理过或正在处理中，跳过");
            return;
        }
        
        // 检查是否已有文本块
        List<DocumentChunk> existingChunks = chunkRepository.findByDocumentIdOrderByChunkIndexAsc(documentId);
        if (!existingChunks.isEmpty()) {
            System.out.println("文档已有 " + existingChunks.size() + " 个文本块，跳过处理");
            document.setStatus("COMPLETED");
            document.setChunkCount(existingChunks.size());
            documentRepository.save(document);
            return;
        }

        try {
            document.setStatus("PROCESSING");
            documentRepository.save(document);

            // 提取文本内容
            System.out.println("正在提取文本内容...");
            String content = extractContent(document);
            System.out.println("提取文本长度: " + (content != null ? content.length() : 0) + " 字符");

            if (content == null || content.trim().isEmpty()) {
                throw new RuntimeException("无法从文档中提取文本内容。如果 PDF 是扫描件，请使用 OCR 工具先转换为文本。");
            }
            
            // 检查文本是否太少（可能是图片 PDF）
            if (content.trim().length() < 50) {
                System.out.println("警告: 提取的文本内容很少，PDF 可能是扫描件或图片格式");
                System.out.println("提取的内容: " + content.trim());
                // 继续处理，但给出警告
            }

            // 分块处理
            System.out.println("正在分割文本...");
            List<String> chunks = splitIntoChunks(content);
            System.out.println("创建 " + chunks.size() + " 个文本块");
            
            // 释放原始文本内存
            content = null;
            System.gc();

            // 保存文本块（不生成嵌入向量，需要配置有效的 API Key）
            int chunkIndex = 0;
            int totalChunks = chunks.size();
            
            for (int i = 0; i < totalChunks; i++) {
                String chunkContent = chunks.get(i);
                try {
                    // 只保存文本块，不调用嵌入 API
                    DocumentChunk chunk = new DocumentChunk(documentId, chunkIndex, chunkContent);
                    chunkRepository.save(chunk);
                    chunkIndex++;
                } catch (Exception e) {
                    System.err.println("保存块 " + chunkIndex + " 时出错: " + e.getMessage());
                    chunkIndex++;
                }
            }
            
            // 释放分块列表内存
            chunks = null;
            System.gc();

            document.setStatus("COMPLETED");
            document.setChunkCount(chunkIndex);
            document.setProcessTime(LocalDateTime.now());
            documentRepository.save(document);
            System.out.println("文档处理完成。总块数: " + chunkIndex);
            System.out.println("提示: 文本块已保存，嵌入向量未生成。如需使用语义搜索功能，请配置有效的嵌入 API Key。");

        } catch (OutOfMemoryError e) {
            System.err.println("内存不足: " + e.getMessage());
            document.setStatus("FAILED");
            document.setErrorMessage("内存不足，无法处理文档。请尝试上传更小的文件。");
            documentRepository.save(document);
            throw new RuntimeException("内存不足: " + e.getMessage(), e);
        } catch (Exception e) {
            System.err.println("文档处理失败: " + e.getMessage());
            e.printStackTrace();
            document.setStatus("FAILED");
            document.setErrorMessage(e.getMessage());
            documentRepository.save(document);
            throw new RuntimeException("文档处理失败: " + e.getMessage(), e);
        }
    }

    private String extractContent(KnowledgeDocument document) throws IOException {
        String filePath = document.getFilePath();
        String fileType = document.getFileType();
        
        System.out.println("提取内容 - 文件路径: " + filePath);
        System.out.println("提取内容 - 文件类型: " + fileType);
        
        // 检查文件是否存在
        File file = new File(filePath);
        if (!file.exists()) {
            throw new RuntimeException("文件不存在: " + filePath);
        }
        
        String content;
        switch (fileType.toUpperCase()) {
            case "PDF":
                content = extractPdfContent(filePath);
                break;
            case "TXT":
            case "MD":
                content = extractTextContent(filePath);
                break;
            case "DOCX":
                content = extractDocxContent(filePath);
                break;
            default:
                throw new RuntimeException("不支持的文件类型: " + fileType);
        }
        
        System.out.println("提取内容 - 结果长度: " + (content != null ? content.length() : "null"));
        return content;
    }

    private String extractPdfContent(String filePath) throws IOException {
        File file = new File(filePath);
        System.out.println("PDF 文件大小: " + file.length() + " bytes");
        
        try (PDDocument document = Loader.loadPDF(file)) {
            System.out.println("PDF 页数: " + document.getNumberOfPages());
            
            // 首先尝试直接提取文本
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);
            
            System.out.println("PDF 直接提取文本长度: " + (text != null ? text.length() : "null"));
            
            // 如果提取的文本太少，可能是扫描件，尝试 OCR
            if (text == null || text.trim().length() < 100) {
                System.out.println("文本太少，可能是扫描件，尝试 OCR 提取...");
                
                if (ocrEnabled) {
                    String ocrText = extractPdfWithOcr(document);
                    if (ocrText != null && ocrText.trim().length() > text.trim().length()) {
                        text = ocrText;
                        System.out.println("OCR 提取文本长度: " + text.length());
                    }
                } else {
                    System.out.println("OCR 未启用，跳过 OCR 提取");
                }
            }
            
            if (text != null && text.length() > 500000) {
                text = text.substring(0, 500000);
                System.out.println("警告: PDF文本过长，已截断到 500000 字符");
            }
            return text;
        } catch (Exception e) {
            System.err.println("PDF 提取失败: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("PDF 提取失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 使用 OCR 提取 PDF 图片中的文字
     */
    private String extractPdfWithOcr(PDDocument document) {
        try {
            // 初始化 Tesseract
            Tesseract tesseract = new Tesseract();
            
            // 设置 Tesseract 数据路径
            if (tesseractDataPath != null && !tesseractDataPath.isEmpty()) {
                tesseract.setDatapath(tesseractDataPath);
            } else {
                // 默认路径
                String defaultPath = System.getenv("TESSDATA_PREFIX");
                if (defaultPath != null) {
                    tesseract.setDatapath(defaultPath);
                } else {
                    // 尝试常见路径
                    String[] commonPaths = {
                        "C:/Program Files/Tesseract-OCR/tessdata",
                        "C:/Tesseract-OCR/tessdata",
                        "/usr/share/tessdata",
                        "/usr/local/share/tessdata"
                    };
                    for (String path : commonPaths) {
                        if (new File(path).exists()) {
                            tesseract.setDatapath(path);
                            System.out.println("使用 Tesseract 数据路径: " + path);
                            break;
                        }
                    }
                }
            }
            
            tesseract.setLanguage(ocrLanguage); // 中文+英文
            tesseract.setOcrEngineMode(1); // LSTM only
            
            StringBuilder allText = new StringBuilder();
            PDFRenderer renderer = new PDFRenderer(document);
            
            int totalPages = document.getNumberOfPages();
            // 限制 OCR 页数，防止内存溢出
            int maxOcrPages = Math.min(totalPages, 20);
            
            System.out.println("开始 OCR 处理 " + maxOcrPages + " 页...");
            
            for (int pageIndex = 0; pageIndex < maxOcrPages; pageIndex++) {
                try {
                    // 渲染 PDF 页面为图片 (150 DPI 平衡质量和内存)
                    BufferedImage image = renderer.renderImage(pageIndex, 150);
                    
                    // OCR 识别
                    String pageText = tesseract.doOCR(image);
                    if (pageText != null && !pageText.trim().isEmpty()) {
                        allText.append(pageText).append("\n\n");
                    }
                    
                    // 释放图片内存
                    image.flush();
                    
                    // 每处理 5 页打印进度
                    if ((pageIndex + 1) % 5 == 0) {
                        System.out.println("OCR 已处理 " + (pageIndex + 1) + "/" + maxOcrPages + " 页");
                    }
                    
                    // 如果已提取足够文本，提前结束
                    if (allText.length() > 100000) {
                        System.out.println("已提取足够文本，提前结束 OCR");
                        break;
                    }
                } catch (Exception e) {
                    System.err.println("OCR 处理第 " + (pageIndex + 1) + " 页失败: " + e.getMessage());
                }
            }
            
            return allText.toString();
        } catch (Exception e) {
            System.err.println("OCR 初始化或处理失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private String extractTextContent(String filePath) throws IOException {
        String content = Files.readString(Paths.get(filePath));
        if (content.length() > 500000) {
            content = content.substring(0, 500000);
            System.out.println("警告: 文本过长，已截断到 500000 字符");
        }
        return content;
    }

    private String extractDocxContent(String filePath) throws IOException {
        StringBuilder content = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(filePath);
             XWPFDocument document = new XWPFDocument(fis)) {
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                content.append(paragraph.getText()).append("\n");
                // 限制长度
                if (content.length() > 500000) {
                    System.out.println("警告: DOCX文本过长，已截断到 500000 字符");
                    break;
                }
            }
        }
        return content.length() > 500000 ? content.substring(0, 500000) : content.toString();
    }

    private List<String> splitIntoChunks(String content) {
        List<String> chunks = new ArrayList<>();

        if (content == null || content.isEmpty()) {
            return chunks;
        }

        int start = 0;
        while (start < content.length()) {
            int end = Math.min(start + chunkSize, content.length());

            // 尝试找到一个好的断点（句号、换行等）
            if (end < content.length()) {
                int breakPoint = findBreakPoint(content, end);
                if (breakPoint > start) {
                    end = breakPoint;
                }
            }

            String chunk = content.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }

            // 下一个分块从当前结束位置开始（不重叠）
            start = end;
            
            // 限制最大分块数量
            if (chunks.size() > 1000) {
                System.out.println("警告: 分块数量超过 1000，已停止分割");
                break;
            }
        }

        return chunks;
    }

    private int findBreakPoint(String content, int position) {
        int windowStart = Math.max(0, position - 100);

        for (int i = position; i > windowStart; i--) {
            char c = content.charAt(i);
            if (c == '。' || c == '！' || c == '？' || c == '.' || c == '!' || c == '?') {
                return i + 1;
            }
        }

        for (int i = position; i > windowStart; i--) {
            char c = content.charAt(i);
            if (c == '\n') {
                return i + 1;
            }
        }

        return position;
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    private boolean isValidFileType(String fileType) {
        if (fileType == null || fileType.isEmpty()) {
            return false;
        }
        String upper = fileType.toUpperCase();
        return upper.equals("PDF") || upper.equals("TXT") || upper.equals("MD") || upper.equals("DOCX");
    }

    /**
     * 将 float 数组转换为字节数组（二进制格式）
     * 比 JSON 格式节省约 70% 的存储空间
     */
    private byte[] floatArrayToBytes(float[] floats) {
        ByteBuffer buffer = ByteBuffer.allocate(floats.length * 4);
        for (float f : floats) {
            buffer.putFloat(f);
        }
        return buffer.array();
    }

    /**
     * 将字节数组转换为 float 数组
     */
    public static float[] bytesToFloatArray(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        float[] floats = new float[bytes.length / 4];
        for (int i = 0; i < floats.length; i++) {
            floats[i] = buffer.getFloat();
        }
        return floats;
    }
}