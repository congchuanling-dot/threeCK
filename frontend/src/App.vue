<template>
  <div class="min-h-screen bg-sanguo-dark flex flex-col">
    <!-- 顶部标题 -->
    <header class="flex-shrink-0 py-3 px-4 border-b border-sanguo-gold/30">
      <h1 class="text-xl font-bold text-sanguo-gold text-center">AI 三国杀</h1>
      <p v-if="game.connected" class="text-xs text-amber-200/70 text-center mt-1">
        已连接 · {{ game.roomId?.value ? `房间 ${game.roomId.value}` : '未加入房间' }}
      </p>
    </header>

    <!-- 大厅：未进入战局时显示 -->
    <section v-if="!inBattle" class="flex-1 flex flex-col items-center justify-center p-6 gap-6">
      <LobbySection @generated="onGeneralGenerated" />
      <div class="flex flex-wrap gap-3 justify-center">
        <button
          class="px-4 py-2 rounded-lg border border-sanguo-gold/50 text-sanguo-gold hover:bg-sanguo-gold/10 transition-colors"
          @click="connect"
        >
          {{ game.connected ? '已连接' : '连接服务器' }}
        </button>
        <button
          v-if="game.connected"
          class="px-4 py-2 rounded-lg bg-sanguo-gold/20 border border-sanguo-gold/50 text-sanguo-gold hover:bg-sanguo-gold/30 transition-colors"
          @click="createRoom"
        >
          创建房间
        </button>
        <button
          v-if="game.connected"
          class="px-4 py-2 rounded-lg bg-amber-800/30 border border-amber-500/50 text-amber-200 hover:bg-amber-700/40 transition-colors"
          @click="startGame"
        >
          开始游戏
        </button>
      </div>
    </section>

    <!-- 战局主界面 - 三国杀风格 -->
    <section v-else class="flex-1 flex flex-col min-h-0">
      <div class="flex-1 grid grid-cols-[260px_1fr] gap-3 p-3 min-h-0">
        <!-- 左侧：战局记录 -->
        <aside class="min-h-0 flex flex-col">
          <GameLog :log="game.gameLog" />
        </aside>

        <!-- 主战场：上方对手 + 中央牌堆 + 下方自己 -->
        <main class="flex-1 flex flex-col min-h-0 gap-3">
          <!-- 上方：对手武将框 -->
          <div class="flex justify-center gap-4 py-2">
            <PlayerEntity
              v-for="p in opponents"
              :key="p.playerId"
              :player="p"
              :is-current-turn="currentSeatIndex === p.seatIndex"
              :selectable="targetSelectable && isTargetValid(p)"
              :selected="selectedTargetId === p.playerId"
              @select="onSelectTarget"
            />
          </div>

          <!-- 中央：出牌区/牌堆 -->
          <div class="flex-1 min-h-[80px] rounded-xl border-2 border-dashed border-amber-700/40 bg-amber-950/20 flex items-center justify-center">
            <BattleArea :cards="battleCardsList" />
          </div>

          <!-- 下方：自己的武将框 + 手牌区 -->
          <div class="flex flex-col gap-2">
            <div class="flex items-center gap-4 justify-center">
              <PlayerEntity
                v-if="mePlayer"
                :player="mePlayer"
                :is-current-turn="currentSeatIndex === mePlayer.seatIndex"
                :selectable="targetSelectable && isTargetValid(mePlayer)"
                :selected="selectedTargetId === mePlayer.playerId"
                @select="onSelectTarget"
              />
            </div>
            <div class="flex flex-col gap-2 rounded-xl border-2 border-amber-700/50 bg-amber-950/30 p-4 shadow-inner">
              <!-- 被杀响应提示 -->
              <div
                v-if="isRespondMode"
                class="mb-2 px-3 py-2 rounded-lg bg-red-900/40 border border-red-500/50 text-red-200 text-sm"
              >
                {{ game.pendingKill?.value?.sourceName ?? '对方' }} 对你出杀！请出闪抵消或承受伤害
              </div>
              <HandArea
                :cards="handCardsList"
                :can-play="canPlayCard || isRespondMode"
                :current-phase="phaseStr"
                :selected-id="selectedCardId"
                @select="onSelectCard"
              />
              <div class="flex justify-between items-center gap-3 flex-wrap">
                <span v-if="selectedCard" class="text-amber-300 text-sm">
                  {{ isRespondMode ? '已选闪，点击出牌抵消' : `已选: ${selectedCard.rankOrName || selectedCard.type || '?'}` }}
                  <span v-if="!isRespondMode && needsTarget && !selectedTargetId" class="text-amber-500">→ 请点击目标</span>
                  <span v-if="!isRespondMode && (selectedCard.rankOrName || selectedCard.type) === '杀' && selectedTargetId" class="text-red-400 font-medium">
                    → 对 {{ targetPlayerName }} 出杀
                  </span>
                </span>
                <div class="flex gap-2">
                  <button
                    class="px-4 py-2 rounded-lg bg-amber-600 hover:bg-amber-500 text-sanguo-dark font-bold text-sm disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
                    :disabled="!canConfirmPlay"
                    @click="confirmPlay"
                  >
                    {{ playButtonText }}
                  </button>
                  <button
                    v-if="isRespondMode"
                    class="px-4 py-2 rounded-lg border-2 border-red-500/60 text-red-300 hover:bg-red-500/20 transition-colors text-sm"
                    @click="acceptDamage"
                  >
                    承受伤害
                  </button>
                  <button
                    class="px-4 py-2 rounded-lg border-2 border-amber-500/60 text-amber-300 hover:bg-amber-500/20 transition-colors text-sm disabled:opacity-40 disabled:cursor-not-allowed"
                    :disabled="!isMyTurn || isRespondMode || endRoundLoading"
                    @click="endRound"
                  >
                    {{ endRoundLoading ? '处理中...' : '结束回合' }}
                  </button>
                </div>
              </div>
            </div>
          </div>
        </main>
      </div>
    </section>

    <!-- 武将卡片预览弹窗 -->
    <GeneralPreviewModal
      :visible="generalPreviewVisible"
      :general="previewGeneral"
      @close="generalPreviewVisible = false"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import LobbySection from './components/LobbySection.vue'
