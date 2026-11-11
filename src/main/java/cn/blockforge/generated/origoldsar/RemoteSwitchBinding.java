package cn.blockforge.generated.origoldsar;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * 金源无线红石遥控开关的绑定状态（保存在物品的
 * {@code origolds_ar:remote_switch_binding} 组件里）：
 * 绑定的维度与方块坐标、对应远程信号发射器的 id、以及信号当前是否开启。
 */
public record RemoteSwitchBinding(ResourceKey<Level> dimension, BlockPos position, UUID emitter, boolean active) {
    public static final Codec<RemoteSwitchBinding> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Identifier.CODEC.xmap(
                    rl -> ResourceKey.create(Registries.DIMENSION, rl),
                    ResourceKey::identifier).fieldOf("dimension").forGetter(RemoteSwitchBinding::dimension),
            Codec.LONG.xmap(WirelessRedstoneData::fromLong, BlockPos::asLong).fieldOf("position").forGetter(RemoteSwitchBinding::position),
            Codec.STRING.xmap(UUID::fromString, UUID::toString).fieldOf("emitter").forGetter(RemoteSwitchBinding::emitter),
            Codec.BOOL.fieldOf("active").forGetter(RemoteSwitchBinding::active)
    ).apply(inst, RemoteSwitchBinding::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, RemoteSwitchBinding> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public RemoteSwitchBinding decode(RegistryFriendlyByteBuf buf) {
            return new RemoteSwitchBinding(
                    WirelessRedstoneData.dimKey(buf.readIdentifier()),
                    WirelessRedstoneData.fromLong(buf.readLong()),
                    buf.readUUID(),
                    buf.readBoolean());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, RemoteSwitchBinding value) {
            buf.writeIdentifier(value.dimension().identifier());
            buf.writeLong(value.position().asLong());
            buf.writeUUID(value.emitter());
            buf.writeBoolean(value.active());
        }
    };
}
