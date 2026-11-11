package cn.blockforge.generated.origoldsar;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@net.neoforged.fml.common.Mod(GeneratedMod.MOD_ID)
public final class GeneratedMod {
    public static final String MOD_ID = "origolds_ar";
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, MOD_ID);
    public static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MOD_ID);
    public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS = DeferredRegister.create(Registries.TRIGGER_TYPE, MOD_ID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, MOD_ID);
    public static final DeferredHolder<CriterionTrigger<?>, OrigoldInfusionTrigger> ORIGOLD_INFUSION_TRIGGER =
            TRIGGERS.register("origold_infusion", OrigoldInfusionTrigger::new);
    private static final ResourceKey<?> ORIGOLDS_CREATIVE_TAB = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB,
            ResourceLocation.fromNamespaceAndPath("origolds", "origolds"));

    public static final DeferredItem<Item> BROKEN_ORIGOLD_PARTS = ITEMS.register("broken_origold_part", () -> new Item(new Item.Properties().rarity(Rarity.COMMON)));
    public static final DeferredItem<Item> SPECTER_MEDALLION = ITEMS.register("specter_medallion", () -> new SpecterMedallion(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> ASSASSIN_MEDALLION = ITEMS.register("assassin_medallion", () -> new AssassinMedallion(new Item.Properties().rarity(Rarity.UNCOMMON)));
    /** 月亮行者勋章：按住月行键（默认空格）时双击潜行键可反转重力方向；按住潜行键时双击月行键可触发月球重力；空中按住潜行可临时把重力提升为常态两倍。 */
    public static final DeferredItem<Item> MOONWALKER_MEDALLION = ITEMS.register("moonwalker_medallion", () -> new MoonwalkerMedallion(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> ETERNAL_POTION = ITEMS.register("eternal_potion", () -> new EternalPotionItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON), net.minecraft.world.item.Items.POTION));
    public static final DeferredItem<Item> ETERNAL_SPLASH_POTION = ITEMS.register("eternal_splash_potion", () -> new EternalPotionItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON), net.minecraft.world.item.Items.SPLASH_POTION));
    public static final DeferredItem<Item> ETERNAL_LINGERING_POTION = ITEMS.register("eternal_lingering_potion", () -> new EternalPotionItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON), net.minecraft.world.item.Items.LINGERING_POTION));
    public static final DeferredItem<Item> FINIS_PEARL = ITEMS.register("finis_pearl", () -> new FinisPearlItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    /** 自循环附魔之瓶：右键消耗 10 经验掷出一只固定爆 10 经验的瓶子，Ctrl+右键掷出全部经验。瓶子不消耗。 */
    public static final DeferredItem<Item> SELF_CIRCULATING_BOTTLE = ITEMS.register("self_circulating_bottle", () -> new SelfCirculatingBottleItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> TOTEM_OF_RESURRECTION = ITEMS.register("totem_of_resurrection", () -> new TotemOfResurrectionItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final DeferredItem<Item> VOID_QUIVER = ITEMS.register("void_quiver", () -> new VoidQuiverItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> SWIFTNESS_RUNE = ITEMS.register("swiftness_rune", () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> GREED_RUNE = ITEMS.register("greed_rune", () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> REAPING_RUNE = ITEMS.register("reaping_rune", () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> SIPHON_RUNE = ITEMS.register("siphon_rune", () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> SHADOW_MEDALLION = ITEMS.register("shadow_medallion", () -> new Item(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<Item> BEAST_RUNE = ITEMS.register("beast_rune", () -> new Item(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<Item> EVERNIGHT_ASCENSION_EMBLEM = ITEMS.register("evernight_ascension_emblem", () -> new Item(new Item.Properties().rarity(Rarity.EPIC)));
    /** 钩爪弩炮：具有弩的全部使用方式，并额外提供钩爪绑定/拉拽能力。 */
    public static final DeferredItem<Item> GRAPNEL_BALLISTA = ITEMS.register("grapnel_ballista", () -> new GrapnelBallista(new Item.Properties().stacksTo(1).durability(465).rarity(Rarity.UNCOMMON)));

    /** 测伤志愿者：放置后生成一只没有 AI 的村民，不可击败、可用发射器穿甲并显示 DPS。 */
    public static final DeferredItem<Item> VOLUNTEER_DUMMY = ITEMS.register("volunteer_dummy", () -> new VolunteerDummyItem(new Item.Properties().rarity(Rarity.UNCOMMON)));

    /** 金源无线红石连接装置：左键选起点、右键选终点，建立可跨维度的无线红石线路。 */
    public static final DeferredItem<Item> WIRELESS_CONNECTOR = ITEMS.register("origold_wireless_connector", () -> new WirelessConnectorItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    /** 金源无线红石遥控开关：左键绑定一个方块，右键远程交互或输出 15 级红石信号。 */
    public static final DeferredItem<Item> ORIGOLD_REMOTE_SWITCH = ITEMS.register("origold_remote_switch", () -> new RemoteSwitchItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final DeferredHolder<EntityType<?>, EntityType<VolunteerDummy>> VOLUNTEER_DUMMY_TYPE =
            ENTITY_TYPES.register("volunteer_dummy", () -> EntityType.Builder.<VolunteerDummy>of(VolunteerDummy::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F)
                    .eyeHeight(1.62F)
                    .clientTrackingRange(10)
                    .build("volunteer_dummy"));
    /** 自循环附魔之瓶的投掷物：命中时按携带的经验量爆出唯一一颗经验球。 */
    public static final DeferredHolder<EntityType<?>, EntityType<SelfCirculatingBottleEntity>> SELF_CIRCULATING_BOTTLE_ENTITY =
            ENTITY_TYPES.register("self_circulating_bottle", () -> EntityType.Builder.<SelfCirculatingBottleEntity>of(SelfCirculatingBottleEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("self_circulating_bottle"));

    /** 永恒药水中封装的原版药水堆栈（物品类型 + 效果内容）。 */
    public static final net.neoforged.neoforge.registries.DeferredHolder<DataComponentType<?>, DataComponentType<ItemStack>> ETERNAL_BASE_POTION =
            DATA_COMPONENTS.registerComponentType("eternal_base_potion",
                    builder -> builder.persistent(ItemStack.CODEC).networkSynchronized(ItemStack.STREAM_CODEC));
    /** 虚空箭匣中封装的箭矢堆栈（物品类型 + 组件，如药水箭的效果内容）。 */
    public static final net.neoforged.neoforge.registries.DeferredHolder<DataComponentType<?>, DataComponentType<ItemStack>> QUIVER_ARROW =
            DATA_COMPONENTS.registerComponentType("quiver_arrow",
                    builder -> builder.persistent(ItemStack.CODEC).networkSynchronized(ItemStack.STREAM_CODEC));
    /** 永恒药水的每瓶独立冷却：该瓶自己的冷却结束时间戳（Unix 毫秒）。 */
    public static final net.neoforged.neoforge.registries.DeferredHolder<DataComponentType<?>, DataComponentType<Long>> ETERNAL_COOLDOWN =
            DATA_COMPONENTS.registerComponentType("eternal_cooldown",
                    builder -> builder.persistent(com.mojang.serialization.Codec.LONG)
                            .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_LONG));
    /** 终末之影珍珠的每颗独立冷却：该颗珍珠自己的冷却结束时间戳（Unix 毫秒）。 */
    public static final net.neoforged.neoforge.registries.DeferredHolder<DataComponentType<?>, DataComponentType<Long>> FINIS_COOLDOWN =
            DATA_COMPONENTS.registerComponentType("finis_cooldown",
                    builder -> builder.persistent(com.mojang.serialization.Codec.LONG)
                            .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_LONG));
    /** 复苏图腾的每根独立冷却：该根图腾自己的冷却结束时间戳（Unix 毫秒）。 */
    public static final net.neoforged.neoforge.registries.DeferredHolder<DataComponentType<?>, DataComponentType<Long>> TOTEM_COOLDOWN =
            DATA_COMPONENTS.registerComponentType("totem_cooldown",
                    builder -> builder.persistent(com.mojang.serialization.Codec.LONG)
                            .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_LONG));
    /** 遥控开关的绑定状态：绑定的维度/方块、对应远程发射器 id 与信号开关。 */
    public static final net.neoforged.neoforge.registries.DeferredHolder<DataComponentType<?>, DataComponentType<RemoteSwitchBinding>> REMOTE_SWITCH_BINDING =
            DATA_COMPONENTS.registerComponentType("remote_switch_binding",
                    builder -> builder.persistent(RemoteSwitchBinding.CODEC)
                            .networkSynchronized(RemoteSwitchBinding.STREAM_CODEC));
    /**
     * 每一个遥控开关的独立身份 id：撤销/重做历史按它单独保存（每一把一支历史栈）。
     * 组件本身随物品持久化，但历史只存在服务端内存里，退出世界即全部清除。
     */
    public static final net.neoforged.neoforge.registries.DeferredHolder<DataComponentType<?>, DataComponentType<java.util.UUID>> REMOTE_SWITCH_UID =
            DATA_COMPONENTS.registerComponentType("remote_switch_uid",
                    builder -> builder.persistent(net.minecraft.core.UUIDUtil.CODEC)
                            .networkSynchronized(net.minecraft.core.UUIDUtil.STREAM_CODEC));
    public static final DeferredBlock<Block> ORIGOLD_ASSEMBLY_STATION = BLOCKS.registerBlock(
            "origold_assembly_station",
            AssemblyStationBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.CRAFTING_TABLE)
                     .strength(1.5F)
                     .noOcclusion());
    public static final DeferredItem<BlockItem> ORIGOLD_ASSEMBLY_STATION_ITEM = ITEMS.register(
            "origold_assembly_station",
            () -> new BlockItem(ORIGOLD_ASSEMBLY_STATION.get(), new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final net.neoforged.neoforge.registries.DeferredHolder<BlockEntityType<?>, BlockEntityType<AssemblyStationBlockEntity>> ORIGOLD_ASSEMBLY_STATION_ENTITY =
            BLOCK_ENTITY_TYPES.register("origold_assembly_station", () -> BlockEntityType.Builder.of(
                    AssemblyStationBlockEntity::new, ORIGOLD_ASSEMBLY_STATION.get()).build(null));
    public static final net.neoforged.neoforge.registries.DeferredHolder<MenuType<?>, MenuType<AssemblyStationMenu>> ASSEMBLY_STATION_MENU =
            MENU_TYPES.register("origold_assembly_station", () -> IMenuTypeExtension.create(AssemblyStationMenu::new));

    public static final net.neoforged.neoforge.registries.DeferredHolder<RecipeSerializer<?>, RecipeSerializer<?>> BROKEN_ORIGOLD_PARTS_RECIPE_SERIALIZER = RECIPE_SERIALIZERS.register(
            "broken_origold_parts",
            BrokenOrigoldPartsRecipe.Serializer::new);
    public static final net.neoforged.neoforge.registries.DeferredHolder<RecipeSerializer<?>, RecipeSerializer<?>> TRAIT_EXTRACTION_RECIPE_SERIALIZER = RECIPE_SERIALIZERS.register(
            "trait_extraction",
            TraitExtractionRecipe.Serializer::new);
    public static final net.neoforged.neoforge.registries.DeferredHolder<RecipeSerializer<?>, RecipeSerializer<?>> TRAIT_REMOVAL_RECIPE_SERIALIZER = RECIPE_SERIALIZERS.register(
            "trait_removal",
            TraitRemovalRecipe.Serializer::new);
    public static final net.neoforged.neoforge.registries.DeferredHolder<RecipeSerializer<?>, RecipeSerializer<?>> ETERNAL_POTION_RECIPE_SERIALIZER = RECIPE_SERIALIZERS.register(
            "eternal_potion",
            EternalPotionRecipe.Serializer::new);
    public static final net.neoforged.neoforge.registries.DeferredHolder<RecipeSerializer<?>, RecipeSerializer<?>> ETERNAL_POTION_SWAP_RECIPE_SERIALIZER = RECIPE_SERIALIZERS.register(
            "eternal_potion_swap",
            EternalPotionSwapRecipe.Serializer::new);
    public static final net.neoforged.neoforge.registries.DeferredHolder<RecipeSerializer<?>, RecipeSerializer<?>> VOID_QUIVER_RECIPE_SERIALIZER = RECIPE_SERIALIZERS.register(
            "void_quiver",
            VoidQuiverRecipe.Serializer::new);
    public static final net.neoforged.neoforge.registries.DeferredHolder<RecipeSerializer<?>, RecipeSerializer<?>> VOID_QUIVER_SWAP_RECIPE_SERIALIZER = RECIPE_SERIALIZERS.register(
            "void_quiver_swap",
            VoidQuiverSwapRecipe.Serializer::new);
    public GeneratedMod(IEventBus modBus, net.neoforged.fml.ModContainer container) {
        // 客户端配置（无线线路样式 CLASSIC / NEO）；CLIENT 类型的配置只会在客户端加载。
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.CLIENT,
                cn.blockforge.generated.origoldsar.client.WireStyleConfig.SPEC);
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            cn.blockforge.generated.origoldsar.client.ArClientSetup.init(container);
        }
        ITEMS.register(modBus);
        BLOCKS.register(modBus);
        RECIPE_SERIALIZERS.register(modBus);
        BLOCK_ENTITY_TYPES.register(modBus);
        MENU_TYPES.register(modBus);
        DATA_COMPONENTS.register(modBus);
        TRIGGERS.register(modBus);
        ENTITY_TYPES.register(modBus);
        modBus.addListener(GeneratedMod::addItemsToOrigoldsTab);
        modBus.addListener(GeneratedMod::addEntityAttributes);
    }

    private static void addEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(VOLUNTEER_DUMMY_TYPE.get(), VolunteerDummy.createAttributes().build());
    }

    private static void addItemsToOrigoldsTab(net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent event) {
        if (ORIGOLDS_CREATIVE_TAB.equals(event.getTabKey())) {
            event.accept(BROKEN_ORIGOLD_PARTS.get());
            event.accept(SPECTER_MEDALLION.get());
            event.accept(ASSASSIN_MEDALLION.get());
            event.accept(MOONWALKER_MEDALLION.get());
            event.accept(ETERNAL_POTION.get());
            event.accept(ETERNAL_SPLASH_POTION.get());
            event.accept(ETERNAL_LINGERING_POTION.get());
            event.accept(FINIS_PEARL.get());
            event.accept(SELF_CIRCULATING_BOTTLE.get());
            event.accept(TOTEM_OF_RESURRECTION.get());
            event.accept(VOID_QUIVER.get());
            event.accept(new ItemStack(net.minecraft.world.item.Items.RECOVERY_COMPASS));
            event.accept(SWIFTNESS_RUNE.get());
            event.accept(GREED_RUNE.get());
            event.accept(REAPING_RUNE.get());
            event.accept(SIPHON_RUNE.get());
            event.accept(SHADOW_MEDALLION.get());
            event.accept(BEAST_RUNE.get());
            event.accept(EVERNIGHT_ASCENSION_EMBLEM.get());
            event.accept(GRAPNEL_BALLISTA.get());
            event.accept(VOLUNTEER_DUMMY.get());
            event.accept(ORIGOLD_ASSEMBLY_STATION_ITEM.get());
            event.accept(WIRELESS_CONNECTOR.get());
            event.accept(ORIGOLD_REMOTE_SWITCH.get());
        }
    }
}
