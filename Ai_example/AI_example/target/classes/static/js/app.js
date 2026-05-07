// DOM Elements
const chatMessages = document.getElementById('chatMessages');
const messageInput = document.getElementById('messageInput');
const sendBtn = document.getElementById('sendBtn');
const historyList = document.getElementById('historyList');
const sidebar = document.getElementById('sidebar');
const sidebarOverlay = document.getElementById('sidebarOverlay');

// State
let currentSessionId = null;
let sessions = [];

// Initialize
messageInput.addEventListener('input', function() {
    this.style.height = 'auto';
    this.style.height = Math.min(this.scrollHeight, 200) + 'px';
});

// Save current session to localStorage
function saveCurrentSession() {
    if (currentSessionId) {
        localStorage.setItem('currentSessionId', currentSessionId);
    } else {
        localStorage.removeItem('currentSessionId');
    }
}

// Restore session from localStorage
function getSavedSessionId() {
    return localStorage.getItem('currentSessionId');
}

// Sidebar Toggle
function toggleSidebar() {
    sidebar.classList.toggle('open');
    sidebarOverlay.classList.toggle('open');
}

// Keyboard Handler
function handleKeyDown(event) {
    if (event.key === 'Enter' && !event.shiftKey) {
        event.preventDefault();
        sendMessage();
    }
}

// Welcome Screen
function showWelcome() {
    chatMessages.innerHTML = `
        <div class="welcome-screen">
            <div class="welcome-logo">
                <svg viewBox="0 0 24 24"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1 17.93c-3.95-.49-7-3.85-7-7.93 0-.62.08-1.21.21-1.79L9 15v1c0 1.1.9 2 2 2v1.93zm6.9-2.54c-.26-.81-1-1.39-1.9-1.39h-1v-3c0-.55-.45-1-1-1H8v-2h2c.55 0 1-.45 1-1V7h2c1.1 0 2-.9 2-2v-.41c2.93 1.19 5 4.06 5 7.41 0 2.08-.8 3.97-2.1 5.39z"/></svg>
            </div>
            <h1 class="welcome-title">AI 全能助手</h1>
            <p class="welcome-subtitle">编程、写作、翻译、数学……有问必答</p>
            <div class="suggestions">
                <div class="suggestion-card" onclick="useSuggestion('帮我写一段 Python 快速排序代码')">
                    <div class="suggestion-title">写一段代码</div>
                    <div class="suggestion-desc">Python 快速排序实现</div>
                </div>
                <div class="suggestion-card" onclick="useSuggestion('用简单的语言解释什么是机器学习')">
                    <div class="suggestion-title">解释概念</div>
                    <div class="suggestion-desc">什么是机器学习</div>
                </div>
                <div class="suggestion-card" onclick="useSuggestion('帮我翻译这段话成英文：人工智能正在改变世界')">
                    <div class="suggestion-title">翻译文本</div>
                    <div class="suggestion-desc">中译英翻译</div>
                </div>
                <div class="suggestion-card" onclick="useSuggestion('给我讲一个有趣的科学小故事')">
                    <div class="suggestion-title">讲故事</div>
                    <div class="suggestion-desc">科学小故事</div>
                </div>
            </div>
        </div>
    `;
}

function useSuggestion(text) {
    messageInput.value = text;
    sendMessage();
}

// Message Functions
function addMessage(content, isUser) {
    const welcome = chatMessages.querySelector('.welcome-screen');
    if (welcome) welcome.remove();

    const messageDiv = document.createElement('div');
    messageDiv.className = `message ${isUser ? 'user' : 'assistant'}`;

    const innerDiv = document.createElement('div');
    innerDiv.className = 'message-inner';

    const avatar = document.createElement('div');
    avatar.className = 'message-avatar';
    
    if (isUser) {
        avatar.textContent = 'U';
    } else {
        avatar.innerHTML = '<svg viewBox="0 0 24 24" width="18" height="18" fill="white"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1 17.93c-3.95-.49-7-3.85-7-7.93 0-.62.08-1.21.21-1.79L9 15v1c0 1.1.9 2 2 2v1.93zm6.9-2.54c-.26-.81-1-1.39-1.9-1.39h-1v-3c0-.55-.45-1-1-1H8v-2h2c.55 0 1-.45 1-1V7h2c1.1 0 2-.9 2-2v-.41c2.93 1.19 5 4.06 5 7.41 0 2.08-.8 3.97-2.1 5.39z"/></svg>';
    }

    const contentDiv = document.createElement('div');
    contentDiv.className = 'message-content';

    if (isUser) {
        contentDiv.textContent = content;
    } else {
        try {
            // 确保 marked 库可用
            if (typeof marked !== 'undefined') {
                // 配置 marked 选项
                marked.setOptions({
                    breaks: true,  // 支持 GitHub 风格的换行
                    gfm: true      // 启用 GitHub Flavored Markdown
                });
                contentDiv.innerHTML = marked.parse(content);
            } else {
                // 如果 marked 不可用，显示原始内容
                contentDiv.innerHTML = '<pre>' + content.replace(/</g, '&lt;').replace(/>/g, '&gt;') + '</pre>';
            }
            // 安全检查 hljs 是否可用
            if (typeof hljs !== 'undefined') {
                contentDiv.querySelectorAll('pre code').forEach(block => {
                    try {
                        hljs.highlightElement(block);
                    } catch (e) {
                        console.warn('Highlight error:', e);
                    }
                });
            }
        } catch (e) {
            console.error('Markdown parse error:', e);
            // 如果解析失败，显示原始内容
            contentDiv.innerHTML = '<pre>' + content.replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/\n/g, '<br>') + '</pre>';
        }
    }

    innerDiv.appendChild(avatar);
    innerDiv.appendChild(contentDiv);
    messageDiv.appendChild(innerDiv);
    chatMessages.appendChild(messageDiv);
    chatMessages.scrollTop = chatMessages.scrollHeight;
}

