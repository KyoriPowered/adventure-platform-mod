package ca.stellardrift.text.fabric.mixin;

import net.minecraft.item.Item;
import net.minecraft.text.HoverEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(HoverEvent.class_5249.class)
public interface AccessorHoverEventShowItem {
    @Accessor("field_24355")
    Item getItem();

    @Accessor("field_24356")
    int getCount();
}
