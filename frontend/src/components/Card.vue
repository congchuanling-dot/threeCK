<template>
  <div
    class="relative card-face bg-gradient-to-b from-amber-50 to-amber-100/90 text-sanguo-dark cursor-pointer select-none transition-all duration-300 ease-out min-w-[72px] w-[72px] hover:scale-110 hover:z-10 hover:-translate-y-2 hover:shadow-xl hover:shadow-amber-900/30 hover:shadow-amber-500/20"
    :class="[
      canPlay ? 'hover:ring-2 hover:ring-sanguo-gold' : 'opacity-80 cursor-not-allowed',
      { 'ring-2 ring-sanguo-gold scale-105 -translate-y-1 shadow-amber-500/40': selected }
    ]"
    @click="onClick"
  >
    <!-- 花色标识 -->
    <div class="absolute top-0.5 left-1 text-xs font-bold" :class="suitColor">
      {{ suitSymbol }}
    </div>
    <div class="absolute top-0.5 right-1 text-xs font-bold" :class="suitColor">
      {{ suitSymbol }}
    </div>
    <!-- 点数/牌名 -->
    <div class="flex flex-col items-center justify-center min-h-[72px] py-2">
      <span class="text-lg font-bold text-center leading-tight">{{ displayRank }}</span>
    </div>
    <!-- 底部花色 -->
    <div class="absolute bottom-0.5 left-1/2 -translate-x-1/2 text-xs" :class="suitColor">
      {{ suitSymbol }}
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const SUIT_SYMBOLS = { SPADE: '♠', HEART: '♥', CLUB: '♣', DIAMOND: '♦' }
const SUIT_COLORS = {
  SPADE: 'text-gray-800',
  HEART: 'text-red-600',
  CLUB: 'text-green-800',
  DIAMOND: 'text-blue-600',
}

const props = defineProps({
  /** 卡牌对象 { id, suit, rankOrName } */
  card: { type: Object, required: true },
  /** 是否可出牌（当前阶段为出牌阶段等） */
  canPlay: { type: Boolean, default: false },
  /** 是否选中态 */
  selected: { type: Boolean, default: false },
})

const emit = defineEmits(['play'])

const suitSymbol = computed(() =>
  props.card?.suit ? SUIT_SYMBOLS[props.card.suit] ?? props.card.suit : '?'
)
const suitColor = computed(() =>
  props.card?.suit ? SUIT_COLORS[props.card.suit] ?? 'text-gray-700' : 'text-gray-700'
)
const displayRank = computed(() => props.card?.rankOrName ?? props.card?.rank ?? '?')

function onClick() {
  if (props.canPlay && props.card?.id) emit('play', props.card)
}
</script>
