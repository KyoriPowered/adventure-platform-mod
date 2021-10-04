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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

/**
 * Via i509VCB, a trick to get Brig onto the Knot classpath in order to properly mix in.
 *
 * <p>YOU SHOULD ONLY USE THIS CLASS DURING "preLaunch" and ONLY TARGET A CLASS WHICH IS NOT ANY CLASS YOU MIXIN TO.
 * This will likely not work on Gson because FabricLoader has some special logic related to Gson.</p>
 *
 * <p>Original on GitHub at <a href="https://github.com/i509VCB/Fabric-Junkkyard/blob/ce278daa93804697c745a51af06ec812896ec2ad/src/main/java/me/i509/junkkyard/hacks/PreLaunchHacks.java">i509VCB/Fabric-Junkkyard</a></p>
 */
public class AdventurePrelaunch implements PreLaunchEntrypoint {
  private static final ClassLoader KNOT_CLASSLOADER = Thread.currentThread().getContextClassLoader();
  private static final Method ADD_URL_METHOD;

  static {
    try {
      ADD_URL_METHOD = KNOT_CLASSLOADER.getClass().getMethod("addURL", URL.class);
      ADD_URL_METHOD.setAccessible(true);
    } catch (final ReflectiveOperationException ex) {
      throw new RuntimeException("Failed to load Classloader fields", ex);
    }
  }

  @Override
  public void onPreLaunch() {
    try {
      AdventurePrelaunch.hackilyLoadForMixin("com.mojang.authlib.UserAuthentication");
    } catch (final ClassNotFoundException | InvocationTargetException | IllegalAccessException e) {
      throw new RuntimeException("please fix fabric loader to enable transforming libraries normally", e);
    }
  }

  /**
   * Hackily load the package which a mixin may exist within.
   *
   * <p>YOU SHOULD NOT TARGET A CLASS WHICH YOU MIXIN TO. (this could be done to better avoid class loads)</p>
   *
   * @param pathOfAClass The path of any class within the package.
   * @throws ClassNotFoundException if an unknown class name is used
   * @throws InvocationTargetException if an error occurs while injecting
   * @throws IllegalAccessException if an error occurs while injecting
   */
  static void hackilyLoadForMixin(final String pathOfAClass) throws ClassNotFoundException, InvocationTargetException, IllegalAccessException {
    final URL url = Class.forName(pathOfAClass).getProtectionDomain().getCodeSource().getLocation();
    ADD_URL_METHOD.invoke(KNOT_CLASSLOADER, url);
  }
}
