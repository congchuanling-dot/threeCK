package com.game.card;

import com.game.card.basic.*;
import com.game.card.equipment.*;
import com.game.card.trick.*;
import com.game.domain.Suit;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 卡牌工厂。根据牌名或物理牌创建对应的卡牌对象。
 * 使用工厂模式，便于扩展新牌型。
 */
public class CardFactory {

    private static final AtomicInteger ID_GEN = new AtomicInteger(1);

    private static final Map<String, CardCreator> CREATORS = new HashMap<>();

    @FunctionalInterface
    private interface CardCreator {
        Card create(String id, Suit suit, int number);
    }

    static {
        // 基础牌
        register("杀", Sha::new);
        register("闪", Shan::new);
        register("桃", Tao::new);
        register("酒", Jiu::new);

        // 锦囊牌
        register("无中生有", WuZhongShengYou::new);
        register("南蛮入侵", NanManRuQin::new);
        register("万箭齐发", WanJianQiFa::new);
        register("过河拆桥", GuoHeChaiQiao::new);
        register("顺手牵羊", ShunShouQianYang::new);
        register("借刀杀人", JieDaoShaRen::new);
        register("五谷丰登", WuGuFengDeng::new);
        register("桃园结义", TaoYuanJieYi::new);
        register("决斗", JueDou::new);
        register("火攻", HuoGong::new);
        register("铁索连环", TieSuoLianHuan::new);
        register("无懈可击", WuXieKeJi::new);

        // 武器
        register("诸葛连弩", ZhuGeLianNu::new);
        register("青釭剑", QingGangJian::new);
        register("寒冰剑", HanBingJian::new);
        register("雌雄双股剑", CiXiongShuangGuJian::new);
        register("青龙偃月刀", QingLongYanYueDao::new);
        register("丈八蛇矛", ZhangBaSheMao::new);
        register("贯石斧", GuanShiFu::new);
        register("方天画戟", FangTianHuaJi::new);
        register("麒麟弓", QiLinGong::new);

        // 防具
        register("八卦阵", BaGuaZhen::new);
        register("仁王盾", RenWangDun::new);
        register("藤甲", TengJia::new);
        register("白银狮子", BaiYinShiZi::new);

        // 坐骑
        register("赤兔", ChiTu::new);
        register("大宛", DaWan::new);
        register("紫骍", ZiXing::new);
        register("爪黄飞电", ZhuaHuangFeiDian::new);
        register("绝影", JueYing::new);
        register("的卢", DiLu::new);
    }

    private static void register(String name, CardCreator creator) {
        CREATORS.put(name, creator);
    }

    /**
     * 根据牌名创建卡牌（自动生成 id）
     */
    public static Optional<Card> create(String name, Suit suit, int number) {
        return create("CARD-" + ID_GEN.incrementAndGet(), name, suit, number);
    }

    /**
     * 根据 id、牌名创建卡牌
     */
    public static Optional<Card> create(String id, String name, Suit suit, int number) {
        CardCreator creator = CREATORS.get(name);
        if (creator == null) return Optional.empty();
        return Optional.of(creator.create(id, suit, number));
    }

    /**
     * 从 domain.Card 创建卡牌，用于与现有引擎集成。
     */
    public static Optional<Card> createFromDomainCard(com.game.domain.Card domainCard) {
        if (domainCard == null) return Optional.empty();
        String name = domainCard.getRankOrName();
        int number = parseNumber(name);
        return create(domainCard.getId(), name, domainCard.getSuit(), number);
    }

    private static int parseNumber(String rankOrName) {
        if (rankOrName == null) return 1;
        return switch (rankOrName) {
            case "A" -> 1;
            case "J" -> 11;
            case "Q" -> 12;
            case "K" -> 13;
            case "2", "3", "4", "5", "6", "7", "8", "9", "10" -> Integer.parseInt(rankOrName);
            default -> 1;
        };
    }

    /**
     * 是否支持该牌名
     */
    public static boolean supports(String name) {
        return CREATORS.containsKey(name);
    }

    /**
     * 获取所有已注册牌名
     */
    public static Set<String> getRegisteredNames() {
        return Collections.unmodifiableSet(CREATORS.keySet());
    }
}
