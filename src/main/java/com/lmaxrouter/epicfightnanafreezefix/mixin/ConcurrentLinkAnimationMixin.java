package com.lmaxrouter.epicfightnanafreezefix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import yesman.epicfight.api.animation.types.ConcurrentLinkAnimation;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

/**
 * [长期记忆: 013/017] 纵深防御 —— 毒化驻留点。
 *
 * 扩散链:Layer.playLivingAnimation 把 AnimationPlayer 的毒化 elapsedTime
 * 写入 ConcurrentLinkAnimation.startsAt;end() 又会把 startsAt 写回
 * player.setElapsedTime(startsAt) —— 毒化在混合动画里长期驻留,即使
 * 根修已掐断新 NaN 的出生,历史驻留的 NaN 仍会在 link 动画的每次
 * pose 查询里复活。
 *
 * 修复语义:getPoseByTime 入口处 startsAt 为 NaN 时洗回 0。退化行为:
 * 混合动画退化为 next 动画第 0 帧的插值(非空 Pose),不产生 T-Pose
 * 闪烁。
 *
 * 目标类是 @OnlyIn(Dist.CLIENT),本 mixin 只进 mixins.json 的 client
 * 列表 —— 放进通用列表会在专用服务端直接类加载失败。
 */
// remap = false:目标为 epicfight 类,生产不混淆;searge 映射无 mod 成员,必须关
@Mixin(value = ConcurrentLinkAnimation.class, remap = false)
public abstract class ConcurrentLinkAnimationMixin {

    @Shadow
    protected float startsAt;

    @Inject(method = "getPoseByTime", at = @At("HEAD"))
    private void epicfightnan$sanitizeStartsAt(LivingEntityPatch<?> entitypatch, float time, float partialTicks, CallbackInfo ci) {
        if (Float.isNaN(this.startsAt)) {
            this.startsAt = 0.0F;
        }
    }
}