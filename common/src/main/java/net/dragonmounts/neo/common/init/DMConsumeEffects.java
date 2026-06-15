package net.dragonmounts.neo.common.init;

// 1.21.1 PORT: vanilla ConsumeEffect / ConsumeEffect.Type registry does not exist in 1.21.1.
// ContorlGrowthConsumeEffect is now a plain record applied directly, so there is nothing to
// register here. Kept as a no-op shell so any central init() call site still resolves.
public class DMConsumeEffects {
    public void init() {}
}
