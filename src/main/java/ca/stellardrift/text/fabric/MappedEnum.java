package ca.stellardrift.text.fabric;

import net.kyori.adventure.util.NameMap;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/**
 * Represents a mapping between an enum controlled by Minecraft, and an enum controlled by
 * Adventure.
 * <p>
 * Because these enums both refer to known constants, it is relatively easy to generate an automated
 * mapping between the two of them. As the two enums represent the same set of values, it is
 * required that any element that appears in one is present in the other, so mappings will never
 * return null.
 *
 * @param <Mc>  The Minecraft type
 * @param <Adv> The Adventure type
 */
public class MappedEnum<Mc extends Enum<Mc>, Adv extends Enum<Adv>> {
    private final EnumMap<Mc, Adv> mcToAdventure;
    private final EnumMap<Adv, Mc> adventureToMc;

    static <Mc extends Enum<Mc>, Adv extends Enum<Adv>> MappedEnum<Mc, Adv> named(Class<Mc> mcType, Function<String, @Nullable Mc> mcByName, Function<Mc, @Nullable String> mcToName, Class<Adv> advType, final NameMap<Adv> names) {
        return new MappedEnum<>(mcType, mcByName, mcToName, advType, names::name, name -> names.value(name).orElse(null));
    }

    MappedEnum(Class<Mc> mcType, Function<String, @Nullable Mc> mcByName, Function<Mc, @Nullable String> mcToName, Class<Adv> advType, Function<Adv, @Nullable String> advToName, Function<String, @Nullable Adv> advByName) {
        mcToAdventure = new EnumMap<>(mcType);
        adventureToMc = new EnumMap<>(advType);

        for (Adv advElement : advType.getEnumConstants()) {
            String mcName = advToName.apply(advElement);
            if (mcName == null) {
                throw new ExceptionInInitializerError("Unable to get name for enum element " + advElement + " of " + advType);
            }
            Mc mcElement = mcByName.apply(mcName);
            if (mcElement == null) {
                throw new ExceptionInInitializerError("Unknown MC " + mcType + "  for Adventure " + mcName);
            }
            mcToAdventure.put(mcElement, advElement);
            adventureToMc.put(advElement, mcElement);
        }

        checkCoverage(adventureToMc, advType);
    }

    /**
     * Validates that all members of an enum are present in the given map Throws {@link
     * IllegalStateException} if there is a missing value
     *
     * @param toCheck   The map to check
     * @param enumClass The enum class to verify coverage
     * @param <T>       The type of enum
     */
    private static <T extends Enum<T>> void checkCoverage(Map<T, ?> toCheck, Class<T> enumClass) throws IllegalStateException {
        for (T value : enumClass.getEnumConstants()) {
            if (!toCheck.containsKey(value)) {
                throw new IllegalStateException("Unmapped " + enumClass.getSimpleName() + " element '" + value + '!');
            }
        }
    }

    /**
     * Given a Minecraft enum element, return the equivalent Adventure element.
     *
     * @param mcItem The Minecraft element
     * @return The adventure equivalent.
     */
    public @NonNull Adv toAdventure(@NonNull Mc mcItem) {
        return requireNonNull(mcToAdventure.get(mcItem), "Invalid enum value presented: " + mcItem);
    }

    /**
     * Given an Adventure enum element, return the equivalent Minecraft element.
     *
     * @param advItem The Minecraft element
     * @return The adventure equivalent.
     */
    public @NonNull Mc toMinecraft(@NonNull Adv advItem) {
        return adventureToMc.get(advItem);
    }
}
