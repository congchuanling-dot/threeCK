<template>
  <Transition name="card-fly-fade">
    <div
      v-if="visible && cardFlyData?.card && cardFlyData?.fromPlayerId"
      class="fixed inset-0 pointer-events-none z-40 overflow-hidden"
    >
      <!-- 飞行中的卡牌：从角色位置飞向中央牌区，带抛物轨迹 -->
      <div
        ref="flyingCardRef"
        class="card-fly-item absolute will-change-transform"
        :style="flyStyle"
      >
        <div class="card-face bg-gradient-to-b from-amber-50 to-amber-100/90 text-sanguo-dark rounded-lg border-2 border-sanguo-gold/40 shadow-xl min-w-[56px] w-[56px] flex flex-col items-center justify-center py-1.5">
          <span class="text-sm font-bold">{{ cardFlyData.card.rankOrName || cardFlyData.card.type || '?' }}</span>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref, computed, watch, onMounted, nextTick } from 'vue'

const props = defineProps({
  /** { card: { id, rankOrName }, fromPlayerId } */
  cardFly: { type: Object, default: null },
  /** 中央牌区元素的引用，用于计算目标位置 */
  battleAreaRef: { type: Object, default: null },
})

const emit = defineEmits(['done'])

const visible = ref(false)
const flyStyle = ref({})
const flyingCardRef = ref(null)

const DURATION_MS = 550

function getRect(el) {
  if (!el) return null
  const rect = el.getBoundingClientRect()
  return {
    x: rect.left + rect.width / 2,
    y: rect.top + rect.height / 2,
    w: rect.width,
    h: rect.height,
  }
}

function startFly() {
  if (!props.cardFly?.fromPlayerId || !props.cardFly?.card) return
  const fromEl = document.querySelector(`[data-player-id="${props.cardFly.fromPlayerId}"]`)
  const toEl = (props.battleAreaRef && (typeof props.battleAreaRef === 'object' && 'value' in props.battleAreaRef ? props.battleAreaRef.value : props.battleAreaRef)) || document.querySelector('[data-battle-center]')
  if (!fromEl || !toEl) {
    emit('done')
    return
  }
  const from = getRect(fromEl)
  const to = getRect(toEl)
  if (!from || !to) {
    emit('done')
    return
  }
  // 卡牌尺寸 56px，居中需偏移 -28
  const cardHalf = 28
  visible.value = true
  flyStyle.value = {
    left: `${from.x - cardHalf}px`,
    top: `${from.y - cardHalf}px`,
    width: '56px',
    height: '56px',
  }
  nextTick(() => {
    requestAnimationFrame(() => {
      requestAnimationFrame(() => {
        const dx = to.x - from.x
        const dy = to.y - from.y
        // 抛物弧线：用贝塞尔曲线模拟先上扬再落下的轨迹，带轻微旋转
        const arcBezier = 'cubic-bezier(0.33, 0.9, 0.66, 0.1)'
        flyStyle.value = {
          ...flyStyle.value,
          transition: `transform ${DURATION_MS}ms ${arcBezier}`,
          transform: `translate(${dx}px, ${dy}px) scale(0.92) rotate(${dy > 0 ? 2 : -2}deg)`,
          opacity: '0.98',
        }
        setTimeout(() => {
          visible.value = false
          flyStyle.value = {}
          emit('done')
        }, DURATION_MS + 50)
      })
    })
  })
}

const cardFlyData = computed(() => props.cardFly)

watch(
  () => props.cardFly,
  (val) => {
    if (val?.card && val?.fromPlayerId) {
      // 等待 DOM 更新（特别是 data-player-id 的 PlayerEntity）
      setTimeout(() => startFly(), 60)
    }
  },
  { flush: 'post' }
)

onMounted(() => {
  if (props.cardFly?.card && props.cardFly?.fromPlayerId) {
    setTimeout(() => startFly(), 80)
  }
})
</script>

<style scoped>
.card-fly-item {
  z-index: 41;
  transition: opacity 0.15s ease-out;
}

.card-fly-fade-enter-active,
.card-fly-fade-leave-active {
  transition: opacity 0.15s ease;
}
.card-fly-fade-enter-from,
.card-fly-fade-leave-to {
  opacity: 0;
}
</style>
