package com.lmaxrouter.epicfightnanafreezefix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import yesman.epicfight.api.animation.Pose;
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
 *
 * 1.0.1 崩溃修复(2026-11-03,[长期记忆: 022/024]):1.0.0 版 handler 尾参
 * 误用 CallbackInfo,而 getPoseByTime 运行时返回非 void —— mixin 校验报
 * "CallbackInfoReturnable is required",APPLY 失败,游戏死。触发链:进世界
 * 首帧 LocalPlayer 构造 → epicfight 挂 capability → 目标类首次加载 →
 * mixin 转换爆炸;主菜单不触发目标类加载,装载期全程零报错。修正:尾参改
 * CallbackInfoReturnable<Pose>,泛型 = 运行时 jar javap 实测返回类型(本机
 * dev jar 与运行时 jar MD5 一致,双重实证);handler 逻辑零改动:不
 * setReturnValue 不 cancel,纯观察合法。教训固化:mod 目标类 + remap=false
 * 时编译期签名校验不可信,handler 尾参类型必须对运行时 jar javap 实证。
 */
// remap = false:目标为 epicfight 类,生产不混淆;searge 映射无 mod 成员,必须关
@Mixin(value = ConcurrentLinkAnimation.class, remap = false)
public abstract class ConcurrentLinkAnimationMixin {

    @Shadow
    protected float startsAt;

    @Inject(method = "getPoseByTime", at = @At("HEAD"))
    private void epicfightnan$sanitizeStartsAt(LivingEntityPatch<?> entitypatch, float time, float partialTicks, CallbackInfoReturnable<Pose> cir) {
        if (Float.isNaN(this.startsAt)) {
            this.startsAt = 0.0F;
        }
    }
}