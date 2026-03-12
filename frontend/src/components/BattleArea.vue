<template>
  <div class="rounded-xl border-2 border-dashed border-sanguo-gold/40 bg-sanguo-dark/60 p-4 min-h-[120px] flex items-center justify-center gap-2 flex-wrap">
    <template v-if="cards.length">
      <Card
        v-for="(item, i) in cards"
        :key="item.cardId || item.id || i"
        :card="normalizeCard(item)"
        :can-play="false"
        class="scale-90 pointer-events-none"
      />
    </template>
    <span v-else class="text-slate-500 text-sm">当前出的牌将显示在此</span>
  </div>
</template>

<script setup>
import Card from './Card.vue'

const props = defineProps({
  /** 当前出的牌列表，项可为 { cardId, playerId } 或完整 card 对象 */
  cards: { type: Array, default: () => [] },
})

function normalizeCard(item) {
  if (item.id && (item.suit || item.rankOrName)) return item
  return {
    id: item.cardId || item.id || '?',
    suit: item.suit || 'SPADE',
    rankOrName: item.rankOrName || item.rank || '?',
  }
}
</script>
