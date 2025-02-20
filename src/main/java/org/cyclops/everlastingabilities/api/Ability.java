package org.cyclops.everlastingabilities.api;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.common.conditions.TrueCondition;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * An ability instance.
 * @author rubensworks
 */
public class Ability implements Comparable<Ability> {

    public static final Ability EMPTY = new Ability(Holder.direct(new AbilityTypeAdapter(TrueCondition.INSTANCE, "", Rarity.COMMON, 0, 0, true, true, true, true) {
        @Override
        public MapCodec<? extends IAbilityType> codec() {
            return null;
        }
    }), 0);

    private final Holder<IAbilityType> abilityType;
    private final int level;

    public Ability(@Nonnull Holder<IAbilityType> abilityType, int level) {
        this.abilityType = Objects.requireNonNull(abilityType);
        this.level = level;
    }

    public Holder<IAbilityType> getAbilityTypeHolder() {
        return abilityType;
    }

    public IAbilityType getAbilityType() {
        return getAbilityTypeHolder().value();
    }

    public int getLevel() {
        return level;
    }

    @Override
    public String toString() {
        return String.format("[%s @ %s]", abilityType.value().getTranslationKey(), level);
    }

    @Override
    public int compareTo(Ability other) {
        return this.toString().compareTo(other.toString());
    }

    public Component getTextComponent() {
        return Component.literal("[")
                .append(Component.translatable(abilityType.value().getTranslationKey()))
                .append(" @ " + level + "]");
    }

    public boolean isEmpty() {
        return getLevel() <= 0;
    }

}
