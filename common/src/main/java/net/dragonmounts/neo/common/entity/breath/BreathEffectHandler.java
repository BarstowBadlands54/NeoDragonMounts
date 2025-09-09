package net.dragonmounts.neo.common.entity.breath;

public interface BreathEffectHandler {
    boolean decayEffectTick();

    boolean isUnaffected();
}
