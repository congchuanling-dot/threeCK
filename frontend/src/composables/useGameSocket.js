import { ref, shallowRef, onUnmounted } from 'vue'

const WS_BASE = import.meta.env.DEV
  ? `${location.protocol === 'https:' ? 'wss:' : 'ws:'}//${location.hostname}:8080`
  : `${location.protocol === 'https:' ? 'wss:' : 'ws:'}//${location.hostname}${location.port ? ':' + location.port : ''}`

/**
 * 游戏 WebSocket 与状态管理。
 * 连接 /ws，接收 ROOM_CREATED / PLAYER_JOINED / PHASE_CHANGED / BROADCAST，维护 gameContext 与日志。
 */
export function useGameSocket() {
  const connected = ref(false)
  const roomId = ref(null)
  const players = ref([])
  const myPlayerId = ref(null)
  const myHandCards = ref([])
  const currentPhase = ref('PREPARE')
  const currentSeatIndex = ref(0)
  const roundNumber = ref(1)
  const battleCards = ref([]) // 当前出的牌（最近几张）
  const gameLog = ref([])
  const ws = shallowRef(null)

  function connect() {
    if (ws.value?.readyState === WebSocket.OPEN) return
    const url = `${WS_BASE}/ws`
    const socket = new WebSocket(url)
    ws.value = socket

    socket.onopen = () => {
      connected.value = true
      addLog('系统', '已连接服务器')
    }

    socket.onclose = () => {
      connected.value = false
      addLog('系统', '连接已断开')
    }

    socket.onerror = (e) => {
      console.error('WebSocket error', e)
    }

    socket.onmessage = (event) => {
      try {
        const msg = JSON.parse(event.data)
        const { type, payload } = msg
        if (type === 'ROOM_CREATED') {
          roomId.value = payload?.roomId ?? null
          myPlayerId.value = payload?.ownerId ?? null
          if (myPlayerId.value) {
            pushPlayer({
              playerId: myPlayerId.value,
              nickname: payload?.ownerNickname ?? '房主',
              seatIndex: 0,
              hp: 4,
              maxHp: 4,
              handCount: 0,
            })
          }
          addLog('系统', `房间已创建：${payload?.roomName ?? payload?.roomId}`)
        } else if (type === 'PLAYER_JOINED') {
          pushPlayer(payload)
          addLog('系统', `${payload?.nickname ?? payload?.playerId} 加入了房间`)
        } else if (type === 'GAME_CONTEXT') {
          if (payload?.currentPhase) currentPhase.value = payload.currentPhase
          if (typeof payload?.currentSeatIndex === 'number') currentSeatIndex.value = payload.currentSeatIndex
          if (typeof payload?.roundNumber === 'number') roundNumber.value = payload.roundNumber
          if (Array.isArray(payload?.players)) {
            players.value = payload.players
            const me = payload.players.find(p => p.playerId === myPlayerId.value)
            if (me && Array.isArray(me.hand)) {
              myHandCards.value = me.hand
            }
          }
        } else if (type === 'YOUR_HAND') {
          myHandCards.value = Array.isArray(payload) ? payload : []
        } else if (type === 'DRAWN_CARDS') {
          if (Array.isArray(payload)) {
            myHandCards.value = [...myHandCards.value, ...payload]
          }
        } else if (type === 'BROADCAST' && payload) {
          const { messageType, data } = payload
          if (messageType === 'PLAY_CARD' && data) {
            battleCards.value = [...(battleCards.value.slice(-4)), { ...data }]
            const p = players.value.find(x => x.playerId === data.playerId)
            addLog(p?.nickname ?? data.playerId, `打出了手牌`)
          } else if (messageType === 'DAMAGE' && data) {
            addLog(data.sourceName ?? '未知', `对 ${data.targetName ?? data.targetId} 造成 ${data.amount ?? 0} 点伤害`)
          }
        }
      } catch (e) {
        console.warn('Parse WS message failed', e)
      }
    }
  }

  function pushPlayer(p) {
    if (!p?.playerId) return
    const idx = players.value.findIndex(x => x.playerId === p.playerId)
    const entry = {
      playerId: p.playerId,
      nickname: p.nickname ?? p.playerId,
      seatIndex: p.seatIndex ?? players.value.length,
      hp: p.hp ?? 4,
      maxHp: p.maxHp ?? 4,
      handCount: p.handCount ?? 0,
      avatar: p.avatar ?? null,
    }
    if (idx >= 0) players.value.splice(idx, 1, entry)
    else players.value.push(entry)
  }

  function addLog(who, text) {
    gameLog.value = [...gameLog.value, { who, text, time: new Date().toLocaleTimeString() }].slice(-100)
  }

  function send(obj) {
    if (ws.value?.readyState !== WebSocket.OPEN) return
    ws.value.send(JSON.stringify(obj))
  }

  function createRoom(roomName, maxPlayers = 4, nickname) {
    send({ action: 'create_room', roomName, maxPlayers, nickname: nickname || '房主' })
  }

  function joinRoom(rid, pid, nickname) {
    myPlayerId.value = pid
    send({ action: 'join', roomId: rid, playerId: pid, nickname: nickname || pid })
  }

  /** 同步本地游戏状态（可由 HTTP 拉取或服务端推送的 GameContext 更新） */
  function updateGameState(state) {
    if (state.players) players.value = state.players
    if (state.myHandCards) myHandCards.value = state.myHandCards
    if (state.currentPhase != null) currentPhase.value = state.currentPhase
    if (state.currentSeatIndex != null) currentSeatIndex.value = state.currentSeatIndex
    if (state.roundNumber != null) roundNumber.value = state.roundNumber
  }

  function disconnect() {
    if (ws.value) {
      ws.value.close()
      ws.value = null
    }
    connected.value = false
    roomId.value = null
    players.value = []
    myPlayerId.value = null
    myHandCards.value = []
  }

  return {
    connected,
    roomId,
    players,
    myPlayerId,
    myHandCards,
    currentPhase,
    currentSeatIndex,
    roundNumber,
    battleCards,
    gameLog,
    connect,
    send,
    createRoom,
    joinRoom,
    updateGameState,
    addLog,
    disconnect,
  }
}
