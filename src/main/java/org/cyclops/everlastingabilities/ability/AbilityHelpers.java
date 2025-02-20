package org.cyclops.everlastingabilities.ability;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.NonNull;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.conditions.ICondition;
import org.apache.commons.lang3.tuple.Triple;
import org.cyclops.cyclopscore.helper.Helpers;
import org.cyclops.everlastingabilities.Capabilities;
import org.cyclops.everlastingabilities.EverlastingAbilities;
import org.cyclops.everlastingabilities.GeneralConfig;
import org.cyclops.everlastingabilities.api.Ability;
import org.cyclops.everlastingabilities.api.AbilityTypes;
import org.cyclops.everlastingabilities.api.IAbilityType;
import org.cyclops.everlastingabilities.api.capability.IAbilityStore;
import org.cyclops.everlastingabilities.api.capability.IMutableAbilityStore;
import org.cyclops.everlastingabilities.item.ItemAbilityTotem;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * General ability helpers.
 * XP-related methods inspired by Ender IO's XpUtil and the Minecraft Wiki
 * @author rubensworks
 */
public class AbilityHelpers {

    /**
     * This value is synced with {@link GeneralConfig#maxPlayerAbilities} from the server.
     * This is to ensure that clients can not hack around the ability limit.
     */
    public static int maxPlayerAbilitiesClient = -1;

    public static final int[] RARITY_COLORS = new int[] {
            Helpers.RGBToInt(255, 255, 255),
            Helpers.RGBToInt(255, 255, 0),
            Helpers.RGBToInt(0, 255, 255),
            Helpers.RGBToInt(255, 0, 255),
    };

    public static Predicate<Holder<IAbilityType>> PREDICATE_ABILITY_ENABLED = ability -> ability.value().getCondition().test(ICondition.IContext.EMPTY);

    public static Registry<IAbilityType> getRegistry(RegistryAccess registryAccess) {
        return registryAccess.registryOrThrow(AbilityTypes.REGISTRY_KEY);
    }

    public static HolderLookup.RegistryLookup<IAbilityType> getRegistryLookup(HolderLookup.Provider holderLookupProvider) {
        return holderLookupProvider.lookupOrThrow(AbilityTypes.REGISTRY_KEY);
    }

    public static int getExperienceForLevel(int level) {
        if (level == 0) {
            return 0;
        } else if (level < 16) {
            return (int) (Math.pow(level, 2) + 6 * level);
        } else if (level < 31) {
            return (int) (2.5 * Math.pow(level, 2) - 40.5 * level + 360);
        } else {
            return (int) (4.5 * Math.pow(level, 2) - 162.5 * level + 2220);
        }
    }

    public static int getLevelForExperience(int experience) {
        int i = 0;
        int newXp, lastXp = -1;
        while ((newXp = getExperienceForLevel(i)) <= experience) {
            if (newXp <= lastXp) break; // Avoid infinite loops when the MC level is too high, resulting in an xp overflow. See https://github.com/CyclopsMC/EverlastingAbilities/issues/27
            i++;
            lastXp = newXp;
        }
        return i - 1;
    }

    public static Predicate<Holder<IAbilityType>> createRarityPredicate(Rarity rarity) {
        return abilityType -> abilityType.value().getRarity() == rarity;
    }

    public static List<Holder<IAbilityType>> getAbilityTypes(Registry<IAbilityType> registry, Predicate<Holder<IAbilityType>> abilityFilter) {
        return registry
                .holders()
                .filter(abilityFilter)
                .collect(Collectors.toList());
    }

    public static List<Holder<IAbilityType>> getAbilityTypes(HolderLookup.Provider holderLookupProvider, Predicate<Holder<IAbilityType>> abilityFilter) {
        return AbilityHelpers.getRegistryLookup(holderLookupProvider)
                .listElements()
                .filter(abilityFilter)
                .collect(Collectors.toList());
    }

    public static List<Holder<IAbilityType>> getAbilityTypesPlayerSpawn(Registry<IAbilityType> registry) {
        return getAbilityTypes(registry, PREDICATE_ABILITY_ENABLED.and(holder -> holder.value().isObtainableOnPlayerSpawn()));
    }

