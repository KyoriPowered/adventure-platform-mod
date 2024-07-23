package net.kyori.adventure.platform.tester.neoforge;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import net.kyori.adventure.platform.modcommon.MinecraftClientAudiences;
import net.kyori.adventure.text.Component;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.event.ClickEvent.openFile;
import static net.kyori.adventure.text.format.TextColor.color;

@Mod(value = "adventure_platform_neoforge_tester", dist = Dist.CLIENT)
public class AdventureNeoTesterClient {
  public AdventureNeoTesterClient() {
    final GuiMessageTag kyoriMessage = new GuiMessageTag(0x987bd8, null, net.minecraft.network.chat.Component.literal("Adventure Message"), "Adventure");
    NeoForge.EVENT_BUS.addListener((RegisterClientCommandsEvent e) -> {
      e.getDispatcher().register(LiteralArgumentBuilder.<CommandSourceStack>literal("adventure_client")
        .then(LiteralArgumentBuilder.<CommandSourceStack>literal("open_file").executes(ctx -> {
          final Path path = FMLLoader.getGamePath().resolve("adventure_test_file.txt").toAbsolutePath();
          try {
            Files.writeString(path, "Hello there " + Minecraft.getInstance().getUser().getName() + "!", StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
          } catch (final IOException ex) {
            throw new RuntimeException("Uh oh! Couldn't write file!", ex);
          }
          final Component message = text()
            .content("Click to open ")
            .append(text(path.getFileName().toString(), color(0xFFA2C4)))
            .append(text('!'))
            .clickEvent(openFile(path.toString()))
            .build();

          Minecraft.getInstance().gui.getChat().addMessage(MinecraftClientAudiences.of().toNative(message), null, kyoriMessage);
          // ctx.getSource().getPlayer().sendMessage(message); // Works as well!

          return Command.SINGLE_SUCCESS;
        })));
    });
  }
}
