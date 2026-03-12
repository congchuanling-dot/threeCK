<template>
  <div class="h-full flex flex-col rounded-lg border border-sanguo-gold/30 bg-sanguo-dark/90 overflow-hidden">
    <div class="flex-shrink-0 px-3 py-2 border-b border-sanguo-gold/30 text-sanguo-gold font-semibold text-sm">
      玩家
    </div>
    <div class="flex-1 overflow-y-auto p-2 space-y-2">
      <Player
        v-for="p in sortedPlayers"
        :key="p.playerId"
        :player="p"
        :is-current-turn="currentSeatIndex === p.seatIndex"
      />
    </div>
  </div>
</template>

<script setup>
import Player from './Player.vue'
import { computed } from 'vue'

const props = defineProps({
  players: { type: Array, default: () => [] },
  currentSeatIndex: { type: Number, default: 0 },
})

const sortedPlayers = computed(() =>
  [...(props.players || [])].sort((a, b) => (a.seatIndex ?? 0) - (b.seatIndex ?? 0))
)
</script>
