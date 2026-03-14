package jp.apple;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import jp.apple.block.BlockTrainPlacer;
import jp.apple.item.ItemArtpeTrain;
import jp.apple.network.PacketFinishEditing;
import jp.apple.network.PacketPreloadModels;
import jp.apple.network.PacketTileUpdate;
import jp.apple.tileentity.TileEntityTrainPlacer;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

@Mod(modid = ARTPECore.MODID, name = ARTPECore.NAME, version = ARTPECore.VERSION, dependencies = "required-after:RTM")
public class ARTPECore {
    public static final String MODID = "artpe";
    public static final String NAME = "ARTPE Train Extension";
    public static final String VERSION = "1.7_1.7.10";
    public static SimpleNetworkWrapper network;

    @Mod.Instance(MODID)
    public static ARTPECore instance;

    public static Block trainPlacerBlock;
    public static Item itemArtpeTrain;

    public static final CreativeTabs tabARTPE = new CreativeTabs("artpe_tab") {
        @Override
        public Item getTabIconItem() {
            return Item.getItemFromBlock(trainPlacerBlock);
        }
    };

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        trainPlacerBlock = new BlockTrainPlacer();
        GameRegistry.registerBlock(trainPlacerBlock, "trainplacerblock");

        itemArtpeTrain = new ItemArtpeTrain();
        GameRegistry.registerItem(itemArtpeTrain, "artpe_train");


        GameRegistry.registerTileEntity(TileEntityTrainPlacer.class, "ARTPE_TPlacer_TE");

        NetworkRegistry.INSTANCE.registerGuiHandler(this, new ARTPEGuiHandler());
        network = NetworkRegistry.INSTANCE.newSimpleChannel(MODID);

        network.registerMessage(PacketFinishEditing.Handler.class, PacketFinishEditing.class, 1, Side.SERVER);
        network.registerMessage(PacketPreloadModels.Handler.class, PacketPreloadModels.class, 2, Side.CLIENT);
        network.registerMessage(PacketTileUpdate.Handler.class, PacketTileUpdate.class, 3, Side.SERVER);
    }
}