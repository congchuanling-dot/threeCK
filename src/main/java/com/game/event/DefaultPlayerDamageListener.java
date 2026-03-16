package com.game.event;

import com.game.domain.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 默认“受到伤害”事件监听器示例。
 * 扣减目标体力，并可扩展濒死、反馈等逻辑。
 */
@Component
public class DefaultPlayerDamageListener implements GameEventListener<PlayerDamageEvent> {

    private static final Logger log = LoggerFactory.getLogger(DefaultPlayerDamageListener.class);

    @Override
    public String getSupportedEventType() {
        return PlayerDamageEvent.EVENT_TYPE;
    }

    @Override
    public void onEvent(PlayerDamageEvent event) {
        Player target = event.getTarget();
        int amount = event.getAmount();
        if (target != null && amount > 0) {
            target.takeDamage(amount);
            log.debug("玩家 {} 受到 {} 点伤害，当前体力 {}", target.getPlayerId(), amount, target.getHp());
            // 濒死：HP=0 时进入濒死轮询，暂不判死
            if (target.getHp() <= 0) {
                target.setAlive(true);
                var ctx = event.getContext();
                ctx.setPendingDeath(target.getPlayerId(), target.getNickname(), target.getSeatIndex());
            }
        }
    }
}