    public static List<Holder<IAbilityType>> getAbilityTypesMobSpawn(Registry<IAbilityType> registry) {
        return getAbilityTypes(registry, PREDICATE_ABILITY_ENABLED.and(holder -> holder.value().isObtainableOnMobSpawn()));
    }

    public static List<Holder<IAbilityType>> getAbilityTypesCrafting(Registry<IAbilityType> registry) {
        return getAbilityTypes(registry, PREDICATE_ABILITY_ENABLED.and(holder -> holder.value().isObtainableOnCraft()));
    }

    public static List<Holder<IAbilityType>> getAbilityTypesCrafting(HolderLookup.Provider provider) {
        return getAbilityTypes(provider, PREDICATE_ABILITY_ENABLED.and(holder -> holder.value().isObtainableOnCraft()));
    }

    public static List<Holder<IAbilityType>> getAbilityTypesLoot(Registry<IAbilityType> registry) {
        return getAbilityTypes(registry, PREDICATE_ABILITY_ENABLED.and(holder -> holder.value().isObtainableOnLoot()));
    }

    public static void onPlayerAbilityChanged(Player player, IAbilityType abilityType, int oldLevel, int newLevel) {
        abilityType.onChangedLevel(player, oldLevel, newLevel);
    }

    public static int getMaxPlayerAbilities(Level world) {
        return world.isClientSide() ? maxPlayerAbilitiesClient : GeneralConfig.maxPlayerAbilities;
    }

    /**
     * Add the given ability.
     * @param player The player.
     * @param ability The ability.
     * @param doAdd If the addition should actually be done.
     * @param modifyXp Whether to require player to have enough XP before adding
     * @return The ability part that was added.
     */
    @NonNull
    public static Ability addPlayerAbility(Player player, Ability ability, boolean doAdd, boolean modifyXp) {
        return Optional.ofNullable(player.getCapability(Capabilities.MutableAbilityStore.ENTITY))
                .map(abilityStore -> {
                    int oldLevel = abilityStore.hasAbilityType(ability.getAbilityTypeHolder())
                            ? abilityStore.getAbility(ability.getAbilityTypeHolder()).getLevel() : 0;

                    // Check max ability count
                    if (getMaxPlayerAbilities(player.getCommandSenderWorld()) >= 0 && oldLevel == 0
                            && getMaxPlayerAbilities(player.getCommandSenderWorld()) <= abilityStore.getAbilities().size()) {
                        return Ability.EMPTY;
                    }

                    Ability result = abilityStore.addAbility(ability, doAdd);
                    int currentXp = player.totalExperience;
                    if (result != null && modifyXp && getExperience(result) > currentXp) {
                        int maxLevels = player.totalExperience / result.getAbilityType().getXpPerLevelScaled();
                        if (maxLevels == 0) {
                            result = Ability.EMPTY;
                        } else {
                            result = new Ability(result.getAbilityTypeHolder(), maxLevels);
                        }
                    }
                    if (doAdd && !result.isEmpty()) {
                        player.totalExperience -= getExperience(result);
                        // Fix xp bar
                        player.experienceLevel = getLevelForExperience(player.totalExperience);
                        int xpForLevel = getExperienceForLevel(player.experienceLevel);
                        player.experienceProgress = (float)(player.totalExperience - xpForLevel) / (float)player.getXpNeededForNextLevel();

                        int newLevel = abilityStore.getAbility(result.getAbilityTypeHolder()).getLevel();
                        onPlayerAbilityChanged(player, result.getAbilityType(), oldLevel, newLevel);
                    }
                    return result;
                })
                .orElse(Ability.EMPTY);
    }

