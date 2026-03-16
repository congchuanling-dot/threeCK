<template>
  <div class="h-screen overflow-hidden bg-sanguo-dark flex flex-col">
    <header class="flex-shrink-0 py-2 px-4 border-b border-sanguo-gold/30">
      <h1 class="text-lg font-bold text-sanguo-gold text-center">AI 三国杀</h1>
      <p v-if="game.connected" class="text-xs text-amber-200/70 text-center">
        {{ game.roomId?.value ? `房间 ${game.roomId.value}` : '未加入房间' }}
      </p>
    </header>

    <!-- 大厅 -->
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
            class="px-4 py-2 rounded-lg border border-sanguo-gold/50 text-sanguo-gold hover:bg-sanguo-gold/10 hover:scale-105 active:scale-95 transition-all duration-200"
            @click="connect"
          >
            {{ game.connected ? '已连接' : '连接服务器' }}
          </button>
          <button
            v-if="game.connected"
            class="px-4 py-2 rounded-lg bg-sanguo-gold/20 border border-sanguo-gold/50 text-sanguo-gold hover:bg-sanguo-gold/30 hover:scale-105 active:scale-95 hover:shadow-lg hover:shadow-sanguo-gold/20 transition-all duration-200"
            @click="createRoom"
          >
            创建房间
          </button>
          <button
            v-if="game.connected"
            class="px-4 py-2 rounded-lg bg-amber-800/30 border border-amber-500/50 text-amber-200 hover:bg-amber-700/40 hover:scale-105 active:scale-95 hover:shadow-lg hover:shadow-amber-500/20 transition-all duration-200"
            @click="startGameClick"
          >
            开始游戏
          </button>
        </div>
      </div>
    </section>

    <!-- 战局：左日志 + 右主区 -->
    <section v-else class="flex-1 min-h-0 flex">
      <aside class="w-48 flex-shrink-0 border-r border-amber-700/40 flex flex-col overflow-hidden">
        <div class="px-2 py-1 text-sanguo-gold font-medium text-sm border-b border-amber-700/40">战局</div>
        <ul class="flex-1 min-h-0 overflow-y-auto p-2 text-xs space-y-1">
          <li v-for="(e, i) in [...(game.gameLog?.value ?? [])].reverse()" :key="i" class="text-amber-200/90">
            <span class="text-slate-500">{{ e.time }}</span> {{ e.who }}：{{ e.text }}
          </li>
        </ul>
      </aside>

      <main class="flex-1 min-h-0 flex flex-col p-3 gap-3 overflow-y-auto">
        <!-- 对手区：横向排列 -->
        <div class="flex-shrink-0 flex flex-wrap justify-center gap-2">
          <div
            v-for="p in opponents"
            :key="p.playerId"
            @click="targetSelectable && isTargetValid(p) && onSelectTarget(p)"
          >
            <PlayerEntity
              :player="p"
              :is-current-turn="currentSeatIndex === p.seatIndex"
              :selectable="targetSelectable && isTargetValid(p)"
              :selected="selectedTargetId === p.playerId"
            />
          </div>
        </div>

        <!-- 中央出牌区：固定高度，牌 5s 后消失，带入场特效 -->
        <div
          ref="battleAreaRef"
          data-battle-center
          class="flex-shrink-0 h-24 rounded-lg border-2 border-amber-700/40 bg-amber-950/40 flex items-center justify-center gap-2 shadow-inner transition-all duration-300"
        >
          <template v-if="displayBattleCards.length">
            <TransitionGroup name="battle-card" tag="div" class="flex items-center justify-center gap-2">
              <Card
                v-for="(c, i) in displayBattleCards"
                :key="(c.cardId || c.id) + '-' + i"
                :card="normalizeBattleCard(c)"
                :can-play="false"
                class="scale-75 pointer-events-none card-enter"
              />
            </TransitionGroup>
          </template>
          <span v-else class="text-slate-500 text-sm">出的牌显示于此</span>
        </div>

        <!-- 自己 -->
        <div v-if="mePlayer" class="flex-shrink-0 flex justify-center">
          <PlayerEntity
            :player="mePlayer"
            :is-current-turn="currentSeatIndex === mePlayer.seatIndex"
            :selectable="targetSelectable && isTargetValid(mePlayer)"
            :selected="selectedTargetId === mePlayer.playerId"
            @select="onSelectTarget"
          />
        </div>

        <!-- 手牌 + 操作：始终可见，不收缩 -->
        <div class="flex-shrink-0 rounded-lg border-2 border-amber-700/50 bg-amber-950/40 p-3">
          <div v-if="isRespondMode" class="mb-2 px-3 py-2 rounded bg-red-900/60 border border-red-500 text-red-100 text-sm animate-pulse shadow-lg shadow-red-900/30">
            ⚔️ 对你出杀！出闪抵消 / 龙胆(杀当闪) / 承受伤害
          </div>
          <div v-if="isDyingMode" class="mb-2 px-3 py-2 rounded bg-orange-900/50 border border-orange-500/50 text-orange-200 text-sm animate-pulse shadow-lg shadow-orange-900/30">
            {{ dyingTargetName }} 濒死，是否出桃？
          </div>
          <HandArea
            :cards="handCardsList"
            :can-play="canPlayCard || isRespondMode || isDyingMode"
            :current-phase="phaseStr"
            :selected-id="selectedCardId"
            @select="onSelectCard"
          />
          <div class="flex flex-wrap items-center gap-2 mt-2">
            <span v-if="selectedCard" class="text-amber-300 text-sm">
              {{ selectedCard.rankOrName || selectedCard.type || '?' }}
              <span v-if="needsTarget && !selectedTargetId" class="text-amber-500">→ 选目标</span>
            </span>
            <button
              class="px-3 py-1.5 rounded bg-amber-600 text-sanguo-dark font-bold text-sm disabled:opacity-40 hover:bg-amber-500 active:scale-95 transition-all duration-200 disabled:hover:bg-amber-600 disabled:active:scale-100"
              :disabled="!canConfirmPlay"
              @click="confirmPlay"
            >
              {{ playButtonText }}
            </button>
            <button
              v-if="isRespondMode && canUseLongdan"
              class="px-3 py-1.5 rounded border border-sanguo-gold text-sanguo-gold text-sm hover:scale-105 active:scale-95 transition-all duration-200"
              :class="{ 'ring-2 ring-sanguo-gold': longdanMode }"
              @click="longdanMode = !longdanMode"
            >
              {{ longdanMode ? '龙胆✓' : '龙胆' }}
            </button>
            <button
              v-if="isRespondMode"
              class="px-3 py-1.5 rounded border border-red-500 text-red-200 text-sm disabled:opacity-40 hover:scale-105 active:scale-95 transition-all duration-200 disabled:hover:scale-100"
              :disabled="acceptDamageLoading"
              @click="acceptDamage"
            >
              承受伤害
            </button>
            <button
              v-if="isDyingMode"
              class="px-3 py-1.5 rounded bg-green-600 text-white text-sm disabled:opacity-40 hover:bg-green-500 active:scale-95 transition-all duration-200"
              :disabled="!selectedCard || (selectedCard?.rankOrName || selectedCard?.type) !== '桃'"
              @click="respondDyingTao"
            >
              出桃
            </button>
            <button v-if="isDyingMode" class="px-3 py-1.5 rounded border border-orange-500 text-orange-300 text-sm hover:scale-105 active:scale-95 transition-all duration-200" @click="() => actions.respondDyingPass()">
              不出
            </button>
            <button
              class="px-3 py-1.5 rounded border border-amber-500 text-amber-300 text-sm disabled:opacity-40 hover:scale-105 active:scale-95 transition-all duration-200 disabled:hover:scale-100"
              :disabled="!isMyTurn || isRespondMode || isDyingMode || endRoundLoading"
              @click="() => actions.endRound()"
            >
              结束回合
            </button>
          </div>
        </div>
      </main>
    </section>

    <KillArrowOverlay v-if="inBattle" :kill-arrow="game.killArrow?.value ?? game.killArrow ?? null" />
    <CardFlyOverlay
      v-if="inBattle"
      :card-fly="game.cardFly?.value ?? game.cardFly ?? null"
      :battle-area-ref="battleAreaRef"
      @done="game.clearCardFly?.()"
    />
    <GeneralPreviewModal :visible="generalPreviewVisible" :general="previewGeneral" @close="() => (generalPreviewVisible = false)" />
    <GeneralSelectModal :visible="generalSelectVisible" @confirm="onGeneralSelected" @close="generalSelectVisible = false" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import LobbySection from './components/LobbySection.vue'
