import { ref, shallowRef } from 'vue'

const WS_BASE = import.meta.env.DEV
  ? `${location.protocol === 'https:' ? 'wss:' : 'ws:'}//${location.hostname}:8080`
  : `${location.protocol === 'https:' ? 'wss:' : 'ws:'}//${location.hostname}${location.port ? ':' + location.port : ''}`

/**
 * 游戏 WebSocket 与状态管理。
 * 连接 /ws，接收 ROOM_CREATED / PLAYER_JOINED / PHASE_CHANGED / BROADCAST，维护 gameContext 与日志。
 * 页面重新可见时会通过 visibilitychange 拉取最新状态，解决后台标签页 WebSocket 被节流导致的回合不同步。
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
  const killArrow = ref(null) // 出杀时箭头 { fromPlayerId, toPlayerId }，约1.5秒后自动清除
  const cardFly = ref(null) // 卡牌飞行动画 { card: { id, rankOrName }, fromPlayerId }，用于指向性出牌效果
  const pendingKill = ref(null) // 待响应的杀 { targetId, sourceId, sourceName, targetName, amount }
  const pendingDeath = ref(null) // 濒死轮询 { targetId, targetName, askingSeatIndex }
  const ws = shallowRef(null)

  /** 从服务器拉取最新状态（解决标签页在后台时 WebSocket 消息被节流导致的不同步） */
  async function syncStateFromServer() {
    const rid = roomId.value
    const pid = myPlayerId.value
    if (!rid || !pid) return
    try {
      const res = await fetch(`/api/game/${rid}/state?playerId=${encodeURIComponent(pid)}`)
      const data = await res.json()
      if (data?.ok) applyGameState(data)
    } catch (e) {
      console.warn('syncStateFromServer failed', e)
    }
  }

  function handleVisibilityChange() {
    if (document.visibilityState === 'visible' && roomId.value && myPlayerId.value) {
      syncStateFromServer()
    }
  }

  function connect() {
    if (ws.value?.readyState === WebSocket.OPEN) return
    document.removeEventListener('visibilitychange', handleVisibilityChange)
    document.addEventListener('visibilitychange', handleVisibilityChange)
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
          pendingKill.value = payload?.pendingKill ?? null
          pendingDeath.value = payload?.pendingDeath ?? null
          if (Array.isArray(payload?.battleCards)) battleCards.value = payload.battleCards
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
            const cardType = data.cardType || '?'
            addLog(p?.nickname ?? data.playerId, `打出了 ${cardType}`)
            // 卡牌飞行动画：从出牌者飞向中央牌区
            cardFly.value = {
              card: { id: data.cardId, rankOrName: cardType },
              fromPlayerId: data.playerId,
            }
            if (data.cardType === '杀' && data.targetId) {
              killArrow.value = { fromPlayerId: data.playerId, toPlayerId: data.targetId }
              setTimeout(() => { killArrow.value = null }, 1600)
              // 若目标是本人，主动同步状态，确保 pendingKill 正确显示（防止 GAME_CONTEXT 丢失或乱序）
              if (data.targetId === myPlayerId.value) {
                syncStateFromServer()
              }
            }
          } else if (messageType === 'DAMAGE' && data) {
            addLog(data.sourceName ?? '未知', `对 ${data.targetName ?? data.targetId} 造成 ${data.amount ?? 0} 点伤害`)
          } else if (messageType === 'SHAN_NEGATED' && data) {
            const name = players.value.find(x => x.playerId === data.targetId)?.nickname ?? data.targetId
            addLog(name ?? '对方', data.message || '出闪抵消了杀')
          } else if (messageType === 'PLAYER_DEATH' && data) {
            addLog('系统', `${data.targetName ?? data.targetId} 阵亡`)
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

  function createRoom(roomName, nickname, botCount = 1) {
    const count = Math.max(1, Math.min(7, botCount ?? 1))
    send({ action: 'create_room', roomName, botCount: count, nickname: nickname || '房主' })
  }

  function joinRoom(rid, pid, nickname) {
    myPlayerId.value = pid
    send({ action: 'join', roomId: rid, playerId: pid, nickname: nickname || pid })
  }

  /** 同步本地游戏状态（HTTP 返回的 hand/players/phase 等） */
  function applyGameState(data) {
    if (!data) return
    if (Array.isArray(data.players)) players.value = data.players
    if (data.phase != null) currentPhase.value = data.phase
    if (typeof data.currentSeatIndex === 'number') currentSeatIndex.value = data.currentSeatIndex
    if (typeof data.roundNumber === 'number') roundNumber.value = data.roundNumber
    pendingKill.value = data.pendingKill ?? null
    pendingDeath.value = data.pendingDeath ?? null
    if (Array.isArray(data.battleCards)) battleCards.value = data.battleCards
    // hand 仅当当前玩家是自己时更新（服务端返回的是当前玩家的手牌）
    const curPlayer = data.players?.find((p) => p.seatIndex === data.currentSeatIndex)
    if (curPlayer && curPlayer.playerId === myPlayerId.value && Array.isArray(data.hand)) {
      myHandCards.value = data.hand
    }
  }

  function disconnect() {
    document.removeEventListener('visibilitychange', handleVisibilityChange)
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

  function clearCardFly() {
    cardFly.value = null
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
    killArrow,
    cardFly,
    clearCardFly,
    connect,
    send,
    createRoom,
    joinRoom,
    applyGameState,
    addLog,
    disconnect,
    pendingKill,
    pendingDeath,
  }
}
