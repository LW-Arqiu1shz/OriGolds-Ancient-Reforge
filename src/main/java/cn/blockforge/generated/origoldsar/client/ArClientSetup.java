package cn.blockforge.generated.origoldsar.client;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** 只在物理客户端调用的初始化（由 {@code GeneratedMod} 构造器在客户端分支里调用）。 */
public final class ArClientSetup {
    private ArClientSetup() {
    }

    public static void init(ModContainer container) {
        // 模组列表里的“配置”按钮：NeoForge 自带的配置界面，改完立即生效。
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        // NEO 样式的两路无线视觉通道（CLASSIC 样式下它们不产生几何）。
        WirelessConnectorClient.registerVisualChannels();
    }
}
