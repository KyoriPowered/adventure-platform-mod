package net.kyori.adventure.platform.forge.impl;

import com.google.auto.service.AutoService;
import com.google.gson.Gson;
import com.mojang.brigadier.Command;
import com.mojang.logging.LogUtils;
import java.util.function.Function;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.forge.AdventureCapabilities;
import net.kyori.adventure.platform.forge.impl.client.ClientProxy;
import net.kyori.adventure.platform.forge.impl.server.DedicatedServerProxy;
import net.kyori.adventure.platform.forge.impl.server.ForgeServerAudiencesImpl;
import net.kyori.adventure.platform.modcommon.impl.PlatformHooks;
import net.kyori.adventure.platform.modcommon.impl.SidedProxy;
import net.kyori.adventure.platform.modcommon.impl.WrappedComponent;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.pointer.Pointers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.flattener.ComponentFlattener.Builder;
import net.kyori.adventure.text.renderer.ComponentRenderer;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.loading.FMLLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

@Mod("adventure_platform_forge")
public class AdventureMod {

  private static final Logger LOGGER = LogUtils.getLogger();

  private static SidedProxy SIDED_PROXY = determineSidedProxy();

  private static SidedProxy determineSidedProxy() {
    switch (FMLLoader.getDist()) {
      case CLIENT: return new ClientProxy();
      case DEDICATED_SERVER: return new DedicatedServerProxy();
      default: throw new IllegalArgumentException("Unexpected value: " + FMLLoader.getDist());
    }
  }

  public AdventureMod() {
    MinecraftForge.EVENT_BUS.addListener(this::registerCommand);
  }

    public void registerCommand(final RegisterCommandsEvent event) {
      LOGGER.info("Registering commands!");
      event.getDispatcher().register(literal("tablist")
        .then(literal("header").then(argument("value", ComponentArgument.textComponent()).executes(ctx -> {
          ctx.getSource().getPlayerOrException().setTabListHeader(ComponentArgument.getComponent(ctx, "value"));
          return Command.SINGLE_SUCCESS;
        })))
        .then(literal("footer").then(argument("value", ComponentArgument.textComponent()).executes(ctx -> {
          ctx.getSource().getPlayerOrException().setTabListFooter(ComponentArgument.getComponent(ctx, "value"));
          return Command.SINGLE_SUCCESS;
        })))
      );
    }

  @Mod.EventBusSubscriber(modid = "adventure_platform_forge", bus = Bus.FORGE)
  public static class EventListeners {
    @SubscribeEvent
    public void onPlayerClone(final PlayerEvent.Clone event) {
      if (event.getPlayer() instanceof final ServerPlayer replaced
        && event.getOriginal() instanceof final ServerPlayer original) {
        ForgeServerAudiencesImpl.forEachInstance(instance -> {
          instance.bossBars().replacePlayer(original, replaced);
        });
      }
    }

    // @SubscribeEvent
    public void registerCommand(final RegisterCommandsEvent event) {
      LOGGER.info("Registering commands!");
      event.getDispatcher().register(literal("tablist")
        .then(literal("header").then(argument("value", ComponentArgument.textComponent()).executes(ctx -> {
          ctx.getSource().getPlayerOrException().setTabListHeader(ComponentArgument.getComponent(ctx, "value"));
          return Command.SINGLE_SUCCESS;
        })))
        .then(literal("footer").then(argument("value", ComponentArgument.textComponent()).executes(ctx -> {
          ctx.getSource().getPlayerOrException().setTabListFooter(ComponentArgument.getComponent(ctx, "value"));
          return Command.SINGLE_SUCCESS;
        })))
      );
    }

    @SubscribeEvent
    public void registerCapabilities(final RegisterCapabilitiesEvent event) {
      event.register(Audience.class);
      event.register(Pointered.class);
    }

    @SubscribeEvent
    public void registerServerCapabilities(final AttachCapabilitiesEvent<Entity> event) {
      // event.addCapability(null, null);
    }
  }

  @AutoService(PlatformHooks.class)
  public static final class ForgeHooks implements PlatformHooks {
    @Override
    public void contributeFlattenerElements(final @NotNull Builder flattenerBuilder) {
      SIDED_PROXY.contributeFlattenerElements(flattenerBuilder);
    }

    @Override
    public @NotNull WrappedComponent createWrappedComponent(final @NotNull Component wrapped, final @Nullable Function<Pointered, ?> partition, final @Nullable ComponentRenderer<Pointered> renderer) {
      return SIDED_PROXY.createWrappedComponent(wrapped, partition, renderer);
    }

    @Override
    public Gson componentSerializerGson() {
      return net.minecraft.network.chat.Component.Serializer.GSON;
    }

    @Override
    public void updateTabList(final ServerPlayer player, final net.minecraft.network.chat.@Nullable Component header, final net.minecraft.network.chat.@Nullable Component footer) {
      if (header == null && footer == null) {
        return;
      } else if (header == null) {
        player.setTabListFooter(footer);
      } else if (footer == null) {
        player.setTabListHeader(header);
      } else {
        player.setTabListHeaderFooter(header, footer);
      }
    }

    @Override
    public Pointers pointers(final Player player) {
      return Pointers.empty();
    }

    @Override
    public Pointered pointered(final Player player) {
      return player.getCapability(AdventureCapabilities.POINTERED).resolve().get();
    }

    @Override
    public void replaceBossBarSubscriber(final ServerBossEvent bar, final ServerPlayer old, final ServerPlayer newPlayer) {
      if (bar.players.remove(old)) {
        bar.players.add(newPlayer);
      }
    }
  }

}
