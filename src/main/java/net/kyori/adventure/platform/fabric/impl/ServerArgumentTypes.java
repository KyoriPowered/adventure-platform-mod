/*
 * This file is part of adventure-platform-fabric, licensed under the MIT License.
 *
 * Copyright (c) 2021 KyoriPowered
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
package net.kyori.adventure.platform.fabric.impl;

import com.mojang.brigadier.arguments.ArgumentType;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenCustomHashMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import net.kyori.adventure.platform.fabric.impl.server.ServerPlayerBridge;
import net.minecraft.Util;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import static java.util.Objects.requireNonNull;

public final class ServerArgumentTypes {
  private static final Map<Class<?>, ServerArgumentType<?>> BY_TYPE = new HashMap<>();
  private static final Map<ResourceLocation, ServerArgumentType<?>> BY_LOCATION = new ConcurrentHashMap<>();
  private static final Int2ObjectMap<ServerArgumentType<?>> BY_ID = new Int2ObjectArrayMap<>();
  private static final Object2IntMap<ArgumentTypeInfo<?, ?>> IDS_BY_TYPE_INFO = new Object2IntOpenCustomHashMap<>(Util.identityStrategy());
  private static final AtomicInteger ID_COUNTER = new AtomicInteger(100000); // start at 100,000. hopefully nobody registers that many types.

  @SuppressWarnings("unchecked")
  public static <T extends ArgumentType<?>> ServerArgumentType<T> byClass(final Class<T> clazz) {
    return (ServerArgumentType<T>) BY_TYPE.get(requireNonNull(clazz, "clazz"));
  }

  private ServerArgumentTypes() {
  }

  public static void register(final ServerArgumentType<?> type) {
    final int id = ID_COUNTER.getAndIncrement();
    BY_ID.put(id, type);
    IDS_BY_TYPE_INFO.put(type.argumentTypeInfo(), id);
    BY_TYPE.put(type.type(), type);
    BY_LOCATION.put(type.id(), type);
  }

  public static Set<ResourceLocation> ids() {
    return Collections.unmodifiableSet(BY_LOCATION.keySet());
  }

  public static boolean isServerType(final ArgumentTypeInfo<?, ?> argumentTypeInfo) {
    return IDS_BY_TYPE_INFO.containsKey(argumentTypeInfo);
  }

  public static boolean hasId(final int id) {
    return BY_ID.containsKey(id);
  }

  public static ServerArgumentType<?> byId(final int id) {
    return BY_ID.get(id);
  }

  public static ServerArgumentType<?> serverType(final ArgumentTypeInfo<?, ?> argumentTypeInfo) {
    return byId(IDS_BY_TYPE_INFO.getInt(argumentTypeInfo));
  }

  public static int id(final ArgumentTypeInfo<?, ?> argumentTypeInfo) {
    return IDS_BY_TYPE_INFO.getInt(argumentTypeInfo);
  }

  public static void knownArgumentTypes(final ServerPlayer player, final Set<ResourceLocation> ids) {
    ((ServerPlayerBridge) player).bridge$knownArguments(ids);
    if (!ids.isEmpty()) { // TODO: Avoid resending the whole command tree, find a way to receive the packet before sending?
      player.server.getCommands().sendCommands(player);
    }
  }

  public static Set<ResourceLocation> knownArgumentTypes(final ServerPlayer player) {
    return ((ServerPlayerBridge) player).bridge$knownArguments();
  }
}
