package com.example.ai.controller;

import com.example.ai.entity.DocumentChunk;
import com.example.ai.entity.KnowledgeDocument;
import com.example.ai.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {
    
    private final DocumentService documentService;
    
    @Autowired
    public KnowledgeController(DocumentService documentService) {
        this.documentService = documentService;
    }
    
    @PostMapping("/documents")
    public ResponseEntity<?> uploadDocument(@RequestParam("file") MultipartFile file) {
        try {
            System.out.println("=== Upload Request ===");
            System.out.println("File name: " + file.getOriginalFilename());
            System.out.println("File size: " + file.getSize());
            System.out.println("Content type: " + file.getContentType());
            
            KnowledgeDocument document = documentService.uploadDocument(file);
            
            // Process document asynchronously (chunk and embed)
            new Thread(() -> {
                try {
                    System.out.println("Starting document processing in background thread...");
                    documentService.processDocument(document.getId());
                } catch (OutOfMemoryError e) {
                    System.err.println("Out of memory while processing document: " + e.getMessage());
                    // Update document status to FAILED
                    try {
                        document.setStatus("FAILED");
                        document.setErrorMessage("内存不足，无法处理文档。请尝试上传更小的文件或增加JVM内存(-Xmx512m)");
                        documentService.updateDocument(document);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                } catch (Exception e) {
                    System.err.println("Process document error: " + e.getMessage());
                    e.printStackTrace();
                }
            }).start();
            return ResponseEntity.ok(document);
        } catch (Exception e) {
            System.err.println("Upload error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("上传失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/documents")
    public List<KnowledgeDocument> getAllDocuments() {
        return documentService.getAllDocuments();
    }
    
    @GetMapping("/documents/{id}")
    public ResponseEntity<KnowledgeDocument> getDocument(@PathVariable Long id) {
        Optional<KnowledgeDocument> document = documentService.getDocument(id);
        return document.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/documents/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id) {
        try {
            System.out.println("=== Delete Document ===");
            System.out.println("Document ID: " + id);
            documentService.deleteDocument(id);
            System.out.println("Document deleted successfully");
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            System.err.println("Delete error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/documents/{id}/chunks")
    public List<DocumentChunk> getDocumentChunks(@PathVariable Long id) {
        return documentService.getDocumentChunks(id);
    }
    
    @PostMapping("/documents/{id}/process")
    public ResponseEntity<Void> processDocument(@PathVariable Long id) {
        try {
            documentService.processDocument(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
