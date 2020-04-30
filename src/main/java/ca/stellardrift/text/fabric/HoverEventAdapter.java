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

package ca.stellardrift.text.fabric;

import ca.stellardrift.text.fabric.mixin.AccessorHoverEventShowItem;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.event.HoverEvent.Action;
import net.kyori.adventure.text.Component;
import net.minecraft.entity.EntityType;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.registry.Registry;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Adapters for the different HoverEvent types to convert between Minecraft and Adventure values
 *
 * @param <Mc> minecraft type
 * @param <Adv> adventure type
 */
abstract class HoverEventAdapter<Mc, Adv> {
    private final HoverEvent.Action<Mc> mcType;
    private final net.kyori.adventure.text.event.HoverEvent.Action<Adv> advType;

    protected HoverEventAdapter(final HoverEvent.Action<Mc> mcType, final Action<Adv> advType) {
        this.mcType = mcType;
        this.advType = advType;
    }

    public HoverEvent.Action<Mc> getMcAction() {
        return mcType;
    }

    public Action<Adv> getAdventureAction() {
        return advType;
    }

    public HoverEvent adapt(net.kyori.adventure.text.event.HoverEvent<Adv> adventure, boolean deep) {
        return new HoverEvent(mcType, toMinecraft(adventure.value(), deep));
    }

    public net.kyori.adventure.text.event.HoverEvent<Adv> adapt(HoverEvent mc) {
        return net.kyori.adventure.text.event.HoverEvent.of(advType, toAdventure(mc.getValue(mcType)));
    }

    protected Text textToMc(final Component comp, final boolean deep) {
        return deep ? TextAdapter.nonWrapping().serialize(comp) : TextAdapter.adapt(comp);
    }

    /**
     * Convert an adventure type to a minecraft type.
     *
     * @param adv The adventure type
     * @param deep Whether to perform a deep conversion, or if a wrapper can be used (when false)
     * @return The Minecraft type
     */
    protected abstract Mc toMinecraft(Adv adv, boolean deep);
    protected abstract Adv toAdventure(Mc mc);

    static final class ShowText extends HoverEventAdapter<Text, Component> {

        ShowText() {
            super(HoverEvent.Action.SHOW_TEXT, Action.SHOW_TEXT);
        }

        @Override
        public Text toMinecraft(final Component component, final boolean deep) {
            return textToMc(component, deep);
        }

        @Override
        public Component toAdventure(final Text mc) {
            return TextAdapter.nonWrapping().deserialize(mc);
        }
    }

    static final class ShowEntity extends HoverEventAdapter<HoverEvent.EntityContent, net.kyori.adventure.text.event.HoverEvent.ShowEntity> {

        ShowEntity() {
            super(HoverEvent.Action.SHOW_ENTITY, Action.SHOW_ENTITY);
        }

        @Override
        public HoverEvent.EntityContent toMinecraft(final net.kyori.adventure.text.event.HoverEvent.ShowEntity showEntity, final boolean deep) {
            final EntityType<?> type = Registry.ENTITY_TYPE.get(TextAdapter.adapt(showEntity.type()));
            return new HoverEvent.EntityContent(type, showEntity.id(), showEntity.name() == null ? null : textToMc(showEntity.name(), deep));
        }

        @Override
        public net.kyori.adventure.text.event.HoverEvent.ShowEntity toAdventure(final HoverEvent.EntityContent mc) {
            final Key type = TextAdapter.adapt(Registry.ENTITY_TYPE.getId(mc.entityType));
            final @Nullable Text text = mc.name;
            return new net.kyori.adventure.text.event.HoverEvent.ShowEntity(type, mc.uuid, text == null ? null : TextAdapter.adapt(text));
        }
    }

    static final class ShowItem extends HoverEventAdapter<HoverEvent.ItemStackContent, net.kyori.adventure.text.event.HoverEvent.ShowItem> {

        ShowItem() {
            super(HoverEvent.Action.SHOW_ITEM, Action.SHOW_ITEM);
        }

        @Override
        public HoverEvent.ItemStackContent toMinecraft(final net.kyori.adventure.text.event.HoverEvent.ShowItem showItem, boolean deep) {
            return null;
        }

        @Override
        public net.kyori.adventure.text.event.HoverEvent.ShowItem toAdventure(final HoverEvent.ItemStackContent itemStackContent) {
            AccessorHoverEventShowItem mc = (AccessorHoverEventShowItem) itemStackContent;
            return new net.kyori.adventure.text.event.HoverEvent.ShowItem(TextAdapter.adapt(Registry.ITEM.getId(mc.getItem())), mc.getCount());
        }
    }

}
