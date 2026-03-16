<template>
  <div class="h-full flex flex-col rounded-lg border border-sanguo-gold/30 bg-sanguo-dark/90 overflow-hidden min-h-0">
    <div class="flex-shrink-0 px-3 py-2 border-b border-sanguo-gold/30 text-sanguo-gold font-semibold text-sm">
      战局记录
    </div>
    <ul class="flex-1 min-h-0 overflow-y-auto overflow-x-hidden p-2 space-y-1 text-sm">
      <li
        v-for="(entry, i) in displayLog"
        :key="i"
        class="flex gap-2 py-1 px-2 rounded bg-black/20 text-amber-100/90"
      >
        <span class="text-slate-400 flex-shrink-0">{{ entry.time }}</span>
        <span class="text-amber-300 font-medium">{{ entry.who }}</span>
        <span>{{ entry.text }}</span>
      </li>
      <li v-if="displayLog.length === 0" class="text-slate-500 py-4 text-center">
        暂无记录
      </li>
    </ul>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  /** 日志项 { who, text, time }，可以是数组或 Ref<Array> */
  log: { type: [Array, Object], default: () => [] },
})

const displayLog = computed(() => {
  const raw = Array.isArray(props.log)
    ? props.log
    : Array.isArray(props.log?.value)
      ? props.log.value
      : []
  return [...raw].reverse()
})
</script>
