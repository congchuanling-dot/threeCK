# 三国杀风格回合制卡牌游戏 - 后端核心引擎

基于 **Spring Boot 3** + **Netty**（WebFlux 默认嵌入式服务器）的回合制卡牌游戏后端，提供领域模型、状态机、游戏上下文、事件驱动架构与 WebSocket 通信。

## 技术栈

- Java 17+
- Spring Boot 3.2
- Spring WebFlux（Reactor Netty）
- WebSocket（Reactive）
- Jackson

## 项目结构

```
src/main/java/com/game/
├── ThreeKingdomsGameApplication.java   # 启动类
├── domain/                             # 领域模型
│   ├── Suit.java                       # 花色
│   ├── Phase.java                      # 阶段：准备、摸牌、出牌、弃牌、结束
│   ├── Card.java                       # 卡牌
│   ├── Player.java                     # 玩家
│   └── GameRoom.java                   # 房间
├── engine/                             # 核心引擎
│   ├── GameContext.java                # 游戏上下文（牌堆、弃牌堆、当前玩家、阶段）
│   ├── GameStateMachine.java          # 回合阶段状态机
│   └── DefaultGameStarter.java        # 默认开局（建牌堆、发牌、阶段事件）
├── event/                              # 事件驱动
│   ├── GameEvent.java                  # 事件标记接口
│   ├── GameEventListener.java         # 监听器接口
│   ├── GameEventPublisher.java        # 事件发布器
│   ├── PlayerPlayCardEvent.java       # 出牌事件
│   ├── PlayerDamageEvent.java         # 受到伤害事件
│   ├── PhaseChangeEvent.java          # 阶段切换事件
│   ├── DefaultPlayerPlayCardListener.java
│   ├── DefaultPlayerDamageListener.java
│   └── GameEventConfig.java           # 注册所有监听器
├── service/
│   └── RoomService.java               # 房间与对局管理（创建/加入/开局/上下文）
├── websocket/
│   ├── WebSocketConfig.java           # /ws 映射
│   ├── GameWebSocketHandler.java      # 连接、创建房间、加入、广播
│   ├── GameMessage.java               # 消息 DTO（ROOM_CREATED, GAME_CONTEXT, DRAWN_CARDS, YOUR_HAND 等）
│   ├── GameContextDTO.java            # 全量状态快照（状态强同步）
│   └── WebSocketSessionStore.java     # session 存储，用于推送
└── controller/
    └── GameController.java            # HTTP：开局、推进阶段、出牌
```

## 运行

```bash
mvn spring-boot:run
```

服务默认端口 `8080`。WebSocket 地址：`ws://localhost:8080/ws`。

## WebSocket 消息格式

- **创建房间**（客户端发送）  
  `{"action":"create_room","roomName":"房间名","maxPlayers":4,"nickname":"房主昵称"}`  
  服务端回复：`{"type":"ROOM_CREATED","payload":{"roomId":"R10001","roomName":"...","maxPlayers":4}}`

- **加入房间**（客户端发送）  
  `{"action":"join","roomId":"R10001","playerId":"玩家ID","nickname":"昵称"}`  
  服务端回复：`{"type":"PLAYER_JOINED","payload":{"roomId":"...","playerId":"...","nickname":"...","seatIndex":0}}`  
  并广播给同房间其他连接。

- **阶段变化**（服务端推送）  
  `{"type":"PHASE_CHANGED","payload":{"phase":"DRAW","currentSeatIndex":0,"roundNumber":1}}`

- **出牌广播**  
  `{"type":"BROADCAST","payload":{"messageType":"PLAY_CARD","data":{"playerId":"...","cardId":"...","cardType":"杀"}}}`

- **全量游戏上下文（状态强同步）**  
  `{"type":"GAME_CONTEXT","payload":{"roomId":"...","currentPhase":"PLAY","currentSeatIndex":0,"roundNumber":1,"drawPileSize":40,"discardPileSize":0,"players":[...]}}`

- **摸牌推送（仅当前玩家）**  
  `{"type":"DRAWN_CARDS","payload":[{ "id":"...", "suit":"SPADE", "rankOrName":"杀" }, ...]}`

- **手牌全量（开局时每人一条）**  
  `{"type":"YOUR_HAND","payload":[...]}`

## 核心游戏逻辑

1. **开局**（仅房主）：52 张基础牌（杀 30、闪 12、桃 10），洗牌后每人发 4 张，当前操作者为座位 0，阶段为 DRAW；进入 DRAW 后自动摸 2 张并进入 PLAY。
2. **出牌阶段**：  
   - **杀**：需传 `targetId`，目标血量 -1，牌进弃牌堆。  
   - **桃**：可选 `targetId`（默认自己），目标血量 +1（不超过上限）。  
   - **闪**：仅弃置，无目标。
3. **结束回合**：当前玩家调用 `endRound` 后，权限交给下一存活玩家，阶段变为 DRAW，再自动摸 2 张进入 PLAY。任何状态变化后都会广播 `GAME_CONTEXT`，摸牌时另向该玩家推送 `DRAWN_CARDS`。

## HTTP 接口示例

- `POST /api/game/{roomId}/start` — 开局（仅房主），Body：`{"playerId":"房主ID"}`。初始化牌堆、发 4 张、自动摸 2 张进入 PLAY，并推送 GAME_CONTEXT + YOUR_HAND + DRAWN_CARDS。
- `POST /api/game/{roomId}/play` — 出牌，Body：`{"playerId":"...","cardId":"...","targetId":"..."}`（杀必填 targetId，桃可选）。
- `POST /api/game/{roomId}/endRound` — 结束回合，Body：`{"playerId":"当前玩家ID"}`。
- `POST /api/game/{roomId}/advance` — 推进阶段（兼容用）。

## 扩展：事件监听

实现 `GameEventListener<E>` 并注册为 Spring Bean，会在 `GameEventConfig` 中自动注册到 `GameEventPublisher`。  
支持事件类型：`PLAYER_PLAY_CARD`、`PLAYER_DAMAGE`、`PHASE_CHANGE` 等，可自行发布新事件类型并编写监听器（如技能、濒死结算）。

## 配置

`src/main/resources/application.properties` 可修改端口、日志级别等。
