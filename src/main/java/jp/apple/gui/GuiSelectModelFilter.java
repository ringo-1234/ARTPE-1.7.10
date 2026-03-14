package jp.apple.gui;

import jp.apple.ARTPECore;
import jp.apple.network.PacketTileUpdate;
import jp.apple.tileentity.TileEntityTrainPlacer;
import jp.ngt.rtm.gui.GuiSelectModel;
import jp.ngt.rtm.modelpack.cfg.TrainConfig;
import jp.ngt.rtm.modelpack.modelset.ModelSetBase;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.world.World;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.List;

public class GuiSelectModelFilter extends GuiSelectModel {

    public GuiSelectModelFilter(World world, TileEntityTrainPlacer selector) {
        super(world, selector);
    }

    @Override
    public void initGui() {

        try {
            Field fAll = GuiSelectModel.class.getDeclaredField("modelListAll");
            fAll.setAccessible(true);
            List<ModelSetBase> allModels = (List<ModelSetBase>) fAll.get(this);
            if (allModels != null) {
                Iterator<ModelSetBase> it = allModels.iterator();
                while (it.hasNext()) {
                    ModelSetBase ms = it.next();
                    if (ms.getConfig() instanceof TrainConfig) {
                        TrainConfig cfg = (TrainConfig) ms.getConfig();
                        if (!"EC".equalsIgnoreCase(cfg.getSubType())) {
                            it.remove();
                        }
                    } else {
                        it.remove();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.initGui();
    }

    @Override
    protected void actionPerformed(GuiButton button) {

        if (button.id == 10900) {
            super.actionPerformed(button);
            return;
        }


        try {
            Field fSelect = GuiSelectModel.class.getDeclaredField("modelListSelect");
            fSelect.setAccessible(true);
            List<ModelSetBase> listSelect = (List<ModelSetBase>) fSelect.get(this);

            if (listSelect != null && button.id >= 0 && button.id < listSelect.size()) {
                ModelSetBase set = listSelect.get(button.id);
                String selectedName = set.getConfig().getName();
                if (selectedName == null) selectedName = "";

                if (this.selector instanceof TileEntityTrainPlacer) {
                    TileEntityTrainPlacer te = (TileEntityTrainPlacer) this.selector;

                    ARTPECore.network.sendToServer(
                            new PacketTileUpdate(te, 3, te.editingIndex, selectedName));

                    te.setModelAtIndex(te.editingIndex, selectedName);
                }


                this.selector.closeGui(selectedName, this.selector.getResourceState());
                this.mc.displayGuiScreen(null);
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        super.actionPerformed(button);
    }
}