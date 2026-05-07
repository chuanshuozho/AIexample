# Chat History Restore Bugfix Design

## Overview

页面刷新后，聊天主区域不恢复历史对话内容，侧边栏点击记录也只加载单条消息。根本原因是前端 `loadChatHistory()` 仅调用 `renderHistoryList()` 更新侧边栏，未将消息渲染到聊天主区域；`loadConversation(item)` 只渲染传入的单条 `item`（一问一答），而非全部历史。修复方向为纯前端 JavaScript 逻辑修改，后端 `/ai/history` 接口已正确返回按 `createTime` 升序排列的所有记录。

## Glossary

- **Bug_Condition (C)**: 页面加载或点击侧边栏历史记录时，聊天主区域未正确渲染全部历史消息的条件
- **Property (P)**: 页面加载时聊天主区域应显示所有历史消息；点击侧边栏记录时应加载全部历史消息
- **Preservation**: 发送新消息、新对话按钮、侧边栏列表渲染、空状态显示等现有行为不受影响
- **loadChatHistory()**: `index.html` 中的异步函数，页面加载时调用 `/ai/history` 获取历史数据并更新侧边栏
- **loadConversation(item)**: `index.html` 中的函数，点击侧边栏历史记录时加载对话到聊天主区域
- **chatHistory**: 前端全局数组，存储从 `/ai/history` 获取的所有 `ChatMessage` 对象
- **ChatMessage**: 后端实体，包含 `id`、`userMessage`、`assistantReply`、`createTime` 字段

## Bug Details

### Bug Condition

当页面加载（刷新/重新打开）且数据库中存在历史聊天记录时，`loadChatHistory()` 只调用 `renderHistoryList()` 更新侧边栏列表，不将任何消息渲染到聊天主区域。当用户点击侧边栏某条记录时，`loadConversation(item)` 只渲染该单条记录的 `userMessage` 和 `assistantReply`，而非 `chatHistory` 中的全部消息。

**Formal Specification:**
```
FUNCTION isBugCondition(input)
  INPUT: input of type { trigger: 'pageLoad' | 'sidebarClick', chatHistory: ChatMessage[] }
  OUTPUT: boolean

  IF input.trigger == 'pageLoad' THEN
    RETURN chatHistory.length > 0
           AND chatMainArea.displayedMessages.length == 0
  END IF

  IF input.trigger == 'sidebarClick' THEN
    RETURN chatHistory.length > 1
           AND chatMainArea.displayedMessages.length == 2  // only 1 user + 1 AI
           AND chatHistory.length * 2 != chatMainArea.displayedMessages.length
  END IF

  RETURN false
END FUNCTION
```

### Examples

- **页面刷新，数据库有 5 条记录**: 期望聊天主区域显示 10 条消息（5 条用户 + 5 条 AI），实际显示空白 `emptyState`
- **点击侧边栏第 3 条记录**: 期望聊天主区域显示全部 5 条记录的所有消息，实际只显示第 3 条的 1 条用户消息 + 1 条 AI 回复
- **页面刷新，数据库为空**: 期望显示 `emptyState`（"开始对话，体验智能客服"），实际行为正确（非 bug 条件）
- **发送新消息后**: 期望消息正常发送并显示，`loadChatHistory()` 被调用刷新侧边栏，实际行为正确（非 bug 条件）

## Expected Behavior

### Preservation Requirements

**Unchanged Behaviors:**
- 用户发送新消息时，消息通过 `/ai/chat` 发送到后端、保存到数据库、在聊天主区域显示用户消息和 AI 回复的流程不变
- 点击"新对话"按钮时，清空聊天主区域并显示 `emptyState` 的行为不变
- 侧边栏历史记录列表按时间倒序显示摘要的行为不变
- 数据库为空时页面加载显示 `emptyState` 的行为不变
- 后端 `/ai/history` 接口返回按 `createTime` 升序排列的所有记录的行为不变
- `addMessage(content, isUser)` 函数的渲染逻辑（Markdown 解析、代码高亮等）不变
- 移动端侧边栏切换行为不变

**Scope:**
所有不涉及页面加载历史恢复和侧边栏点击加载全部消息的输入应完全不受此修复影响。包括：
- 用户输入新消息并发送
- 点击"新对话"按钮
- 移动端侧边栏开关
- 文本框自动调整高度
- Markdown 渲染和代码高亮

## Hypothesized Root Cause

基于源码分析，根因明确：

1. **`loadChatHistory()` 缺少主区域渲染逻辑**: 函数体仅包含 `renderHistoryList()` 调用，获取到 `chatHistory` 数据后没有遍历数组将消息渲染到 `chatMessages` 容器中。这是页面刷新后聊天主区域为空的直接原因。

   ```javascript
   // 当前代码 — 缺少渲染到主区域的逻辑
   async function loadChatHistory() {
       const response = await fetch('/ai/history');
       chatHistory = await response.json();
       renderHistoryList();  // ← 只更新侧边栏
       // ← 缺少：遍历 chatHistory 并调用 addMessage() 渲染到主区域
   }
   ```

