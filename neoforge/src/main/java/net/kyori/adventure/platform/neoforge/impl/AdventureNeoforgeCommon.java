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
import java.util.Objects;
import net.kyori.adventure.platform.modcommon.impl.AdventureCommon;
import net.kyori.adventure.platform.modcommon.impl.ClickCallbackRegistry;
import net.kyori.adventure.platform.modcommon.impl.LocaleHolderBridge;
import net.kyori.adventure.platform.modcommon.impl.PlatformHooks;
import net.kyori.adventure.platform.modcommon.impl.SidedProxy;
import net.kyori.adventure.platform.modcommon.impl.client.ClientProxy;
import net.kyori.adventure.platform.modcommon.impl.server.DedicatedServerProxy;
import net.kyori.adventure.platform.neoforge.impl.services.ANSIComponentSerializerProviderImpl;
import net.kyori.adventure.platform.neoforge.impl.services.ClickCallbackProviderImpl;
import net.kyori.adventure.platform.neoforge.impl.services.ComponentLoggerProviderImpl;
import net.kyori.adventure.platform.neoforge.impl.services.DataComponentValueConverterProvider;
import net.kyori.adventure.platform.neoforge.impl.services.GsonComponentSerializerProviderImpl;
import net.kyori.adventure.platform.neoforge.impl.services.PlainTextComponentSerializerProviderImpl;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.ClientInformationUpdatedEvent;
import org.jetbrains.annotations.Nullable;

@Mod("adventure_platform_neoforge")
@SuppressWarnings("checkstyle:HideUtilityClassConstructor") // Not a utility class, this is our main mod class.
public class AdventureNeoforgeCommon {
  public static SidedProxy SIDE_PROXY;

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

  public AdventureNeoforgeCommon() {
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
  }
}
