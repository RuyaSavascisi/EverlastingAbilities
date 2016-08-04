package org.cyclops.everlastingabilities.inventory.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.cyclops.cyclopscore.helper.InventoryHelpers;
import org.cyclops.cyclopscore.inventory.container.ItemInventoryContainer;
import org.cyclops.everlastingabilities.client.gui.GuiAbilityContainer;
import org.cyclops.everlastingabilities.item.ItemGuiAbilityContainer;

/**
 * Container for the labeller.
 * @author rubensworks
 */
public class ContainerAbilityContainer extends ItemInventoryContainer<ItemGuiAbilityContainer> {

    @SideOnly(Side.CLIENT)
    private GuiAbilityContainer gui;

    /**
     * Make a new instance.
     *
     * @param player The player.
     * @param itemIndex The index of the item in use inside the player inventory.
     */
    public ContainerAbilityContainer(EntityPlayer player, int itemIndex) {
        super(player.inventory, (ItemGuiAbilityContainer) InventoryHelpers.getItemFromIndex(player, itemIndex).getItem(), itemIndex);
        this.addPlayerInventory(player.inventory, 8, 31);
    }

    @SideOnly(Side.CLIENT)
    public void setGui(GuiAbilityContainer gui) {
        this.gui = gui;
    }

    @SideOnly(Side.CLIENT)
    public GuiAbilityContainer getGui() {
        return this.gui;
    }

    @Override
    protected int getSizeInventory() {
        return 0;
    }
}