    /**
     * Remove the given ability.
     * @param player The player.
     * @param ability The ability.
     * @param doRemove If the removal should actually be done.
     * @param modifyXp Whether to refund XP cost of ability
     * @return The ability part that was removed.
     */
    @NonNull
    public static Ability removePlayerAbility(Player player, Ability ability, boolean doRemove, boolean modifyXp) {
        return Optional.ofNullable(player.getCapability(Capabilities.MutableAbilityStore.ENTITY))
                .map(abilityStore -> {
                    int oldLevel = abilityStore.hasAbilityType(ability.getAbilityTypeHolder())
                            ? abilityStore.getAbility(ability.getAbilityTypeHolder()).getLevel() : 0;
                    Ability result = abilityStore.removeAbility(ability, doRemove);
                    if (modifyXp && !result.isEmpty()) {
                        player.giveExperiencePoints(getExperience(result));
                        int newLevel = abilityStore.hasAbilityType(result.getAbilityTypeHolder())
                                ? abilityStore.getAbility(result.getAbilityTypeHolder()).getLevel() : 0;
                        onPlayerAbilityChanged(player, result.getAbilityType(), oldLevel, newLevel);
                    }
                    return result;
                })
                .orElse(Ability.EMPTY);
    }

    public static int getExperience(@NonNull Ability ability) {
        if (ability.isEmpty()) {
            return 0;
        }
        return ability.getAbilityType().getXpPerLevelScaled() * ability.getLevel();
    }

    public static void setPlayerAbilities(ServerPlayer player, Map<Holder<IAbilityType>, Integer> abilityTypes) {
        Optional.ofNullable(player.getCapability(Capabilities.MutableAbilityStore.ENTITY))
                .ifPresent(abilityStore -> abilityStore.setAbilities(abilityTypes));
    }

    public static boolean canInsert(Ability ability, IMutableAbilityStore mutableAbilityStore) {
        Ability added = mutableAbilityStore.addAbility(ability, false);
        return added.getLevel() == ability.getLevel();
    }

    public static boolean canExtract(Ability ability, IMutableAbilityStore mutableAbilityStore) {
        Ability added = mutableAbilityStore.removeAbility(ability, false);
        return added.getLevel() == ability.getLevel();
    }

    public static boolean canInsertToPlayer(Ability ability, Player player) {
        Ability added = addPlayerAbility(player, ability, false, true);
        return added.getLevel() == ability.getLevel();
    }

    public static Ability insert(Ability ability, IMutableAbilityStore mutableAbilityStore) {
        return mutableAbilityStore.addAbility(ability, true);
    }

    public static Ability extract(Ability ability, IMutableAbilityStore mutableAbilityStore) {
        return mutableAbilityStore.removeAbility(ability, true);
    }

    public static Optional<Holder<IAbilityType>> getRandomAbility(List<Holder<IAbilityType>> abilityTypes, RandomSource random, Rarity rarity) {
        List<Holder<IAbilityType>> filtered = abilityTypes.stream().filter(createRarityPredicate(rarity)).toList();
        if (filtered.size() > 0) {
            return Optional.of(filtered.get(random.nextInt(filtered.size())));
        }
        return Optional.empty();
    }

    public static Optional<Holder<IAbilityType>> getRandomAbilityUntilRarity(List<Holder<IAbilityType>> abilityTypes, RandomSource random, Rarity rarity, boolean inclusive) {
        NavigableSet<Rarity> validRarities = AbilityHelpers.getValidAbilityRarities(abilityTypes).headSet(rarity, inclusive);
        Iterator<Rarity> it = validRarities.descendingIterator();
        while (it.hasNext()) {
            Optional<Holder<IAbilityType>> optional = getRandomAbility(abilityTypes, random, it.next());
            if (optional.isPresent()) {
                return optional;
            }
        }
        return Optional.empty();
    }

    public static Optional<ItemStack> getRandomTotem(List<Holder<IAbilityType>> abilityTypes, Rarity rarity, RandomSource rand) {
        return getRandomAbility(abilityTypes, rand, rarity).flatMap(
                abilityType -> Optional.of(ItemAbilityTotem.getTotem(new Ability(abilityType, 1))));
    }