import GameLog from './components/GameLog.vue'
import BattleArea from './components/BattleArea.vue'
import HandArea from './components/HandArea.vue'
import PlayerEntity from './components/PlayerEntity.vue'
import GeneralPreviewModal from './components/GeneralPreviewModal.vue'
import { useGameSocket } from './composables/useGameSocket.js'

const game = useGameSocket()
const generalPreviewVisible = ref(false)
const previewGeneral = ref(null)
const selectedCardId = ref(null)
const selectedTargetId = ref(null)

// 有房间且已经拿到手牌时，进入战局界面
const inBattle = computed(() => !!game.roomId && game.myHandCards.value.length > 0)

const canPlayCard = computed(() => game.currentPhase.value === 'PLAY')
const isRespondMode = computed(() => {
  const pk = game.pendingKill.value
  return pk && pk.targetId === game.myPlayerId.value
})

const handCardsList = computed(() =>
  Array.isArray(game.myHandCards?.value) ? game.myHandCards.value : []
)
const battleCardsList = computed(() =>
  Array.isArray(game.battleCards?.value) ? game.battleCards.value : []
)
const phaseStr = computed(() => game.currentPhase?.value ?? 'PREPARE')

const currentPlayer = computed(() =>
  game.players.value.find((p) => p.seatIndex === game.currentSeatIndex.value)
)
const isMyTurn = computed(
  () => currentPlayer.value && currentPlayer.value.playerId === game.myPlayerId.value
)

// 合并后端推送的玩家与本地手牌数：若后端有 handCount 用后端，否则用本地手牌长度（仅当前用户）
const playersWithHandCount = computed(() => {
  const me = game.myPlayerId.value
  return game.players.value.map((p) => ({
    ...p,
    handCount: p.playerId === me ? game.myHandCards.value.length : p.handCount ?? 0,
  }))
})

const currentSeatIndex = computed(() => game.currentSeatIndex.value ?? 0)
const opponents = computed(() =>
  playersWithHandCount.value.filter((p) => p.playerId !== game.myPlayerId.value)
)
const mePlayer = computed(() =>
  playersWithHandCount.value.find((p) => p.playerId === game.myPlayerId.value)
)

const selectedCard = computed(() =>
  handCardsList.value.find((c) => c.id === selectedCardId.value) ?? null
)
const targetSelectable = computed(
  () => !isRespondMode.value && isMyTurn.value && canPlayCard.value && selectedCard.value && needsTarget.value
)
const needsTarget = computed(() => {
  const t = selectedCard.value?.rankOrName || selectedCard.value?.type
  return t === '杀' // 桃只能给自己用，不需要选目标
})
const canConfirmPlay = computed(() => {
  if (isRespondMode.value) {
    return !!selectedCard.value && (selectedCard.value?.rankOrName || selectedCard.value?.type) === '闪'
  }
  if (!selectedCard.value || !isMyTurn.value) return false
  const t = selectedCard.value?.rankOrName || selectedCard.value?.type
  if (t === '闪') return false
  if (t === '杀') return !!selectedTargetId.value
  if (t === '桃') {
    // 桃只能给自己用，且满血时不能使用
    const me = mePlayer.value
    return me && (me.hp ?? 4) < (me.maxHp ?? 4)
  }
  return true
})

const targetPlayerName = computed(() => {
  if (!selectedTargetId.value) return ''
  const p = game.players.value.find((x) => x.playerId === selectedTargetId.value)
  return p?.nickname ?? p?.playerId ?? ''
})

