package jp.apple.block;

import jp.apple.ARTPECore;
import jp.apple.ARTPEGuiHandler;
import jp.apple.tileentity.TileEntityTrainPlacer;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class BlockTrainPlacer extends BlockContainer {
    public BlockTrainPlacer() {
        super(Material.iron);
        this.setBlockName("trainplacerblock");
        this.setCreativeTab(ARTPECore.tabARTPE);
        this.setHardness(2.0F);
        this.setBlockTextureName("artpe:trainplacerblock");
    }

    @Override
    public int getRenderType() {
        return 0;
    }

    @Override
    public boolean isOpaqueCube() {
        return true;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return true;
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            return true;
        }

        TileEntity te = world.getTileEntity(x, y, z);
        if (!(te instanceof TileEntityTrainPlacer)) {
            return false;
        }

        player.openGui(ARTPECore.instance, ARTPEGuiHandler.GUI_ID_TRAIN_PLACER, world, x, y, z);
        return true;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityTrainPlacer();
    }
}