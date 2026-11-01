package com.lmaxrouter.epicfightnanafreezefix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import yesman.epicfight.api.animation.AnimationClip;

/**
 * [长期记忆: 013/017] 终极防线 —— 死点入口洗毒。
 *
 * 死点本体:AnimationClip.getPoseInTime 的手写二分搜索。NaN 传入时,
 * (bakedTimes[i] <= NaN) 与 (bakedTimes[i+1] > NaN) 永远为 false,
 * begin/end 区间永不收缩,while(end - begin > 1) 无限循环 —— render
 * thread 单核烧死,这就是 issue #2625 的硬冻结现场。
 *
 * 修复语义:入参 time 为 NaN 时洗成 0(取第 0 帧姿势,良性退化),
 * 兜住一切流入二分搜索的毒值 —— 含根修漏网的路径、未来 addon 的
 * 新毒化源、以及资产 bakedTimes 数据病。
 *
 * 注入策略:ModifyVariable 捕获方法入参(HEAD, argsOnly)。目标方法
 * getPoseInTime(float) 无重载,单参数捕获无歧义。
 */
// remap = false:目标为 epicfight 类,生产不混淆;searge 映射无 mod 成员,必须关
@Mixin(value = AnimationClip.class, remap = false)
public abstract class AnimationClipMixin {

    @ModifyVariable(method = "getPoseInTime", at = @At("HEAD"), argsOnly = true)
    private float epicfightnan$sanitizeTime(float time) {
        return Float.isNaN(time) ? 0.0F : time;
    }
}