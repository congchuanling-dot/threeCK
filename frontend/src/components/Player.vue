<template>
  <div
    class="flex items-center gap-3 rounded-xl bg-sanguo-dark/80 border border-sanguo-gold/30 px-3 py-2 transition-colors"
    :class="{ 'ring-1 ring-sanguo-gold': isCurrentTurn }"
  >
    <!-- 头像：AI 生成原画或占位 -->
    <div class="relative flex-shrink-0 w-12 h-12 rounded-lg overflow-hidden border border-sanguo-gold/50 bg-sanguo-dark">
      <img
        v-if="player.avatar"
        :src="player.avatar"
        :alt="player.nickname"
        class="w-full h-full object-cover"
      />
      <div
        v-else
        class="w-full h-full flex items-center justify-center text-sanguo-gold font-bold text-lg bg-gradient-to-br from-amber-900/40 to-amber-950/40"
      >
        {{ avatarLetter }}
      </div>
      <!-- 当前回合标识 -->
      <div
        v-if="isCurrentTurn"
        class="absolute inset-0 flex items-center justify-center bg-amber-500/20 rounded-lg"
      >
        <span class="text-amber-400 text-xs font-bold">回合中</span>
      </div>
    </div>
    <!-- 昵称、血量、手牌数 -->
    <div class="flex-1 min-w-0">
      <div class="font-semibold text-sanguo-paper truncate">{{ player.nickname || player.playerId }}</div>
      <div class="flex items-center gap-3 text-sm text-amber-200/90">
        <span class="flex items-center gap-1">
          <span class="text-red-400">♥</span>
          <span>{{ player.hp ?? 4 }}</span>
          <span class="text-slate-400">/ {{ player.maxHp ?? 4 }}</span>
        </span>
        <span class="text-slate-400">手牌 {{ player.handCount ?? 0 }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  player: { type: Object, required: true },
  /** 是否为当前回合玩家（按 currentSeatIndex 判断） */
  isCurrentTurn: { type: Boolean, default: false },
})

const avatarLetter = computed(() => {
  const name = props.player?.nickname || props.player?.playerId || ''
  return name.charAt(0).toUpperCase() || '?'
})
</script>
