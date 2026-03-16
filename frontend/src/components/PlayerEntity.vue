<template>
  <div
    :data-player-id="player?.playerId"
    class="relative w-36 flex-shrink-0 rounded-xl overflow-hidden transition-all duration-300 ease-out"
    :class="[
      frameBase,
      selectable && 'cursor-pointer hover:scale-105 hover:shadow-2xl hover:shadow-amber-500/20',
      selected && 'ring-2 ring-amber-400 scale-105 shadow-2xl shadow-amber-500/30',
      isCurrentTurn && 'ring-1 ring-amber-500/60 turn-glow'
    ]"
    @click="onClick"
  >
    <!-- 武将框装饰 -->
    <div class="absolute inset-0 rounded-xl pointer-events-none border-2" :class="selected ? 'border-amber-400' : 'border-amber-700/50'" />
    <!-- 头像区 -->
    <div class="h-20 flex items-center justify-center bg-gradient-to-b from-amber-900/60 to-amber-950/80 border-b-2 border-amber-600/50">
      <div class="w-14 h-14 rounded-lg overflow-hidden border-2 border-amber-500/60 bg-sanguo-dark flex items-center justify-center">
        <span class="text-2xl font-bold text-amber-300">{{ avatarLetter }}</span>
      </div>
    </div>
    <!-- 信息区 -->
    <div class="px-2 py-2 bg-sanguo-dark/95 border-t border-amber-700/40">
      <div class="text-center font-semibold text-amber-100 text-sm truncate">
        {{ player.nickname || player.playerId }}
      </div>
      <div v-if="player?.general?.name" class="text-center text-sanguo-gold/80 text-xs truncate mt-0.5">
        {{ player.general.name }}
      </div>
      <div class="flex justify-center items-center gap-2 mt-1">
        <span class="text-red-400 font-bold">{{ player.hp ?? 4 }}</span>
        <span class="text-slate-500">/</span>
        <span class="text-slate-400">{{ player.maxHp ?? 4 }}</span>
        <span class="text-slate-500 text-xs ml-1">手牌{{ player.handCount ?? 0 }}</span>
      </div>
      <!-- 装备栏：武器、防具、-1马、+1马 -->
      <div class="mt-2 grid grid-cols-2 gap-1 text-[10px]">
        <div class="flex items-center gap-0.5 truncate" title="武器">
          <span class="text-sanguo-gold/70 shrink-0">⚔</span>
          <span class="truncate text-amber-200/90">{{ player.weapon?.rankOrName || player.weapon?.type || '无' }}</span>
        </div>
        <div class="flex items-center gap-0.5 truncate" title="防具">
          <span class="text-sanguo-gold/70 shrink-0">🛡</span>
          <span class="truncate text-amber-200/90">{{ player.armor?.rankOrName || player.armor?.type || '无' }}</span>
        </div>
        <div class="flex items-center gap-0.5 truncate" title="-1马(进攻)">
          <span class="text-sanguo-gold/70 shrink-0">-1</span>
          <span class="truncate text-amber-200/90">{{ player.offensiveHorse?.rankOrName || player.offensiveHorse?.type || '无' }}</span>
        </div>
        <div class="flex items-center gap-0.5 truncate" title="+1马(防御)">
          <span class="text-sanguo-gold/70 shrink-0">+1</span>
          <span class="truncate text-amber-200/90">{{ player.defensiveHorse?.rankOrName || player.defensiveHorse?.type || '无' }}</span>
        </div>
      </div>
    </div>
    <div v-if="isCurrentTurn" class="absolute top-0 right-0 px-2 py-0.5 bg-amber-500/90 text-sanguo-dark text-xs font-bold rounded-bl-lg turn-glow shadow-lg shadow-amber-500/50">
      回合中
    </div>
    <div v-if="selectable && !selected" class="absolute inset-0 flex items-center justify-center bg-amber-500/10 rounded-xl pointer-events-none">
      <span class="text-amber-300 text-xs font-medium">点击选择目标</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  player: { type: Object, required: true },
  isCurrentTurn: { type: Boolean, default: false },
  selectable: { type: Boolean, default: false },
  selected: { type: Boolean, default: false },
})

const emit = defineEmits(['select'])

const avatarLetter = computed(() => {
  const name = props.player?.nickname || props.player?.playerId || ''
  return name.charAt(0).toUpperCase() || '?'
})

const frameBase = computed(() =>
  props.selected ? 'bg-amber-900/40' : 'bg-amber-950/60'
)

function onClick() {
  if (props.selectable) emit('select', props.player)
}
</script>
