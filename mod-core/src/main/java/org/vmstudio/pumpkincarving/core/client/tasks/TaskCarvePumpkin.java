package org.vmstudio.pumpkincarving.core.client.tasks;

import io.netty.buffer.Unpooled;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.vmstudio.pumpkincarving.core.common.AddonNetworking;
import org.vmstudio.pumpkincarving.core.common.AddonUtils;
import org.vmstudio.pumpkincarving.core.network.NetworkHelper;
import org.vmstudio.visor.api.VisorAPI;
import org.vmstudio.visor.api.client.player.pose.PlayerPoseClient;
import org.vmstudio.visor.api.client.player.pose.PlayerPoseType;
import org.vmstudio.visor.api.client.tasks.RegisterVisorTask;
import org.vmstudio.visor.api.client.tasks.TaskType;
import org.vmstudio.visor.api.client.tasks.VisorTask;
import org.vmstudio.visor.api.common.HandType;
import org.vmstudio.visor.api.common.addon.VisorAddon;

import java.util.EnumMap;

@RegisterVisorTask
public class TaskCarvePumpkin extends VisorTask {
    public static final String ID = "carve_pumpkin";

    private final EnumMap<HandType, HandState> handStates = new EnumMap<>(HandType.class);

    public TaskCarvePumpkin(@NotNull VisorAddon owner) {
        super(owner);
        for (HandType handType : HandType.values()) {
            handStates.put(handType, new HandState());
        }
    }

    @Override
    protected void onRun(@Nullable LocalPlayer player) {
        if (player == null || player.level() == null) {
            return;
        }

        PlayerPoseClient pose = VisorAPI.client().getVRLocalPlayer().getPoseData(PlayerPoseType.TICK);
        for (HandType handType : HandType.values()) {
            tickHand(player, player.level(), pose, handType);
        }
    }

    private void tickHand(LocalPlayer player, Level level, PlayerPoseClient pose, HandType handType) {
        HandState handState = handStates.get(handType);
        handState.tickCooldown();

        InteractionHand interactionHand = handType.asInteractionHand();
        if (!(player.getItemInHand(interactionHand).getItem() instanceof ShearsItem)) {
            handState.reset();
            return;
        }

        Vec3 tip = AddonUtils.getShearsTip(pose.getGripHand(handType));
        PumpkinContact contact = findPumpkinContact(level, tip);
        if (contact == null) {
            handState.resetMotion();
            return;
        }

        if (!handState.matches(contact.pos(), contact.face())) {
            handState.startTracking(contact.pos(), contact.face(), tip);
            return;
        }

        Vec3 delta = tip.subtract(handState.lastTip);
        handState.lastTip = tip;

        double movement = delta.length();
        if (movement < AddonUtils.MIN_TICK_MOVEMENT || movement > AddonUtils.MAX_TICK_MOVEMENT) {
            return;
        }

        Vec3 horizontalAxis = AddonUtils.getFaceHorizontalAxis(contact.face());
        double horizontalComponent = delta.dot(horizontalAxis);
        double verticalComponent = delta.y;

        StrokeType strokeType = classifyStroke(horizontalComponent, verticalComponent);
        if (strokeType == StrokeType.NONE) {
            return;
        }

        double strokeDistance = Math.sqrt(horizontalComponent * horizontalComponent + verticalComponent * verticalComponent);
        handState.accumulate(strokeType, strokeDistance);
        spawnCutParticles(level, contact, tip, delta, strokeType == StrokeType.SLASH ? 2 : -2);

        if (!handState.tryCompleteStroke(strokeType)) {
            return;
        }

        VisorAPI.client().getInputManager().triggerHapticPulse(handType, 0.05F);
        spawnCutParticles(level, contact, tip, delta.scale(0.6D), strokeType == StrokeType.SLASH ? 5 : -5);
        if (!handState.hasCrossed()) {
            return;
        }

        if (handState.cooldown > 0) {
            return;
        }

        sendCarvePacket(interactionHand, contact.pos(), contact.face());
        VisorAPI.client().getInputManager().triggerHapticPulse(handType, 0.12F);
        spawnCutParticles(level, contact, tip, delta.scale(0.9D), 8);
        handState.onCarved();
    }

