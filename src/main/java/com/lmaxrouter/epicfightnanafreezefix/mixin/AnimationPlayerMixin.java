package com.lmaxrouter.epicfightnanafreezefix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import yesman.epicfight.api.animation.AnimationPlayer;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/**
 * [长期记忆: 013/017] 根因层修复 —— NaN 的出生点。
 *
 * AnimationPlayer.tick 对零时长循环动画(EMPTY_ANIMATION: totalTime = 0 且
 * isRepeat = true)执行 elapsedTime %= totalTime,即 elapsedTime %= 0.0F,
 * 每一 tick 产出 NaN;NaN 随后毒化 ConcurrentLinkAnimation.startsAt,最终把
 * render thread 带进 AnimationClip.getPoseInTime 二分搜索的死循环(硬冻结)。
 *
 * 修复语义:零时长循环动画永远停在第 0 秒 —— 时间钉 0,跳过整个 tick。
 * EMPTY 的 pose 与时间无关,语义完全等价。
 *
 * 注入策略:tick 方法 HEAD + cancellable。不依赖方法体内任何一行实现
 * (不用 ordinal 钉 getTotalTime() 调用序号),上游改 tick 内部实现打不到
 * 这里,鲁棒性最高。
 *
 * 自愈性:守卫同时是解药 —— 若 elapsedTime 已被毒化为 NaN(历史 bug 残留
 * 或其他 NaN 源),只要当前播放的是零长循环,下一 tick 即被洗回 0。
 */
@Mixin(AnimationPlayer.class)
public abstract class AnimationPlayerMixin {

    @Shadow
    protected float elapsedTime;

    @Shadow
    protected float prevElapsedTime;

    @Shadow
    protected AssetAccessor<? extends DynamicAnimation> play;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void epicfightnan$guardZeroLengthLoop(LivingEntityPatch<?> entitypatch, CallbackInfo ci) {
        DynamicAnimation currentPlay = this.play.get();

        // 零时长循环动画:totalTime <= 0 且 isRepeat。
        // 写成 !(totalTime > 0) 而非 totalTime <= 0:NaN 的 totalTime 也一并拦截。
        if (currentPlay.isRepeat() && !(currentPlay.getTotalTime() > 0.0F)) {
            // 时间钉 0,同时覆盖洗掉可能已存在的 NaN(自愈)
            this.elapsedTime = 0.0F;
            this.prevElapsedTime = 0.0F;
            ci.cancel();
        }
    }
}