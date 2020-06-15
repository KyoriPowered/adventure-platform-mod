/*
 * Copyright © 2020 zml [at] stellardrift [.] ca
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the “Software”), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package ca.stellardrift.adventure.fabric.mixin;

import ca.stellardrift.adventure.fabric.FabricAudience;
import ca.stellardrift.adventure.fabric.FabricBossBarListener;
import ca.stellardrift.adventure.fabric.FabricPlatform;
import ca.stellardrift.adventure.fabric.GameEnums;
import com.mojang.authlib.GameProfile;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.EmptyComponent;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.title.Title;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.MessageType;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundIdS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.StopSoundS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class MixinServerPlayerEntity extends PlayerEntity implements FabricAudience {
    @Shadow
    public ServerPlayNetworkHandler networkHandler;

    public MixinServerPlayerEntity(World world, BlockPos pos, GameProfile gameProfile) {
        super(world, pos, gameProfile);
    }

    /**
     * Send a message to this receiver as a component
     *
     * @param text The text to send
     */
    @Override
    public void sendMessage(Component text) {
        sendMessage(MessageType.SYSTEM, text);
    }

    @Override
    public void sendMessage(MessageType type, Component text, UUID source) {
        if (type == MessageType.GAME_INFO) {
            networkHandler.sendPacket(new TitleS2CPacket(TitleS2CPacket.Action.ACTIONBAR, FabricPlatform.adapt(text)));
        } else {
            networkHandler.sendPacket(new GameMessageS2CPacket(FabricPlatform.adapt(text), type, source));
        }
    }

    @Override
    public void showBossBar(@NonNull BossBar bar) {
        FabricBossBarListener.INSTANCE.subscribe((ServerPlayerEntity) (Object) this, bar);
    }

    @Override
    public void hideBossBar(@NonNull BossBar bar) {
        FabricBossBarListener.INSTANCE.unsubscribe((ServerPlayerEntity) (Object) this, bar);
    }

    @Override
    public void playSound(@NonNull Sound sound) {
        this.networkHandler.sendPacket(new PlaySoundIdS2CPacket(FabricPlatform.adapt(sound.name()),
                GameEnums.SOUND_SOURCE.toMinecraft(sound.source()), this.getPos(), sound.volume(), sound.pitch()));
    }

    @Override
    public void playSound(final @NonNull Sound sound, final double x, final double y, final double z) {
        this.networkHandler.sendPacket(new PlaySoundIdS2CPacket(FabricPlatform.adapt(sound.name()),
          GameEnums.SOUND_SOURCE.toMinecraft(sound.source()), new Vec3d(x, y, z), sound.volume(), sound.pitch()));
    }

    @Override
    public void stopSound(@NonNull SoundStop stop) {
        final @Nullable Key sound = stop.sound();
        Sound.@Nullable Source src = stop.source();
        @Nullable SoundCategory cat = src == null ? null : GameEnums.SOUND_SOURCE.toMinecraft(src);
        this.networkHandler.sendPacket(new StopSoundS2CPacket(sound == null ? null : FabricPlatform.adapt(sound), cat));
    }

    private static final String BOOK_TITLE = "title";
    private static final String BOOK_AUTHOR = "author";
    private static final String BOOK_PAGES = "pages";
    private static final String BOOK_RESOLVED = "resolved";

    @Override
    public void openBook(final @NonNull Book book) {
        final ItemStack bookStack = new ItemStack(Items.WRITTEN_BOOK, 1);
        final CompoundTag bookTag = bookStack.getOrCreateTag();
        bookTag.putString(BOOK_TITLE, adventure$serialize(book.title()));
        bookTag.putString(BOOK_AUTHOR, adventure$serialize(book.author()));
        final ListTag pages = new ListTag();
        for(final Component page : book.pages()) {
            pages.add(StringTag.of(adventure$serialize(page)));
        }
        bookTag.put(BOOK_PAGES, pages);
        bookTag.putBoolean(BOOK_RESOLVED, true); // todo: any parseable texts?

        final ItemStack previous = this.inventory.getMainHandStack();
        this.networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(-2, this.inventory.selectedSlot, bookStack));
        this.openEditBookScreen(bookStack, Hand.MAIN_HAND);
        this.networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(-2, this.inventory.selectedSlot, previous));
    }

    private String adventure$serialize(final @NonNull Component component) {
        return GsonComponentSerializer.INSTANCE.serialize(component);
    }

    @Override
    public void showTitle(final @NonNull Title title) {
        if (!EmptyComponent.empty().equals(title.subtitle())) {
            this.networkHandler.sendPacket(new TitleS2CPacket(TitleS2CPacket.Action.SUBTITLE, FabricPlatform.adapt(title.subtitle())));
        }

        if (!EmptyComponent.empty().equals(title.title())) {
            this.networkHandler.sendPacket(new TitleS2CPacket(TitleS2CPacket.Action.TITLE, FabricPlatform.adapt(title.title())));
        }

        final int fadeIn = ticks(title.fadeInTime());
        final int fadeOut = ticks(title.fadeOutTime());
        final int dwell = ticks(title.stayTime());
        if (fadeIn != -1 || fadeOut != -1 || dwell != -1) {
            this.networkHandler.sendPacket(new TitleS2CPacket(fadeIn, dwell, fadeOut));
        }
    }

    private int ticks(Duration duration) {
        return (int) duration.get(ChronoUnit.SECONDS) * 20;
    }

    @Override
    public void clearTitle() {
        this.networkHandler.sendPacket(new TitleS2CPacket(TitleS2CPacket.Action.CLEAR, null));
    }

    @Override
    public void resetTitle() {
        this.networkHandler.sendPacket(new TitleS2CPacket(TitleS2CPacket.Action.RESET, null));
    }

    // Player tracking for boss bars

    @Inject(method = "copyFrom", at = @At("RETURN"))
    private void copyBossBars(final ServerPlayerEntity old, boolean alive, final CallbackInfo ci) {
        FabricBossBarListener.INSTANCE.replacePlayer(old, (ServerPlayerEntity) (Object) this);
    }

    @Inject(method = "onDisconnect", at = @At("RETURN"))
    private void removeBossBarsOnDisconnect(CallbackInfo ci) {
        FabricBossBarListener.INSTANCE.unsubscribeFromAll((ServerPlayerEntity) (Object) this);
    }

}
