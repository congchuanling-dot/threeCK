<template>
  <div class="rounded-xl border-2 border-sanguo-gold/40 bg-sanguo-dark/80 p-6 max-w-xl mx-auto">
    <h2 class="text-lg font-bold text-sanguo-gold mb-4">游戏大厅</h2>
    <div class="space-y-4">
      <div>
        <label class="block text-sm text-amber-200/80 mb-2">武将描述</label>
        <textarea
          v-model="generalDesc"
          class="w-full rounded-lg border border-sanguo-gold/40 bg-black/30 text-sanguo-paper placeholder-slate-500 px-3 py-2 focus:ring-2 focus:ring-sanguo-gold/50 focus:border-sanguo-gold/50 outline-none resize-none"
          rows="3"
          placeholder="例如：关羽，字云长，武力高强，忠义双全..."
        />
      </div>
      <button
        class="w-full py-3 rounded-lg bg-gradient-to-r from-amber-700/80 to-sanguo-red/80 text-white font-semibold hover:from-amber-600 hover:to-red-800 transition-all shadow-lg disabled:opacity-50 disabled:cursor-not-allowed"
        :disabled="generating"
        @click="generateGeneral"
      >
        {{ generating ? '生成中…' : 'AI 生成武将' }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const generalDesc = ref('')
const generating = ref(false)

const emit = defineEmits(['generated'])

async function generateGeneral() {
  if (generating.value || !generalDesc.value.trim()) return
  generating.value = true
  try {
    // 模拟 AI 生成：实际可替换为调用后端 API
    await new Promise((r) => setTimeout(r, 1200))
    const general = {
      name: generalDesc.value.trim().slice(0, 20) || '未命名武将',
      title: 'AI 生成',
      imageUrl: null,
      skillDescription:
        '【忠义】当你成为杀的目标时，可弃置一张牌令该杀无效。\n【武圣】你可以将一张红色牌当【杀】使用或打出。',
    }
    emit('generated', general)
  } finally {
    generating.value = false
  }
}
</script>
