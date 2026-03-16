<template>
  <div class="h-screen overflow-hidden bg-sanguo-dark flex flex-col">
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
      <div class="flex flex-col gap-4 items-center">
        <div v-if="game.connected" class="flex items-center gap-3">
          <label class="text-amber-200 text-sm">机器人数量：</label>
          <select
            v-model.number="botCount"
            class="px-3 py-2 rounded-lg bg-amber-950/50 border border-amber-500/50 text-amber-200 text-sm"
          >
            <option v-for="n in 7" :key="n" :value="n">{{ n }} 个</option>
          </select>
        </div>
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
            @click="generalSelectVisible = true"
          >
            开始游戏
          </button>
        </div>
      </div>
    </section>

    <!-- 战局主界面 - 三国杀风格（大改布局） -->
    <section v-else class="flex-1 flex flex-col min-h-0 overflow-hidden">
      <div class="flex-1 grid grid-cols-[200px_1fr] gap-4 p-4 min-h-0 overflow-hidden">
        <!-- 左侧：战局记录 -->
        <aside class="flex flex-col min-h-0 overflow-hidden">
          <GameLog :log="game.gameLog" />
        </aside>

        <!-- 主战场：只有左侧战局记录有滚动条 -->
        <main class="flex flex-col min-h-0 overflow-hidden">
          <!-- 上方：对手 + 出牌区（保证最小高度，中央出牌区不被挤没） -->
          <div class="flex-1 min-h-[220px] flex flex-col gap-3 overflow-hidden">
            <div class="flex-shrink-0 flex justify-center gap-3 flex-wrap">
              <div
                v-for="p in opponents"
                :key="p.playerId"
                :data-player-id="p.playerId"
              >
                <PlayerEntity
                  :player="p"
                  :is-current-turn="currentSeatIndex === p.seatIndex"
                  :selectable="targetSelectable && isTargetValid(p)"
                  :selected="selectedTargetId === p.playerId"
                  @select="onSelectTarget"
                />
              </div>
            </div>
            <!-- 中央：出牌区（固定高度 120px，无滚动） -->
            <div class="flex-shrink-0 h-[120px] rounded-xl border-2 border-amber-700/40 bg-amber-950/30 flex items-center justify-center p-3">
              <BattleArea :cards="battleCardsList" />
            </div>
          </div>

          <!-- 下方：自己 + 手牌 + 操作按钮（始终贴底可见） -->
          <div class="flex-shrink-0 flex flex-col gap-3 mt-2">
            <!-- 自己武将 -->
            <div v-if="mePlayer" class="flex justify-center">
              <PlayerEntity
                :data-player-id="mePlayer.playerId"
                :player="mePlayer"
                :is-current-turn="currentSeatIndex === mePlayer.seatIndex"
                :selectable="targetSelectable && isTargetValid(mePlayer)"
                :selected="selectedTargetId === mePlayer.playerId"
                @select="onSelectTarget"
              />
            </div>

            <!-- 手牌区 + 操作 -->
            <div class="rounded-xl border-2 border-amber-700/50 bg-amber-950/40 p-4">
              <!-- 被杀响应提示：更醒目，带脉动动画 -->
              <div
                v-if="isRespondMode"
                class="mb-2 px-4 py-3 rounded-lg bg-red-900/60 border-2 border-red-500 text-red-100 text-base font-medium animate-pulse"
              >
                ⚔️ {{ game.pendingKill?.value?.sourceName ?? '对方' }} 对你出杀！请选择：出闪抵消、使用「龙胆」将杀当闪、或点击「承受伤害」
              </div>
              <!-- 濒死轮询提示 -->
              <div
                v-if="isDyingMode"
                class="mb-2 px-3 py-2 rounded-lg bg-orange-900/50 border border-orange-500/50 text-orange-200 text-sm"
              >
                {{ dyingTargetName }} 濒死！是否出桃救援？（可自救）
              </div>
              <HandArea
                :cards="handCardsList"
                :can-play="canPlayCard || isRespondMode || isDyingMode"
                :current-phase="phaseStr"
                :selected-id="selectedCardId"
                @select="onSelectCard"
              />
              <div class="flex justify-between items-center gap-3 flex-wrap">
                <span v-if="selectedCard" class="text-amber-300 text-sm">
                  {{ isRespondMode && longdanMode && (selectedCard.rankOrName || selectedCard.type) === '杀' ? '已选杀，龙胆当闪，点击出牌' : isRespondMode && !longdanMode ? '已选闪，点击出牌抵消' : `已选: ${selectedCard.rankOrName || selectedCard.type || '?'}` }}
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
                    v-if="isRespondMode && canUseLongdan"
                    class="px-4 py-2 rounded-lg border-2 border-sanguo-gold/70 text-sanguo-gold hover:bg-sanguo-gold/20 transition-colors font-medium"
                    :class="{ 'ring-2 ring-sanguo-gold': longdanMode }"
                    @click="longdanMode = !longdanMode"
                  >
                    {{ longdanMode ? '龙胆已选 ✓' : '龙胆（杀当闪）' }}
                  </button>
                  <button
                    v-if="isRespondMode"
                    class="px-4 py-2 rounded-lg border-2 border-red-500 text-red-200 hover:bg-red-500/30 transition-colors font-medium"
                    :disabled="acceptDamageLoading"
                    @click="acceptDamage"
                  >
                    {{ acceptDamageLoading ? '处理中...' : '承受伤害' }}
                  </button>
                  <button
                    v-if="isDyingMode"
                    class="px-4 py-2 rounded-lg bg-green-600/80 hover:bg-green-500 text-white font-medium text-sm disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
                    :disabled="!selectedCard || (selectedCard?.rankOrName || selectedCard?.type) !== '桃'"
                    @click="respondDyingTao"
                  >
                    出桃救人
                  </button>
                  <button
                    v-if="isDyingMode"
                    class="px-4 py-2 rounded-lg border-2 border-orange-500/60 text-orange-300 hover:bg-orange-500/20 transition-colors text-sm"
                    @click="respondDyingPass"
                  >
                    不出
                  </button>
                  <button
                    class="px-4 py-2 rounded-lg border-2 border-amber-500/60 text-amber-300 hover:bg-amber-500/20 transition-colors text-sm disabled:opacity-40 disabled:cursor-not-allowed"
                    :disabled="!isMyTurn || isRespondMode || isDyingMode || endRoundLoading"
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

    <!-- 出杀箭头动画（从出牌者指向目标） -->
    <KillArrowOverlay v-if="inBattle" :kill-arrow="game.killArrow?.value ?? game.killArrow ?? null" />

    <!-- 武将卡片预览弹窗 -->
    <GeneralPreviewModal
      :visible="generalPreviewVisible"
      :general="previewGeneral"
      @close="generalPreviewVisible = false"
    />

    <!-- 选将框：开始游戏前选择武将 -->
    <GeneralSelectModal
      :visible="generalSelectVisible"
      @confirm="onGeneralSelected"
      @close="generalSelectVisible = false"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import LobbySection from './components/LobbySection.vue'