import HandArea from './components/HandArea.vue'
import PlayerEntity from './components/PlayerEntity.vue'
import Card from './components/Card.vue'
import KillArrowOverlay from './components/KillArrowOverlay.vue'
import CardFlyOverlay from './components/CardFlyOverlay.vue'
import GeneralPreviewModal from './components/GeneralPreviewModal.vue'
import GeneralSelectModal from './components/GeneralSelectModal.vue'
import { useGameSocket } from './composables/useGameSocket.js'
import { useGameActions } from './composables/useGameActions.js'
import { useBattleCards } from './composables/useBattleCards.js'
import { CARD_TYPES, TRICK_NEEDS_TARGET, cardNeedsTargetForPlay, isEquipment } from './constants/cardTypes.js'
import { getCardType, isSha, isShan, isTao, normalizeCard as normalizeBattleCard } from './utils/cardUtils.js'

const game = useGameSocket()
const actions = useGameActions(game)
const { displayBattleCards } = useBattleCards(game)
const generalPreviewVisible = ref(false)
const previewGeneral = ref(null)
const generalSelectVisible = ref(false)
const pendingStartAfterRoom = ref(false)
const botCount = ref(1)
const selectedCardId = ref(null)
const selectedTargetId = ref(null)
const longdanMode = ref(false)
const battleAreaRef = ref(null)
const { acceptDamageLoading, endRoundLoading } = actions

