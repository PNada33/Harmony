package xd.harm.modules.impl.player.autobuy;

public class PotionEffectMatcher {
    public final int id;
    public final int amplifier;
    public final int duration;

    public PotionEffectMatcher(int id, int amplifier, int duration) {
        this.id = id;
        this.amplifier = amplifier;
        this.duration = duration;
    }

    public PotionEffectMatcher(int id, int amplifier) {
        this(id, amplifier, -1);
    }
}
