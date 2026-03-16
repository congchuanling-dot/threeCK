/**
 * 牌相关工具函数，统一牌名/类型判断，提升可复用性与可维护性
 */
import { CARD_TYPES } from '../constants/cardTypes.js'

/**
 * 获取牌的显示类型（兼容 rankOrName / type / cardType）
 * @param {Object} card - 牌对象
 * @returns {string}
 */
export function getCardType(card) {
  if (!card) return ''
  return card.rankOrName || card.type || card.cardType || ''
}

/**
 * 判断牌是否为指定类型
 */
export function isCardType(card, type) {
  return getCardType(card) === type
}

export function isSha(card) {
  return isCardType(card, CARD_TYPES.SHA)
}

export function isShan(card) {
  return isCardType(card, CARD_TYPES.SHAN)
}

export function isTao(card) {
  return isCardType(card, CARD_TYPES.TAO)
}

/**
 * 规范化牌对象（兼容多种后端格式）
 */
export function normalizeCard(item) {
  if (!item) return null
  if (item.id && (item.suit || item.rankOrName)) return item
  return {
    id: item.cardId || item.id || '?',
    suit: item.suit || 'SPADE',
    rankOrName: item.rankOrName || item.rank || item.cardType || item.type || '?',
  }
}
