/*
 * This file is part of adventure-platform-mod, licensed under the MIT License.
 *
 * Copyright (c) 2022-2024 KyoriPowered
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
package net.kyori.adventure.platform.modcommon.impl.mixin.minecraft.commands;

import com.google.common.collect.Iterators;
import com.mojang.brigadier.tree.CommandNode;
import java.util.Iterator;
import net.kyori.adventure.platform.modcommon.impl.HiddenRequirement;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Commands.class)
public abstract class CommandsMixin {
  /**
   * Hide hidden commands from the client upon sync.
   *
   * <p>This injection is optional because its failure won't break any essential behavior.</p>
   *
   * @param itr original rootCommandSource.getChildren() iterator
   * @return the filtered iterator
   */
  @ModifyVariable(method = "fillUsableCommands", at = @At("STORE"), ordinal = 0, require = 0)
  private Iterator<CommandNode<CommandSourceStack>> adventure$filterHiddenCommands(final Iterator<CommandNode<CommandSourceStack>> itr) {
    return Iterators.filter(itr, node -> !(node.getRequirement() instanceof HiddenRequirement<CommandSourceStack>));
  }
}
