package jp.apple.network;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import jp.apple.ARTPECore;
import jp.ngt.rtm.modelpack.ModelPackManager;
import jp.ngt.rtm.modelpack.modelset.ModelSetBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.util.ArrayList;
import java.util.List;

public class PacketFinishEditing implements IMessage {
    private List<String> trainModels;
    private List<Integer> trainDirs;
    private String trainName;

    public PacketFinishEditing() {
    }

    public PacketFinishEditing(List<String> models, List<Integer> dirs, String name) {
        this.trainModels = models;
        this.trainDirs = dirs;
        this.trainName = name;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int size = buf.readInt();
        this.trainModels = new ArrayList<String>();
        this.trainDirs = new ArrayList<Integer>();
        for (int i = 0; i < size; i++) {
            this.trainModels.add(ByteBufUtils.readUTF8String(buf));
            this.trainDirs.add(buf.readInt());
        }
        this.trainName = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.trainModels.size());
        for (int i = 0; i < this.trainModels.size(); i++) {
            ByteBufUtils.writeUTF8String(buf, this.trainModels.get(i));
            buf.writeInt(this.trainDirs.get(i));
        }
        ByteBufUtils.writeUTF8String(buf, this.trainName != null ? this.trainName : "車両");
    }

    public static class Handler implements IMessageHandler<PacketFinishEditing, IMessage> {
        @Override
        public IMessage onMessage(PacketFinishEditing message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            if (player == null) return null;


            List<String> validModels = new ArrayList<String>();
            List<Integer> validDirs = new ArrayList<Integer>();
            for (int i = 0; i < message.trainModels.size(); i++) {
                String modelId = message.trainModels.get(i);
                if (modelId == null || modelId.isEmpty() || "未選択".equals(modelId)) continue;
                validModels.add(modelId);
                validDirs.add(message.trainDirs.get(i));
            }
            if (validModels.isEmpty()) return null;


            float[] distances = new float[validModels.size()];
            for (int i = 0; i < validModels.size(); i++) {
                distances[i] = getTrainDistance(validModels.get(i));
            }

            ItemStack resultStack = new ItemStack(ARTPECore.itemArtpeTrain, 1, 0);
            NBTTagCompound rootTag = new NBTTagCompound();
            NBTTagList formationListNBT = new NBTTagList();

            double currentZ = 0.0;
            for (int i = 0; i < validModels.size(); i++) {

                if (i > 0) {
                    currentZ += (double) (distances[i - 1] + distances[i]);
                }

                NBTTagCompound trainTag = new NBTTagCompound();
                trainTag.setString("model", validModels.get(i));
                trainTag.setInteger("index", i);
                trainTag.setInteger("dir", validDirs.get(i));
                trainTag.setFloat("pos_z", (float) -currentZ);
                trainTag.setFloat("pos_x", 0.0f);
                trainTag.setFloat("pos_y", 0.0f);
                formationListNBT.appendTag(trainTag);
            }

            if (formationListNBT.tagCount() > 0) {
                rootTag.setTag("formations", formationListNBT);
                resultStack.setTagCompound(rootTag);
                if (message.trainName != null && !message.trainName.isEmpty()) {
                    resultStack.setStackDisplayName(message.trainName);
                }
                player.inventory.addItemStackToInventory(resultStack);
                player.inventoryContainer.detectAndSendChanges();
            }
            return null;
        }

        private float getTrainDistance(String modelName) {
            try {

                ModelSetBase ms = ModelPackManager.INSTANCE.getModelSet("ModelTrain", modelName);
                if (ms != null && !ms.isDummy()) {

                    jp.ngt.rtm.modelpack.cfg.TrainConfig cfg =
                            (jp.ngt.rtm.modelpack.cfg.TrainConfig) ms.getConfig();
                    return cfg.trainDistance;
                }
            } catch (Exception ignored) {
            }
            return 10.125f;
        }
    }
}