function addLoadingMessage() {
    const welcome = chatMessages.querySelector('.welcome-screen');
    if (welcome) welcome.remove();

    const messageDiv = document.createElement('div');
    messageDiv.className = 'message assistant';
    messageDiv.id = 'loadingMessage';

    const innerDiv = document.createElement('div');
    innerDiv.className = 'message-inner';

    const avatar = document.createElement('div');
    avatar.className = 'message-avatar';
    avatar.innerHTML = '<svg viewBox="0 0 24 24" width="18" height="18" fill="white"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1 17.93c-3.95-.49-7-3.85-7-7.93 0-.62.08-1.21.21-1.79L9 15v1c0 1.1.9 2 2 2v1.93zm6.9-2.54c-.26-.81-1-1.39-1.9-1.39h-1v-3c0-.55-.45-1-1-1H8v-2h2c.55 0 1-.45 1-1V7h2c1.1 0 2-.9 2-2v-.41c2.93 1.19 5 4.06 5 7.41 0 2.08-.8 3.97-2.1 5.39z"/></svg>';

    const loadingDiv = document.createElement('div');
    loadingDiv.className = 'loading';
    loadingDiv.innerHTML = '<span></span><span></span><span></span>';

    innerDiv.appendChild(avatar);
    innerDiv.appendChild(loadingDiv);
    messageDiv.appendChild(innerDiv);
    chatMessages.appendChild(messageDiv);
    chatMessages.scrollTop = chatMessages.scrollHeight;
}

function removeLoadingMessage() {
    const loading = document.getElementById('loadingMessage');
    if (loading) loading.remove();
}

// Session List
function renderSessionList() {
    if (sessions.length === 0) {
        historyList.innerHTML = '<div class="session-empty">暂无历史记录</div>';
        return;
    }
    historyList.innerHTML = '';
    sessions.forEach(session => {
        const div = document.createElement('div');
        div.className = 'session-item' + (session.id === currentSessionId ? ' active' : '');

        const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
        svg.setAttribute('viewBox', '0 0 24 24');
        svg.innerHTML = '<path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>';

        const contentDiv = document.createElement('div');
        contentDiv.className = 'session-item-content';

        const titleDiv = document.createElement('div');
        titleDiv.className = 'session-item-title';
        titleDiv.textContent = session.title || '新对话';

        const deleteBtn = document.createElement('div');
        deleteBtn.className = 'session-delete';
        deleteBtn.innerHTML = '<svg viewBox="0 0 24 24"><path d="M18 6L6 18M6 6l12 12"/></svg>';
        deleteBtn.onclick = (e) => {
            e.stopPropagation();
            showDeleteConfirm(session.id);
        };

        div.onclick = () => switchSession(session.id);

        contentDiv.appendChild(titleDiv);
        div.appendChild(svg);
        div.appendChild(contentDiv);
        div.appendChild(deleteBtn);
        historyList.appendChild(div);
    });
}

