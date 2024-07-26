/*
 * This file is part of adventure-platform-mod, licensed under the MIT License.
 *
 * Copyright (c) 2024 KyoriPowered
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
package net.kyori.adventure.platform.neoforge.impl.mixin.neoforge;

import com.google.common.collect.Iterators;
import com.mojang.brigadier.tree.CommandNode;
import java.util.Iterator;
import net.kyori.adventure.platform.modcommon.impl.HiddenRequirement;
import net.kyori.adventure.platform.neoforge.impl.HiddenRequirementHelper;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.neoforge.server.command.CommandHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(CommandHelper.class)
abstract class CommandHelperMixin {

  /**
   * Hide hidden commands from the client upon sync.
   *
   * <p>This injection is optional because its failure won't break any essential behavior.</p>
   *
   * @param itr original rootCommandSource.getChildren() iterator
   * @return the filtered iterator
   */
  @ModifyVariable(method = "mergeCommandNode", at = @At("STORE"), ordinal = 0, require = 0)
  private static Iterator<CommandNode<CommandSourceStack>> adventure$filterHiddenCommands(final Iterator<CommandNode<CommandSourceStack>> itr) {
    if (HiddenRequirementHelper.SENDING.get()) {
      return Iterators.filter(itr, node -> !(node.getRequirement() instanceof HiddenRequirement<CommandSourceStack>));
    }
    return itr;
  }
}
