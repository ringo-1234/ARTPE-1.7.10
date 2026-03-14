package jp.apple.gui;

import jp.apple.tileentity.TileEntityTrainPlacer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;

public class ContainerTrainPlacer extends Container {
    public final TileEntityTrainPlacer tile;

    public ContainerTrainPlacer(InventoryPlayer playerInv, TileEntityTrainPlacer tile) {
        this.tile = tile;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return true;
    }
}