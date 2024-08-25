/*
 * This file is part of adventure-platform-mod, licensed under the MIT License.
 *
 * Copyright (c) 2024 KyoriPowered
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package net.kyori.adventure.platform.neoforge.impl;

import com.google.auto.service.AutoService;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import net.kyori.adventure.Adventure;
import net.kyori.adventure.permission.PermissionChecker;
import net.kyori.adventure.platform.modcommon.ComponentArgumentType;
import net.kyori.adventure.platform.modcommon.KeyArgumentType;
import net.kyori.adventure.platform.modcommon.impl.AdventureCommon;
import net.kyori.adventure.platform.modcommon.impl.ClickCallbackRegistry;
import net.kyori.adventure.platform.modcommon.impl.ComponentArgumentTypeSerializer;
import net.kyori.adventure.platform.modcommon.impl.LocaleHolderBridge;
import net.kyori.adventure.platform.modcommon.impl.PlatformHooks;
import net.kyori.adventure.platform.modcommon.impl.SidedProxy;
import net.kyori.adventure.platform.modcommon.impl.client.ClientProxy;
import net.kyori.adventure.platform.modcommon.impl.server.DedicatedServerProxy;
import net.kyori.adventure.platform.neoforge.CollectPointersEvent;
import net.kyori.adventure.platform.neoforge.impl.services.ANSIComponentSerializerProviderImpl;
import net.kyori.adventure.platform.neoforge.impl.services.ClickCallbackProviderImpl;
import net.kyori.adventure.platform.neoforge.impl.services.ComponentLoggerProviderImpl;
import net.kyori.adventure.platform.neoforge.impl.services.DataComponentValueConverterProvider;
import net.kyori.adventure.platform.neoforge.impl.services.GsonComponentSerializerProviderImpl;
import net.kyori.adventure.platform.neoforge.impl.services.PlainTextComponentSerializerProviderImpl;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.pointer.Pointers;
import net.kyori.adventure.util.TriState;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.ClientInformationUpdatedEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;
import org.jetbrains.annotations.Nullable;

@Mod("adventure_platform_neoforge")
@SuppressWarnings("checkstyle:HideUtilityClassConstructor") // Not a utility class, this is our main mod class.
public final class AdventureNeoforgeCommon {
  public static SidedProxy SIDE_PROXY;
  private static final DeferredRegister<ArgumentTypeInfo<?, ?>> COMMAND_ARGUMENT_TYPES = DeferredRegister.create(Registries.COMMAND_ARGUMENT_TYPE, Adventure.NAMESPACE);

  static {
    if (FMLLoader.getDist() == Dist.DEDICATED_SERVER) {
      SIDE_PROXY = new DedicatedServerProxy();
    } else {
      SIDE_PROXY = new ClientProxy();
    }

    /*
    hack around service loader issues:
     adventure tries to load services using the api's class loader, which is not on a module layer able to see the game/mods.
     to work around this, we jij delegating service providers and manually populate them with game-accessing providers.
     better solution: add something in adventure to switch the lookup loader?
    */
    ANSIComponentSerializerProviderImpl.DELEGATE = new net.kyori.adventure.platform.modcommon.impl.service.ANSIComponentSerializerProviderImpl();
    ClickCallbackProviderImpl.DELEGATE = new net.kyori.adventure.platform.modcommon.impl.service.ClickCallbackProviderImpl();
    ComponentLoggerProviderImpl.DELEGATE = new net.kyori.adventure.platform.modcommon.impl.service.ComponentLoggerProviderImpl();
    DataComponentValueConverterProvider.DELEGATE = new net.kyori.adventure.platform.modcommon.impl.service.DataComponentValueConverterProvider();
    GsonComponentSerializerProviderImpl.DELEGATE = new net.kyori.adventure.platform.modcommon.impl.service.GsonComponentSerializerProviderImpl();
    PlainTextComponentSerializerProviderImpl.DELEGATE = new net.kyori.adventure.platform.modcommon.impl.service.PlainTextComponentSerializerProviderImpl();
  }

  public static void registerArgumentTypes() {
    COMMAND_ARGUMENT_TYPES.register("component", () -> ArgumentTypeInfos.registerByClass(
      ComponentArgumentType.class,
      ComponentArgumentTypeSerializer.INSTANCE
    ));
    COMMAND_ARGUMENT_TYPES.register("key", () -> ArgumentTypeInfos.registerByClass(
      KeyArgumentType.class,
      SingletonArgumentInfo.contextFree(KeyArgumentType::key)
    ));
  }

  public AdventureNeoforgeCommon(final IEventBus bus) {
    COMMAND_ARGUMENT_TYPES.register(bus);

    NeoForge.EVENT_BUS.addListener((ClientInformationUpdatedEvent e) -> {
      if (!Objects.equals(e.getOldInformation().language(), e.getUpdatedInformation().language())) {
        AdventureCommon.HOOKS.onLocaleChange(e.getEntity(), LocaleHolderBridge.toLocale(e.getUpdatedInformation().language()));
      }
    });
    NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> {
      ClickCallbackRegistry.INSTANCE.registerToDispatcher(e.getDispatcher());
    });

    AdventureCommon.scheduleClickCallbackCleanup();
  }

  @AutoService(PlatformHooks.class)
  public static final class ForgeHooks implements PlatformHooks {
    private static final Cache<String, PermissionNode<Boolean>> PERMISSION_NODE_CACHE = CacheBuilder.newBuilder().maximumSize(100).build();
    private static final PermissionNode<Boolean> NULL = new PermissionNode<>(Adventure.NAMESPACE, "null", PermissionTypes.BOOLEAN, (player, playerUUID, context) -> true);

    @Override
    public SidedProxy sidedProxy() {
      return AdventureNeoforgeCommon.SIDE_PROXY;
    }

    @Override
    public void updateTabList(
      final ServerPlayer player,
      final net.minecraft.network.chat.@Nullable Component header,
      final net.minecraft.network.chat.@Nullable Component footer
    ) {
      player.setTabListHeaderFooter(
        header == null ? player.getTabListHeader() : header,
        footer == null ? player.getTabListFooter() : footer
      );
    }

    @Override
    public void collectPointers(final Pointered pointered, final Pointers.Builder builder) {
      PlatformHooks.super.collectPointers(pointered, builder);
      if (pointered instanceof ServerPlayer player) {
        builder.withStatic(PermissionChecker.POINTER, perm -> this.hasPermission(player, perm));
      }
      NeoForge.EVENT_BUS.post(new CollectPointersEvent(pointered, builder));
    }

    @SuppressWarnings("unchecked")
    private TriState hasPermission(final ServerPlayer player, final String permission) {
      final PermissionNode<Boolean> node;
      try {
        node = PERMISSION_NODE_CACHE.get(permission, () -> (PermissionNode<Boolean>) PermissionAPI.getRegisteredNodes().stream()
          .filter(n -> n.getNodeName().equals(permission) && n.getType() == PermissionTypes.BOOLEAN)
          .findFirst()
          .orElse(NULL));
      } catch (final ExecutionException e) {
        throw new RuntimeException(e);
      }
      if (node == NULL) {
        return TriState.NOT_SET;
      }
      return TriState.byBoolean(PermissionAPI.getPermission(player, node));
    }
  }
}
