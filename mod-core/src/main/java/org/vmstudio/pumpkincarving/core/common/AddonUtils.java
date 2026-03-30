package org.vmstudio.pumpkincarving.core.common;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;
import org.vmstudio.visor.api.common.player.VRPose;

public final class AddonUtils {
    public static final Vector3f SHEARS_TIP_OFFSET = new Vector3f(0.0F, -0.10F, -0.25F);
    public static final double MAX_TIP_TO_FACE_DISTANCE = 0.18D;
    public static final double MAX_PLAYER_TO_BLOCK_DISTANCE_SQR = 36.0D;
    public static final double MIN_TICK_MOVEMENT = 0.01D;
    public static final double MAX_TICK_MOVEMENT = 0.35D;
    public static final double REQUIRED_STROKE_DISTANCE = 0.26D;
    public static final double DIAGONAL_BALANCE_RATIO = 1.6D;
    public static final double PARTICLE_OFFSET_FROM_FACE = 0.02D;
    public static final int CARVE_COOLDOWN_TICKS = 12;

    private AddonUtils() {
    }

    public static @NotNull Vec3 getShearsTip(VRPose handPose) {
        Vector3f tip = handPose.getCustomVector(new Vector3f(SHEARS_TIP_OFFSET)).add(handPose.getPosition());
        return new Vec3(tip.x(), tip.y(), tip.z());
    }

    public static boolean isCarvablePumpkin(BlockState state) {
        return state.is(Blocks.PUMPKIN);
    }

    public static boolean isValidPumpkinFace(Direction face) {
        return face.getAxis().isHorizontal();
    }

    public static Direction resolveCarvedFacing(Direction contactedFace, Direction fallbackFacing) {
        return isValidPumpkinFace(contactedFace) ? contactedFace : fallbackFacing.getOpposite();
    }

    public static Direction getNearestFace(BlockPos pos, Vec3 point) {
        double west = Math.abs(point.x - pos.getX());
        double east = Math.abs(point.x - (pos.getX() + 1.0D));
        double down = Math.abs(point.y - pos.getY());
        double up = Math.abs(point.y - (pos.getY() + 1.0D));
        double north = Math.abs(point.z - pos.getZ());
        double south = Math.abs(point.z - (pos.getZ() + 1.0D));

        Direction bestFace = Direction.WEST;
        double bestDistance = west;

        if (east < bestDistance) {
            bestDistance = east;
            bestFace = Direction.EAST;
        }
        if (down < bestDistance) {
            bestDistance = down;
            bestFace = Direction.DOWN;
        }
        if (up < bestDistance) {
            bestDistance = up;
            bestFace = Direction.UP;
        }
        if (north < bestDistance) {
            bestDistance = north;
            bestFace = Direction.NORTH;
        }
        if (south < bestDistance) {
            bestFace = Direction.SOUTH;
        }

        return bestFace;
    }

    public static Vec3 getFaceHorizontalAxis(Direction face) {
        return switch (face) {
            case NORTH, SOUTH -> new Vec3(1.0D, 0.0D, 0.0D);
            case EAST, WEST -> new Vec3(0.0D, 0.0D, 1.0D);
            default -> Vec3.ZERO;
        };
    }
}
