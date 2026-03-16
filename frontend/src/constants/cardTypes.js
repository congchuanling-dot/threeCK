/**
 * 牌型常量，避免魔法字符串，便于统一维护与扩展。
 */
export const CARD_TYPES = {
  SHA: '杀',
  SHAN: '闪',
  TAO: '桃',
  JIU: '酒',
}

/** 需要指定目标的锦囊 */
export const TRICK_NEEDS_TARGET = ['过河拆桥', '顺手牵羊', '借刀杀人', '决斗', '火攻', '铁索连环']

/** 装备牌（目标为自身） */
export const EQUIPMENT_NAMES = ['诸葛连弩', '青釭剑', '寒冰剑', '雌雄双股剑', '青龙偃月刀', '丈八蛇矛', '贯石斧', '方天画戟', '麒麟弓', '八卦阵', '仁王盾', '藤甲', '白银狮子', '赤兔', '大宛', '紫骍', '爪黄飞电', '绝影', '的卢']

/** 无目标锦囊（无中生有、南蛮入侵等） */
export const TRICK_NO_TARGET = ['无中生有', '南蛮入侵', '万箭齐发', '五谷丰登', '桃园结义', '无懈可击']

/**
 * @param {string} type
 * @returns {boolean}
 */
export function isSha(type) {
  return type === CARD_TYPES.SHA
}

/**
 * @param {string} type
 * @returns {boolean}
 */
export function isShan(type) {
  return type === CARD_TYPES.SHAN
}

/**
 * @param {string} type
 * @returns {boolean}
 */
export function isTao(type) {
  return type === CARD_TYPES.TAO
}

/**
 * 从牌对象中解析牌型（兼容 rankOrName / type 等字段）
 * @param {Object} card
 * @returns {string|null}
 */
export function getCardType(card) {
  return card?.rankOrName ?? card?.type ?? null
}

/** 牌是否需要选目标（杀、需目标锦囊，用于目标选择 UI） */
export function cardNeedsTargetForPlay(type) {
  if (!type) return false
  return type === CARD_TYPES.SHA || TRICK_NEEDS_TARGET.includes(type)
}

/** 是否为装备牌（目标固定为自己） */
export function isEquipment(type) {
  return type && EQUIPMENT_NAMES.includes(type)
}

/** 是否为锦囊牌 */
export function isTrickCard(type) {
  return type && !isEquipment(type) && ![CARD_TYPES.SHA, CARD_TYPES.SHAN, CARD_TYPES.TAO, CARD_TYPES.JIU].includes(type)
}

/** 是否为可主动使用的锦囊/装备（非闪） */
export function isPlayableTrickOrEquip(cardType) {
  if (!cardType || cardType === CARD_TYPES.SHAN) return false
  return TRICK_NEEDS_TARGET.includes(cardType) || TRICK_NO_TARGET.includes(cardType) || EQUIPMENT_NAMES.includes(cardType)
}
