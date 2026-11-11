package cn.blockforge.generated.origoldsar;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Sent from the client to the server when the player double-taps TAB while
 * holding the dash key: a powerful upward leap with an independent cooldown
 * from the horizontal dashes.
 */
public record AssassinLeapPayload() implements CustomPacketPayload {
    public static final Type<AssassinLeapPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(GeneratedMod.MOD_ID, "assassin_leap"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AssassinLeapPayload> STREAM_CODEC =
            StreamCodec.unit(new AssassinLeapPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
