package jp.apple;

import cpw.mods.fml.common.network.IGuiHandler;
import jp.apple.gui.ContainerTrainPlacer;
import jp.apple.gui.GuiTrainPlacer;
import jp.apple.tileentity.TileEntityTrainPlacer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

public class ARTPEGuiHandler implements IGuiHandler {
    public static final int GUI_ID_TRAIN_PLACER = 0;

    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        if (ID == GUI_ID_TRAIN_PLACER) {
            return new ContainerTrainPlacer(player.inventory, (TileEntityTrainPlacer) world.getTileEntity(x, y, z));
        }
        return null;
    }

    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        if (ID == GUI_ID_TRAIN_PLACER) {
            return new GuiTrainPlacer(new ContainerTrainPlacer(player.inventory, (TileEntityTrainPlacer) world.getTileEntity(x, y, z)));
        }
        return null;
    }
}