const inBattle = computed(() => !!game.roomId?.value && game.myHandCards?.value?.length > 0)
const canPlayCard = computed(() => game.currentPhase?.value === 'PLAY')
const isRespondMode = computed(() => {
  const pk = game.pendingKill?.value
  return pk && pk.targetId === game.myPlayerId?.value
})
const isDyingMode = computed(() => {
  const pd = game.pendingDeath?.value ?? game.pendingDeath
  if (!pd) return false
  const me = game.players?.value?.find((p) => p.playerId === game.myPlayerId?.value)
  return me && me.seatIndex === pd.askingSeatIndex
})
const dyingTargetName = computed(() => {
  const pd = game.pendingDeath?.value ?? game.pendingDeath
  return pd?.targetName ?? pd?.targetId ?? '濒死'
})
const handCardsList = computed(() => Array.isArray(game.myHandCards?.value) ? game.myHandCards.value : [])
const phaseStr = computed(() => game.currentPhase?.value ?? 'PREPARE')
const playersWithHandCount = computed(() => {
  const me = game.myPlayerId?.value
  return (game.players?.value ?? []).map((p) => ({
    ...p,
    handCount: p.playerId === me ? game.myHandCards?.value?.length ?? 0 : p.handCount ?? 0,
  }))
})
const opponents = computed(() => playersWithHandCount.value.filter((p) => p.playerId !== game.myPlayerId?.value))
const mePlayer = computed(() => playersWithHandCount.value.find((p) => p.playerId === game.myPlayerId?.value))
const currentSeatIndex = computed(() => game.currentSeatIndex?.value ?? 0)
const isMyTurn = computed(() => {
  const cur = game.players?.value?.find((p) => p.seatIndex === currentSeatIndex.value)
  return cur && cur.playerId === game.myPlayerId?.value
})
const hasLongdanSkill = computed(() => mePlayer.value?.general?.skills?.some((s) => s.id === 'longdan') ?? false)
const hasShaInHand = computed(() => handCardsList.value.some((c) => (c.rankOrName || c.type) === '杀'))
const canUseLongdan = computed(() => hasLongdanSkill.value && hasShaInHand.value)
const selectedCard = computed(() => handCardsList.value.find((c) => c.id === selectedCardId.value) ?? null)
const targetSelectable = computed(
  () => !isRespondMode.value && game.players?.value?.find((p) => p.seatIndex === currentSeatIndex.value)?.playerId === game.myPlayerId?.value && canPlayCard.value && selectedCard.value && needsTarget.value
)
const needsTarget = computed(() => cardNeedsTargetForPlay(selectedCard.value?.rankOrName || selectedCard.value?.type))
const canConfirmPlay = computed(() => {
  if (isRespondMode.value) {
    const t = selectedCard.value?.rankOrName || selectedCard.value?.type
    if (longdanMode.value && t === '杀') return true
    return !!selectedCard.value && t === '闪'
  }
  if (!selectedCard.value) return false
  const t = selectedCard.value?.rankOrName || selectedCard.value?.type
  if (t === '闪') return false
  if (t === '杀') return !!selectedTargetId.value
  if (t === '桃') {
    const me = mePlayer.value
    return me && (me.hp ?? 4) < (me.maxHp ?? 4)
  }
  if (t === '酒') {
    const me = mePlayer.value
    return me && (me.hp ?? 4) < (me.maxHp ?? 4)
  }
  if (TRICK_NEEDS_TARGET.includes(t)) return !!selectedTargetId.value
  if (isEquipment(t)) return true
  return true
})
const targetPlayerName = computed(() => game.players?.value?.find((x) => x.playerId === selectedTargetId.value)?.nickname ?? selectedTargetId.value ?? '')
const playButtonText = computed(() => {
  if (isRespondMode.value) return longdanMode.value && (selectedCard.value?.rankOrName || selectedCard.value?.type) === '杀' ? '龙胆出牌' : '出闪'
  const t = selectedCard.value?.rankOrName || selectedCard.value?.type
  if (t === '杀' && selectedTargetId.value) return `对 ${targetPlayerName.value} 出杀`
  if (TRICK_NEEDS_TARGET.includes(t) && selectedTargetId.value) return `对 ${targetPlayerName.value} 使用`
  if (isEquipment(t)) return '装备'
  return '出牌'
})

