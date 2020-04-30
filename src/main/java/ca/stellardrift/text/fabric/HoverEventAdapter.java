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

import java.util.HashMap;
import java.util.Map;

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
        return (deep ? TextAdapter.textNonWrapping() : TextAdapter.text()).serialize(comp);
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
            return TextAdapter.textNonWrapping().deserialize(mc);
        }
    }

    static final class ShowEntity extends HoverEventAdapter<HoverEvent.EntityContent, net.kyori.adventure.text.event.HoverEvent.ShowEntity> {

        ShowEntity() {
            super(HoverEvent.Action.SHOW_ENTITY, Action.SHOW_ENTITY);
        }

        @Override
        public HoverEvent.EntityContent toMinecraft(final net.kyori.adventure.text.event.HoverEvent.ShowEntity showEntity, final boolean deep) {
            final EntityType<?> type = Registry.ENTITY_TYPE.get(TextAdapter.toIdentifier(showEntity.type()));
            return new HoverEvent.EntityContent(type, showEntity.id(), showEntity.name() == null ? null : textToMc(showEntity.name(), deep));
        }

        @Override
        public net.kyori.adventure.text.event.HoverEvent.ShowEntity toAdventure(final HoverEvent.EntityContent mc) {
            final Key type = TextAdapter.toKey(Registry.ENTITY_TYPE.getId(mc.entityType));
            final @Nullable Text text = mc.name;
            return new net.kyori.adventure.text.event.HoverEvent.ShowEntity(type, mc.uuid, text == null ? null : TextAdapter.text().deserialize(text));
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
            return new net.kyori.adventure.text.event.HoverEvent.ShowItem(TextAdapter.toKey(Registry.ITEM.getId(mc.getItem())), mc.getCount());
        }
    }

}
