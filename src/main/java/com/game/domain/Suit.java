package com.game.domain;

/**
 * 扑克花色枚举。
 * 可用于标准扑克牌或扩展为三国杀风格的牌型。
 */
public enum Suit {
    /** 黑桃 */
    SPADE("黑桃", "♠"),
    /** 红桃 */
    HEART("红桃", "♥"),
    /** 梅花 */
    CLUB("梅花", "♣"),
    /** 方片 */
    DIAMOND("方片", "♦");

    private final String displayName;
    private final String symbol;

    Suit(String displayName, String symbol) {
        this.displayName = displayName;
        this.symbol = symbol;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSymbol() {
        return symbol;
    }
}
