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

package net.kyori.adventure.platform.fabric.client;

import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.platform.fabric.FabricPlatform;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.network.chat.FormattedText;

import static java.util.Objects.requireNonNull;

/**
 * An implementation of the book GUI's contents.
 *
 * <p>This implementation gets its data directly from an
 * Adventure {@link Book}, without needing
 * an {@link net.minecraft.world.item.ItemStack}.</p>
 */
public class AdventureBookAccess implements BookViewScreen.BookAccess {
  private final Book book;

  public AdventureBookAccess(final Book book) {
    this.book = requireNonNull(book, "book");
  }

  @Override
  public int getPageCount() {
    return this.book.pages().size();
  }

  @Override
  public FormattedText getPageRaw(final int index) {
    return FabricPlatform.adapt(this.book.pages().get(index));
  }
}