    public static Optional<Rarity> getRandomRarity(List<Holder<IAbilityType>> abilityTypes, RandomSource rand) {
        int chance = rand.nextInt(50);
        Rarity rarity;
        if (chance >= 49) {
            rarity = Rarity.EPIC;
        } else if (chance >= 40) {
            rarity = Rarity.RARE;
        } else if (chance >= 25) {
            rarity = Rarity.UNCOMMON;
        } else {
            rarity = Rarity.COMMON;
        }

        // Fallback to a random selection of a rarity that is guaranteed to exist in the registered abilities
        if (!hasRarityAbilities(abilityTypes, rarity)) {
            int size = abilityTypes.size();
            if (size == 0) {
                return Optional.empty();
            }
            rarity = Iterables.get(abilityTypes, rand.nextInt(size)).value().getRarity();
        }

        return Optional.of(rarity);
    }

    public static boolean hasRarityAbilities(List<Holder<IAbilityType>> abilityTypes, Rarity rarity) {
        return abilityTypes.stream().anyMatch(createRarityPredicate(rarity));
    }

    public static NavigableSet<Rarity> getValidAbilityRarities(List<Holder<IAbilityType>> abilityTypes) {
        NavigableSet<Rarity> rarities = Sets.newTreeSet();
        for (Rarity rarity : Rarity.values()) {
            if (hasRarityAbilities(abilityTypes, rarity)) {
                rarities.add(rarity);
            }
        }
        return rarities;
    }

    public static Triple<Integer, Integer, Integer> getAverageRarityColor(IAbilityStore abilityStore) {
        int r = 0;
        int g = 0;
        int b = 0;
        int count = 1;
        for (Holder<IAbilityType> abilityType : abilityStore.getAbilityTypes()) {
            Triple<Float, Float, Float> color = Helpers.intToRGB(AbilityHelpers.RARITY_COLORS
                    [Math.min(AbilityHelpers.RARITY_COLORS.length - 1, abilityType.value().getRarity().ordinal())]);
            r += color.getLeft() * 255;
            g += color.getMiddle() * 255;
            b += color.getRight() * 255;
            count++;
        }
        return Triple.of(r / count, g / count, b / count);
    }

    public static Supplier<Rarity> getSafeRarity(Supplier<Integer> rarityGetter) {
        return () -> {
            Integer rarity = rarityGetter.get();
            return rarity < 0 ? Rarity.COMMON : (rarity >= Rarity.values().length ? Rarity.EPIC : Rarity.values()[rarity]);
        };
    }

    public static Tag serialize(Registry<IAbilityType> registry, IMutableAbilityStore capability) {
        ListTag list = new ListTag();
        for (Ability ability : capability.getAbilities()) {
            CompoundTag tag = new CompoundTag();
            tag.putString("name", registry.getKey(ability.getAbilityType()).toString());
            tag.putInt("level", ability.getLevel());
            list.add(tag);
        }
        return list;
    }

    public static void deserialize(Registry<IAbilityType> registry, IMutableAbilityStore capability, Tag nbt) {
        Map<Holder<IAbilityType>, Integer> abilityTypes = Maps.newHashMap();
        if (nbt instanceof ListTag) {
            if (((ListTag) nbt).getElementType() == Tag.TAG_COMPOUND) {
                ListTag list = (ListTag) nbt;
                for (int i = 0; i < list.size(); i++) {
                    CompoundTag tag = list.getCompound(i);
                    String name = tag.getString("name");
                    int level = tag.getInt("level");
                    Optional<Holder.Reference<IAbilityType>> abilityTypeOptional = registry.getHolder(ResourceLocation.parse(name));
                    if (abilityTypeOptional.isPresent()) {
                        abilityTypes.put(abilityTypeOptional.get(), level);
                    } else {
                        EverlastingAbilities.clog(org.apache.logging.log4j.Level.WARN, "Skipped loading unknown ability by name: " + name);
                    }
                }
            }
        } else {
            EverlastingAbilities.clog(org.apache.logging.log4j.Level.WARN, "Resetting a corrupted ability storage.");
        }
        capability.setAbilities(abilityTypes);
    }

}
