<template>
  <Transition name="kill-arrow">
    <div
      v-if="visible && fromPos && toPos"
      class="fixed inset-0 pointer-events-none z-50 flex items-center justify-center"
    >
      <svg
        class="absolute w-full h-full"
        xmlns="http://www.w3.org/2000/svg"
      >
        <defs>
          <marker
            id="arrowhead"
            markerWidth="12"
            markerHeight="12"
            refX="10"
            refY="6"
            orient="auto"
          >
            <path d="M 0 0 L 12 6 L 0 12 z" fill="#dc2626" stroke="#fca5a5" stroke-width="1" />
          </marker>
          <linearGradient id="arrowGrad" x1="0%" y1="0%" x2="100%" y2="0%">
            <stop offset="0%" stop-color="#fca5a5" />
            <stop offset="100%" stop-color="#dc2626" />
          </linearGradient>
        </defs>
        <line
          :x1="fromPos.x"
          :y1="fromPos.y"
          :x2="toPos.x"
          :y2="toPos.y"
          stroke="url(#arrowGrad)"
          stroke-width="4"
          stroke-linecap="round"
          marker-end="url(#arrowhead)"
          class="animate-kill-line"
        />
      </svg>
    </div>
  </Transition>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'

const props = defineProps({
  /** 是否显示 { fromPlayerId, toPlayerId } */
  killArrow: { type: Object, default: null },
})

const visible = ref(false)
const fromPos = ref(null)
const toPos = ref(null)

function getCenter(el) {
  if (!el) return null
  const rect = el.getBoundingClientRect()
  return {
    x: rect.left + rect.width / 2,
    y: rect.top + rect.height / 2,
  }
}

function updatePositions() {
  if (!props.killArrow?.fromPlayerId || !props.killArrow?.toPlayerId) {
    visible.value = false
    return
  }
  const fromEl = document.querySelector(`[data-player-id="${props.killArrow.fromPlayerId}"]`)
  const toEl = document.querySelector(`[data-player-id="${props.killArrow.toPlayerId}"]`)
  if (!fromEl || !toEl) {
    visible.value = false
    return
  }
  fromPos.value = getCenter(fromEl)
  toPos.value = getCenter(toEl)
  visible.value = !!fromPos.value && !!toPos.value
}

watch(() => props.killArrow, (val) => {
  if (val) {
    setTimeout(updatePositions, 80)
  } else {
    visible.value = false
  }
}, { immediate: true })

onMounted(() => {
  if (props.killArrow) setTimeout(updatePositions, 100)
})
</script>

<style scoped>
.kill-arrow-enter-active {
  transition: opacity 0.2s ease;
}
.kill-arrow-leave-active {
  transition: opacity 0.8s ease;
}
.kill-arrow-enter-from,
.kill-arrow-leave-to {
  opacity: 0;
}

@keyframes kill-line {
  0% { opacity: 1; stroke-width: 4; }
  70% { opacity: 1; stroke-width: 4; }
  100% { opacity: 0; stroke-width: 2; }
}

.animate-kill-line {
  animation: kill-line 1.2s ease-out forwards;
}
</style>
