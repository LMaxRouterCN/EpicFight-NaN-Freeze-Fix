package com.lmaxrouter.epicfightnanafreezefix;

import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * EpicFight NaN 冻结修复 — Forge mod 入口壳。
 *
 * 全部功能在 mixin 包的三个 mixin 里,由 Mixin 系统在 mod 构造前应用;
 * 这个类只负责两件事:
 *   1. 给 Forge 一个 @Mod 入口(FML 需要,否则 mod jar 不被当作 mod 加载)
 *   2. 留一行启动日志,方便在 latest.log 里确认本 mod 确实加载了
 */
@Mod("epicfightnanafreezefix")
public class EpicFightNaNFreezeFix {

    private static final Logger LOGGER = LoggerFactory.getLogger("epicfightnanafreezefix");

    public EpicFightNaNFreezeFix() {
        // mixin 生效与否看游戏是否还会冻结;守卫本身零日志(每 tick 触发,不打)
        LOGGER.info("EpicFight NaN Freeze Fix loaded - zero-length loop guard active");
    }
}