const playButtonText = computed(() => {
  if (isRespondMode.value) return '出闪抵消'
  const t = selectedCard.value?.rankOrName || selectedCard.value?.type
  if (t === '杀' && selectedTargetId.value && targetPlayerName.value) return `对 ${targetPlayerName.value} 出杀`
  return '出牌'
})

function isTargetValid(p) {
  if (!p || (p.hp ?? 4) <= 0) return false
  const t = selectedCard.value?.rankOrName || selectedCard.value?.type
  if (t === '杀') return p.playerId !== game.myPlayerId.value
  if (t === '桃') return p.playerId === game.myPlayerId.value && (p.hp ?? 4) < (p.maxHp ?? 4)
  return false
}

function onSelectCard(card) {
  if (!card?.id) return
  if (isRespondMode.value) {
    if ((card.rankOrName || card.type) !== '闪') return
  } else if (!canPlayCard.value || (card.rankOrName || card.type) === '闪') return
  if (selectedCardId.value === card.id) {
    selectedCardId.value = null
    selectedTargetId.value = null
    return
  }
  selectedCardId.value = card.id
  selectedTargetId.value = null
}

function onSelectTarget(player) {
  if (!targetSelectable.value || !player) return
  if (!isTargetValid(player)) return
  selectedTargetId.value = player.playerId
}

async function confirmPlay() {
  if (!canConfirmPlay.value) return
  const card = selectedCard.value
  if (!card) return
  await playCard(card)
  selectedCardId.value = null
  selectedTargetId.value = null
}

function connect() {
  game.connect()
}

function createRoom() {
  if (!game.connected.value) return
  game.createRoom('AI 三国杀房间', 4, '玩家1')
}

async function startGame() {
  const rid = game.roomId.value
  const pid = game.myPlayerId.value
  if (!rid || !pid) return
  try {
    const res = await fetch(`/api/game/${rid}/start`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ playerId: pid }),
    })
    const data = await res.json()
    if (data?.ok) {
      game.addLog('系统', '游戏开始')
      game.applyGameState(data)
    } else if (data?.message) {
      game.addLog('系统', data.message)
    }
  } catch (e) {
    console.error('startGame failed', e)
  }
}

/** 出牌：点击手牌后发送到后端。响应杀时出闪也走 /play 接口。 */
async function playCard(card) {
  const rid = game.roomId.value
  const pid = game.myPlayerId.value
  if (!rid || !pid || !card?.id) return
  if (!isRespondMode.value && game.currentPhase.value !== 'PLAY') return
  // 「杀」「桃」使用用户点击选择的目标
  const cardType = card.rankOrName || card.type
  let targetId = null
  if (cardType === '杀' || cardType === '桃') {
    targetId = selectedTargetId.value || (cardType === '杀'
      ? game.players.value.find((p) => p.playerId !== pid && (p.hp ?? 4) > 0)?.playerId
      : pid)
  }
  try {
    const res = await fetch(`/api/game/${rid}/play`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ playerId: pid, cardId: card.id, targetId }),
    })
    const data = await res.json()
    if (data?.ok) {
      game.applyGameState(data)
      selectedCardId.value = null
    } else if (data?.message) {
      game.addLog('系统', data.message)
    }
  } catch (e) {
    console.error('playCard failed', e)
  }
}

/** 承受伤害：被杀目标选择不出闪，承受伤害 */
async function acceptDamage() {
  const rid = game.roomId.value
  const pid = game.myPlayerId.value
  if (!rid || !pid || !isRespondMode.value) return
  try {
    const res = await fetch(`/api/game/${rid}/respond`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ playerId: pid, action: 'PASS' }),
    })
    const data = await res.json()
    if (data?.ok) {
      game.applyGameState(data)
      selectedCardId.value = null
      selectedTargetId.value = null
    } else if (data?.message) {
      game.addLog('系统', data.message)
    }
  } catch (e) {
    console.error('acceptDamage failed', e)
  }
}

const endRoundLoading = ref(false)
async function endRound() {
  const rid = game.roomId.value
  const pid = game.myPlayerId.value
  if (!rid || !pid) return
  endRoundLoading.value = true
  try {
    const res = await fetch(`/api/game/${rid}/endRound`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ playerId: pid }),
    })
    const data = await res.json()
    if (data?.ok) {
      game.applyGameState(data)
    } else if (data?.message) {
      game.addLog('系统', data.message)
    }
  } catch (e) {
    console.error('endRound failed', e)
  } finally {
    endRoundLoading.value = false
  }
}

/** AI 生成武将成功：弹出武将卡片预览 */
function onGeneralGenerated(general) {
  previewGeneral.value = general
  generalPreviewVisible.value = true
}

onMounted(() => {
  game.connect()
})
</script>