2. **`loadConversation(item)` 只处理单条记录**: 函数接收单个 `item` 参数，只调用两次 `addMessage()`（一次用户消息、一次 AI 回复）。由于数据库没有会话分组概念，所有消息属于同一对话流，点击任意记录应加载全部历史。

   ```javascript
   // 当前代码 — 只加载单条
   function loadConversation(item) {
       chatMessages.innerHTML = '';
       addMessage(item.userMessage, true);      // ← 只加载 item 的用户消息
       addMessage(item.assistantReply, false);   // ← 只加载 item 的 AI 回复
   }
   ```

## Correctness Properties

Property 1: Bug Condition - 页面加载时历史消息渲染到聊天主区域

_For any_ 页面加载事件，当 `/ai/history` 返回的 `chatHistory` 数组长度 > 0 时，修复后的 `loadChatHistory()` 函数 SHALL 遍历所有历史记录，按时间顺序将每条记录的 `userMessage` 和 `assistantReply` 渲染到聊天主区域，并隐藏 `emptyState`。渲染后主区域的消息数量 SHALL 等于 `chatHistory.length * 2`。

**Validates: Requirements 2.1, 2.3**

Property 2: Bug Condition - 侧边栏点击加载全部历史消息

_For any_ 侧边栏历史记录点击事件，修复后的 `loadConversation(item)` 函数 SHALL 加载 `chatHistory` 中的全部记录（而非仅传入的 `item`），将所有 `userMessage` 和 `assistantReply` 按时间顺序渲染到聊天主区域。渲染后主区域的消息数量 SHALL 等于 `chatHistory.length * 2`。

**Validates: Requirements 2.2**

Property 3: Preservation - 现有功能行为不变

_For any_ 不涉及页面加载历史恢复和侧边栏点击加载的操作（发送新消息、点击新对话、空数据库页面加载等），修复后的代码 SHALL 产生与原始代码完全相同的行为，保留所有现有功能的正确性。

**Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5**

## Fix Implementation

### Changes Required

**File**: `Ai_example/AI_example/src/main/resources/static/index.html`

**Function 1**: `loadChatHistory()`

**Specific Changes**:
1. **添加主区域渲染逻辑**: 在 `renderHistoryList()` 调用之后，检查 `chatHistory.length > 0`，若有数据则清空 `chatMessages`，遍历 `chatHistory` 数组，对每条记录调用 `addMessage(item.userMessage, true)` 和 `addMessage(item.assistantReply, false)`
2. **处理空状态**: 若 `chatHistory.length === 0`，保持 `emptyState` 显示不变

```javascript
async function loadChatHistory() {
    try {
        const response = await fetch('/ai/history');
        chatHistory = await response.json();
        renderHistoryList();

        // 新增：将历史消息渲染到聊天主区域
        if (chatHistory.length > 0) {
            chatMessages.innerHTML = '';
            chatHistory.forEach(item => {
                addMessage(item.userMessage, true);
                addMessage(item.assistantReply, false);
            });
        }
    } catch (error) {
        console.error('Failed to load history:', error);
    }
}
```

**Function 2**: `loadConversation(item)`

**Specific Changes**:
1. **加载全部历史消息**: 将原来只加载单条 `item` 的逻辑改为遍历 `chatHistory` 数组，渲染所有记录
2. **高亮当前选中项**（可选增强）: 可在侧边栏中标记当前点击的记录

```javascript
function loadConversation(item) {
    chatMessages.innerHTML = '';
    chatHistory.forEach(msg => {
        addMessage(msg.userMessage, true);
        addMessage(msg.assistantReply, false);
    });

    if (window.innerWidth <= 900) {
        toggleSidebar();
    }
}
```

**注意事项**:
- `sendMessage()` 中调用 `loadChatHistory()` 刷新侧边栏的逻辑需要调整：发送新消息后不应重新渲染整个主区域（因为新消息已经通过 `addMessage()` 添加了），只需更新侧边栏。可通过添加一个参数控制是否渲染主区域，或将侧边栏刷新逻辑独立为 `refreshSidebar()` 函数。

```javascript
// 方案：为 loadChatHistory 添加 renderMain 参数
async function loadChatHistory(renderMain = true) {
    try {
        const response = await fetch('/ai/history');
        chatHistory = await response.json();
        renderHistoryList();

        if (renderMain && chatHistory.length > 0) {
            chatMessages.innerHTML = '';
            chatHistory.forEach(item => {
                addMessage(item.userMessage, true);
                addMessage(item.assistantReply, false);
            });
        }
    } catch (error) {
        console.error('Failed to load history:', error);
    }
}

// sendMessage() 中调用时传 false，避免重复渲染
loadChatHistory(false);
```

## Testing Strategy

### Validation Approach

