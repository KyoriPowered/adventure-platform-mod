/*
 * This file is part of adventure-platform-mod, licensed under the MIT License.
 *
 * Copyright (c) 2020-2024 KyoriPowered
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
package net.kyori.adventure.platform.modcommon;

import com.google.gson.stream.JsonReader;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Collection;
import java.util.List;
import net.kyori.adventure.Adventure;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.platform.modcommon.impl.accessor.minecraft.commands.ParserUtilsAccess;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.util.Index;
import net.minecraft.commands.arguments.ComponentArgument;
import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

/**
 * An argument that takes JSON-format text.
 *
 * <p>For this argument type to function, adventure-platform must also be present on the client.
 * In the Fabric environment, clients without adventure-platform will receive fallback argument types with limited
 * functionality, in other environments clients without the mod are expected to error on join.</p>
 *
 * @since 4.0.0
 */
public final class ComponentArgumentType implements ArgumentType<Component> {
  private static final ComponentArgumentType JSON_INSTANCE = new ComponentArgumentType(Format.JSON);
  private static final ComponentArgumentType MINIMESSAGE_INSTANCE = new ComponentArgumentType(Format.MINIMESSAGE);

  /**
   * Get the argument type for component arguments.
   *
   * @return argument type instance
   * @since 4.0.0
   * @deprecated use {@link #json()} or {@link #miniMessage()} instead.
   */
  @Deprecated(forRemoval = true, since = "5.1.0")
  public static @NotNull ComponentArgumentType component() {
    return JSON_INSTANCE;
  }

  /**
   * Get the component from the provided context.
   *
   * @param ctx Context to get from
   * @param key argument key
   * @return parsed component
   * @since 4.0.0
   */
  public static @NotNull Component component(final @NotNull CommandContext<?> ctx, final @NotNull String key) {
    return ctx.getArgument(key, Component.class);
  }

  /**
   * Get the argument type for component arguments in JSON format.
   *
   * @return argument type instance
   * @since 5.1.0
   */
  public static @NotNull ComponentArgumentType json() {
    return JSON_INSTANCE;
  }

  /**
   * Get the argument type for component arguments in MiniMessage format.
   *
   * @return argument type instance
   * @since 5.1.0
   */
  public static @NotNull ComponentArgumentType miniMessage() {
    return MINIMESSAGE_INSTANCE;
  }

  /**
   * Get an argument type for component arguments.
   *
   * @param format the format to use when parsing component text
   * @return an argument type
   * @since 5.1.0
   */
  public static @NotNull ComponentArgumentType component(final @NotNull Format format) {
    return switch (format) {
      case JSON -> JSON_INSTANCE;
      case MINIMESSAGE -> MINIMESSAGE_INSTANCE;
    };
  }

  private final Format format;

  private ComponentArgumentType(final Format format) {
    this.format = requireNonNull(format, "format");
  }

  @Override
  public @NotNull Component parse(final @NotNull StringReader reader) throws CommandSyntaxException {
    final String remaining = reader.getRemaining();
    try {
      final ReadResult result = this.format.parse(remaining);
      reader.setCursor(reader.getCursor() + result.charsConsumed());
      return result.parsed();
    } catch (final Exception ex) {
      final String message = ex.getCause() == null ? ex.getMessage() : ex.getCause().getMessage();
      throw ComponentArgument.ERROR_INVALID_JSON.createWithContext(reader, message);
    }
  }

  @Override
  public @NotNull Collection<String> getExamples() {
    return this.format.examples();
  }

  /**
   * Get the format used for this argument.
   *
   * @return the format used for this argument
   * @since 4.1.0
   */
  public @NotNull Format format() {
    return this.format;
  }

  /**
   * The result of reading a component.
   *
   * @param parsed        the parsed component
   * @param charsConsumed the number of characters consumed from the input string
   */
  record ReadResult(Component parsed, int charsConsumed) {}

  /**
   * Supported text formats for registering components.
   *
   * @since 5.1.0
   */
  public enum Format {
    JSON(
      Key.key(Adventure.NAMESPACE, "json"),
      "\"Hello world!\"",
      "[\"Message\", {\"text\": \"example\", \"color\": \"#aabbcc\"}]"
    ) {
      @Override
      ReadResult parse(final String allInput) throws Exception {
        try (final JsonReader json = new JsonReader(new java.io.StringReader(allInput))) {
          final Component ret = GsonComponentSerializer.gson().serializer().fromJson(json, Component.class);
          return new ReadResult(ret, ParserUtilsAccess.getPos(json));
        }
      }
    },
    MINIMESSAGE(
      Key.key(Adventure.NAMESPACE, "minimessage/v1"),
      "<rainbow>hello world!",
      "hello <bold>everyone</bold> here!",
      "hello <hover:show_text:'sneak sneak'>everyone</hover> who likes <blue>cats"
    ) {
      @Override
      ReadResult parse(final String allInput) throws Exception {
        final Component parsed = MiniMessage.miniMessage().deserialize(allInput);
        return new ReadResult(parsed, allInput.length());
      }
    };

    public static final Index<Key, Format> INDEX = Index.create(Format.class, Format::id);

    private final Key id;
    private final List<String> examples;

    Format(final Key id, final String... examples) {
      this.id = id;
      this.examples = List.of(examples);
    }

    abstract ReadResult parse(final String allInput) throws Exception;

    /**
     * Get a unique identifier for this format.
     *
     * @return a unique identifier for this format
     * @since 5.1.0
     */
    public Key id() {
      return this.id;
    }

    /**
     * Get examples of this format in use.
     *
     * @return examples of this format in use, as an unmodifiable list
     * @since 5.1.0
     */
    public List<String> examples() {
      return this.examples;
    }
  }
}
