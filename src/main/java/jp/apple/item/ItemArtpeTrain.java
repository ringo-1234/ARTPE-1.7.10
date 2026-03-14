package jp.apple.item;

import jp.ngt.rtm.entity.train.*;
import jp.ngt.rtm.entity.train.util.Formation;
import jp.ngt.rtm.entity.train.util.FormationEntry;
import jp.ngt.rtm.entity.train.util.TrainState;
import jp.ngt.rtm.modelpack.ModelPackManager;
import jp.ngt.rtm.modelpack.cfg.TrainConfig;
import jp.ngt.rtm.modelpack.modelset.ModelSetVehicleBase;
import jp.ngt.rtm.rail.TileEntityLargeRailBase;
import jp.ngt.rtm.rail.util.RailMap;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class ItemArtpeTrain extends Item {
    private static final AtomicLong lastId = new AtomicLong(0);
    private static final int SEARCH_SPLIT = 2048;
    private static final int POS_SPLIT = 2048;

    public ItemArtpeTrain() {
        super();
        this.setUnlocalizedName("artpe_train");
        this.setMaxStackSize(1);
        this.setTextureName("artpe:item_train");
    }

    private long getUniqueId() {
        return lastId.incrementAndGet() + System.currentTimeMillis();
    }

    @Override
    public boolean onItemUse(ItemStack itemStack, EntityPlayer player, World world,
                             int x, int y, int z, int side,
                             float hitX, float hitY, float hitZ) {
        if (world.isRemote) return false;

        RailMap rm0 = findRailMap(world, player, x, y, z);
        if (rm0 == null) return false;

        List<TrainSet> trainSets = getFormationFromItem(itemStack);
        if (trainSets.isEmpty()) return false;

        int startIndex = rm0.getNearlestPoint(SEARCH_SPLIT,
                (double) x + 0.5, (double) z + 0.5);
        double startDist = rm0.getLength() * ((double) startIndex / SEARCH_SPLIT);

        float railYawAtStart = MathHelper.wrapAngleTo180_float(
                rm0.getRailRotation(SEARCH_SPLIT, startIndex));
        float fixedYaw = EntityBogie.fixBogieYaw(-player.rotationYaw, railYawAtStart);
        boolean isReverse = Math.abs(
                MathHelper.wrapAngleTo180_float(fixedYaw - railYawAtStart)) > 90.0F;
        double dirMul = isReverse ? -1.0D : 1.0D;

        long formationId = getUniqueId();
        Formation formation = new Formation(formationId, trainSets.size());
        RailContext ctx = new RailContext(rm0, startDist);
        float prevYaw = fixedYaw;

        for (int i = 0; i < trainSets.size(); i++) {
            TrainSet set = trainSets.get(i);
            double offsetFromStart = set.posZ * dirMul;
            double targetDist = startDist + offsetFromStart;
            PosRotation pr = resolvePos(world, player, ctx, targetDist, prevYaw, dirMul);
            prevYaw = pr.yaw;

            EntityTrainBase train = createTrainEntity(world, set.modelName);
            int entryDir = set.dir;
            float finalYaw = pr.yaw + (entryDir == 1 ? 180.0F : 0.0F);

            train.setPositionAndRotation(pr.posX, pr.posY, pr.posZ, finalYaw, pr.pitch);


            train.setModelName(set.modelName);
            train.setTrainStateData_NoSync(TrainState.TrainStateType.State_TrainDir.id, (byte) 1);
            train.setTrainStateData_NoSync(TrainState.TrainStateType.State_Notch.id, (byte) -8);
            train.setTrainStateData_NoSync(TrainState.TrainStateType.State_Direction.id, TrainState.Direction_Center.data);
            train.setSpeed_NoSync(0.0F);

            train.prevPosX = train.lastTickPosX = train.posX;
            train.prevPosY = train.lastTickPosY = train.posY;
            train.prevPosZ = train.lastTickPosZ = train.posZ;
            train.motionX = train.motionY = train.motionZ = 0.0D;


            try {
                train.bogieController.setupBogiePos(train);
                train.bogieController.spawnBogies(world, train);
            } catch (Exception e) {

            }

            world.spawnEntityInWorld(train);

            try {
                train.onModelChanged();
            } catch (Exception e) {

            }

            FormationEntry entry = new FormationEntry(train, i, entryDir);
            formation.entries[i] = entry;
            train.setFormation(formation);
        }

        formation.setSpeed(0.0F);
        if (formation.entries[0] != null) {
            formation.setTrainStateData(
                    TrainState.TrainStateType.State_Direction.id,
                    TrainState.Direction_Front.data,
                    formation.entries[0].train);
        }
        formation.setSpeed(0.0F);

        try {
            java.lang.reflect.Method realloc =
                    Formation.class.getDeclaredMethod("reallocation");
            realloc.setAccessible(true);
            realloc.invoke(formation);
        } catch (Exception e) {
            formation.sendPacket();
        }

        formation.getTrainStream().forEach(train -> {
            train.setTrainStateData_NoSync(
                    TrainState.TrainStateType.State_Direction.id,
                    TrainState.Direction_Center.data);
        });
        formation.sendPacket();

        if (!player.capabilities.isCreativeMode) {
            --itemStack.stackSize;
        }
        return true;
    }


    private RailMap findRailMap(World world, EntityPlayer player, int bx, int by, int bz) {
        for (int dy = 0; dy >= -2; dy--) {
            RailMap rm = TileEntityLargeRailBase.getRailMapFromCoordinates(
                    world, player, bx + 0.5, by + dy, bz + 0.5);
            if (rm != null) return rm;
        }
        return null;
    }

    private RailMap findNextRailMap(World world, EntityPlayer player,
                                    double cx, double cy, double cz,
                                    RailMap exclude) {
        int bx = MathHelper.floor_double(cx);
        int by = MathHelper.floor_double(cy);
        int bz = MathHelper.floor_double(cz);
        for (int dy = 1; dy >= -1; dy--) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    RailMap rm = TileEntityLargeRailBase.getRailMapFromCoordinates(
                            world, player,
                            bx + dx + 0.5, by + dy, bz + dz + 0.5);
                    if (rm != null && !rm.equals(exclude)) return rm;
                }
            }
        }
        return null;
    }

    private PosRotation resolvePos(World world, EntityPlayer player,
                                   RailContext ctx, double targetDist,
                                   float refYaw, double dirMul) {
        return traverseFromStart(world, player, ctx.railMap, ctx.baseDist,
                targetDist, refYaw, dirMul, 0);
    }

    private PosRotation traverseFromStart(World world, EntityPlayer player,
                                          RailMap startMap, double startDist,
                                          double targetDist, float refYaw,
                                          double dirMul, int depth) {
        double len = startMap.getLength();
        double localTarget = targetDist;

        if (localTarget >= 0.0D && localTarget <= len) {
            return sampleRail(startMap, localTarget, refYaw);
        }
        if (depth >= 16) {
            return extrapolate(startMap, localTarget, refYaw);
        }

        boolean forward = localTarget > len;
        int edgeIdx = forward ? POS_SPLIT : 0;
        double[] edgePosZX = startMap.getRailPos(POS_SPLIT, edgeIdx);
        double edgeX = edgePosZX[1];
        double edgeZ = edgePosZX[0];
        double edgeY = startMap.getRailHeight(POS_SPLIT, edgeIdx);

        RailMap nextMap = findNextRailMap(world, player, edgeX, edgeY, edgeZ, startMap);
        if (nextMap == null) {
            return extrapolate(startMap, localTarget, refYaw);
        }

        double overflow = forward ? (localTarget - len) : localTarget;
        int edgeIdxNext = forward ? 0 : POS_SPLIT;
        double[] nextEdgePosZX = nextMap.getRailPos(POS_SPLIT, edgeIdxNext);
        double[] nextEndPosZX = nextMap.getRailPos(POS_SPLIT, forward ? POS_SPLIT : 0);
        double distToStart = dist2D(edgeX, edgeZ, nextEdgePosZX[1], nextEdgePosZX[0]);
        double distToEnd = dist2D(edgeX, edgeZ, nextEndPosZX[1], nextEndPosZX[0]);

        double nextTargetDist;
        if (distToStart <= distToEnd) {
            nextTargetDist = forward ? overflow : nextMap.getLength() + overflow;
        } else {
            nextTargetDist = forward ? nextMap.getLength() - overflow : -overflow;
        }

        return traverseFromStart(world, player, nextMap, 0.0,
                nextTargetDist, refYaw, dirMul, depth + 1);
    }

    private double dist2D(double x0, double z0, double x1, double z1) {
        double dx = x1 - x0, dz = z1 - z0;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private PosRotation sampleRail(RailMap rm, double dist, float refYaw) {
        double len = rm.getLength();
        if (len <= 0.0) {
            double[] p = rm.getRailPos(1, 0);
            return new PosRotation(refYaw, 0f, 0f,
                    p[1],
                    rm.getRailHeight(1, 0) + EntityTrainBase.TRAIN_HEIGHT,
                    p[0]);
        }
        double ratio = MathHelper.clamp_double(dist / len, 0.0, 1.0);
        int index = MathHelper.clamp_int((int) (ratio * POS_SPLIT), 0, POS_SPLIT);

        float railYaw = MathHelper.wrapAngleTo180_float(
                rm.getRailRotation(POS_SPLIT, index));
        float yaw = EntityBogie.fixBogieYaw(refYaw, railYaw);
        float pitch = EntityBogie.fixBogiePitch(
                rm.getRailPitch(POS_SPLIT, index), railYaw, yaw);
        float roll = rm.getCant(POS_SPLIT, index);

        double[] posZX = rm.getRailPos(POS_SPLIT, index);
        return new PosRotation(yaw, pitch, roll,
                posZX[1],
                rm.getRailHeight(POS_SPLIT, index) + EntityTrainBase.TRAIN_HEIGHT,
                posZX[0]);
    }

    private PosRotation extrapolate(RailMap rm, double dist, float refYaw) {
        boolean forward = dist > rm.getLength();
        double clampedDist = forward ? rm.getLength() : 0.0;
        double overflow = forward ? dist - rm.getLength() : dist;

        PosRotation edge = sampleRail(rm, clampedDist, refYaw);
        float radF = (float) Math.toRadians(edge.yaw);
        float pitchF = (float) Math.toRadians(edge.pitch);
        double cosP = Math.cos(pitchF);

        return new PosRotation(edge.yaw, edge.pitch, edge.roll,
                edge.posX + (-Math.sin(radF) * cosP * overflow),
                edge.posY + (-Math.sin(pitchF) * overflow),
                edge.posZ + (Math.cos(radF) * cosP * overflow));
    }

    private EntityTrainBase createTrainEntity(World world, String modelName) {
        try {
            ModelSetVehicleBase ms = (ModelSetVehicleBase)
                    ModelPackManager.INSTANCE.getModelSet("ModelTrain", modelName);
            if (ms != null && !ms.isDummy()) {
                String subType = ((TrainConfig) ms.getConfig()).getSubType();
                if ("CC".equalsIgnoreCase(subType)) return new EntityFreightCar(world, "");
                if ("TC".equalsIgnoreCase(subType)) return new EntityTanker(world, "");
            }
        } catch (Exception ignored) {
        }

        return new EntityTrain(world, "");
    }

    private List<TrainSet> getFormationFromItem(ItemStack stack) {
        List<TrainSet> list = new ArrayList<TrainSet>();
        if (!stack.hasTagCompound()) return list;
        NBTTagList tagList = stack.getTagCompound().getTagList("formations", 10);
        for (int i = 0; i < tagList.tagCount(); i++) {
            NBTTagCompound tag = tagList.getCompoundTagAt(i);
            list.add(new TrainSet(
                    tag.getString("model"),
                    tag.getInteger("index"),
                    tag.getFloat("pos_x"),
                    tag.getFloat("pos_y"),
                    tag.getFloat("pos_z"),
                    tag.getFloat("yaw"),
                    tag.getFloat("pitch"),
                    tag.hasKey("dir") ? tag.getInteger("dir") : 0));
        }
        return list;
    }


    private static class RailContext {
        final RailMap railMap;
        final double baseDist;

        RailContext(RailMap rm, double base) {
            this.railMap = rm;
            this.baseDist = base;
        }
    }

    public static class TrainSet {
        public String modelName;
        public double posX, posY, posZ;
        public float yaw, pitch;
        public int index, dir;

        public TrainSet(String model, int index, double x, double y, double z,
                        float yaw, float pitch, int dir) {
            this.modelName = model;
            this.index = index;
            this.posX = x;
            this.posY = y;
            this.posZ = z;
            this.yaw = yaw;
            this.pitch = pitch;
            this.dir = dir;
        }
    }

    private static class PosRotation {
        final float yaw, pitch, roll;
        final double posX, posY, posZ;

        PosRotation(float yaw, float pitch, float roll,
                    double posX, double posY, double posZ) {
            this.yaw = yaw;
            this.pitch = pitch;
            this.roll = roll;
            this.posX = posX;
            this.posY = posY;
            this.posZ = posZ;
        }
    }
}