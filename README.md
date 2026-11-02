# EpicFight NaN Freeze Fix

修复 Epic Fight(Minecraft 1.20.1 / Forge,20.14.x)的客户端硬冻结 [issue#2625](https://github.com/Antikythera-Studios/epicfight/issues/2625)。

Render thread 会永久卡死在 `AnimationClip.getPoseInTime` 的二分搜索里 —— 根因是零时长循环动画(EMPTY_ANIMATION)在 `AnimationPlayer.tick` 中执行 `elapsedTime %= 0.0F` 产生 NaN,NaN 毒化 `ConcurrentLinkAnimation.startsAt` 后,任何 pose 查询都会把 render thread 带进永不终止的搜索循环。

完整根因分析(带双 thread dump 实证):https://github.com/Antikythera-Studios/epicfight/issues/2625

## 为什么是独立模组,而不是等上游合并?

Epic Fight 本体更新很快,但它的附属模组 Weapons of Miracles 更新极慢且闭源 —— 想继续使用 WoM 的玩家因此被锁在旧版 Epic Fight 上,而上游的修复即使被合并,也大概率只进入新版本分支。

这个模组通过 Mixin 在本地直接修补问题,旧版用户无需依赖上游发版。

## Why a standalone mod?

Epic Fight itself updates quickly, but its addon *Weapons of Miracles* is closed-source and updates far slower — players who want WoM are locked to older Epic Fight builds, and even if this fix gets merged upstream, it would most likely only land in newer version branches. This mod patches the problem locally via Mixin, so legacy-version players don't have to wait.

Root cause: a zero-length looping animation (EMPTY_ANIMATION) makes `AnimationPlayer.tick` compute `elapsedTime %= 0.0F`, producing NaN that poisons `ConcurrentLinkAnimation.startsAt`; any subsequent pose query drives the render thread into a never-terminating binary search inside `AnimationClip.getPoseInTime`. Full analysis with two thread dumps: https://github.com/Antikythera-Studios/epicfight/issues/2625

## 修复内容(三层防御)

1. **根修** —— `AnimationPlayer.tick`:零时长循环动画跳过取模,时间钉死 0,NaN 不再出生
2. **纵深** —— `ConcurrentLinkAnimation.getPoseByTime`:查询口洗 `startsAt`,防止毒化驻留
3. **终极防线** —— `AnimationClip.getPoseInTime`:入口洗 NaN,兜住一切流入的毒值

## 构建

需要 JDK 17。`gradlew build`,产物在 build/libs/。

## License

MPL-2.0