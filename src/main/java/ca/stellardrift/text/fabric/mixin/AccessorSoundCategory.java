package ca.stellardrift.text.fabric.mixin;

import java.util.Map;
import net.minecraft.sound.SoundCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SoundCategory.class)
public interface AccessorSoundCategory {
  @Accessor("NAME_MAP")
  static Map<String, SoundCategory> getNameMap() {
    throw new UnsupportedOperationException("mixin replaced");
  }
}
