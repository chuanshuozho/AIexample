// Knowledge Base Management JavaScript

let documents = [];
let deleteTargetId = null;

// Initialize
document.addEventListener('DOMContentLoaded', () => {
    loadDocuments();
    setupUploadArea();
    setupModal();
});

// Load documents from API
async function loadDocuments() {
    try {
        const response = await fetch('/api/knowledge/documents');
        documents = await response.json();
        renderDocuments();
    } catch (error) {
        console.error('Failed to load documents:', error);
        showEmptyState();
    }
}

// Render documents list
function renderDocuments() {
    const container = document.getElementById('documentList');
    
    if (documents.length === 0) {
        showEmptyState();
        return;
    }
    
    container.innerHTML = documents.map(doc => `
        <div class="document-item" data-id="${doc.id}">
            <div class="document-info">
                <div class="document-icon">${getFileIcon(doc.fileType)}</div>
                <div class="document-details">
                    <div class="document-name">${escapeHtml(doc.fileName)}</div>
                    <div class="document-meta">
                        ${formatFileSize(doc.fileSize)} · ${doc.chunkCount || 0} 个分片 · ${formatDate(doc.uploadTime)}
                    </div>
                </div>
            </div>
            <div class="document-status">
                <span class="status-badge status-${doc.status.toLowerCase()}">${getStatusText(doc.status)}</span>
            </div>
            <div class="document-actions">
                <button class="btn btn-danger" onclick="showDeleteModal(${doc.id})">删除</button>
            </div>
        </div>
    `).join('');
}

// Show empty state
function showEmptyState() {
    const container = document.getElementById('documentList');
    container.innerHTML = `
        <div class="empty-state">
            <div class="empty-icon">📭</div>
            <p>暂无文档</p>
            <p style="font-size: 14px; margin-top: 8px;">上传文档以构建知识库</p>
        </div>
    `;
}

// Setup upload area
function setupUploadArea() {
    const dropZone = document.getElementById('dropZone');
    const fileInput = document.getElementById('fileInput');
    
    // Click to upload
    dropZone.addEventListener('click', () => fileInput.click());
    
    // File input change
    fileInput.addEventListener('change', (e) => {
        if (e.target.files.length > 0) {
            uploadFiles(e.target.files);
        }
    });
    
    // Drag and drop
    dropZone.addEventListener('dragover', (e) => {
        e.preventDefault();
        dropZone.classList.add('dragover');
    });
    
    dropZone.addEventListener('dragleave', () => {
        dropZone.classList.remove('dragover');
    });
    
    dropZone.addEventListener('drop', (e) => {
        e.preventDefault();
        dropZone.classList.remove('dragover');
        if (e.dataTransfer.files.length > 0) {
            uploadFiles(e.dataTransfer.files);
        }
    });
}

// Upload files
async function uploadFiles(files) {
    const uploadProgress = document.getElementById('uploadProgress');
    const progressFill = document.getElementById('progressFill');
    
    uploadProgress.style.display = 'block';
    
    for (let i = 0; i < files.length; i++) {
        const file = files[i];
        const progress = ((i + 1) / files.length) * 100;
        progressFill.style.width = progress + '%';
        
        const formData = new FormData();
        formData.append('file', file);
        
        try {
            const response = await fetch('/api/knowledge/documents', {
                method: 'POST',
                body: formData
            });
            
            if (!response.ok) {
                throw new Error('Upload failed');
            }
        } catch (error) {
            console.error('Failed to upload file:', file.name, error);
            alert('上传失败: ' + file.name);
        }
    }
    
    // Hide progress and reload
    setTimeout(() => {
        uploadProgress.style.display = 'none';
        progressFill.style.width = '0%';
        loadDocuments();
    }, 500);
}

// Setup modal
function setupModal() {
    const modal = document.getElementById('deleteModal');
    const cancelBtn = document.getElementById('cancelDelete');
    const confirmBtn = document.getElementById('confirmDelete');
    
    cancelBtn.addEventListener('click', () => {
        modal.classList.remove('show');
        deleteTargetId = null;
    });
    
    confirmBtn.addEventListener('click', async () => {
        if (deleteTargetId) {
            await deleteDocument(deleteTargetId);
            modal.classList.remove('show');
            deleteTargetId = null;
        }
    });
    
    // Close on outside click
    modal.addEventListener('click', (e) => {
        if (e.target === modal) {
            modal.classList.remove('show');
            deleteTargetId = null;
        }
    });
}

// Show delete modal
function showDeleteModal(id) {
    deleteTargetId = id;
    document.getElementById('deleteModal').classList.add('show');
}

// Delete document
async function deleteDocument(id) {
    try {
        const response = await fetch(`/api/knowledge/documents/${id}`, {
            method: 'DELETE'
        });
        
        if (response.ok) {
            loadDocuments();
        } else {
            throw new Error('Delete failed');
        }
    } catch (error) {
        console.error('Failed to delete document:', error);
        alert('删除失败');
    }
}

// Helper functions
function getFileIcon(type) {
    const icons = {
        'PDF': '📄',
        'TXT': '📝',
        'MD': '📑',
        'DOCX': '📘'
    };
    return icons[type] || '📄';
}

function formatFileSize(bytes) {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
}

function formatDate(dateStr) {
    const date = new Date(dateStr);
    return date.toLocaleDateString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    });
}

function getStatusText(status) {
    const texts = {
        'PENDING': '等待处理',
        'PROCESSING': '处理中',
        'COMPLETED': '已完成',
        'FAILED': '处理失败'
    };
    return texts[status] || status;
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}
