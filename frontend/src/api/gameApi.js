/**
 * 游戏相关 API 封装，统一路径与错误处理。
 */
import { get, post } from './client.js'

const BASE = '/api/game'

export const gameApi = {
  /** 获取可选武将列表 */
  listGenerals() {
    return get(`${BASE}/generals`)
  },

  /** 获取当前游戏状态 */
  getState(roomId, playerId) {
    const q = playerId ? `?playerId=${encodeURIComponent(playerId)}` : ''
    return get(`${BASE}/${roomId}/state${q}`)
  },

  /** 开始游戏 */
  start(roomId, { playerId, generalId }) {
    return post(`${BASE}/${roomId}/start`, { playerId, generalId })
  },

  /** 出牌 */
  play(roomId, { playerId, cardId, targetId, skillId }) {
    const body = { playerId, cardId, targetId }
    if (skillId) body.skillId = skillId
    return post(`${BASE}/${roomId}/play`, body)
  },

  /** 响应杀（承受伤害） */
  respond(roomId, { playerId, action = 'PASS' }) {
    return post(`${BASE}/${roomId}/respond`, { playerId, action })
  },

  /** 濒死响应 */
  respondDying(roomId, { playerId, action, cardId }) {
    const body = { playerId, action }
    if (cardId) body.cardId = cardId
    return post(`${BASE}/${roomId}/respondDying`, body)
  },

  /** 结束回合 */
  endRound(roomId, { playerId }) {
    return post(`${BASE}/${roomId}/endRound`, { playerId })
  },
}
