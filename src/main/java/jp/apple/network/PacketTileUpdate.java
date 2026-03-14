package jp.apple.network;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import jp.apple.tileentity.TileEntityTrainPlacer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class PacketTileUpdate implements IMessage {
    private int x, y, z, type, data;
    private String modelName = "";

    public PacketTileUpdate() {
    }

    public PacketTileUpdate(TileEntityTrainPlacer tile, int type, int data) {
        this.x = tile.xCoord;
        this.y = tile.yCoord;
        this.z = tile.zCoord;
        this.type = type;
        this.data = data;
    }

    public PacketTileUpdate(TileEntityTrainPlacer tile, int type, int index, String modelName) {
        this(tile, type, index);
        this.modelName = (modelName != null) ? modelName : "";
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
        type = buf.readInt();
        data = buf.readInt();
        modelName = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
        buf.writeInt(type);
        buf.writeInt(data);
        ByteBufUtils.writeUTF8String(buf, modelName);
    }

    public static class Handler implements IMessageHandler<PacketTileUpdate, IMessage> {
        @Override
        public IMessage onMessage(PacketTileUpdate msg, MessageContext ctx) {
            World world = ctx.getServerHandler().playerEntity.worldObj;
            TileEntity te = world.getTileEntity(msg.x, msg.y, msg.z);
            if (te instanceof TileEntityTrainPlacer) {
                TileEntityTrainPlacer tile = (TileEntityTrainPlacer) te;
                switch (msg.type) {
                    case 0:
                        tile.addEmptySlot();
                        break;
                    case 1:
                        tile.toggleDirection(msg.data);
                        break;
                    case 2:
                        tile.editingIndex = msg.data;
                        break;
                    case 3:
                        tile.setModelAtIndex(msg.data, msg.modelName);
                        break;
                }
                tile.markDirty();
                world.markBlockForUpdate(msg.x, msg.y, msg.z);
            }
            return null;
        }
    }
}