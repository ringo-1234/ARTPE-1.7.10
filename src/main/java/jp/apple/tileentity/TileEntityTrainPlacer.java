package jp.apple.tileentity;

import jp.ngt.rtm.modelpack.IModelSelector;
import jp.ngt.rtm.modelpack.ModelPackManager;
import jp.ngt.rtm.modelpack.modelset.ModelSetBase;
import jp.ngt.rtm.modelpack.state.ResourceState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;

import java.util.ArrayList;
import java.util.List;

public class TileEntityTrainPlacer extends TileEntity implements IModelSelector {
    public final List<String> trainModels = new ArrayList<String>();
    public final List<Integer> trainDirs = new ArrayList<Integer>();
    public int editingIndex = 0;

    public TileEntityTrainPlacer() {
        if (trainModels.isEmpty()) {
            trainModels.add("");
            trainDirs.add(0);
        }
    }


    @Override
    public ResourceState getResourceState() {

        String name = (editingIndex < trainModels.size()) ? trainModels.get(editingIndex) : "";
        ResourceState state = new ResourceState(this);

        state.setName(name != null ? name : "");
        return state;
    }

    @Override
    public String getModelType() {
        return "ModelTrain";
    }

    @Override
    public String getModelName() {
        if (editingIndex >= 0 && editingIndex < trainModels.size()) {
            String name = trainModels.get(editingIndex);
            return (name == null || name.isEmpty()) ? "kiha600" : name;
        }
        return "kiha600";
    }

    @Override
    public void setModelName(String name) {

        if (editingIndex >= 0 && editingIndex < trainModels.size()) {
            trainModels.set(editingIndex, name != null ? name : "");
            this.markDirty();
        }
    }

    @Override
    public int[] getPos() {
        return new int[]{this.xCoord, this.yCoord, this.zCoord};
    }

    @Override
    public boolean closeGui(String modelName, ResourceState state) {

        if (modelName != null && editingIndex < trainModels.size()) {
            trainModels.set(editingIndex, modelName);
            this.markDirty();
        }
        return true;
    }

    @Override
    public ModelSetBase getModelSet() {
        return ModelPackManager.INSTANCE.getModelSet(getModelType(), getModelName());
    }


    public void setModelAtIndex(int index, String name) {
        if (index >= 0 && index < trainModels.size()) {
            trainModels.set(index, name != null ? name : "");
            this.markDirty();
            if (this.worldObj != null) {
                this.worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
            }
        }
    }

    public void addEmptySlot() {
        trainModels.add("");
        trainDirs.add(0);
        this.markDirty();
        if (this.worldObj != null) {
            this.worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
    }

    public void toggleDirection(int index) {
        if (index >= 0 && index < trainDirs.size()) {
            trainDirs.set(index, trainDirs.get(index) == 0 ? 1 : 0);
            this.markDirty();
            if (this.worldObj != null) {
                this.worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
            }
        }
    }


    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        NBTTagList mList = new NBTTagList();
        for (String m : trainModels) {
            mList.appendTag(new NBTTagString(m != null ? m : ""));
        }
        nbt.setTag("ModelList", mList);
        int[] dArray = new int[trainDirs.size()];
        for (int i = 0; i < trainDirs.size(); i++) dArray[i] = trainDirs.get(i);
        nbt.setIntArray("DirList", dArray);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        trainModels.clear();
        trainDirs.clear();
        if (nbt.hasKey("ModelList")) {
            NBTTagList mList = nbt.getTagList("ModelList", 8);
            for (int i = 0; i < mList.tagCount(); i++) {
                trainModels.add(mList.getStringTagAt(i));
            }
            int[] dArray = nbt.getIntArray("DirList");
            for (int d : dArray) trainDirs.add(d);
        }
        if (trainModels.isEmpty()) {
            trainModels.add("");
            trainDirs.add(0);
        }
        while (trainDirs.size() < trainModels.size()) trainDirs.add(0);
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound nbt = new NBTTagCompound();
        this.writeToNBT(nbt);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 1, nbt);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        this.readFromNBT(pkt.func_148857_g());
    }
}