<template>
  <div class="flex flex-col rounded-xl border-2 border-amber-700/50 bg-amber-950/30 p-4 shadow-inner transition-all duration-300">
    <div class="flex-shrink-0 text-amber-200/90 text-sm mb-2 font-medium">
      手牌 ({{ cards.length }}) · 当前阶段：<span class="text-sanguo-gold font-semibold">{{ phaseLabel }}</span>
    </div>
    <div class="flex flex-wrap gap-2 justify-center items-end min-h-[100px]">
      <Card
        v-for="(card, idx) in cards"
        :key="card.id"
        :card="card"
        :can-play="canPlay"
        :selected="selectedId === card.id"
        :style="{ animationDelay: `${idx * 20}ms` }"
        class="card-enter"
        @play="$emit('select', $event)"
      />
    </div>
  </div>
</template>

<script setup>
import Card from './Card.vue'
import { computed } from 'vue'

const props = defineProps({
  cards: { type: Array, default: () => [] },
  canPlay: { type: Boolean, default: false },
  currentPhase: { type: String, default: 'PREPARE' },
  selectedId: { type: String, default: null },
})

defineEmits(['select'])

const phaseLabel = computed(() => {
  const map = {
    PREPARE: '准备',
    DRAW: '摸牌',
    PLAY: '出牌',
    DISCARD: '弃牌',
    END: '结束',
  }
  return map[props.currentPhase] ?? props.currentPhase
})
</script>
