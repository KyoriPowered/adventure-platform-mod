package net.kyori.adventure.platform.fabric.impl.mixin.minecraft.server.level;

import java.util.Set;
import net.kyori.adventure.platform.fabric.impl.server.ServerPlayerBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin implements ServerPlayerBridge {
  @Shadow public ServerGamePacketListenerImpl connection;
  private Component adventure$tabListHeader = Component.empty();
  private Component adventure$tabListFooter = Component.empty();
  private Set<ResourceLocation> adventure$arguments = Set.of();

  // Tab list

  @Override
  public void bridge$updateTabList(final @Nullable Component header, final @Nullable Component footer) {
    if (header != null) {
      this.adventure$tabListHeader = header;
    }
    if (footer != null) {
      this.adventure$tabListFooter = footer;
    }
    final ClientboundTabListPacket packet = new ClientboundTabListPacket(
      this.adventure$tabListHeader,
      this.adventure$tabListFooter
    );

    this.connection.send(packet);
  }

  // Known argument type tracking

  @Override
  public Set<ResourceLocation> bridge$knownArguments() {
    return this.adventure$arguments;
  }

  @Override
  public void bridge$knownArguments(final Set<ResourceLocation> arguments) {
    this.adventure$arguments = Set.copyOf(arguments);
  }

  @Inject(method = "restoreFrom", at = @At("RETURN"))
  public void adventure$copyData(final ServerPlayer from, final boolean keepEverything, final CallbackInfo ci) {
    this.bridge$knownArguments(((ServerPlayerBridge) from).bridge$knownArguments());
  }
}