// Delete Confirmation Modal
function showDeleteConfirm(sessionId) {
    const session = sessions.find(s => s.id === sessionId);
    const title = session ? (session.title || '新对话') : '此对话';

    const overlay = document.createElement('div');
    overlay.className = 'modal-overlay';
    overlay.innerHTML = `
        <div class="modal-box">
            <div class="modal-title">删除对话？</div>
            <div class="modal-desc">确定要删除「${title}」吗？此操作无法撤销。</div>
            <div class="modal-actions">
                <button class="modal-btn modal-btn-cancel">取消</button>
                <button class="modal-btn modal-btn-delete">删除</button>
            </div>
        </div>
    `;

    document.body.appendChild(overlay);

    const cancelBtn = overlay.querySelector('.modal-btn-cancel');
    const deleteBtn = overlay.querySelector('.modal-btn-delete');

    cancelBtn.onclick = () => overlay.remove();
    overlay.onclick = (e) => { if (e.target === overlay) overlay.remove(); };

    deleteBtn.onclick = async () => {
        await deleteSession(sessionId);
        overlay.remove();
    };
}

// API Functions
async function switchSession(sessionId) {
    currentSessionId = sessionId;
    saveCurrentSession();
    renderSessionList();
    chatMessages.innerHTML = '';
    try {
        const response = await fetch('/ai/sessions/' + sessionId + '/messages');
        const messages = await response.json();
        if (messages.length === 0) {
            showWelcome();
        } else {
            messages.forEach(msg => {
                addMessage(msg.userMessage, true);
                addMessage(msg.assistantReply, false);
            });
        }
    } catch (error) {
        console.error('Failed to load session messages:', error);
        showWelcome();
    }
    if (window.innerWidth <= 768) toggleSidebar();
    messageInput.focus();
}

async function createNewSession() {
    try {
        const response = await fetch('/ai/sessions', { method: 'POST' });
        const session = await response.json();
        currentSessionId = session.id;
        showWelcome();
        await loadSessions(false);
    } catch (error) {
        console.error('Failed to create session:', error);
    }
}

async function clearChat() {
    await createNewSession();
    if (window.innerWidth <= 768) toggleSidebar();
    messageInput.focus();
}

async function deleteSession(sessionId) {
    try {
        await fetch('/ai/sessions/' + sessionId, { method: 'DELETE' });
        sessions = sessions.filter(s => s.id !== sessionId);
        renderSessionList();
        if (sessionId === currentSessionId) {
            if (sessions.length > 0) {
                await switchSession(sessions[0].id);
            } else {
                currentSessionId = null;
                saveCurrentSession();
                showWelcome();
            }
        }
    } catch (error) {
        console.error('Failed to delete session:', error);
    }
}

async function sendMessage() {
    const message = messageInput.value.trim();
    if (!message) return;

    sendBtn.disabled = true;
    messageInput.value = '';
    messageInput.style.height = 'auto';

    if (!currentSessionId) {
        try {
            const res = await fetch('/ai/sessions', { method: 'POST' });
            const session = await res.json();
            currentSessionId = session.id;
        } catch (error) {
            addMessage('创建会话失败，请重试。', false);
            sendBtn.disabled = false;
            return;
        }
    }

    addMessage(message, true);
    addLoadingMessage();

    try {
        const response = await fetch('/ai/chat', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ message: message, sessionId: currentSessionId })
        });
        
        // 先检查响应状态
        if (!response.ok) {
            throw new Error('HTTP ' + response.status + ': ' + response.statusText);
        }
        
        // 先获取原始文本，再解析 JSON
        const text = await response.text();
        console.log('Raw response:', text.substring(0, 500));
        
        let data;
        try {
            data = JSON.parse(text);
        } catch (parseError) {
            console.error('JSON parse error:', parseError);
            throw new Error('JSON解析失败: ' + parseError.message);
        }
        
        removeLoadingMessage();
        
        // 处理不同的响应格式
        if (data.reply) {
            addMessage(data.reply, false);
        } else if (data.error && data.error.message) {
            addMessage('错误: ' + data.error.message, false);
        } else {
            addMessage('收到未知格式的响应', false);
            console.log('Unknown response:', data);
        }
        await loadSessions(false);
    } catch (error) {
        removeLoadingMessage();
        console.error('Chat error:', error);
        addMessage('抱歉，服务暂时不可用，请稍后再试。错误: ' + error.message, false);
    }

    sendBtn.disabled = false;
    messageInput.focus();
}

async function loadSessions(restoreLatest = true) {
    try {
        const response = await fetch('/ai/sessions');
        sessions = await response.json();
        renderSessionList();
        
        if (restoreLatest) {
            // Try to restore saved session first
            const savedSessionId = getSavedSessionId();
            const savedSessionExists = savedSessionId && sessions.some(s => s.id == savedSessionId);
            
            if (savedSessionExists) {
                await switchSession(parseInt(savedSessionId));
            } else if (sessions.length > 0) {
                await switchSession(sessions[0].id);
            } else {
                showWelcome();
            }
        } else {
            renderSessionList();
        }
    } catch (error) {
        console.error('Failed to load sessions:', error);
        showWelcome();
    }
}

// Start
messageInput.focus();
loadSessions();
