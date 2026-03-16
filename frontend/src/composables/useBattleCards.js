/**
 * 中央出牌区展示逻辑：牌 5 秒后消失
 */
import { ref, watch } from 'vue'
import { normalizeCard } from '../utils/cardUtils.js'
import { BATTLE_CARD_DISPLAY_MS } from '../constants/config.js'

/**
 * @param {object} game - useGameSocket 返回值，需包含 battleCards
 */
export function useBattleCards(game) {
  const battleCardsRef = game?.battleCards
  const displayBattleCards = ref([])
  const prevBattleIds = ref(new Set())

  watch(
    () => (Array.isArray(battleCardsRef?.value) ? battleCardsRef.value : []),
    (newCards) => {
      const newIds = new Set(
        (newCards || []).map((c) => c.cardId || c.id).filter(Boolean)
      )
      const prev = prevBattleIds.value
      displayBattleCards.value = displayBattleCards.value.filter((dc) =>
        newIds.has(dc?.cardId || dc?.id)
      )
      for (const c of newCards || []) {
        const id = c.cardId || c.id
        if (!id || prev.has(id)) continue
        const card = { ...normalizeCard(c), cardId: id }
        displayBattleCards.value.push(card)
        setTimeout(() => {
          displayBattleCards.value = displayBattleCards.value.filter(
            (x) => (x?.cardId || x?.id) !== id
          )
        }, BATTLE_CARD_DISPLAY_MS)
      }
      prevBattleIds.value = newIds
    },
    { immediate: true }
  )

  return { displayBattleCards }
}
