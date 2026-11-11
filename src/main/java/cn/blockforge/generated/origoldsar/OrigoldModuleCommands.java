package cn.blockforge.generated.origoldsar;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Collection;

@EventBusSubscriber(modid = GeneratedMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class OrigoldModuleCommands {
    private static final String OBJECT_ARGUMENT = "object";
    private static final String ID_ARGUMENT = "id";
    private static final String SIZE_ARGUMENT = "size";
    private static final double MIN_SIZE = 0.01D;
    private static final double MAX_SIZE = 6553500D;
    private static final SuggestionProvider<CommandSourceStack> TRAIT_SUGGESTIONS =
            (context, builder) -> {
                builder.suggest("atk");
                builder.suggest("critdmg");
                builder.suggest("dur");
                builder.suggest("tooleff");
                builder.suggest("pen");
                builder.suggest("arm");
                builder.suggest("armt");
                return builder.buildFuture();
            };

    private OrigoldModuleCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("orimodify")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument(OBJECT_ARGUMENT, EntityArgument.entities())
                                .then(Commands.literal("module")
                                        .then(Commands.literal("clear")
                                                .executes(OrigoldModuleCommands::clear))
                                        .then(Commands.literal("install")
                                                .then(Commands.argument(ID_ARGUMENT, StringArgumentType.word())
                                                         .suggests(TRAIT_SUGGESTIONS)
                                                        .then(Commands.argument(SIZE_ARGUMENT, DoubleArgumentType.doubleArg(MIN_SIZE, MAX_SIZE))
                                                                .executes(context -> modify(
                                                                        context,
                                                                        ModuleOperation.INSTALL,
                                                                        StringArgumentType.getString(context, ID_ARGUMENT),
                                                                        DoubleArgumentType.getDouble(context, SIZE_ARGUMENT))))))
                                        .then(Commands.literal("remove")
                                                .then(Commands.argument(ID_ARGUMENT, StringArgumentType.word())
                                                         .suggests(TRAIT_SUGGESTIONS)
                                                        .executes(context -> modify(
                                                                context,
                                                                ModuleOperation.REMOVE,
                                                                StringArgumentType.getString(context, ID_ARGUMENT),
                                                                0))))
                                        .then(Commands.literal("set")
                                                .then(Commands.argument(ID_ARGUMENT, StringArgumentType.word())
                                                         .suggests(TRAIT_SUGGESTIONS)
                                                        .then(Commands.argument(SIZE_ARGUMENT, DoubleArgumentType.doubleArg(MIN_SIZE, MAX_SIZE))
                                                                .executes(context -> modify(
                                                                        context,
                                                                        ModuleOperation.SET,
                                                                        StringArgumentType.getString(context, ID_ARGUMENT),
                                                                        DoubleArgumentType.getDouble(context, SIZE_ARGUMENT)))))))));
    }

    private static int clear(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Collection<? extends Entity> targets = EntityArgument.getEntities(context, OBJECT_ARGUMENT);
        int changed = 0;
        for (Entity target : targets) {
            LivingEntity living = getLivingTarget(context, target);
            if (living == null) {
                continue;
            }
            ItemStack stack = living.getMainHandItem();
            if (stack.isEmpty()) {
                context.getSource().sendFailure(Component.translatable(
                        "command.origolds_ar.module.empty_hand", target.getDisplayName()));
                continue;
            }
            Component itemName = stack.getHoverName();
            TraitLibrary.clearTraits(stack);
            living.sendSystemMessage(Component.translatable("command.origolds_ar.module.clear", itemName));
            changed++;
        }
        return changed;
    }

    private static int modify(
            CommandContext<CommandSourceStack> context,
            ModuleOperation operation,
            String commandId,
            double size) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        int trait = TraitLibrary.idFromCommandName(commandId);
        if (trait < 0) {
            context.getSource().sendFailure(Component.translatable("command.origolds_ar.module.invalid_id", commandId));
            return 0;
        }
        if (operation != ModuleOperation.REMOVE && !TraitLibrary.isValidCommandSize(trait, size)) {
            double minimum = MIN_SIZE;
            double maximum = MAX_SIZE;
            context.getSource().sendFailure(Component.translatable(
                    "command.origolds_ar.module.invalid_size", TraitLibrary.component(trait), minimum, maximum));
            return 0;
        }

        Collection<? extends Entity> targets = EntityArgument.getEntities(context, OBJECT_ARGUMENT);
        int changed = 0;
        for (Entity target : targets) {
            LivingEntity living = getLivingTarget(context, target);
            if (living == null) {
                continue;
            }
            ItemStack stack = living.getMainHandItem();
            if (stack.isEmpty()) {
                context.getSource().sendFailure(Component.translatable(
                        "command.origolds_ar.module.empty_hand", target.getDisplayName()));
                continue;
            }

            boolean modified = switch (operation) {
                case INSTALL -> TraitLibrary.installTrait(stack, trait, size);
                case REMOVE -> TraitLibrary.removeTrait(stack, trait);
                case SET -> TraitLibrary.setTrait(stack, trait, size);
            };
            if (!modified) {
                String errorKey = operation == ModuleOperation.INSTALL
                        ? "command.origolds_ar.module.already_installed"
                        : "command.origolds_ar.module.not_installed";
                context.getSource().sendFailure(Component.translatable(
                        errorKey, target.getDisplayName(), TraitLibrary.component(trait)));
                continue;
            }

            Component itemName = stack.getHoverName();
            Component traitName = TraitLibrary.component(trait);
            Component message = switch (operation) {
                case INSTALL -> Component.translatable("command.origolds_ar.module.install", itemName, traitName, size);
                case REMOVE -> Component.translatable("command.origolds_ar.module.remove", itemName, traitName);
                case SET -> Component.translatable("command.origolds_ar.module.set", itemName, traitName, size);
            };
            living.sendSystemMessage(message);
            changed++;
        }
        return changed;
    }

    private static LivingEntity getLivingTarget(CommandContext<CommandSourceStack> context, Entity target) {
        if (target instanceof LivingEntity living) {
            return living;
        }
        context.getSource().sendFailure(Component.translatable(
                "command.origolds_ar.module.not_living", target.getDisplayName()));
        return null;
    }

    private enum ModuleOperation {
        INSTALL,
        REMOVE,
        SET
    }
}
