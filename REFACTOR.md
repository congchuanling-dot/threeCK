# 企业级重构说明

## 一、前端重构

### 1. 新增目录结构

```
frontend/src/
├── api/
│   ├── client.js       # 统一 HTTP 客户端（fetch 封装、错误处理）
│   └── gameApi.js      # 游戏 API 封装（listGenerals, start, play, respond...）
├── constants/
│   ├── cardTypes.js    # 牌型常量 CARD_TYPES.SHA/SHAN/TAO
│   └── config.js       # 配置常量（超时、日志条数等）
├── utils/
│   └── cardUtils.js    # 牌工具函数（getCardType, isSha, normalizeCard）
├── composables/
│   ├── useGameSocket.js   # 已有：WebSocket + 状态
│   ├── useGameActions.js  # 新增：所有 HTTP 游戏动作
│   └── useBattleCards.js  # 新增：中央出牌区 5s 消失逻辑
└── App.vue             # 瘦身：仅 UI 编排 + 调用 composables
```

### 2. API 层

- **client.js**：`get(url)`、`post(url, body)`，统一 `Content-Type`、`res.json()` 解析、非 2xx 抛错
- **gameApi.js**：`gameApi.start(roomId, { playerId, generalId })` 等，路径集中维护，便于 Mock 与切换环境

### 3. 常量与工具

- **CARD_TYPES**：`SHA`、`SHAN`、`TAO`，避免魔法字符串
- **cardUtils**：`getCardType(card)`、`isSha(card)`、`normalizeCard(item)`，统一牌型判断与规范化

### 4. Composable 拆分

- **useGameActions(game)**：`startGame`、`playCard`、`acceptDamage`、`respondDyingTao`、`respondDyingPass`、`endRound`，统一 `handleApiResult` 与 `game.addLog`
- **useBattleCards(game)**：`displayBattleCards`，牌 5 秒后消失，`BATTLE_CARD_DISPLAY_MS` 可配置

### 5. App.vue 变化

- 移除重复 `fetch` 与 `handleApiResult` 逻辑
- 使用 `actions.playCard`、`actions.acceptDamage` 等
- 使用 `useBattleCards` 替代内联 watch
- 使用 `normalizeCard`、`CARD_TYPES` 减少魔法字符串

---

## 二、后端重构

### 1. 新增常量类

```
src/main/java/com/game/constants/
├── CardType.java       # 牌型常量：SHA="杀", SHAN="闪", TAO="桃"
└── GameConstants.java  # 游戏常量：DEFAULT_HP, DEFAULT_INITIAL_HAND_SIZE
```

### 2. 使用方式

- `DefaultGameStarter` 已改为使用 `CardType.SHA`、`CardType.SHAN`、`CardType.TAO`
- 其他类（`BotService`、`GameController`、`LongdanSkill` 等）可逐步替换 `"杀"`、`"闪"`、`"桃"` 为 `CardType.*`

### 3. 扩展建议

- **技能系统**：将 Controller 中硬编码的 `"longdan"` 抽成策略/责任链，通过注册扩展
- **Service 分层**：将出牌、响应、濒死等业务下沉到 Service，Controller 只做参数校验与编排
- **DTO/类型**：为 API 定义 DTO，便于前后端契约与校验

---

## 三、可复用性与可扩展性

| 维度     | 改进点                                       |
|----------|----------------------------------------------|
| API 调用 | 统一封装，易 Mock、换环境、加拦截器          |
| 牌型判断 | 常量 + 工具函数，一处修改全局生效            |
| 游戏操作 | composable 抽离，其他页面可复用              |
| 配置     | 前端 config.js、后端 GameConstants 集中管理 |
| 技能     | 后端可按策略模式扩展，注册新技能             |

---

## 四、后续可做

1. 将 `useGameSocket` 中 WS 地址改为从 `config.js` 读取
2. 后端全面替换魔法字符串为 `CardType`、`GameConstants`
3. 补充单元测试（API 层、cardUtils、useGameActions）
4. 定义 OpenAPI / 类型（如引入 TypeScript 或 JSDoc）
