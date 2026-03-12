<template>
  <Teleport to="body">
    <Transition name="modal">
      <div
        v-if="visible"
        class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm"
        @click.self="close"
      >
        <div
          class="general-card relative max-w-md w-full rounded-2xl overflow-hidden shadow-2xl border-2 border-sanguo-gold/60 bg-gradient-to-b from-amber-950/95 to-sanguo-dark"
          @click.stop
        >
          <!-- 装饰边框 -->
          <div class="absolute inset-0 rounded-2xl pointer-events-none border border-amber-600/30 m-1" />
          <!-- 顶部纹样 -->
          <div class="h-2 bg-gradient-to-r from-transparent via-sanguo-gold/50 to-transparent" />
          <!-- 原画区域 -->
          <div class="relative h-48 bg-gradient-to-b from-amber-900/50 to-sanguo-dark overflow-hidden">
            <img
              v-if="general?.imageUrl"
              :src="general.imageUrl"
              :alt="general.name"
              class="w-full h-full object-cover"
            />
            <div
              v-else
              class="w-full h-full flex items-center justify-center text-6xl text-sanguo-gold/40 font-serif"
            >
              将
            </div>
            <div class="absolute bottom-0 left-0 right-0 h-16 bg-gradient-to-t from-sanguo-dark to-transparent" />
            <div class="absolute bottom-3 left-4 right-4">
              <h2 class="text-xl font-bold text-sanguo-gold drop-shadow-lg">
                {{ general?.name ?? '未命名武将' }}
              </h2>
              <p v-if="general?.title" class="text-sm text-amber-200/80">{{ general.title }}</p>
            </div>
          </div>
          <!-- 技能描述 -->
          <div class="p-4 space-y-3">
            <h3 class="text-sm font-semibold text-sanguo-gold border-b border-sanguo-gold/30 pb-1">
              技能描述
            </h3>
            <p class="text-sm text-amber-100/90 leading-relaxed whitespace-pre-wrap">
              {{ general?.skillDescription ?? '暂无技能描述' }}
            </p>
          </div>
          <!-- 底部按钮 -->
          <div class="p-4 pt-0 flex justify-end">
            <button
              class="px-5 py-2 rounded-lg bg-sanguo-gold/20 border border-sanguo-gold/50 text-sanguo-gold hover:bg-sanguo-gold/30 transition-colors"
              @click="close"
            >
              关闭
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
const props = defineProps({
  visible: { type: Boolean, default: false },
  /** 武将数据 { name, title?, imageUrl?, skillDescription? } */
  general: { type: Object, default: null },
})

const emit = defineEmits(['close'])

function close() {
  emit('close')
}
</script>

<style scoped>
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.2s ease;
}
.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}
.modal-enter-active .general-card,
.modal-leave-active .general-card {
  transition: transform 0.2s ease;
}
.modal-enter-from .general-card,
.modal-leave-to .general-card {
  transform: scale(0.95);
}
</style>
