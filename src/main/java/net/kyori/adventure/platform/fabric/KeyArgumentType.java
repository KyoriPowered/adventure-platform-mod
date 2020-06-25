package net.kyori.adventure.platform.fabric;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kyori.adventure.key.Key;
import net.minecraft.util.Identifier;

/**
 * An argument that will be decoded as a Key
 */
public class KeyArgumentType implements ArgumentType<Key> {
  private static final KeyArgumentType INSTANCE = new KeyArgumentType();

  public static KeyArgumentType key() {
    return INSTANCE;
  }

  public static Key getKey(final CommandContext<?> ctx, final String id) {
    return ctx.getArgument(id, Key.class);
  }

  private KeyArgumentType() {}

  @Override
  public Key parse(final StringReader reader) throws CommandSyntaxException {
    // TODO: do this without creating an Identifier instance
    return FabricPlatform.adapt(Identifier.fromCommandInput(reader));
  }
}
