/*
 * This file is part of adventure-platform-mod, licensed under the MIT License.
 *
 * Copyright (c) 2022-2025 KyoriPowered
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
package net.kyori.adventure.platform.fabric.impl.mixin.minecraft.commands;

import com.google.common.collect.Iterators;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import java.util.Iterator;
import java.util.Set;
import net.kyori.adventure.platform.fabric.impl.ServerArgumentType;
import net.kyori.adventure.platform.fabric.impl.ServerArgumentTypes;
import net.kyori.adventure.platform.fabric.impl.accessor.brigadier.builder.RequiredArgumentBuilderAccess;
import net.kyori.adventure.platform.modcommon.impl.HiddenRequirement;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Commands.class)
public abstract class CommandsMixin {

  @SuppressWarnings({"rawtypes", "unchecked"}) // argument type generics
  @WrapOperation(
    method = "fillUsableCommands",
    at = @At(
      value = "INVOKE",
      target = "Lcom/mojang/brigadier/builder/ArgumentBuilder;build()Lcom/mojang/brigadier/tree/CommandNode;",
      remap = false
    )
  )
  private static <T> CommandNode<T> adventure$replaceArgumentType(
    final ArgumentBuilder<T, ?> instance,
    final Operation<CommandNode<T>> original,
    final @Local(argsOnly = true) T sourceRaw
  ) {
    if (!(instance instanceof RequiredArgumentBuilder builder)) {
      return original.call(instance); // only replace argument types for required arguments
    }
    final CommandSourceStack source = (CommandSourceStack) sourceRaw;
    ServerArgumentType<ArgumentType<T>> type = ServerArgumentTypes.byClass((Class) builder.getType().getClass());
    final Set<ResourceLocation> knownExtraCommands = ServerArgumentTypes.knownArgumentTypes(source.getPlayer()); // throws an exception, we can ignore bc this is always a player
    // If we have a replacement and the arg type isn't known to the client, change the argument type
    // This is super un-typesafe, but as long as the returned CommandNode is only used for serialization we are fine.
    // Repeat as long as a type is replaceable -- that way you can have a hierarchy of argument types.
    while (type != null && !knownExtraCommands.contains(type.id())) {
      final CommandBuildContext ctx = CommandBuildContext.simple(source.registryAccess(), source.enabledFeatures());
      ((RequiredArgumentBuilderAccess) builder).accessor$type(type.fallbackProvider().apply(builder.getType(), ctx));
      if (type.fallbackSuggestions() != null) {
        builder.suggests(type.fallbackSuggestions());
      }
      type = ServerArgumentTypes.byClass((Class) builder.getType().getClass());
    }
    return original.call(instance);
  }

  /**
   * Hide hidden commands from the client upon sync.
   *
   * <p>This injection is optional because its failure won't break any essential behavior.</p>
   *
   * @param itr original rootCommandSource.getChildren() iterator
   * @return the filtered iterator
   */
  @ModifyVariable(method = "fillUsableCommands", at = @At("STORE"), ordinal = 0, require = 0)
  private static Iterator<CommandNode<CommandSourceStack>> adventure$filterHiddenCommands(final Iterator<CommandNode<CommandSourceStack>> itr) {
    return Iterators.filter(itr, node -> !(node.getRequirement() instanceof HiddenRequirement<CommandSourceStack>));
  }
}