import GameLog from './components/GameLog.vue'
import BattleArea from './components/BattleArea.vue'
import HandArea from './components/HandArea.vue'
import PlayerEntity from './components/PlayerEntity.vue'
import KillArrowOverlay from './components/KillArrowOverlay.vue'
import GeneralPreviewModal from './components/GeneralPreviewModal.vue'
import GeneralSelectModal from './components/GeneralSelectModal.vue'
import { useGameSocket } from './composables/useGameSocket.js'

const game = useGameSocket()
const generalPreviewVisible = ref(false)
const previewGeneral = ref(null)
/** 选将框可见性 */
const generalSelectVisible = ref(false)
/** 点击开始游戏但尚无房间时，创建房间后自动打开选将框 */
const pendingStartAfterRoom = ref(false)
const botCount = ref(1)  // 机器人数量 1-7
const selectedCardId = ref(null)
const selectedTargetId = ref(null)
/** 龙胆技能模式：发动龙胆时，可将杀当闪使用 */
const longdanMode = ref(false)

// 有房间且已经拿到手牌时，进入战局界面
const inBattle = computed(() => !!game.roomId && game.myHandCards.value.length > 0)

const canPlayCard = computed(() => game.currentPhase.value === 'PLAY')
const isRespondMode = computed(() => {
  const pk = game.pendingKill.value
  return pk && pk.targetId === game.myPlayerId.value
})

const isDyingMode = computed(() => {
  const pd = game.pendingDeath?.value ?? game.pendingDeath
  if (!pd) return false
  const askingSeat = pd.askingSeatIndex
  const me = game.players.value?.find((p) => p.playerId === game.myPlayerId.value)
  return me != null && me.seatIndex === askingSeat
})

