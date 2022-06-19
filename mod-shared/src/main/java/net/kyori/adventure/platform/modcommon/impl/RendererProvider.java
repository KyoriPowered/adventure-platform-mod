package net.kyori.adventure.platform.modcommon.impl;

import java.util.function.Function;
import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.renderer.ComponentRenderer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public interface RendererProvider {
  @NotNull ComponentRenderer<Pointered> renderer();
  @NotNull Function<Pointered, ?> partition();

  Component toNative(net.kyori.adventure.text.Component adventure);
}