    private @Nullable PumpkinContact findPumpkinContact(Level level, Vec3 tip) {
        BlockPos centerPos = BlockPos.containing(tip);
        PumpkinContact bestContact = null;
        double bestDistance = AddonUtils.MAX_TIP_TO_FACE_DISTANCE;

        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos pos = centerPos.offset(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (!AddonUtils.isCarvablePumpkin(state)) {
                        continue;
                    }

                    Direction face = AddonUtils.getNearestFace(pos, tip);
                    if (!AddonUtils.isValidPumpkinFace(face)) {
                        continue;
                    }

                    double distance = distanceToFace(pos, face, tip);
                    if (distance > bestDistance || !isWithinFaceBounds(pos, face, tip)) {
                        continue;
                    }

                    bestDistance = distance;
                    bestContact = new PumpkinContact(pos.immutable(), face, state);
                }
            }
        }

        return bestContact;
    }

    private double distanceToFace(BlockPos pos, Direction face, Vec3 tip) {
        return switch (face) {
            case NORTH -> Math.abs(tip.z - pos.getZ());
            case SOUTH -> Math.abs(tip.z - (pos.getZ() + 1.0D));
            case WEST -> Math.abs(tip.x - pos.getX());
            case EAST -> Math.abs(tip.x - (pos.getX() + 1.0D));
            default -> Double.MAX_VALUE;
        };
    }

    private boolean isWithinFaceBounds(BlockPos pos, Direction face, Vec3 tip) {
        double inset = 0.12D;
        return switch (face) {
            case NORTH, SOUTH -> tip.x >= pos.getX() + inset
                    && tip.x <= pos.getX() + 1.0D - inset
                    && tip.y >= pos.getY() + inset
                    && tip.y <= pos.getY() + 1.0D - inset;
            case EAST, WEST -> tip.z >= pos.getZ() + inset
                    && tip.z <= pos.getZ() + 1.0D - inset
                    && tip.y >= pos.getY() + inset
                    && tip.y <= pos.getY() + 1.0D - inset;
            default -> false;
        };
    }

    private StrokeType classifyStroke(double horizontalComponent, double verticalComponent) {
        double absHorizontal = Math.abs(horizontalComponent);
        double absVertical = Math.abs(verticalComponent);
        if (absHorizontal < AddonUtils.MIN_TICK_MOVEMENT || absVertical < AddonUtils.MIN_TICK_MOVEMENT) {
            return StrokeType.NONE;
        }

        double ratio = Math.max(absHorizontal, absVertical) / Math.min(absHorizontal, absVertical);
        if (ratio > AddonUtils.DIAGONAL_BALANCE_RATIO) {
            return StrokeType.NONE;
        }

        return Math.signum(horizontalComponent) == Math.signum(verticalComponent)
                ? StrokeType.BACKSLASH
                : StrokeType.SLASH;
    }

    private void spawnCutParticles(Level level, PumpkinContact contact, Vec3 tip, Vec3 delta, int count) {
        int particleCount = Math.max(0, Math.abs(count));
        if (particleCount == 0) {
            return;
        }

        Vec3 faceNormal = Vec3.atLowerCornerOf(contact.face().getNormal());
        Vec3 origin = tip.subtract(faceNormal.scale(AddonUtils.PARTICLE_OFFSET_FROM_FACE));
        double directionScale = count > 0 ? 0.015D : -0.015D;
        double vx = faceNormal.x * 0.01D + delta.x * directionScale;
        double vy = faceNormal.y * 0.01D + delta.y * directionScale;
        double vz = faceNormal.z * 0.01D + delta.z * directionScale;

        for (int i = 0; i < particleCount; i++) {
            double ox = (level.random.nextDouble() - 0.5D) * 0.05D;
            double oy = (level.random.nextDouble() - 0.5D) * 0.05D;
            double oz = (level.random.nextDouble() - 0.5D) * 0.05D;
            level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, contact.state()),
                    origin.x + ox,
                    origin.y + oy,
                    origin.z + oz,
                    vx,
                    vy,
                    vz);
        }
    }

    private void sendCarvePacket(InteractionHand hand, BlockPos pos, Direction face) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeEnum(hand);
        buf.writeBlockPos(pos);
        buf.writeEnum(face);
        NetworkHelper.sendToServer(AddonNetworking.CARVE_PUMPKIN_C2S, buf);
    }

    @Override
    protected void onClear(@Nullable LocalPlayer player) {
        handStates.values().forEach(HandState::reset);
    }

    @Override
    public boolean isActive(@Nullable LocalPlayer player) {
        return player != null && VisorAPI.clientState().stateMode().isActive();
    }

    @Override
    public @NotNull TaskType getType() {
        return TaskType.VR_PLAYER_TICK;
    }

    @Override
    public @NotNull String getId() {
        return ID;
    }

    private enum StrokeType {
        NONE,
        SLASH,
        BACKSLASH
    }

    private static final class HandState {
        private BlockPos targetPos;
        private Direction targetFace;
        private Vec3 lastTip = Vec3.ZERO;
        private double slashProgress;
        private double backslashProgress;
        private boolean slashDone;
        private boolean backslashDone;
        private int cooldown;

        private void tickCooldown() {
            if (cooldown > 0) {
                cooldown--;
            }
        }

        private boolean matches(BlockPos pos, Direction face) {
            return pos.equals(targetPos) && face == targetFace;
        }

        private void startTracking(BlockPos pos, Direction face, Vec3 tip) {
            targetPos = pos.immutable();
            targetFace = face;
            lastTip = tip;
            slashProgress = 0.0D;
            backslashProgress = 0.0D;
            slashDone = false;
            backslashDone = false;
        }

        private void accumulate(StrokeType strokeType, double amount) {
            if (strokeType == StrokeType.SLASH && !slashDone) {
                slashProgress += amount;
            } else if (strokeType == StrokeType.BACKSLASH && !backslashDone) {
                backslashProgress += amount;
            }
        }

        private boolean tryCompleteStroke(StrokeType strokeType) {
            if (strokeType == StrokeType.SLASH && !slashDone && slashProgress >= AddonUtils.REQUIRED_STROKE_DISTANCE) {
                slashDone = true;
                slashProgress = 0.0D;
                return true;
            }

            if (strokeType == StrokeType.BACKSLASH && !backslashDone && backslashProgress >= AddonUtils.REQUIRED_STROKE_DISTANCE) {
                backslashDone = true;
                backslashProgress = 0.0D;
                return true;
            }

            return false;
        }

        private boolean hasCrossed() {
            return slashDone && backslashDone;
        }

        private void onCarved() {
            resetMotion();
            cooldown = AddonUtils.CARVE_COOLDOWN_TICKS;
        }

        private void resetMotion() {
            targetPos = null;
            targetFace = null;
            lastTip = Vec3.ZERO;
            slashProgress = 0.0D;
            backslashProgress = 0.0D;
            slashDone = false;
            backslashDone = false;
        }

        private void reset() {
            resetMotion();
            cooldown = 0;
        }
    }

    private record PumpkinContact(BlockPos pos, Direction face, BlockState state) {
    }
}