function isTargetValid(p) {
  if (!p || (p.hp ?? 4) <= 0) return false
  const t = selectedCard.value?.rankOrName || selectedCard.value?.type
  if (t === '杀') return p.playerId !== game.myPlayerId?.value
  if (t === '桃' || t === '酒') return p.playerId === game.myPlayerId?.value && (p.hp ?? 4) < (p.maxHp ?? 4)
  if (TRICK_NEEDS_TARGET.includes(t)) return p.playerId !== game.myPlayerId?.value
  return false
}

function onSelectCard(card) {
  if (!card?.id) return
  if (isRespondMode.value) {
    const ct = card.rankOrName || card.type
    if (!longdanMode.value && ct !== '闪') return
    if (longdanMode.value && ct !== '杀') return
  } else if (isDyingMode.value) {
    if ((card.rankOrName || card.type) !== '桃') return
  } else if (!canPlayCard.value) return
  else if ((card.rankOrName || card.type) === '闪') return
  selectedCardId.value = selectedCardId.value === card.id ? null : card.id
  if (!selectedCardId.value) selectedTargetId.value = null
}

function onSelectTarget(p) {
  if (!targetSelectable.value || !p || !isTargetValid(p)) return
  selectedTargetId.value = p.playerId
}

async function confirmPlay() {
  if (!canConfirmPlay.value || !selectedCard.value) return
  const ok = await actions.playCard(selectedCard.value, {
    targetId: selectedTargetId.value,
    useLongdan: isRespondMode.value && longdanMode.value,
  })
  if (ok) {
    selectedCardId.value = null
    selectedTargetId.value = null
    longdanMode.value = false
  }
}

function connect() { game.connect() }
function createRoom() {
  if (game.connected?.value) game.createRoom('AI 三国杀房间', '玩家1', botCount.value)
}

async function onGeneralSelected(generalId) {
  generalSelectVisible.value = false
  await actions.startGame(generalId)
}

async function respondDyingTao() {
  const card = selectedCard.value
  if (!card || !isTao(card)) return
  await actions.respondDyingTao(card.id)
  selectedCardId.value = null
}

async function acceptDamage() {
  await actions.acceptDamage()
  selectedCardId.value = null
  selectedTargetId.value = null
}

function onGeneralGenerated(g) {
  previewGeneral.value = g
  generalPreviewVisible.value = true
}

watch(isRespondMode, (v) => { if (!v) longdanMode.value = false })

// 开始游戏：无房间时先建房
watch(
  () => game.roomId?.value,
  (rid) => {
    if (rid && pendingStartAfterRoom.value) {
      pendingStartAfterRoom.value = false
      generalSelectVisible.value = true
    }
  }
)
function startGameClick() {
  if (game.roomId?.value && game.myPlayerId?.value) {
    generalSelectVisible.value = true
  } else {
    pendingStartAfterRoom.value = true
    game.addLog('系统', '正在创建房间…')
    game.createRoom('AI 三国杀房间', '玩家1', botCount.value)
  }
}

onMounted(() => {
  game.connect()
})
</script>
