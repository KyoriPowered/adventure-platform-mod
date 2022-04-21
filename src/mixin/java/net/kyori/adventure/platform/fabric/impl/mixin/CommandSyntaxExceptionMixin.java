package net.kyori.adventure.platform.fabric.impl.mixin;

import com.mojang.brigadier.Message;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kyori.adventure.platform.fabric.FabricAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.util.ComponentMessageThrowable;
import net.minecraft.network.chat.ComponentUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = CommandSyntaxException.class, remap = false)
abstract class CommandSyntaxExceptionMixin implements ComponentMessageThrowable {
  @Shadow
  public abstract Message getRawMessage();

  @Override
  public @NonNull Component componentMessage() {
    final net.minecraft.network.chat.Component minecraft = ComponentUtils.fromMessage(this.getRawMessage());
    return FabricAudiences.nonWrappingSerializer().deserialize(minecraft);
  }
}
