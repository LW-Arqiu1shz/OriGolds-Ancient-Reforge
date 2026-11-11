package cn.blockforge.generated.origoldsar;

import net.minecraft.world.item.Item;

/**
 * 月亮行者勋章：持有者按住“月行”键（默认空格）时双击潜行键，可反转自身的重力方向；
 * 按住潜行键时双击“月行”键，可触发月球重力（重力降为 25%，终端下落速度降为 40%）；
 * 在空中按住潜行键，可临时把重力提升为常态两倍（松开或落地即恢复原重力）；
 * 持有者免疫摔落伤害。
 * 与其他勋章一样不可堆叠。
 */
public class MoonwalkerMedallion extends Item {
    public MoonwalkerMedallion(Properties properties) {
        super(properties.stacksTo(1));
    }
}
