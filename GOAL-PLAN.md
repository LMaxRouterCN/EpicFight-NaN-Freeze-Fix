# GOAL-PLAN — EpicFight NaN Freeze Fix

## 任务
以独立 Mixin 修复模组形式,修复 Epic Fight(MC 1.20.1 / Forge / 20.14.17)客户端硬冻结 bug。
上游 issue:https://github.com/Antikythera-Studios/epicfight/issues/2625

## 背景与根因链(定案,详见案卷)
- 症状:LAN 存档 host 客户端硬冻结,render thread 单核 97.9% 烧死,窗口拖动响应但标题栏按钮无反应
- 死点:AnimationClip.getPoseInTime:100 的手写二分搜索,NaN 使三分支全 false,区间永不收缩
- NaN 出生:AnimationPlayer.tick 中 EMPTY_ANIMATION(totalTime=0 且 isRepeat=true)的 elapsedTime %= 0.0F
- 扩散:Layer.playLivingAnimation 把毒化的 elapsedTime 写入 ConcurrentLinkAnimation.startsAt,后续任意 pose 查询携带 NaN
- 全案卷:D:\Documents\my\日常\epicfight-bug-case\(GOAL-PLAN 侦察章节 + evidence\upstream-src-20261102\ 五源码副本)
- 长期记忆:013(根因定案) / 016(上游打标机制) / 017(修复路线与拍板)

## 已拍板决策(max,2026-11-02)
- 路径:C 双轨 —— 本 mixin 模组为主,上游 PR 后续 fire-and-forget(上游 PR 队列 4 个躺 3-7 个月零合并,期望值已知)
- 目标分支:1.20.1(整合包 IR2040 锁 epicfight 20.14.17;1.20.1 HEAD 20.15.1 涉案三文件零改动,同修)
- 修复层级:三层全上
- 命名:EpicFight NaN Freeze Fix / modId: epicfightnanafreezefix / 包: com.lmaxrouter.epicfightnanafreezefix
- README 动机(max 口述):epicfight 本体更新快,但附属 Weapons of Miracles 更新极慢且闭源,玩家被锁旧版;上游修复即使合并也大概率不回移旧分支 → 独立 mixin 模组是旧版玩家出路

## 修复设计(三层防御)
1. AnimationPlayerMixin —— 根修,tick 方法 HEAD cancellable 注入:
   currentPlay.isRepeat() && currentPlay.getTotalTime() <= 0.0F 时,elapsedTime/prevElapsedTime 钉 0 并 cancel 整个 tick。
   不用 ordinal 钉 getTotalTime() 调用序号(上游一改就错位),注入点钉方法头,鲁棒。
   语义:零时长循环动画永远停在第 0 秒(EMPTY 的 pose 与时间无关,等价)。
   附带:守卫可自愈已毒化的 elapsedTime(EMPTY 即零长循环,下一 tick 洗回 0)。
2. ConcurrentLinkAnimationMixin —— 纵深,getPoseByTime HEAD(非 cancellable):
   startsAt 字段为 NaN 时洗回 0。防毒化长期驻留(end() 会把 startsAt 写回 player.elapsedTime)。
   退化语义:毒化时退化为 next 动画第 0 帧混合,非空 Pose,避免 T-Pose 闪烁。
3. AnimationClipMixin —— 终极防线,@ModifyVariable(getPoseInTime, HEAD, argsOnly):
   入参 time 为 NaN 时洗成 0。兜住一切流入二分搜索的毒值(含上游层漏网、资产 bakedTimes 数据病)。

关键观察(issue 已注明):current 侧 pose 存活 —— EMPTY_CLIP 无 bakedTimes,NaN 走不进二分搜索;
真正的死点只在 next 侧(有 bakedTimes 的 clip)。三层防御的价值在防未来毒化路径,非仅当前案。

## Mixin 分端
- mixins(双端):AnimationPlayerMixin, AnimationClipMixin(服务端 Animator 亦走 tick/pose)
- client:ConcurrentLinkAnimationMixin(目标类 @OnlyIn(Dist.CLIENT),进通用列表会在服务端加载失败)

## 构建方案
- 骨架继承 vs2-sentinel-guard-fix(同类项目,本机构建验证过):
  MDG legacyforge 2.0.116(本机缓存组合,绕 maven.neoforged.net 不可达)
  / UTF-8 编译(防中文注释 GBK 坑)/ reobfJar 输出覆写 / verifyMixinConfig 构建期守卫
- 依赖:libs/ 下 epicfight 20.14.17 jar,compileOnly fileTree(自 IR2040 mods 目录复制)
- mixin 目标全为 epicfight 自有类(非 MC 类),handler 体内零 MC 方法调用 → 无 refmap 需求
- 产物契约:epicfight-nan-freeze-fix-1.0.0+1.20.1-forge.jar(modVersion/mcVersion 单一事实源)

## 验证计划
1. gradlew build 通过 + verifyMixinConfig 全绿
2. jar 投入独立测试实例(max 惯例:不用模组自带 run/,编译产物放单独游戏实例)
3. 实测:IR2040 复现路径(LAN 存档游玩至原冻结触发点)+ 无副作用观察
4. 可选后续:插桩 build 定位具体 entity/animation 复现对(修复不依赖它,仅issue 补充)

## 状态
- [x] 根因定案 + issue 发布(2026-10-26)
- [x] 修复侦察:两分支验证 / 上游 PR 尸检 / GPL 确认(2026-11-02)
- [x] 三决策拍板 + 项目目录就位(2026-11-02)
- [x] GOAL-PLAN / README 落盘
- [ ] 项目骨架(构建文件 + wrapper + libs)
- [ ] mixin 三类实现 + mixins.json + mods.toml + pack.mcmeta
- [ ] 构建通过
- [ ] 实测验证

## 用户规范对齐
- 版本号:独立模组正常三位式 1.0.0;jar 命名套用通用契约(模组名-版本+mc版本+加载器)
- 注释:所有代码创建/修改必须带注释,禁删既有注释
- 公开信息(git commit/README):中文优先,中英双语
- License:MPL-2.0(max 偏好;上游 GPL-3.0 注脚:本地自用零风险,公开发布场景的合规评估在发布时做)
## 构建状态 (2026-11-02)

- BUILD SUCCESSFUL (gradle 8.14.2, 腾讯镜像本地缓存, 冷构建 9m35s)
- 产物: build/libs/epicfight-nan-freeze-fix-1.0.0+1.20.1-forge.jar (6KB), verifyMixinConfig 3 mixin OK
- git: 1f34a35(初始) -> cd1a3a7(remap修复+镜像wrapper) -> 本commit, 未推远程
- 坑1: mixin AP "Unable to locate obfuscation mapping" -> @Mixin(remap=false) x3 [长期记忆: 019]
- 坑2: services.gradle.org 不可达 -> wrapper 指腾讯镜像, URL哈希命中本地完整缓存, 零下载 [长期记忆: 020]
- 待游戏内验证: NaN冻结复现场景 + 日志无 mixin apply 报错
