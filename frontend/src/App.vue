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

    <!-- 战局主界面 -->
    <section v-else class="flex-1 grid grid-cols-[280px_1fr_220px] gap-4 p-4 min-h-0">
      <!-- 左侧：游戏记录日志 -->
      <aside class="min-h-0 flex flex-col">
        <GameLog :log="game.gameLog" />
      </aside>

      <!-- 中间：战场 + 手牌 -->
      <main class="min-h-0 flex flex-col gap-4">
        <div class="flex-1 min-h-0 rounded-lg overflow-hidden">
          <BattleArea :cards="game.battleCards" />
        </div>
        <div class="space-y-2">
          <HandArea
            :cards="game.myHandCards.value"
            :can-play="canPlayCard"
            :current-phase="game.currentPhase"
            :selected-id="selectedCardId"
            @play="playCard"
          />
          <div class="flex justify-end">
            <button
              class="px-4 py-2 rounded-lg bg-sanguo-gold/20 border border-sanguo-gold/50 text-sanguo-gold hover:bg-sanguo-gold/30 transition-colors text-sm disabled:opacity-40 disabled:cursor-not-allowed"
              :disabled="!isMyTurn"
              @click="endRound"
            >
              结束回合
            </button>
          </div>
        </div>
      </main>

      <!-- 右侧：玩家列表 -->
      <aside class="min-h-0 flex flex-col">
        <PlayerList :players="playersWithHandCount" :current-seat-index="game.currentSeatIndex.value" />
      </aside>
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
import PlayerList from './components/PlayerList.vue'
import GeneralPreviewModal from './components/GeneralPreviewModal.vue'
import { useGameSocket } from './composables/useGameSocket.js'

const game = useGameSocket()
const generalPreviewVisible = ref(false)
const previewGeneral = ref(null)
const selectedCardId = ref(null)

// 有房间且已经拿到手牌时，进入战局界面
const inBattle = computed(() => !!game.roomId && game.myHandCards.value.length > 0)

const canPlayCard = computed(() => game.currentPhase.value === 'PLAY')

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
      // 优先使用 HTTP 返回的手牌，保证 MVP 可以直接玩
      if (Array.isArray(data.hand)) {
        game.myHandCards.value = data.hand
      }
    } else if (data?.message) {
      game.addLog('系统', data.message)
    }
  } catch (e) {
    console.error('startGame failed', e)
  }
}

/** 出牌：点击手牌后发送到后端并本地从手牌移除（最终状态以服务端广播为准） */
async function playCard(card) {
  const rid = game.roomId.value
  const pid = game.myPlayerId.value
  if (!rid || !pid || !card?.id) return
  if (game.currentPhase.value !== 'PLAY') return
  try {
    const res = await fetch(`/api/game/${rid}/play`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ playerId: pid, cardId: card.id }),
    })
    const data = await res.json()
    if (data?.ok) {
      game.myHandCards.value = game.myHandCards.value.filter((c) => c.id !== card.id)
      selectedCardId.value = null
    }
  } catch (e) {
    console.error('playCard failed', e)
  }
}

async function endRound() {
  const rid = game.roomId.value
  const pid = game.myPlayerId.value
  if (!rid || !pid) return
  try {
    const res = await fetch(`/api/game/${rid}/endRound`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ playerId: pid }),
    })
    const data = await res.json()
    if (!data?.ok && data?.message) {
      game.addLog('系统', data.message)
    }
  } catch (e) {
    console.error('endRound failed', e)
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
