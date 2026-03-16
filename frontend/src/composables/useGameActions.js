/**
 * 游戏操作 Composable：封装所有 HTTP 游戏动作，统一状态更新与日志。
 */
import { ref } from 'vue'
import { gameApi } from '../api/gameApi.js'
import { getCardType, isSha, isTao, isJiu } from '../constants/cardTypes.js'

/**
 * @param {import('./useGameSocket.js').UseGameSocket} game - useGameSocket 返回值
 */
export function useGameActions(game) {
  const acceptDamageLoading = ref(false)
  const endRoundLoading = ref(false)

  function handleApiResult(data, successLog) {
    if (data?.ok) {
      if (successLog) game.addLog('系统', successLog)
      game.applyGameState(data)
      return true
    }
    if (data?.message) game.addLog('系统', data.message)
    return false
  }

  async function startGame(generalId) {
    const rid = game.roomId?.value
    const pid = game.myPlayerId?.value
    if (!rid || !pid) return
    try {
      const data = await gameApi.start(rid, { playerId: pid, generalId })
      handleApiResult(data, '游戏开始')
    } catch (e) {
      console.error('startGame failed', e)
      game.addLog('系统', '开局失败')
    }
  }

  async function playCard(card, { targetId, useLongdan = false } = {}) {
    const rid = game.roomId?.value
    const pid = game.myPlayerId?.value
    if (!rid || !pid || !card?.id) return
    const cardType = getCardType(card)
    const finalTargetId = targetId ?? (isSha(cardType)
      ? game.players?.value?.find((p) => p.playerId !== pid && (p.hp ?? 4) > 0)?.playerId
      : isTao(cardType) || isJiu(cardType) ? pid : null)
    try {
      const body = { playerId: pid, cardId: card.id, targetId: finalTargetId }
      if (useLongdan) body.skillId = 'longdan'
      const data = await gameApi.play(rid, body)
      if (handleApiResult(data)) return true
    } catch (e) {
      console.error('playCard failed', e)
      game.addLog('系统', '出牌失败')
    }
    return false
  }

  async function acceptDamage() {
    const rid = game.roomId?.value
    const pid = game.myPlayerId?.value
    if (!rid || !pid) return
    acceptDamageLoading.value = true
    try {
      const data = await gameApi.respond(rid, { playerId: pid, action: 'PASS' })
      handleApiResult(data) || game.addLog('系统', data?.message ?? '失败')
    } catch (e) {
      console.error('acceptDamage failed', e)
      game.addLog('系统', '网络错误')
    } finally {
      acceptDamageLoading.value = false
    }
  }

  async function respondDyingTao(cardId) {
    const rid = game.roomId?.value
    const pid = game.myPlayerId?.value
    if (!rid || !pid || !cardId) return
    try {
      const data = await gameApi.respondDying(rid, { playerId: pid, action: 'USE_TAO', cardId })
      handleApiResult(data)
    } catch (e) {
      console.error('respondDyingTao failed', e)
      game.addLog('系统', '出桃失败')
    }
  }

  async function respondDyingPass() {
    const rid = game.roomId?.value
    const pid = game.myPlayerId?.value
    if (!rid || !pid) return
    try {
      const data = await gameApi.respondDying(rid, { playerId: pid, action: 'PASS' })
      handleApiResult(data)
    } catch (e) {
      console.error('respondDyingPass failed', e)
    }
  }

  async function endRound() {
    const rid = game.roomId?.value
    const pid = game.myPlayerId?.value
    if (!rid || !pid) return
    endRoundLoading.value = true
    try {
      const data = await gameApi.endRound(rid, { playerId: pid })
      handleApiResult(data)
    } catch (e) {
      console.error('endRound failed', e)
      game.addLog('系统', '结束回合失败')
    } finally {
      endRoundLoading.value = false
    }
  }

  return {
    startGame,
    playCard,
    acceptDamage,
    respondDyingTao,
    respondDyingPass,
    endRound,
    acceptDamageLoading,
    endRoundLoading,
  }
}
