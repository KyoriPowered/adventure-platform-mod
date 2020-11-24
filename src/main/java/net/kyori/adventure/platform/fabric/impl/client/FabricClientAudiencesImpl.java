/*
 * This file is part of adventure, licensed under the MIT License.
 *
 * Copyright (c) 2020 KyoriPowered
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
package net.kyori.adventure.platform.fabric.impl.client;

import java.util.Locale;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.fabric.FabricAudiences;
import net.kyori.adventure.platform.fabric.FabricClientAudiences;
import net.kyori.adventure.platform.fabric.impl.WrappedComponent;
import net.kyori.adventure.text.renderer.ComponentRenderer;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import static java.util.Objects.requireNonNull;

public class FabricClientAudiencesImpl implements FabricClientAudiences {
  public static final FabricClientAudiences INSTANCE = new FabricClientAudiencesImpl(GlobalTranslator.renderer());
  private final PlainComponentSerializer plainSerializer;
  private final ComponentRenderer<Locale> renderer;
  private final ClientAudience audience;

  public FabricClientAudiencesImpl(final ComponentRenderer<Locale> renderer) {
    this.renderer = renderer;
    this.audience = new ClientAudience(Minecraft.getInstance(), this);
    this.plainSerializer = new PlainComponentSerializer(comp -> KeyMapping.createNameSupplier(comp.keybind()).get().getString(), comp -> this.plainSerializer().serialize(this.renderer.render(comp, Locale.getDefault())));
  }

  @Override
  public Audience audience() {
    return this.audience;
  }

  @Override
  public PlainComponentSerializer plainSerializer() {
    return this.plainSerializer;
  }

  @Override
  public ComponentRenderer<Locale> localeRenderer() {
    return this.renderer;
  }

  @Override
  public Component toNative(final net.kyori.adventure.text.Component adventure) {
    return new WrappedComponent(requireNonNull(adventure, "adventure"), this.renderer);
  }

  @Override
  public net.kyori.adventure.text.Component toAdventure(final Component vanilla) {
    if(vanilla instanceof WrappedComponent) {
      return ((WrappedComponent) vanilla).wrapped();
    } else {
      return FabricAudiences.nonWrappingSerializer().deserialize(vanilla);
    }
  }
}
