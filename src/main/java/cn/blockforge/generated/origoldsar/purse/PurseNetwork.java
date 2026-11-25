package cn.blockforge.generated.origoldsar.purse;

import cn.blockforge.generated.origoldsar.GeneratedMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** 异界行囊的网络包：客户端在物品栏界面上的操作，和服务端同步装备状态。 */
/** 26.x：由 GeneratedMod 构造器挂到 MOD 总线上。 */
public final class PurseNetwork {
    public static final int ACTION_CLOSE = 0;
    public static final int ACTION_OPEN = 1;
    /** 把装备的行囊拿到鼠标上。 */
    public static final int ACTION_TAKE = 2;
    /** 把鼠标上的行囊装备上（原来装备的换到鼠标上）。 */
    public static final int ACTION_EQUIP_CARRIED = 3;
    /** 创造模式物品栏：取下行囊直接放回物品栏（满了扔在脚下）。 */
    public static final int ACTION_TAKE_TO_INVENTORY = 4;

    private PurseNetwork() {
    }

    /** 客户端 -> 服务端：物品栏界面上的行囊操作。 */
    public record ActionPayload(int action) implements CustomPacketPayload {
        public static final Type<ActionPayload> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath(GeneratedMod.MOD_ID, "purse_action"));
        public static final StreamCodec<ByteBuf, ActionPayload> STREAM_CODEC =
                ByteBufCodecs.VAR_INT.map(ActionPayload::new, ActionPayload::action);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** 服务端 -> 客户端：当前装备的行囊（空 = 没装备）和那一行是否展开。 */
    public record SyncPayload(ItemStack purse, boolean open) implements CustomPacketPayload {
        public static final Type<SyncPayload> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath(GeneratedMod.MOD_ID, "purse_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SyncPayload> STREAM_CODEC = StreamCodec.composite(
                ItemStack.OPTIONAL_STREAM_CODEC, SyncPayload::purse,
                ByteBufCodecs.BOOL, SyncPayload::open,
                SyncPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(ActionPayload.TYPE, ActionPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        handleAction(player, payload.action());
                    }
                }));
        registrar.playToClient(SyncPayload.TYPE, SyncPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        cn.blockforge.generated.origoldsar.client.PurseInventoryOverlay.onSync(payload)));
    }

    public static void sync(ServerPlayer player) {
        PurseData data = player.getData(GeneratedMod.PURSE_DATA);
        PacketDistributor.sendToPlayer(player, new SyncPayload(data.purse().copy(), data.panelOpen()));
    }

    private static void handleAction(ServerPlayer player, int action) {
        PurseData data = player.getData(GeneratedMod.PURSE_DATA);
        AbstractContainerMenu menu = player.containerMenu;
        switch (action) {
            case ACTION_CLOSE, ACTION_OPEN -> data.setPanelOpen(action == ACTION_OPEN);
            case ACTION_TAKE -> {
                if (menu != player.inventoryMenu || !data.equipped() || !menu.getCarried().isEmpty()) {
                    return;
                }
                menu.setCarried(data.unequip());
                sync(player);
                menu.broadcastChanges();
            }
            case ACTION_TAKE_TO_INVENTORY -> {
                if (!data.equipped()) {
                    return;
                }
                ItemStack purse = data.unequip();
                if (!player.getInventory().add(purse)) {
                    player.drop(purse, false);
                }
                sync(player);
                player.inventoryMenu.broadcastChanges();
            }
            case ACTION_EQUIP_CARRIED -> {
                ItemStack carried = menu.getCarried();
                if (menu != player.inventoryMenu || !OtherworldPurseItem.isPurse(carried)) {
                    return;
                }
                ItemStack previous = data.unequip();
                data.equip(carried);
                menu.setCarried(previous);
                sync(player);
                menu.broadcastChanges();
            }
            default -> {
            }
        }
    }
}
