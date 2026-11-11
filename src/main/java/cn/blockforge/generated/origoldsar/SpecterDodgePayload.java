package cn.blockforge.generated.origoldsar;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SpecterDodgePayload() implements CustomPacketPayload {
    public static final Type<SpecterDodgePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(GeneratedMod.MOD_ID, "specter_dodge"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SpecterDodgePayload> STREAM_CODEC =
            StreamCodec.unit(new SpecterDodgePayload());
    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