测试策略分两阶段：首先在未修复代码上验证 bug 存在（探索性测试），然后在修复后验证 bug 已解决且现有行为未被破坏。

### Exploratory Bug Condition Checking

**Goal**: 在实施修复前，通过测试用例确认 bug 的存在并验证根因分析。

**Test Plan**: 模拟页面加载和侧边栏点击场景，断言聊天主区域的消息渲染状态。在未修复代码上运行以观察失败。

**Test Cases**:
1. **页面加载测试**: 模拟 `/ai/history` 返回 3 条记录，调用 `loadChatHistory()`，断言 `chatMessages` 中有 6 个消息 DOM 元素（将在未修复代码上失败）
2. **侧边栏点击测试**: 设置 `chatHistory` 为 3 条记录，调用 `loadConversation(chatHistory[1])`，断言 `chatMessages` 中有 6 个消息 DOM 元素（将在未修复代码上失败，只有 2 个）
3. **空历史测试**: 模拟 `/ai/history` 返回空数组，调用 `loadChatHistory()`，断言 `emptyState` 可见（在未修复代码上应通过）
4. **消息内容验证**: 模拟 `/ai/history` 返回已知内容的记录，断言渲染的消息文本与数据匹配

**Expected Counterexamples**:
- `loadChatHistory()` 调用后 `chatMessages` 子元素数量为 1（仅 `emptyState`），而非预期的 `chatHistory.length * 2`
- `loadConversation(item)` 调用后 `chatMessages` 子元素数量为 2，而非预期的 `chatHistory.length * 2`

### Fix Checking

**Goal**: 验证修复后，所有满足 bug 条件的输入都能产生正确行为。

**Pseudocode:**
```
FOR ALL input WHERE isBugCondition(input) DO
  IF input.trigger == 'pageLoad' THEN
    result := loadChatHistory_fixed()
    ASSERT chatMessages.children.length == chatHistory.length * 2
    ASSERT emptyState.style.display == 'none'
    FOR EACH item IN chatHistory DO
      ASSERT messageAt(item.index * 2).text == item.userMessage
      ASSERT messageAt(item.index * 2 + 1).html == marked.parse(item.assistantReply)
    END FOR
  END IF

  IF input.trigger == 'sidebarClick' THEN
    result := loadConversation_fixed(item)
    ASSERT chatMessages.children.length == chatHistory.length * 2
  END IF
END FOR
```

### Preservation Checking

**Goal**: 验证修复后，所有不满足 bug 条件的输入产生与原始代码相同的结果。

**Pseudocode:**
```
FOR ALL input WHERE NOT isBugCondition(input) DO
  ASSERT originalBehavior(input) == fixedBehavior(input)
END FOR
```

**Testing Approach**: 属性基测试适用于保留性检查，因为：
- 可自动生成大量测试用例覆盖输入域
- 能捕获手动单元测试可能遗漏的边界情况
- 对非 bug 输入的行为不变性提供强保证

**Test Plan**: 先在未修复代码上观察非 bug 输入的行为，然后编写测试确保修复后行为一致。

**Test Cases**:
1. **发送新消息保留**: 验证 `sendMessage()` 流程在修复后仍正确工作，新消息正常显示且不会触发主区域全量重渲染
2. **新对话按钮保留**: 验证 `clearChat()` 在修复后仍正确清空主区域并显示 `emptyState`
3. **空数据库保留**: 验证数据库为空时页面加载仍显示 `emptyState`
4. **侧边栏列表保留**: 验证 `renderHistoryList()` 仍按时间倒序显示历史记录摘要

### Unit Tests

- 测试 `loadChatHistory()` 在有历史数据时正确渲染所有消息到主区域
- 测试 `loadChatHistory()` 在无历史数据时保持 `emptyState` 显示
- 测试 `loadChatHistory(false)` 不渲染主区域（仅更新侧边栏）
- 测试 `loadConversation(item)` 加载全部历史消息而非单条
- 测试 `clearChat()` 在修复后仍正确工作
- 测试消息渲染顺序与 `chatHistory` 数组顺序一致

### Property-Based Tests

- 生成随机长度（0-100）的 `chatHistory` 数组，验证 `loadChatHistory()` 后主区域消息数量始终等于 `chatHistory.length * 2`（或 0 时显示 `emptyState`）
- 生成随机 `chatHistory` 数组，验证 `loadConversation()` 后主区域消息数量始终等于 `chatHistory.length * 2`
- 生成随机操作序列（发送消息、点击新对话、点击历史记录），验证每次操作后 UI 状态一致性

### Integration Tests

- 完整流程测试：发送消息 → 刷新页面 → 验证历史恢复 → 点击侧边栏 → 验证全部加载
- 连续操作测试：发送多条消息 → 刷新 → 验证所有消息恢复 → 点击新对话 → 验证清空 → 点击侧边栏记录 → 验证全部加载
- Markdown 渲染测试：发送包含代码块的消息 → 刷新 → 验证历史恢复后代码高亮正常
