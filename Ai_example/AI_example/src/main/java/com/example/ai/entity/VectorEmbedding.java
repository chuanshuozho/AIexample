package com.example.ai.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "vector_embeddings")
public class VectorEmbedding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chunk_id", nullable = false, unique = true)
    private Long chunkId;

    // 使用 LONGBLOB 存储二进制向量数据，比 JSON 更节省空间
    @Column(nullable = false, columnDefinition = "LONGBLOB")
    private byte[] embedding;

    @Column(nullable = false)
    private Integer dimensions;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }

    // Constructors
    public VectorEmbedding() {}

    public VectorEmbedding(Long chunkId, byte[] embedding, Integer dimensions) {
        this.chunkId = chunkId;
        this.embedding = embedding;
        this.dimensions = dimensions;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getChunkId() { return chunkId; }
    public void setChunkId(Long chunkId) { this.chunkId = chunkId; }

    public byte[] getEmbedding() { return embedding; }
    public void setEmbedding(byte[] embedding) { this.embedding = embedding; }

    public Integer getDimensions() { return dimensions; }
    public void setDimensions(Integer dimensions) { this.dimensions = dimensions; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
