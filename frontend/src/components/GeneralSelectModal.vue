<template>
  <Teleport to="body">
    <Transition name="modal">
      <div
        v-if="visible"
        class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm"
        @click.self="close"
      >
        <div
          class="max-w-lg w-full rounded-2xl overflow-hidden shadow-2xl border-2 border-sanguo-gold/60 bg-gradient-to-b from-amber-950/95 to-sanguo-dark"
          @click.stop
        >
          <div class="p-4 border-b border-sanguo-gold/30">
            <h2 class="text-xl font-bold text-sanguo-gold text-center">选择武将</h2>
            <p class="text-amber-200/80 text-sm text-center mt-1">选择你的武将开始对局</p>
          </div>
          <div v-if="loading" class="p-8 text-center text-amber-200">加载中...</div>
          <div v-else class="p-4 grid gap-3 max-h-[60vh] overflow-y-auto">
            <button
              v-for="g in generals"
              :key="g.id"
              class="flex flex-col items-start gap-2 p-4 rounded-xl border-2 transition-all text-left"
              :class="selectedId === g.id
                ? 'border-sanguo-gold bg-sanguo-gold/20'
                : 'border-amber-700/50 bg-amber-950/30 hover:border-amber-600/70'"
              @click="selectedId = g.id"
            >
              <span class="font-bold text-amber-100">{{ g.name }}</span>
              <div v-if="g.skills?.length" class="flex flex-wrap gap-2">
                <span
                  v-for="s in g.skills"
                  :key="s.id"
                  class="px-2 py-0.5 rounded bg-amber-700/50 text-amber-200 text-xs"
                >
                  {{ s.name }}
                </span>
              </div>
              <p v-if="g.skills?.length" class="text-amber-200/80 text-xs">{{ g.skills[0]?.description }}</p>
            </button>
          </div>
          <div class="p-4 flex justify-end gap-3 border-t border-sanguo-gold/30">
            <button
              class="px-4 py-2 rounded-lg border border-amber-500/50 text-amber-200 hover:bg-amber-500/20"
              @click="close"
            >
              取消
            </button>
            <button
              class="px-4 py-2 rounded-lg bg-sanguo-gold/30 border border-sanguo-gold text-sanguo-gold hover:bg-sanguo-gold/40 disabled:opacity-40"
              :disabled="!selectedId"
              @click="confirm"
            >
              确定
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
})

const emit = defineEmits(['confirm', 'close'])

const generals = ref([])
const loading = ref(false)
const selectedId = ref(null)

watch(() => props.visible, async (visible) => {
  if (visible) {
    selectedId.value = null
    loading.value = true
    try {
      const res = await fetch('/api/game/generals')
      const data = await res.json()
      generals.value = Array.isArray(data) ? data : []
      if (generals.value.length) selectedId.value = generals.value[0].id
    } catch (e) {
      console.error('load generals failed', e)
      generals.value = []
    } finally {
      loading.value = false
    }
  }
})

function close() {
  emit('close')
}

function confirm() {
  if (selectedId.value) {
    emit('confirm', selectedId.value)
    close()
  }
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
</style>
