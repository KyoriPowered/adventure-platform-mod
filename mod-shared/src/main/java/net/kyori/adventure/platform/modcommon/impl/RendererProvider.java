package net.kyori.adventure.platform.modcommon.impl;

import net.kyori.adventure.pointer.Pointered;
import net.kyori.adventure.text.renderer.ComponentRenderer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public interface RendererProvider {
  @NotNull ComponentRenderer<Pointered> renderer();

  Component toNative(net.kyori.adventure.text.Component adventure);
}
