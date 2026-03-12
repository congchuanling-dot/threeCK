# AI 三国杀 - 前端

Vue 3 + Vite + Tailwind CSS 对战页原型，与后端 WebSocket/HTTP 对接。

## 功能

- **游戏大厅**：武将描述输入框 +「AI 生成武将」按钮；连接服务器、创建房间、开始游戏。
- **战局主界面**：左侧战局记录、中间战场区+底部手牌区（hover 放大）、右侧玩家列表（头像/血量/手牌数）。
- **状态与交互**：WebSocket 接收 `ROOM_CREATED` / `PLAYER_JOINED` / `PHASE_CHANGED` / `BROADCAST`；出牌时调用 `POST /api/game/:roomId/play`。
- **武将预览**：AI 生成武将成功后弹出武将卡片弹窗（技能描述 + 图片占位）。

## 运行

```bash
cd frontend
npm install
npm run dev
```

开发环境：前端 `http://localhost:5173`，需后端 `http://localhost:8080` 运行（含 `/ws` 与 `/api/game/*`）。  
Vite 已配置 `/api` 代理到 8080；WebSocket 直连 `ws://localhost:8080/ws`（见 `useGameSocket.js`）。

## 关键文件

- `src/App.vue`：布局、大厅/战局切换、连接/创建房间/开局、出牌请求、武将弹窗。
- `src/composables/useGameSocket.js`：WebSocket 连接、消息解析、房间/玩家/阶段/日志/手牌状态。
- `src/components/Card.vue`：单张卡牌展示、hover 放大、可出牌时高亮、`@play` 事件。
- `src/components/Player.vue`：玩家项（头像、昵称、血量、手牌数、当前回合标识）。
- `src/components/GeneralPreviewModal.vue`：武将卡片预览弹窗（技能描述、图片）。
- `src/components/GameLog.vue`、`BattleArea.vue`、`HandArea.vue`、`PlayerList.vue`、`LobbySection.vue`。

## 出牌流程

1. 后端推送 `PHASE_CHANGED`，阶段为 `PLAY` 时手牌可点击。
2. 用户点击手牌 → `HandArea` 触发 `@play(card)` → `App.vue` 调用 `POST /api/game/{roomId}/play`，body `{ playerId, cardId }`。
3. 请求成功后本地从 `myHandCards` 移除该牌；后端通过 WebSocket 广播 `PLAY_CARD`，其他端可更新战场区与日志。
