# Implementation Plan

- [ ] 1. Write bug condition exploration test
  - **Property 1: Bug Condition** - 页面加载与侧边栏点击未渲染全部历史消息
  - **CRITICAL**: This test MUST FAIL on unfixed code - failure confirms the bug exists
  - **DO NOT attempt to fix the test or the code when it fails**
  - **NOTE**: This test encodes the expected behavior - it will validate the fix when it passes after implementation
  - **GOAL**: Surface counterexamples that demonstrate the bug exists
  - **Scoped PBT Approach**: Scope the property to concrete failing cases:
    - Case A (pageLoad): Mock `/ai/history` returning N (N>0) ChatMessage records, call `loadChatHistory()`, assert `chatMessages` children count == N*2 (user + AI per record) and `emptyState` is hidden
    - Case B (sidebarClick): Set `chatHistory` to N (N>1) records, call `loadConversation(chatHistory[k])` for any k, assert `chatMessages` children count == N*2
  - Bug Condition from design: `isBugCondition(input)` — pageLoad with chatHistory.length > 0 renders 0 messages; sidebarClick with chatHistory.length > 1 renders only 2 messages
  - Expected Behavior from design: pageLoad should render chatHistory.length * 2 messages; sidebarClick should render chatHistory.length * 2 messages
  - Create test file: `Ai_example/AI_example/src/test/js/chatHistoryBugCondition.test.js` (or inline `<script>` test harness)
  - Since this is a pure frontend HTML file without a JS build system, use a lightweight approach: extract the JS functions into a testable format or use JSDOM/Node-based testing
  - Run test on UNFIXED code
  - **EXPECTED OUTCOME**: Test FAILS (this is correct - it proves the bug exists)
  - Document counterexamples: e.g., "loadChatHistory() with 3 records → chatMessages has 1 child (emptyState) instead of 6" and "loadConversation(chatHistory[1]) with 3 records → chatMessages has 2 children instead of 6"
  - Mark task complete when test is written, run, and failure is documented
  - _Requirements: 1.1, 1.2, 1.3, 2.1, 2.2, 2.3_

- [ ] 2. Write preservation property tests (BEFORE implementing fix)
  - **Property 2: Preservation** - 现有功能行为不变（发送消息、新对话、空历史、侧边栏列表）
  - **IMPORTANT**: Follow observation-first methodology
  - Observe on UNFIXED code:
    - `clearChat()` → `chatMessages` contains only `emptyState` with `display: flex`, `emptyState` text is "开始对话，体验智能客服"
    - `addMessage("hello", true)` → creates `.message.user` element with text "hello", hides `emptyState`
    - `addMessage("world", false)` → creates `.message.assistant` element with parsed Markdown content
    - `renderHistoryList()` with chatHistory of N items → `historyList` has N `.history-item` children, displayed in reverse chronological order
    - `loadChatHistory()` with empty `/ai/history` response → `emptyState` remains visible, `historyList` shows "暂无历史记录"
    - `loadChatHistory(false)` (after fix adds renderMain param) → should only update sidebar, not re-render main area
  - Write property-based tests capturing observed behavior patterns from Preservation Requirements:
    - For all calls to `clearChat()`: chatMessages contains emptyState with display flex
    - For all calls to `addMessage(content, true)`: a `.message.user` element is appended with correct text
    - For all calls to `addMessage(content, false)`: a `.message.assistant` element is appended with `marked.parse(content)` HTML
    - For all chatHistory arrays: `renderHistoryList()` produces historyList children count == chatHistory.length (or empty message when 0)
    - For empty chatHistory: `loadChatHistory()` keeps emptyState visible
  - Run tests on UNFIXED code
  - **EXPECTED OUTCOME**: Tests PASS (this confirms baseline behavior to preserve)
  - Mark task complete when tests are written, run, and passing on unfixed code
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

- [x] 3. Fix for chat history not restoring on page load and sidebar click only loading single message

  - [x] 3.1 Modify `loadChatHistory()` to accept `renderMain` parameter and render history to main area
    - Add `renderMain = true` default parameter to `loadChatHistory()`
    - After `renderHistoryList()`, when `renderMain` is true and `chatHistory.length > 0`: clear `chatMessages.innerHTML`, iterate `chatHistory` calling `addMessage(item.userMessage, true)` and `addMessage(item.assistantReply, false)` for each item
    - When `chatHistory.length === 0`, keep `emptyState` displayed (unchanged behavior)
    - Wrap in try/catch preserving existing error handling
    - _Bug_Condition: isBugCondition(input) where input.trigger == 'pageLoad' AND chatHistory.length > 0 AND chatMainArea.displayedMessages.length == 0_
    - _Expected_Behavior: After loadChatHistory(), chatMessages.children.length == chatHistory.length * 2, emptyState hidden_
    - _Preservation: Empty history still shows emptyState; renderHistoryList() still called_
    - _Requirements: 2.1, 2.3, 3.4_

  - [x] 3.2 Modify `loadConversation(item)` to load all history messages instead of single item
    - Replace single-item rendering (`addMessage(item.userMessage, true)` + `addMessage(item.assistantReply, false)`) with loop over entire `chatHistory` array
    - For each `msg` in `chatHistory`: call `addMessage(msg.userMessage, true)` and `addMessage(msg.assistantReply, false)`
    - Keep existing mobile sidebar toggle behavior
    - _Bug_Condition: isBugCondition(input) where input.trigger == 'sidebarClick' AND chatHistory.length > 1 AND only 2 messages rendered_
    - _Expected_Behavior: After loadConversation(), chatMessages.children.length == chatHistory.length * 2_
    - _Preservation: Mobile sidebar toggle unchanged_
    - _Requirements: 2.2_

  - [x] 3.3 Update `sendMessage()` to call `loadChatHistory(false)` to avoid re-rendering main area
    - Change `loadChatHistory()` call in `sendMessage()` to `loadChatHistory(false)`
    - This ensures sending a new message only refreshes the sidebar list, not the entire main chat area (since the new message is already displayed via `addMessage()`)
    - _Preservation: sendMessage flow unchanged — message sent, displayed, sidebar updated_
    - _Requirements: 3.1_

  - [x] 3.4 Verify bug condition exploration test now passes
    - **Property 1: Expected Behavior** - 页面加载时历史消息渲染到聊天主区域 & 侧边栏点击加载全部历史
    - **IMPORTANT**: Re-run the SAME test from task 1 - do NOT write a new test
    - The test from task 1 encodes the expected behavior
    - When this test passes, it confirms the expected behavior is satisfied
    - Run bug condition exploration test from step 1
    - **EXPECTED OUTCOME**: Test PASSES (confirms bug is fixed)
    - _Requirements: 2.1, 2.2, 2.3_

  - [x] 3.5 Verify preservation tests still pass
    - **Property 2: Preservation** - 现有功能行为不变
    - **IMPORTANT**: Re-run the SAME tests from task 2 - do NOT write new tests
    - Run preservation property tests from step 2
    - **EXPECTED OUTCOME**: Tests PASS (confirms no regressions)
    - Confirm all tests still pass after fix (no regressions)

- [ ] 4. Checkpoint - Ensure all tests pass
  - Run all tests (bug condition + preservation) together
  - Verify no regressions in existing functionality
  - Ensure all tests pass, ask the user if questions arise
