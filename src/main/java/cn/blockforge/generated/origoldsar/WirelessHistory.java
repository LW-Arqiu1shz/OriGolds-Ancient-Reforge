package cn.blockforge.generated.origoldsar;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 无线红石操作的撤销 / 重做历史（纯服务端运行期内存，不写盘——玩家退出世界即全部清除）。
 *
 * <p>两套互相独立的栈：
 * <ul>
 *   <li><b>线路历史</b>：按玩家保存。手持任意金源无线红石连接装置时，Ctrl+Z / Ctrl+Y
 *       撤销 / 重做的都是它——新增连线、删除连线、反转输入输出方向等；</li>
 *   <li><b>开关历史</b>：按“每一个遥控开关”独立保存（身份来自物品的
 *       {@code remote_switch_uid} 组件，首次记录操作时惰性分配）。手持某一特定遥控开关时，
 *       撤销 / 重做只作用于这一把开关自身的绑定 / 解绑操作。</li>
 * </ul>
 *
 * <p>每支栈最多保留最近 {@value #LIMIT} 个<b>非撤销、非重做</b>的操作：新操作入栈时若已满，
 * 挤掉最早的一条；新操作会同时清空该方向的重做分支（标准撤销栈语义）。
 */
public final class WirelessHistory {
    /** 单支历史栈的最大长度（16 个可撤销操作）。 */
    public static final int LIMIT = 16;

    /** 一条可撤销 / 可重做的操作。 */
    public sealed interface Op permits LinkOp, SwitchOp {}

    /** 连接装置线路操作的类型。 */
    public enum LinkKind { ADDED, REMOVED, REVERSED }

    /**
     * 线路操作快照。ADDED / REMOVED 携带操作涉及线路的完整记录；
     * REVERSED 携带反转<b>前</b>的记录（反转是自身的逆操作，执行时只需 id）。
     */
    public record LinkOp(LinkKind kind, WirelessRedstoneData.WirelessLink link) implements Op {}

    /**
     * 遥控开关操作快照：操作前后物品上的绑定组件与世界里对应远程发射器的完整记录
     * （null 表示该侧不存在，例如首次绑定前 / 解绑后）。
     */
    public record SwitchOp(RemoteSwitchBinding beforeBinding, WirelessRedstoneData.SwitchEmitter beforeEmitter,
                           RemoteSwitchBinding afterBinding, WirelessRedstoneData.SwitchEmitter afterEmitter)
            implements Op {}

    /** 玩家的线路历史（撤销栈 / 重做栈）。 */
    private static final Map<UUID, Deque<Op>> LINK_UNDO = new ConcurrentHashMap<>();
    private static final Map<UUID, Deque<Op>> LINK_REDO = new ConcurrentHashMap<>();
    /** 每个遥控开关（栈 uid）自己的历史。 */
    private static final Map<UUID, Deque<Op>> SWITCH_UNDO = new ConcurrentHashMap<>();
    private static final Map<UUID, Deque<Op>> SWITCH_REDO = new ConcurrentHashMap<>();
    /** 栈 uid -> 归属玩家：玩家退出世界时把名下所有开关的历史一并清除。 */
    private static final Map<UUID, UUID> SWITCH_OWNER = new ConcurrentHashMap<>();

    private WirelessHistory() {}

    /** 记录一次连接装置的线路操作（同时清空该玩家的线路重做分支）。 */
    public static void recordLink(UUID player, Op op) {
        push(LINK_UNDO, LINK_REDO, player, op);
    }

    /** 记录一次某个遥控开关自身的操作（同时清空该开关的重做分支）。 */
    public static void recordSwitch(UUID switchUid, UUID owner, Op op) {
        SWITCH_OWNER.put(switchUid, owner);
        push(SWITCH_UNDO, SWITCH_REDO, switchUid, op);
    }

    private static void push(Map<UUID, Deque<Op>> undo, Map<UUID, Deque<Op>> redo, UUID key, Op op) {
        Deque<Op> stack = undo.computeIfAbsent(key, k -> new ArrayDeque<>());
        while (stack.size() >= LIMIT) {
            stack.removeFirst(); // 超过 16 条，挤掉最早的记录
        }
        stack.addLast(op);
        redo.remove(key); // 新操作使旧的重做分支失效
    }

    /** 撤销一次线路操作：从撤销栈弹出、压入重做栈；无可撤销时返回 null。 */
    public static Op undoLink(UUID player) {
        return move(LINK_UNDO, LINK_REDO, player);
    }

    /** 重做一次线路操作。 */
    public static Op redoLink(UUID player) {
        return move(LINK_REDO, LINK_UNDO, player);
    }

    /** 撤销某个遥控开关自身的操作。 */
    public static Op undoSwitch(UUID switchUid) {
        return move(SWITCH_UNDO, SWITCH_REDO, switchUid);
    }

    /** 重做某个遥控开关自身的操作。 */
    public static Op redoSwitch(UUID switchUid) {
        return move(SWITCH_REDO, SWITCH_UNDO, switchUid);
    }

    private static Op move(Map<UUID, Deque<Op>> from, Map<UUID, Deque<Op>> to, UUID key) {
        Deque<Op> src = from.get(key);
        if (src == null || src.isEmpty()) {
            return null;
        }
        Op op = src.removeLast();
        Deque<Op> dst = to.computeIfAbsent(key, k -> new ArrayDeque<>());
        while (dst.size() >= LIMIT) {
            dst.removeFirst();
        }
        dst.addLast(op);
        return op;
    }

    /** 玩家退出世界：清掉他的线路历史与他名下所有遥控开关的历史。 */
    public static void clearPlayer(UUID player) {
        LINK_UNDO.remove(player);
        LINK_REDO.remove(player);
        List<UUID> owned = new ArrayList<>();
        SWITCH_OWNER.forEach((uid, owner) -> {
            if (owner.equals(player)) {
                owned.add(uid);
            }
        });
        for (UUID uid : owned) {
            SWITCH_UNDO.remove(uid);
            SWITCH_REDO.remove(uid);
            SWITCH_OWNER.remove(uid);
        }
    }

    /** 关服清理：所有历史都不保留。 */
    public static void clearAll() {
        LINK_UNDO.clear();
        LINK_REDO.clear();
        SWITCH_UNDO.clear();
        SWITCH_REDO.clear();
        SWITCH_OWNER.clear();
    }

    /** 操作的可读名称（动作栏提示“已撤销：xxx”用）。 */
    public static String describe(Op op) {
        if (op instanceof LinkOp link) {
            return switch (link.kind()) {
                case ADDED -> "message.origolds_ar.op_add_link";
                case REMOVED -> "message.origolds_ar.op_remove_link";
                case REVERSED -> "message.origolds_ar.op_reverse_link";
            };
        }
        SwitchOp sw = (SwitchOp) op;
        if (sw.beforeBinding() == null) {
            return "message.origolds_ar.op_bind";
        }
        if (sw.afterBinding() == null) {
            return "message.origolds_ar.op_unbind";
        }
        return "message.origolds_ar.op_rebind";
    }
}
