package jp.apple.network;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import jp.ngt.rtm.modelpack.ModelPackManager;

import java.util.ArrayList;
import java.util.List;

public class PacketPreloadModels implements IMessage {
    private List<String> models;

    public PacketPreloadModels() {
    }

    public PacketPreloadModels(List<String> models) {
        this.models = models;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int size = buf.readInt();
        this.models = new ArrayList<String>();
        for (int i = 0; i < size; i++) {
            this.models.add(ByteBufUtils.readUTF8String(buf));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.models != null ? this.models.size() : 0);
        if (this.models != null) {
            for (String s : this.models) {
                ByteBufUtils.writeUTF8String(buf, s != null ? s : "");
            }
        }
    }

    public static class Handler implements IMessageHandler<PacketPreloadModels, IMessage> {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketPreloadModels message, MessageContext ctx) {
            if (message.models == null) return null;

            for (String name : message.models) {
                if (name == null || name.isEmpty() || name.equals("未選択")) continue;
                try {
                    ModelPackManager.INSTANCE.getModelSet("ModelTrain", name);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }
}