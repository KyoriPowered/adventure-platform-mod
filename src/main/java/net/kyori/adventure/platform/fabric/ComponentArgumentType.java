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

package net.kyori.adventure.platform.fabric;

import net.kyori.adventure.platform.fabric.mixin.AccessorTextSerializer;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonReader;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.minecraft.command.arguments.TextArgumentType;
import net.minecraft.command.arguments.serialize.ArgumentSerializer;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

/**
 * An argument that takes JSON-format text.
 *
 * <p>At the moment, using this argument type will require this mod
 * <strong>both server- and clientside</strong>.
 */
public class ComponentArgumentType implements ArgumentType<Component> {

  private static final ComponentArgumentType INSTANCE = new ComponentArgumentType();
  private static final Set<String> EXAMPLES = ImmutableSet.of(
    "\"Hello world!\"",
    "[\"Message\", {\"text\": \"example\", \"color\": \"#aabbcc\"}]"
  );

  /**
   * Get the argument type for component arguments
   *
   * @return argument type instance
   */
  public static ComponentArgumentType component() {
    return INSTANCE;
  }

  /**
   * Get the component from the provided context
   *
   * @param ctx Context to get from
   * @param key argument key
   * @return parsed component
   */
  public static Component getComponent(CommandContext<?> ctx, String key) {
    return ctx.getArgument(key, Component.class);
  }

  private ComponentArgumentType() { }

  @Override
  public Component parse(final StringReader reader) throws CommandSyntaxException {
    try (final JsonReader json = new JsonReader(new java.io.StringReader(reader.getRemaining()))) {
      final Component ret = AccessorTextSerializer.getGSON().fromJson(json, Component.class);
      reader.setCursor(reader.getCursor() + AccessorTextSerializer.getPosition(json));
      return ret;
    } catch(JsonParseException | IOException ex) {
      final String message = ex.getCause() == null ? ex.getMessage() : ex.getCause().getMessage();
      throw TextArgumentType.INVALID_COMPONENT_EXCEPTION.createWithContext(reader, message);
    }
  }


  @Override
  public Collection<String> getExamples() {
    return EXAMPLES;
  }

  static class Serializer implements ArgumentSerializer<ComponentArgumentType> {
    private static final Identifier SERIALIZER_GSON = new Identifier("adventure", "gson");

    @Override
    public void toPacket(final ComponentArgumentType type, final PacketByteBuf buffer) {
      buffer.writeIdentifier(SERIALIZER_GSON);
    }

    @Override
    public ComponentArgumentType fromPacket(final PacketByteBuf buffer) {
      buffer.readIdentifier(); // TODO: Serializer type
      return ComponentArgumentType.component();
    }

    @Override
    public void toJson(final ComponentArgumentType type, final JsonObject json) {
      json.add("serializer", AccessorTextSerializer.getGSON().toJsonTree(SERIALIZER_GSON));
    }
  }
}
