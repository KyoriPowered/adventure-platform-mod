package net.kyori.adventure.platform.neoforge.impl;

import com.google.auto.service.AutoService;
import net.kyori.adventure.platform.modcommon.impl.AdventureCommon;
import net.kyori.adventure.platform.modcommon.impl.ClickCallbackRegistry;
import net.kyori.adventure.platform.modcommon.impl.PlatformHooks;
import net.kyori.adventure.platform.modcommon.impl.SidedProxy;
import net.kyori.adventure.platform.neoforge.services.ANSIComponentSerializerProviderImpl;
import net.kyori.adventure.platform.neoforge.services.ClickCallbackProviderImpl;
import net.kyori.adventure.platform.neoforge.services.ComponentLoggerProviderImpl;
import net.kyori.adventure.platform.neoforge.services.DataComponentValueConverterProvider;
import net.kyori.adventure.platform.neoforge.services.GsonComponentSerializerProviderImpl;
import net.kyori.adventure.platform.neoforge.services.PlainTextComponentSerializerProviderImpl;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.jetbrains.annotations.Nullable;

@Mod("adventure_platform_neoforge")
public class AdventureNeoforgeCommon {
  public static SidedProxy SIDE_PROXY;

  static {
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
     // todo: look into initializing SidedProxy earlier to avoid lazy supplier for flattener
  }

  public AdventureNeoforgeCommon() {
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