const dyingTargetName = computed(() => {
  const pd = game.pendingDeath?.value ?? game.pendingDeath
  return pd?.targetName ?? pd?.targetId ?? '濒死玩家'
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

/** 是否拥有龙胆技能（赵云） */
const hasLongdanSkill = computed(() =>
  mePlayer.value?.general?.skills?.some((s) => s.id === 'longdan') ?? false
)
/** 手牌中是否有杀 */
const hasShaInHand = computed(() =>
  handCardsList.value.some((c) => (c.rankOrName || c.type) === '杀')
)
/** 能否使用龙胆（有技能且手牌有杀） */
const canUseLongdan = computed(() => hasLongdanSkill.value && hasShaInHand.value)

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
    const t = selectedCard.value?.rankOrName || selectedCard.value?.type
    if (longdanMode.value && t === '杀') return true
    return !!selectedCard.value && t === '闪'
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
  if (isRespondMode.value) {
    if (longdanMode.value && (selectedCard.value?.rankOrName || selectedCard.value?.type) === '杀') return '龙胆：杀当闪'
    return '出闪抵消'
  }
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
    const ct = card.rankOrName || card.type
    if (longdanMode.value && ct === '杀') { /* 龙胆模式可选杀 */ }
    else if (ct !== '闪') return
  } else if (isDyingMode.value) {
    if ((card.rankOrName || card.type) !== '桃') return
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
  game.createRoom('AI 三国杀房间', '玩家1', botCount.value)
}

/** 选将框确认后，以所选武将开始游戏 */
async function onGeneralSelected(generalId) {
  generalSelectVisible.value = false
  const rid = game.roomId.value
  const pid = game.myPlayerId.value
  if (!rid || !pid) return
  try {
    const res = await fetch(`/api/game/${rid}/start`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ playerId: pid, generalId }),
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
  const useLongdan = isRespondMode.value && longdanMode.value && (cardType === '杀')
  try {
    const body = { playerId: pid, cardId: card.id, targetId }
    if (useLongdan) body.skillId = 'longdan'
    const res = await fetch(`/api/game/${rid}/play`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    })
    const data = await res.json()
    if (data?.ok) {
      game.applyGameState(data)
      selectedCardId.value = null
      selectedTargetId.value = null
      longdanMode.value = false
    } else if (data?.message) {
      game.addLog('系统', data.message)
    }
  } catch (e) {
    console.error('playCard failed', e)
  }
}

/** 濒死轮询：出桃救人 */
async function respondDyingTao() {
  const rid = game.roomId.value
  const pid = game.myPlayerId.value
  const card = selectedCard.value
  if (!rid || !pid || !isDyingMode.value || !card || (card.rankOrName || card.type) !== '桃') return
  try {
    const res = await fetch(`/api/game/${rid}/respondDying`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ playerId: pid, action: 'USE_TAO', cardId: card.id }),
    })
    const data = await res.json()
    if (data?.ok) {
      game.applyGameState(data)
      selectedCardId.value = null
      game.addLog('系统', `你出桃救了 ${dyingTargetName.value}`)
    } else if (data?.message) {
      game.addLog('系统', data.message)
    }
  } catch (e) {
    console.error('respondDyingTao failed', e)
  }
}

/** 濒死轮询：选择不出桃 */
async function respondDyingPass() {
  const rid = game.roomId.value
  const pid = game.myPlayerId.value
  if (!rid || !pid || !isDyingMode.value) return
  try {
    const res = await fetch(`/api/game/${rid}/respondDying`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ playerId: pid, action: 'PASS' }),
    })
    const data = await res.json()
    if (data?.ok) {
      game.applyGameState(data)
      selectedCardId.value = null
    } else if (data?.message) {
      game.addLog('系统', data.message)
    }
  } catch (e) {
    console.error('respondDyingPass failed', e)
  }
}

const acceptDamageLoading = ref(false)
/** 承受伤害：被杀目标选择不出闪，承受伤害 */
async function acceptDamage() {
  const rid = game.roomId.value
  const pid = game.myPlayerId.value
  if (!rid || !pid || !isRespondMode.value) return
  acceptDamageLoading.value = true
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
      game.addLog('系统', '你承受了伤害')
    } else {
      game.addLog('系统', data?.message || '响应失败，请重试')
    }
  } catch (e) {
    console.error('acceptDamage failed', e)
    game.addLog('系统', '网络错误，请检查连接后重试')
  } finally {
    acceptDamageLoading.value = false
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

watch(isRespondMode, (val) => {
  if (!val) longdanMode.value = false
})

onMounted(() => {
  game.connect()
})
</script>
