package cn.blockforge.generated.origoldsar.client;

import java.lang.reflect.Method;

/**
 * 只读探测“与本模组共用一个金源无线连接装置”的附属模组当前处于什么模式。
 *
 * <p>目前已知的附属模组是《OriGolds: Industrialized Age》（现代工业时代）。它给金源无线连接
 * 装置加入了两个新模式——“金源能量电线”与“无线物流管道”，并在开启其中任意一个时把本模组的
 * {@link WirelessConnectorClient#connectorDisplay} 反射改写为 {@code false}，以此关闭本模组的
 * 无线红石显示模式，实现三模式互斥。
 *
 * <p>本类反过来只读探测这两个模式此刻是否开着，供本模组在玩家从其它模式切回无线红石模式时
 * 显示“已切换至无线红石显示模式”，而不是普通的“无线红石显示模式：开”。只读不写：关闭附属的
 * 那两个模式由附属自己的客户端 tick 在读到本模组模式开启后完成（见其 {@code onClientTick}）。
 *
 * <p>附属模组没有安装、尚未初始化，或者它日后的接口发生变更时，{@link #isOtherModeActive()}
 * 一律返回 {@code false}，既不会抛异常也不会刷屏日志：本模组单独安装时的行为与从前完全一致。
 */
public final class AddonModeBridge {
    /** 附属模组“金源无线连接装置”客户端表现类的全限定名。 */
    private static final String ADDON_CLIENT_CLASS = "cn.origolds.ia.OGEWirelessConnectorClient";
    /** 附属“金源能量电线显示模式”是否开启（public static boolean）。 */
    private static final String DISPLAY_MODE_METHOD = "isDisplayModeOn";
    /** 附属“无线物流管道显示模式”是否开启（public static boolean）。 */
    private static final String LOGISTICS_MODE_METHOD = "isLogisticsModeOn";

    /** 是否已经尝试解析过附属类（无论成功失败都只尝试一次）。 */
    private static boolean resolved = false;
    /** 解析到的公共静态查询方法，附属不存在时为 null。 */
    private static Method displayModeQuery;
    private static Method logisticsModeQuery;

    private AddonModeBridge() {}

    /**
     * 附属模组给连接装置加入的显示模式中，是否至少有一个当前处于开启状态。
     *
     * <p>返回 {@code false} 表示“没有其它模式占着连接装置”（附属未安装，或附属的两个模式都关着），
     * 此时切到本模组就是普通的开/关，而不是模式之间的切换。
     */
    public static boolean isOtherModeActive() {
        resolve();
        return query(displayModeQuery) || query(logisticsModeQuery);
    }

    /** 解析附属类与方法；只尝试一次，失败后永远按“附属不存在”处理。 */
    private static void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        try {
            Class<?> addonClient = loadAddonClass();
            displayModeQuery = addonClient.getMethod(DISPLAY_MODE_METHOD);
            logisticsModeQuery = addonClient.getMethod(LOGISTICS_MODE_METHOD);
        } catch (Throwable ignored) {
            // 附属未安装 / 类初始化失败 / 方法签名变更：按“无其它模式”处理，本模组照常工作。
            displayModeQuery = null;
            logisticsModeQuery = null;
        }
    }

    /**
     * 找到附属模组客户端类。先按本模组类加载器直接查找，再退回线程上下文类加载器——
     * 不同加载器下 mod 之间互不可见的情况都覆盖到。
     */
    private static Class<?> loadAddonClass() throws ClassNotFoundException {
        try {
            return Class.forName(ADDON_CLIENT_CLASS, false, AddonModeBridge.class.getClassLoader());
        } catch (ClassNotFoundException first) {
            ClassLoader context = Thread.currentThread().getContextClassLoader();
            if (context == null) {
                throw first;
            }
            return Class.forName(ADDON_CLIENT_CLASS, false, context);
        }
    }

    /** 调用一个静态无参 boolean 查询方法；方法为 null 或调用出错时按 false 处理。 */
    private static boolean query(Method method) {
        if (method == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(method.invoke(null));
        } catch (Throwable ignored) {
            return false;
        }
    }
}
