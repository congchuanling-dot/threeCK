<template>
  <div class="relative w-full h-full min-h-[200px] flex items-center justify-center overflow-visible">
    <!-- 中央出牌区：独立块，牌 5 秒后消失 -->
    <div class="absolute inset-0 flex items-center justify-center pointer-events-none">
      <div class="w-[180px] h-[100px] rounded-xl border-2 border-sanguo-gold/50 bg-amber-950/40 flex items-center justify-center shadow-xl flex-shrink-0">
        <BattleArea :cards="displayBattleCards" />
      </div>
    </div>

    <!-- 玩家坐席：按人数围成一圈，自己在底部 -->
    <div
      class="relative w-full max-w-[260px] aspect-square flex-shrink-0 mx-auto"
      :style="{ '--n': sortedPlayers.length }"
    >
      <div
        v-for="(p, i) in sortedPlayers"
        :key="p.playerId"
        class="absolute transform -translate-x-1/2 -translate-y-1/2 transition-all duration-200"
        :style="playerPositionStyle(i)"
        @click="selectable && isTargetValid(p) && $emit('select', p)"
      >
        <PlayerEntity
          :player="p"
          :is-current-turn="currentSeatIndex === p.seatIndex"
          :selectable="targetSelectable && isTargetValid(p)"
          :selected="selectedTargetId === p.playerId"
          :class="{ 'cursor-pointer': selectable && isTargetValid(p) }"
          @select="$emit('select', $event)"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import PlayerEntity from './PlayerEntity.vue'
import BattleArea from './BattleArea.vue'

const props = defineProps({
  players: { type: Array, default: () => [] },
  myPlayerId: { type: String, default: null },
  battleCards: { type: Array, default: () => [] },
  currentSeatIndex: { type: Number, default: 0 },
  targetSelectable: { type: Boolean, default: false },
  selectedTargetId: { type: String, default: null },
  selectedCard: { type: Object, default: null },
})

defineEmits(['select'])

const selectable = computed(() => props.targetSelectable && !!props.selectedCard)

function isTargetValid(p) {
  if (!p || (p.hp ?? 4) <= 0) return false
  const t = props.selectedCard?.rankOrName || props.selectedCard?.type
  if (t === '杀') return p.playerId !== props.myPlayerId
  if (t === '桃') return p.playerId === props.myPlayerId && (p.hp ?? 4) < (p.maxHp ?? 4)
  return false
}

// 自己排第一（底部），其余按 seatIndex 围成一圈
const sortedPlayers = computed(() => {
  const list = [...(props.players || [])]
  const meIdx = list.findIndex((p) => p.playerId === props.myPlayerId)
  if (meIdx < 0) return list
  const me = list.splice(meIdx, 1)[0]
  list.sort((a, b) => (a.seatIndex ?? 0) - (b.seatIndex ?? 0))
  return [me, ...list]
})

// 圆形布局：2–8 人，角度均匀分布，自己在 0°（底部）
function playerPositionStyle(index) {
  const n = sortedPlayers.value.length
  if (n <= 0) return {}
  const r = 38 // 半径 %，避免武将溢出容器
  const angleDeg = (360 * index) / n
  const angleRad = (angleDeg * Math.PI) / 180
  const x = 50 + r * Math.sin(angleRad)
  const y = 50 + r * Math.cos(angleRad)
  return {
    left: `${x}%`,
    top: `${y}%`,
  }
}

// 中央出牌：牌 5 秒后消失
const displayBattleCards = ref([])
const prevBattleIds = ref(new Set())

watch(
  () => props.battleCards,
  (newCards) => {
    const newIds = new Set((newCards || []).map((c) => c.cardId || c.id).filter(Boolean))
    const prev = prevBattleIds.value
    displayBattleCards.value = displayBattleCards.value.filter((dc) => newIds.has(dc.cardId || dc.id))
    for (const c of newCards || []) {
      const id = c.cardId || c.id
      if (!prev.has(id)) {
        const card = {
          ...normalizeCard(c),
          cardId: id,
        }
        displayBattleCards.value.push(card)
        setTimeout(() => {
          displayBattleCards.value = displayBattleCards.value.filter((x) => (x.cardId || x.id) !== id)
        }, 5000)
      }
    }
    prevBattleIds.value = newIds
  },
  { immediate: true }
)

function normalizeCard(item) {
  if (item.id && (item.suit || item.rankOrName)) return item
  return {
    id: item.cardId || item.id || '?',
    suit: item.suit || 'SPADE',
    rankOrName: item.rankOrName || item.rank || item.cardType || item.type || '?',
  }
}
</script>
