package net.kyori.adventure.platform.fabric.impl.accessor;

import com.mojang.brigadier.arguments.ArgumentType;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.Registry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ArgumentTypeInfos.class)
public interface ArgumentTypeInfosAccess {
  @Invoker("register")
  public static <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>> ArgumentTypeInfo<A, T> adventure$invoke$register(
    final Registry<ArgumentTypeInfo<?, ?>> registry,
    final String string,
    final Class<? extends A> class_,
    final ArgumentTypeInfo<A, T> argumentTypeInfo
  ) {
    throw new IncompatibleClassChangeError("mixin not injected");
  }
}
