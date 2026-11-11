package cn.blockforge.generated.origoldsar;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Sent from the client to the server when the player triggers an assassin
 * dash. {@code direction} is the pressed movement key:
 * 0 = forward (W), 1 = left (A), 2 = back (S), 3 = right (D).
 */
public record AssassinDashPayload(byte direction) implements CustomPacketPayload {
    public static final Type<AssassinDashPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(GeneratedMod.MOD_ID, "assassin_dash"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AssassinDashPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public AssassinDashPayload decode(RegistryFriendlyByteBuf buf) {
                    return new AssassinDashPayload(buf.readByte());
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, AssassinDashPayload payload) {
                    buf.writeByte(payload.direction());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
